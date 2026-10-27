package com.example.whatif.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B6B4A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA7F2C7),
    onPrimaryContainer = Color(0xFF002112),
    secondaryContainer = Color(0xFFD0E8D8),
    onSecondaryContainer = Color(0xFF0A1F14),
    tertiary = Color(0xFFB5541C),
    onTertiary = Color(0xFFFFFFFF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8BD5AC),
    onPrimary = Color(0xFF003822),
    primaryContainer = Color(0xFF005234),
    onPrimaryContainer = Color(0xFFA7F2C7),
    secondaryContainer = Color(0xFF364B3E),
    onSecondaryContainer = Color(0xFFD0E8D8),
    tertiary = Color(0xFFFFB68A),
    onTertiary = Color(0xFF552100),
)

@Composable
fun WhatIfTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}