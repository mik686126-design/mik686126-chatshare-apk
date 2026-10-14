package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AuthScreen
import com.example.ui.ChatDetailScreen
import com.example.ui.ChatListScreen
import com.example.ui.ChatViewModel
import com.example.ui.FullScreenImageViewer
import com.example.ui.ProfileSetupDialog
import com.example.ui.Screen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppEntry()
            }
        }
    }
}

@Composable
fun MainAppEntry(viewModel: ChatViewModel = viewModel()) {
    val currentUser by viewModel.currentUser.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (currentUser == null) {
            AuthScreen(
                onAuthSuccess = {
                    // Reactive listener updates currentUser
                }
            )
        } else {
            ChatShareApp(viewModel)
        }
    }
}

@Composable
fun ChatShareApp(viewModel: ChatViewModel) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val contacts by viewModel.filteredContacts.collectAsState()
    val allMessages by viewModel.messages.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        when (val screen = uiState.currentScreen) {
            is Screen.ChatList -> {
                ChatListScreen(
                    viewModel = viewModel,
                    contacts = contacts,
                    uiState = uiState,
                    onContactClick = { contactId ->
                        viewModel.openChat(contactId)
                    },
                    onCameraQuickClick = {
                        // Open first active chat or Saved Messages to send camera photo
                        viewModel.openChat(contacts.firstOrNull()?.id ?: "c5")
                    }
                )
            }

            is Screen.ChatDetail -> {
                val activeContact = viewModel.repository.getContactById(screen.contactId)
                val chatMessages = allMessages.filter { it.chatId == screen.contactId }

                ChatDetailScreen(
                    contactId = screen.contactId,
                    viewModel = viewModel,
                    messages = chatMessages,
                    contact = activeContact,
                    onBack = {
                        viewModel.navigateBack()
                    }
                )
            }
        }

        // Full-screen photo viewer dialog
        uiState.selectedPreviewImage?.let { photoMsg ->
            FullScreenImageViewer(
                message = photoMsg,
                onClose = { viewModel.previewImage(null) },
                onShare = { viewModel.shareFile(context, photoMsg) }
            )
        }

        // Setup Mobile Number & Profile dialog
        if (uiState.showProfileSetupDialog) {
            ProfileSetupDialog(
                initialName = currentUser?.displayName ?: "",
                onDismiss = { viewModel.showProfileSetupDialog(false) },
                onSaveProfile = { name, phone, status ->
                    viewModel.saveUserProfile(name, phone, status)
                }
            )
        }
    }
}
