package com.sistemaprestamista.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Esquema claro de marca. La app está diseñada light-only (todas las pantallas
// pintan fondos claros hardcoded), así que NO se respeta el modo oscuro del
// sistema ni dynamic color: mezclarían superficies oscuras (diálogos, menús)
// con los fondos blancos fijos de las pantallas.
private val BrandLightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = BrandSurface,
    primaryContainer = BrandPrimaryContainer,
    onPrimaryContainer = BrandOnPrimaryContainer,
    inversePrimary = BrandPrimaryContainer,
    secondary = BrandSecondary,
    onSecondary = BrandSurface,
    secondaryContainer = BrandSecondaryContainer,
    onSecondaryContainer = BrandOnSecondaryContainer,
    tertiary = BrandTertiary,
    onTertiary = BrandSurface,
    tertiaryContainer = BrandTertiaryContainer,
    onTertiaryContainer = BrandOnTertiaryContainer,
    error = BrandError,
    onError = BrandSurface,
    errorContainer = BrandErrorContainer,
    onErrorContainer = BrandOnErrorContainer,
    background = BrandBackground,
    onBackground = BrandOnBackground,
    surface = BrandSurface,
    onSurface = BrandOnSurface,
    surfaceVariant = BrandSurfaceVariant,
    onSurfaceVariant = BrandOnSurfaceVariant,
    surfaceTint = BrandPrimary,
    inverseSurface = BrandPrimaryDark,
    inverseOnSurface = BrandSurface,
    outline = BrandOutline,
    outlineVariant = BrandOutlineVariant,
)

@Composable
fun SistemaPrestamistaAndroidTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = BrandLightColorScheme,
        typography = Typography,
        content = content,
    )
}
