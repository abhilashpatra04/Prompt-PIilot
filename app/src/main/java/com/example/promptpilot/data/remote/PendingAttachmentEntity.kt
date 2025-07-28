package com.example.promptpilot.data.remote

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_attachments")
data class PendingAttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val conversationId: String,
    val name: String,
    val url: String,
    val type: String // "IMAGE" or "PDF"
) 