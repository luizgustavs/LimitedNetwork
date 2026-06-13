package com.gxstxvx.limitednetwork.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color.White,
    background = Color.Black,
    surface = Color.Black,
    surfaceVariant = Color(0xFF1A1A1A),
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color.White.copy(alpha = 0.72f)
)

@Composable
fun LimitedNetworkTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
