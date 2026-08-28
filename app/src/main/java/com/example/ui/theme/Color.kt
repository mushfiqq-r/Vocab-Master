package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Theme Modes
enum class AppThemeMode(val title: String) {
    SYSTEM("System Default"),
    LIGHT("Light"),
    DARK("Dark"),
    AMOLED("AMOLED Black")
}

// 5 Custom Accent Colors
enum class AccentColor(
    val title: String,
    val hexCode: String,
    val primaryLight: Color,
    val primaryDark: Color,
    val containerLight: Color,
    val containerDark: Color,
    val onPrimaryLight: Color,
    val onPrimaryDark: Color
) {
    INDIGO(
        title = "Royal Indigo",
        hexCode = "#283593",
        primaryLight = Color(0xFF283593),
        primaryDark = Color(0xFF9FA8DA),
        containerLight = Color(0xFFDEE0FF),
        containerDark = Color(0xFF232D6B),
        onPrimaryLight = Color.White,
        onPrimaryDark = Color(0xFF0D1442)
    ),
    EMERALD(
        title = "Emerald Sage",
        hexCode = "#00796B",
        primaryLight = Color(0xFF00796B),
        primaryDark = Color(0xFF4DB6AC),
        containerLight = Color(0xFFB2DFDB),
        containerDark = Color(0xFF004D40),
        onPrimaryLight = Color.White,
        onPrimaryDark = Color(0xFF00201A)
    ),
    RUBY(
        title = "Crimson Ruby",
        hexCode = "#C2185B",
        primaryLight = Color(0xFFC2185B),
        primaryDark = Color(0xFFF06292),
        containerLight = Color(0xFFF8BBD0),
        containerDark = Color(0xFF880E4F),
        onPrimaryLight = Color.White,
        onPrimaryDark = Color(0xFF3B0018)
    ),
    AMETHYST(
        title = "Regal Amethyst",
        hexCode = "#7B1FA2",
        primaryLight = Color(0xFF7B1FA2),
        primaryDark = Color(0xFFBA68C8),
        containerLight = Color(0xFFE1BEE7),
        containerDark = Color(0xFF4A148C),
        onPrimaryLight = Color.White,
        onPrimaryDark = Color(0xFF2E003D)
    ),
    AMBER(
        title = "Sunset Amber",
        hexCode = "#E65100",
        primaryLight = Color(0xFFE65100),
        primaryDark = Color(0xFFFFB74D),
        containerLight = Color(0xFFFFE0B2),
        containerDark = Color(0xFFBF360C),
        onPrimaryLight = Color.White,
        onPrimaryDark = Color(0xFF3E1200)
    )
}

// Brand Core Colors
val IndigoPrimary = Color(0xFF283593)
val IndigoPrimaryDark = Color(0xFF9FA8DA)
val IndigoSecondary = Color(0xFF3F51B5)
val IndigoSecondaryDark = Color(0xFFC5CAE9)
val AmberAccent = Color(0xFFFFB300)
val AmberAccentDark = Color(0xFFFFD54F)

// Book Identifier Palette
val BookGreColor = Color(0xFF3949AB)      // Royal Indigo
val BookWs1Color = Color(0xFF00897B)      // Scholarly Teal
val BookWs2Color = Color(0xFF8E24AA)      // Regal Purple

// Status Colors
val StatusNew = Color(0xFF1E88E5)         // Blue
val StatusLearning = Color(0xFFFB8C00)    // Amber Orange
val StatusMastered = Color(0xFF43A047)    // Emerald Green
val StatusDanger = Color(0xFFE53935)      // Crimson Red

// Semantic Quiz & Feedback Tokens
val EmeraldSuccess = Color(0xFF2E7D32)
val EmeraldDark = Color(0xFF1B5E20)
val EmeraldLight = Color(0xFFE8F5E9)
val CoralRed = Color(0xFFD32F2F)
val CoralLight = Color(0xFFFFEBEE)

// Light Theme Tokens
val LightBackground = Color(0xFFF8F9FE)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEEF1F8)
val LightOnBackground = Color(0xFF1A1C24)
val LightOnSurface = Color(0xFF1A1C24)
val LightOutline = Color(0xFFD3D7E3)

// Dark Theme Tokens
val DarkBackground = Color(0xFF0F121C)
val DarkSurface = Color(0xFF181C2A)
val DarkSurfaceVariant = Color(0xFF242A3D)
val DarkOnBackground = Color(0xFFE8EAF3)
val DarkOnSurface = Color(0xFFE8EAF3)
val DarkOutline = Color(0xFF3A425B)

// AMOLED Theme Tokens (Pure Pitch Black for maximum OLED power efficiency & high contrast)
val AmoledBackground = Color(0xFF000000)
val AmoledSurface = Color(0xFF070707)
val AmoledSurfaceVariant = Color(0xFF121212)
val AmoledOnBackground = Color(0xFFFFFFFF)
val AmoledOnSurface = Color(0xFFFFFFFF)
val AmoledOutline = Color(0xFF262626)
val AmoledOutlineVariant = Color(0xFF1C1C1C)
