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

    val messageId = UUID.randomUUID().toString()

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

    val currentConversationState: StateFlow<String> = _currentConversation.asStateFlow()
    val conversationsState: StateFlow<MutableList<ConversationModel>> = _conversations.asStateFlow()
    val messagesState: StateFlow<HashMap<String, MutableList<MessageModel>>> =
        _messages.asStateFlow()
    val isFabExpanded: StateFlow<Boolean> get() = _isFabExpanded
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

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
        _isFetching.value = false
    }

    suspend fun onConversation(conversation: ConversationModel) {
        _isFetching.value = true
        _currentConversation.value = conversation.id
        fetchMessages()
        loadPendingAttachments(conversation.id)
        _isFetching.value = false
    }

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

        val newMessageModel = MessageModel(
            id = UUID.randomUUID().toString(),
            question = message,
            answer = "Thinking...",
            conversationId = _currentConversation.value,
            text = message,
            sender = SenderType.USER,
            timestamp = System.currentTimeMillis(),
            attachments = attachments,
            model = _selectedModel.value.model
        )

        val currentListMessage: MutableList<MessageModel> =
            getMessagesByConversation(_currentConversation.value).toMutableList()

        // Insert message to list
        currentListMessage.add(0, newMessageModel)
        setMessages(currentListMessage)
        messageRepo.createMessage(newMessageModel)

        try {
            _isStreaming.value = true

            // IMPORTANT: Use IO dispatcher for network operations
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
                    // Update UI on Main thread
                    viewModelScope.launch(Dispatchers.Main) {
                        updateLocalAnswer(streamedText)
                    }
                }
            }

            // Final update with complete response
            updateLocalAnswer(aiReply)
            _isStreaming.value = false

        } catch (e: Exception) {
            Log.e("PromptPilot", "Error in sendMessage", e)
            _errorState.value = "Network error: ${e.message}"
            _isStreaming.value = false
            updateLocalAnswer("Sorry, I encountered an error. Please try again. Error: ${e.message}")
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

        // Clear pending attachments for new conversation
        clearPendingAttachments()
    }

    private fun getMessagesByConversation(conversationId: String): MutableList<MessageModel> {
        if (_messages.value[conversationId] == null) return mutableListOf()

        val messagesMap: HashMap<String, MutableList<MessageModel>> =
            _messages.value.clone() as HashMap<String, MutableList<MessageModel>>

        return messagesMap[conversationId]!!
    }

    suspend fun deleteConversationAndFiles(conversationId: String) {
        // 1. Delete conversation from Firestore
        conversationRepo.deleteConversation(conversationId)
        // 2. Delete all messages for this conversation
        messageRepo.deleteMessagesByConversation(conversationId)
        // 3. Delete all pending attachments for this conversation from Room
        pendingAttachmentRepo.clearPendingAttachments(conversationId)
        // 4. Delete all files for this conversation from backend
        try {
            val response = backendApi.deleteFilesForConversation(conversationId)
            if (!response.isSuccessful) {
                // Optionally handle error
            }
        } catch (e: Exception) {
            // Optionally handle network error
        }
        // 5. Remove from local state
        val conversations: MutableList<ConversationModel> = _conversations.value.toMutableList()
        val conversationToRemove = conversations.find { it.id == conversationId }
        if (conversationToRemove != null) {
            conversations.remove(conversationToRemove)
            _conversations.value = conversations
        }

        // 6. If we deleted the current conversation, start a new one
        if (conversationId == _currentConversation.value) {
            newConversation()
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

        if (currentListMessage.isNotEmpty()) {
            currentListMessage[0] = currentListMessage[0].copy(answer = answer)
            setMessages(currentListMessage)
        }
    }

    private fun setMessages(messages: MutableList<MessageModel>) {
        val messagesMap: HashMap<String, MutableList<MessageModel>> =
            _messages.value.clone() as HashMap<String, MutableList<MessageModel>>

        messagesMap[_currentConversation.value] = messages
        _messages.value = messagesMap
    }

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
        val entities = pendingAttachmentRepo.getPendingAttachments(conversationId)
        _pendingAttachments.value = entities.map {
            ChatAttachment(
                name = it.name,
                url = it.url,
                type = if (it.type == "IMAGE") AttachmentType.IMAGE else AttachmentType.PDF
            )
        }
    }

    fun addAttachment(attachment: ChatAttachment) {
        _pendingAttachments.update { it + attachment }
        viewModelScope.launch {
            pendingAttachmentRepo.insertPendingAttachment(
                PendingAttachmentEntity(
                    conversationId = _currentConversation.value,
                    name = attachment.name,
                    url = attachment.url,
                    type = if (attachment.type == AttachmentType.IMAGE) "IMAGE" else "PDF"
                )
            )
        }
    }

    fun removeAttachment(attachment: ChatAttachment) {
        _pendingAttachments.update { it - attachment }
        viewModelScope.launch {
            val entities = pendingAttachmentRepo.getPendingAttachments(_currentConversation.value)
            val entity = entities.find { it.url == attachment.url && it.name == attachment.name }
            if (entity != null) {
                pendingAttachmentRepo.deletePendingAttachment(entity)
            }
        }
    }

    fun clearPendingAttachments() {
        _pendingAttachments.value = emptyList()
        viewModelScope.launch {
            pendingAttachmentRepo.clearPendingAttachments(_currentConversation.value)
        }
    }
}

//package com.example.promptpilot.viewmodels
//
//import android.net.Uri
//import android.util.Log
//import androidx.compose.ui.platform.LocalContext
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.example.promptpilot.data.remote.ConversationRepository
//import com.example.promptpilot.data.remote.MessageRepository
//import com.example.promptpilot.data.remote.OpenAIRepositoryImpl
//import com.example.promptpilot.data.remote.PendingAttachmentEntity
//import com.example.promptpilot.data.remote.PendingAttachmentRepository
//import com.example.promptpilot.helpers.uploadPdfsToBackend
//import com.example.promptpilot.models.AI_Model
//import com.example.promptpilot.models.AttachmentType
//import com.example.promptpilot.models.ChatAttachment
//import com.example.promptpilot.models.ConversationModel
//import com.example.promptpilot.models.MessageModel
//import com.example.promptpilot.models.SenderType
//import dagger.hilt.android.lifecycle.HiltViewModel
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.flow.collectLatest
//import kotlinx.coroutines.flow.update
//import kotlinx.coroutines.launch
//import java.util.Date
//import java.util.UUID
//import javax.inject.Inject
//
//
//@HiltViewModel
//class ConversationViewModel @Inject constructor(
//    private val conversationRepo: ConversationRepository,
//    private val messageRepo: MessageRepository,
//    private val openAIRepo: OpenAIRepositoryImpl,
//    private val pendingAttachmentRepo: PendingAttachmentRepository,
//    private val backendApi: com.example.promptpilot.data.api.BackendApi
//) : ViewModel() {
//
//    val messageId = UUID.randomUUID().toString()
//
//
//    private val _currentConversation: MutableStateFlow<String> =
//        MutableStateFlow(Date().time.toString())
//    private val _conversations: MutableStateFlow<MutableList<ConversationModel>> = MutableStateFlow(
//        mutableListOf()
//    )
//    private val _messages: MutableStateFlow<HashMap<String, MutableList<MessageModel>>> =
//        MutableStateFlow(HashMap())
//    private val _isFetching: MutableStateFlow<Boolean> = MutableStateFlow(false)
//    private val _isFabExpanded = MutableStateFlow(false)
//
//    val currentConversationState: StateFlow<String> = _currentConversation.asStateFlow()
//    val conversationsState: StateFlow<MutableList<ConversationModel>> = _conversations.asStateFlow()
//    val messagesState: StateFlow<HashMap<String, MutableList<MessageModel>>> =
//        _messages.asStateFlow()
//    val isFabExpanded: StateFlow<Boolean> get() = _isFabExpanded
//    private val _errorState = MutableStateFlow<String?>(null)
//
//    private var stopReceivingResults = false
//    private val _selectedModel = MutableStateFlow(AI_Model.Deepseek)
//    val selectedModel: StateFlow<AI_Model> get() = _selectedModel
//    fun setSelectedModel(model: AI_Model) {
//        _selectedModel.value = model
//    }
//    suspend fun initialize() {
//        _isFetching.value = true
//
//        _conversations.value = conversationRepo.fetchConversations()
//
//        if (_conversations.value.isNotEmpty()) {
//            _currentConversation.value = _conversations.value.first().id
//
//            // 1. Load from Room for instant UI
//            val localMessages = messageRepo.fetchMessagesLocal(_currentConversation.value)
//            setMessages(localMessages.toMutableList())
//
//            // 2. Then sync with Firestore for updates
//            messageRepo.fetchMessages(_currentConversation.value).collectLatest { remoteMessages ->
//                // Update Room with any new/changed messages from Firestore
//                remoteMessages.forEach { message ->
//                    messageRepo.createMessage(message) // This will update Room and Firestore, but since IDs match, no duplicates
//                }
//                setMessages(remoteMessages.toMutableList())
//            }
//        }
//
//        _isFetching.value = false
//    }
//
//    suspend fun onConversation(conversation: ConversationModel) {
//        _isFetching.value = true
//        _currentConversation.value = conversation.id
//
//        fetchMessages()
//        // Load pending attachments for the new conversation
//        loadPendingAttachments(conversation.id)
//        _isFetching.value = false
//    }
//
//    suspend fun sendMessage(message: String, attachments: List<ChatAttachment>, context: android.content.Context) {
//        Log.d("PromptPilot", "sendMessage called with: $message")
//        val pdfAttachments = attachments.filter { it.type == AttachmentType.PDF }
//
//        if (pdfAttachments.isNotEmpty()) {
//           val pdfUris = pdfAttachments.map { Uri.parse(it.url) }
//           val uploaded = uploadPdfsToBackend(backendApi, _currentConversation.value, pdfUris, context)
//           if (!uploaded) {
//               // Handle upload failure (show error to user)
//               return
//           }
//        }
//        stopReceivingResults = false
//        if (getMessagesByConversation(_currentConversation.value).isEmpty()) {
//            createConversationRemote(message)
//        }
//
//        val newMessageModel = MessageModel(
//            id = messageId,
//            question = message,
//            answer = "Let me think...",
//            conversationId = _currentConversation.value,
//            text = message, // or whatever text you want to store
//            sender = SenderType.USER, // or SenderType.ASSISTANT as appropriate
//            timestamp = System.currentTimeMillis(),
//            attachments = attachments
//        )
//
//        val currentListMessage: MutableList<MessageModel> =
//            getMessagesByConversation(_currentConversation.value).toMutableList()
//
//        // Insert message to list
//        currentListMessage.add(0, newMessageModel)
//        setMessages(currentListMessage)
//        messageRepo.createMessage(newMessageModel)
//
//        try {
//            val aiReply = openAIRepo.getAIResponseFromBackend(
//                uid = "abhilash04",
//                prompt = message,
//                model = _selectedModel.value.model,
//                chatId = _currentConversation.value,
//                image_urls = attachments.map { it.url }
//            )
//            if (aiReply.startsWith("Network error")) {
//                // Update UI with error state
//                _errorState.value = aiReply // or use a dedicated error flow/state
//            } else {
//                updateLocalAnswer(aiReply)
//                // Save to Firestore
//                // messageRepo.createMessage(newMessageModel.copy(answer = aiReply))
//            }
//        } catch (_: Exception) {
//            _errorState.value = "Network error: Unable to connect to server."
//        }
//
//
//    }
//
//    private fun createConversationRemote(title: String) {
//        val newConversation = ConversationModel(
//            id = _currentConversation.value,
//            title = title,
//            createdAt = Date(),
//        )
//
//        conversationRepo.newConversation(newConversation)
//
//        val conversations = _conversations.value.toMutableList()
//        conversations.add(0, newConversation)
//
//        _conversations.value = conversations
//    }
//
//    fun newConversation() {
//        val conversationId: String = Date().time.toString()
//
//        _currentConversation.value = conversationId
//    }
//
//    private fun getMessagesByConversation(conversationId: String): MutableList<MessageModel> {
//        if (_messages.value[conversationId] == null) return mutableListOf()
//
//        val messagesMap: HashMap<String, MutableList<MessageModel>> =
//            _messages.value.clone() as HashMap<String, MutableList<MessageModel>>
//
//        return messagesMap[conversationId]!!
//    }
//
//    suspend fun deleteConversationAndFiles(conversationId: String) {
//        // 1. Delete conversation from Firestore
//        conversationRepo.deleteConversation(conversationId)
//        // 2. Delete all messages for this conversation
//        messageRepo.deleteMessagesByConversation(conversationId)
//        // 3. Delete all pending attachments for this conversation from Room
//        pendingAttachmentRepo.clearPendingAttachments(conversationId)
//        // 4. Delete all files for this conversation from backend (Cloudinary/Firestore)
//        try {
//            val response = backendApi.deleteFilesForConversation(conversationId)
//            if (!response.isSuccessful) {
//                // Optionally handle error
//            }
//        } catch (e: Exception) {
//            // Optionally handle network error
//        }
//        // 5. Remove from local state
//        val conversations: MutableList<ConversationModel> = _conversations.value.toMutableList()
//        val conversationToRemove = conversations.find { it.id == conversationId }
//        if (conversationToRemove != null) {
//            conversations.remove(conversationToRemove)
//            _conversations.value = conversations
//        }
//    }
//
//    private suspend fun fetchMessages() {
//        if (_currentConversation.value.isEmpty() ||
//            _messages.value[_currentConversation.value] != null) return
//
//        val flow: Flow<List<MessageModel>> = messageRepo.fetchMessages(_currentConversation.value)
//
//        flow.collectLatest {
//            setMessages(it.toMutableList())
//        }
//    }
//
//    private fun updateLocalAnswer(answer: String) {
//        val currentListMessage: MutableList<MessageModel> =
//            getMessagesByConversation(_currentConversation.value).toMutableList()
//
//        currentListMessage[0] = currentListMessage[0].copy(answer = answer)
//
//        setMessages(currentListMessage)
//    }
//
//    private fun setMessages(messages: MutableList<MessageModel>) {
//        val messagesMap: HashMap<String, MutableList<MessageModel>> =
//            _messages.value.clone() as HashMap<String, MutableList<MessageModel>>
//
//        messagesMap[_currentConversation.value] = messages
//
//        _messages.value = messagesMap
//    }
//    fun stopReceivingResults() {
//        stopReceivingResults = true
//    }
//
//    private val _pendingAttachments = MutableStateFlow<List<ChatAttachment>>(emptyList())
//    val pendingAttachments: StateFlow<List<ChatAttachment>> = _pendingAttachments.asStateFlow()
//
//    init {
//        // Load pending attachments for the current conversation on ViewModel creation
//        viewModelScope.launch {
//            loadPendingAttachments(_currentConversation.value)
//        }
//    }
//
//    private suspend fun loadPendingAttachments(conversationId: String) {
//        val entities = pendingAttachmentRepo.getPendingAttachments(conversationId)
//        _pendingAttachments.value = entities.map {
//            ChatAttachment(
//                name = it.name,
//                url = it.url,
//                type = if (it.type == "IMAGE") com.example.promptpilot.models.AttachmentType.IMAGE else com.example.promptpilot.models.AttachmentType.PDF
//            )
//        }
//    }
//
//    fun addAttachment(attachment: ChatAttachment) {
//        _pendingAttachments.update { it + attachment }
//        // Persist in Room
//        viewModelScope.launch {
//            pendingAttachmentRepo.insertPendingAttachment(
//                PendingAttachmentEntity(
//                    conversationId = _currentConversation.value,
//                    name = attachment.name,
//                    url = attachment.url,
//                    type = if (attachment.type == com.example.promptpilot.models.AttachmentType.IMAGE) "IMAGE" else "PDF"
//                )
//            )
//        }
//    }
//
//    fun removeAttachment(attachment: ChatAttachment) {
//        _pendingAttachments.update { it - attachment }
//        // Remove from Room
//        viewModelScope.launch {
//            val entities = pendingAttachmentRepo.getPendingAttachments(_currentConversation.value)
//            val entity = entities.find { it.url == attachment.url && it.name == attachment.name }
//            if (entity != null) {
//                pendingAttachmentRepo.deletePendingAttachment(entity)
//            }
//        }
//    }
//
//    fun clearPendingAttachments() {
//        _pendingAttachments.value = emptyList()
//        // Clear from Room
//        viewModelScope.launch {
//            pendingAttachmentRepo.clearPendingAttachments(_currentConversation.value)
//        }
//    }
//}