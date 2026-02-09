package com.olnatura.qr.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val OlnaturaGreen = Color(0xFF6E8F3B)      // verde como tu captura (aprox)
private val OlnaturaGreenLight = Color(0xFFDAEDC2) // banner claro
private val Background = Color(0xFFF4F7F2)

private val scheme = lightColorScheme(
    primary = OlnaturaGreen,
    onPrimary = Color.White,
    background = Background,
    surface = Color.White,
    onSurface = Color(0xFF0E1A12),
)

@Composable
fun OlnaturaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = scheme,
        typography = Typography(),
        content = content
    )
}

object OlnaturaColors {
    val Green = OlnaturaGreen
    val GreenLight = OlnaturaGreenLight
}