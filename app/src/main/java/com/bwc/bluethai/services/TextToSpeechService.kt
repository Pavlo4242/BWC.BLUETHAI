package com.bwc.bluethai.services

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TextToSpeechService(context: Context, private val onInit: (isSuccess: Boolean, isThaiSupported: Boolean, isEnglishSupported: Boolean) -> Unit) {
    private var tts: TextToSpeech? = null
    private var isReady = false
    private val thaiLocale = Locale("th", "TH")
    private val usLocale = Locale.US

    // Store language support status
    var isThaiSupported: Boolean = false
        private set
    var isEnglishSupported: Boolean = false
        private set

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isReady = true
                Log.d("TTS", "TextToSpeech engine initialized successfully.")

                // Check language availability and store it
                isThaiSupported = tts?.isLanguageAvailable(thaiLocale) ?: 0 >= TextToSpeech.LANG_AVAILABLE
                isEnglishSupported = tts?.isLanguageAvailable(usLocale) ?: 0 >= TextToSpeech.LANG_AVAILABLE

                Log.d("TTS", "Thai available: $isThaiSupported, US English available: $isEnglishSupported")
                onInit(true, isThaiSupported, isEnglishSupported)
            } else {
                Log.e("TTS", "Failed to initialize TextToSpeech engine.")
                onInit(false, false, false)
            }
        }
    }

    fun speak(text: String, isEnglish: Boolean) {
        if (!isReady) {
            Log.e("TTS", "Engine not ready")
            return
        }
        val locale = if (isEnglish) usLocale else thaiLocale
        val availability = tts?.isLanguageAvailable(locale)
        if (availability == null || availability < TextToSpeech.LANG_AVAILABLE) {
            Log.e("TTS", "Language not available: ${locale.language}, Availability: $availability")
            return
        }
        tts?.language = locale
        val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        if (result == TextToSpeech.ERROR) {
            Log.e("TTS", "Error occurred while speaking text: $text")
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}