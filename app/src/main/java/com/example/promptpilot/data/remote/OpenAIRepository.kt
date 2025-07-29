package com.example.promptpilot.data.remote

import com.example.promptpilot.models.TextCompletionsParam
import kotlinx.coroutines.flow.Flow

interface OpenAIRepository {
    fun textCompletionsWithStream(params: TextCompletionsParam): Flow<String>
    suspend fun getAIResponseFromBackend(
        uid: String,
        prompt: String,
        model: String,
        chatId: String? = null,
        title: String = "Untitled",
        image_urls: List<String>? = null
    ): String

}