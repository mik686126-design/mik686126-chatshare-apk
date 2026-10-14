package com.example.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val userId: String = "",
    val displayName: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val statusText: String = "Hey there! I am using ChatShare.",
    val avatarColor: Long = 0xFF00A884,
    @ServerTimestamp val createdAt: Date? = null,
    @ServerTimestamp val updatedAt: Date? = null
)

@IgnoreExtraProperties
data class CloudChat(
    @DocumentId val id: String = "",
    val participants: List<String> = emptyList(),
    val participantPhones: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val lastMessageText: String = "",
    val lastMessageType: String = "TEXT",
    val lastMessageSenderId: String = "",
    @ServerTimestamp val createdAt: Date? = null,
    @ServerTimestamp val updatedAt: Date? = null
)

@IgnoreExtraProperties
data class CloudMessage(
    @DocumentId val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val type: String = "TEXT", // TEXT, IMAGE, DOCUMENT
    val fileData: String? = null, // Base64 data or URI for internet transfer
    val fileName: String? = null,
    val fileSizeBytes: Long = 0L,
    val mimeType: String? = null,
    val documentType: String = "OTHER",
    @ServerTimestamp val createdAt: Date? = null
)
