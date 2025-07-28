package com.example.promptpilot.data.remote

import javax.inject.Inject

class PendingAttachmentRepository @Inject constructor(
    private val pendingAttachmentDao: PendingAttachmentDao
) {
    suspend fun getPendingAttachments(conversationId: String): List<PendingAttachmentEntity> =
        pendingAttachmentDao.getPendingAttachments(conversationId)

    suspend fun insertPendingAttachment(attachment: PendingAttachmentEntity) =
        pendingAttachmentDao.insertPendingAttachment(attachment)

    suspend fun deletePendingAttachment(attachment: PendingAttachmentEntity) =
        pendingAttachmentDao.deletePendingAttachment(attachment)

    suspend fun clearPendingAttachments(conversationId: String) =
        pendingAttachmentDao.clearPendingAttachments(conversationId)
} 