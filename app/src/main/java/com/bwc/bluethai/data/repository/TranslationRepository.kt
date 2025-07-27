package com.bwc.bluethai.data.repository

import com.bwc.bluethai.BuildConfig
import com.bwc.bluethai.data.*
import com.bwc.bluethai.data.model.*
import android.content.Context
import android.util.Log
import com.bwc.bluethai.data.local.AppDatabase
import com.bwc.bluethai.data.model.ConversationSession
import com.bwc.bluethai.data.model.TranslationEntry
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class TranslationRepository(context: Context) {

    companion object {
        const val PATTAYA_PROMPT_TO_ENGLISH = """
You are a real-time Thai/Isaan-to-English interpreter for Pattaya bar conversations. Translate ONLY the input text to modern, informal English following these rules:
1. **Input Languages Accepted**: Central Thai (standard or slang), Isaan dialect (Northeastern Thai/Lao-influenced).
2. **Output Constraints**: ONLY output the English translation. NO explanations, notes, or apologies. Errors (untranslatable input) → "[UNTRANSLATABLE]".
3. **Translation Principles**: Prioritize cultural equivalence. Preserve vulgarity, threats, and transactional language. Isaan terms → closest English slang. Force ambiguous phonemes into Thai/Isaan.
4. **Examples**: "เหี้ย" → "motherfucker"; "เฮ็ดส่ำใด?" → "What the fuck are you doing?"; "สัก 2,000 บาทก็พอแล้ว" → "2k baht and I'm yours."; "ควย!" → "Fuck you!".
5. **Strict Format**: Input: "[Thai/Isaan text]"; Output: "[English translation ONLY]".
"""
        const val PATTAYA_PROMPT_TO_THAI = """
You are a real-time English-to-Thai/Isaan interpreter for Pattaya bar conversations. Translate ONLY the input text to informal Thai/Isaan following these rules:
1. **Input Language Accepted**: Modern informal English.
2. **Output Constraints**: ONLY output the Thai/Isaan translation. NO explanations or notes. Errors → "[UNTRANSLATABLE]".
3. **Translation Principles**: Use aggressive pronouns (มึง/กู) and slang. English vulgarity → strongest Thai/Isaan equivalent. Transactional terms → direct Thai phrasing.
4. **Examples**: "Fuck off!" → "ไสหัวไป!"; "How much for short time?" → "ชั่วคราวเท่าไหร่?"; "You’re scum." → "มึงมันขยะ"; "Wanna get high?" → "อยากเมาป่ะ?".
5. **Strict Format**: Input: "[English text]"; Output: "[Thai/Isaan translation ONLY]".
"""
        const val VULGAR_TO_THAI = "\"\"\"\n" +
                "English→Thai/Isaan translator. Rules:\n" +
                "1. Input: English. Output: ONLY Thai/Isaan.\n" +
                "2. Use มึง/กู + strongest slang matching vulgarity.\n" +
                "3. Errors → \"[UNTRANSLATABLE]\".\n" +
                "\n" +
                "Examples:\n" +
                "\"Fuck off!\" → \"ไสหัวไป!\"\n" +
                "\"Short time?\" → \"ชั่วคราวเท่าไหร่?\"\n" +
                "\"Wanna get high?\" → \"อยากเมาป่ะ?\"\n" +
                "\n" +
                "Format: \"[input]\" → \"[output]\""
        const val VULGAR_TO_ENGLISH = "Role: Thai/Isaan-to-English bar translator.  \n" +
                "\n" +
                "Rules:  \n" +
                "1. Input: Thai/Isaan only. Output: Raw English translation.  \n" +
                "2. Preserve tone (vulgarity/threats/transactions).  \n" +
                "3. No explanations. Errors → \"[UNTRANSLATABLE]\".  \n" +
                "\n" +
                "Examples:  \n" +
                "- \"เหี้ย\" → \"motherfucker\"  \n" +
                "- \"เฮ็ดส่ำใด?\" → \"WTF are you doing?\"  \n" +
                "- \"2,000 บาท\" → \"2k baht\".  \n" +
                "\n" +
                "Format:  \n" +
                "Input: \"[text]\" → Output: \"[translation]\"\n" +
                "\"\"\""

        const val HISO_PROMPT_TO_ENGLISH = "Role: Thai/Isaan-to-English high-society interpreter.\n" +
                "\n" +
                "Rules:\n" +
                "1. Input: Thai/Isaan only. Output: Polished English translation.\n" +
                "2. Preserve formality, indirectness, and cultural nuance.\n" +
                "3. No explanations. Errors → \"[UNTRANSLATABLE]\".\n" +
                "\n" +
                "Examples:\n" +
                "- \"กรุณาอย่าใช้คำหยาบ\" → \"Kindly refrain from crude language.\"\n" +
                "- \"ท่านต้องการอะไรเพิ่มไหมครับ?\" → \"Might I offer you anything further, Sir/Madam?\"\n" +
                "- \"ขอโทษอย่างสูง\" → \"My deepest apologies.\"\n" +
                "\n" +
                "Format:\n" +
                "Input: \"[text]\" → Output: \"[translation]\""+
                "\"\"\""

        const val HISO_PROMPT_TO_THAI = "Role: English-to-Thai/Isaan high-society interpreter.\n" +
                "\n" +
                "Rules:\n" +
                "1. Input: English only. Output: Formal Thai/Isaan (if contextually elegant).\n" +
                "2. Use honorifics (ท่าน, คุณ) and royal/formal register.\n" +
                "3. No explanations. Errors → \"[UNTRANSLATABLE]\".\n" +
                "\n" +
                "Examples:\n" +
                "- \"How delightful to see you!\" → \"ยินดีอย่างยิ่งที่ได้พบคุณครับ/คะ!\"\n" +
                "- \"This is unacceptable.\" → \"นี่เป็นสิ่งที่ยอมรับไม่ได้ค่ะ/ครับ\"\n" +
                "- \"May I assist you?\" → \"ท่านต้องการความช่วยเหลือไหมคะ/ครับ?\"\n" +
                "\n" +
                "Format:\n" +
                "Input: \"[text]\" → Output: \"[translation]\""+
                "\"\"\""

        const val DIRECT_PROMPT_TO_THAI = "Translate the following English text to Thai. Output only the translation."
        const val DIRECT_PROMPT_TO_ENGLISH = "Translate the following Thai text to English. Output only the translation."

    }



    var generativeModel: GenerativeModel? = null
        private set

    private val sessionDao = AppDatabase.getDatabase(context).sessionDao()
    private val entryDao = AppDatabase.getDatabase(context).entryDao()

    fun reinitializeModel(apiKey: String, modelName: String, systemInstruction: String) {
        if (apiKey.isBlank()) {
            generativeModel = null
            return
        }
        val config = generationConfig {
            temperature = 0.7f
        }
        val safetySettings = listOf(
            SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.NONE),
            SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.NONE),
            SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.NONE),
            SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.NONE),
        )

        generativeModel = GenerativeModel(
            modelName = modelName,
            apiKey = apiKey,
            generationConfig = config,
            safetySettings = safetySettings,
            systemInstruction = content { text(systemInstruction) } // Pass the instruction here
        )
    }

    fun translateText(text: String): Flow<String> = flow {
        val model = generativeModel ?: throw IllegalStateException("GenerativeModel not initialized")

        try {
            // The model is already configured with the prompt, so just send the text.
            model.generateContentStream(text).collect { chunk ->
                chunk.text?.let {
                    emit(it)
                } ?: run {
                    Log.e("Translation", "Empty chunk received")
                }
            }
        } catch (e: Exception) {
            Log.e("Translation", "Error during translation", e)
            throw e
        }
    }.flowOn(Dispatchers.IO)


      /*  var fullResponse = ""
        try {
            model.generateContentStream(text).collect { chunk ->
                chunk.text?.let {
                    emit(it)
                } ?: run {
                    Log.e("Translation", "Empty chunk received")
                }
            }
        } catch (e: Exception) {
            Log.e("Translation", "Error during translation", e)
            throw e
        }
    }.flowOn(Dispatchers.IO)*/

    // Database operations
    fun getAllSessions(): Flow<List<ConversationSession>> = sessionDao.getAllSessions()

    fun getSessionsWithPreviews(): Flow<List<SessionPreview>> =
        sessionDao.getSessionsWithPreviews().map { sessionPreviews ->
            sessionPreviews.map {
                SessionPreview(
                    session = ConversationSession(it.id, it.startTime),
                    previewText = it.previewText ?: "No messages"
                )
            }
        }.flowOn(Dispatchers.IO)

    fun getEntriesForSession(sessionId: Long): Flow<List<TranslationEntry>> = entryDao.getEntriesForSession(sessionId)

    suspend fun startNewSession(): Long {
        val newSession = ConversationSession()
        return sessionDao.insertSession(newSession)
    }

    suspend fun saveTranslationEntry(entry: TranslationEntry) {
        entryDao.insertEntry(entry)
    }

    suspend fun deleteSession(sessionId: Long) {
        entryDao.deleteEntriesForSession(sessionId)
        sessionDao.deleteSessionById(sessionId)
    }
}