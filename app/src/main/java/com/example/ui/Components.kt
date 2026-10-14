package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.model.ChatMessage
import com.example.model.DocumentType
import com.example.model.MessageStatus
import com.example.model.MessageType
import com.example.ui.theme.DocColor
import com.example.ui.theme.GenericFileColor
import com.example.ui.theme.PdfColor
import com.example.ui.theme.SheetColor
import com.example.ui.theme.SlideColor
import com.example.ui.theme.WhatsAppBlueCheck
import com.example.ui.theme.WhatsAppDarkGreen
import com.example.ui.theme.WhatsAppLightGreen
import com.example.ui.theme.WhatsAppLightIncomingBubble
import com.example.ui.theme.WhatsAppLightOutgoingBubble
import com.example.ui.theme.ZipColor
import com.example.util.FileUtils

@Composable
fun ContactAvatar(
    name: String,
    avatarColor: Long,
    sizeDp: Int = 46,
    isOnline: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(sizeDp.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Color(avatarColor)),
            contentAlignment = Alignment.Center
        ) {
            val initials = name.split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .map { it.first().uppercaseChar() }
                .joinToString("")
            Text(
                text = initials.ifBlank { "?" },
                color = Color.White,
                fontSize = (sizeDp * 0.38).sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (isOnline) {
            Box(
                modifier = Modifier
                    .size((sizeDp * 0.3).dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(WhatsAppLightGreen)
            )
        }
    }
}

@Composable
fun StatusTicks(status: MessageStatus, modifier: Modifier = Modifier) {
    when (status) {
        MessageStatus.SENDING -> {
            Text(
                text = "🕒",
                fontSize = 10.sp,
                modifier = modifier
            )
        }
        MessageStatus.SENT -> {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Sent",
                tint = Color(0xFF8696A0),
                modifier = modifier.size(14.dp)
            )
        }
        MessageStatus.DELIVERED -> {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Delivered",
                tint = Color(0xFF8696A0),
                modifier = modifier.size(15.dp)
            )
        }
        MessageStatus.READ -> {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Read",
                tint = WhatsAppBlueCheck,
                modifier = modifier.size(15.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: ChatMessage,
    onImageClick: (ChatMessage) -> Unit,
    onOpenDocument: (ChatMessage) -> Unit,
    onShareFile: (ChatMessage) -> Unit,
    onMessageLongClick: (ChatMessage) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isFromMe = message.isFromMe
    val bubbleColor = if (isFromMe) WhatsAppLightOutgoingBubble else WhatsAppLightIncomingBubble
    val bubbleShape = if (isFromMe) {
        RoundedCornerShape(topStart = 12.dp, topEnd = 4.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = bubbleShape,
            color = bubbleColor,
            shadowElevation = 1.dp,
            modifier = Modifier
                .widthIn(min = 90.dp, max = 320.dp)
                .combinedClickable(
                    onClick = {
                        if (message.type == MessageType.TEXT) {
                            onMessageLongClick(message)
                        }
                    },
                    onLongClick = { onMessageLongClick(message) }
                )
        ) {
            Column(
                modifier = Modifier.padding(6.dp)
            ) {
                // Sender name for group chats
                if (!isFromMe && message.chatId == "c2") {
                    Text(
                        text = message.senderName,
                        color = WhatsAppDarkGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                when (message.type) {
                    MessageType.TEXT -> {
                        Text(
                            text = message.text,
                            color = Color(0xFF111B21),
                            fontSize = 15.sp,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    MessageType.IMAGE -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.05f))
                                .clickable { onImageClick(message) }
                        ) {
                            AsyncImage(
                                model = message.fileUri,
                                contentDescription = "Shared photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                            )

                            // Quick Share overlay button
                            IconButton(
                                onClick = { onShareFile(message) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(32.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Send to Others",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        if (message.text.isNotBlank()) {
                            Text(
                                text = message.text,
                                color = Color(0xFF111B21),
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                            )
                        }
                    }

                    MessageType.DOCUMENT -> {
                        DocumentCard(
                            message = message,
                            onOpen = { onOpenDocument(message) },
                            onShare = { onShareFile(message) }
                        )

                        if (message.text.isNotBlank()) {
                            Text(
                                text = message.text,
                                color = Color(0xFF111B21),
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Timestamp, Star & Status Row
                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 2.dp, end = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (message.isStarred) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Starred",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier
                                .size(13.dp)
                                .padding(end = 3.dp)
                        )
                    }

                    Text(
                        text = FileUtils.formatTime(message.timestamp),
                        color = Color(0xFF667781),
                        fontSize = 11.sp
                    )
                    if (isFromMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        StatusTicks(status = message.status)
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentCard(
    message: ChatMessage,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (docIcon, docColor) = getDocumentVisuals(message.documentType)

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE9EDEF).copy(alpha = 0.7f)),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpen() }
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Colored Document Icon
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(docColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = docIcon,
                        contentDescription = "Document icon",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Title & Details
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = message.fileName ?: "Document",
                        color = Color(0xFF111B21),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = message.formattedFileSize.ifBlank { message.documentType.label },
                            color = Color(0xFF667781),
                            fontSize = 11.sp
                        )
                        Text(
                            text = " • ${message.documentType.label}",
                            color = docColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Action Buttons Row: Open & Send to Others
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White,
                    shadowElevation = 0.5.dp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onOpen() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "Open",
                            tint = WhatsAppDarkGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Open",
                            color = WhatsAppDarkGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = WhatsAppDarkGreen,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onShare() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Send to Others",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Send to Others",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

fun getDocumentVisuals(docType: DocumentType): Pair<ImageVector, Color> {
    return when (docType) {
        DocumentType.PDF -> Icons.Default.PictureAsPdf to PdfColor
        DocumentType.DOC -> Icons.Default.Description to DocColor
        DocumentType.SHEET -> Icons.Default.TableChart to SheetColor
        DocumentType.SLIDE -> Icons.Default.Description to SlideColor
        DocumentType.ZIP -> Icons.Default.FolderZip to ZipColor
        DocumentType.IMAGE -> Icons.Default.Image to WhatsAppLightGreen
        DocumentType.TXT -> Icons.Default.Description to Color(0xFF607D8B)
        DocumentType.OTHER -> Icons.Default.InsertDriveFile to GenericFileColor
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentBottomSheet(
    onDismiss: () -> Unit,
    onPickDocument: () -> Unit,
    onPickGallery: () -> Unit,
    onCaptureCamera: () -> Unit,
    onDirectShareToOthers: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Share with others",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = WhatsAppDarkGreen,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                AttachmentOption(
                    title = "Document",
                    icon = Icons.Default.Description,
                    backgroundColor = Color(0xFF5F66CD),
                    onClick = {
                        onDismiss()
                        onPickDocument()
                    },
                    testTag = "attach_document_btn"
                )

                AttachmentOption(
                    title = "Gallery",
                    icon = Icons.Default.Image,
                    backgroundColor = Color(0xFFC32AA3),
                    onClick = {
                        onDismiss()
                        onPickGallery()
                    },
                    testTag = "attach_gallery_btn"
                )

                AttachmentOption(
                    title = "Camera",
                    icon = Icons.Default.CameraAlt,
                    backgroundColor = Color(0xFFE91E63),
                    onClick = {
                        onDismiss()
                        onCaptureCamera()
                    },
                    testTag = "attach_camera_btn"
                )

                AttachmentOption(
                    title = "Send to Others",
                    icon = Icons.Default.Share,
                    backgroundColor = Color(0xFF00A884),
                    onClick = {
                        onDismiss()
                        onDirectShareToOthers()
                    },
                    testTag = "attach_share_others_btn"
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun AttachmentOption(
    title: String,
    icon: ImageVector,
    backgroundColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .testTag(testTag)
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            fontSize = 12.sp,
            color = Color(0xFF41525D),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun FullScreenImageViewer(
    message: ChatMessage,
    onClose: () -> Unit,
    onShare: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // High-res Image
            AsyncImage(
                model = message.fileUri,
                contentDescription = "Full photo preview",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = message.fileName ?: "Photo",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "${FileUtils.formatDateHeader(message.timestamp)}, ${FileUtils.formatTime(message.timestamp)}",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }

                IconButton(
                    onClick = onShare,
                    modifier = Modifier.testTag("full_screen_share_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Send to Others",
                        tint = Color.White
                    )
                }
            }

            // Caption overlay at bottom
            if (message.text.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(16.dp)
                ) {
                    Text(
                        text = message.text,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
