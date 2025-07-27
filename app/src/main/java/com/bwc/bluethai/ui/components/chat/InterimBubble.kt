package com.bwc.bluethai.ui.components.chat

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@Composable
fun InterimBubble(
    text: String,
    isInputEnglish: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isInputEnglish) Arrangement.Start else Arrangement.End
    ) {
        ChatBubble(text = text, isEnglish = isInputEnglish, isInterim = true)
    }
}