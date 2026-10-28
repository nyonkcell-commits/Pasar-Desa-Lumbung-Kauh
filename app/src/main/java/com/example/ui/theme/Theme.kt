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
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF00390F),
    primaryContainer = Color(0xFF1B5E20),
    onPrimaryContainer = Color(0xFFA5D6A7),
    secondary = Color(0xFFFFB74D),
    onSecondary = Color(0xFF4E2600),
    secondaryContainer = Color(0xFF7B3D00),
    onSecondaryContainer = Color(0xFFFFDCC1),
    tertiary = Color(0xFFFF8A80),
    background = PasarDarkBackground,
    surface = PasarDarkSurface,
    surfaceVariant = PasarDarkSurfaceVariant,
    onBackground = Color(0xFFE2E3DE),
    onSurface = Color(0xFFE2E3DE),
    outline = Color(0xFF8C9388)
)

private val LightColorScheme = lightColorScheme(
    primary = PasarGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = PasarGreenContainer,
    onPrimaryContainer = PasarOnGreenContainer,
    secondary = PasarGoldAmber,
    onSecondary = Color.White,
    secondaryContainer = PasarAmberContainer,
    onSecondaryContainer = PasarOnAmberContainer,
    tertiary = PasarTerracotta,
    onTertiary = Color.White,
    tertiaryContainer = PasarTerracottaContainer,
    background = PasarLightBackground,
    surface = PasarLightSurface,
    surfaceVariant = PasarLightSurfaceVariant,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    outline = PasarLightBorder
)

@Composable
fun PasarDesaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set false to preserve our custom market identity
    content: @Composable () -> Unit
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
