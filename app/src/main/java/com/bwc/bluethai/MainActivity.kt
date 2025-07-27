package com.bwc.bluethai

import com.bwc.bluethai.ui.screens.TranslatorScreen
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.bwc.bluethai.ui.screens.DebugLogScreen
import com.bwc.bluethai.ui.screens.SettingsScreen
import com.bwc.bluethai.ui.theme.BWCTranslatorTheme
import com.bwc.bluethai.ui.theme.getDynamicTypography
import com.bwc.bluethai.viewmodel.*
import java.io.File
import java.io.IOException

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Log file initialization can be simplified
        val logFileName = "app_logs.txt"
        val logFile = File(applicationContext.filesDir, logFileName)
        if (!logFile.exists()) {
            try {
                logFile.createNewFile()
            } catch (e: IOException) {
                Log.e("MainActivity", "Error creating log file: ${e.message}")
            }
        }

        setContent {
            val viewModel: TranslatorViewModel by viewModels {
                TranslatorViewModel.TranslatorViewModelFactory(application)
            }
            val uiState by viewModel.uiState.collectAsState()
            val successState = uiState as? TranslatorUiState.Success

            // Use the AppScreen enum for the navigation state
            var currentScreen by remember { mutableStateOf(AppScreen.Translator) }

            val dynamicTypography = getDynamicTypography(successState?.baseFontSize ?: 18)

            BWCTranslatorTheme(typography = dynamicTypography) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Back handler now uses the enum, which is safer
                    BackHandler(enabled = currentScreen != AppScreen.Translator) {
                        when (currentScreen) {
                            AppScreen.DebugLogs -> currentScreen = AppScreen.Settings
                            AppScreen.Settings -> currentScreen = AppScreen.Translator
                            else -> { /* Do nothing, should not happen */ }
                        }
                    }

                    // Screen switching logic also uses the enum
                    when (currentScreen) {
                        AppScreen.Translator -> {
                            TranslatorScreen(
                                viewModel = viewModel,
                                onNavigateToSettings = { currentScreen = AppScreen.Settings }
                            )
                        }
                        AppScreen.Settings -> {
                            if (successState != null) {
                                SettingsScreen(
                                    availableKeys = availableApiKeys,
                                    currentKeyName = successState.currentApiKeyName,
                                    onApiKeySelected = viewModel::setApiKey,
                                    currentFontSize = successState.baseFontSize,
                                    onFontSizeChange = viewModel::setFontSize,
                                    currentPromptStyle = successState.promptStyle,
                                    onPromptStyleChange = viewModel::setPromptStyle,
                                    modelSelection = successState.modelSelection,
                                    onModelSelectionChange = viewModel::updateModelSelection,
                                    onNavigateToDebugLogs = { currentScreen = AppScreen.DebugLogs },
                                    onNavigateToHistory = { viewModel.toggleHistoryDialog(true) },
                                    onBackupDatabase = {
                                        viewModel.backupDatabase(this@MainActivity)
                                        Toast.makeText(this@MainActivity, "Database Backup Initiated.", Toast.LENGTH_SHORT).show()
                                    },
                                    onNavigateBack = { currentScreen = AppScreen.Translator }
                                )
                            }
                        }
                        AppScreen.DebugLogs -> {
                            if (successState != null) {
                                DebugLogScreen(
                                    logs = successState.debugLogs,
                                    onExportLogs = { viewModel.exportLogs(this@MainActivity) },
                                    onNavigateBack = { currentScreen = AppScreen.Settings }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}