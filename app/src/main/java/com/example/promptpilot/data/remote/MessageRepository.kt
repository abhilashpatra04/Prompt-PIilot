package com.example.promptpilot.data.remote

import com.example.promptpilot.models.MessageModel
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun fetchMessages(conversationId: String): Flow<List<MessageModel>>
    fun createMessage(message: MessageModel): MessageModel
    fun deleteMessage()
    suspend fun deleteMessagesByConversation(conversationId: String)
    
}