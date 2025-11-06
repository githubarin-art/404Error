package com.runanywhere.startup_hackathon20.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Keep original schemes for fallback
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

// ========================================
// 404Error Emergency Alert Theme - LIGHT MODE
// ========================================
private val EmergencyLightColorScheme = lightColorScheme(
    // Primary - Safety Red (SOS button, urgent actions)
    primary = SafetyRed,
    onPrimary = OffWhite,
    primaryContainer = SafetyRedLight,
    onPrimaryContainer = Charcoal,
    
    // Secondary - Amber Yellow (warnings, notifications)
    secondary = AmberYellowDark,
    onSecondary = Charcoal,
    secondaryContainer = AmberYellowLight,
    onSecondaryContainer = Charcoal,
    
    // Tertiary - Trust Blue (calm actions, info)
    tertiary = TrustBlue,
    onTertiary = OffWhite,
    tertiaryContainer = TrustBlueLight,
    onTertiaryContainer = Charcoal,
    
    // Background & Surface
    background = OffWhite,
    onBackground = Charcoal,
    surface = LightGray,
    onSurface = Charcoal,
    surfaceVariant = MediumGray,
    onSurfaceVariant = CharcoalMedium,
    
    // Error
    error = CriticalRed,
    onError = OffWhite,
    errorContainer = SafetyRedLight,
    onErrorContainer = Charcoal,
    
    // Outline
    outline = CharcoalLight,
    outlineVariant = MediumGray,
    
    // Surface Tints
    surfaceTint = SafetyRed
)

// ========================================
// 404Error Emergency Alert Theme - DARK MODE (404 Style)
// ========================================
private val EmergencyDarkColorScheme = darkColorScheme(
    // Primary - Safety Red (even more prominent in dark mode)
    primary = SafetyRedLight,
    onPrimary = DarkBackground,
    primaryContainer = SafetyRedDark,
    onPrimaryContainer = OffWhite,
    
    // Secondary - Amber Yellow (high visibility)
    secondary = AmberYellow,
    onSecondary = DarkBackground,
    secondaryContainer = AmberYellowDark,
    onSecondaryContainer = OffWhite,
    
    // Tertiary - Trust Blue (calming in dark)
    tertiary = TrustBlueLight,
    onTertiary = DarkBackground,
    tertiaryContainer = TrustBlueDark,
    onTertiaryContainer = OffWhite,
    
    // Background & Surface - 404 Dark Mode
    background = DarkBackground,
    onBackground = OffWhite,
    surface = DarkSurface,
    onSurface = OffWhite,
    surfaceVariant = DarkerSurface,
    onSurfaceVariant = LightGray,
    
    // Error - Bright in dark mode
    error = ErrorCode,
    onError = DarkBackground,
    errorContainer = SafetyRedDark,
    onErrorContainer = OffWhite,
    
    // Outline
    outline = CharcoalLight,
    outlineVariant = DarkSurface,
    
    // Surface Tints
    surfaceTint = SafetyRedLight
)

@Composable
fun Startup_hackathon20Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color disabled - using custom emergency alert theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // Use 404Error Emergency Alert theme
        darkTheme -> EmergencyDarkColorScheme
        else -> EmergencyLightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}