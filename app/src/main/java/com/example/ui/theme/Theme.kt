package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val StudioDarkColorScheme = darkColorScheme(
    primary = EarbudCyan,
    onPrimary = Color(0xFF00262E),
    primaryContainer = EarbudCyanContainer,
    onPrimaryContainer = OnEarbudCyanContainer,
    secondary = SpeakerAmber,
    onSecondary = Color(0xFF2B1B00),
    secondaryContainer = SpeakerAmberContainer,
    onSecondaryContainer = OnSpeakerAmberContainer,
    tertiary = GeminiLiveEmerald,
    onTertiary = Color(0xFF002819),
    background = MidnightBackground,
    onBackground = TextPrimaryLight,
    surface = MidnightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = MidnightSurfaceVariant,
    onSurfaceVariant = TextSecondaryMuted,
    error = ErrorCoral,
    errorContainer = ErrorContainerDark,
    onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
fun HamZabanTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StudioDarkColorScheme,
        typography = Typography,
        content = content
    )
}
