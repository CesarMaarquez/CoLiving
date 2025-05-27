package net.azarquiel.coliving.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00BFA5),
    onPrimary = Color.White,
    secondary = Color(0xFFF48FB1),
    onSecondary = Color.White,
    background = Color(0xFFF1F1F1),
    onBackground = Color(0xFF212121),
    surface = Color.White,
    onSurface = Color(0xFF212121),
    error = Color(0xFFEF5350),
    onError = Color.White,
    outline = Color(0xFFB0BEC5) // Gris suave para bordes
)

@Composable
fun CoLivingTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography(),
        shapes = Shapes(),
        content = content
    )
}
