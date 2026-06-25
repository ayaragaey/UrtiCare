package com.example.urticare20.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PastelIceBlue,
    secondary = CoralPink,
    tertiary = SoftYellow,
    background = AmoledBlack,
    surface = DarkSurface,
    onPrimary = AmoledBlack,
    onSecondary = AmoledBlack,
    onTertiary = AmoledBlack,
    onBackground = LightGray,
    onSurface = LightGray,
    surfaceVariant = DarkCard,
    outline = DarkBorder
)

@Composable
fun Urticare20Theme(
    darkTheme: Boolean = false, // Default to false for premium light/white mode
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = AmoledBlack.toArgb()
            window.navigationBarColor = AmoledBlack.toArgb()
            
            val windowInsetsController = WindowCompat.getInsetsController(window, view)
            windowInsetsController.isAppearanceLightStatusBars = !darkTheme
            windowInsetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}