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
import com.example.photozen.data.repository.ThemeMode

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
 * PicZen Light Color Scheme
 * Clean and bright for daytime use
 */
private val PicZenLightColorScheme = lightColorScheme(
    primary = PicZenLightPrimary,
    onPrimary = PicZenLightOnPrimary,
    primaryContainer = PicZenLightPrimaryContainer,
    onPrimaryContainer = PicZenLightOnPrimaryContainer,
    secondary = PicZenLightSecondary,
    onSecondary = PicZenLightOnSecondary,
    secondaryContainer = PicZenLightSecondaryContainer,
    onSecondaryContainer = PicZenLightOnSecondaryContainer,
    tertiary = PicZenLightTertiary,
    onTertiary = PicZenLightOnTertiary,
    tertiaryContainer = PicZenLightTertiaryContainer,
    onTertiaryContainer = PicZenLightOnTertiaryContainer,
    background = PicZenLightBackground,
    onBackground = PicZenLightOnBackground,
    surface = PicZenLightSurface,
    onSurface = PicZenLightOnSurface,
    surfaceVariant = PicZenLightSurfaceVariant,
    onSurfaceVariant = PicZenLightOnSurfaceVariant,
    outline = PicZenLightOutline,
    outlineVariant = PicZenLightOutlineVariant,
    inverseSurface = PicZenLightInverseSurface,
    inverseOnSurface = PicZenLightInverseOnSurface,
    inversePrimary = PicZenLightInversePrimary,
    error = PicZenLightError,
    onError = PicZenLightOnError,
    errorContainer = PicZenLightErrorContainer,
    onErrorContainer = PicZenLightOnErrorContainer
)

/**
 * PicZen Theme
 * 
 * @param themeMode The theme mode to use (DARK, LIGHT, or SYSTEM)
 * @param content Composable content
 */
@Composable
fun PicZenTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    
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

/**
 * Legacy PicZen Theme for backward compatibility
 * 
 * @param darkTheme Defaults to true for optimal photo viewing experience
 * @param content Composable content
 */
@Composable
fun PicZenTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    PicZenTheme(
        themeMode = if (darkTheme) ThemeMode.DARK else ThemeMode.LIGHT,
        content = content
    )
}
