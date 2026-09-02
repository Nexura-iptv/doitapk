package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.AuthManager
import com.example.auth.SubscriptionManager
import com.example.auth.UserProfile
import com.example.data.local.ChatMessage
import com.example.data.local.ChatSession
import com.example.data.local.NovaDatabase
import com.example.data.model.AiModelInfo
import com.example.data.model.AvailableModels
import com.example.data.model.SubscriptionPlan
import com.example.data.network.GroqClient
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NovaAiViewModel(application: Application) : AndroidViewModel(application) {

    private val database = NovaDatabase.getDatabase(application)
    private val groqClient = GroqClient(application)
    val authManager = AuthManager(application)
    val subscriptionManager = SubscriptionManager(application)

    val repository = ChatRepository(
        sessionDao = database.chatSessionDao(),
        messageDao = database.chatMessageDao(),
        groqClient = groqClient
    )

    // Current Active Subscription Plan
    val currentPlan: StateFlow<SubscriptionPlan> = subscriptionManager.currentPlan

    // Chat Sessions list
    val allSessions: StateFlow<List<ChatSession>> = repository.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Active session ID
    private val _currentSessionId = MutableStateFlow<Long?>(null)
    val currentSessionId: StateFlow<Long?> = _currentSessionId.asStateFlow()

    // Current Messages for the selected session
    val currentMessages: StateFlow<List<ChatMessage>> = _currentSessionId
        .flatMapLatest { sessionId ->
            if (sessionId != null) {
                repository.getMessagesForSession(sessionId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Model selection
    private val _selectedModel = MutableStateFlow<AiModelInfo>(AvailableModels.defaultModel)
    val selectedModel: StateFlow<AiModelInfo> = _selectedModel.asStateFlow()

    // Input text
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    // Attached Image
    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    // Loading status
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    // Errors
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Auth Profile State
    val currentUser: StateFlow<UserProfile> = authManager.currentUser

    // Dialog Visibilities
    private val _showAuthDialog = MutableStateFlow(false)
    val showAuthDialog: StateFlow<Boolean> = _showAuthDialog.asStateFlow()

    private val _showProfileDialog = MutableStateFlow(false)
    val showProfileDialog: StateFlow<Boolean> = _showProfileDialog.asStateFlow()

    private val _showModelSelector = MutableStateFlow(false)
    val showModelSelector: StateFlow<Boolean> = _showModelSelector.asStateFlow()

    private val _showPlanDialog = MutableStateFlow(false)
    val showPlanDialog: StateFlow<Boolean> = _showPlanDialog.asStateFlow()

    private val _customizingPlan = MutableStateFlow<SubscriptionPlan?>(null)
    val customizingPlan: StateFlow<SubscriptionPlan?> = _customizingPlan.asStateFlow()

    init {
        // Create initial session if none exists or select most recent
        viewModelScope.launch {
            allSessions.collect { sessions ->
                if (_currentSessionId.value == null && sessions.isNotEmpty()) {
                    _currentSessionId.value = sessions.first().id
                }
            }
        }
    }

    fun onInputTextChanged(text: String) {
        _inputText.value = text
    }

    fun onImageSelected(uri: Uri?) {
        _selectedImageUri.value = uri
        // If image attached and current model is not vision, switch to vision preview
        if (uri != null && !_selectedModel.value.isVisionSupported) {
            _selectedModel.value = AvailableModels.LLAMA_3_2_11B_VISION
        }
    }

    fun onClearImage() {
        _selectedImageUri.value = null
    }

    fun onSelectModel(model: AiModelInfo) {
        _selectedModel.value = model
    }

    fun startNewChat() {
        viewModelScope.launch {
            val newId = repository.createNewSession(
                title = "Yeni Sohbet",
                modelId = _selectedModel.value.id
            )
            _currentSessionId.value = newId
            _selectedImageUri.value = null
            _inputText.value = ""
        }
    }

    fun selectSession(sessionId: Long) {
        _currentSessionId.value = sessionId
        viewModelScope.launch {
            val session = repository.getSessionById(sessionId)
            if (session != null) {
                val model = AvailableModels.allModels.find { it.id == session.modelId }
                    ?: AvailableModels.defaultModel
                _selectedModel.value = model
            }
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (_currentSessionId.value == sessionId) {
                val remaining = allSessions.value.filter { it.id != sessionId }
                if (remaining.isNotEmpty()) {
                    _currentSessionId.value = remaining.first().id
                } else {
                    _currentSessionId.value = null
                }
            }
        }
    }

    fun sendMessage() {
        val prompt = _inputText.value.trim()
        val imageUri = _selectedImageUri.value

        if (prompt.isBlank() && imageUri == null) return
        if (_isGenerating.value) return

        viewModelScope.launch {
            _isGenerating.value = true
            _errorMessage.value = null
            _inputText.value = ""
            _selectedImageUri.value = null

            var sessionId = _currentSessionId.value
            if (sessionId == null) {
                sessionId = repository.createNewSession(
                    title = if (prompt.isNotBlank()) {
                        if (prompt.length > 28) prompt.take(28) + "…" else prompt
                    } else "Görsel Analizi",
                    modelId = _selectedModel.value.id
                )
                _currentSessionId.value = sessionId
            }

            val result = repository.sendMessage(
                sessionId = sessionId,
                modelId = _selectedModel.value.id,
                userPrompt = prompt,
                imageUri = imageUri
            )

            _isGenerating.value = false
            if (result.isFailure) {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Mesaj gönderilemedi"
            }
        }
    }

    fun retryLastMessage(prompt: String, imageUri: Uri? = null) {
        _inputText.value = prompt
        _selectedImageUri.value = imageUri
        sendMessage()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // Auth actions
    fun setShowAuthDialog(show: Boolean) {
        _showAuthDialog.value = show
    }

    fun setShowProfileDialog(show: Boolean) {
        _showProfileDialog.value = show
    }

    fun setShowModelSelector(show: Boolean) {
        _showModelSelector.value = show
    }

    fun setShowPlanDialog(show: Boolean) {
        _showPlanDialog.value = show
    }

    fun selectPlanAndBuy(plan: SubscriptionPlan) {
        // 1. Open brandofpaper.ikas.shop in external browser
        subscriptionManager.openWebsite(plan)
        // 2. Close plan dialog and trigger customized transition screen
        _showPlanDialog.value = false
        _customizingPlan.value = plan
    }

    fun onPlanCustomizationFinished() {
        val plan = _customizingPlan.value
        if (plan != null) {
            subscriptionManager.activatePlan(plan)
        }
        _customizingPlan.value = null
    }

    fun signIn(
        username: String,
        email: String,
        password: String = "",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = authManager.signIn(username, email, password)
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.message ?: "Giriş yapılamadı")
            }
        }
    }

    fun signOut() {
        authManager.signOut()
    }
}
