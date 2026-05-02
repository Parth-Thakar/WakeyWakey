package com.z.wakeywakey.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AppColorScheme = darkColorScheme(
    primary = PurplePrimary,
    onPrimary = TextWhite,
    primaryContainer = PurpleGlow,
    onPrimaryContainer = PurpleLight,
    secondary = PurpleLight,
    onSecondary = TextWhite,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = PurpleLight,
    tertiary = PurpleFuchsia,
    onTertiary = TextWhite,
    background = DeepBlack,
    onBackground = TextWhite,
    surface = DarkSurface,
    onSurface = TextWhite,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextGray,
    outline = BorderColor,
    error = ErrorRed,
    onError = TextWhite,
)

@Composable
fun WakeyWakeyTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DeepBlack.toArgb()
            window.navigationBarColor = DeepBlack.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content
    )
}
