package com.example.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChatMessage
import com.example.model.Contact
import com.example.model.MessageType
import com.example.ui.theme.WhatsAppDarkGreen
import com.example.ui.theme.WhatsAppLightBg
import com.example.ui.theme.WhatsAppLightGreen
import com.example.util.FileUtils
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    contactId: String,
    viewModel: ChatViewModel,
    messages: List<ChatMessage>,
    contact: Contact?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBack()
    }

    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showClearChatDialog by remember { mutableStateOf(false) }
    var showDeleteContactDialog by remember { mutableStateOf(false) }
    var selectedMessageForOptions by remember { mutableStateOf<ChatMessage?>(null) }
    val listState = rememberLazyListState()

    // Pick Photo from Gallery (Android Photo Picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.sendMediaMessage(
                context = context,
                chatId = contactId,
                uri = uri,
                isImage = true,
                caption = inputText
            )
            inputText = ""
        }
    }

    // Pick Document (PDF, Word, Excel, TXT, etc.)
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.sendMediaMessage(
                context = context,
                chatId = contactId,
                uri = uri,
                isImage = false,
                caption = inputText
            )
            inputText = ""
        }
    }

    // Capture quick camera picture
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            try {
                val picturesDir = File(context.filesDir, "pictures").apply { if (!exists()) mkdirs() }
                val photoFile = File(picturesDir, "camera_${System.currentTimeMillis()}.jpg")
                FileOutputStream(photoFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                val photoUri = Uri.fromFile(photoFile)
                viewModel.sendMediaMessage(
                    context = context,
                    chatId = contactId,
                    uri = photoUri,
                    isImage = true,
                    caption = inputText
                )
                inputText = ""
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WhatsAppLightBg)
    ) {
        // WhatsApp Top Bar for Contact
        Surface(
            color = WhatsAppDarkGreen,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("chat_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                if (contact != null) {
                    ContactAvatar(
                        name = contact.name,
                        avatarColor = contact.avatarColor,
                        sizeDp = 40,
                        isOnline = contact.isOnline,
                        modifier = Modifier.padding(end = 10.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = contact.name,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (contact.lastSeenText.contains("typing", ignoreCase = true)) {
                                "typing…"
                            } else {
                                contact.phoneNumber
                            },
                            color = if (contact.lastSeenText.contains("typing", ignoreCase = true)) {
                                WhatsAppLightGreen
                            } else {
                                Color.White.copy(alpha = 0.8f)
                            },
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                IconButton(onClick = { /* Voice Call */ }) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Voice Call",
                        tint = Color.White
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Chat Options",
                            tint = Color.White
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null, tint = WhatsAppDarkGreen) },
                            text = { Text("Send Picture") },
                            onClick = {
                                showMenu = false
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.AttachFile, contentDescription = null, tint = WhatsAppDarkGreen) },
                            text = { Text("Send Document (PDF/Docs)") },
                            onClick = {
                                showMenu = false
                                documentPickerLauncher.launch("*/*")
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = WhatsAppDarkGreen) },
                            text = { Text("Send to Others (Share App)") },
                            onClick = {
                                showMenu = false
                                val lastMedia = messages.lastOrNull { it.fileUri != null }
                                if (lastMedia != null) {
                                    viewModel.shareFile(context, lastMedia)
                                } else {
                                    FileUtils.shareFileToOthers(context, null, null, "Check out ChatShare app to send pictures and documents!")
                                }
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color(0xFFD32F2F)) },
                            text = { Text("Clear Chat Messages", color = Color(0xFFD32F2F)) },
                            onClick = {
                                showMenu = false
                                showClearChatDialog = true
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                            text = { Text("Delete Contact & Chat", color = Color.Red) },
                            onClick = {
                                showMenu = false
                                showDeleteContactDialog = true
                            }
                        )
                    }
                }
            }
        }

        // Chat Message List
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(WhatsAppLightBg)
        ) {
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            text = "No messages yet",
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF54656F),
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Send a text, photo, or document to start chatting. Everything is safely stored in your phone memory.",
                            color = Color(0xFF8696A0),
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    // Info banner
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFFEECD),
                                shadowElevation = 0.5.dp
                            ) {
                                Text(
                                    text = "🔒 Messages & files are stored locally in your device memory.",
                                    color = Color(0xFF54656F),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    items(messages, key = { it.id }) { message ->
                        MessageBubble(
                            message = message,
                            onImageClick = { viewModel.previewImage(it) },
                            onOpenDocument = { viewModel.openFile(context, it) },
                            onShareFile = { viewModel.shareFile(context, it) },
                            onMessageLongClick = { selectedMessageForOptions = it }
                        )
                    }
                }
            }
        }

        // WhatsApp Bottom Input Bar
        Surface(
            color = Color.Transparent,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Input Pill container
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 1.dp,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { /* Emoji picker */ },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SentimentSatisfiedAlt,
                                contentDescription = "Emoji",
                                tint = Color(0xFF8696A0),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        TextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Type a message", color = Color(0xFF8696A0), fontSize = 15.sp) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = Color(0xFF111B21),
                                unfocusedTextColor = Color(0xFF111B21),
                                cursorColor = WhatsAppDarkGreen,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            maxLines = 4,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("message_input_field")
                        )

                        // Attachment Paperclip
                        IconButton(
                            onClick = { showAttachmentSheet = true },
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("attach_paperclip_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = "Attach Document or Picture",
                                tint = Color(0xFF8696A0),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Camera Quick Button (only visible if input is empty)
                        if (inputText.isBlank()) {
                            IconButton(
                                onClick = { cameraLauncher.launch(null) },
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("input_camera_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Camera",
                                    tint = Color(0xFF8696A0),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Send or Mic Circle Button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(WhatsAppDarkGreen)
                        .clickable {
                            if (inputText.isNotBlank()) {
                                viewModel.sendTextMessage(contactId, inputText)
                                inputText = ""
                            } else {
                                // Open Gallery to send quick photo
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        }
                        .testTag("send_message_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (inputText.isNotBlank()) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice note / Send",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }

    // Attachment Modal Bottom Sheet
    if (showAttachmentSheet) {
        AttachmentBottomSheet(
            onDismiss = { showAttachmentSheet = false },
            onPickDocument = {
                documentPickerLauncher.launch("*/*")
            },
            onPickGallery = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onCaptureCamera = {
                cameraLauncher.launch(null)
            },
            onDirectShareToOthers = {
                val lastFileMsg = messages.lastOrNull { it.fileUri != null }
                if (lastFileMsg != null) {
                    viewModel.shareFile(context, lastFileMsg)
                } else {
                    documentPickerLauncher.launch("*/*")
                }
            }
        )
    }

    // Message Actions Bottom Sheet (Delete Message, Copy, Star, Share)
    selectedMessageForOptions?.let { message ->
        ModalBottomSheet(
            onDismissRequest = { selectedMessageForOptions = null },
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Message Options",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = WhatsAppDarkGreen,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (message.text.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.copyMessageText(context, message.text)
                                selectedMessageForOptions = null
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF54656F))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Copy Text", fontSize = 15.sp, color = Color(0xFF111B21))
                    }
                }

                if (message.fileUri != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.shareFile(context, message)
                                selectedMessageForOptions = null
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = WhatsAppDarkGreen)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Share / Send to Others", fontSize = 15.sp, color = Color(0xFF111B21))
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.toggleStar(message.id)
                            selectedMessageForOptions = null
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (message.isStarred) Icons.Default.StarOutline else Icons.Default.Star,
                        contentDescription = null,
                        tint = if (message.isStarred) Color(0xFF54656F) else Color(0xFFFFB300)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = if (message.isStarred) "Unstar message" else "Star message",
                        fontSize = 15.sp,
                        color = Color(0xFF111B21)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.deleteMessage(message.id)
                            selectedMessageForOptions = null
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Delete Message", fontSize = 15.sp, color = Color.Red, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Clear Chat History Confirmation Dialog
    if (showClearChatDialog) {
        AlertDialog(
            onDismissRequest = { showClearChatDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Clear Chat History?", fontWeight = FontWeight.Bold) },
            text = { Text("All messages in this chat will be permanently deleted from your phone memory.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearChat(contactId)
                        showClearChatDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Clear Messages", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearChatDialog = false }) {
                    Text("Cancel", color = Color(0xFF54656F))
                }
            }
        )
    }

    // Delete Entire Contact & Chat Dialog
    if (showDeleteContactDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteContactDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color.Red,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Delete Contact & Chat?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete ${contact?.name ?: "this contact"} and all shared pictures, documents, and chat records?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteContactDialog = false
                        viewModel.deleteContact(contactId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteContactDialog = false }) {
                    Text("Cancel", color = Color(0xFF54656F))
                }
            }
        )
    }
}
