package com.bwc.bluethai.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// This function creates a Typography object with a dynamic base size
fun getDynamicTypography(baseSize: Int): Typography {
    return Typography(
        bodyLarge = TextStyle(
            fontFamily = Inter,
            fontWeight = FontWeight.Normal,
            fontSize = baseSize.sp,
            lineHeight = (baseSize + 8).sp,
            letterSpacing = 0.5.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = Inter,
            fontWeight = FontWeight.SemiBold,
            fontSize = (baseSize + 8).sp,
        ),
        titleMedium = TextStyle(
            fontFamily = Inter,
            fontWeight = FontWeight.Medium,
            fontSize = (baseSize + 2).sp,
        )
        // Define other styles as needed based on the baseSize
    )
}