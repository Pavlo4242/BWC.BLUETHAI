package com.bwc.bluethai.ui.components.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bwc.bluethai.ui.theme.BWCTranslatorTheme
import com.bwc.bluethai.ui.theme.BubbleEnBg
import com.bwc.bluethai.ui.theme.BubbleThBg
import com.bwc.bluethai.ui.theme.Sarabun
import com.bwc.bluethai.ui.theme.TextPrimary
import com.bwc.bluethai.ui.theme.TextSecondary

@Composable
fun ChatBubble(
    text: String,
    isEnglish: Boolean,
    modifier: Modifier = Modifier,
    isInterim: Boolean = false,
    onSpeakClick: (() -> Unit)? = null,
    onCopyClick: (() -> Unit)? = null,
    showCopiedIndicator: Boolean = false
) {
    Box(modifier = modifier) {
        Card(
            shape = RoundedCornerShape(16.dp).copy(
                bottomStart = CornerSize(if (isEnglish) 4.dp else 16.dp),
                bottomEnd = CornerSize(if (!isEnglish) 4.dp else 16.dp)
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isEnglish) BubbleEnBg else BubbleThBg
            ),
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Text(
                text = text,
                color = TextPrimary,
                fontSize = 20.sp,
                fontFamily = if (isEnglish) null else Sarabun,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }

        if (!isInterim && (onSpeakClick != null || onCopyClick != null)) Row(
            modifier = Modifier
                .align(if (isEnglish) Alignment.BottomStart else Alignment.BottomEnd)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            onSpeakClick?.let { onClick ->
                IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.VolumeUp,
                        contentDescription = "Speak",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            onCopyClick?.let { onClick ->
                IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
                    if (showCopiedIndicator) {
                        Text("Copied!", fontSize = 10.sp, color = TextSecondary)
                    } else {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatBubblePreview_English() {
    BWCTranslatorTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            ChatBubble(
                text = "Hello, how are you doing today?",
                isEnglish = true,
                onSpeakClick = {},
                onCopyClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatBubblePreview_Thai() {
    BWCTranslatorTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            ChatBubble(
                text = "สวัสดีวันนี้เป็นอย่างไรบ้าง",
                isEnglish = false,
                onSpeakClick = {},
                onCopyClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatBubblePreview_English_Copied() {
    BWCTranslatorTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            ChatBubble(
                text = "This text has been copied.",
                isEnglish = true,
                onSpeakClick = {},
                onCopyClick = {},
                showCopiedIndicator = true
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatBubblePreview_Interim() {
    BWCTranslatorTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            ChatBubble(
                text = "This is an interim result...",
                isEnglish = true,
                isInterim = true,
                onSpeakClick = {},
                onCopyClick = {}
            )
        }
    }
}
