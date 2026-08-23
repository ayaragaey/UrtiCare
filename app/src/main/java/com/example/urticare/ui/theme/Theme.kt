package com.example.urticare.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = PastelIceBlue,
    secondary = CoralPink,
    tertiary = SoftYellow,
    background = AmoledBlack,
    surface = DarkSurface,
    onPrimary = PureWhite,
    onSecondary = PureWhite,
    onTertiary = PureWhite,
    onBackground = LightGray,
    onSurface = LightGray,
    surfaceVariant = DarkCard,
    onSurfaceVariant = LightGray
)

@Composable
fun Urticare20Theme(
    darkTheme: Boolean = false, // Force false for light theme selective glassmorphism
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = AmoledBlack.toArgb()
            window.navigationBarColor = AmoledBlack.toArgb()
            
            val windowInsetsController = WindowCompat.getInsetsController(window, view)
            // Enable dark icons on light backdrops for perfect readability
            windowInsetsController.isAppearanceLightStatusBars = true
            windowInsetsController.isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}