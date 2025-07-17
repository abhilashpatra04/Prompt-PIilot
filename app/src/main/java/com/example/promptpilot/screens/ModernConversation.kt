package com.example.promptpilot.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.promptpilot.models.MessageModel
import com.example.promptpilot.ui.theme.MainTheme
import com.example.promptpilot.viewmodels.ConversationViewModel
import kotlinx.coroutines.launch

// The main modern chat screen composable
@Composable
fun ModernConversation(
    conversationViewModel: ConversationViewModel = hiltViewModel(), // Connects to the same ViewModel as the old UI
    onMenuClick: () -> Unit = {}
) {
    // Collect the current conversation ID and messages from the ViewModel
    val conversationId by conversationViewModel.currentConversationState.collectAsState()
    val messagesMap by conversationViewModel.messagesState.collectAsState()
    val messages: List<MessageModel> = messagesMap[conversationId] ?: emptyList()

    // State for scrolling the message list
    val listState = rememberLazyListState()

    // Use the app's theme for consistent look
    MainTheme {
        Surface(
            modifier = Modifier.fillMaxSize(), // Fill the whole screen
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
//                    .padding(8.dp) // Add some padding around the edges
            ) {
                // Modern app bar at the top
//                ModernAppBar(onMenuClick = onMenuClick)
//                Spacer(modifier = Modifier.height(8.dp))
                // The message list, takes up all available space except for the input box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(
                            color = Color.Transparent,
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(8.dp)
                ) {
                    ModernMessageList(messages = messages, listState = listState)
                }
                Spacer(modifier = Modifier.height(8.dp))
                // The modern input box at the bottom
                val coroutineScope = rememberCoroutineScope()
                Box(
                    modifier = Modifier.imePadding() // <-- ADD THIS
                ) {
                    ModernTextInput(
                        onSend = { text ->
                            coroutineScope.launch {
                                conversationViewModel.sendMessage(text)
                            }
                        }
                    )
                }
            }
        }
    }
}

// A modern, beautiful message list with rounded bubbles
@Composable
fun ModernMessageList(messages: List<MessageModel>, listState: LazyListState) {
    // Sort messages by their timestamp (oldest to newest)
    val sortedMessages = messages.sortedBy { it.createdAt }
    LazyColumn(
        state = listState,
        reverseLayout = false, // Newest at the bottom
        modifier = Modifier.fillMaxSize()
    ) {
        items(sortedMessages.size) { index ->
            val message = sortedMessages[index]
            // Show user message bubble if question is not blank
            if (message.question.isNotBlank()) {
                ModernMessageBubble(text = message.question, isUser = true)
                Spacer(modifier = Modifier.height(4.dp))
            }
            // Show AI message bubble if answer is not blank
            if (message.answer.isNotBlank()) {
                ModernMessageBubble(text = message.answer, isUser = false)
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

// A modern chat bubble with soft colors and rounded corners
@Composable
fun ModernMessageBubble(text: String, isUser: Boolean) {
    // This composable displays a single chat bubble, styled for user or AI
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End // Always left-aligned for both user and AI
    ) {
        if (isUser) {
            // User message: left-aligned, max width, colored bubble
            Surface(
                color = Color.Gray,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 2.dp,
                tonalElevation = 2.dp,
                modifier = Modifier.widthIn(max = 320.dp)
            ) {
                Text(
                    text = text,
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(12.dp)
                )
            }
        } else {
            // AI message: fill width, with padding, neutral color
            Surface(
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()

            ) {
                Text(
                    text = text,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}
//@Preview(name = "ModernConversation Light", showBackground = true)
//@Preview(name = "ModernConversation Dark", showBackground = true)
//@Composable
//fun ModernConversationPreview() {
//    // For the preview, we can't use hiltViewModel().
//    // We'll create a dummy ViewModel or pass empty data.
//    // Since ModernConversation directly uses the ViewModel to get messages,
//    // providing a simple preview without a fake ViewModel is tricky.
//    // Let's create a version that takes messages directly for preview purposes.
//    MainTheme {
//        Surface(
//            modifier = Modifier.fillMaxSize(),
//            color = Color.Black
//        ) {
//            Column(
//                modifier = Modifier
//                    .fillMaxSize()
//            ) {
//                ModernAppBar(onMenuClick = {})
//                HorizontalDivider(modifier = Modifier.padding(vertical = 1.dp))
//                Spacer(modifier = Modifier.height(8.dp))
//                Box(
//                    modifier = Modifier
//                        .weight(1f)
//                        .fillMaxWidth()
//                        .background(
//                            color = Color.Transparent,
//                            shape = RoundedCornerShape(24.dp)
//                        )
//                        .padding(8.dp)
//                ) {
//                    ModernMessageList(
//                        messages = listOf(
//                            MessageModel(question = "Hello AI!", answer = ""),
//                            MessageModel(question = "", answer = "Hello User! How can I help you today?"),
//                            MessageModel(question = "Tell me a joke.", answer = ""),
//                            MessageModel(question = "", answer = "Why don't scientists trust atoms? Because they make up everything!")
//                        ),
//                        listState = rememberLazyListState()
//                    )
//                }
//                Spacer(modifier = Modifier.height(8.dp))
//                ModernTextInput(onSend = {})
//            }
//        }
//    }
//}
//
