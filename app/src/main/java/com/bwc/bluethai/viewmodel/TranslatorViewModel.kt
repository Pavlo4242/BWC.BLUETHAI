package com.bwc.translator.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bwc.translator.BuildConfig
import com.bwc.translator.data.model.ConversationSession
import com.bwc.translator.data.model.TranslationEntry
import com.bwc.translator.data.repository.TranslationRepository
import com.bwc.translator.services.RecognitionState
import com.bwc.translator.services.SpeechRecognizerService
import com.bwc.translator.services.TextToSpeechService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

val availableApiKeys = mapOf(
    "Key 1" to BuildConfig.GEMINI_API_KEY,
    "Key 2" to BuildConfig.GEMINI_API_KEY_DEBUG_1,
    "Key 3" to BuildConfig.GEMINI_API_KEY_DEBUG_2,
    "Key 4" to BuildConfig.GEMINI_API_KEY_DEBUG_3
)

enum class InputMode { HOLD, TAP }

data class ModelSelectionState(
    val version: Float = 1.5f,
    val isPro: Boolean = false
) {
    fun getModelName(): String {
        val type = if (isPro) "pro" else "flash"
        return "gemini-$version-$type"
    }
}

sealed class TranslatorUiState {
    data object Loading : TranslatorUiState()
    data class Success(
        val isInputEnglish: Boolean = true,
        val isListening: Boolean = false,
        val isPlaybackEnabled: Boolean = true,
        val inputMode: InputMode = InputMode.HOLD,
        val currentSessionId: Long? = null,
        val currentEntries: List<TranslationEntry> = emptyList(),
        val sessions: List<Pair<ConversationSession, String>> = emptyList(),
        val interimText: String = "",
        val streamingTranslation: Pair<String, String>? = null,
        val error: String? = null,
        val currentApiKeyName: String = "Key 1",
        val debugLogs: List<String> = emptyList(),
        val baseFontSize: Int = 18,
        val useCustomPrompt: Boolean = false,
        val modelSelection: ModelSelectionState = ModelSelectionState()
    ) : TranslatorUiState()
}

@OptIn(ExperimentalCoroutinesApi::class)
class TranslatorViewModel(application: Application) : ViewModel() {

    private val repository = TranslationRepository(application)
    private val speechRecognizer = SpeechRecognizerService(application)
    private val textToSpeech = TextToSpeechService(application) { /* TTS init status */ }

    private val _internalState = MutableStateFlow<TranslatorUiState>(TranslatorUiState.Success())
    private var translationJob: Job? = null
    private var lastInteractionTime = System.currentTimeMillis()
    private val SESSION_TIMEOUT = 5 * 60 * 1000 // 5 minutes

    private val sessionsWithPreviews: Flow<List<Pair<ConversationSession, String>>> =
        repository.getAllSessions().flatMapLatest { sessions ->
            if (sessions.isEmpty()) {
                flowOf(emptyList())
            } else {
                val previewFlows = sessions.map { session ->
                    repository.getEntriesForSession(session.id).map { entries ->
                        val preview = entries.firstOrNull()?.englishText ?: "Empty Chat"
                        session to preview
                    }
                }
                combine(previewFlows) { previews -> previews.toList() }
            }
        }

    val uiState: StateFlow<TranslatorUiState> = combine(
        _internalState,
        sessionsWithPreviews
    ) { state, sessions ->
        if (state is TranslatorUiState.Success) {
            state.copy(sessions = sessions)
        } else {
            state
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TranslatorUiState.Loading
    )

    private val _showHistoryDialog = MutableStateFlow(false)
    val showHistoryDialog: StateFlow<Boolean> = _showHistoryDialog.asStateFlow()

    init {
        viewModelScope.launch {
            _internalState
                .mapNotNull { state ->
                    (state as? TranslatorUiState.Success)?.let {
                        Triple(it.currentApiKeyName, it.useCustomPrompt, it.isInputEnglish) to it.modelSelection
                    }
                }
                .distinctUntilChanged()
                .collect { (config, modelSelection) ->
                    val (keyName, useCustom, isEnglish) = config
                    val apiKey = availableApiKeys[keyName] ?: ""
                    val modelName = modelSelection.getModelName()
                    val systemInstruction = if (useCustom) {
                        if (isEnglish) TranslationRepository.PIRATE_PROMPT_TO_THAI else TranslationRepository.PIRATE_PROMPT_TO_ENGLISH
                    } else {
                        if (isEnglish) TranslationRepository.PATTAYA_PROMPT_TO_THAI else TranslationRepository.PATTAYA_PROMPT_TO_ENGLISH
                    }
                    addDebugLog("MODEL REINITIALIZED. Name: $modelName, Prompt: ${if(useCustom) "Pirate" else "Pattaya"}, Language: ${if(isEnglish) "EN->TH" else "TH->EN"}")
                    repository.reinitializeModel(apiKey, modelName, systemInstruction)
                }
        }

        viewModelScope.launch {
            _internalState
                .mapNotNull { (it as? TranslatorUiState.Success)?.currentSessionId }
                .distinctUntilChanged()
                .collect { sessionId ->
                    repository.getEntriesForSession(sessionId).collect { entries ->
                        val currentState = _internalState.value
                        if (currentState is TranslatorUiState.Success) {
                            _internalState.update { currentState.copy(currentEntries = entries) }
                        }
                    }
                }
        }
        viewModelScope.launch {
            speechRecognizer.recognitionState.collect { state ->
                handleRecognitionState(state)
            }
        }
        viewModelScope.launch {
            val sessions = repository.getAllSessions().first()
            if (sessions.isEmpty()) {
                startNewSession()
            } else {
                if ((_internalState.value as? TranslatorUiState.Success)?.currentSessionId == null) {
                    loadSession(sessions.first().id)
                }
            }
        }
    }

    private fun addDebugLog(log: String) {
        val currentState = _internalState.value
        if (currentState is TranslatorUiState.Success) {
            val updatedLogs = listOf(log) + currentState.debugLogs
            _internalState.update { currentState.copy(debugLogs = updatedLogs) }
        }
    }

    private fun handleRecognitionState(state: RecognitionState) {
        val uiSuccessState = _internalState.value as? TranslatorUiState.Success ?: return
        when (state) {
            is RecognitionState.Listening -> _internalState.update { uiSuccessState.copy(isListening = true, interimText = "") }
            is RecognitionState.Idle -> _internalState.update { uiSuccessState.copy(isListening = false) }
            is RecognitionState.PartialResult -> _internalState.update { uiSuccessState.copy(interimText = state.text) }
            is RecognitionState.FinalResult -> {
                if (uiSuccessState.inputMode == InputMode.HOLD) {
                    _internalState.update { uiSuccessState.copy(isListening = false) }
                }
                _internalState.update { uiSuccessState.copy(interimText = "") }
                if (state.text.isNotBlank()) {
                    processFinalTranscript(state.text)
                }
            }
            is RecognitionState.Error -> _internalState.update { uiSuccessState.copy(error = state.message, isListening = false) }
        }
    }

    private fun processFinalTranscript(text: String) {
        translationJob?.cancel()
        translationJob = viewModelScope.launch {
            if (System.currentTimeMillis() - lastInteractionTime > SESSION_TIMEOUT) {
                startNewSession()
            }

            val currentState = _internalState.value as? TranslatorUiState.Success ?: return@launch
            val sourceText = text
            var translatedText = ""

            _internalState.update { currentState.copy(streamingTranslation = sourceText to "") }
            try {
                // --- START OF FIX ---
                // Determine which system prompt is being used for logging purposes
                val systemPrompt = if (currentState.useCustomPrompt) {
                    if (currentState.isInputEnglish) TranslationRepository.PIRATE_PROMPT_TO_THAI else TranslationRepository.PIRATE_PROMPT_TO_ENGLISH
                } else {
                    if (currentState.isInputEnglish) TranslationRepository.PATTAYA_PROMPT_TO_THAI else TranslationRepository.PATTAYA_PROMPT_TO_ENGLISH
                }

                val fullRequestLog = """
                    --- REQUEST TO AI ---
                    System Prompt:
                    $systemPrompt
                    
                    User Input:
                    $sourceText
                """.trimIndent()
                addDebugLog(fullRequestLog)
                // --- END OF FIX ---

                repository.translateText(text).collect { streamedText ->
                    translatedText = streamedText
                    _internalState.update { currentState.copy(streamingTranslation = sourceText to translatedText) }
                }
                addDebugLog("--- AI RESPONSE ---\n$translatedText")

                val newEntry = TranslationEntry(
                    sessionId = currentState.currentSessionId ?: return@launch,
                    englishText = if (currentState.isInputEnglish) sourceText else translatedText,
                    thaiText = if (currentState.isInputEnglish) translatedText else sourceText,
                    isFromEnglish = currentState.isInputEnglish
                )
                repository.saveTranslationEntry(newEntry)

                val updatedEntries = currentState.currentEntries + newEntry
                _internalState.update {
                    currentState.copy(
                        currentEntries = updatedEntries,
                        streamingTranslation = null
                    )
                }
                lastInteractionTime = System.currentTimeMillis()
                speak(translatedText, !currentState.isInputEnglish)

            } catch (e: Exception) {
                val errorMsg = "Translation failed: ${e.message}"
                addDebugLog("ERROR:\n$errorMsg")
                _internalState.update { currentState.copy(error = errorMsg, streamingTranslation = null) }
            }
        }
    }

    // --- Public actions from UI ---

    fun updateModelSelection(newSelection: ModelSelectionState) {
        val currentState = _internalState.value
        if (currentState is TranslatorUiState.Success) {
            _internalState.update { currentState.copy(modelSelection = newSelection) }
        }
    }

    fun setInputMode(mode: InputMode) {
        val currentState = _internalState.value
        if (currentState is TranslatorUiState.Success) {
            _internalState.update { currentState.copy(inputMode = mode) }
        }
    }
    fun setApiKey(keyName: String) {
        val currentState = _internalState.value
        if (currentState is TranslatorUiState.Success) {
            _internalState.update { currentState.copy(currentApiKeyName = keyName) }
        }
    }
    fun setFontSize(size: Int) {
        val currentState = _internalState.value
        if (currentState is TranslatorUiState.Success) {
            _internalState.update { currentState.copy(baseFontSize = size) }
        }
    }
    fun setUseCustomPrompt(useCustom: Boolean) {
        val currentState = _internalState.value
        if (currentState is TranslatorUiState.Success) {
            _internalState.update { currentState.copy(useCustomPrompt = useCustom) }
        }
    }
    fun startListening() {
        val isEnglish = (_internalState.value as? TranslatorUiState.Success)?.isInputEnglish ?: return
        speechRecognizer.startListening(isEnglish)
    }
    fun stopListening() = speechRecognizer.stopListening()
    fun toggleListening() {
        if ((_internalState.value as? TranslatorUiState.Success)?.isListening == true) {
            stopListening()
        } else {
            startListening()
        }
    }
    fun swapLanguage() {
        val currentState = _internalState.value
        if (currentState is TranslatorUiState.Success) {
            _internalState.update { currentState.copy(isInputEnglish = !currentState.isInputEnglish) }
        }
    }
    fun setPlaybackEnabled(isEnabled: Boolean) {
        val currentState = _internalState.value
        if (currentState is TranslatorUiState.Success) {
            _internalState.update { currentState.copy(isPlaybackEnabled = isEnabled) }
        }
    }
    fun speak(text: String, isEnglish: Boolean) {
        if ((_internalState.value as? TranslatorUiState.Success)?.isPlaybackEnabled == true) {
            textToSpeech.speak(text, isEnglish)
        }
    }
    fun clearError() {
        val currentState = _internalState.value
        if (currentState is TranslatorUiState.Success) {
            _internalState.update { currentState.copy(error = null) }
        }
    }
    fun toggleHistoryDialog(show: Boolean) {
        _showHistoryDialog.value = show
    }
    fun loadSession(sessionId: Long) {
        val currentState = _internalState.value
        if (currentState is TranslatorUiState.Success) {
            _internalState.update { currentState.copy(currentSessionId = sessionId) }
            toggleHistoryDialog(false)
        }
    }
    fun startNewSession() {
        viewModelScope.launch {
            val newId = repository.startNewSession()
            val currentState = _internalState.value
            if (currentState is TranslatorUiState.Success) {
                _internalState.update { currentState.copy(currentSessionId = newId, currentEntries = emptyList()) }
                toggleHistoryDialog(false)
            }
        }
    }
    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
        }
    }
    override fun onCleared() {
        super.onCleared()
        speechRecognizer.destroy()
        textToSpeech.shutdown()
    }
    class TranslatorViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TranslatorViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return TranslatorViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
