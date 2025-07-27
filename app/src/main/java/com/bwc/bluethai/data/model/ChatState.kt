package com.bwc.bluethai.data.model

data class ChatState(
    val entries: List<Entry>,
    val interimText: String,
    val isInputEnglish: Boolean,
    val streamingTranslation: Pair<String, String>?
) {
    data class Entry(
        val id: Int,
        val englishText: String,
        val thaiText: String,
        val isFromEnglish: Boolean
    )
}