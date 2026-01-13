package com.example.photozen.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * PicZen Dark Color Scheme
 * Optimized for photo viewing with true dark backgrounds
 */
private val PicZenDarkColorScheme = darkColorScheme(
    primary = PicZenPrimary,
    onPrimary = PicZenOnPrimary,
    primaryContainer = PicZenPrimaryContainer,
    onPrimaryContainer = PicZenOnPrimaryContainer,
    secondary = PicZenSecondary,
    onSecondary = PicZenOnSecondary,
    secondaryContainer = PicZenSecondaryContainer,
    onSecondaryContainer = PicZenOnSecondaryContainer,
    tertiary = PicZenTertiary,
    onTertiary = PicZenOnTertiary,
    tertiaryContainer = PicZenTertiaryContainer,
    onTertiaryContainer = PicZenOnTertiaryContainer,
    background = PicZenBackground,
    onBackground = PicZenOnBackground,
    surface = PicZenSurface,
    onSurface = PicZenOnSurface,
    surfaceVariant = PicZenSurfaceVariant,
    onSurfaceVariant = PicZenOnSurfaceVariant,
    outline = PicZenOutline,
    outlineVariant = PicZenOutlineVariant,
    inverseSurface = PicZenInverseSurface,
    inverseOnSurface = PicZenInverseOnSurface,
    inversePrimary = PicZenInversePrimary,
    error = PicZenError,
    onError = PicZenOnError,
    errorContainer = PicZenErrorContainer,
    onErrorContainer = PicZenOnErrorContainer
)

/**
 * PicZen Light Color Scheme (for future use if needed)
 */
private val PicZenLightColorScheme = lightColorScheme(
    primary = PicZenPrimaryContainer,
    onPrimary = PicZenOnPrimaryContainer,
    primaryContainer = PicZenPrimary,
    onPrimaryContainer = PicZenOnPrimary,
    secondary = PicZenSecondaryContainer,
    onSecondary = PicZenOnSecondaryContainer,
    secondaryContainer = PicZenSecondary,
    onSecondaryContainer = PicZenOnSecondary,
    background = PicZenOnBackground,
    onBackground = PicZenBackground,
    surface = PicZenInverseSurface,
    onSurface = PicZenInverseOnSurface
)

/**
 * PicZen Theme
 * 
 * @param darkTheme Defaults to true for optimal photo viewing experience
 * @param content Composable content
 */
@Composable
fun PicZenTheme(
    darkTheme: Boolean = true, // Default to dark theme for photo viewing
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) PicZenDarkColorScheme else PicZenLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
