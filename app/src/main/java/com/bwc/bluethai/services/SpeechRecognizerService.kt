package com.bwc.bluethai.services

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class RecognitionState {
    data object Idle : RecognitionState()
    data object Listening : RecognitionState()
    data class PartialResult(val text: String) : RecognitionState()
    data class FinalResult(val text: String) : RecognitionState()
    data class Error(val message: String) : RecognitionState()
}

class SpeechRecognizerService(private val context: Context) {

    private val _recognitionState = MutableStateFlow<RecognitionState>(RecognitionState.Idle)
    val recognitionState: StateFlow<RecognitionState> = _recognitionState.asStateFlow()

    private val speechRecognizer: SpeechRecognizer? = if (SpeechRecognizer.isRecognitionAvailable(context)) {
        SpeechRecognizer.createSpeechRecognizer(context)
    } else {
        null
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _recognitionState.value = RecognitionState.Listening
        }
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {
            _recognitionState.value = RecognitionState.Idle
        }
        override fun onError(error: Int) {
            val message = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech input"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                else -> "Recognition error (code: $error)"
            }

            if (error != SpeechRecognizer.ERROR_NO_MATCH &&
                error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                _recognitionState.value = RecognitionState.Error(message)
            }
            _recognitionState.value = RecognitionState.Idle
        }
        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                _recognitionState.value = RecognitionState.FinalResult(matches[0])
            }
        }
        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                _recognitionState.value = RecognitionState.PartialResult(matches[0])
            }
        }
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    init {
        speechRecognizer?.setRecognitionListener(recognitionListener)
        if (speechRecognizer == null) {
            Log.e("SpeechRecognizer", "Speech recognition not available")
        }
    }

    fun startListening(isEnglish: Boolean) {
        if (speechRecognizer == null) {
            _recognitionState.value = RecognitionState.Error("Speech recognizer not available")
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (isEnglish) "en-US" else "th-TH")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)

            // Disable profanity filter using different methods
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                putExtra("android.speech.extra.PROFANITY_FILTER", false)  // Hidden API
            }
            putExtra("profanity_filter", false)  // Alternative key some devices use

        }

        try {
            speechRecognizer.startListening(intent)
        } catch (e: Exception) {
            _recognitionState.value = RecognitionState.Error("Recognition failed: ${e.localizedMessage}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            _recognitionState.value = RecognitionState.Error("Stop failed: ${e.localizedMessage}")
        } finally {
            _recognitionState.value = RecognitionState.Idle
        }
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            // Log destruction error if needed
        }
    }
}