package com.example.model

data class ChatMessage(
    val id: String,
    val chatId: String,
    val senderName: String,
    val isFromMe: Boolean,
    val type: MessageType,
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.READ,
    val fileUri: String? = null,
    val fileName: String? = null,
    val fileSizeBytes: Long = 0L,
    val mimeType: String? = null,
    val documentType: DocumentType = DocumentType.OTHER,
    val pageCount: Int? = null,
    val isStarred: Boolean = false
) {
    val formattedFileSize: String
        get() {
            if (fileSizeBytes <= 0) return ""
            val kb = fileSizeBytes / 1024.0
            if (kb < 1024) {
                return "%.1f KB".format(kb)
            }
            val mb = kb / 1024.0
            return "%.1f MB".format(mb)
        }
}
