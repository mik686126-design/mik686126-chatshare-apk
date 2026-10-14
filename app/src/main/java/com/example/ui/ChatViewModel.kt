package com.example.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChatRepository
import com.example.data.cloud.FirestoreChatRepository
import com.example.model.ChatMessage
import com.example.model.CloudChat
import com.example.model.CloudMessage
import com.example.model.Contact
import com.example.model.DocumentType
import com.example.model.MessageStatus
import com.example.model.MessageType
import com.example.model.UserProfile
import com.example.util.FileUtils
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface Screen {
    data object ChatList : Screen
    data class ChatDetail(val contactId: String) : Screen
}

data class UiState(
    val currentScreen: Screen = Screen.ChatList,
    val selectedTab: Int = 0, // 0: Chats, 1: Docs & Media, 2: Status
    val searchQuery: String = "",
    val isSearchOpen: Boolean = false,
    val showNewChatDialog: Boolean = false,
    val showProfileSetupDialog: Boolean = false,
    val selectedPreviewImage: ChatMessage? = null,
    val contactPendingDelete: Contact? = null,
    val messagePendingActions: ChatMessage? = null,
    val isOnlineSyncActive: Boolean = true
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    val repository = ChatRepository(application.applicationContext)
    val firestoreRepo = FirestoreChatRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(Firebase.auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    val contacts: StateFlow<List<Contact>> = repository.contacts
    val messages: StateFlow<List<ChatMessage>> = repository.messages

    val filteredContacts: StateFlow<List<Contact>> = combine(
        contacts,
        _uiState
    ) { contactList, state ->
        if (state.searchQuery.isBlank()) {
            contactList
        } else {
            contactList.filter {
                it.name.contains(state.searchQuery, ignoreCase = true) ||
                it.phoneNumber.contains(state.searchQuery, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMediaAndDocs: StateFlow<List<ChatMessage>> = combine(
        messages,
        _uiState
    ) { msgList, state ->
        val mediaList = msgList.filter { it.type == MessageType.IMAGE || it.type == MessageType.DOCUMENT }
            .sortedByDescending { it.timestamp }
        if (state.searchQuery.isBlank()) {
            mediaList
        } else {
            mediaList.filter {
                (it.fileName ?: "").contains(state.searchQuery, ignoreCase = true) ||
                it.text.contains(state.searchQuery, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Monitor Firebase Auth State
        Firebase.auth.addAuthStateListener { auth ->
            _currentUser.value = auth.currentUser
            if (auth.currentUser != null) {
                listenToCloudProfileAndChats()
            } else {
                _userProfile.value = null
            }
        }

        if (Firebase.auth.currentUser != null) {
            listenToCloudProfileAndChats()
        }
    }

    private fun listenToCloudProfileAndChats() {
        viewModelScope.launch {
            firestoreRepo.observeCurrentUserProfile().collect { profile ->
                _userProfile.value = profile
                if (profile == null) {
                    _uiState.value = _uiState.value.copy(showProfileSetupDialog = true)
                }
            }
        }

        // Real-time synchronization of cloud chats across internet
        viewModelScope.launch {
            firestoreRepo.observeUserChats().collect { cloudChats ->
                handleIncomingCloudChats(cloudChats)
            }
        }
    }

    private suspend fun handleIncomingCloudChats(cloudChats: List<CloudChat>) {
        val myUid = firestoreRepo.currentUser?.uid ?: return
        val currentContacts = contacts.value

        for (chat in cloudChats) {
            // Find other participant
            val otherUid = chat.participants.firstOrNull { it != myUid } ?: continue
            val otherPhone = chat.participantPhones.firstOrNull { it != _userProfile.value?.phoneNumber } ?: otherUid
            val otherName = chat.participantNames[otherUid] ?: otherPhone

            // Ensure contact exists in local database
            var localContact = currentContacts.find { it.phoneNumber == otherPhone || it.id == chat.id }
            if (localContact == null) {
                localContact = repository.addContact(otherName, otherPhone, "ChatShare User")
            }

            // Listen for live messages in this chat
            observeCloudMessagesForChat(chat.id, localContact.id)
        }
    }

    private fun observeCloudMessagesForChat(cloudChatId: String, localContactId: String) {
        viewModelScope.launch {
            val myUid = firestoreRepo.currentUser?.uid ?: return@launch
            firestoreRepo.observeChatMessages(cloudChatId).collect { cloudMsgs ->
                for (cloudMsg in cloudMsgs) {
                    // Check if already in local room DB
                    val existing = repository.messages.value.find { it.id == cloudMsg.id }
                    if (existing == null) {
                        val isFromMe = cloudMsg.senderId == myUid
                        var localFilePath = cloudMsg.fileData
                        var parsedDocType = DocumentType.OTHER

                        // If received a file from other person via base64, save to local storage
                        if (!isFromMe && !cloudMsg.fileData.isNullOrBlank() && (cloudMsg.type == "IMAGE" || cloudMsg.type == "DOCUMENT")) {
                            val saved = FileUtils.saveBase64ToAppStorage(
                                context = getApplication<Application>().applicationContext,
                                base64Str = cloudMsg.fileData,
                                fileName = cloudMsg.fileName ?: "file_${System.currentTimeMillis()}",
                                isImage = cloudMsg.type == "IMAGE"
                            )
                            if (saved != null) {
                                localFilePath = saved.internalUriString
                                parsedDocType = saved.documentType
                            }
                        }

                        val parsedType = try {
                            MessageType.valueOf(cloudMsg.type)
                        } catch (_: Exception) {
                            MessageType.TEXT
                        }

                        val message = ChatMessage(
                            id = cloudMsg.id,
                            chatId = localContactId,
                            senderName = cloudMsg.senderName,
                            isFromMe = isFromMe,
                            type = parsedType,
                            text = cloudMsg.text,
                            timestamp = cloudMsg.createdAt?.time ?: System.currentTimeMillis(),
                            status = MessageStatus.READ,
                            fileUri = localFilePath,
                            fileName = cloudMsg.fileName,
                            fileSizeBytes = cloudMsg.fileSizeBytes,
                            mimeType = cloudMsg.mimeType,
                            documentType = parsedDocType
                        )

                        // Insert into local persistent repository
                        withContext(Dispatchers.IO) {
                            com.example.data.local.ChatDatabase.getDatabase(getApplication()).chatMessageDao()
                                .insertMessage(com.example.data.local.ChatMessageEntity.fromDomain(message))
                        }
                    }
                }
            }
        }
    }

    fun setSelectedTab(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tabIndex)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun toggleSearch(isOpen: Boolean) {
        _uiState.value = _uiState.value.copy(
            isSearchOpen = isOpen,
            searchQuery = if (!isOpen) "" else _uiState.value.searchQuery
        )
    }

    fun openChat(contactId: String) {
        _uiState.value = _uiState.value.copy(currentScreen = Screen.ChatDetail(contactId))
    }

    fun navigateBack(): Boolean {
        if (_uiState.value.selectedPreviewImage != null) {
            _uiState.value = _uiState.value.copy(selectedPreviewImage = null)
            return true
        }
        if (_uiState.value.messagePendingActions != null) {
            _uiState.value = _uiState.value.copy(messagePendingActions = null)
            return true
        }
        if (_uiState.value.currentScreen !is Screen.ChatList) {
            _uiState.value = _uiState.value.copy(currentScreen = Screen.ChatList)
            return true
        }
        return false
    }

    fun showNewChatDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showNewChatDialog = show)
    }

    fun showProfileSetupDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showProfileSetupDialog = show)
    }

    fun setContactPendingDelete(contact: Contact?) {
        _uiState.value = _uiState.value.copy(contactPendingDelete = contact)
    }

    fun setMessagePendingActions(message: ChatMessage?) {
        _uiState.value = _uiState.value.copy(messagePendingActions = message)
    }

    fun previewImage(message: ChatMessage?) {
        _uiState.value = _uiState.value.copy(selectedPreviewImage = message)
    }

    fun saveUserProfile(name: String, phone: String, status: String) {
        viewModelScope.launch {
            try {
                firestoreRepo.saveUserProfile(name, phone, status)
                _uiState.value = _uiState.value.copy(showProfileSetupDialog = false)
                Toast.makeText(getApplication(), "Profile and Number Registered!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("ChatVM", "Failed to save profile", e)
                Toast.makeText(getApplication(), "Failed to save profile: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun sendTextMessage(chatId: String, text: String) {
        if (text.isBlank()) return
        val contact = repository.getContactById(chatId)

        viewModelScope.launch {
            // 1. Send locally to Room DB
            val localMsg = repository.sendMessage(
                chatId = chatId,
                text = text.trim(),
                type = MessageType.TEXT
            )

            // 2. Broadcast over the internet to other user via Firestore
            val profile = _userProfile.value
            if (profile != null && contact != null) {
                try {
                    val cloudChatId = firestoreRepo.getOrCreateChat(
                        myPhone = profile.phoneNumber,
                        myName = profile.displayName,
                        targetPhone = contact.phoneNumber,
                        targetName = contact.name
                    )
                    firestoreRepo.sendMessage(
                        chatId = cloudChatId,
                        senderName = profile.displayName,
                        text = text.trim(),
                        type = "TEXT"
                    )
                } catch (e: Exception) {
                    Log.e("ChatVM", "Cloud send failed", e)
                }
            }
        }
    }

    fun sendMediaMessage(context: Context, chatId: String, uri: Uri, isImage: Boolean, caption: String = "") {
        val contact = repository.getContactById(chatId)
        viewModelScope.launch {
            val stored = FileUtils.copyUriToAppStorage(context, uri, isImage)
            if (stored != null) {
                // 1. Send locally to Room DB
                val localMsg = repository.sendMessage(
                    chatId = chatId,
                    text = caption.trim(),
                    type = if (isImage) MessageType.IMAGE else MessageType.DOCUMENT,
                    fileUri = stored.internalUriString,
                    fileName = stored.fileName,
                    fileSizeBytes = stored.fileSizeBytes,
                    mimeType = stored.mimeType,
                    documentType = stored.documentType
                )

                // 2. Transmit file over the internet via Firestore
                val profile = _userProfile.value
                if (profile != null && contact != null) {
                    try {
                        val base64Data = FileUtils.encodeFileToBase64(stored.internalUriString)
                        val cloudChatId = firestoreRepo.getOrCreateChat(
                            myPhone = profile.phoneNumber,
                            myName = profile.displayName,
                            targetPhone = contact.phoneNumber,
                            targetName = contact.name
                        )
                        firestoreRepo.sendMessage(
                            chatId = cloudChatId,
                            senderName = profile.displayName,
                            text = caption.trim(),
                            type = if (isImage) "IMAGE" else "DOCUMENT",
                            fileData = base64Data,
                            fileName = stored.fileName,
                            fileSizeBytes = stored.fileSizeBytes,
                            mimeType = stored.mimeType,
                            documentType = stored.documentType.name
                        )
                    } catch (e: Exception) {
                        Log.e("ChatVM", "Cloud media send failed", e)
                    }
                }
            }
        }
    }

    fun createNewContact(name: String, phone: String, statusText: String = "") {
        if (name.isBlank()) return
        viewModelScope.launch {
            val contact = repository.addContact(
                name = name.trim(),
                phoneNumber = phone.trim().ifBlank { "+92 300 0000000" },
                statusText = statusText.trim().ifBlank { "Hey there! I am using ChatShare." }
            )

            // Connect to other user via Firestore if registered
            val profile = _userProfile.value
            if (profile != null) {
                try {
                    firestoreRepo.getOrCreateChat(
                        myPhone = profile.phoneNumber,
                        myName = profile.displayName,
                        targetPhone = contact.phoneNumber,
                        targetName = contact.name
                    )
                } catch (e: Exception) {
                    Log.e("ChatVM", "Creating cloud chat failed", e)
                }
            }

            openChat(contact.id)
        }
    }

    fun deleteContact(contactId: String) {
        viewModelScope.launch {
            repository.deleteContact(contactId)
            if (_uiState.value.currentScreen is Screen.ChatDetail) {
                val currentChatId = (_uiState.value.currentScreen as Screen.ChatDetail).contactId
                if (currentChatId == contactId) {
                    navigateBack()
                }
            }
            _uiState.value = _uiState.value.copy(contactPendingDelete = null)
        }
    }

    fun clearChat(chatId: String) {
        viewModelScope.launch {
            repository.clearChatMessages(chatId)
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            repository.deleteMessage(messageId)
            _uiState.value = _uiState.value.copy(messagePendingActions = null)
        }
    }

    fun toggleStar(messageId: String) {
        viewModelScope.launch {
            repository.toggleStar(messageId)
            _uiState.value = _uiState.value.copy(messagePendingActions = null)
        }
    }

    fun copyMessageText(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("ChatShare Message", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
        _uiState.value = _uiState.value.copy(messagePendingActions = null)
    }

    fun shareFile(context: Context, message: ChatMessage) {
        FileUtils.shareFileToOthers(
            context = context,
            filePathOrUri = message.fileUri,
            mimeType = message.mimeType,
            caption = message.text.ifBlank { message.fileName }
        )
    }

    fun openFile(context: Context, message: ChatMessage) {
        FileUtils.openFile(
            context = context,
            filePathOrUri = message.fileUri,
            mimeType = message.mimeType
        )
    }

    fun signOut() {
        Firebase.auth.signOut()
        _currentUser.value = null
        _userProfile.value = null
    }
}
