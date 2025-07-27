package com.bwc.bluethai.viewmodel

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bwc.bluethai.data.model.ChatState
import com.bwc.bluethai.ui.theme.BWCTranslatorTheme
import com.bwc.bluethai.ui.components.chat.*

@Composable
fun TranslationEntryItem(
    entry: ChatState.Entry,
    modifier: Modifier = Modifier,
    onSpeakSource: (() -> Unit)? = null,
    onSpeakTranslation: (() -> Unit)? = null,
    onCopySource: (() -> Unit)? = null,
    onCopyTranslation: (() -> Unit)? = null,
    showCopiedSource: Boolean = false,
    showCopiedTranslation: Boolean = false
) {
    Column(
        horizontalAlignment = if (entry.isFromEnglish) Alignment.Start else Alignment.End,
        modifier = modifier.fillMaxWidth()
    ) {
        val sourceText = if (entry.isFromEnglish) entry.englishText else entry.thaiText
        val translatedText = if (entry.isFromEnglish) entry.thaiText else entry.englishText

        ChatBubble(
            text = sourceText,
            isEnglish = entry.isFromEnglish,
            onSpeakClick = onSpeakSource,
            onCopyClick = onCopySource,
            showCopiedIndicator = showCopiedSource
        )
        ChatBubble(
            text = translatedText,
            isEnglish = !entry.isFromEnglish,
            onSpeakClick = onSpeakTranslation,
            onCopyClick = onCopyTranslation,
            showCopiedIndicator = showCopiedTranslation
        )
    }
}

@Preview(showBackground = true, name = "From English")
@Composable
private fun TranslationEntryItemPreview_FromEnglish() {
    BWCTranslatorTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            TranslationEntryItem(
                entry = ChatState.Entry(
                    id = 1,
                    englishText = "This is a test message.",
                    thaiText = "นี่คือข้อความทดสอบ",
                    isFromEnglish = true
                ),
                onSpeakSource = {},
                onSpeakTranslation = {},
                onCopySource = {},
                onCopyTranslation = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "From Thai")
@Composable
private fun TranslationEntryItemPreview_FromThai() {
    BWCTranslatorTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            TranslationEntryItem(
                entry = ChatState.Entry(
                    id = 2,
                    englishText = "How much is this?",
                    thaiText = "อันนี้ราคาเท่าไหร่",
                    isFromEnglish = false
                ),
                onSpeakSource = {},
                onSpeakTranslation = {},
                onCopySource = {},
                onCopyTranslation = {}
            )
        }
    }
}