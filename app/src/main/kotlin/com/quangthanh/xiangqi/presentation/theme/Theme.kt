package com.quangthanh.xiangqi.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val colorScheme = lightColorScheme(
    primary = Color(0xFF8D3B2A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD2),
    onPrimaryContainer = Color(0xFF3B0904),
    secondary = Color(0xFF6D5D3A),
    secondaryContainer = Color(0xFFF3D9A6),
    onSecondaryContainer = Color(0xFF3E2C0A),
    background = Color(0xFFF6E7C8),
    onBackground = Color(0xFF2B2117),
    surface = Color(0xFFFFF8EC),
    onSurface = Color(0xFF2B2117),
    surfaceContainer = Color(0xFFF9EBD0),
    surfaceContainerHigh = Color(0xFFF6E3C0),
    surfaceContainerHighest = Color(0xFFF2DDB5),
    outline = Color(0xFF8A7556),
    outlineVariant = Color(0xFFD7C2A0),
)

@Composable
fun ChessTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colorScheme, content = content)
}
