package com.example.promptpilot.data.remote

import com.example.promptpilot.models.TextCompletionsParam
import kotlinx.coroutines.flow.Flow

interface OpenAIRepository {
    fun textCompletionsWithStream(params: TextCompletionsParam): Flow<String>

    suspend fun getAIResponseFromBackend(
        uid: String,
        prompt: String,
        model: String,
        chatId: String?,
        title: String,
        image_urls: List<String>?
    ): String

    suspend fun getStreamingAIResponse(
        uid: String,
        prompt: String,
        model: String,
        chatId: String?,
        imageUrls: List<String>? = null,
        webSearch: Boolean = false,
        agentType: String? = null,
        onChunkReceived: (String) -> Unit
    ): String
}