package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = PrimaryFixedDim,
    onPrimary = Color(0xFF003353),
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = SecondaryFixedDim,
    onSecondary = Color(0xFF00354A),
    secondaryContainer = Secondary,
    onSecondaryContainer = Color(0xFFC4E7FF),
    tertiary = TertiaryFixedDim,
    onTertiary = Color(0xFF262375),
    tertiaryContainer = Tertiary,
    onTertiaryContainer = Color(0xFFE2DFFF),
    background = Color(0xFF110E2D),
    onBackground = Color(0xFFE5E0FF),
    surface = Color(0xFF141132),
    onSurface = Color(0xFFE5E0FF),
    surfaceVariant = Color(0xFF2A2750),
    onSurfaceVariant = Color(0xFFC4C2E0),
    outline = Color(0xFF8E8BAE),
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    background = SurfaceCanvas,
    onBackground = NeutralNavy,
    surface = Surface,
    onSurface = NeutralNavy,
    surfaceVariant = SurfaceContainerHigh,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    outlineVariant = OutlineVariant,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color disabled to maintain custom Atmospheric Intelligence design branding
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

