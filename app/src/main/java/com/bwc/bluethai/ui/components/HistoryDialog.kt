package com.bwc.bluethai.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.bwc.bluethai.data.model.ConversationSession
import com.bwc.bluethai.data.model.SessionPreview
import com.bwc.bluethai.ui.theme.BubbleThBg
import com.bwc.bluethai.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryDialog(
    sessions: List<SessionPreview>,
    onDismiss: () -> Unit,
    onSessionClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit,
    onNewChatClick: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Conversation History",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (sessions.isEmpty()) {
                    Text("No past conversations.", color = TextSecondary)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(sessions) { sessionPreview ->
                            com.bwc.bluethai.ui.components.HistoryItem(
                                session = sessionPreview.session,
                                preview = sessionPreview.previewText,
                                onClick = { onSessionClick(sessionPreview.session.id) },
                                onDelete = { onDeleteClick(sessionPreview.session.id) }
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        onNewChatClick()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BubbleThBg)
                ) {
                    Text("Start New Chat")
                }
            }
        }
    }
}

@Preview
@Composable
fun HistoryDialogPreview() {
    val sampleSessions = listOf(
        SessionPreview(session = ConversationSession(id = 1L, startTime = Date()), previewText = "Hello, how are you?"),
        SessionPreview(session = ConversationSession(id = 2L, startTime = Date()), previewText = "This is a longer conversation preview to test text overflow handling and make sure it looks good."),
        SessionPreview(session = ConversationSession(id = 3L, startTime = Date()), previewText = "")
    )
    HistoryDialog(
        sessions = sampleSessions,
        onDismiss = {},
        onSessionClick = {},
        onDeleteClick = {},
        onNewChatClick = {}
    )
}

@Preview
@Composable
fun HistoryItemPreview() {
    val session = ConversationSession(id = 1L, startTime = Date())
    HistoryItem(
        session = session,
        preview = "This is a sample preview text.",
        onClick = {},
        onDelete = {}
    )
}


@Composable
private fun HistoryItem(session: ConversationSession, preview: String, onClick: () -> Unit, onDelete: () -> Unit) {
    val formatter = remember { SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = preview.ifEmpty { "Empty Chat" },
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatter.format(session.startTime),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete Session", tint = TextSecondary)
        }
    }
}