package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.ChatMessage
import com.example.model.DocumentType
import com.example.model.MessageType
import com.example.ui.theme.WhatsAppDarkGreen
import com.example.ui.theme.WhatsAppLightGreen
import com.example.util.FileUtils

@Composable
fun DocsMediaScreen(
    viewModel: ChatViewModel,
    onOpenChat: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allMedia by viewModel.allMediaAndDocs.collectAsState()
    var selectedFilter by remember { mutableStateOf(0) } // 0: All, 1: Documents, 2: Pictures

    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            // Send to Saved Messages by default from vault
            viewModel.sendMediaMessage(context, "c5", uri, false, "Uploaded from Docs Vault")
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.sendMediaMessage(context, "c5", uri, true, "Uploaded from Photos Vault")
        }
    }

    val filteredList = when (selectedFilter) {
        1 -> allMedia.filter { it.type == MessageType.DOCUMENT }
        2 -> allMedia.filter { it.type == MessageType.IMAGE }
        else -> allMedia
    }

    val totalDocs = allMedia.count { it.type == MessageType.DOCUMENT }
    val totalPhotos = allMedia.count { it.type == MessageType.IMAGE }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8FA))
    ) {
        // Vault Header & Quick Actions
        Surface(
            color = Color.White,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Shared Documents & Pictures",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = WhatsAppDarkGreen
                )
                Text(
                    text = "Easily view, open, and send your files to others",
                    fontSize = 13.sp,
                    color = Color(0xFF667781)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { docPickerLauncher.launch("*/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppDarkGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("vault_upload_doc_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = "Upload Doc",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Doc", fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("vault_upload_photo_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Upload Photo",
                            tint = WhatsAppDarkGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Photo", fontSize = 13.sp, color = WhatsAppDarkGreen)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == 0,
                        onClick = { selectedFilter = 0 },
                        label = { Text("All (${allMedia.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WhatsAppDarkGreen,
                            selectedLabelColor = Color.White
                        )
                    )

                    FilterChip(
                        selected = selectedFilter == 1,
                        onClick = { selectedFilter = 1 },
                        label = { Text("Documents ($totalDocs)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WhatsAppDarkGreen,
                            selectedLabelColor = Color.White
                        )
                    )

                    FilterChip(
                        selected = selectedFilter == 2,
                        onClick = { selectedFilter = 2 },
                        label = { Text("Pictures ($totalPhotos)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WhatsAppDarkGreen,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // List of Documents & Pictures
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "Empty files",
                        tint = Color.LightGray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No shared pictures or documents yet",
                        color = Color.Gray,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Use the buttons above or send files in chats",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { it.id }) { item ->
                    MediaVaultCard(
                        item = item,
                        onOpen = { viewModel.openFile(context, item) },
                        onShare = { viewModel.shareFile(context, item) },
                        onPreviewImage = { viewModel.previewImage(item) },
                        onGoToChat = { onOpenChat(item.chatId) },
                        onDelete = { viewModel.deleteMessage(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun MediaVaultCard(
    item: ChatMessage,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onPreviewImage: () -> Unit,
    onGoToChat: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail or Colored Document Badge
            if (item.type == MessageType.IMAGE) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF0F2F5))
                        .clickable { onPreviewImage() }
                ) {
                    AsyncImage(
                        model = item.fileUri,
                        contentDescription = "Photo thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                val (docIcon, docColor) = getDocumentVisuals(item.documentType)
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(docColor.copy(alpha = 0.15f))
                        .clickable { onOpen() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = docIcon,
                        contentDescription = item.documentType.label,
                        tint = docColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Information
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.fileName ?: if (item.type == MessageType.IMAGE) "Shared Photo" else "Document",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF111B21),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.formattedFileSize.ifBlank { item.documentType.label },
                        fontSize = 12.sp,
                        color = Color(0xFF667781)
                    )
                    Text(
                        text = " • ${FileUtils.formatDateHeader(item.timestamp)}",
                        fontSize = 12.sp,
                        color = Color(0xFF667781)
                    )
                }

                if (item.text.isNotBlank() && item.text != item.fileName) {
                    Text(
                        text = item.text,
                        fontSize = 12.sp,
                        color = Color(0xFF54656F),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Actions: Open, Send to Others, Go to Chat, Delete
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onShare,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(WhatsAppDarkGreen)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Send to Others like WhatsApp",
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }

                Spacer(modifier = Modifier.width(5.dp))

                IconButton(
                    onClick = onGoToChat,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE9EDEF))
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "Go to Chat",
                        tint = Color(0xFF54656F),
                        modifier = Modifier.size(17.dp)
                    )
                }

                Spacer(modifier = Modifier.width(5.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFEBEE))
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete from vault",
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}
