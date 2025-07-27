package com.bwc.translator.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bwc.translator.ui.theme.*
import com.bwc.translator.viewmodel.InputMode

@Composable
fun ControlsBar(
    isListening: Boolean,
    isInputEnglish: Boolean,
    isPlaybackEnabled: Boolean,
    isMicEnabled: Boolean,
    inputMode: InputMode,
    onMicPress: () -> Unit,
    onMicRelease: () -> Unit,
    onMicClick: () -> Unit,
    onSwapLanguage: () -> Unit,
    onModeChange: (InputMode) -> Unit,
    onPlaybackChange: (Boolean) -> Unit,
    onSettingsClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: Language Swap and Mode Toggle
            ControlItem(modifier = Modifier.weight(1f), alignment = Alignment.CenterStart) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LanguageSwap(
                        isInputEnglish = isInputEnglish,
                        onClick = onSwapLanguage
                    )
                    Spacer(Modifier.height(8.dp))
                    ModeToggle(
                        currentMode = inputMode,
                        onModeChange = onModeChange
                    )
                }
            }

            // Center: Microphone Button
            MicButton(
                isListening = isListening,
                isEnabled = isMicEnabled,
                inputMode = inputMode,
                onPress = onMicPress,
                onRelease = onMicRelease,
                onClick = onMicClick
            )

            // Right side: Playback Toggle and Settings
            ControlItem(modifier = Modifier.weight(1f), alignment = Alignment.CenterEnd) {
                PlaybackToggle(
                    isEnabled = isPlaybackEnabled,
                    onEnabledChange = onPlaybackChange,
                    onSettingsClick = onSettingsClick
                )
            }
        }
    }
}

@Composable
fun MicButton(
    isListening: Boolean,
    isEnabled: Boolean,
    inputMode: InputMode,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Restart
        ), label = "pulse_alpha"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Restart
        ), label = "pulse_shadow"
    )

    val bgColor by animateColorAsState(
        targetValue = when {
            !isEnabled -> MicButtonDisabled
            isListening -> MicButtonListening
            else -> MicButton
        }, label = "mic_bg_color"
    )

    Box(
        modifier = Modifier
            .size(72.dp)
            .shadow(
                elevation = if (isListening) pulse.dp else 0.dp,
                shape = CircleShape,
                ambientColor = MicButtonListening.copy(alpha = pulseAlpha),
                spotColor = MicButtonListening.copy(alpha = pulseAlpha)
            )
            .clip(CircleShape)
            .background(bgColor)
            .pointerInput(inputMode) {
                detectTapGestures(
                    onTap = {
                        if (inputMode == InputMode.TAP) {
                            onClick()
                        }
                    },
                    onPress = {
                        if (inputMode == InputMode.HOLD) {
                            onPress()
                            tryAwaitRelease()
                            onRelease()
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Microphone",
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
fun LanguageSwap(isInputEnglish: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (isInputEnglish) "EN" else "TH", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Icon(Icons.Default.SwapHoriz, "Swap Languages")
            Text(if (isInputEnglish) "TH" else "EN", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun ModeToggle(currentMode: InputMode, onModeChange: (InputMode) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Switch(
            checked = currentMode == InputMode.TAP,
            onCheckedChange = { isChecked ->
                onModeChange(if (isChecked) InputMode.TAP else InputMode.HOLD)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF10B981)
            )
        )
        Text(
            text = if (currentMode == InputMode.HOLD) "Hold to Talk" else "Tap to Talk",
            fontSize = 12.sp,
            color = TextSecondary
        )
    }
}


@Composable
fun PlaybackToggle(
    isEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        IconButton(onClick = { onEnabledChange(!isEnabled) }) {
            Icon(
                imageVector = if (isEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                contentDescription = if (isEnabled) "Disable Playback" else "Enable Playback",
                tint = if (isEnabled) MaterialTheme.colorScheme.onSurface else TextSecondary
            )
        }
        Text(
            text = "Settings",
            fontSize = 12.sp,
            color = TextSecondary,
            modifier = Modifier.clickable { onSettingsClick() }
        )
    }
}

@Composable
private fun ControlItem(modifier: Modifier = Modifier, alignment: Alignment, content: @Composable () -> Unit) {
    Box(
        modifier = modifier,
        contentAlignment = alignment
    ) {
        content()
    }
}
