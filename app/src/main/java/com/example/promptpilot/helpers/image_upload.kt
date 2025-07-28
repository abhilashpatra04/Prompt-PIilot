package com.example.promptpilot.helpers

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject

suspend fun uploadImageToCloudinary(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
    val inputStream = context.contentResolver.openInputStream(uri)
    val bytes = inputStream?.readBytes() ?: return@withContext null
    val requestBody = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("file", "image.jpg", RequestBody.create("image/*".toMediaTypeOrNull(), bytes))
        .addFormDataPart("upload_preset", "Prompt Pilot") // Set this in your Cloudinary dashboard
        .build()
    val request = Request.Builder()
        .url("https://api.cloudinary.com/v1_1/dkkyiygll/image/upload")
        .post(requestBody)
        .build()
    val client = OkHttpClient()
    val response = client.newCall(request).execute()
    val responseBody = response.body.string()
    Log.d("ImageUpload", "Cloudinary response: $responseBody")
    val json = JSONObject(responseBody)
    return@withContext json.optString("secure_url")
}