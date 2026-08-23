package com.example.urticare.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun HomeOutlineIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        
        // Roof path
        val roofPath = Path().apply {
            moveTo(w * 0.15f, h * 0.45f)
            lineTo(w * 0.5f, h * 0.15f)
            lineTo(w * 0.85f, h * 0.45f)
        }
        drawPath(
            path = roofPath,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        
        // Walls path
        val wallsPath = Path().apply {
            moveTo(w * 0.22f, h * 0.42f)
            lineTo(w * 0.22f, h * 0.85f)
            lineTo(w * 0.78f, h * 0.85f)
            lineTo(w * 0.78f, h * 0.42f)
        }
        drawPath(
            path = wallsPath,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        
        // Door
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.4f, h * 0.6f),
            size = Size(w * 0.2f, h * 0.25f),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun AnalyticsOutlineIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        
        // Clipboard Outline Box
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.22f, h * 0.22f),
            size = Size(w * 0.56f, h * 0.65f),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round)
        )
        
        // Clipboard Clip at top
        val clipPath = Path().apply {
            moveTo(w * 0.38f, h * 0.22f)
            lineTo(w * 0.38f, h * 0.14f)
            lineTo(w * 0.62f, h * 0.14f)
            lineTo(w * 0.62f, h * 0.22f)
        }
        drawPath(
            path = clipPath,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        
        // Data lines inside clipboard
        drawLine(
            color = color,
            start = Offset(w * 0.34f, h * 0.38f),
            end = Offset(w * 0.66f, h * 0.38f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.34f, h * 0.52f),
            end = Offset(w * 0.66f, h * 0.52f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.34f, h * 0.66f),
            end = Offset(w * 0.54f, h * 0.66f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun ChatbotOutlineIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        
        // Robot head outline
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.18f, h * 0.26f),
            size = Size(w * 0.64f, h * 0.52f),
            cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx()),
            style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round)
        )
        
        // Antenna
        drawLine(
            color = color,
            start = Offset(w * 0.5f, h * 0.26f),
            end = Offset(w * 0.5f, h * 0.14f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawCircle(
            color = color,
            center = Offset(w * 0.5f, h * 0.12f),
            radius = 2.dp.toPx()
        )
        
        // Eyes
        drawCircle(
            color = color,
            center = Offset(w * 0.36f, h * 0.48f),
            radius = 2.dp.toPx()
        )
        drawCircle(
            color = color,
            center = Offset(w * 0.64f, h * 0.48f),
            radius = 2.dp.toPx()
        )
        
        // Mouth
        val mouthPath = Path().apply {
            moveTo(w * 0.42f, h * 0.64f)
            quadraticBezierTo(w * 0.5f, h * 0.70f, w * 0.58f, h * 0.64f)
        }
        drawPath(
            path = mouthPath,
            color = color,
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun BookOutlineIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        
        // Left page outline
        val leftPagePath = Path().apply {
            moveTo(w * 0.5f, h * 0.82f)
            quadraticBezierTo(w * 0.35f, h * 0.76f, w * 0.15f, h * 0.82f)
            lineTo(w * 0.15f, h * 0.28f)
            quadraticBezierTo(w * 0.35f, h * 0.22f, w * 0.5f, h * 0.28f)
            close()
        }
        drawPath(
            path = leftPagePath,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        
        // Right page outline
        val rightPagePath = Path().apply {
            moveTo(w * 0.5f, h * 0.82f)
            quadraticBezierTo(w * 0.65f, h * 0.76f, w * 0.85f, h * 0.82f)
            lineTo(w * 0.85f, h * 0.28f)
            quadraticBezierTo(w * 0.65f, h * 0.22f, w * 0.5f, h * 0.28f)
            close()
        }
        drawPath(
            path = rightPagePath,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        
        // Spine center line
        drawLine(
            color = color,
            start = Offset(w * 0.5f, h * 0.28f),
            end = Offset(w * 0.5f, h * 0.82f),
            strokeWidth = 2.dp.toPx()
        )
    }
}

@Composable
fun UserOutlineIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height

        // Head circle
        drawCircle(
            color = color,
            center = Offset(w * 0.5f, h * 0.32f),
            radius = w * 0.18f,
            style = Stroke(width = 2.dp.toPx())
        )

        // Shoulders arch
        val shoulderPath = Path().apply {
            moveTo(w * 0.18f, h * 0.82f)
            quadraticBezierTo(w * 0.5f, h * 0.52f, w * 0.82f, h * 0.82f)
        }
        drawPath(
            path = shoulderPath,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

