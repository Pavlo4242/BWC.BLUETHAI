package com.bwc.bluethai.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.bwc.bluethai.ui.theme.*
import com.bwc.bluethai.viewmodel.InputMode
import com.bwc.bluethai.R
import com.bwc.bluethai.viewmodel.PromptStyle
import com.bwc.bluethai.viewmodel.PromptStyle.*


@Composable
fun ControlsBar(
    isListening: Boolean,
    isInputEnglish: Boolean,
    isPlaybackEnabled: Boolean,
    isMicEnabled: Boolean,
    inputMode: InputMode,
    currentPromptStyle: PromptStyle,
    onPromptStyleChange: (PromptStyle) -> Unit,
    onMicPress: () -> Unit,
    onMicRelease: () -> Unit,
    onMicClick: () -> Unit,
    onSwapLanguage: () -> Unit,
    onModeChange: (InputMode) -> Unit,
    onPlaybackChange: (Boolean) -> Unit,
    onSettingsClick: () -> Unit,
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val bottomBarHeight = screenHeight * 0.12f

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = Modifier
            .height(bottomBarHeight)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(bottomBarHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left section
            Column(
                modifier = Modifier
                    .weight(0.3f)
                    .offset(y = (-12).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when (currentPromptStyle) {
                        PATTAYA -> "Pattaya"
                        VULGAR -> "Pirate"
                        HISO -> "Formal"
                        DIRECT -> "Direct"
                    },
                    fontSize = 16.sp,
                    color = TextSecondary,
                    modifier = Modifier
                        .clickable { onSettingsClick() }
                        .offset(y = 4.dp)
                )

                Spacer(Modifier.height(4.dp))

                LanguageSwap(
                    isInputEnglish = isInputEnglish,
                    onClick = onSwapLanguage,
                    modifier = Modifier.offset(y = (-4).dp)
                )
            }

            // Center section
            Box(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                MicButton(
                    isListening = isListening,
                    isEnabled = isMicEnabled,
                    inputMode = inputMode,
                    onPress = onMicPress,
                    onRelease = onMicRelease,
                    onClick = onMicClick
                )
            }

            // Right section
            Column(
                modifier = Modifier
                    .weight(0.3f)
                    .offset(y = (-8).dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModeToggle(
                    currentMode = inputMode,
                    onModeChange = onModeChange,
                    modifier = Modifier.offset(y = (-4).dp)
                )

                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_settings),
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun ControlsBarPreview() {
    ControlsBar(
        isListening = false,
        isInputEnglish = true,
        isPlaybackEnabled = true,
        isMicEnabled = true,
        inputMode = InputMode.HOLD,
        currentPromptStyle = DIRECT,
        onMicPress = {},
        onMicRelease = {},
        onMicClick = {},
        onSwapLanguage = {},
        onModeChange = {},
        onPlaybackChange = {},
        onPromptStyleChange = {},
        onSettingsClick = {}
    )
}

@Preview
@Composable
fun MicButtonPreview() {
    MicButton(
        isListening = false,
        isEnabled = true,
        inputMode = InputMode.HOLD,
        onPress = {},
        onRelease = {},
        onClick = {}
    )
}

@Preview
@Composable
fun LanguageSwapPreview() {
    LanguageSwap(
        isInputEnglish = true,
        onClick = {}
    )
}

@Preview
@Composable
fun ModeTogglePreview() {
    ModeToggle(
        currentMode = InputMode.HOLD,
        onModeChange = {}
    )
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
    val infiniteTransition = rememberInfiniteTransition()
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Restart
        )
    )

    val bgColor by animateColorAsState(
        when {
            !isEnabled -> MicButtonDisabled
            isListening -> MicButtonListening
            else -> MicButton
        }
    )

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            //.border(1.dp, Color.Red)
            .fillMaxHeight(0.9f)
            .shadow(
                elevation = if (isListening) pulse.dp else 0.dp,
                shape = CircleShape
            )
            .clip(CircleShape)
            .background(bgColor)
            .pointerInput(inputMode) {
                detectTapGestures(
                    onTap = { if (inputMode == InputMode.TAP) onClick() },
                    onPress = { if (inputMode == InputMode.HOLD) {
                        onPress()
                        tryAwaitRelease()
                        onRelease()
                    }}
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_stand_bj),
            contentDescription = "Microphone",
            tint = Color.White,
            modifier = Modifier
                .size(94.dp)
                .offset(y = 4.dp)
        )
    }
}
@Composable
fun LanguageSwap(isInputEnglish: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp) // Sets clickable area size
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp) // 4dp between icons
        ) {

            Icon(
                painter = painterResource(
                    id = if (isInputEnglish) R.drawable.ic_th else R.drawable.ic_th
                ),
                contentDescription = if (isInputEnglish) "Thai" else "English",
                modifier = Modifier.size(96.dp)
            )
        }
    }
}

@Composable
fun ModeToggle(
    currentMode: InputMode,
    onModeChange: (InputMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier

        .fillMaxHeight()
        .wrapContentHeight(Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Switch(
            checked = currentMode == InputMode.TAP,
            onCheckedChange = { isChecked ->
                onModeChange(if (isChecked) InputMode.TAP else InputMode.HOLD)
            },
            modifier = Modifier

                .width(64.dp)
                .height(48.dp),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF10B981)
            )
        )
        Text(
            text = if (currentMode == InputMode.HOLD) "Hold to Talk" else "Tap to Talk",
            fontSize = 14.sp,
            color = TextSecondary
        )
    }
}


@Composable
fun LanguageSwap(
    isInputEnglish: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp),
                ) {
        /*Icon(
            painter = painterResource(
                id = if (isInputEnglish) R.drawable.ic_en else R.drawable.ic_th
            ),
            contentDescription = if (isInputEnglish) "English" else "Thai",
            modifier = Modifier
                .size(32.dp)
*/      Text(
            text = if (isInputEnglish) "EN" else "TH",
            fontSize = 24.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
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
@Preview(showBackground = true)
@Composable
fun PlaybackTogglePreview() {
    // Wrap in your app's theme if available
    MaterialTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Preview in enabled state
            PlaybackToggle(
                isEnabled = true,
                onEnabledChange = {},
                onSettingsClick = {},
                currentPromptStyle = DIRECT
            )

            // Preview in disabled state
            PlaybackToggle(
                isEnabled = false,
                onEnabledChange = {},
                onSettingsClick = {},
                currentPromptStyle = PATTAYA
            )
        }
    }
}

@Composable
fun PlaybackToggle(
    isEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    currentPromptStyle: PromptStyle,
    onSettingsClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(
            onClick = { onEnabledChange(!isEnabled) },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                painter = painterResource(
                    id = if (isEnabled) R.drawable.ic_settings else R.drawable.ic_69_white
                ),
                contentDescription = if (isEnabled) "Settings" else "Enable Playback",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(32.dp)
            ) }

        Text(
            text = when (currentPromptStyle) {
                PATTAYA -> "Pattaya"
                VULGAR -> "Pirate"
                HISO -> "Formal"
                DIRECT -> "Direct"
            },
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.clickable { onSettingsClick() }
        )
    }
}