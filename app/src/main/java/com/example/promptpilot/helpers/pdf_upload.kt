package com.example.promptpilot.helpers

import android.content.Context
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import com.example.promptpilot.data.api.BackendApi

suspend fun uploadPdfsToBackend(
    backendApi: BackendApi,
    chatId: String,
    pdfUris: List<Uri>,
    context: Context
): Boolean {
    val files = pdfUris.map { uri ->
        val inputStream = context.contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes() ?: return false
        val requestBody = RequestBody.create("application/pdf".toMediaTypeOrNull(), bytes)
        MultipartBody.Part.createFormData("files", "file.pdf", requestBody)
    }
    val chatIdBody = RequestBody.create("text/plain".toMediaTypeOrNull(), chatId)
    val response = backendApi.uploadPdf(chatIdBody, files)
    return response.isSuccessful
}
