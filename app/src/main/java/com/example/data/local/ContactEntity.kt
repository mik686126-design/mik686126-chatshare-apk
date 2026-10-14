package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.Contact

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val phoneNumber: String,
    val avatarColor: Long = 0xFF128C7E,
    val avatarUrl: String? = null,
    val statusText: String = "Hey there! I am using ChatShare.",
    val isOnline: Boolean = false,
    val lastSeenText: String = "Online",
    val isPinned: Boolean = false,
    val unreadCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Contact {
        return Contact(
            id = id,
            name = name,
            phoneNumber = phoneNumber,
            avatarColor = avatarColor,
            avatarUrl = avatarUrl,
            statusText = statusText,
            isOnline = isOnline,
            lastSeenText = lastSeenText,
            isPinned = isPinned,
            unreadCount = unreadCount
        )
    }

    companion object {
        fun fromDomain(contact: Contact, createdAt: Long = System.currentTimeMillis()): ContactEntity {
            return ContactEntity(
                id = contact.id,
                name = contact.name,
                phoneNumber = contact.phoneNumber,
                avatarColor = contact.avatarColor,
                avatarUrl = contact.avatarUrl,
                statusText = contact.statusText,
                isOnline = contact.isOnline,
                lastSeenText = contact.lastSeenText,
                isPinned = contact.isPinned,
                unreadCount = contact.unreadCount,
                createdAt = createdAt
            )
        }
    }
}
