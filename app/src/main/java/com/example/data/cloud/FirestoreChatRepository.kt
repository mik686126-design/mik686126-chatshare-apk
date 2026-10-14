package com.example.data.cloud

import android.content.Context
import android.util.Log
import com.example.R
import com.example.model.CloudChat
import com.example.model.CloudMessage
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirestoreChatRepository(private val context: Context) {

    private val auth: FirebaseAuth = Firebase.auth
    private val db: FirebaseFirestore by lazy {
        val dbId = context.getString(R.string.firestore_database_id)
        FirebaseFirestore.getInstance(dbId)
    }

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    fun requireUserId(): String {
        return auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in with Google")
    }

    // Observe current user profile in Firestore
    fun observeCurrentUserProfile(): Flow<UserProfile?> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener: ListenerRegistration = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreRepo", "Error observing user profile", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val profile = snapshot.toObject(UserProfile::class.java)
                    trySend(profile)
                } else {
                    trySend(null)
                }
            }

        awaitClose { listener.remove() }
    }

    // Save or update user profile with Name and Phone Number
    suspend fun saveUserProfile(displayName: String, phoneNumber: String, statusText: String) {
        val user = auth.currentUser ?: return
        val uid = user.uid

        val profileData = hashMapOf(
            "userId" to uid,
            "displayName" to displayName.trim(),
            "phoneNumber" to phoneNumber.trim(),
            "email" to (user.email ?: ""),
            "statusText" to statusText.trim().ifBlank { "Hey there! I am using ChatShare." },
            "avatarColor" to 0xFF00A884L,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        db.collection("users").document(uid)
            .set(profileData, SetOptions.merge())
            .await()
    }

    // Search user by phone number across the internet
    suspend fun findUserByPhoneNumber(phone: String): UserProfile? {
        val cleaned = phone.replace(" ", "").replace("-", "").trim()
        val snapshot = db.collection("users").get().await()
        for (doc in snapshot.documents) {
            val profile = doc.toObject(UserProfile::class.java)
            if (profile != null) {
                val candidateCleaned = profile.phoneNumber.replace(" ", "").replace("-", "").trim()
                if (candidateCleaned == cleaned || profile.phoneNumber.contains(cleaned) || cleaned.contains(candidateCleaned)) {
                    return profile
                }
            }
        }
        return null
    }

    // Observe all live chats where current user is a participant
    fun observeUserChats(): Flow<List<CloudChat>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("chats")
            .whereArrayContains("participants", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreRepo", "Error observing chats", error)
                    return@addSnapshotListener
                }
                val chats = snapshot?.documents?.mapNotNull { it.toObject(CloudChat::class.java) } ?: emptyList()
                // Sort client-side by updatedAt descending
                val sorted = chats.sortedByDescending { it.updatedAt?.time ?: 0L }
                trySend(sorted)
            }

        awaitClose { listener.remove() }
    }

    // Observe real-time messages in a specific chat
    fun observeChatMessages(chatId: String): Flow<List<CloudMessage>> = callbackFlow {
        val listener = db.collection("chats").document(chatId)
            .collection("messages")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreRepo", "Error observing messages for $chatId", error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { it.toObject(CloudMessage::class.java) } ?: emptyList()
                val sorted = messages.sortedBy { it.createdAt?.time ?: 0L }
                trySend(sorted)
            }

        awaitClose { listener.remove() }
    }

    // Start or get live chat with another person by phone number
    suspend fun getOrCreateChat(
        myPhone: String,
        myName: String,
        targetPhone: String,
        targetName: String
    ): String {
        val myUid = requireUserId()
        val targetUser = findUserByPhoneNumber(targetPhone)
        val targetUid = targetUser?.userId ?: "pending_${targetPhone.filter { it.isDigit() }}"

        // Check if chat already exists
        val existingChats = db.collection("chats")
            .whereArrayContains("participants", myUid)
            .get()
            .await()

        for (doc in existingChats.documents) {
            val chat = doc.toObject(CloudChat::class.java)
            if (chat != null && (chat.participants.contains(targetUid) || chat.participantPhones.contains(targetPhone))) {
                return chat.id
            }
        }

        // Create new chat
        val newChatId = "chat_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val chatData = hashMapOf(
            "id" to newChatId,
            "participants" to listOf(myUid, targetUid),
            "participantPhones" to listOf(myPhone, targetPhone),
            "participantNames" to mapOf(myUid to myName, targetUid to (targetUser?.displayName ?: targetName)),
            "lastMessageText" to "Chat started",
            "lastMessageType" to "TEXT",
            "lastMessageSenderId" to myUid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        db.collection("chats").document(newChatId).set(chatData).await()
        return newChatId
    }

    // Send message to another person in real-time over the internet
    suspend fun sendMessage(
        chatId: String,
        senderName: String,
        text: String,
        type: String = "TEXT",
        fileData: String? = null,
        fileName: String? = null,
        fileSizeBytes: Long = 0L,
        mimeType: String? = null,
        documentType: String = "OTHER"
    ) {
        val myUid = requireUserId()
        val messageId = "msg_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"

        val msgPayload = hashMapOf(
            "id" to messageId,
            "chatId" to chatId,
            "senderId" to myUid,
            "senderName" to senderName,
            "text" to text,
            "type" to type,
            "fileData" to fileData,
            "fileName" to fileName,
            "fileSizeBytes" to fileSizeBytes,
            "mimeType" to mimeType,
            "documentType" to documentType,
            "createdAt" to FieldValue.serverTimestamp()
        )

        // 1. Add message to subcollection
        db.collection("chats").document(chatId)
            .collection("messages").document(messageId)
            .set(msgPayload)
            .await()

        // 2. Update chat metadata
        val preview = when (type) {
            "IMAGE" -> if (text.isNotBlank()) text else "📷 Photo"
            "DOCUMENT" -> "📄 ${fileName ?: "Document"}"
            else -> text
        }

        db.collection("chats").document(chatId).update(
            mapOf(
                "lastMessageText" to preview,
                "lastMessageType" to type,
                "lastMessageSenderId" to myUid,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun deleteChat(chatId: String) {
        val messages = db.collection("chats").document(chatId).collection("messages").get().await()
        for (doc in messages.documents) {
            doc.reference.delete()
        }
        db.collection("chats").document(chatId).delete().await()
    }

    suspend fun deleteMessage(chatId: String, messageId: String) {
        db.collection("chats").document(chatId).collection("messages").document(messageId).delete().await()
    }
}
