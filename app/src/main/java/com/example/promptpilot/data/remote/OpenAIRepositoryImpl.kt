package com.example.promptpilot.data.remote

import android.util.Log
import com.example.promptpilot.data.api.BackendApi
import com.example.promptpilot.data.api.BackendChatRequest
import com.example.promptpilot.models.TextCompletionsParam
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

class OpenAIRepositoryImpl @Inject constructor(
    private val backendApi: BackendApi,
    @com.example.promptpilot.di.StreamingClient private val okHttpClient: OkHttpClient
) : OpenAIRepository {

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
                image_urls = image_urls
            )
            val response = backendApi.getAIResponse(backendRequest)
            Log.d("PromptPilot", "AI reply received: ${response.reply}")
            response.reply
        } catch (e: Exception) {
            Log.e("PromptPilot", "Error getting AI response", e)
            "Network error: Unable to connect to server."
        }
    }

    // Enhanced streaming response with web search and agent support
    suspend fun getStreamingAIResponse(
        uid: String,
        prompt: String,
        model: String,
        chatId: String?,
        imageUrls: List<String>? = null,
        webSearch: Boolean = false,
        agentType: String? = null,
        onChunkReceived: (String) -> Unit
    ): String {
        Log.d("PromptPilot", "getStreamingAIResponse called with: $prompt, webSearch: $webSearch, agent: $agentType")

        return try {
            val requestBody = JSONObject().apply {
                put("uid", uid)
                put("prompt", prompt)
                put("model", model)
                put("chat_id", chatId)
                put("title", "Untitled")
                put("image_urls", imageUrls)
                put("web_search", webSearch)
                put("agent_type", agentType)
                put("stream", true)
            }

            val request = Request.Builder()
                .url("http://10.54.138.76:8000/chat") // Replace with your backend URL
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}: ${response.message}")
            }

            val responseBody = response.body
            val reader = BufferedReader(InputStreamReader(responseBody.byteStream()))

            var fullResponse = ""
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                if (line!!.startsWith("data: ")) {
                    val jsonStr = line.substring(6) // Remove "data: " prefix
                    try {
                        val jsonData = JSONObject(jsonStr)

                        if (jsonData.has("chunk")) {
                            val chunk = jsonData.getString("chunk")
                            fullResponse += chunk
                            onChunkReceived(fullResponse) // Update UI with accumulated text
                        } else if (jsonData.has("done") && jsonData.getBoolean("done")) {
                            break
                        } else if (jsonData.has("error")) {
                            throw Exception(jsonData.getString("error"))
                        }
                    } catch (e: Exception) {
                        Log.e("PromptPilot", "Error parsing streaming response: ${e.message}")
                        continue
                    }
                }
            }

            reader.close()
            response.close()

            Log.d("PromptPilot", "Streaming completed. Full response length: ${fullResponse.length}")
            fullResponse

        } catch (e: Exception) {
            Log.e("PromptPilot", "Error in streaming response", e)
            onChunkReceived("Sorry, I encountered an error. Please try again.")
            "Network error: Unable to connect to server."
        }
    }
}
//package com.example.promptpilot.data.remote
//
//import android.util.Log
//import com.example.promptpilot.data.api.BackendApi
//import com.example.promptpilot.data.api.BackendChatRequest
//import com.example.promptpilot.models.TextCompletionsParam
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.flow
//import javax.inject.Inject
//
//
//class OpenAIRepositoryImpl @Inject constructor(
//    private val backendApi: BackendApi,
//) : OpenAIRepository {
//    override fun textCompletionsWithStream(params: TextCompletionsParam): Flow<String> = flow {
//        throw UnsupportedOperationException("Direct OpenAI streaming is deprecated. Use getAIResponseFromBackend instead.")
//    }
//    // New: Use backend server for AI response
//    override suspend fun getAIResponseFromBackend(
//        uid: String,
//        prompt: String,
//        model: String,
//        chatId: String?,
//        title: String,
//        image_urls: List<String>?
//    ): String {
//        Log.d("PromptPilot", "getAIResponseFromBackend called with: $prompt, $model")
//        return try {
//            val backendRequest = BackendChatRequest(
//                uid = uid,
//                prompt = prompt,
//                model = model,
//                chat_id = chatId,
//                title = title,
//                image_urls = image_urls
//            )
//            val response = backendApi.getAIResponse(backendRequest)
//            Log.d("PromptPilot", "AI reply received: ${response.reply}")
//            response.reply
//        } catch (e: Exception) {
//            Log.e("PromptPilot", "Error getting AI response", e)
//            // Log the error if needed
//            // Return a special error string, or rethrow a custom exception
//            "Network error: Unable to connect to server."
//        }
//    }
//
//}