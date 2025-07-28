package com.example.promptpilot.screens

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.promptpilot.data.remote.ChatImage
import com.example.promptpilot.helpers.uploadImageToCloudinary
import com.example.promptpilot.models.AttachmentType
import com.example.promptpilot.models.ChatAttachment
import com.example.promptpilot.viewmodels.ConversationViewModel
import kotlinx.coroutines.launch

// The modern input box composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernTextInput(
    onSend: (String, List<ChatAttachment>) -> Unit,
    supportsImage: Boolean,
    conversationViewModel: ConversationViewModel = hiltViewModel()
) {
    val pendingAttachments by conversationViewModel.pendingAttachments.collectAsState()
        var text by remember { mutableStateOf(TextFieldValue("")) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
        val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            scope.launch {
                val url = uploadImageToCloudinary(context, uri)
                if (url != null) {
                    conversationViewModel.addAttachment(ChatAttachment(
                        name = uri.lastPathSegment ?: "image",
                        url = url,
                        type = AttachmentType.IMAGE
                    ))
                }
            }
        }
    }
    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            scope.launch {
                conversationViewModel.addAttachment(ChatAttachment(
                    name = uri.lastPathSegment ?: "pdf",
                    url = uri.toString(),
                    type = AttachmentType.PDF
                ))
            }
        }
    }
    var expanded by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topEnd = 16.dp, topStart = 16.dp))
                .background(Color.DarkGray)
            .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                .clip(RoundedCornerShape(topEnd = 16.dp, topStart = 16.dp))
                .background(Color.DarkGray)
        ) {
            // 1. Image chips row (if any images)
            AttachmentChipsRow(
                attachments = pendingAttachments,
                onRemove = { conversationViewModel.removeAttachment(it) },
                onClick = { /* set zoomedAttachment = it, see Step 4 */ }
            )

            // 2. Text input row
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Type your message...", fontSize = 14.sp, color = Color.White) },
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color.White,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                    minLines = 1,
                    maxLines = 6,
                    singleLine = false,
                )

            // 3. Bottom row: image picker and send button
                Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (supportsImage) {
                        IconButton(onClick = { launcher.launch("image/*") }) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Add Image",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = { pdfLauncher.launch("application/pdf") }) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "Add PDF",
                                tint = Color.White
                            )
                        }

                    } else {
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Add Image",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "Add PDF",
                                tint = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = {
                        if (text.text.isNotEmpty() || pendingAttachments.any { it.type == AttachmentType.IMAGE || it.type == AttachmentType.PDF }) {
                                scope.launch {
                                    try {
                                    val contextAttachments = pendingAttachments.filter { it.type == AttachmentType.IMAGE || it.type == AttachmentType.PDF }
                                    Log.d("PromptPilot", "Sending attachments: ${contextAttachments.map { it.url }}")
                                    conversationViewModel.sendMessage(text.text.trim(), pendingAttachments, context)
                                    conversationViewModel.clearPendingAttachments()
                                        text = TextFieldValue("")
                                    } catch (e: Exception) {
                                        Log.e("AttachmentUpload", "Error uploading attachment or sending message", e)
                                    }
                                }
                            }
                        }
                    ) {
                        if (text.text.isEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Input",
                                tint = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = Color.White,
                                modifier = Modifier
                                    .background(
                                        color = Color.Transparent,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(6.dp)
                            )
                        }
                    }
                }
            }
        }
}

@Composable
fun ImageContextChips(
    images: List<ChatImage>,
    onToggleContext: (ChatImage) -> Unit,
    expanded: Boolean,
    onExpandToggle: () -> Unit
) {
    val maxVisible = 1 // Increase if you want to show more chips by default
    val visibleImages = if (expanded) images else images.take(maxVisible)
    var images by remember { mutableStateOf(listOf<ChatImage>()) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        images.forEach { image ->
            Box(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (image.useAsContext) Color.DarkGray else Color.Black)
                    .border(1.dp, Color.White, RoundedCornerShape(16.dp))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "@${image.name}",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 14.sp
                    )
                    IconButton(
                        onClick = {
                            images = images.filter { it != image }
                        },
                        modifier = Modifier.size(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove Image",
                            tint = Color.Red,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun AttachmentChipsRow(
    attachments: List<ChatAttachment>,
    onRemove: (ChatAttachment) -> Unit,
    onClick: (ChatAttachment) -> Unit
) {
    LazyRow {
        items(attachments) { attachment ->
            Box(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.DarkGray)
                    .border(1.dp, Color.White, RoundedCornerShape(16.dp))
                    .clickable { onClick(attachment) }
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
                    IconButton(
                        onClick = { onRemove(attachment) },
                        modifier = Modifier.size(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove",
                            tint = Color.Red,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}