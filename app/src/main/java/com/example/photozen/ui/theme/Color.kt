package com.example.photozen.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * PicZen Color Palette
 * 
 * Design Philosophy:
 * - Dark theme optimized for photo viewing (reduces eye strain, makes photos pop)
 * - Neutral grays to not distract from photo colors
 * - Accent colors for actions: Green (Keep), Red (Trash), Amber (Maybe)
 */

// Primary - Subtle teal accent for primary actions
val PicZenPrimary = Color(0xFF5EEAD4)           // Teal 300
val PicZenOnPrimary = Color(0xFF003731)
val PicZenPrimaryContainer = Color(0xFF004D44)
val PicZenOnPrimaryContainer = Color(0xFF9EF5E5)

// Secondary - Warm neutral for secondary elements
val PicZenSecondary = Color(0xFFB4C7C3)
val PicZenOnSecondary = Color(0xFF1F312F)
val PicZenSecondaryContainer = Color(0xFF354845)
val PicZenOnSecondaryContainer = Color(0xFFD0E4DF)

// Tertiary - Amber for "Maybe" status
val PicZenTertiary = Color(0xFFFBBF24)          // Amber 400
val PicZenOnTertiary = Color(0xFF3D2800)
val PicZenTertiaryContainer = Color(0xFF573C00)
val PicZenOnTertiaryContainer = Color(0xFFFFDEA6)

// Background & Surface - True dark for photo viewing
val PicZenBackground = Color(0xFF0F0F0F)        // Near black
val PicZenOnBackground = Color(0xFFE1E3E0)
val PicZenSurface = Color(0xFF171717)           // Slightly lighter
val PicZenOnSurface = Color(0xFFE1E3E0)
val PicZenSurfaceVariant = Color(0xFF252525)
val PicZenOnSurfaceVariant = Color(0xFFA3A3A3)

// Outline
val PicZenOutline = Color(0xFF525252)
val PicZenOutlineVariant = Color(0xFF3F4946)

// Inverse
val PicZenInverseSurface = Color(0xFFE1E3E0)
val PicZenInverseOnSurface = Color(0xFF191C1B)
val PicZenInversePrimary = Color(0xFF006B5F)

// Action Colors
val KeepGreen = Color(0xFF22C55E)               // Green 500 - Swipe Right
val TrashRed = Color(0xFFEF4444)                // Red 500 - Swipe Left
val MaybeAmber = Color(0xFFFBBF24)              // Amber 400 - Swipe Up

// Error
val PicZenError = Color(0xFFFFB4AB)
val PicZenOnError = Color(0xFF690005)
val PicZenErrorContainer = Color(0xFF93000A)
val PicZenOnErrorContainer = Color(0xFFFFDAD6)

// ==================== Light Theme Colors ====================

// Light Primary - Teal accent
val PicZenLightPrimary = Color(0xFF006B5F)
val PicZenLightOnPrimary = Color(0xFFFFFFFF)
val PicZenLightPrimaryContainer = Color(0xFF9EF5E5)
val PicZenLightOnPrimaryContainer = Color(0xFF00201C)

// Light Secondary
val PicZenLightSecondary = Color(0xFF4A635F)
val PicZenLightOnSecondary = Color(0xFFFFFFFF)
val PicZenLightSecondaryContainer = Color(0xFFCCE8E2)
val PicZenLightOnSecondaryContainer = Color(0xFF06201C)

// Light Tertiary - Amber
val PicZenLightTertiary = Color(0xFF7D5700)
val PicZenLightOnTertiary = Color(0xFFFFFFFF)
val PicZenLightTertiaryContainer = Color(0xFFFFDEA6)
val PicZenLightOnTertiaryContainer = Color(0xFF271900)

// Light Background & Surface
val PicZenLightBackground = Color(0xFFFBFDFB)
val PicZenLightOnBackground = Color(0xFF191C1B)
val PicZenLightSurface = Color(0xFFFBFDFB)
val PicZenLightOnSurface = Color(0xFF191C1B)
val PicZenLightSurfaceVariant = Color(0xFFDBE5E1)
val PicZenLightOnSurfaceVariant = Color(0xFF3F4946)

// Light Outline
val PicZenLightOutline = Color(0xFF6F7976)
val PicZenLightOutlineVariant = Color(0xFFBFC9C5)

// Light Inverse
val PicZenLightInverseSurface = Color(0xFF2D3130)
val PicZenLightInverseOnSurface = Color(0xFFEFF1EF)
val PicZenLightInversePrimary = Color(0xFF5EEAD4)

// Light Error
val PicZenLightError = Color(0xFFBA1A1A)
val PicZenLightOnError = Color(0xFFFFFFFF)
val PicZenLightErrorContainer = Color(0xFFFFDAD6)
val PicZenLightOnErrorContainer = Color(0xFF410002)
