package com.example.model

data class Contact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val avatarColor: Long = 0xFF128C7E,
    val avatarUrl: String? = null,
    val statusText: String = "Hey there! I am using ChatShare.",
    val isOnline: Boolean = false,
    val lastSeenText: String = "Online",
    val isPinned: Boolean = false,
    val unreadCount: Int = 0
)
