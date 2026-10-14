package com.example.data

import android.content.Context
import com.example.model.ChatMessage
import com.example.model.Contact
import com.example.model.DocumentType
import com.example.model.MessageStatus
import com.example.model.MessageType
import com.example.util.SampleData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class ChatRepository(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main)

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    init {
        SampleData.initializeSampleFiles(context)
        _contacts.value = SampleData.getInitialContacts()
        _messages.value = SampleData.getInitialMessages(context)
    }

    fun getContactById(contactId: String): Contact? {
        return _contacts.value.find { it.id == contactId }
    }

    fun getMessagesForChat(chatId: String): List<ChatMessage> {
        return _messages.value.filter { it.chatId == chatId }.sortedBy { it.timestamp }
    }

    fun getLastMessage(chatId: String): ChatMessage? {
        return _messages.value.filter { it.chatId == chatId }.maxByOrNull { it.timestamp }
    }

    fun getAllMediaAndDocs(): List<ChatMessage> {
        return _messages.value.filter { it.type == MessageType.IMAGE || it.type == MessageType.DOCUMENT }
            .sortedByDescending { it.timestamp }
    }

    fun sendMessage(
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

        _messages.value = _messages.value + message

        // Animate status to Delivered and Read
        scope.launch {
            delay(600)
            updateMessageStatus(message.id, MessageStatus.DELIVERED)
            delay(800)
            updateMessageStatus(message.id, MessageStatus.READ)

            // Auto-respond from contact if not self chat
            if (chatId != "c5") {
                simulateContactReply(chatId, type, fileName)
            }
        }

        return message
    }

    private fun updateMessageStatus(messageId: String, newStatus: MessageStatus) {
        _messages.value = _messages.value.map {
            if (it.id == messageId) it.copy(status = newStatus) else it
        }
    }

    private fun simulateContactReply(chatId: String, sentType: MessageType, fileName: String?) {
        val contact = getContactById(chatId) ?: return

        scope.launch {
            // Set contact to typing...
            _contacts.value = _contacts.value.map {
                if (it.id == chatId) it.copy(isOnline = true, lastSeenText = "typing…") else it
            }

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

            _messages.value = _messages.value + replyMessage

            // Restore contact status
            _contacts.value = _contacts.value.map {
                if (it.id == chatId) it.copy(isOnline = true, lastSeenText = "Online") else it
            }
        }
    }

    fun addContact(name: String, phoneNumber: String): Contact {
        val newContact = Contact(
            id = "c_${System.currentTimeMillis()}",
            name = name,
            phoneNumber = phoneNumber,
            avatarColor = listOf(0xFF00A884, 0xFF128C7E, 0xFF2196F3, 0xFF9C27B0, 0xFFFF9800).random(),
            statusText = "Hey there! I am using ChatShare.",
            isOnline = true,
            lastSeenText = "Online"
        )
        _contacts.value = listOf(newContact) + _contacts.value
        return newContact
    }

    fun toggleStar(messageId: String) {
        _messages.value = _messages.value.map {
            if (it.id == messageId) it.copy(isStarred = !it.isStarred) else it
        }
    }

    fun deleteMessage(messageId: String) {
        _messages.value = _messages.value.filter { it.id != messageId }
    }
}
