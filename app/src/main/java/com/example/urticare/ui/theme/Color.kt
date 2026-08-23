package com.example.urticare.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Core Brand Color Palette
val PrimaryPurple = Color(0xFF814B92)
val SuccessGreen = Color(0xFF509729)
val SecondaryTeal = Color(0xFF1A7E97)
val NeutralGrey = Color(0xFF737373)

// Signature 3-Color Gradient Mix Brush (#814B92 -> #509729 -> #1A7E97)
val BrandLinearGradient = Brush.horizontalGradient(
    colors = listOf(PrimaryPurple, SuccessGreen, SecondaryTeal)
)
val BrandSweepGradient = Brush.sweepGradient(
    colors = listOf(PrimaryPurple, SuccessGreen, SecondaryTeal, PrimaryPurple)
)

// Ambient Canvas Colors (Light Mode)
val PureWhite = Color(0xFFFFFFFF)
val LightGreyBg = Color(0xFFF8FAFC)
val TextDarkPrimary = Color(0xFF0F172A)
val TextGreyMuted = Color(0xFF64748B)
val DividerLight = Color(0xFFE2E8F0)

// Glassmorphism Colors
val GlassCardBg = Color(0xFFFFFFFF)
val GlassCardBorder = Color(0xFFE2E8F0)

// Tinted Glass Colors
val TintedPurpleGlass = Color(0x1E814B92)
val TintedGreenGlass = Color(0x1E509729)
val TintedTealGlass = Color(0x1E1A7E97)

// Backward Compatibility Color Mappings
val AmoledBlack = LightGreyBg
val DarkSurface = LightGreyBg
val DarkCard = GlassCardBg
val DarkBorder = GlassCardBorder

val CoralPink = PrimaryPurple
val SoftYellow = SuccessGreen
val PastelIceBlue = SecondaryTeal
val SoftPurple = PrimaryPurple

val LightGray = TextDarkPrimary
val MutedGray = TextGreyMuted
val DarkGray = DividerLight
val AlertRed = Color(0xFFEF4444)
val LinkBlue = SecondaryTeal