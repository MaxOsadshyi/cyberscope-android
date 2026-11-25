package com.example.helloworld.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val CsBackground = Color(0xFF0E1117)
val CsSurface = Color(0xFF161B24)
val CsSurfaceVariant = Color(0xFF1F2735)
val CsBorder = Color(0xFF2B3548)
val CsAmberPrimary = Color(0xFFF59E0B)
val CsCyanSecondary = Color(0xFF38BDF8)
val CsTextPrimary = Color(0xFFF3F4F6)
val CsTextSecondary = Color(0xFF9CA3AF)

private val CyberScopeDarkScheme = darkColorScheme(
    primary = CsAmberPrimary,
    onPrimary = Color(0xFF111827),
    secondary = CsCyanSecondary,
    onSecondary = Color(0xFF0F172A),
    background = CsBackground,
    onBackground = CsTextPrimary,
    surface = CsSurface,
    onSurface = CsTextPrimary,
    surfaceVariant = CsSurfaceVariant,
    onSurfaceVariant = CsTextSecondary,
    outline = CsBorder
)

@Composable
fun HelloWorldTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CyberScopeDarkScheme,
        typography = Typography,
        content = content
    )
}