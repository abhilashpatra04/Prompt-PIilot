package com.example.promptpilot.data.remote

data class ChatImage(
    val name: String,
    val url: String,
    var useAsContext: Boolean = false
)