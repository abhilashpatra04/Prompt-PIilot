package com.example.promptpilot.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.Image
import coil.compose.rememberAsyncImagePainter
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.promptpilot.models.AttachmentType
import com.example.promptpilot.models.ChatAttachment
import com.example.promptpilot.models.MessageModel
import com.example.promptpilot.ui.theme.MainTheme
import com.example.promptpilot.viewmodels.ConversationViewModel
import kotlinx.coroutines.launch

// The main modern chat screen composable
@Composable
fun ModernConversation(
    conversationViewModel: ConversationViewModel = hiltViewModel() // Connects to the same ViewModel as the old UI
) {
    // Collect the current conversation ID and messages from the ViewModel

    val conversationId by conversationViewModel.currentConversationState.collectAsState()
    val messagesMap by conversationViewModel.messagesState.collectAsState()
    val messages: List<MessageModel> = messagesMap[conversationId] ?: emptyList()
    val (zoomedAttachment, setZoomedAttachment) = remember { mutableStateOf<ChatAttachment?>(null) }

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
            ) {
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
                    ModernMessageList(
                        messages = messages,
                        listState = listState,
                        onAttachmentClick = { setZoomedAttachment(it) }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                // The modern input box at the bottom
                val coroutineScope = rememberCoroutineScope()
                val selectedModel by conversationViewModel.selectedModel.collectAsState()
                val supportsImage = selectedModel.model.startsWith("gemini")
                val context = LocalContext.current
                Box(
                    modifier = Modifier.imePadding() // <-- ADD THIS
                ) {
                    ModernTextInput(
                        onSend = { prompt, imageUrls ->
                            coroutineScope.launch {
                                conversationViewModel.sendMessage(prompt, imageUrls,context)
                            }
                        },
                        supportsImage = supportsImage
                    )
                }
            }
        }
    }
    if (zoomedAttachment != null) {
        AlertDialog(
            onDismissRequest = { setZoomedAttachment(null) },
            confirmButton = {},
            dismissButton = {},
            title = { Text(zoomedAttachment.name) },
            text = {
                if (zoomedAttachment.type == AttachmentType.IMAGE) {
                    Image(
                        painter = rememberAsyncImagePainter(zoomedAttachment.url),
                        contentDescription = null,
                        modifier = Modifier.size(300.dp)
                    )
                } else {
                    // For PDFs, just show a big icon (or implement a PDF preview if you want)
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(100.dp),
                        tint = Color.Red
                    )
                }
            }
        )
    }
}

// A modern, beautiful message list with rounded bubbles
@Composable
fun ModernMessageList(messages: List<MessageModel>, listState: LazyListState,onAttachmentClick: (ChatAttachment) -> Unit) {
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
                ModernMessageBubble(
                    message = message,
                    isUser = true,
                    onAttachmentClick= onAttachmentClick
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            if (message.answer.isNotBlank()) {
                ModernMessageBubble(
                    message = message,
                    isUser = false,
                    onAttachmentClick= onAttachmentClick
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

// A modern chat bubble with soft colors and rounded corners
//@Composable
//fun ModernMessageBubble(
//    text: String,
//    isUser: Boolean,
//    imageUrl: String? = null,
//    onAttachmentClick: () -> Unit = { /* We'll add zoom logic next */ },
//    modelName: String? = null)
//{
//    // This composable displays a single chat bubble, styled for user or AI
//    Row(
//        modifier = Modifier.fillMaxWidth(),
//        horizontalArrangement = Arrangement.End // Always left-aligned for both user and AI
//    ) {
//        if (isUser) {
//            // User message: left-aligned, max width, colored bubble
//            Surface(
//                color = Color.Gray,
//                shape = RoundedCornerShape(16.dp),
//                shadowElevation = 2.dp,
//                tonalElevation = 2.dp,
//                modifier = Modifier.widthIn(max = 320.dp)
//            ) {
//                Text(
//                    text = text,
//                    color = Color.Black,
//                    fontSize = 16.sp,
//                    fontWeight = FontWeight.Medium,
//                    modifier = Modifier.padding(12.dp)
//                )
//            }
//        } else {
//            // AI message: fill width, with padding, neutral color
//            Surface(
//                color = Color.Transparent,
//                modifier = Modifier
//                    .fillMaxWidth()
//
//            ) {
//                Text(
//                    text = text,
//                    color = Color.White,
//                    fontSize = 16.sp,
//                    fontWeight = FontWeight.Normal,
//                    modifier = Modifier.padding(8.dp)
//                )
//            }
//        }
//        if (!isUser && modelName != null) {
//            Text(
//                text = "generated by \"$modelName\"",
//                color = Color.Gray,
//                fontSize = 12.sp,
//                modifier = Modifier.padding(start = 8.dp, top = 2.dp)
//            )
//        }
//    }
//}
@Composable
fun ModernMessageBubble(
    message: MessageModel,
    isUser: Boolean,
//    imageUrl: String? = null,
    onAttachmentClick: (ChatAttachment) -> Unit,
//    modelName: String? = null
) {
    Column {
        // Show attachments if any
        if (message.attachments.isNotEmpty() and isUser) {
            LazyRow(horizontalArrangement = Arrangement.End) {
                items(message.attachments) { attachment ->
                    Box(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.DarkGray)
                            .border(1.dp, Color.White, RoundedCornerShape(16.dp))
                            .clickable { onAttachmentClick(attachment) }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (attachment.type == AttachmentType.IMAGE) Icons.Default.Image else Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "@${attachment.name}",
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
        // Existing bubble code for text
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            if (isUser) {
                Surface(
                    color = Color.Gray,
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 2.dp,
                    tonalElevation = 2.dp,
                    modifier = Modifier.widthIn(max = 320.dp)
                ) {
                    Text(
                        text = message.question,
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                Surface(
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = message.answer,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
            // Optionally show model name for AI
            if (!isUser && message.model != null) {
                Text(
                    text = "generated by \"${message.model}\"",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                )
            }
        }
    }
}