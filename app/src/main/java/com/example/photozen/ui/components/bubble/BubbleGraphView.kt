package com.example.photozen.ui.components.bubble

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/**
 * Interactive bubble graph visualization with physics simulation.
 * 
 * Features:
 * - Center node (parent tag) fixed in center
 * - Child nodes float around with spring physics
 * - Tap to select a bubble
 * - Long press to navigate into bubble (show children)
 * - Drag to move bubbles (they spring back)
 * 
 * @param nodes List of bubble nodes to display
 * @param onBubbleClick Callback when a bubble is clicked (tap)
 * @param onBubbleLongClick Callback when a bubble is long pressed
 * @param modifier Modifier for the graph
 */
@Composable
fun BubbleGraphView(
    nodes: List<BubbleNode>,
    onBubbleClick: (BubbleNode) -> Unit,
    onBubbleLongClick: ((BubbleNode) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    
    // Physics state
    var physicsEngine by remember { mutableStateOf<BubblePhysicsEngine?>(null) }
    val bubbleStates = remember { mutableStateListOf<BubbleState>() }
    var lastFrameTime by remember { mutableStateOf(0L) }
    
    // Dragging state
    var draggedBubbleIndex by remember { mutableStateOf(-1) }
    
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
    
    // Initialize physics engine and bubble states when size changes
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(nodes, onBubbleLongClick) {
                detectTapGestures(
                    onTap = { offset ->
                        // Find clicked bubble
                        bubbleStates.forEachIndexed { _, state ->
                            val distance = (offset - state.position).getDistance()
                            if (distance <= state.node.radius) {
                                onBubbleClick(state.node)
                                return@detectTapGestures
                            }
                        }
                    },
                    onLongPress = { offset ->
                        // Find long-pressed bubble
                        if (onBubbleLongClick != null) {
                            bubbleStates.forEachIndexed { _, state ->
                                val distance = (offset - state.position).getDistance()
                                if (distance <= state.node.radius) {
                                    onBubbleLongClick(state.node)
                                    return@detectTapGestures
                                }
                            }
                        }
                    }
                )
            }
            .pointerInput(nodes) {
                detectDragGestures(
                    onDragStart = { offset ->
                        // Find bubble under finger
                        bubbleStates.forEachIndexed { index, state ->
                            if (state.node.isCenter) return@forEachIndexed // Can't drag center
                            val distance = (offset - state.position).getDistance()
                            if (distance <= state.node.radius) {
                                draggedBubbleIndex = index
                                state.startDrag()
                                return@forEachIndexed
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        if (draggedBubbleIndex >= 0) {
                            change.consume()
                            val state = bubbleStates[draggedBubbleIndex]
                            state.setPosition(state.position + dragAmount)
                        }
                    },
                    onDragEnd = {
                        if (draggedBubbleIndex >= 0) {
                            val state = bubbleStates[draggedBubbleIndex]
                            val engine = physicsEngine
                            if (engine != null) {
                                // Calculate target position (will be updated by physics)
                                val initialPositions = engine.calculateInitialPositions(nodes)
                                val targetPos = initialPositions.getOrNull(draggedBubbleIndex)
                                    ?: state.position
                                
                                scope.launch {
                                    state.endDrag(scope, targetPos)
                                }
                            }
                            draggedBubbleIndex = -1
                        }
                    },
                    onDragCancel = {
                        if (draggedBubbleIndex >= 0) {
                            bubbleStates[draggedBubbleIndex].let { bubbleState ->
                                scope.launch {
                                    bubbleState.endDrag(scope, bubbleState.position)
                                }
                            }
                            draggedBubbleIndex = -1
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Initialize physics engine on first draw
            if (physicsEngine == null && size.width > 0 && size.height > 0) {
                physicsEngine = BubblePhysicsEngine(
                    centerX = size.width / 2,
                    centerY = size.height / 2,
                    boundaryWidth = size.width,
                    boundaryHeight = size.height
                )
                
                val initialPositions = physicsEngine!!.calculateInitialPositions(nodes)
                bubbleStates.clear()
                nodes.forEachIndexed { index, node ->
                    bubbleStates.add(BubbleState(initialPositions[index], node))
                }
            }
            
            // Draw connections from center to children
            val centerBubble = bubbleStates.find { it.node.isCenter }
            if (centerBubble != null) {
                bubbleStates.filter { !it.node.isCenter }.forEach { child ->
                    drawLine(
                        color = Color.White.copy(alpha = 0.2f),
                        start = centerBubble.position,
                        end = child.position,
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
            
            // Draw bubbles
            bubbleStates.forEach { state ->
                drawBubble(
                    state = state,
                    pulseScale = if (state.node.isCenter) pulseScale else 1f,
                    isDragging = bubbleStates.indexOf(state) == draggedBubbleIndex
                )
            }
        }
    }
    
    // Physics simulation loop - throttled to 60fps for battery efficiency
    LaunchedEffect(nodes) {
        val targetFrameTime = 16_666_666L // ~60fps in nanoseconds
        lastFrameTime = System.nanoTime()
        
        while (isActive) {
            withFrameNanos { frameTime ->
                val elapsed = frameTime - lastFrameTime
                
                // Only update if enough time has passed (throttle to 60fps)
                if (elapsed >= targetFrameTime) {
                    val deltaTime = elapsed / 1_000_000_000f
                    lastFrameTime = frameTime
                    
                    // Only run physics if there are bubbles and engine is initialized
                    if (bubbleStates.isNotEmpty() && physicsEngine != null) {
                        physicsEngine?.step(bubbleStates, deltaTime)
                    }
                }
            }
        }
    }
}

/**
 * Draw a single bubble with its label and photo count.
 */
private fun DrawScope.drawBubble(
    state: BubbleState,
    pulseScale: Float = 1f,
    isDragging: Boolean = false
) {
    val node = state.node
    val position = state.position
    val radius = node.radius * pulseScale
    val color = Color(node.color)
    
    // Shadow
    drawCircle(
        color = Color.Black.copy(alpha = 0.3f),
        radius = radius + 4.dp.toPx(),
        center = position + Offset(2.dp.toPx(), 4.dp.toPx())
    )
    
    // Main bubble
    drawCircle(
        color = color,
        radius = radius,
        center = position
    )
    
    // Highlight gradient (top-left shine)
    drawCircle(
        color = Color.White.copy(alpha = 0.3f),
        radius = radius * 0.7f,
        center = position + Offset(-radius * 0.2f, -radius * 0.2f)
    )
    
    // Border (thicker when dragging)
    drawCircle(
        color = if (isDragging) Color.White else Color.White.copy(alpha = 0.5f),
        radius = radius,
        center = position,
        style = Stroke(width = if (isDragging) 4.dp.toPx() else 2.dp.toPx())
    )
    
    // Draw children indicator (small arrow) for bubbles with children
    if (node.hasChildren && !node.isCenter) {
        val indicatorSize = radius * 0.25f
        val indicatorX = position.x + radius * 0.6f
        val indicatorY = position.y - radius * 0.6f
        
        // Small circle background
        drawCircle(
            color = Color.White,
            radius = indicatorSize,
            center = Offset(indicatorX, indicatorY)
        )
        
        // Arrow symbol using path
        val arrowPath = Path().apply {
            moveTo(indicatorX - indicatorSize * 0.3f, indicatorY - indicatorSize * 0.3f)
            lineTo(indicatorX + indicatorSize * 0.4f, indicatorY)
            lineTo(indicatorX - indicatorSize * 0.3f, indicatorY + indicatorSize * 0.3f)
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
            textSize = (radius * 0.35f).coerceIn(24f, 48f)
            isFakeBoldText = true
            isAntiAlias = true
            setShadowLayer(4f, 0f, 2f, android.graphics.Color.argb(100, 0, 0, 0))
        }
        
        // Label
        drawText(
            node.label,
            position.x,
            position.y + textPaint.textSize * 0.1f,
            textPaint
        )
        
        // Photo count (smaller, below label)
        if (node.photoCount > 0) {
            val countPaint = android.graphics.Paint(textPaint).apply {
                textSize = textPaint.textSize * 0.6f
                alpha = 200
            }
            drawText(
                "${node.photoCount} 张",
                position.x,
                position.y + textPaint.textSize * 0.8f,
                countPaint
            )
        }
    }
}

/**
 * Extension to calculate distance of an Offset from origin.
 */
private fun Offset.getDistance(): Float = sqrt(x * x + y * y)
