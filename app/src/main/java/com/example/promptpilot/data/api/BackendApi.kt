package com.example.promptpilot.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.Part

data class BackendChatRequest(
    val uid: String,
    val prompt: String,
    val model: String,
    val chat_id: String? = null,
    val title: String = "Untitled",
    val image_urls: List<String>? = null,
    val web_search: Boolean = false,
    val agent_type: String? = null,
    val stream: Boolean = false
)

data class BackendChatResponse(
    val reply: String,
    val chat_id: String
)

data class UploadResponse(
    val status: String,
    val message: String,
    val files: List<String>?
)

data class WebSearchRequest(
    val query: String
)

data class WebSearchResponse(
    val results: List<Map<String, Any>>,
    val extracted_content: String?,
    val sources: List<Map<String, String>>?
)

interface BackendApi {
    @POST("chat")
    suspend fun getAIResponse(@Body request: BackendChatRequest): BackendChatResponse

    @POST("delete_files_for_conversation")
    suspend fun deleteFilesForConversation(@Query("conversation_id") conversationId: String): Response<Unit>

    @Multipart
    @POST("upload_pdf")
    suspend fun uploadPdf(
        @Part("chat_id") chatId: RequestBody,
        @Part files: List<MultipartBody.Part>
    ): Response<UploadResponse>

    @POST("websearch")
    suspend fun performWebSearch(@Body request: WebSearchRequest): WebSearchResponse
}

//package com.example.promptpilot.data.api
//
//import retrofit2.Response
//import retrofit2.http.Body
//import retrofit2.http.POST
//import retrofit2.http.Query
//import okhttp3.MultipartBody
//import okhttp3.RequestBody
//import retrofit2.http.Multipart
//import retrofit2.http.Part
//
//data class BackendChatRequest(
//    val uid: String,
//    val prompt: String,
//    val model: String,
//    val chat_id: String? = null,
//    val title: String = "Untitled",
//    val image_urls: List<String>? = null
//)
//
//data class BackendChatResponse(
//    val reply: String,
//    val chat_id: String
//)
//
//data class UploadResponse(
//    val status: String,
//    val message: String,
//    val files: List<String>?
//)
//
//interface BackendApi {
//    @POST("chat")
//    suspend fun getAIResponse(@Body request: BackendChatRequest): BackendChatResponse
//    @POST("delete_files_for_conversation")
//    suspend fun deleteFilesForConversation(@Query("conversation_id") conversationId: String): Response<Unit>
//
//    @Multipart
//    @POST("upload_pdf")
//    suspend fun uploadPdf(
//        @Part("chat_id") chatId: RequestBody,
//        @Part files: List<MultipartBody.Part>
//    ): Response<UploadResponse>
//}