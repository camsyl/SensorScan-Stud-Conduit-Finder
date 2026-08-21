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

private val DarkColorScheme = darkColorScheme(
    primary = AmberPrimary,
    onPrimary = Color(0xFF261900),
    primaryContainer = AmberSecondary,
    onPrimaryContainer = Color(0xFF332000),
    secondary = CyanElectric,
    onSecondary = Color(0xFF00363D),
    secondaryContainer = Color(0xFF004F58),
    onSecondaryContainer = Color(0xFF9EEFFE),
    tertiary = TargetGreen,
    onTertiary = Color(0xFF00391A),
    background = CarbonBackground,
    onBackground = TextHighEmphasis,
    surface = CarbonSurface,
    onSurface = TextHighEmphasis,
    surfaceVariant = CarbonSurfaceVariant,
    onSurfaceVariant = TextMediumEmphasis,
    outline = CarbonBorder,
    error = HazardRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = LightToolPrimary,
    onPrimary = Color.White,
    secondary = LightToolSecondary,
    onSecondary = Color.White,
    tertiary = TargetGreen,
    background = LightToolBackground,
    onBackground = Color(0xFF191C20),
    surface = LightToolSurface,
    onSurface = Color(0xFF191C20),
    surfaceVariant = LightToolSurfaceVariant,
    onSurfaceVariant = Color(0xFF44474E),
    outline = Color(0xFFB0B8C4),
    error = HazardRed
)

@Composable
fun SensorScanTheme(
    darkTheme: Boolean = true, // Default to high-contrast dark industrial instrument theme
    dynamicColor: Boolean = false, // Keep tool identity crisp by default
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
