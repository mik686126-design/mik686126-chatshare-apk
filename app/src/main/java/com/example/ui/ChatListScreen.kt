package com.example.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Contact
import com.example.model.MessageType
import com.example.ui.theme.WhatsAppDarkGreen
import com.example.ui.theme.WhatsAppLightGreen
import com.example.util.FileUtils

@Composable
fun ChatListScreen(
    viewModel: ChatViewModel,
    contacts: List<Contact>,
    uiState: UiState,
    onContactClick: (String) -> Unit,
    onCameraQuickClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // WhatsApp Iconic Top Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(WhatsAppDarkGreen)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            if (uiState.isSearchOpen) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search chats, docs, photos…", color = Color.White.copy(alpha = 0.7f)) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("search_input_field")
                    )

                    IconButton(onClick = { viewModel.toggleSearch(false) }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Close search",
                            tint = Color.White
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ChatShare",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = onCameraQuickClick,
                        modifier = Modifier.testTag("top_camera_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Camera",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = { viewModel.toggleSearch(true) },
                        modifier = Modifier.testTag("top_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White
                        )
                    }

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = Color.White
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("New Chat") },
                                onClick = {
                                    showMenu = false
                                    viewModel.showNewChatDialog(true)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Docs & Media Vault") },
                                onClick = {
                                    showMenu = false
                                    viewModel.setSelectedTab(1)
                                }
                            )
                        }
                    }
                }
            }
        }

        // WhatsApp Style Tabs
        TabRow(
            selectedTabIndex = uiState.selectedTab,
            containerColor = WhatsAppDarkGreen,
            contentColor = Color.White,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTab]),
                    color = Color.White,
                    height = 3.dp
                )
            }
        ) {
            Tab(
                selected = uiState.selectedTab == 0,
                onClick = { viewModel.setSelectedTab(0) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "CHATS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (uiState.selectedTab == 0) Color.White else Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${contacts.size}",
                                fontSize = 11.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                modifier = Modifier.testTag("tab_chats")
            )

            Tab(
                selected = uiState.selectedTab == 1,
                onClick = { viewModel.setSelectedTab(1) },
                text = {
                    Text(
                        text = "DOCS & MEDIA",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (uiState.selectedTab == 1) Color.White else Color.White.copy(alpha = 0.7f)
                    )
                },
                modifier = Modifier.testTag("tab_media")
            )

            Tab(
                selected = uiState.selectedTab == 2,
                onClick = { viewModel.setSelectedTab(2) },
                text = {
                    Text(
                        text = "STATUS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (uiState.selectedTab == 2) Color.White else Color.White.copy(alpha = 0.7f)
                    )
                },
                modifier = Modifier.testTag("tab_status")
            )
        }

        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (uiState.selectedTab) {
                0 -> {
                    ChatsListContent(
                        contacts = contacts,
                        viewModel = viewModel,
                        onContactClick = onContactClick
                    )
                }
                1 -> {
                    DocsMediaScreen(
                        viewModel = viewModel,
                        onOpenChat = onContactClick
                    )
                }
                2 -> {
                    StatusScreen(
                        contacts = contacts,
                        onContactClick = onContactClick
                    )
                }
            }

            // WhatsApp Floating Action Button
            if (uiState.selectedTab == 0) {
                FloatingActionButton(
                    onClick = { viewModel.showNewChatDialog(true) },
                    containerColor = WhatsAppLightGreen,
                    contentColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(20.dp)
                        .testTag("new_chat_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Chat",
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }

    // New Chat Dialog
    if (uiState.showNewChatDialog) {
        NewChatDialog(
            onDismiss = { viewModel.showNewChatDialog(false) },
            onConfirm = { name, phone ->
                viewModel.createNewContact(name, phone)
                viewModel.showNewChatDialog(false)
            }
        )
    }
}

@Composable
fun ChatsListContent(
    contacts: List<Contact>,
    viewModel: ChatViewModel,
    onContactClick: (String) -> Unit
) {
    if (contacts.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No chats found",
                color = Color.Gray,
                fontSize = 16.sp
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(contacts, key = { it.id }) { contact ->
                val lastMessage = viewModel.repository.getLastMessage(contact.id)

                ChatItemRow(
                    contact = contact,
                    lastMessage = lastMessage,
                    onClick = { onContactClick(contact.id) }
                )
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = Color(0xFFE9EDEF),
                    modifier = Modifier.padding(start = 72.dp)
                )
            }
        }
    }
}

@Composable
fun ChatItemRow(
    contact: Contact,
    lastMessage: com.example.model.ChatMessage?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("chat_item_${contact.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ContactAvatar(
            name = contact.name,
            avatarColor = contact.avatarColor,
            sizeDp = 50,
            isOnline = contact.isOnline
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = contact.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = Color(0xFF111B21),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (lastMessage != null) {
                    Text(
                        text = FileUtils.formatTime(lastMessage.timestamp),
                        fontSize = 12.sp,
                        color = if (contact.unreadCount > 0) WhatsAppLightGreen else Color(0xFF667781)
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (lastMessage != null) {
                    if (lastMessage.isFromMe) {
                        StatusTicks(
                            status = lastMessage.status,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }

                    // Icon for photo or document
                    when (lastMessage.type) {
                        MessageType.IMAGE -> {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Photo",
                                tint = Color(0xFF667781),
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(end = 4.dp)
                            )
                        }
                        MessageType.DOCUMENT -> {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = "Document",
                                tint = Color(0xFF667781),
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(end = 4.dp)
                            )
                        }
                        else -> Unit
                    }

                    val previewText = when (lastMessage.type) {
                        MessageType.IMAGE -> if (lastMessage.text.isNotBlank()) lastMessage.text else "Photo"
                        MessageType.DOCUMENT -> lastMessage.fileName ?: "Document"
                        MessageType.TEXT -> lastMessage.text
                    }

                    Text(
                        text = previewText,
                        fontSize = 14.sp,
                        color = Color(0xFF667781),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Text(
                        text = contact.statusText,
                        fontSize = 14.sp,
                        color = Color(0xFF667781),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (contact.isPinned) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        tint = Color(0xFF8696A0),
                        modifier = Modifier
                            .size(16.dp)
                            .padding(start = 6.dp)
                    )
                }

                if (contact.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .clip(CircleShape)
                            .background(WhatsAppLightGreen)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${contact.unreadCount}",
                            fontSize = 11.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NewChatDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "New Contact / Chat",
                fontWeight = FontWeight.Bold,
                color = WhatsAppDarkGreen
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Contact Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("contact_name_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number or Group Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("contact_phone_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, phone.ifBlank { "+92 300 0000000" })
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppDarkGreen),
                modifier = Modifier.testTag("start_chat_btn")
            ) {
                Text("Start Chat")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}
