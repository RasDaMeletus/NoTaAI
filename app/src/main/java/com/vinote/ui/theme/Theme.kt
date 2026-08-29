package com.vinote.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = ViNotePrimary,
    onPrimary = ViNoteOnPrimary,
    primaryContainer = ViNotePrimaryContainer,
    onPrimaryContainer = ViNoteOnPrimaryContainer,
    secondary = ViNoteSecondary,
    onSecondary = ViNoteOnPrimary,
    secondaryContainer = ViNoteSecondaryFixed,
    onSecondaryContainer = ViNoteOnSecondaryFixedVariant,
    tertiary = ViNoteTertiary,
    onTertiary = ViNoteOnPrimary,
    tertiaryContainer = ViNoteTertiaryContainer,
    onTertiaryContainer = ViNoteTertiaryFixed,
    background = ViNoteSurface,
    onBackground = ViNoteTextPrimary,
    surface = ViNoteSurfaceContainerLowest,
    onSurface = ViNoteTextPrimary,
    surfaceVariant = ViNoteSurfaceContainer,
    onSurfaceVariant = ViNoteTextSecondary,
    error = ViNoteError,
    onError = ViNoteOnError,
    errorContainer = ViNoteErrorContainer,
    onErrorContainer = ViNoteOnErrorContainer
)

private val LightColorScheme = lightColorScheme(
    primary = ViNotePrimary,
    onPrimary = ViNoteOnPrimary,
    primaryContainer = ViNotePrimaryContainer,
    onPrimaryContainer = ViNoteOnPrimaryContainer,
    secondary = ViNoteSecondary,
    onSecondary = ViNoteOnPrimary,
    secondaryContainer = ViNoteSecondaryFixed,
    onSecondaryContainer = ViNoteOnSecondaryFixedVariant,
    tertiary = ViNoteTertiary,
    onTertiary = ViNoteOnPrimary,
    tertiaryContainer = ViNoteTertiaryContainer,
    onTertiaryContainer = ViNoteTertiaryFixed,
    background = ViNoteSurface,
    onBackground = ViNoteTextPrimary,
    surface = ViNoteSurfaceContainerLowest,
    onSurface = ViNoteTextPrimary,
    surfaceVariant = ViNoteSurfaceContainer,
    onSurfaceVariant = ViNoteTextSecondary,
    error = ViNoteError,
    onError = ViNoteOnError,
    errorContainer = ViNoteErrorContainer,
    onErrorContainer = ViNoteOnErrorContainer
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
