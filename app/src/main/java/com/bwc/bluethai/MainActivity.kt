package com.bwc.translator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.bwc.translator.ui.screens.DebugLogScreen
import com.bwc.translator.ui.screens.SettingsScreen
import com.bwc.translator.ui.screens.TranslatorScreen
import com.bwc.translator.ui.theme.BWCTranslatorTheme
import com.bwc.translator.ui.theme.getDynamicTypography
import com.bwc.translator.viewmodel.TranslatorUiState
import com.bwc.translator.viewmodel.TranslatorViewModel
import com.bwc.translator.viewmodel.availableApiKeys

class MainActivity : ComponentActivity() {

    private val translatorViewModel: TranslatorViewModel by viewModels {
        TranslatorViewModel.TranslatorViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var currentScreen by remember { mutableStateOf("Translator") }
            val uiState by translatorViewModel.uiState.collectAsState()
            val successState = uiState as? TranslatorUiState.Success

            val dynamicTypography = getDynamicTypography(successState?.baseFontSize ?: 18)

            BWCTranslatorTheme(typography = dynamicTypography) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BackHandler(enabled = currentScreen != "Translator") {
                        when (currentScreen) {
                            "DebugLogs" -> currentScreen = "Settings"
                            "Settings" -> currentScreen = "Translator"
                        }
                    }

                    when (currentScreen) {
                        "Translator" -> TranslatorScreen(
                            viewModel = translatorViewModel,
                            onNavigateToSettings = { currentScreen = "Settings" }
                        )
                        "Settings" -> {
                            if (successState != null) {
                                // This makes the usage of the import explicit to fix the build error.
                                val keys = availableApiKeys
                                SettingsScreen(
                                    availableKeys = keys,
                                    currentKeyName = successState.currentApiKeyName,
                                    onApiKeySelected = { translatorViewModel.setApiKey(it) },
                                    currentFontSize = successState.baseFontSize,
                                    onFontSizeChange = { translatorViewModel.setFontSize(it) },
                                    useCustomPrompt = successState.useCustomPrompt,
                                    onUseCustomPromptChange = { translatorViewModel.setUseCustomPrompt(it) },
                                    modelSelection = successState.modelSelection,
                                    onModelSelectionChange = { translatorViewModel.updateModelSelection(it) },
                                    onNavigateToDebugLogs = { currentScreen = "DebugLogs" },
                                    onNavigateToHistory = {
                                        currentScreen = "Translator"
                                        translatorViewModel.toggleHistoryDialog(true)
                                    },
                                    onNavigateBack = { currentScreen = "Translator" }
                                )
                            }
                        }
                        "DebugLogs" -> {
                            if (successState != null) {
                                DebugLogScreen(
                                    logs = successState.debugLogs,
                                    onNavigateBack = { currentScreen = "Settings" }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
