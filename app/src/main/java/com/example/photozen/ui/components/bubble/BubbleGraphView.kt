package com.example.photozen.ui.components.bubble

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Interactive bubble graph visualization with smooth physics-based dragging.
 * 
 * Features:
 * - Ultra-smooth draggable bubbles that follow finger precisely
 * - Physics-based collision and bouncing effects
 * - Inertia-based animation when released
 * - Positions are saved and restored
 * - Tap to select a bubble
 * - Long press for options
 */
@Composable
fun BubbleGraphView(
    nodes: List<BubbleNode>,
    onBubbleClick: (BubbleNode) -> Unit,
    onBubbleLongClick: ((BubbleNode) -> Unit)? = null,
    savedPositions: Map<String, Pair<Float, Float>> = emptyMap(),
    onPositionsChanged: ((Map<String, Pair<Float, Float>>) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    
    // Container size
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    
    // Bubble states with animatable positions
    val bubbleAnimatables = remember(nodes.map { it.id }.sorted().joinToString()) {
        nodes.map { node ->
            node to Animatable(Offset.Zero, Offset.VectorConverter)
        }
    }
    
    // Track initialization
    var initialized by remember { mutableStateOf(false) }
    
    // Track dragging state
    var draggedNodeId by remember { mutableStateOf<String?>(null) }
    
    // Velocity tracking for inertia
    var lastDragVelocity by remember { mutableStateOf(Offset.Zero) }
    
    // Pulse animation for center bubble
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    // Initialize positions when size is available
    LaunchedEffect(containerSize, nodes, savedPositions) {
        if (containerSize.width > 0 && containerSize.height > 0 && !initialized) {
            val centerX = containerSize.width / 2f
            val centerY = containerSize.height / 2f
            val childNodes = nodes.filter { !it.isCenter }
            val orbitRadius = min(containerSize.width, containerSize.height) * 0.3f
            
            bubbleAnimatables.forEachIndexed { index, (node, animatable) ->
                val savedPos = savedPositions[node.id]
                val targetPos = if (savedPos != null) {
                    Offset(savedPos.first, savedPos.second)
                } else if (node.isCenter) {
                    Offset(centerX, centerY)
                } else {
                    val childIndex = childNodes.indexOf(node)
                    val total = childNodes.size
                    val angle = (2 * Math.PI * childIndex / total).toFloat() - (Math.PI / 2).toFloat()
                    Offset(
                        centerX + orbitRadius * cos(angle),
                        centerY + orbitRadius * sin(angle)
                    )
                }
                animatable.snapTo(targetPos)
            }
            initialized = true
        }
    }
    
    // Collision detection and resolution
    fun resolveCollisions(
        currentIndex: Int,
        currentPos: Offset,
        excludeDragged: Boolean = true
    ): Offset {
        var resolvedPos = currentPos
        val currentNode = bubbleAnimatables[currentIndex].first
        val margin = 8f
        
        bubbleAnimatables.forEachIndexed { otherIndex, (otherNode, otherAnimatable) ->
            if (otherIndex == currentIndex) return@forEachIndexed
            if (excludeDragged && otherNode.id == draggedNodeId) return@forEachIndexed
            
            val otherPos = otherAnimatable.value
            val minDist = currentNode.radius + otherNode.radius + margin
            val diff = resolvedPos - otherPos
            val dist = sqrt(diff.x * diff.x + diff.y * diff.y)
            
            if (dist < minDist && dist > 0.1f) {
                // Push away
                val overlap = minDist - dist
                val direction = diff / dist
                resolvedPos += direction * overlap
            }
        }
        
        // Boundary constraints
        val boundaryMargin = currentNode.radius + 4f
        resolvedPos = Offset(
            resolvedPos.x.coerceIn(boundaryMargin, containerSize.width - boundaryMargin),
            resolvedPos.y.coerceIn(boundaryMargin, containerSize.height - boundaryMargin)
        )
        
        return resolvedPos
    }
    
    // Push other bubbles away from dragged bubble
    fun pushOtherBubbles(draggedIndex: Int, draggedPos: Offset) {
        val draggedNode = bubbleAnimatables[draggedIndex].first
        val margin = 8f
        
        bubbleAnimatables.forEachIndexed { otherIndex, (otherNode, otherAnimatable) ->
            if (otherIndex == draggedIndex) return@forEachIndexed
            if (otherNode.isCenter) return@forEachIndexed // Don't push center
            
            val otherPos = otherAnimatable.value
            val minDist = draggedNode.radius + otherNode.radius + margin
            val diff = otherPos - draggedPos
            val dist = sqrt(diff.x * diff.x + diff.y * diff.y)
            
            if (dist < minDist && dist > 0.1f) {
                // Calculate push direction and amount
                val overlap = minDist - dist
                val direction = diff / dist
                val targetPos = resolveCollisions(otherIndex, otherPos + direction * (overlap * 1.2f), excludeDragged = true)
                
                // Animate other bubble away with spring physics
                scope.launch {
                    otherAnimatable.animateTo(
                        targetPos,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessHigh
                        )
                    )
                }
            }
        }
    }
    
    // Save positions helper
    fun savePositions() {
        if (onPositionsChanged != null && initialized) {
            val positions = bubbleAnimatables.associate { (node, animatable) ->
                node.id to Pair(animatable.value.x, animatable.value.y)
            }
            onPositionsChanged(positions)
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
    ) {
        // Draw connection lines
        if (initialized && containerSize.width > 0) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerBubble = bubbleAnimatables.find { it.first.isCenter }
                if (centerBubble != null) {
                    val centerPos = centerBubble.second.value
                    bubbleAnimatables.filter { !it.first.isCenter }.forEach { (_, animatable) ->
                        drawLine(
                            color = Color.White.copy(alpha = 0.2f),
                            start = centerPos,
                            end = animatable.value,
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }
            }
        }
        
        // Render each bubble as a separate composable for better touch handling
        bubbleAnimatables.forEachIndexed { index, (node, animatable) ->
            key(node.id) {
                val position = animatable.value
                val isDragging = node.id == draggedNodeId
                val radiusDp = with(density) { node.radius.toDp() }
                val scale = if (node.isCenter) pulseScale else if (isDragging) 1.1f else 1f
                
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (position.x - node.radius).roundToInt(),
                                (position.y - node.radius).roundToInt()
                            )
                        }
                        .size(radiusDp * 2)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .then(
                            if (!node.isCenter) {
                                Modifier.pointerInput(node.id) {
                                    var velocityTracker = mutableListOf<Pair<Long, Offset>>()
                                    
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        draggedNodeId = node.id
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        velocityTracker.clear()
                                        
                                        var lastPosition = down.position
                                        var lastTime = System.currentTimeMillis()
                                        velocityTracker.add(lastTime to position)
                                        
                                        // Immediately snap to finger position during drag
                                        scope.launch {
                                            animatable.stop()
                                        }
                                        
                                        try {
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                
                                                if (event.type == PointerEventType.Move) {
                                                    val change = event.changes.firstOrNull() ?: continue
                                                    if (change.pressed) {
                                                        val delta = change.positionChange()
                                                        change.consume()
                                                        
                                                        // Directly update position - no animation delay
                                                        val currentPos = animatable.value
                                                        var newPos = currentPos + delta
                                                        
                                                        // Apply boundary constraints immediately
                                                        val boundaryMargin = node.radius + 4f
                                                        newPos = Offset(
                                                            newPos.x.coerceIn(boundaryMargin, containerSize.width - boundaryMargin),
                                                            newPos.y.coerceIn(boundaryMargin, containerSize.height - boundaryMargin)
                                                        )
                                                        
                                                        // Snap to new position (no animation = instant response)
                                                        scope.launch {
                                                            animatable.snapTo(newPos)
                                                        }
                                                        
                                                        // Push other bubbles
                                                        pushOtherBubbles(index, newPos)
                                                        
                                                        // Track velocity
                                                        val currentTime = System.currentTimeMillis()
                                                        velocityTracker.add(currentTime to newPos)
                                                        // Keep only recent samples
                                                        if (velocityTracker.size > 10) {
                                                            velocityTracker = velocityTracker.takeLast(10).toMutableList()
                                                        }
                                                        
                                                        lastPosition = change.position
                                                        lastTime = currentTime
                                                    }
                                                }
                                                
                                                if (event.changes.all { it.changedToUp() }) {
                                                    // Calculate release velocity
                                                    val velocity = if (velocityTracker.size >= 2) {
                                                        val recent = velocityTracker.takeLast(5)
                                                        val timeDiff = (recent.last().first - recent.first().first).coerceAtLeast(1)
                                                        val posDiff = recent.last().second - recent.first().second
                                                        Offset(
                                                            posDiff.x / timeDiff * 100,
                                                            posDiff.y / timeDiff * 100
                                                        )
                                                    } else {
                                                        Offset.Zero
                                                    }
                                                    
                                                    draggedNodeId = null
                                                    
                                                    // Apply inertia animation if velocity is significant
                                                    if (velocity.getDistance() > 50f) {
                                                        val currentPos = animatable.value
                                                        var targetPos = currentPos + velocity * 2f
                                                        
                                                        // Resolve collisions and boundaries for target
                                                        targetPos = resolveCollisions(index, targetPos, excludeDragged = false)
                                                        
                                                        scope.launch {
                                                            animatable.animateTo(
                                                                targetPos,
                                                                animationSpec = spring(
                                                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                                                    stiffness = Spring.StiffnessLow
                                                                )
                                                            )
                                                            savePositions()
                                                        }
                                                    } else {
                                                        savePositions()
                                                    }
                                                    
                                                    break
                                                }
                                            }
                                        } catch (e: Exception) {
                                            draggedNodeId = null
                                            savePositions()
                                        }
                                    }
                                }
                            } else Modifier
                        )
                        .pointerInput(node.id) {
                            detectTapGestures(
                                onTap = {
                                    onBubbleClick(node)
                                },
                                onLongPress = {
                                    if (onBubbleLongClick != null) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onBubbleLongClick(node)
                                    }
                                }
                            )
                        }
                ) {
                    // Draw bubble content
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawBubbleContent(
                            node = node,
                            radius = node.radius,
                            isDragging = isDragging
                        )
                    }
                }
            }
        }
    }
}

/**
 * Draw bubble content (circle, label, count).
 */
private fun DrawScope.drawBubbleContent(
    node: BubbleNode,
    radius: Float,
    isDragging: Boolean
) {
    val center = Offset(size.width / 2, size.height / 2)
    val color = Color(node.color)
    
    // Shadow
    drawCircle(
        color = Color.Black.copy(alpha = if (isDragging) 0.4f else 0.25f),
        radius = radius + (if (isDragging) 6.dp.toPx() else 3.dp.toPx()),
        center = center + Offset(2.dp.toPx(), if (isDragging) 6.dp.toPx() else 3.dp.toPx())
    )
    
    // Main bubble
    drawCircle(
        color = color,
        radius = radius,
        center = center
    )
    
    // Highlight (top-left shine)
    drawCircle(
        color = Color.White.copy(alpha = 0.3f),
        radius = radius * 0.6f,
        center = center + Offset(-radius * 0.25f, -radius * 0.25f)
    )
    
    // Border
    drawCircle(
        color = if (isDragging) Color.White else Color.White.copy(alpha = 0.5f),
        radius = radius,
        center = center,
        style = Stroke(width = if (isDragging) 4.dp.toPx() else 2.dp.toPx())
    )
    
    // Children indicator
    if (node.hasChildren && !node.isCenter) {
        val indicatorSize = radius * 0.25f
        val indicatorCenter = center + Offset(radius * 0.6f, -radius * 0.6f)
        
        drawCircle(
            color = Color.White,
            radius = indicatorSize,
            center = indicatorCenter
        )
        
        val arrowPath = Path().apply {
            moveTo(indicatorCenter.x - indicatorSize * 0.3f, indicatorCenter.y - indicatorSize * 0.3f)
            lineTo(indicatorCenter.x + indicatorSize * 0.4f, indicatorCenter.y)
            lineTo(indicatorCenter.x - indicatorSize * 0.3f, indicatorCenter.y + indicatorSize * 0.3f)
        }
        drawPath(
            path = arrowPath,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
    
    // Draw label and count using native canvas
    drawContext.canvas.nativeCanvas.apply {
        val textPaint = android.graphics.Paint().apply {
            this.color = android.graphics.Color.WHITE
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = (radius * 0.45f).coerceIn(28f, 64f)
            isFakeBoldText = true
            isAntiAlias = true
            setShadowLayer(4f, 0f, 2f, android.graphics.Color.argb(100, 0, 0, 0))
        }
        
        drawText(
            node.label,
            center.x,
            center.y + textPaint.textSize * 0.1f,
            textPaint
        )
        
        if (node.photoCount > 0) {
            val countPaint = android.graphics.Paint(textPaint).apply {
                textSize = textPaint.textSize * 0.7f
                alpha = 200
            }
            drawText(
                "${node.photoCount} 张",
                center.x,
                center.y + textPaint.textSize * 0.8f,
                countPaint
            )
        }
    }
}

/**
 * Extension to calculate distance of an Offset from origin.
 */
private fun Offset.getDistance(): Float = sqrt(x * x + y * y)
