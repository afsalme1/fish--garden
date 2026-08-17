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

private val DarkColorScheme =
  darkColorScheme(
    primary = AquaPrimaryDark,
    onPrimary = AquaOnPrimaryDark,
    primaryContainer = AquaPrimaryContainerDark,
    onPrimaryContainer = AquaOnPrimaryContainerDark,
    secondary = AquaSecondaryDark,
    onSecondary = AquaOnSecondaryDark,
    secondaryContainer = AquaSecondaryContainerDark,
    onSecondaryContainer = AquaOnSecondaryContainerDark,
    tertiary = AquaTertiaryDark,
    onTertiary = AquaOnTertiaryDark,
    tertiaryContainer = AquaTertiaryContainerDark,
    onTertiaryContainer = AquaOnTertiaryContainerDark,
    background = AquaBackgroundDark,
    onBackground = AquaOnBackgroundDark,
    surface = AquaSurfaceDark,
    onSurface = AquaOnSurfaceDark,
    surfaceVariant = AquaSurfaceVariantDark,
    onSurfaceVariant = AquaOnSurfaceVariantDark,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = AquaPrimaryLight,
    onPrimary = AquaOnPrimaryLight,
    primaryContainer = AquaPrimaryContainerLight,
    onPrimaryContainer = AquaOnPrimaryContainerLight,
    secondary = AquaSecondaryLight,
    onSecondary = AquaOnSecondaryLight,
    secondaryContainer = AquaSecondaryContainerLight,
    onSecondaryContainer = AquaOnSecondaryContainerLight,
    tertiary = AquaTertiaryLight,
    onTertiary = AquaOnTertiaryLight,
    tertiaryContainer = AquaTertiaryContainerLight,
    onTertiaryContainer = AquaOnTertiaryContainerLight,
    background = AquaBackgroundLight,
    onBackground = AquaOnBackgroundLight,
    surface = AquaSurfaceLight,
    onSurface = AquaOnSurfaceLight,
    surfaceVariant = AquaSurfaceVariantLight,
    onSurfaceVariant = AquaOnSurfaceVariantLight,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Use our vibrant aquatic theme by default
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
