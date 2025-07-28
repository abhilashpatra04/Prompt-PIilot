package com.example.promptpilot.data.remote

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete

@Dao
interface PendingAttachmentDao {
    @Query("SELECT * FROM pending_attachments WHERE conversationId = :conversationId")
    suspend fun getPendingAttachments(conversationId: String): List<PendingAttachmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingAttachment(attachment: PendingAttachmentEntity)

    @Delete
    suspend fun deletePendingAttachment(attachment: PendingAttachmentEntity)

    @Query("DELETE FROM pending_attachments WHERE conversationId = :conversationId")
    suspend fun clearPendingAttachments(conversationId: String)
} 