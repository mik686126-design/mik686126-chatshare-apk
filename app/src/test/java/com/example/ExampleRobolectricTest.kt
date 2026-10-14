package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ChatRepository
import com.example.model.DocumentType
import com.example.model.MessageType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ChatShare", appName)
    }

    @Test
    fun `test repository initialization and sending document message`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = ChatRepository(context)

        val msg = repository.sendMessage(
            chatId = "c1",
            text = "Here is the contract document",
            type = MessageType.DOCUMENT,
            fileName = "Contract.pdf",
            fileSizeBytes = 102400L,
            mimeType = "application/pdf",
            documentType = DocumentType.PDF
        )

        assertNotNull(msg)
        assertEquals("Contract.pdf", msg.fileName)
        assertEquals(MessageType.DOCUMENT, msg.type)
        assertEquals(DocumentType.PDF, msg.documentType)
    }

    @Test
    fun `test sending picture message`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = ChatRepository(context)

        val msg = repository.sendMessage(
            chatId = "c1",
            text = "Trip photo",
            type = MessageType.IMAGE,
            fileName = "trip.jpg",
            fileSizeBytes = 2048000L,
            mimeType = "image/jpeg",
            documentType = DocumentType.IMAGE
        )

        assertNotNull(msg)
        assertEquals("trip.jpg", msg.fileName)
        assertEquals(MessageType.IMAGE, msg.type)
        assertEquals("2.0 MB", msg.formattedFileSize)
    }
}
