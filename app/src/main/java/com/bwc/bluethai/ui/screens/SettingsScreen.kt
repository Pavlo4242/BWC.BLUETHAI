
package com.bwc.bluethai.ui.screens
/** LOgs not capturing what we need -- API Key Selector still wrong, still getting asterisks on the screen */
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bwc.bluethai.viewmodel.ModelSelectionState
import com.bwc.bluethai.viewmodel.PromptStyle
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    availableKeys: Map<String, String>,
    currentKeyName: String,
    onApiKeySelected: (String) -> Unit,
    currentFontSize: Int,
    onFontSizeChange: (Int) -> Unit,
    /* useCustomPrompt: Boolean,
     onUseCustomPromptChange: (Boolean) -> Unit,*/
    currentPromptStyle: PromptStyle,
    onPromptStyleChange: (PromptStyle) -> Unit,
    modelSelection: ModelSelectionState,
    onModelSelectionChange: (ModelSelectionState) -> Unit,
    onNavigateToDebugLogs: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateBack: () -> Unit,
    onBackupDatabase: () -> Unit

) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // API Key Slider
            item {
                Column {
                    val keyNames = availableKeys.keys.toList()
                    val currentIndex = keyNames.indexOf(currentKeyName)
                    Text("API Key: $currentKeyName", style = MaterialTheme.typography.titleMedium)
                    Slider(
                        value = currentIndex.toFloat(),
                        onValueChange = { onApiKeySelected(keyNames[it.roundToInt()]) },
                        valueRange = 0f..(keyNames.size - 1).toFloat(),
                        steps = keyNames.size - 2
                    )
                }
            }

            // Model Selector
            item {
                Column {
                    Text("Model: ${modelSelection.getModelName()}", style = MaterialTheme.typography.titleMedium)
                    // Version Slider
                    val versionIndex = when(modelSelection.version) {
                        1.5f -> 0
                        2.0f -> 1
                        else -> 2 // 2.5f
                    }
                    Slider(
                        value = versionIndex.toFloat(),
                        onValueChange = {
                            val newVersion = when(it.roundToInt()) {
                                0 -> 1.5f
                                1 -> 2.0f
                                else -> 2.5f
                            }
                            onModelSelectionChange(modelSelection.copy(version = newVersion))
                        },
                        valueRange = 0f..2f,
                        steps = 1
                    )
                    // Pro/Flash Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("Flash")
                        Switch(
                            checked = modelSelection.isPro,
                            onCheckedChange = { onModelSelectionChange(modelSelection.copy(isPro = it)) },
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Text("Pro")
                    }
                }
            }

            // Font Size Slider
            item {
                Column {
                    Text("Font Size: ${currentFontSize}sp", style = MaterialTheme.typography.titleMedium)
                    Slider(
                        value = currentFontSize.toFloat(),
                        onValueChange = { onFontSizeChange(it.roundToInt()) },
                        valueRange = 14f..28f,
                        steps = 6
                    )
                }
            }

            // Custom Prompt Toggle
            /*item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Use Polite Prompt")
                        Text("Translates like HiSo", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = useCustomPrompt,
                        onCheckedChange = onUseCustomPromptChange
                    )
                }*/
// Prompt Style
            item {
                Column(
                    //modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    /*Text(
                        "Prompt Style",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )*/

                    // Define the order and display names
                    val promptStyles = listOf(
                        PromptStyle.DIRECT to "Direct",
                        PromptStyle.PATTAYA to "Pattaya",
                        PromptStyle.VULGAR to "Vulgar",
                        PromptStyle.HISO to "Formal"
                    )

                    // Display as clickable chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        promptStyles.forEach { (style, displayName) ->
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = if (currentPromptStyle == style)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clickable { onPromptStyleChange(style) }
                                    .padding(4.dp)
                            ) {
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (currentPromptStyle == style)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Navigation Items
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            item {
                SettingsNavigationItem(
                    icon = Icons.Default.History,
                    text = "Conversation History",
                    onClick = onNavigateToHistory
                )
            }

            item {
                SettingsNavigationItem(
                    icon = Icons.Default.BugReport,
                    text = "Debug Logs",
                    onClick = onNavigateToDebugLogs
                )
            }

            item {
                SettingsNavigationItem(
                    icon = Icons.Default.Backup,
                    text = "Backup Database",
                    onClick = onBackupDatabase
                )
            }
        }
    }
}

@Composable
private fun SettingsNavigationItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center


    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.padding(end = 16.dp))
        Text(text)
    }
}

@Preview
@Composable
fun SettingsScreenPreview() {
    SettingsScreen(
        availableKeys = mapOf(
            "Key 1" to "dummy_key_1",
            "Key 2" to "dummy_key_2"
        ),
        currentKeyName = "Key 1",
        onApiKeySelected = {},
        currentFontSize = 16,
        onFontSizeChange = {},
        currentPromptStyle = PromptStyle.PATTAYA,
        onPromptStyleChange = {},
        modelSelection = ModelSelectionState(version = 1.5f, isPro = false),
        onModelSelectionChange = {},
        onNavigateToDebugLogs = {},
        onNavigateToHistory = {},
        onNavigateBack = {},
        onBackupDatabase = {}
    )
}

@Preview
@Composable
fun SettingsNavigationItemPreview() {
    SettingsNavigationItem(
        icon = Icons.Default.History,
        text = "Conversation History",
        onClick = {}
    )
}




