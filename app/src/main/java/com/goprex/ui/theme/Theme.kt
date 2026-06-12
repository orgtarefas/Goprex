package com.goprex.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Orange,
    onPrimary = White,
    primaryContainer = OrangeLight,
    onPrimaryContainer = NavyBlue,
    secondary = NavyBlue,
    onSecondary = White,
    secondaryContainer = NavyBlueLight,
    onSecondaryContainer = White,
    tertiary = OrangeDark,
    background = White,
    onBackground = NavyBlue,
    surface = White,
    onSurface = NavyBlue,
    surfaceVariant = BackgroundGray,
    onSurfaceVariant = TextGray,
    error = ErrorRed,
    onError = White
)

private val DarkColorScheme = darkColorScheme(
    primary = OrangeLight,
    onPrimary = NavyBlue,
    primaryContainer = OrangeDark,
    onPrimaryContainer = White,
    secondary = NavyBlueLight,
    onSecondary = White,
    secondaryContainer = NavyBlueDark,
    onSecondaryContainer = White,
    tertiary = OrangeLight,
    background = NavyBlueDark,
    onBackground = White,
    surface = NavyBlue,
    onSurface = White,
    surfaceVariant = NavyBlueLight,
    onSurfaceVariant = White.copy(alpha = 0.7f),
    error = ErrorRed,
    onError = White
)

@Composable
fun GoprexTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = NavyBlue.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}