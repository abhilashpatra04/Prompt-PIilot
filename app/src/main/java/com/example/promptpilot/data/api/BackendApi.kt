package com.example.promptpilot.data.api

import retrofit2.http.Body
import retrofit2.http.POST

data class BackendChatRequest(
    val uid: String,
    val prompt: String,
    val model: String,
    val chat_id: String? = null,
    val title: String = "Untitled"
)

data class BackendChatResponse(
    val reply: String,
    val chat_id: String
)

interface BackendApi {
    @POST("chat")
    suspend fun getAIResponse(@Body request: BackendChatRequest): BackendChatResponse
}