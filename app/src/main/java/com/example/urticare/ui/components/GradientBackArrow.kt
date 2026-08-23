package com.example.urticare.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Custom gradient Back Arrow matching the rounded, smooth Green-to-Teal arrow graphic.
 */
@Composable
fun CustomGradientBackArrow(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    strokeWidth: Dp = 3.2.dp
) {
    val arrowBrush = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF509729), // Green tip (#509729)
            Color(0xFF2E8B75), // Smooth blend
            Color(0xFF1A7E97)  // Teal stem (#1A7E97)
        )
    )

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = strokeWidth.toPx()

        // 1. Horizontal Stem: from right end (w * 0.95) to left tip (w * 0.1)
        drawLine(
            brush = arrowBrush,
            start = Offset(w * 0.95f, h * 0.5f),
            end = Offset(w * 0.1f, h * 0.5f),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )

        // 2. Chevron Top Arm: from (w * 0.55, h * 0.15) to (w * 0.1f, h * 0.5f)
        drawLine(
            brush = arrowBrush,
            start = Offset(w * 0.55f, h * 0.15f),
            end = Offset(w * 0.1f, h * 0.5f),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )

        // 3. Chevron Bottom Arm: from (w * 0.55, h * 0.85) to (w * 0.1f, h * 0.5f)
        drawLine(
            brush = arrowBrush,
            start = Offset(w * 0.55f, h * 0.85f),
            end = Offset(w * 0.1f, h * 0.5f),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Standard Circular Back Button containing the Custom Gradient Back Arrow.
 */
@Composable
fun GradientBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    Box(
        modifier = modifier
            .background(Color.White, shape = CircleShape)
            .border(1.dp, Color(0xFFE2E8F0), CircleShape)
            .size(size)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        CustomGradientBackArrow(size = 18.dp, strokeWidth = 3.2.dp)
    }
}
