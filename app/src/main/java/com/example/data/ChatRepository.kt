package com.example.data

import android.content.Context
import com.example.data.local.ChatDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ContactEntity
import com.example.model.ChatMessage
import com.example.model.Contact
import com.example.model.DocumentType
import com.example.model.MessageStatus
import com.example.model.MessageType
import com.example.util.SampleData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class ChatRepository(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val database = ChatDatabase.getDatabase(context)
    private val contactDao = database.contactDao()
    private val messageDao = database.chatMessageDao()

    val contacts: StateFlow<List<Contact>> = contactDao.getAllContacts()
        .map { list -> list.map { it.toDomain() } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val messages: StateFlow<List<ChatMessage>> = messageDao.getAllMessages()
        .map { list -> list.map { it.toDomain() } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    init {
        scope.launch {
            if (contactDao.getContactCount() == 0) {
                // Initialize sample files and first-time starter contacts
                SampleData.initializeSampleFiles(context)
                val initialContacts = SampleData.getInitialContacts().mapIndexed { index, contact ->
                    ContactEntity.fromDomain(contact, createdAt = System.currentTimeMillis() - (index * 1000L))
                }
                contactDao.insertContacts(initialContacts)

                val initialMessages = SampleData.getInitialMessages(context).map {
                    ChatMessageEntity.fromDomain(it)
                }
                messageDao.insertMessages(initialMessages)
            }
        }
    }

    fun getContactById(contactId: String): Contact? {
        return contacts.value.find { it.id == contactId }
    }

    fun getLastMessage(chatId: String): ChatMessage? {
        return messages.value.filter { it.chatId == chatId }.maxByOrNull { it.timestamp }
    }

    suspend fun addContact(name: String, phoneNumber: String, statusText: String = "Hey there! I am using ChatShare."): Contact {
        return withContext(Dispatchers.IO) {
            val contact = Contact(
                id = "c_${System.currentTimeMillis()}",
                name = name,
                phoneNumber = phoneNumber,
                avatarColor = listOf(0xFF00A884, 0xFF128C7E, 0xFF2196F3, 0xFF9C27B0, 0xFFFF9800, 0xFFE91E63).random(),
                statusText = statusText.ifBlank { "Hey there! I am using ChatShare." },
                isOnline = true,
                lastSeenText = "Online",
                isPinned = false,
                unreadCount = 0
            )
            contactDao.insertContact(ContactEntity.fromDomain(contact, createdAt = System.currentTimeMillis()))
            contact
        }
    }

    suspend fun deleteContact(contactId: String) {
        withContext(Dispatchers.IO) {
            contactDao.deleteContactById(contactId)
            messageDao.deleteMessagesByChatId(contactId)
        }
    }

    suspend fun clearChatMessages(chatId: String) {
        withContext(Dispatchers.IO) {
            messageDao.deleteMessagesByChatId(chatId)
        }
    }

    suspend fun deleteMessage(messageId: String) {
        withContext(Dispatchers.IO) {
            messageDao.deleteMessageById(messageId)
        }
    }

    suspend fun toggleStar(messageId: String) {
        withContext(Dispatchers.IO) {
            messageDao.toggleStar(messageId)
        }
    }

    suspend fun sendMessage(
        chatId: String,
        text: String = "",
        type: MessageType = MessageType.TEXT,
        fileUri: String? = null,
        fileName: String? = null,
        fileSizeBytes: Long = 0L,
        mimeType: String? = null,
        documentType: DocumentType = DocumentType.OTHER
    ): ChatMessage {
        val message = ChatMessage(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            senderName = "Me",
            isFromMe = true,
            type = type,
            text = text,
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.SENT,
            fileUri = fileUri,
            fileName = fileName,
            fileSizeBytes = fileSizeBytes,
            mimeType = mimeType,
            documentType = documentType
        )

        withContext(Dispatchers.IO) {
            messageDao.insertMessage(ChatMessageEntity.fromDomain(message))
        }

        // Animate status to Delivered and Read in database
        scope.launch {
            delay(600)
            messageDao.updateMessageStatus(message.id, MessageStatus.DELIVERED.name)
            delay(800)
            messageDao.updateMessageStatus(message.id, MessageStatus.READ.name)

            // Auto-respond from contact if not self chat
            if (chatId != "c5") {
                simulateContactReply(chatId, type, fileName)
            }
        }

        return message
    }

    private fun simulateContactReply(chatId: String, sentType: MessageType, fileName: String?) {
        scope.launch {
            val contact = contactDao.getContactById(chatId) ?: return@launch

            // Set typing indicator
            contactDao.updateContact(contact.copy(isOnline = true, lastSeenText = "typing…"))

            delay(1800)

            val replyText = when (sentType) {
                MessageType.DOCUMENT -> {
                    val name = fileName ?: "file"
                    listOf(
                        "Received \"$name\"! Thanks for sending the document. 👍",
                        "Got the document! Reviewing it right now.",
                        "Thanks for sharing! Looks complete and verified."
                    ).random()
                }
                MessageType.IMAGE -> {
                    listOf(
                        "Nice picture! Received clearly. 📸",
                        "Got the image! Looks great.",
                        "Thanks for sharing this photo!"
                    ).random()
                }
                MessageType.TEXT -> {
                    listOf(
                        "Got your message! I'm on it.",
                        "Sounds good! Let me know if you need any other documents.",
                        "Understood! Thanks for updating me."
                    ).random()
                }
            }

            val replyMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                chatId = chatId,
                senderName = contact.name,
                isFromMe = false,
                type = MessageType.TEXT,
                text = replyText,
                timestamp = System.currentTimeMillis(),
                status = MessageStatus.READ
            )

            messageDao.insertMessage(ChatMessageEntity.fromDomain(replyMessage))

            // Restore contact status
            contactDao.updateContact(contact.copy(isOnline = true, lastSeenText = "Online"))
        }
    }
}
