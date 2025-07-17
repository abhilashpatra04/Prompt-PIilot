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
//    override fun textCompletionsWithStream(params: TextCompletionsParam): Flow<String> =
//        callbackFlow {
//            withContext(Dispatchers.IO) {
//                val response =
//                    (if (params.isChatCompletions) backendApi.getAIResponse(
//                        params.toJson()
//                    ) else backendApi.getAIResponse(params.toJson())).execute()
//
//                if (response.isSuccessful) {
//                    val input = response.body()?.byteStream()?.bufferedReader() ?: throw Exception()
//                    try {
//                        while (true) {
//                            val line = withContext(Dispatchers.IO) {
//                                input.readLine()
//                            } ?: continue
//                            if (line == "data: [DONE]") {
//                                close()
//                            } else if (line.startsWith("data:")) {
//                                try {
//                                    // Handle & convert data -> emit to client
//                                    val value =
//                                        if (params.isChatCompletions) lookupDataFromResponseTurbo(
//                                            line
//                                        ) else lookupDataFromResponse(
//                                            line
//                                        )
//
//                                    if (value.isNotEmpty()) {
//                                        trySend(value)
//                                    }
//                                } catch (e: Exception) {
//                                    e.printStackTrace()
//                                    Log.e("ChatGPT Lite BUG", e.toString())
//                                }
//                            }
//                        }
//                    } catch (e: IOException) {
//                        Log.e("ChatGPT Lite BUG", e.toString())
//                        throw Exception(e)
//                    } finally {
//                        withContext(Dispatchers.IO) {
//                            input.close()
//                        }
//
//                        close()
//                    }
//                } else {
//                    if (!response.isSuccessful) {
//                        var jsonObject: JSONObject?
//                        try {
//                            jsonObject = JSONObject(response.errorBody()!!.string())
//                            println(jsonObject)
//                            trySend("Failure! Try again. $jsonObject")
//                        } catch (e: JSONException) {
//                            e.printStackTrace()
//                        }
//                    }
//                    trySend("Failure! Try again")
//                    close()
//                }
//            }
//
//            close()
//        }
//    /** Replace any double newline characters (\n\n) with a space.
//    Replace any single newline characters (\n) with a space.
//     */
//    private fun lookupDataFromResponse(jsonString: String): String {
//        val regex = """"text"\s*:\s*"([^"]+)"""".toRegex()
//        val matchResult = regex.find(jsonString)
//
//        if (matchResult != null && matchResult.groupValues.size > 1) {
//            val extractedText = matchResult.groupValues[1]
//            return extractedText
//                .replace("\\n\\n", " ")
//                .replace("\\n", " ")
//        }
//
//        return " "
//    }
//
//    private fun lookupDataFromResponseTurbo(jsonString: String): String {
//        val regex = """"content"\s*:\s*"([^"]+)"""".toRegex()
//        val matchResult = regex.find(jsonString)
//
//        if (matchResult != null && matchResult.groupValues.size > 1) {
//            val extractedText = matchResult.groupValues[1]
//            return extractedText
//                .replace("\\n\\n", " ")
//                .replace("\\n", " ")
//        }
//
//        return " "
//    }
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
        imageUri: String?
    ): String {
        Log.d("PromptPilot", "getAIResponseFromBackend called with: $prompt, $model")
        return try {
            val backendRequest = BackendChatRequest(
                uid = uid,
                prompt = prompt,
                model = model,
                chat_id = chatId,
                title = title
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