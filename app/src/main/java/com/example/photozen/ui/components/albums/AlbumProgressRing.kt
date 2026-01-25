package com.example.photozen.ui.components.albums

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.photozen.ui.theme.KeepGreen
import com.example.photozen.ui.theme.MaybeAmber
import com.example.photozen.ui.theme.PicZenMotion

/**
 * Album progress ring component
 *
 * Circular progress indicator that displays sorting completion percentage.
 *
 * ## Design Specs
 * - Default size: 36dp
 * - Stroke width: 3dp
 * - Background track: onSurface 10% alpha
 * - Progress color: Amber (0-50%), Primary (50-99%), Green (100%)
 * - Progress animation: Springs.playful()
 * - Completion pulse: 1.0 -> 1.2 -> 1.0
 *
 * @param progress Progress value (0.0 - 1.0)
 * @param modifier Modifier
 * @param size Ring size (default 36dp)
 * @param strokeWidth Stroke width (default 3dp)
 * @param showPercentage Whether to show percentage text in center
 * @param animateOnComplete Whether to play pulse animation on completion
 */
@Composable
fun AlbumProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    strokeWidth: Dp = 3.dp,
    showPercentage: Boolean = false,
    animateOnComplete: Boolean = true
) {
    // Progress animation
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = PicZenMotion.Springs.playful(),
        label = "progressAnimation"
    )

    // Completion state detection
    val isComplete = progress >= 1f
    var wasComplete by remember { mutableStateOf(isComplete) }

    // Completion pulse animation
    val pulseScale by animateFloatAsState(
        targetValue = if (isComplete && !wasComplete && animateOnComplete) 1.2f else 1f,
        animationSpec = PicZenMotion.Springs.playful(),
        label = "pulseScale",
        finishedListener = {
            if (isComplete) wasComplete = true
        }
    )

    // Reset wasComplete when progress drops below 1
    LaunchedEffect(progress) {
        if (progress < 1f) {
            wasComplete = false
        }
    }

    // Dynamic color based on progress
    val ringColor = when {
        progress >= 1f -> KeepGreen
        progress >= 0.5f -> MaterialTheme.colorScheme.primary
        else -> MaybeAmber
    }

    val animatedColor by animateColorAsState(
        targetValue = ringColor,
        animationSpec = tween(PicZenMotion.Duration.Normal),
        label = "colorAnimation"
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = pulseScale
                scaleY = pulseScale
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sweepAngle = animatedProgress * 360f
            val strokePx = strokeWidth.toPx()
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            val topLeft = Offset(strokePx / 2, strokePx / 2)

            // Background track
            drawArc(
                color = Color.White.copy(alpha = 0.2f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Progress arc
            if (animatedProgress > 0f) {
                drawArc(
                    color = animatedColor,
                    startAngle = -90f,  // Start from 12 o'clock
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        // Optional: Center percentage text
        if (showPercentage && !isComplete) {
            Text(
                text = "${(animatedProgress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        // Completion state: Show checkmark icon
        AnimatedVisibility(
            visible = isComplete,
            enter = scaleIn(animationSpec = PicZenMotion.Springs.playful()) + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Completed",
                modifier = Modifier.size(size * 0.5f),
                tint = KeepGreen
            )
        }
    }
}
