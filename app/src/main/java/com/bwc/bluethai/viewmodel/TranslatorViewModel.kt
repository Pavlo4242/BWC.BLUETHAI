package com.bwc.bluethai.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bwc.bluethai.BuildConfig
import com.bwc.bluethai.data.model.SessionPreview
import com.bwc.bluethai.data.model.TranslationEntry
import com.bwc.bluethai.data.repository.TranslationRepository
import com.bwc.bluethai.services.RecognitionState
import com.bwc.bluethai.services.SpeechRecognizerService
import com.bwc.bluethai.services.TextToSpeechService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.first
import com.bwc.bluethai.data.local.AppDatabase
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.nio.channels.FileChannel

val availableApiKeys = mapOf(
    "Key 1" to BuildConfig.GEMINI_API_KEY,
    "Key 2" to BuildConfig.GEMINI_API_KEY_DEBUG_1,
    "Key 3" to BuildConfig.GEMINI_API_KEY_DEBUG_2,
    "Key 4" to BuildConfig.GEMINI_API_KEY_DEBUG_3
)

data class ModelSelectionState(
    val version: Float = 1.5f,
    val isPro: Boolean = false
) {
    fun getModelName(): String {
        val type = if (isPro) "pro" else "flash"
        return "gemini-$version-$type"
    }
}

enum class PromptStyle {
    PATTAYA,
    VULGAR,
    HISO,
    DIRECT
}
enum class InputMode { HOLD, TAP }

sealed class TranslatorUiState {
    data object Loading : TranslatorUiState()
    data class Success(
        val baseFontSize: Int = 18,
        val currentApiKeyName: String = "Key 1",
        val currentEntries: List<TranslationEntry> = emptyList(),
        val currentSessionId: Long? = null,
        val debugLogs: List<String> = emptyList(),
        val error: String? = null,
        val inputMode: InputMode = InputMode.HOLD,
        val interimText: String = "",
        val isInputEnglish: Boolean = true,
        val isListening: Boolean = false,
        val isPlaybackEnabled: Boolean = true,
        val modelSelection: ModelSelectionState = ModelSelectionState(),
        val sessions: List<SessionPreview> = emptyList(),
        val streamingTranslation: Pair<String, String>? = null,
        //val useCustomPrompt: Boolean = false,
        val promptStyle: PromptStyle = PromptStyle.PATTAYA,
        val isTtsReady: Boolean = false, // Add this
        val showSettingsDialog: Boolean = false
    ) : TranslatorUiState()


}



@OptIn(ExperimentalCoroutinesApi::class)
class TranslatorViewModel(application: Application) : ViewModel() {

    private val repository = TranslationRepository(application)
    private val speechRecognizer = SpeechRecognizerService(application)

    private val _isTtsReady = MutableStateFlow(false)
    private val textToSpeech = TextToSpeechService(application) { isSuccess, isThaiSupported, isEnglishSupported ->
        _isTtsReady.value = isSuccess && isThaiSupported && isEnglishSupported
    }

    private val _showHistoryDialog = MutableStateFlow(false)
    val showHistoryDialog: StateFlow<Boolean> = _showHistoryDialog.asStateFlow()

    private val logFileName = "app_logs.txt"
    private val logFile: File by lazy { File(application.applicationContext.filesDir, logFileName) }
    private val appDatabase: AppDatabase by lazy { AppDatabase.getDatabase(application) }
    private val databaseName = "bluethai-db"

    private val _internalState = MutableStateFlow(TranslatorUiState.Success())
    private val _currentSessionId = MutableStateFlow<Long?>(null)
    private var translationJob: Job? = null

    private val entriesForCurrentSession: StateFlow<List<TranslationEntry>> = _currentSessionId
        .filterNotNull()
        .flatMapLatest { sessionId ->
            repository.getEntriesForSession(sessionId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val sessionsWithPreviews: StateFlow<List<SessionPreview>> =
        repository.getSessionsWithPreviews()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<TranslatorUiState> = combine(
        _internalState,
        _currentSessionId,
        entriesForCurrentSession,
        sessionsWithPreviews,
        _isTtsReady
    ) { internalState, sessionId, entries, sessions, isTtsReady ->
        internalState.copy(
            currentSessionId = sessionId,
            currentEntries = entries,
            sessions = sessions,
            debugLogs = getLogsFromFile(),
            isTtsReady = isTtsReady
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TranslatorUiState.Loading
    )

    init {
        viewModelScope.launch {
            speechRecognizer.recognitionState.collect { state ->
                handleRecognitionState(state)
            }
        }
        viewModelScope.launch {
            val sessions = repository.getAllSessions().first()
            if(sessions.isEmpty()) {
                startNewSession()
            } else {
                loadSession(sessions.first().id)
            }
            // Initial model setup
            reinitializeModelWithCurrentState()
        }
    }



    // --- HELPER FUNCTIONS ---
    private fun getSystemPrompt(state: TranslatorUiState.Success): String {
        val isEnglishInput = state.isInputEnglish

        return when (state.promptStyle) {
            PromptStyle.PATTAYA -> if (isEnglishInput) TranslationRepository.PATTAYA_PROMPT_TO_THAI else TranslationRepository.PATTAYA_PROMPT_TO_ENGLISH
            PromptStyle.HISO -> if (isEnglishInput) TranslationRepository.HISO_PROMPT_TO_THAI else TranslationRepository.HISO_PROMPT_TO_ENGLISH
            PromptStyle.VULGAR -> if (isEnglishInput) TranslationRepository.VULGAR_TO_THAI else TranslationRepository.VULGAR_TO_ENGLISH
            PromptStyle.DIRECT -> if (isEnglishInput) TranslationRepository.DIRECT_PROMPT_TO_THAI else TranslationRepository.DIRECT_PROMPT_TO_ENGLISH
        }
    }
    /*private fun getSystemPrompt(state: TranslatorUiState.Success): String {
        return when {
            state.isInputEnglish && !state.useCustomPrompt -> TranslationRepository.PATTAYA_PROMPT_TO_THAI
            !state.isInputEnglish && !state.useCustomPrompt -> TranslationRepository.PATTAYA_PROMPT_TO_ENGLISH
            state.isInputEnglish && state.useCustomPrompt -> TranslationRepository.PIRATE_PROMPT_TO_THAI
            else -> TranslationRepository.PIRATE_PROMPT_TO_ENGLISH
        }
    }
*/
    private fun reinitializeModelWithCurrentState() {
        viewModelScope.launch {
            val currentState = _internalState.first() // Get the most recent state
            val apiKey = availableApiKeys[currentState.currentApiKeyName] ?: ""
            val modelName = currentState.modelSelection.getModelName()
            val prompt = getSystemPrompt(currentState)

            repository.reinitializeModel(apiKey, modelName, prompt)
            logToFile("Model re-initialized. Prompt: ${prompt.lines().firstOrNull()}")
        }
    }

    // --- RECOGNITION AND TRANSLATION ---

    private fun handleRecognitionState(state: RecognitionState) {
        _internalState.update {
            when (state) {
                is RecognitionState.Listening -> it.copy(isListening = true, interimText = "")
                is RecognitionState.Idle -> it.copy(isListening = false)
                is RecognitionState.PartialResult -> it.copy(interimText = state.text)
                is RecognitionState.FinalResult -> {
                    if (state.text.isNotBlank()) {
                        processFinalTranscript(state.text)
                    }
                    it.copy(isListening = false, interimText = "")
                }
                is RecognitionState.Error -> it.copy(error = state.message, isListening = false)
            }
        }
    }

    private fun processFinalTranscript(text: String) {
        translationJob?.cancel()
        translationJob = viewModelScope.launch(Dispatchers.IO) {
            val currentState = _internalState.value
            val sourceText = text
            var translatedText = ""

            _internalState.update { it.copy(streamingTranslation = sourceText to "") }

            try {
                // The model is already configured. Just call translateText.
                repository.translateText(text).collect { streamedText ->
                    translatedText = streamedText
                    _internalState.update { it.copy(streamingTranslation = sourceText to translatedText) }
                }

                val sessionId = _currentSessionId.value ?: return@launch
                val newEntry = TranslationEntry(
                    sessionId = sessionId,
                    englishText = if (currentState.isInputEnglish) sourceText else translatedText,
                    thaiText = if (currentState.isInputEnglish) translatedText else sourceText,
                    isFromEnglish = currentState.isInputEnglish
                )
                repository.saveTranslationEntry(newEntry)

                if (currentState.isPlaybackEnabled) {
                    speak(translatedText, !currentState.isInputEnglish)
                }

            } catch (e: Exception) {
                _internalState.update { it.copy(error = "Translation failed: ${e.message}") }
            } finally {
                _internalState.update { it.copy(streamingTranslation = null) }
            }
        }
    }

    // --- PUBLIC ACTIONS FROM UI ---
    fun exportLogs(context: Context) {
        viewModelScope.launch {
            val logFile = File(context.filesDir, logFileName)
            if (!logFile.exists()) {
                Toast.makeText(context, "No logs found to export.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            try {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${BuildConfig.APPLICATION_ID}.fileprovider", // Authority from your manifest
                    logFile
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "App Logs")
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                // Create a chooser intent and start it from the provided context
                val chooserIntent = Intent.createChooser(shareIntent, "Share Logs")
                chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Needed when starting from a non-Activity context
                context.startActivity(chooserIntent)

            } catch (e: Exception) {
                Toast.makeText(context, "Could not share logs: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("ViewModelLogExport", "Error sharing logs: ${e.message}")
            }
        }
    }

    fun swapLanguage() {
        Log.i("AppLog", "swapLanguage")
        _internalState.update { it.copy(isInputEnglish = !it.isInputEnglish) }
        reinitializeModelWithCurrentState() // Re-initialize model on language swap
    }

    fun setApiKey(keyName: String) {
        Log.i("AppLog", "setApiKey: $keyName")
        _internalState.update { it.copy(currentApiKeyName = keyName) }
        reinitializeModelWithCurrentState()
    }

/*
    fun setUseCustomPrompt(useCustom: Boolean) {
        Log.i("AppLog", "setUseCustomPrompt: $useCustom")
        _internalState.update { it.copy(useCustomPrompt = useCustom) }
        reinitializeModelWithCurrentState()
    }
*/

    fun setPromptStyle(style: PromptStyle) {
        Log.i("AppLog", "setPromptStyle: $style")
        _internalState.update { it.copy(promptStyle = style) }
        reinitializeModelWithCurrentState() // This re-initializes the model with the new prompt
    }
    fun updateModelSelection(newSelection: ModelSelectionState) {
        Log.i("AppLog", "updateModelSelection: $newSelection")
        _internalState.update { it.copy(modelSelection = newSelection) }
        reinitializeModelWithCurrentState()
    }

    fun setInputMode(mode: InputMode) {
        Log.i("AppLog", "setInputMode: $mode")
        _internalState.update { it.copy(inputMode = mode) }
    }

    fun clearError() {
        Log.i("AppLog", "clearError")
        _internalState.update { it.copy(error = null) }
    }

    fun setFontSize(size: Int) {
        Log.i("AppLog", "setFontSize: $size")
        _internalState.update { it.copy(baseFontSize = size) }
    }

    fun setPlaybackEnabled(isEnabled: Boolean) {
        Log.i("AppLog", "setPlaybackEnabled: $isEnabled")
        _internalState.update { it.copy(isPlaybackEnabled = isEnabled) }
    }

    fun startListening() {
        Log.i("AppLog", "startListening")
        speechRecognizer.startListening(_internalState.value.isInputEnglish)
    }

    fun stopListening() {
        Log.i("AppLog", "stopListening")
        speechRecognizer.stopListening()
    }

    fun toggleListening() {
        Log.i("AppLog", "toggleListening")
        if (_internalState.value.isListening) stopListening() else startListening()
    }

    fun speak(text: String, isEnglish: Boolean) {
        Log.i("AppLog", "speak: text=$text, isEnglish=$isEnglish")
        if (_internalState.value.isPlaybackEnabled) {
            textToSpeech.speak(text, isEnglish)
        }
    }

    fun toggleHistoryDialog(show: Boolean) {
        Log.i("AppLog", "toggleHistoryDialog: $show")
        _showHistoryDialog.value = show
    }

    fun loadSession(sessionId: Long) {
        Log.i("AppLog", "loadSession: $sessionId")
        _currentSessionId.value = sessionId
        toggleHistoryDialog(false)
    }

    fun startNewSession() {
        Log.i("AppLog", "startNewSession")
        viewModelScope.launch {
            val newId = withContext(Dispatchers.IO) { repository.startNewSession() }
            _currentSessionId.value = newId
            toggleHistoryDialog(false)
        }
    }

    fun deleteSession(sessionId: Long) {
        Log.i("AppLog", "deleteSession: $sessionId")
        viewModelScope.launch {
            if (_currentSessionId.value == sessionId) {
                val sessionsAfterDeletion = repository.getAllSessions().first().filter { it.id != sessionId }
                if (sessionsAfterDeletion.isNotEmpty()) {
                    loadSession(sessionsAfterDeletion.first().id)
                } else {
                    startNewSession()
                }
            }
            withContext(Dispatchers.IO) { repository.deleteSession(sessionId) }
        }
    }


    // Custom Logging Function
    private fun logToFile(message: String) {
        viewModelScope.launch {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val logEntry = "$timestamp: $message\n"

            try {
                FileOutputStream(logFile, true).bufferedWriter().use { writer ->
                    writer.append(logEntry)
                }
                // Update the logs in the state
                _internalState.update {
                    it.copy(debugLogs = getLogsFromFile())
                }


                Log.d("AppLog", "Logged: $message")
            } catch (e: IOException) {
                Log.e("AppLog", "Failed to write to log file: ${e.message}")
            }
        }
    }

    // Retrieve Logs from File
    private fun getLogsFromFile(): List<String> {
        return try {
            if (!logFile.exists()) {
                return emptyList()
            }
            logFile.readLines()
        } catch (e: IOException) {
            Log.e("AppLog", "Failed to read from log file: ${e.message}")
            emptyList()
        }
    }

    // ADD the database backup call
    fun backupDatabase(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dbFile = context.getDatabasePath(databaseName)
                val backupDir = context.filesDir // Internal storage
                val backupFile = File(backupDir, "translator_database_backup.db")

                if (!dbFile.exists()) {
                    logToFile("Database file not found.")
                    return@launch
                }

                // Copy the database
                copyFile(FileInputStream(dbFile).channel, FileOutputStream(backupFile).channel)
                logToFile("Database backed up successfully to ${backupFile.absolutePath}")

            } catch (e: Exception) {
                logToFile("Database backup failed: ${e.message}")
                Log.e("DatabaseBackup", "Error during backup", e)

            }
        }
    }

    // Helper function to copy file
    @Throws(IOException::class)
    private fun copyFile(source: FileChannel, destination: FileChannel) {
        destination.transferFrom(source, 0, source.size())
        source.close()
        destination.close()
    }

    // --- Lifecycle and Factory ---

    override fun onCleared() {
        Log.i("AppLog", "onCleared called")
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