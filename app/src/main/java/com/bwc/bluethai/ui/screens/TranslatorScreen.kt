package com.bwc.translator.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bwc.translator.data.model.ChatState
import com.bwc.translator.ui.components.ControlsBar
import com.bwc.translator.ui.components.HistoryDialog
import com.bwc.translator.ui.components.chat.ChatList
import com.bwc.translator.ui.components.chat.InitialPlaceholder
import com.bwc.translator.viewmodel.TranslatorUiState
import com.bwc.translator.viewmodel.TranslatorViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun TranslatorScreen(
    viewModel: TranslatorViewModel = viewModel(),
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

    // History dialog is now controlled by the ViewModel state directly
    val showHistoryDialog by viewModel.showHistoryDialog.collectAsState()
    if (showHistoryDialog) {
        HistoryDialog(
            sessions = successState?.sessions ?: emptyList(),
            onDismiss = { viewModel.toggleHistoryDialog(false) },
            onSessionClick = { viewModel.loadSession(it) },
            onDeleteClick = { viewModel.deleteSession(it) },
            onNewChatClick = { viewModel.startNewSession() }
        )
    }

    Scaffold(
        bottomBar = {
            if (successState != null) {
                ControlsBar(
                    isListening = successState.isListening,
                    isInputEnglish = successState.isInputEnglish,
                    isPlaybackEnabled = successState.isPlaybackEnabled,
                    isMicEnabled = recordAudioPermission.status.isGranted,
                    inputMode = successState.inputMode,
                    onMicPress = {
                        if (recordAudioPermission.status.isGranted) {
                            viewModel.startListening()
                        } else {
                            recordAudioPermission.launchPermissionRequest()
                        }
                    },
                    onMicRelease = { viewModel.stopListening() },
                    onMicClick = {
                        if (recordAudioPermission.status.isGranted) {
                            viewModel.toggleListening()
                        } else {
                            recordAudioPermission.launchPermissionRequest()
                        }
                    },
                    onSwapLanguage = { viewModel.swapLanguage() },
                    onModeChange = { viewModel.setInputMode(it) },
                    onPlaybackChange = { viewModel.setPlaybackEnabled(it) },
                    onSettingsClick = onNavigateToSettings
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is TranslatorUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        InitialPlaceholder(text = "Loading session...")
                    }
                }
                is TranslatorUiState.Success -> {
                    if (state.currentEntries.isEmpty() && state.interimText.isBlank() && state.streamingTranslation == null) {
                        InitialPlaceholder(text = "Tap or hold the mic to start.")
                    } else {
                        val chatState = ChatState(
                            entries = state.currentEntries,
                            interimText = state.interimText,
                            isInputEnglish = state.isInputEnglish,
                            streamingTranslation = state.streamingTranslation
                        )
                        ChatList(
                            state = chatState,
                            onSpeakEnglish = { text -> viewModel.speak(text, isEnglish = true) },
                            onSpeakThai = { text -> viewModel.speak(text, isEnglish = false) }
                        )
                    }
                }
            }
        }
    }
}