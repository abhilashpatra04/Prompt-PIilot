package com.example.promptpilot.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.promptpilot.viewmodels.ConversationViewModel
import com.example.promptpilot.ui.theme.MainTheme
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import com.example.promptpilot.models.AI_Model

// The modern navigation drawer composable
@Composable
fun ModernAppDrawer(
    onChatSelected: (String) -> Unit, // Callback when a chat is selected
    onNewChat: () -> Unit,           // Callback when 'New Chat' is clicked
    onSettings: () -> Unit,          // Callback when 'Settings' is clicked
    conversationViewModel: ConversationViewModel = hiltViewModel() // Connects to the same ViewModel as the old UI
) {
    // Collect the list of conversations and the current conversation ID from the ViewModel
    val conversations by conversationViewModel.conversationsState.collectAsState()
    val currentConversationId by conversationViewModel.currentConversationState.collectAsState()
    val scope = rememberCoroutineScope()
    val modelOptions = AI_Model.entries
    val selectedModel by conversationViewModel.selectedModel.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    MainTheme {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .background(Color.DarkGray)
                .padding(vertical = 24.dp, horizontal = 12.dp)
                .padding(top = 24.dp)
        ) {
            // 'New Chat' button
            DrawerItem(
                icon = Icons.Filled.Add,
                label = "New Chat",
                selected = false,
                onClick = onNewChat,
            )
            Spacer(modifier = Modifier.height(16.dp))
            // List of previous conversations
            Text(
                text = "Previous Conversations",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyColumn(
                modifier = Modifier.weight(1f, fill = false)
            ) {
                items(conversations.size) { index ->
                    val conversation = conversations[index]
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .background(
                                if (conversation.id == currentConversationId) Color.Gray else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onChatSelected(conversation.id) }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        // Conversation title
                        Text(
                            text = conversation.title,
                            fontSize = 16.sp,
                            color = Color.White,
                            fontWeight = if (conversation.id == currentConversationId) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        // Delete button
                        IconButton(onClick = {
                            // Launch a coroutine to call the suspend function
                            scope.launch { // <--- Use the scope here
                                conversationViewModel.deleteConversationAndFiles(conversation.id)
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete Conversation",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            // 'Settings' button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
                    .padding(vertical = 12.dp, horizontal = 8.dp)
            ) {
                Text(
                    text = "Selected Model: ${selectedModel.name}",
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select Model",
                    tint = Color.White
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    modelOptions.forEach { model ->
                        DropdownMenuItem(
                            text = { Text(model.name) },
                            onClick = {
                                conversationViewModel.setSelectedModel(model)
                                expanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            DrawerItem(
                icon = Icons.Filled.Settings,
                label = "Settings",
                selected = false,
                onClick = onSettings
            )
        }
    }
}

// A single item in the drawer (icon + label)
@Composable
fun DrawerItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector?, // Optional icon
    label: String, // Item label
    selected: Boolean, // Whether this item is selected
    onClick: () -> Unit // Called when item is clicked
) {
    val backgroundColor = if (selected) Color.LightGray else Color.Transparent
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(backgroundColor, shape = RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(
            text = label,
            fontSize = 16.sp,
            color = Color.White,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
//// Preview Composable for ModernAppDrawer
//@Preview(showBackground = true, name = "Modern App Drawer Preview")
//@Composable
//fun ModernAppDrawerPreview() {
//    MainTheme { // Ensure the preview is wrapped in your theme
//        ModernAppDrawer(
//            onChatSelected = { chatId -> println("Chat selected: $chatId") },
//            onNewChat = { println("New chat clicked") },
//            onSettings = { println("Settings clicked") },
//        )
//    }
//}

//@Preview(showBackground = true, name = "Drawer Item Preview")
//@Composable
//fun DrawerItemPreview() {
//    ModernAppDrawer(
//        onChatSelected = { chatId -> println("Chat selected: $chatId") },
//        onNewChat = { println("New chat clicked") },
//        onSettings = { println("Settings clicked") },
////            conversationViewModel = // Pass null, ModernAppDrawer will use dummy data
//    )
//}