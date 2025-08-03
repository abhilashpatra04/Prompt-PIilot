package com.example.promptpilot.data.remote

import android.util.Log
import com.example.promptpilot.data.api.BackendApi
import com.example.promptpilot.data.api.BackendChatRequest
import com.example.promptpilot.models.TextCompletionsParam
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class OpenAIRepositoryImpl @Inject constructor(
    private val backendApi: BackendApi,
    @com.example.promptpilot.di.StreamingClient private val okHttpClient: OkHttpClient
) : OpenAIRepository {

    companion object {
        private const val TIMEOUT_SECONDS = 120L // 2 minutes timeout
        private const val BASE_URL = "http://10.196.75.76:8000" // Replace with your actual server URL
    }

    override fun textCompletionsWithStream(params: TextCompletionsParam): Flow<String> = flow {
        throw UnsupportedOperationException("Direct OpenAI streaming is deprecated. Use getStreamingAIResponse instead.")
    }

    // Regular non-streaming response
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
                image_urls = image_urls,
                stream = false // Explicitly set to false for non-streaming
            )

            val response = withTimeoutOrNull(TIMEOUT_SECONDS * 1000) {
                backendApi.getAIResponse(backendRequest)
            }

            if (response != null) {
                Log.d("PromptPilot", "AI reply received: ${response.reply}")
                response.reply
            } else {
                "Request timed out. Please try again with a shorter message."
            }
        } catch (e: Exception) {
            Log.e("PromptPilot", "Error getting AI response", e)
            when {
                e.message?.contains("timeout", ignoreCase = true) == true -> "Request timed out. Please try again."
                e.message?.contains("network", ignoreCase = true) == true -> "Network error. Please check your connection."
                else -> "Unable to connect to server. Please try again."
            }
        }
    }

    suspend fun getStreamingAIResponse(
        uid: String,
        prompt: String,
        model: String,
        chatId: String?,
        imageUrls: List<String>? = null,
        webSearch: Boolean = false,
        agentType: String? = null,
        onChunkReceived: (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        Log.d("PromptPilot", "getStreamingAIResponse called with: $prompt, webSearch: $webSearch, agent: $agentType")

        try {
            // Create JSON request body with proper null handling
            val requestBody = JSONObject().apply {
                put("uid", uid)
                put("prompt", prompt)
                put("model", model)
                put("chat_id", chatId ?: JSONObject.NULL)
                put("title", "Untitled")
                put("image_urls", if (!imageUrls.isNullOrEmpty()) {
                    org.json.JSONArray().apply {
                        imageUrls.forEach { put(it) }
                    }
                } else {
                    JSONObject.NULL
                })
                put("web_search", webSearch)
                put("agent_type", agentType ?: JSONObject.NULL)
                put("stream", true)
            }

            Log.d("PromptPilot", "Request body: ${requestBody.toString()}")

            // Create request with proper timeout configuration
            val request = Request.Builder()
                .url("$BASE_URL/chat")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "text/plain")
                .addHeader("Cache-Control", "no-cache")
                .build()

            // Create a client with longer timeouts for streaming
            val streamingClient = okHttpClient.newBuilder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            val response = streamingClient.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                Log.e("PromptPilot", "HTTP Error ${response.code}: $errorBody")
                val errorMessage = when (response.code) {
                    500 -> "Server error. Please try again."
                    503 -> "Service temporarily unavailable. Please try again later."
                    else -> "Connection error (${response.code}). Please try again."
                }
                throw Exception(errorMessage)
            }

            val responseBody = response.body ?: throw Exception("Empty response body")
            val reader = BufferedReader(InputStreamReader(responseBody.byteStream()))

            var fullResponse = ""
            var line: String?
            var lastUpdateTime = System.currentTimeMillis()

            try {
                while (reader.readLine().also { line = it } != null) {
                    val currentTime = System.currentTimeMillis()

                    // Check for timeout during streaming
                    if (currentTime - lastUpdateTime > TIMEOUT_SECONDS * 1000) {
                        Log.w("PromptPilot", "Streaming timeout - no data received for ${TIMEOUT_SECONDS} seconds")
                        break
                    }

                    if (line!!.startsWith("data: ")) {
                        val jsonStr = line!!.substring(6).trim() // Remove "data: " prefix

                        if (jsonStr == "[DONE]" || jsonStr.isEmpty()) {
                            Log.d("PromptPilot", "Streaming completed normally")
                            break
                        }

                        try {
                            val jsonData = JSONObject(jsonStr)

                            when {
                                jsonData.has("chunk") -> {
                                    val chunk = jsonData.getString("chunk")
                                    if (chunk.isNotEmpty()) {
                                        fullResponse += chunk
                                        onChunkReceived(fullResponse)
                                        lastUpdateTime = currentTime
                                    }
                                }
                                jsonData.has("done") && jsonData.getBoolean("done") -> {
                                    Log.d("PromptPilot", "Received done signal")
                                    break
                                }
                                jsonData.has("error") -> {
                                    val error = jsonData.getString("error")
                                    Log.e("PromptPilot", "Server error: $error")
                                    throw Exception("Server error: $error")
                                }
                            }
                        } catch (e: Exception) {
                            Log.w("PromptPilot", "Error parsing streaming response line: $jsonStr", e)
                            // Continue reading instead of failing completely
                            continue
                        }
                    }
                }
            } finally {
                try {
                    reader.close()
                    response.close()
                } catch (e: Exception) {
                    Log.w("PromptPilot", "Error closing resources", e)
                }
            }

            Log.d("PromptPilot", "Streaming completed. Full response length: ${fullResponse.length}")

            if (fullResponse.isEmpty()) {
                throw Exception("No response received from server")
            }

            fullResponse

        } catch (e: Exception) {
            Log.e("PromptPilot", "Error in streaming response", e)
            val errorMessage = when {
                e.message?.contains("timeout", ignoreCase = true) == true ->
                    "Response timed out. Please try with a shorter message."
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Network error. Please check your connection."
                e.message?.contains("server error", ignoreCase = true) == true ->
                    e.message ?: "Server error occurred."
                else -> "Connection error. Please try again."
            }

            onChunkReceived(errorMessage)
            errorMessage
        }
    }
}