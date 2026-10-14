package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = WhatsAppAccentGreen,
    onPrimary = Color.White,
    primaryContainer = WhatsAppDarkGreen,
    onPrimaryContainer = Color.White,
    secondary = WhatsAppLightGreen,
    onSecondary = Color.Black,
    background = WhatsAppDarkBg,
    onBackground = WhatsAppDarkTextPrimary,
    surface = WhatsAppDarkSurface,
    onSurface = WhatsAppDarkTextPrimary,
    surfaceVariant = WhatsAppDarkSurfaceVariant,
    onSurfaceVariant = WhatsAppDarkTextSecondary,
)

private val LightColorScheme = lightColorScheme(
    primary = WhatsAppDarkGreen,
    onPrimary = Color.White,
    primaryContainer = WhatsAppTealGreen,
    onPrimaryContainer = Color.White,
    secondary = WhatsAppLightGreen,
    onSecondary = Color.White,
    background = WhatsAppLightBg,
    onBackground = WhatsAppLightTextPrimary,
    surface = WhatsAppLightSurface,
    onSurface = WhatsAppLightTextPrimary,
    surfaceVariant = Color(0xFFF0F2F5),
    onSurfaceVariant = WhatsAppLightTextSecondary,
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
