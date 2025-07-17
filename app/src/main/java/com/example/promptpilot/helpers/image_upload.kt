package com.example.promptpilot.helpers

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

suspend fun uploadImageToFirebase(uri: Uri): String? {
    val storageRef = FirebaseStorage.getInstance().reference
    val fileRef = storageRef.child("chat_images/${System.currentTimeMillis()}_${uri.lastPathSegment}")
    val uploadTask = fileRef.putFile(uri).await()
    return fileRef.downloadUrl.await().toString()
}