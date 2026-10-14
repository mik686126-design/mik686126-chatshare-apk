package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.model.ChatMessage
import com.example.model.DocumentType
import com.example.model.MessageStatus
import com.example.model.MessageType

@Entity(
    tableName = "messages",
    indices = [Index(value = ["chatId"]), Index(value = ["timestamp"])]
)
data class ChatMessageEntity(
    @PrimaryKey
    val id: String,
    val chatId: String,
    val senderName: String,
    val isFromMe: Boolean,
    val type: String, // TEXT, IMAGE, DOCUMENT
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "READ", // SENDING, SENT, DELIVERED, READ
    val fileUri: String? = null,
    val fileName: String? = null,
    val fileSizeBytes: Long = 0L,
    val mimeType: String? = null,
    val documentType: String = "OTHER",
    val pageCount: Int? = null,
    val isStarred: Boolean = false
) {
    fun toDomain(): ChatMessage {
        val parsedType = try {
            MessageType.valueOf(type)
        } catch (_: Exception) {
            MessageType.TEXT
        }

        val parsedStatus = try {
            MessageStatus.valueOf(status)
        } catch (_: Exception) {
            MessageStatus.READ
        }

        val parsedDocType = try {
            DocumentType.valueOf(documentType)
        } catch (_: Exception) {
            DocumentType.OTHER
        }

        return ChatMessage(
            id = id,
            chatId = chatId,
            senderName = senderName,
            isFromMe = isFromMe,
            type = parsedType,
            text = text,
            timestamp = timestamp,
            status = parsedStatus,
            fileUri = fileUri,
            fileName = fileName,
            fileSizeBytes = fileSizeBytes,
            mimeType = mimeType,
            documentType = parsedDocType,
            pageCount = pageCount,
            isStarred = isStarred
        )
    }

    companion object {
        fun fromDomain(msg: ChatMessage): ChatMessageEntity {
            return ChatMessageEntity(
                id = msg.id,
                chatId = msg.chatId,
                senderName = msg.senderName,
                isFromMe = msg.isFromMe,
                type = msg.type.name,
                text = msg.text,
                timestamp = msg.timestamp,
                status = msg.status.name,
                fileUri = msg.fileUri,
                fileName = msg.fileName,
                fileSizeBytes = msg.fileSizeBytes,
                mimeType = msg.mimeType,
                documentType = msg.documentType.name,
                pageCount = msg.pageCount,
                isStarred = msg.isStarred
            )
        }
    }
}
