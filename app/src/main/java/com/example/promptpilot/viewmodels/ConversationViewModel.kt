package com.example.promptpilot.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.promptpilot.data.remote.ConversationRepository
import com.example.promptpilot.data.remote.MessageRepository
import com.example.promptpilot.data.remote.OpenAIRepositoryImpl
import com.example.promptpilot.data.remote.PendingAttachmentEntity
import com.example.promptpilot.data.remote.PendingAttachmentRepository
import com.example.promptpilot.helpers.uploadPdfsToBackend
import com.example.promptpilot.models.AI_Model
import com.example.promptpilot.models.AttachmentType
import com.example.promptpilot.models.ChatAttachment
import com.example.promptpilot.models.ConversationModel
import com.example.promptpilot.models.MessageModel
import com.example.promptpilot.models.SenderType
import com.example.promptpilot.screens.AgentType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Date
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val conversationRepo: ConversationRepository,
    private val messageRepo: MessageRepository,
    private val openAIRepo: OpenAIRepositoryImpl,
    private val pendingAttachmentRepo: PendingAttachmentRepository,
    private val backendApi: com.example.promptpilot.data.api.BackendApi
) : ViewModel() {

    companion object {
        private const val OPERATION_TIMEOUT_MS = 60000L
    }

    private val _currentConversation: MutableStateFlow<String> =
        MutableStateFlow(Date().time.toString())
    private val _conversations: MutableStateFlow<MutableList<ConversationModel>> = MutableStateFlow(
        mutableListOf()
    )
    private val _messages: MutableStateFlow<HashMap<String, MutableList<MessageModel>>> =
        MutableStateFlow(HashMap())
    private val _isFetching: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val _isFabExpanded = MutableStateFlow(false)
    private val _isStreaming: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val _errorState = MutableStateFlow<String?>(null)

    // Add scroll trigger for UI
    private val _shouldScrollToBottom = MutableStateFlow(false)
    val shouldScrollToBottom: StateFlow<Boolean> = _shouldScrollToBottom.asStateFlow()

    val currentConversationState: StateFlow<String> = _currentConversation.asStateFlow()
    val conversationsState: StateFlow<MutableList<ConversationModel>> = _conversations.asStateFlow()
    val messagesState: StateFlow<HashMap<String, MutableList<MessageModel>>> =
        _messages.asStateFlow()
    val isFabExpanded: StateFlow<Boolean> get() = _isFabExpanded
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    private var stopReceivingResults = false
    private val _selectedModel = MutableStateFlow(AI_Model.Deepseek)
    val selectedModel: StateFlow<AI_Model> get() = _selectedModel

    fun setSelectedModel(model: AI_Model) {
        _selectedModel.value = model
    }

    fun clearError() {
        _errorState.value = null
    }

    fun onScrollHandled() {
        _shouldScrollToBottom.value = false
    }

    suspend fun initialize() {
        _isFetching.value = true
        try {
            _conversations.value = conversationRepo.fetchConversations()

            if (_conversations.value.isNotEmpty()) {
                _currentConversation.value = _conversations.value.first().id
                // Load from Room for instant UI
                val localMessages = messageRepo.fetchMessagesLocal(_currentConversation.value)
                setMessages(localMessages.toMutableList())
                // Then sync with Firestore for updates
                messageRepo.fetchMessages(_currentConversation.value).collectLatest { remoteMessages ->
                    remoteMessages.forEach { message ->
                        messageRepo.createMessage(message)
                    }
                    setMessages(remoteMessages.toMutableList())
                }
            }
        } catch (e: Exception) {
            Log.e("PromptPilot", "Error initializing ViewModel", e)
            _errorState.value = "Failed to initialize conversations"
        } finally {
            _isFetching.value = false
        }
    }

    suspend fun onConversation(conversation: ConversationModel) {
        _isFetching.value = true
        try {
            _currentConversation.value = conversation.id
            fetchMessages()
            loadPendingAttachments(conversation.id)
            // Scroll to bottom when switching conversations
            _shouldScrollToBottom.value = true
        } catch (e: Exception) {
            Log.e("PromptPilot", "Error switching conversation", e)
            _errorState.value = "Failed to load conversation"
        } finally {
            _isFetching.value = false
        }
    }

//    suspend fun sendMessage(
//        message: String,
//        attachments: List<ChatAttachment>,
//        context: android.content.Context,
//        webSearch: Boolean = false,
//        agentType: AgentType? = null
//    ) {
//        Log.d("PromptPilot", "sendMessage called with: $message, webSearch: $webSearch, agent: $agentType")
//
//        // Clear any previous errors
//        _errorState.value = null
//
//        try {
//            // Handle PDF uploads with timeout
//            val pdfAttachments = attachments.filter { it.type == AttachmentType.PDF }
//            if (pdfAttachments.isNotEmpty()) {
//                val pdfUris = pdfAttachments.map { Uri.parse(it.url) }
//                val uploaded = withTimeoutOrNull(OPERATION_TIMEOUT_MS) {
//                    withContext(Dispatchers.IO) {
//                        uploadPdfsToBackend(backendApi, _currentConversation.value, pdfUris, context)
//                    }
//                }
//                if (uploaded != true) {
//                    _errorState.value = "Failed to upload PDF files. Please try again."
//                    return
//                }
//            }
//
//            stopReceivingResults = false
//            if (getMessagesByConversation(_currentConversation.value).isEmpty()) {
//                createConversationRemote(message)
//            }
//
//            // Create user message
//            val userMessageModel = MessageModel(
//                id = UUID.randomUUID().toString(),
//                question = message,
//                answer = "", // User message doesn't have an answer
//                conversationId = _currentConversation.value,
//                text = message,
//                sender = SenderType.USER,
//                timestamp = System.currentTimeMillis(),
//                attachments = attachments,
//                model = _selectedModel.value.model
//            )
//
//            // Create AI message placeholder
//            val aiMessageModel = MessageModel(
//                id = UUID.randomUUID().toString(),
//                question = "", // AI message doesn't have a question
//                answer = "Thinking...",
//                conversationId = _currentConversation.value,
//                text = "Thinking...",
//                sender = SenderType.ASSISTANT,
//                timestamp = System.currentTimeMillis() + 1, // Slightly later timestamp
//                attachments = emptyList(),
//                model = _selectedModel.value.model
//            )
//
//            val currentListMessage: MutableList<MessageModel> =
//                getMessagesByConversation(_currentConversation.value).toMutableList()
//
//            // Add user message first, then AI message - IMPORTANT: Add to the END of list (most recent)
//            currentListMessage.add(userMessageModel)
//            currentListMessage.add(aiMessageModel)
//
//            setMessages(currentListMessage)
//            messageRepo.createMessage(userMessageModel)
//
//            // Trigger scroll to bottom for new messages
//            _shouldScrollToBottom.value = true
//
//            _isStreaming.value = true
//
//            // Use timeout for streaming response
//            val result = withTimeoutOrNull(120000L) { // 2 minute timeout for AI response
//                withContext(Dispatchers.IO) {
//                    openAIRepo.getStreamingAIResponse(
//                        uid = "abhilash04",
//                        prompt = message,
//                        model = _selectedModel.value.model,
//                        chatId = _currentConversation.value,
//                        imageUrls = attachments.map { it.url }.takeIf { it.isNotEmpty() },
//                        webSearch = webSearch,
//                        agentType = agentType?.name
//                    ) { streamedText ->
//                        // Update UI on Main thread
//                        viewModelScope.launch(Dispatchers.Main) {
//                            updateLatestAIMessage(streamedText)
//                        }
//                    }
//                }
//            }
//
//            if (result != null) {
//                // Final update with complete response
//                updateLatestAIMessage(result)
//                // Save the complete AI message
//                val updatedMessages = getMessagesByConversation(_currentConversation.value)
//                val latestAIMessage = updatedMessages.lastOrNull { it.sender == SenderType.ASSISTANT }
//                if (latestAIMessage != null) {
//                    messageRepo.createMessage(latestAIMessage.copy(answer = result, text = result))
//                }
//                Log.d("PromptPilot", "Message sent successfully")
//            } else {
//                // Timeout occurred
//                val timeoutMessage = "Request timed out. Please try with a shorter message or check your connection."
//                updateLatestAIMessage(timeoutMessage)
//                _errorState.value = timeoutMessage
//            }
//
//        } catch (e: Exception) {
//            Log.e("PromptPilot", "Error in sendMessage", e)
//            val errorMessage = when {
//                e.message?.contains("timeout", ignoreCase = true) == true ->
//                    "Request timed out. Please try again with a shorter message."
//                e.message?.contains("network", ignoreCase = true) == true ->
//                    "Network error. Please check your connection and try again."
//                e.message?.contains("server", ignoreCase = true) == true ->
//                    "Server error. Please try again later."
//                else -> "Something went wrong. Please try again."
//            }
//
//            _errorState.value = errorMessage
//            updateLatestAIMessage("Sorry, I encountered an error. Please try again.")
//        } finally {
//            _isStreaming.value = false
//        }
//    }

    suspend fun sendMessage(
        message: String,
        attachments: List<ChatAttachment>,
        context: android.content.Context,
        webSearch: Boolean = false,
        agentType: AgentType? = null
    ) {
        Log.d("PromptPilot", "sendMessage called with: $message, webSearch: $webSearch, agent: $agentType")

        val pdfAttachments = attachments.filter { it.type == AttachmentType.PDF }
        if (pdfAttachments.isNotEmpty()) {
            val pdfUris = pdfAttachments.map { Uri.parse(it.url) }
            val uploaded = withContext(Dispatchers.IO) {
                uploadPdfsToBackend(backendApi, _currentConversation.value, pdfUris, context)
            }
            if (!uploaded) {
                return
            }
        }

        stopReceivingResults = false
        if (getMessagesByConversation(_currentConversation.value).isEmpty()) {
            createConversationRemote(message)
        }

        // Create new message with proper timestamp
        val newMessageModel = MessageModel(
            id = UUID.randomUUID().toString(),
            question = message,
            answer = "Thinking...", // Initial placeholder
            conversationId = _currentConversation.value,
            text = message,
            sender = SenderType.USER,
            timestamp = System.currentTimeMillis(),
            createdAt = Date(), // Add this for proper sorting
            attachments = attachments,
            model = _selectedModel.value.model
        )

        val currentListMessage: MutableList<MessageModel> =
            getMessagesByConversation(_currentConversation.value).toMutableList()

        // Add message to the end of the list (newest messages at the end)
        currentListMessage.add(newMessageModel)
        setMessages(currentListMessage)
        messageRepo.createMessage(newMessageModel)

        try {
            _isStreaming.value = true

            // Use IO dispatcher for network operations
            val aiReply = withContext(Dispatchers.IO) {
                openAIRepo.getStreamingAIResponse(
                    uid = "abhilash04",
                    prompt = message,
                    model = _selectedModel.value.model,
                    chatId = _currentConversation.value,
                    imageUrls = attachments.map { it.url }.takeIf { it.isNotEmpty() },
                    webSearch = webSearch,
                    agentType = agentType?.name
                ) { streamedText ->
                    // Update UI on Main thread for streaming
                    viewModelScope.launch(Dispatchers.Main) {
                        updateLatestMessageAnswer(streamedText)
                    }
                }
            }

            // Final update with complete response
            updateLatestMessageAnswer(aiReply)
            _isStreaming.value = false

        } catch (e: Exception) {
            Log.e("PromptPilot", "Error in sendMessage", e)
            _errorState.value = "Network error: ${e.message}"
            _isStreaming.value = false
            updateLatestMessageAnswer("Sorry, I encountered an error. Please try again. Error: ${e.message}")
        }
    }


    // Updated method to handle latest message answer updates
    private fun updateLatestMessageAnswer(answer: String) {
        val currentListMessage: MutableList<MessageModel> =
            getMessagesByConversation(_currentConversation.value).toMutableList()

        if (currentListMessage.isNotEmpty()) {
            // Update the last message (newest) instead of first
            val lastIndex = currentListMessage.size - 1
            currentListMessage[lastIndex] = currentListMessage[lastIndex].copy(answer = answer)
            setMessages(currentListMessage)
        }
    }

    // Also update this method to handle proper message ordering
    private fun setMessages(messages: MutableList<MessageModel>) {
        val messagesMap: HashMap<String, MutableList<MessageModel>> =
            _messages.value.clone() as HashMap<String, MutableList<MessageModel>>

        // Ensure messages are sorted by timestamp
        val sortedMessages = messages.sortedBy { it.timestamp }.toMutableList()
        messagesMap[_currentConversation.value] = sortedMessages
        _messages.value = messagesMap
    }

    private fun createConversationRemote(title: String) {
        val newConversation = ConversationModel(
            id = _currentConversation.value,
            title = title.take(100), // Limit title length
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
        _errorState.value = null // Clear any errors when starting new conversation

        // Clear pending attachments for new conversation
        clearPendingAttachments()

        // Clear messages for new conversation
        val messagesMap = _messages.value.toMutableMap()
        messagesMap[conversationId] = mutableListOf()
        _messages.value = HashMap(messagesMap)
    }

    private fun getMessagesByConversation(conversationId: String): MutableList<MessageModel> {
        if (_messages.value[conversationId] == null) return mutableListOf()

        val messagesMap: HashMap<String, MutableList<MessageModel>> =
            _messages.value.clone() as HashMap<String, MutableList<MessageModel>>

        return messagesMap[conversationId] ?: mutableListOf()
    }

    suspend fun deleteConversationAndFiles(conversationId: String) {
        try {
            Log.d("PromptPilot", "Starting deletion of conversation: $conversationId")

            // Delete conversation from Firestore
            conversationRepo.deleteConversation(conversationId)

            // Delete all messages for this conversation
            messageRepo.deleteMessagesByConversation(conversationId)

            // Delete all pending attachments for this conversation from Room
            pendingAttachmentRepo.clearPendingAttachments(conversationId)

            // Delete all files for this conversation from backend with timeout
            try {
                val response = withTimeoutOrNull(30000L) { // 30 second timeout
                    backendApi.deleteFilesForConversation(conversationId)
                }

                if (response?.isSuccessful != true) {
                    Log.w("PromptPilot", "Backend file deletion failed or timed out for conversation: $conversationId")
                }
            } catch (e: Exception) {
                Log.w("PromptPilot", "Error deleting files from backend for conversation: $conversationId", e)
            }

            // Remove from local state
            val conversations: MutableList<ConversationModel> = _conversations.value.toMutableList()
            val conversationToRemove = conversations.find { it.id == conversationId }
            if (conversationToRemove != null) {
                conversations.remove(conversationToRemove)
                _conversations.value = conversations
            }

            // Remove messages from local state
            val messagesMap = _messages.value.toMutableMap()
            messagesMap.remove(conversationId)
            _messages.value = HashMap(messagesMap)

            // If we deleted the current conversation, start a new one
            if (conversationId == _currentConversation.value) {
                newConversation()
            }

            Log.d("PromptPilot", "Successfully deleted conversation: $conversationId")

        } catch (e: Exception) {
            Log.e("PromptPilot", "Error deleting conversation: $conversationId", e)
            _errorState.value = "Failed to delete conversation. Please try again."
        }
    }

    private suspend fun fetchMessages() {
        if (_currentConversation.value.isEmpty() ||
            _messages.value[_currentConversation.value] != null) return

        try {
            val flow: Flow<List<MessageModel>> = messageRepo.fetchMessages(_currentConversation.value)
            flow.collectLatest { messages ->
                // Sort messages by timestamp to ensure correct order
                val sortedMessages = messages.sortedBy { it.timestamp }.toMutableList()
                setMessages(sortedMessages)
                // Scroll to bottom when messages are loaded
                if (sortedMessages.isNotEmpty()) {
                    _shouldScrollToBottom.value = true
                }
            }
        } catch (e: Exception) {
            Log.e("PromptPilot", "Error fetching messages", e)
            _errorState.value = "Failed to load messages"
        }
    }

//    private fun updateLatestAIMessage(answer: String) {
//        val currentListMessage: MutableList<MessageModel> =
//            getMessagesByConversation(_currentConversation.value).toMutableList()
//
//        // Find the latest AI message and update it
//        val latestAIMessageIndex = currentListMessage.indexOfLast { it.sender == SenderType.ASSISTANT }
//        if (latestAIMessageIndex != -1) {
//            currentListMessage[latestAIMessageIndex] = currentListMessage[latestAIMessageIndex].copy(
//                answer = answer,
//                text = answer
//            )
//            setMessages(currentListMessage)
//        }
//    }
//
//    private fun setMessages(messages: MutableList<MessageModel>) {
//        val messagesMap: HashMap<String, MutableList<MessageModel>> =
//            _messages.value.clone() as HashMap<String, MutableList<MessageModel>>
//
//        // Ensure messages are sorted by timestamp (oldest first, newest last)
//        val sortedMessages = messages.sortedBy { it.timestamp }.toMutableList()
//        messagesMap[_currentConversation.value] = sortedMessages
//        _messages.value = messagesMap
//    }

    fun stopReceivingResults() {
        stopReceivingResults = true
        _isStreaming.value = false
    }

    // Pending attachments management
    private val _pendingAttachments = MutableStateFlow<List<ChatAttachment>>(emptyList())
    val pendingAttachments: StateFlow<List<ChatAttachment>> = _pendingAttachments.asStateFlow()

    init {
        viewModelScope.launch {
            loadPendingAttachments(_currentConversation.value)
        }
    }

    private suspend fun loadPendingAttachments(conversationId: String) {
        try {
            val entities = pendingAttachmentRepo.getPendingAttachments(conversationId)
            _pendingAttachments.value = entities.map {
                ChatAttachment(
                    name = it.name,
                    url = it.url,
                    type = if (it.type == "IMAGE") AttachmentType.IMAGE else AttachmentType.PDF
                )
            }
        } catch (e: Exception) {
            Log.e("PromptPilot", "Error loading pending attachments", e)
        }
    }

    fun addAttachment(attachment: ChatAttachment) {
        _pendingAttachments.update { it + attachment }
        viewModelScope.launch {
            try {
                pendingAttachmentRepo.insertPendingAttachment(
                    PendingAttachmentEntity(
                        conversationId = _currentConversation.value,
                        name = attachment.name,
                        url = attachment.url,
                        type = if (attachment.type == AttachmentType.IMAGE) "IMAGE" else "PDF"
                    )
                )
            } catch (e: Exception) {
                Log.e("PromptPilot", "Error adding attachment", e)
                _pendingAttachments.update { it - attachment }
            }
        }
    }

    fun removeAttachment(attachment: ChatAttachment) {
        _pendingAttachments.update { it - attachment }
        viewModelScope.launch {
            try {
                val entities = pendingAttachmentRepo.getPendingAttachments(_currentConversation.value)
                val entity = entities.find { it.url == attachment.url && it.name == attachment.name }
                if (entity != null) {
                    pendingAttachmentRepo.deletePendingAttachment(entity)
                }
            } catch (e: Exception) {
                Log.e("PromptPilot", "Error removing attachment", e)
                _pendingAttachments.update { it + attachment }
            }
        }
    }

    fun clearPendingAttachments() {
        _pendingAttachments.value = emptyList()
        viewModelScope.launch {
            try {
                pendingAttachmentRepo.clearPendingAttachments(_currentConversation.value)
            } catch (e: Exception) {
                Log.e("PromptPilot", "Error clearing pending attachments", e)
            }
        }
    }
    private val _isTtsEnabled = MutableStateFlow(true) // Default to on
    val isTtsEnabled: StateFlow<Boolean> = _isTtsEnabled.asStateFlow()

    fun toggleTts() {
        _isTtsEnabled.value = !_isTtsEnabled.value
    }
}