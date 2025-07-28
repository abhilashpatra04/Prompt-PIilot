package com.example.promptpilot.data.remote

import androidx.room.TypeConverter
import com.example.promptpilot.models.ChatAttachment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    @TypeConverter
    fun fromAttachmentList(value: List<ChatAttachment>?): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toAttachmentList(value: String): List<ChatAttachment>? {
        val listType = object : TypeToken<List<ChatAttachment>>() {}.type
        return Gson().fromJson(value, listType)
    }
}