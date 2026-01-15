package com.example.photozen.ui.components.bubble

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Represents a bubble node in the physics simulation.
 * 
 * @param id Unique identifier for the bubble
 * @param label Display text for the bubble
 * @param radius Size of the bubble
 * @param color Color of the bubble (ARGB Int)
 * @param photoCount Number of photos with this tag
 * @param isCenter Whether this is the center/parent node
 * @param hasChildren Whether this tag has child tags (for navigation)
 * @param linkedAlbumId MediaStore bucket ID if linked to an album
 * @param linkedAlbumName Name of the linked album
 */
@Stable
data class BubbleNode(
    val id: String,
    val label: String,
    val radius: Float,
    val color: Int,
    val photoCount: Int = 0,
    val isCenter: Boolean = false,
    val hasChildren: Boolean = false,
    val linkedAlbumId: String? = null,
    val linkedAlbumName: String? = null
) {
    val isLinkedToAlbum: Boolean
        get() = linkedAlbumId != null
}

/**
 * Mutable state for a bubble's position and velocity.
 */
class BubbleState(
    initialPosition: Offset,
    val node: BubbleNode
) {
    var position: Offset = initialPosition
        private set
    
    var velocity: Offset = Offset.Zero
        private set
    
    var isDragging: Boolean = false
        private set
    
    // Target position for physics simulation
    var targetPosition: Offset = initialPosition
        private set
    
    // Animatable for smooth spring-back animation
    private val animatablePosition = Animatable(initialPosition, Offset.VectorConverter)
    
    /**
     * Update position directly (during dragging or physics push).
     */
    fun setPosition(newPosition: Offset) {
        position = newPosition
    }
    
    /**
     * Set target position for dragged bubble to stay at.
     */
    fun setTargetPosition(target: Offset) {
        targetPosition = target
    }
    
    /**
     * Apply velocity for physics simulation.
     */
    fun applyVelocity(newVelocity: Offset) {
        velocity = newVelocity
    }
    
    /**
     * Start dragging this bubble.
     */
    fun startDrag() {
        isDragging = true
        velocity = Offset.Zero
        targetPosition = position
    }
    
    /**
     * End dragging - bubble stays at current position.
     */
    fun endDrag() {
        isDragging = false
        velocity = Offset.Zero
        targetPosition = position
    }
    
    /**
     * Animate to a specific position smoothly.
     */
    suspend fun animateTo(scope: CoroutineScope, targetPos: Offset) {
        scope.launch {
            animatablePosition.snapTo(position)
            animatablePosition.animateTo(
                targetPos,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) {
                position = value
            }
            targetPosition = targetPos
        }
    }
    
    /**
     * Update position from physics step.
     */
    fun physicsStep(deltaTime: Float, damping: Float = 0.92f) {
        if (!isDragging) {
            position += velocity * deltaTime
            velocity *= damping
        }
    }
}

/**
 * Physics engine for bubble layout simulation.
 * 
 * Implements smooth dragging with collision avoidance:
 * - Bubbles can be freely dragged within boundaries
 * - Dragged bubbles push other bubbles away smoothly
 * - Non-dragged bubbles maintain their positions unless pushed
 */
class BubblePhysicsEngine(
    private val centerX: Float,
    private val centerY: Float,
    private val boundaryWidth: Float,
    private val boundaryHeight: Float
) {
    companion object {
        // Physics constants
        const val PUSH_STRENGTH = 1200f           // Strength of push force when bubbles collide
        const val PUSH_VELOCITY_DAMPING = 0.85f   // How quickly pushed bubbles slow down
        const val MIN_GAP = 8f                    // Minimum gap between bubbles
        const val MAX_VELOCITY = 500f             // Maximum velocity cap
        const val SETTLE_THRESHOLD = 0.5f         // Velocity below which bubble is considered settled
    }
    
    /**
     * Perform one physics simulation step.
     * Handles collision detection and smooth pushing of bubbles.
     * 
     * @param bubbles List of bubble states to simulate
     * @param deltaTime Time step in seconds
     */
    fun step(bubbles: List<BubbleState>, deltaTime: Float) {
        val dt = min(deltaTime, 0.05f) // Cap delta time to prevent explosions
        
        // Find the dragged bubble (if any)
        val draggedBubble = bubbles.find { it.isDragging }
        
        // Calculate push forces for each non-dragged bubble
        bubbles.forEach { bubble ->
            if (bubble.isDragging || bubble.node.isCenter) return@forEach
            
            var pushForce = Offset.Zero
            
            // Check collision with all other bubbles
            bubbles.forEach { other ->
                if (other.node.id == bubble.node.id) return@forEach
                
                val diff = bubble.position - other.position
                val distance = diff.getDistance()
                val minDist = bubble.node.radius + other.node.radius + MIN_GAP
                
                if (distance < minDist && distance > 0.1f) {
                    val overlap = minDist - distance
                    val direction = diff / distance
                    
                    // Stronger push from dragged bubble
                    val strength = if (other.isDragging) PUSH_STRENGTH * 1.5f else PUSH_STRENGTH
                    pushForce += direction * (overlap * strength * dt)
                }
            }
            
            // Apply push force directly to position for immediate response
            if (pushForce.getDistance() > 0.1f) {
                val newVelocity = bubble.velocity + pushForce
                val speed = newVelocity.getDistance()
                val cappedVelocity = if (speed > MAX_VELOCITY) {
                    newVelocity * (MAX_VELOCITY / speed)
                } else {
                    newVelocity
                }
                bubble.applyVelocity(cappedVelocity)
            }
        }
        
        // Update positions for non-dragged bubbles
        bubbles.forEach { bubble ->
            if (!bubble.isDragging && !bubble.node.isCenter) {
                bubble.physicsStep(dt, PUSH_VELOCITY_DAMPING)
                
                // Clamp to boundaries
                val margin = bubble.node.radius + 4f
                val clampedX = bubble.position.x.coerceIn(margin, boundaryWidth - margin)
                val clampedY = bubble.position.y.coerceIn(margin, boundaryHeight - margin)
                
                // If hit boundary, zero out velocity in that direction
                if (clampedX != bubble.position.x) {
                    bubble.applyVelocity(Offset(0f, bubble.velocity.y * 0.5f))
                }
                if (clampedY != bubble.position.y) {
                    bubble.applyVelocity(Offset(bubble.velocity.x * 0.5f, 0f))
                }
                
                bubble.setPosition(Offset(clampedX, clampedY))
                
                // Settle velocity if very slow
                if (bubble.velocity.getDistance() < SETTLE_THRESHOLD) {
                    bubble.applyVelocity(Offset.Zero)
                }
            }
        }
        
        // Also clamp dragged bubble to boundaries
        draggedBubble?.let { bubble ->
            val margin = bubble.node.radius + 4f
            val clampedX = bubble.position.x.coerceIn(margin, boundaryWidth - margin)
            val clampedY = bubble.position.y.coerceIn(margin, boundaryHeight - margin)
            if (clampedX != bubble.position.x || clampedY != bubble.position.y) {
                bubble.setPosition(Offset(clampedX, clampedY))
            }
        }
    }
    
    /**
     * Calculate initial positions for bubbles in a circular arrangement.
     */
    fun calculateInitialPositions(nodes: List<BubbleNode>): List<Offset> {
        val childNodes = nodes.filter { !it.isCenter }
        
        val positions = mutableListOf<Offset>()
        
        nodes.forEach { node ->
            if (node.isCenter) {
                positions.add(Offset(centerX, centerY))
            } else {
                val index = childNodes.indexOf(node)
                val total = childNodes.size
                val angle = (2 * Math.PI * index / total).toFloat() - (Math.PI / 2).toFloat()
                val orbitRadius = min(boundaryWidth, boundaryHeight) * 0.3f
                
                val x = centerX + orbitRadius * kotlin.math.cos(angle)
                val y = centerY + orbitRadius * kotlin.math.sin(angle)
                
                positions.add(Offset(x, y))
            }
        }
        
        return positions
    }
    
    /**
     * Get positions as a map of tagId -> Pair(x, y).
     */
    fun getPositionsMap(bubbles: List<BubbleState>): Map<String, Pair<Float, Float>> {
        return bubbles.associate { it.node.id to Pair(it.position.x, it.position.y) }
    }
}

/**
 * Extension to calculate distance of an Offset from origin.
 */
private fun Offset.getDistance(): Float = sqrt(x * x + y * y)
