//package com.example.promptpilot.data.remote
//
//import android.util.Log
//import com.example.promptpilot.models.MessageModel
//import com.example.promptpilot.models.SenderType
//import com.google.firebase.firestore.FirebaseFirestore
//import com.google.firebase.firestore.Query
//import kotlinx.coroutines.channels.awaitClose
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.callbackFlow
//import kotlinx.coroutines.tasks.await
//import javax.inject.Inject
//
//class MessageRepositoryImpl @Inject constructor(
//    private val firestore: FirebaseFirestore,
//    private val messageDao: MessageDao
//) : MessageRepository {
//
//    override suspend fun createMessage(message: MessageModel): MessageModel {
//        try {
//            // Store in Room database first (for offline support)
//            val messageEntity = MessageEntity(
//                id = message.id,
//                conversationId = message.conversationId,
//                question = message.question,
//                answer = message.answer,
//                timestamp = message.timestamp,
//                sender = message.sender.name,
//                model = message.model,
//                attachments = message.attachments
//            )
//            messageDao.insertMessage(messageEntity)
//
//            // Then store in Firestore
//            val firestoreMessage = hashMapOf(
//                "id" to message.id,
//                "conversationId" to message.conversationId,
//                "question" to message.question,
//                "answer" to message.answer,
//                "timestamp" to message.timestamp,
//                "sender" to message.sender.name,
//                "model" to message.model,
//                "uid" to "abhilash04", // You might want to pass this dynamically
//                "createdAt" to com.google.firebase.Timestamp.now()
//            )
//
//            firestore.collection("messages")
//                .document(message.id)
//                .set(firestoreMessage)
//                .await()
//
//            Log.d("MessageRepo", "Message stored successfully: ${message.id}")
//        } catch (e: Exception) {
//            Log.e("MessageRepo", "Error creating message", e)
//            // Don't throw - at least we have it in Room
//        }
//    }
//
//
//
//    override fun fetchMessages(conversationId: String): Flow<List<MessageModel>> = callbackFlow {
//        val listener = firestore.collection("messages")
//            .whereEqualTo("conversationId", conversationId)
//            .orderBy("timestamp", Query.Direction.ASCENDING)
//            .addSnapshotListener { snapshot, error ->
//                if (error != null) {
//                    Log.e("MessageRepo", "Error fetching messages", error)
//                    close(error)
//                    return@addSnapshotListener
//                }
//
//                val messages = mutableListOf<MessageModel>()
//                snapshot?.documents?.forEach { document ->
//                    try {
//                        val data = document.data ?: return@forEach
//
//                        val senderType = when (data["sender"] as? String) {
//                            "USER" -> SenderType.USER
//                            "ASSISTANT" -> SenderType.ASSISTANT
//                            else -> SenderType.USER
//                        }
//
//                        val message = MessageModel(
//                            id = data["id"] as? String ?: document.id,
//                            conversationId = data["conversationId"] as? String ?: "",
//                            question = data["question"] as? String ?: "",
//                            answer = data["answer"] as? String ?: "",
//                            text = if (senderType == SenderType.USER) data["question"] as? String ?: "" else data["answer"] as? String ?: "",
//                            sender = senderType,
//                            timestamp = data["timestamp"] as? Long ?: System.currentTimeMillis(),
//                            attachments = emptyList(), // You can expand this to handle attachments
//                            model = data["model"] as? String ?: ""
//                        )
//                        messages.add(message)
//                    } catch (e: Exception) {
//                        Log.e("MessageRepo", "Error parsing message document: ${document.id}", e)
//                    }
//                }
//
//                // Sort by timestamp to ensure proper order
//                val sortedMessages = messages.sortedBy { it.timestamp }
//                trySend(sortedMessages)
//            }
//
//        awaitClose { listener.remove() }
//    }
//
//    override suspend fun fetchMessagesLocal(conversationId: String): List<MessageModel> {
//        return try {
//            val entities = messageDao.getMessagesByConversation(conversationId)
//            entities.map { entity ->
//                MessageModel(
//                    id = entity.id,
//                    conversationId = entity.conversationId,
//                    question = entity.question,
//                    answer = entity.answer,
//                    text = if (SenderType.valueOf(entity.sender) == SenderType.USER) entity.question else entity.answer,
//                    sender = SenderType.valueOf(entity.sender),
//                    timestamp = entity.timestamp,
//                    attachments = entity.attachments,
//                    model = entity.model
//                )
//            }.sortedBy { it.timestamp } // Ensure proper order
//        } catch (e: Exception) {
//            Log.e("MessageRepo", "Error fetching local messages", e)
//            emptyList()
//        }
//    }
//
//    override suspend fun deleteMessagesByConversation(conversationId: String) {
//        try {
//            // Delete from Room first
//            messageDao.deleteMessagesByConversation(conversationId)
//
//            // Then delete from Firestore
//            val batch = firestore.batch()
//            val messages = firestore.collection("messages")
//                .whereEqualTo("conversationId", conversationId)
//                .get()
//                .await()
//
//            messages.documents.forEach { document ->
//                batch.delete(document.reference)
//            }
//
//            batch.commit().await()
//            Log.d("MessageRepo", "Deleted all messages for conversation: $conversationId")
//        } catch (e: Exception) {
//            Log.e("MessageRepo", "Error deleting messages for conversation: $conversationId", e)
//        }
//    }
//
//    suspend fun deleteMessage(messageId: String) {
//        try {
//            // Delete from Room
//            messageDao.deleteMessagesByConversation(messageId)
//
//            // Delete from Firestore
//            firestore.collection("messages")
//                .document(messageId)
//                .delete()
//                .await()
//
//            Log.d("MessageRepo", "Deleted message: $messageId")
//        } catch (e: Exception) {
//            Log.e("MessageRepo", "Error deleting message: $messageId", e)
//        }
//    }
//}

package com.example.promptpilot.data.remote

import android.content.ContentValues
import android.util.Log
import com.example.promptpilot.constants.messageCollection
import com.example.promptpilot.helpers.DataHolder
import com.example.promptpilot.models.MessageModel
import com.example.promptpilot.models.SenderType
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class MessageRepositoryImpl @Inject constructor(
    private val fsInstance: FirebaseFirestore,
    private val appDatabase: AppDatabase
) : MessageRepository {
    private lateinit var result: QuerySnapshot
    override fun fetchMessages(conversationId: String): Flow<List<MessageModel>> =
        callbackFlow {
        result =
            fsInstance
                .collection(messageCollection)
                .whereEqualTo("conversationId", conversationId)
                .orderBy("createdAt", Query.Direction.DESCENDING).get().await()

        if (result.documents.isNotEmpty()) {
            val documents = result.documents

            trySend(element = documents.map {
                it.toObject(MessageModel::class.java)
            }.toList() as List<MessageModel>)

            awaitClose {
                close()
            }
        } else {
            trySend(listOf())

            awaitClose {
                close()
            }
        }
    }

    override suspend fun createMessage(message: MessageModel): MessageModel {
        // Save to Room
        appDatabase.messageDao().insertMessage(message.toEntity())
        // Save to Firestore (use message.id as the document ID)
        fsInstance.collection(messageCollection).document(message.id).set(message)
        return message
    }

    override fun deleteMessage() {
        val docRef = fsInstance
            .collection("messages")
            .document(DataHolder.docPath)

        // Remove the fields from the document
        val updates = hashMapOf<String, Any>(
            "answer" to FieldValue.delete(),
            "conversationId" to FieldValue.delete(),
            "createdAt" to FieldValue.delete(),
            "id" to FieldValue.delete(),
            "question" to FieldValue.delete()
        )
        docRef.update(updates)
            .addOnSuccessListener {
                Log.d(
                    ContentValues.TAG,
                    "DocumentSnapshot successfully deleted from message!"
                )
            }
            .addOnFailureListener { e ->
                Log.w(
                    ContentValues.TAG,
                    "Error deleting document", e
                )
            }

    }
    override suspend fun deleteMessagesByConversation(conversationId: String) {
        val querySnapshot = fsInstance.collection(messageCollection)
            .whereEqualTo("conversationId", conversationId)
            .get()
            .await()

        for (doc in querySnapshot.documents) {
            doc.reference.delete()
        }
    }
    override suspend fun fetchMessagesLocal(conversationId: String): List<MessageModel> {
        return appDatabase.messageDao().getMessagesByConversation(conversationId)
            .map { it.toModel() }
    }

    private fun MessageModel.toEntity(): MessageEntity = MessageEntity(
        id = id,
        text = text,
        conversationId = conversationId,
        sender = sender.name,
        timestamp = timestamp,
        attachments = attachments
    )
    private fun MessageEntity.toModel(): MessageModel = MessageModel(
        id = id,
        text = text,
        conversationId = conversationId,
        sender = SenderType.valueOf(sender),
        timestamp = timestamp,
        attachments = attachments
    )
}
