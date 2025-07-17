package com.example.promptpilot.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.promptpilot.data.remote.ConversationRepository
import com.example.promptpilot.data.remote.MessageRepository
import com.example.promptpilot.data.remote.OpenAIRepositoryImpl
import com.example.promptpilot.models.AI_Model
import com.example.promptpilot.models.ConversationModel
import com.example.promptpilot.models.MessageModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import java.util.Date
import javax.inject.Inject

/**
 * Used to communicate between screens.
 */

@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val conversationRepo: ConversationRepository,
    private val messageRepo: MessageRepository,
    private val openAIRepo: OpenAIRepositoryImpl,
) : ViewModel() {
    private val _currentConversation: MutableStateFlow<String> =
        MutableStateFlow(Date().time.toString())
    private val _conversations: MutableStateFlow<MutableList<ConversationModel>> = MutableStateFlow(
        mutableListOf()
    )
    private val _messages: MutableStateFlow<HashMap<String, MutableList<MessageModel>>> =
        MutableStateFlow(HashMap())
    private val _isFetching: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val _isFabExpanded = MutableStateFlow(false)

    val currentConversationState: StateFlow<String> = _currentConversation.asStateFlow()
    val conversationsState: StateFlow<MutableList<ConversationModel>> = _conversations.asStateFlow()
    val messagesState: StateFlow<HashMap<String, MutableList<MessageModel>>> =
        _messages.asStateFlow()
    val isFabExpanded: StateFlow<Boolean> get() = _isFabExpanded
    private val _errorState = MutableStateFlow<String?>(null)

    private var stopReceivingResults = false
    private val _selectedModel = MutableStateFlow(AI_Model.Deepseek)
    val selectedModel: StateFlow<AI_Model> get() = _selectedModel
    fun setSelectedModel(model: AI_Model) {
        _selectedModel.value = model
    }
    suspend fun initialize() {
        _isFetching.value = true

        _conversations.value = conversationRepo.fetchConversations()

        if (_conversations.value.isNotEmpty()) {
            _currentConversation.value = _conversations.value.first().id
            fetchMessages()
        }

        _isFetching.value = false
    }

    suspend fun onConversation(conversation: ConversationModel) {
        _isFetching.value = true
        _currentConversation.value = conversation.id

        fetchMessages()
        _isFetching.value = false
    }

    suspend fun sendMessage(message: String, imageUri: String?) {
        Log.d("PromptPilot", "sendMessage called with: $message")
        stopReceivingResults = false
        if (getMessagesByConversation(_currentConversation.value).isEmpty()) {
            createConversationRemote(message)
        }

        val newMessageModel = MessageModel(
            question = message,
            answer = "Let me think...",
            conversationId = _currentConversation.value,
        )

        val currentListMessage: MutableList<MessageModel> =
            getMessagesByConversation(_currentConversation.value).toMutableList()

        // Insert message to list
        currentListMessage.add(0, newMessageModel)
        setMessages(currentListMessage)

        try {
            val aiReply = openAIRepo.getAIResponseFromBackend(
                uid = "abhilash04",
                prompt = message,
                model = _selectedModel.value.model,
                chatId = _currentConversation.value,
                imageUri = imageUri
            )
            if (aiReply.startsWith("Network error")) {
                // Update UI with error state
                _errorState.value = aiReply // or use a dedicated error flow/state
            } else {
                updateLocalAnswer(aiReply)
                // Save to Firestore
                // messageRepo.createMessage(newMessageModel.copy(answer = aiReply))
            }
        } catch (_: Exception) {
            _errorState.value = "Network error: Unable to connect to server."
        }


    }

    private fun createConversationRemote(title: String) {
        val newConversation = ConversationModel(
            id = _currentConversation.value,
            title = title,
            createdAt = Date(),
        )

        conversationRepo.newConversation(newConversation)

        val conversations = _conversations.value.toMutableList()
        conversations.add(0, newConversation)

        _conversations.value = conversations
    }

    fun newConversation() {
        val conversationId: String = Date().time.toString()

        _currentConversation.value = conversationId
    }

    private fun getMessagesByConversation(conversationId: String): MutableList<MessageModel> {
        if (_messages.value[conversationId] == null) return mutableListOf()

        val messagesMap: HashMap<String, MutableList<MessageModel>> =
            _messages.value.clone() as HashMap<String, MutableList<MessageModel>>

        return messagesMap[conversationId]!!
    }

    suspend fun deleteConversation(conversationId: String) {
        // Delete remote
        conversationRepo.deleteConversation(conversationId)
        
        // Delete all messages for this conversation
        messageRepo.deleteMessagesByConversation(conversationId)

        // Delete local
        val conversations: MutableList<ConversationModel> = _conversations.value.toMutableList()
        val conversationToRemove = conversations.find { it.id == conversationId }

        if (conversationToRemove != null) {
            conversations.remove(conversationToRemove)
            _conversations.value = conversations
        }
    }

    private suspend fun fetchMessages() {
        if (_currentConversation.value.isEmpty() ||
            _messages.value[_currentConversation.value] != null) return

        val flow: Flow<List<MessageModel>> = messageRepo.fetchMessages(_currentConversation.value)

        flow.collectLatest {
            setMessages(it.toMutableList())
        }
    }

    private fun updateLocalAnswer(answer: String) {
        val currentListMessage: MutableList<MessageModel> =
            getMessagesByConversation(_currentConversation.value).toMutableList()

        currentListMessage[0] = currentListMessage[0].copy(answer = answer)

        setMessages(currentListMessage)
    }

    private fun setMessages(messages: MutableList<MessageModel>) {
        val messagesMap: HashMap<String, MutableList<MessageModel>> =
            _messages.value.clone() as HashMap<String, MutableList<MessageModel>>

        messagesMap[_currentConversation.value] = messages

        _messages.value = messagesMap
    }
    fun stopReceivingResults() {
        stopReceivingResults = true
    }

}