package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

fun getCustomColorScheme(
    themeMode: AppThemeMode,
    accent: AccentColor,
    isSystemDark: Boolean
): ColorScheme {
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.AMOLED -> true
    }

    val isAmoled = themeMode == AppThemeMode.AMOLED

    return if (isAmoled) {
        darkColorScheme(
            primary = accent.primaryDark,
            onPrimary = accent.onPrimaryDark,
            primaryContainer = accent.containerDark,
            onPrimaryContainer = Color(0xFFF0F0FF),
            secondary = AmberAccentDark,
            onSecondary = Color(0xFF402D00),
            secondaryContainer = Color(0xFF332400),
            onSecondaryContainer = Color(0xFFFFDF9E),
            tertiary = BookWs1Color,
            background = AmoledBackground,
            onBackground = AmoledOnBackground,
            surface = AmoledSurface,
            onSurface = AmoledOnSurface,
            surfaceVariant = AmoledSurfaceVariant,
            onSurfaceVariant = Color(0xFFDCDFEA),
            outline = AmoledOutline,
            outlineVariant = AmoledOutlineVariant
        )
    } else if (isDark) {
        darkColorScheme(
            primary = accent.primaryDark,
            onPrimary = accent.onPrimaryDark,
            primaryContainer = accent.containerDark,
            onPrimaryContainer = Color(0xFFE0E0FF),
            secondary = AmberAccentDark,
            onSecondary = Color(0xFF402D00),
            secondaryContainer = Color(0xFF5B4300),
            onSecondaryContainer = Color(0xFFFFDF9E),
            tertiary = BookWs1Color,
            background = DarkBackground,
            onBackground = DarkOnBackground,
            surface = DarkSurface,
            onSurface = DarkOnSurface,
            surfaceVariant = DarkSurfaceVariant,
            onSurfaceVariant = Color(0xFFC7CBD8),
            outline = DarkOutline
        )
    } else {
        lightColorScheme(
            primary = accent.primaryLight,
            onPrimary = accent.onPrimaryLight,
            primaryContainer = accent.containerLight,
            onPrimaryContainer = Color(0xFF00105C),
            secondary = AmberAccent,
            onSecondary = Color(0xFF281A00),
            secondaryContainer = Color(0xFFFFE088),
            onSecondaryContainer = Color(0xFF251600),
            tertiary = BookWs1Color,
            background = LightBackground,
            onBackground = LightOnBackground,
            surface = LightSurface,
            onSurface = LightOnSurface,
            surfaceVariant = LightSurfaceVariant,
            onSurfaceVariant = Color(0xFF444754),
            outline = LightOutline
        )
    }
}

@Composable
fun VocabTutorTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    accentColor: AccentColor = AccentColor.INDIGO,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && themeMode == AppThemeMode.SYSTEM -> {
            val context = LocalContext.current
            if (systemInDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> getCustomColorScheme(themeMode, accentColor, systemInDark)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Alias for backward compatibility
@Composable
fun VocabMasterTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    accentColor: AccentColor = AccentColor.INDIGO,
    content: @Composable () -> Unit
) = VocabTutorTheme(themeMode = themeMode, accentColor = accentColor, content = content)

