package com.example.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChatRepository
import com.example.model.ChatMessage
import com.example.model.Contact
import com.example.model.MessageType
import com.example.util.FileUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
    val selectedPreviewImage: ChatMessage? = null,
    val contactPendingDelete: Contact? = null,
    val messagePendingActions: ChatMessage? = null
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    val repository = ChatRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

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

    fun setContactPendingDelete(contact: Contact?) {
        _uiState.value = _uiState.value.copy(contactPendingDelete = contact)
    }

    fun setMessagePendingActions(message: ChatMessage?) {
        _uiState.value = _uiState.value.copy(messagePendingActions = message)
    }

    fun previewImage(message: ChatMessage?) {
        _uiState.value = _uiState.value.copy(selectedPreviewImage = message)
    }

    fun sendTextMessage(chatId: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.sendMessage(
                chatId = chatId,
                text = text.trim(),
                type = MessageType.TEXT
            )
        }
    }

    fun sendMediaMessage(context: Context, chatId: String, uri: Uri, isImage: Boolean, caption: String = "") {
        viewModelScope.launch {
            val stored = FileUtils.copyUriToAppStorage(context, uri, isImage)
            if (stored != null) {
                repository.sendMessage(
                    chatId = chatId,
                    text = caption.trim(),
                    type = if (isImage) MessageType.IMAGE else MessageType.DOCUMENT,
                    fileUri = stored.internalUriString,
                    fileName = stored.fileName,
                    fileSizeBytes = stored.fileSizeBytes,
                    mimeType = stored.mimeType,
                    documentType = stored.documentType
                )
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
}
