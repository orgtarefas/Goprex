package com.goprex.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = GoPrexOrange,
    onPrimary = Color.White,
    primaryContainer = GoPrexOrangeLight,
    onPrimaryContainer = GoPrexDark,
    secondary = GoPrexDark,
    onSecondary = Color.White,
    secondaryContainer = GoPrexDarkLight,
    onSecondaryContainer = Color.White,
    tertiary = BrandGray,
    onTertiary = GoPrexDark,
    tertiaryContainer = BrandGrayLight,
    onTertiaryContainer = GoPrexDark,
    background = Color.White,
    onBackground = GoPrexDark,
    surface = Color.White,
    onSurface = GoPrexDark,
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF757575),
    error = Color(0xFFD32F2F),
    onError = Color.White
)

@Composable
fun GoprexTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = GoPrexDark.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}