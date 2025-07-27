package com.bwc.bluethai.ui.screens

import android.Manifest
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bwc.bluethai.data.model.ChatState
import com.bwc.bluethai.data.model.ConversationSession
import com.bwc.bluethai.data.model.SessionPreview
import com.bwc.bluethai.ui.components.ControlsBar
import com.bwc.bluethai.ui.components.HistoryDialog
import com.bwc.bluethai.ui.components.chat.ChatList
import com.bwc.bluethai.ui.components.chat.InitialPlaceholder
import com.bwc.bluethai.ui.theme.BWCTranslatorTheme
import com.bwc.bluethai.viewmodel.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.Date

// REMOVED: The AppScreen enum is no longer needed here.

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun TranslatorScreen(
    viewModel: TranslatorViewModel,
    // ADDED: A way for this screen to tell the MainActivity to navigate.
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val recordAudioPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    val successState = uiState as? TranslatorUiState.Success
    LaunchedEffect(successState?.error) {
        successState?.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    // REMOVED: All the state and 'when' block for navigation are gone.
    // This composable now ONLY shows the translator UI.

    val showHistoryDialog by viewModel.showHistoryDialog.collectAsState()
    if (showHistoryDialog) {
        HistoryDialog(
            sessions = successState?.sessions ?: emptyList(),
            onDismiss = { viewModel.toggleHistoryDialog(false) },
            onSessionClick = { sessionId ->
                viewModel.loadSession(sessionId)
                viewModel.toggleHistoryDialog(false)
            },
            onDeleteClick = { sessionId -> viewModel.deleteSession(sessionId) },
            onNewChatClick = {
                viewModel.startNewSession()
                viewModel.toggleHistoryDialog(false)
            }
        )
    }

    val isTtsReady = successState?.isTtsReady ?: false
    TranslatorScreenContent(
        uiState = uiState,
        isMicEnabled = recordAudioPermission.status.isGranted,
        onMicPress = {
            if (recordAudioPermission.status.isGranted) viewModel.startListening()
            else recordAudioPermission.launchPermissionRequest()
        },
        onMicRelease = viewModel::stopListening,
        onMicClick = {
            if (recordAudioPermission.status.isGranted) viewModel.toggleListening()
            else recordAudioPermission.launchPermissionRequest()
        },
        onPlaybackChange = { enabled ->
            if (enabled && !isTtsReady) {
                Toast.makeText(context, "Please install required (English/Thai) voice packs in Android's TTS settings.", Toast.LENGTH_LONG).show()
                val ttsIntent = Intent("com.android.settings.TTS_SETTINGS").apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                context.startActivity(ttsIntent)
            } else {
                viewModel.setPlaybackEnabled(enabled)
            }
        },
        onSwapLanguage = viewModel::swapLanguage,
        onModeChange = viewModel::setInputMode,
        // CHANGED: The settings click now calls the lambda passed from MainActivity.
        onSettingsClick = onNavigateToSettings,
        onSpeakEnglish = { text -> viewModel.speak(text, isEnglish = true) },
        onSpeakThai = { text -> viewModel.speak(text, isEnglish = false) },
        onPromptStyleChange = { promptStyle -> viewModel.setPromptStyle(promptStyle) }
    )
}

// MOVED: This function is now at the top level of the file, making it visible everywhere.
@Composable
fun TranslatorScreenContent(
    uiState: TranslatorUiState,
    isMicEnabled: Boolean,
    onMicPress: () -> Unit,
    onMicRelease: () -> Unit,
    onMicClick: () -> Unit,
    onPlaybackChange: (Boolean) -> Unit,
    onSwapLanguage: () -> Unit,
    onModeChange: (InputMode) -> Unit,
    onSettingsClick: () -> Unit,
    onSpeakEnglish: (String) -> Unit,
    onPromptStyleChange: (PromptStyle) -> Unit,
    onSpeakThai: (String) -> Unit,
) {
    val successState = uiState as? TranslatorUiState.Success

    Scaffold(
        bottomBar = {
            if (successState != null) {
                Box(modifier = Modifier
                    //.border(1.dp, Color.Red)
                    .fillMaxHeight(0.12f)) { // Fixed height for bottom bar container
                    ControlsBar(
                        isListening = successState.isListening,
                        isInputEnglish = successState.isInputEnglish,
                        isPlaybackEnabled = successState.isPlaybackEnabled,
                        isMicEnabled = isMicEnabled,
                        inputMode = successState.inputMode,
                        currentPromptStyle = successState.promptStyle,
                        onMicPress = onMicPress,
                        onMicRelease = onMicRelease,
                        onMicClick = onMicClick,
                        onSwapLanguage = onSwapLanguage,
                        onModeChange = onModeChange,
                        onPlaybackChange = onPlaybackChange,
                        onSettingsClick = onSettingsClick,
                        onPromptStyleChange = onPromptStyleChange
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                //.height(48.dp) // Fixed height (consider if this should be fillMaxHeight)
                .fillMaxSize() // Takes all available space
                .padding(paddingValues) // Applies scaffold padding
        ) {
            when (val state = uiState) {
                is TranslatorUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        InitialPlaceholder(text = "Loading session...")
                    }
                }

                is TranslatorUiState.Success -> {
                    if (state.currentEntries.isEmpty() && state.interimText.isEmpty() && state.streamingTranslation == null) {
                        InitialPlaceholder(text = "Tap or hold the mic to start.")
                    } else {
                        val chatState = ChatState(
                            entries = state.currentEntries.map { entry ->
                                ChatState.Entry(
                                    id = entry.id,
                                    englishText = entry.englishText,
                                    thaiText = entry.thaiText,
                                    isFromEnglish = entry.isFromEnglish
                                )
                            },
                            interimText = state.interimText,
                            isInputEnglish = state.isInputEnglish,
                            streamingTranslation = state.streamingTranslation
                        )
                        ChatList(
                            state = chatState,
                            onSpeakEnglish = onSpeakEnglish,
                            onSpeakThai = onSpeakThai
                        )
                    }
                }
            }
        }
    }
}

// MOVED: This preview function is also at the top level now.
@Preview(showBackground = true, name = "Screen - Empty State")
@Composable
fun TranslatorScreenPreview_Empty() {
    val emptyState = TranslatorUiState.Success(
        currentEntries = emptyList(),
        sessions = listOf(
            SessionPreview(
                session = ConversationSession(1L, Date()),
                previewText = "Previous chat..."
            )
        )
    )
    BWCTranslatorTheme {
        TranslatorScreenContent(
            uiState = emptyState,
            isMicEnabled = true,
            onPlaybackChange = {},
            onMicPress = {},
            onMicRelease = {},
            onMicClick = {},
            onSwapLanguage = {},
            onModeChange = {},
            onSpeakEnglish = {},
            onSpeakThai = {},
            onSettingsClick = {},
            onPromptStyleChange = { },
        )
    }
}