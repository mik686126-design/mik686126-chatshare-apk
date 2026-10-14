package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
                        placeholder = { Text("Search chats, numbers, docs…", color = Color.White.copy(alpha = 0.7f)) },
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
                                leadingIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null, tint = WhatsAppDarkGreen) },
                                text = { Text("New Contact / Chat") },
                                onClick = {
                                    showMenu = false
                                    viewModel.showNewChatDialog(true)
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = WhatsAppDarkGreen) },
                                text = { Text("Docs & Media Vault") },
                                onClick = {
                                    showMenu = false
                                    viewModel.setSelectedTab(1)
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null, tint = WhatsAppDarkGreen) },
                                text = { Text("My Profile / Number") },
                                onClick = {
                                    showMenu = false
                                    viewModel.showProfileSetupDialog(true)
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color.Red) },
                                text = { Text("Sign Out", color = Color.Red) },
                                onClick = {
                                    showMenu = false
                                    viewModel.signOut()
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
                        onContactClick = onContactClick,
                        onDeleteContact = { contact ->
                            viewModel.setContactPendingDelete(contact)
                        }
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

            // WhatsApp Floating Action Button (New Chat)
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
                        contentDescription = "New Contact / Chat",
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }

    // New Contact Dialog (Saves permanently to phone storage)
    if (uiState.showNewChatDialog) {
        NewChatDialog(
            onDismiss = { viewModel.showNewChatDialog(false) },
            onConfirm = { name, phone, status ->
                viewModel.createNewContact(name, phone, status)
                viewModel.showNewChatDialog(false)
            }
        )
    }

    // Delete Contact Confirmation Dialog
    uiState.contactPendingDelete?.let { contact ->
        AlertDialog(
            onDismissRequest = { viewModel.setContactPendingDelete(null) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.Red,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text("Delete Chat with ${contact.name}?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "This will permanently remove this contact and delete all its chat history, pictures, and documents from your device memory.",
                    color = Color(0xFF54656F),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteContact(contact.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Delete Chat", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setContactPendingDelete(null) }) {
                    Text("Cancel", color = Color(0xFF54656F))
                }
            }
        )
    }
}

@Composable
fun ChatsListContent(
    contacts: List<Contact>,
    viewModel: ChatViewModel,
    onContactClick: (String) -> Unit,
    onDeleteContact: (Contact) -> Unit
) {
    if (contacts.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "No saved contacts found",
                    color = Color.Gray,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap the + button below to add a new contact or phone number.",
                    color = Color(0xFF8696A0),
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
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
                    onClick = { onContactClick(contact.id) },
                    onDelete = { onDeleteContact(contact) }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatItemRow(
    contact: Contact,
    lastMessage: com.example.model.ChatMessage?,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showItemMenu by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { onClick() },
                    onLongClick = { showItemMenu = true }
                )
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
                            text = contact.phoneNumber,
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

                    // 3-dot options button for easy delete on tap as well
                    IconButton(
                        onClick = { showItemMenu = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Contact options",
                            tint = Color(0xFF8696A0),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        DropdownMenu(
            expanded = showItemMenu,
            onDismissRequest = { showItemMenu = false }
        ) {
            DropdownMenuItem(
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = WhatsAppDarkGreen) },
                text = { Text(contact.phoneNumber) },
                onClick = { showItemMenu = false }
            )
            DropdownMenuItem(
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                text = { Text("Delete Chat & Contact", color = Color.Red) },
                onClick = {
                    showItemMenu = false
                    onDelete()
                }
            )
        }
    }
}

@Composable
fun NewChatDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, status: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = WhatsAppDarkGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Save New Contact",
                    fontWeight = FontWeight.Bold,
                    color = WhatsAppDarkGreen
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "This contact will be permanently saved in your phone memory.",
                    color = Color(0xFF54656F),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    label = { Text("Contact Name *") },
                    isError = nameError,
                    supportingText = { if (nameError) Text("Name cannot be empty") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("contact_name_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number (e.g. +92 300 1234567)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("contact_phone_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = statusText,
                    onValueChange = { statusText = it },
                    label = { Text("About / Status (optional)") },
                    placeholder = { Text("Hey there! I am using ChatShare.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                    } else {
                        onConfirm(
                            name.trim(),
                            phone.ifBlank { "+92 300 0000000" },
                            statusText.ifBlank { "Hey there! I am using ChatShare." }
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppDarkGreen),
                modifier = Modifier.testTag("start_chat_btn")
            ) {
                Text("Save & Chat", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}
