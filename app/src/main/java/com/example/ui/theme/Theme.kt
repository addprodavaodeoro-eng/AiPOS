package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    primaryContainer = TealLight,
    onPrimaryContainer = TealDark,
    background = BackgroundLight,
    onBackground = Slate900,
    surface = SurfaceLight,
    onSurface = Slate900,
    surfaceVariant = TealCard,
    onSurfaceVariant = TealText,
    outline = Slate200,
    outlineVariant = Slate100,
    secondary = TealPrimary,
    onSecondary = Color.White,
    tertiary = Amber600,
    onTertiary = Color.White,
    error = Red600,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = TealPrimaryDark,
    onPrimary = Slate900,
    primaryContainer = TealContainerDark,
    onPrimaryContainer = TealOnContainerDark,
    background = BackgroundDark,
    onBackground = Slate50,
    surface = SurfaceDark,
    onSurface = Slate50,
    surfaceVariant = TealCardDark,
    onSurfaceVariant = TealOnContainerDark,
    outline = Slate700,
    outlineVariant = Slate800,
    secondary = TealPrimaryDark,
    onSecondary = Slate900,
    tertiary = Amber500,
    onTertiary = Slate900,
    error = Red500,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = LocalThemeState.current.isDarkTheme,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
