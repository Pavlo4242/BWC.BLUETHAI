package com.bwc.bluethai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView


private val DarkColorScheme = darkColorScheme(
    primary = MicButton,
    background = BgLight,
    surface = BgDark,
    onPrimary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    secondary = TextSecondary
)

@Composable
fun BWCTranslatorTheme(
    // The theme now accepts a Typography object to allow for dynamic font sizes
    typography: Typography = getDynamicTypography(18),
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme // Force dark theme for this app
    val view = LocalView.current

    /*
    // We are letting the XML theme handle this automatically now.
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    */

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography, // Use the typography passed into the function
        content = content
    )
}
