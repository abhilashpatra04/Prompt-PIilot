package com.example.promptpilot.data.remote

import com.example.promptpilot.constants.conversationCollection
import com.example.promptpilot.models.ConversationModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ConversationRepositoryImpl @Inject constructor(
    private val fsInstance: FirebaseFirestore,
) : ConversationRepository {

    override suspend fun fetchConversations(): MutableList<ConversationModel> {

        if (getFireBaseSnapShot().documents.isNotEmpty()) {
            val documents = getFireBaseSnapShot().documents

            return documents.map {
                it.toObject(ConversationModel::class.java)
            }.toList() as MutableList<ConversationModel>
        }

        return mutableListOf()
    }

    override fun newConversation(conversation: ConversationModel): ConversationModel {
        fsInstance.collection(conversationCollection).add(conversation)
        return conversation
    }

    override suspend fun deleteConversation(conversationId: String) {
        val snapshot = fsInstance
            .collection("conversations")
            .whereEqualTo("id", conversationId)
            .get()
            .await()
        for (doc in snapshot.documents) {
            fsInstance.collection("conversations").document(doc.id).delete().await()
        }
    }

    private suspend fun getFireBaseSnapShot() =
        fsInstance.collection(conversationCollection)
            .orderBy("createdAt", Query.Direction.DESCENDING).get().await()

}