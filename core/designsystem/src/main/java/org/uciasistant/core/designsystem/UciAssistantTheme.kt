package org.uciasistant.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val UciColorScheme = lightColorScheme(
    primary = Color(0xFF0D766E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC9F0E8),
    onPrimaryContainer = Color(0xFF00201C),
    secondary = Color(0xFF4A635E),
    onSecondary = Color.White,
    background = Color(0xFFF5F8F7),
    onBackground = Color(0xFF151D1B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF151D1B),
    surfaceVariant = Color(0xFFE7EEEB),
    onSurfaceVariant = Color(0xFF424C48),
    outline = Color(0xFF707B76),
)

@Composable
fun UciAssistantTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = UciColorScheme,
        content = content,
    )
}
