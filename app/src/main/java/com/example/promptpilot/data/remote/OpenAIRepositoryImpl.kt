package com.example.promptpilot.data.remote

import android.util.Log
import com.example.promptpilot.data.api.BackendApi
import com.example.promptpilot.data.api.BackendChatRequest
import com.example.promptpilot.models.TextCompletionsParam
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject


class OpenAIRepositoryImpl @Inject constructor(
    private val backendApi: BackendApi,
) : OpenAIRepository {
    override fun textCompletionsWithStream(params: TextCompletionsParam): Flow<String> = flow {
        throw UnsupportedOperationException("Direct OpenAI streaming is deprecated. Use getAIResponseFromBackend instead.")
    }
    // New: Use backend server for AI response
    override suspend fun getAIResponseFromBackend(
        uid: String,
        prompt: String,
        model: String,
        chatId: String?,
        title: String,
        image_urls: List<String>?
    ): String {
        Log.d("PromptPilot", "getAIResponseFromBackend called with: $prompt, $model")
        return try {
            val backendRequest = BackendChatRequest(
                uid = uid,
                prompt = prompt,
                model = model,
                chat_id = chatId,
                title = title,
                image_urls = image_urls
            )
            val response = backendApi.getAIResponse(backendRequest)
            Log.d("PromptPilot", "AI reply received: ${response.reply}")
            response.reply
        } catch (e: Exception) {
            Log.e("PromptPilot", "Error getting AI response", e)
            // Log the error if needed
            // Return a special error string, or rethrow a custom exception
            "Network error: Unable to connect to server."
        }
    }
    
}