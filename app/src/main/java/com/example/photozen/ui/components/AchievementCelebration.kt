package com.example.photozen.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Data class for confetti particle.
 */
private data class ConfettiParticle(
    val id: Int,
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val rotation: Float,
    val rotationSpeed: Float,
    val size: Float,
    val shape: ConfettiShape
)

private enum class ConfettiShape {
    CIRCLE, SQUARE, STAR, RIBBON
}

/**
 * Confetti celebration overlay with particle animation.
 */
@Composable
fun ConfettiOverlay(
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val particles = remember { mutableStateListOf<ConfettiParticle>() }
    var frameCount by remember { mutableStateOf(0) }
    
    // Initialize particles when visible
    LaunchedEffect(isVisible) {
        if (isVisible) {
            particles.clear()
            val colors = listOf(
                Color(0xFFFF6B6B), // Red
                Color(0xFFFFD93D), // Yellow
                Color(0xFF6BCB77), // Green
                Color(0xFF4D96FF), // Blue
                Color(0xFFC9B1FF), // Purple
                Color(0xFFFF9F45), // Orange
                Color(0xFFFF69B4), // Pink
            )
            
            repeat(80) { i ->
                val startX = Random.nextFloat()
                val startY = -0.1f - Random.nextFloat() * 0.3f
                particles.add(
                    ConfettiParticle(
                        id = i,
                        x = startX,
                        y = startY,
                        vx = (Random.nextFloat() - 0.5f) * 0.02f,
                        vy = 0.008f + Random.nextFloat() * 0.012f,
                        color = colors.random(),
                        rotation = Random.nextFloat() * 360f,
                        rotationSpeed = (Random.nextFloat() - 0.5f) * 10f,
                        size = 8f + Random.nextFloat() * 12f,
                        shape = ConfettiShape.entries.random()
                    )
                )
            }
        }
    }
    
    // Animation loop
    LaunchedEffect(isVisible) {
        if (isVisible) {
            while (true) {
                delay(16) // ~60fps
                frameCount++
                
                // Update particles
                particles.forEachIndexed { index, particle ->
                    particles[index] = particle.copy(
                        x = particle.x + particle.vx,
                        y = particle.y + particle.vy,
                        rotation = particle.rotation + particle.rotationSpeed
                    )
                }
                
                // Remove particles that have fallen off screen
                particles.removeAll { it.y > 1.2f }
                
                // Stop after particles are gone
                if (particles.isEmpty()) break
            }
        }
    }
    
    AnimatedVisibility(
        visible = isVisible && particles.isNotEmpty(),
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Canvas(modifier = modifier.fillMaxSize()) {
            particles.forEach { particle ->
                val x = particle.x * size.width
                val y = particle.y * size.height
                
                rotate(particle.rotation, pivot = Offset(x, y)) {
                    when (particle.shape) {
                        ConfettiShape.CIRCLE -> {
                            drawCircle(
                                color = particle.color,
                                radius = particle.size,
                                center = Offset(x, y)
                            )
                        }
                        ConfettiShape.SQUARE -> {
                            drawRect(
                                color = particle.color,
                                topLeft = Offset(x - particle.size / 2, y - particle.size / 2),
                                size = androidx.compose.ui.geometry.Size(particle.size, particle.size)
                            )
                        }
                        ConfettiShape.STAR -> {
                            val path = Path().apply {
                                val points = 5
                                val outerRadius = particle.size
                                val innerRadius = particle.size * 0.4f
                                for (i in 0 until points * 2) {
                                    val radius = if (i % 2 == 0) outerRadius else innerRadius
                                    val angle = Math.PI * i / points - Math.PI / 2
                                    val px = x + radius * cos(angle).toFloat()
                                    val py = y + radius * sin(angle).toFloat()
                                    if (i == 0) moveTo(px, py) else lineTo(px, py)
                                }
                                close()
                            }
                            drawPath(path, particle.color)
                        }
                        ConfettiShape.RIBBON -> {
                            drawRect(
                                color = particle.color,
                                topLeft = Offset(x - particle.size * 0.3f, y - particle.size),
                                size = androidx.compose.ui.geometry.Size(particle.size * 0.6f, particle.size * 2)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Achievement unlock toast notification with animation.
 */
@Composable
fun AchievementToast(
    achievementName: String,
    achievementDescription: String,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Auto-dismiss after 3 seconds
    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(3500)
            onDismiss()
        }
    }
    
    // Scale animation
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )
    
    // Glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(300)
        ) + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Glow effect behind card
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .scale(scale * 1.05f)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFD700).copy(alpha = glowAlpha),
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
            )
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(scale),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFFD700).copy(alpha = 0.15f),
                                    Color(0xFFFFA500).copy(alpha = 0.1f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Trophy icon with glow
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFFFD700),
                                        Color(0xFFFFA500)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🎉 成就解锁！",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = achievementName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = achievementDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Combined achievement celebration overlay with toast and confetti.
 */
@Composable
fun AchievementCelebration(
    achievementName: String,
    achievementDescription: String,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Confetti falling from top
        ConfettiOverlay(
            isVisible = isVisible,
            modifier = Modifier.fillMaxSize()
        )
        
        // Toast at bottom
        AchievementToast(
            achievementName = achievementName,
            achievementDescription = achievementDescription,
            isVisible = isVisible,
            onDismiss = onDismiss,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
