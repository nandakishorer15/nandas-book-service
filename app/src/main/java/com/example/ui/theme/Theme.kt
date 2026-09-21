package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = LibraryPrimaryDark,
    onPrimary = LibraryOnPrimaryDark,
    primaryContainer = LibraryPrimaryContainerDark,
    onPrimaryContainer = LibraryOnPrimaryContainerDark,
    secondary = LibrarySecondaryDark,
    onSecondary = LibraryOnSecondaryDark,
    background = LibraryBackgroundDark,
    surface = LibrarySurfaceDark,
    surfaceVariant = LibrarySurfaceVariantDark,
    onBackground = LibraryOnSurfaceDark,
    onSurface = LibraryOnSurfaceDark,
    onSurfaceVariant = LibraryOnSurfaceVariantDark,
    outline = LibraryOutlineVariant
)

private val LightColorScheme = lightColorScheme(
    primary = LibraryPrimary,
    onPrimary = LibraryOnPrimary,
    primaryContainer = LibraryPrimaryContainer,
    onPrimaryContainer = LibraryOnPrimaryContainer,
    secondary = LibrarySecondary,
    onSecondary = LibraryOnSecondary,
    secondaryContainer = LibrarySecondaryContainer,
    onSecondaryContainer = LibraryOnSecondaryContainer,
    background = LibraryBackground,
    surface = LibrarySurface,
    surfaceVariant = LibrarySurfaceVariant,
    onBackground = LibraryOnSurface,
    onSurface = LibraryOnSurface,
    onSurfaceVariant = LibraryOnSurfaceVariant,
    outline = LibraryOutline
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep editorial book branding consistent
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
