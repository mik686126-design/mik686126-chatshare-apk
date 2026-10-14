package com.example.util

import android.content.Context
import com.example.model.ChatMessage
import com.example.model.Contact
import com.example.model.DocumentType
import com.example.model.MessageStatus
import com.example.model.MessageType
import java.io.File
import java.io.FileOutputStream

object SampleData {

    fun initializeSampleFiles(context: Context) {
        try {
            val docsDir = File(context.filesDir, "documents").apply { if (!exists()) mkdirs() }
            val sampleDocFile = File(docsDir, "ChatShare_Guide.pdf")
            if (!sampleDocFile.exists()) {
                FileOutputStream(sampleDocFile).use { out ->
                    out.write(
                        ("Welcome to ChatShare!\n\n" +
                         "This application lets you send pictures and documents just like WhatsApp.\n" +
                         "1. Tap the paperclip icon in any chat.\n" +
                         "2. Choose 'Gallery' for pictures or 'Document' for PDFs, Word files, etc.\n" +
                         "3. Tap 'Send to Others' to share via WhatsApp, Gmail, or any other app.\n").toByteArray()
                    )
                }
            }

            val sampleInvoiceFile = File(docsDir, "Invoice_Oct_2026.pdf")
            if (!sampleInvoiceFile.exists()) {
                FileOutputStream(sampleInvoiceFile).use { out ->
                    out.write("INVOICE #94821\nAmount Due: $250.00\nStatus: Paid\nThank you for your business!".toByteArray())
                }
            }

            val sampleDocx = File(docsDir, "Team_Meeting_Notes.docx")
            if (!sampleDocx.exists()) {
                FileOutputStream(sampleDocx).use { out ->
                    out.write("Meeting Notes: Discussing picture and document sharing architecture in ChatShare.".toByteArray())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getInitialContacts(): List<Contact> {
        return listOf(
            Contact(
                id = "c1",
                name = "Sarah Khan",
                phoneNumber = "+92 300 1234567",
                avatarColor = 0xFF00A884,
                statusText = "Always available for project discussion",
                isOnline = true,
                lastSeenText = "Online",
                isPinned = true,
                unreadCount = 2
            ),
            Contact(
                id = "c2",
                name = "Office Documents & Team",
                phoneNumber = "Group • 8 participants",
                avatarColor = 0xFF075E54,
                statusText = "Work group for reports and file sharing",
                isOnline = true,
                lastSeenText = "Hamza, Sarah, Ali, You",
                isPinned = true,
                unreadCount = 1
            ),
            Contact(
                id = "c3",
                name = "Hamza Malik",
                phoneNumber = "+92 321 9876543",
                avatarColor = 0xFF128C7E,
                statusText = "At the gym 🏋️",
                isOnline = false,
                lastSeenText = "Last seen today at 4:10 PM",
                isPinned = false,
                unreadCount = 0
            ),
            Contact(
                id = "c4",
                name = "Fatima Ali",
                phoneNumber = "+92 333 4567890",
                avatarColor = 0xFFE91E63,
                statusText = "Design in progress 🎨",
                isOnline = true,
                lastSeenText = "Online",
                isPinned = false,
                unreadCount = 0
            ),
            Contact(
                id = "c5",
                name = "Saved Messages (You)",
                phoneNumber = "+92 300 0000000",
                avatarColor = 0xFF25D366,
                statusText = "Personal cloud & documents vault",
                isOnline = false,
                lastSeenText = "Self",
                isPinned = false,
                unreadCount = 0
            )
        )
    }

    fun getInitialMessages(context: Context): List<ChatMessage> {
        val docsDir = File(context.filesDir, "documents")
        val guidePath = File(docsDir, "ChatShare_Guide.pdf").absolutePath
        val invoicePath = File(docsDir, "Invoice_Oct_2026.pdf").absolutePath
        val notesPath = File(docsDir, "Team_Meeting_Notes.docx").absolutePath

        val now = System.currentTimeMillis()
        val m = 60 * 1000L

        return listOf(
            // Chat 1: Sarah Khan
            ChatMessage(
                id = "m1",
                chatId = "c1",
                senderName = "Sarah Khan",
                isFromMe = false,
                type = MessageType.TEXT,
                text = "Salam! Could you please send the latest guide and documents for the project?",
                timestamp = now - (30 * m),
                status = MessageStatus.READ
            ),
            ChatMessage(
                id = "m2",
                chatId = "c1",
                senderName = "Me",
                isFromMe = true,
                type = MessageType.DOCUMENT,
                text = "Here is the official guide and PDF. You can open it or forward to others!",
                timestamp = now - (25 * m),
                status = MessageStatus.READ,
                fileUri = guidePath,
                fileName = "ChatShare_Guide.pdf",
                fileSizeBytes = 184520L,
                mimeType = "application/pdf",
                documentType = DocumentType.PDF,
                pageCount = 4
            ),
            ChatMessage(
                id = "m3",
                chatId = "c1",
                senderName = "Sarah Khan",
                isFromMe = false,
                type = MessageType.TEXT,
                text = "Received it! Looks super clean and easy to use.",
                timestamp = now - (15 * m),
                status = MessageStatus.READ
            ),
            ChatMessage(
                id = "m4",
                chatId = "c1",
                senderName = "Sarah Khan",
                isFromMe = false,
                type = MessageType.DOCUMENT,
                text = "Also here is the signed invoice from our side:",
                timestamp = now - (5 * m),
                status = MessageStatus.READ,
                fileUri = invoicePath,
                fileName = "Invoice_Oct_2026.pdf",
                fileSizeBytes = 245800L,
                mimeType = "application/pdf",
                documentType = DocumentType.PDF,
                pageCount = 2
            ),

            // Chat 2: Office Documents & Team
            ChatMessage(
                id = "m5",
                chatId = "c2",
                senderName = "Hamza Malik",
                isFromMe = false,
                type = MessageType.TEXT,
                text = "Team, please share all meeting notes and photos from yesterday's presentation.",
                timestamp = now - (120 * m),
                status = MessageStatus.READ
            ),
            ChatMessage(
                id = "m6",
                chatId = "c2",
                senderName = "Me",
                isFromMe = true,
                type = MessageType.DOCUMENT,
                text = "Attached the meeting agenda and action items document:",
                timestamp = now - (110 * m),
                status = MessageStatus.READ,
                fileUri = notesPath,
                fileName = "Team_Meeting_Notes.docx",
                fileSizeBytes = 412500L,
                mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                documentType = DocumentType.DOC
            ),

            // Chat 3: Hamza Malik
            ChatMessage(
                id = "m7",
                chatId = "c3",
                senderName = "Hamza Malik",
                isFromMe = false,
                type = MessageType.TEXT,
                text = "Bro, send me the pictures we took earlier!",
                timestamp = now - (180 * m),
                status = MessageStatus.READ
            ),
            ChatMessage(
                id = "m8",
                chatId = "c3",
                senderName = "Me",
                isFromMe = true,
                type = MessageType.TEXT,
                text = "Sure, selecting from gallery now 📸",
                timestamp = now - (170 * m),
                status = MessageStatus.READ
            ),

            // Chat 4: Fatima Ali
            ChatMessage(
                id = "m9",
                chatId = "c4",
                senderName = "Fatima Ali",
                isFromMe = false,
                type = MessageType.TEXT,
                text = "Working on the new logo icons right now. Will send pictures shortly!",
                timestamp = now - (240 * m),
                status = MessageStatus.READ
            ),

            // Chat 5: Saved Messages
            ChatMessage(
                id = "m10",
                chatId = "c5",
                senderName = "Me",
                isFromMe = true,
                type = MessageType.DOCUMENT,
                text = "Important backup docs",
                timestamp = now - (300 * m),
                status = MessageStatus.READ,
                fileUri = guidePath,
                fileName = "ChatShare_Guide.pdf",
                fileSizeBytes = 184520L,
                mimeType = "application/pdf",
                documentType = DocumentType.PDF,
                pageCount = 4
            )
        )
    }
}
