package com.example.promptpilot.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.promptpilot.viewmodels.ConversationViewModel
import kotlinx.coroutines.launch

// The main modern scaffold composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernScaffold(
    conversationViewModel: ConversationViewModel = hiltViewModel() // Connects to the same ViewModel as the old UI
) {
    // Ensure conversations are loaded from persistent storage on app start
    LaunchedEffect(Unit) {
        conversationViewModel.initialize()
    }
    // State for controlling the navigation drawer (open/close)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // ModalNavigationDrawer is the Material 3 way to show a side drawer
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(Color.Black)
    ) {
        ModernAppBar(
            onMenuClick = { scope.launch { drawerState.open() } }
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 1.dp))

        Box(modifier = Modifier.weight(1f))
            {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    // The modern navigation drawer
                    Box(modifier = Modifier.padding(end = 40.dp)
                        .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))){
                        ModernAppDrawer(
                            onChatSelected = { conversationId ->
                                // Find the conversation by ID and switch to it
                                val conversation = conversationViewModel.conversationsState.value.find { it.id == conversationId }
                                if (conversation != null) {
                                    scope.launch { conversationViewModel.onConversation(conversation) }
                                    // Optionally close the drawer after switching
                                    scope.launch { drawerState.close() }
                                }
                            },
                            onNewChat = {
                                scope.launch { conversationViewModel.newConversation() }
                                // Optionally close the drawer after starting new chat
                                scope.launch { drawerState.close() }
                            },
                            onSettings = { /* TODO: Settings logic */ },
                            conversationViewModel = conversationViewModel
                        )
                    }
                },
                content = {
                    // The main content area
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .systemBarsPadding(),
                        color = Color.Black
                    ) {
                        ModernConversation(
                            conversationViewModel = conversationViewModel
                        )
                    }
                }
            )
        }
    }
}

// Preview for ModernScaffold (for easy testing in Android Studio)
@Composable
fun ModernScaffoldPreview() {
    ModernScaffold()
} 