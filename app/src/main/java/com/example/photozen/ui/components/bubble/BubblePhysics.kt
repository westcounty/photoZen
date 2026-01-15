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
    
    // Animatable for smooth spring-back animation
    private val animatablePosition = Animatable(initialPosition, Offset.VectorConverter)
    
    /**
     * Update position directly (during dragging).
     */
    fun setPosition(newPosition: Offset) {
        position = newPosition
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
    }
    
    /**
     * End dragging and animate back to equilibrium.
     */
    suspend fun endDrag(scope: CoroutineScope, targetPosition: Offset) {
        isDragging = false
        scope.launch {
            animatablePosition.snapTo(position)
            animatablePosition.animateTo(
                targetPosition,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) {
                position = value
            }
        }
    }
    
    /**
     * Update position from physics step.
     */
    fun physicsStep(deltaTime: Float, damping: Float = 0.95f) {
        if (!isDragging) {
            position += velocity * deltaTime
            velocity *= damping
        }
    }
}

/**
 * Physics engine for bubble layout simulation.
 * 
 * Implements simple spring-repulsion physics:
 * - Center attraction: All bubbles are pulled toward center
 * - Repulsion: Bubbles push each other to avoid overlap
 * - Damping: Velocity decays over time
 */
class BubblePhysicsEngine(
    private val centerX: Float,
    private val centerY: Float,
    private val boundaryWidth: Float,
    private val boundaryHeight: Float
) {
    companion object {
        // Physics constants
        const val CENTER_ATTRACTION = 0.02f      // Strength of center pull
        const val REPULSION_STRENGTH = 800f      // Strength of bubble-to-bubble repulsion
        const val BOUNDARY_REPULSION = 500f       // Strength of boundary repulsion
        const val MIN_DISTANCE = 10f              // Minimum distance between bubbles
        const val DAMPING = 0.92f                 // Velocity damping per frame
        const val MAX_VELOCITY = 300f             // Maximum velocity cap
    }
    
    /**
     * Perform one physics simulation step.
     * 
     * @param bubbles List of bubble states to simulate
     * @param deltaTime Time step in seconds
     */
    fun step(bubbles: List<BubbleState>, deltaTime: Float) {
        val dt = min(deltaTime, 0.05f) // Cap delta time to prevent explosions
        
        // Calculate forces for each bubble
        bubbles.forEach { bubble ->
            if (bubble.isDragging || bubble.node.isCenter) return@forEach
            
            var force = Offset.Zero
            
            // 1. Center attraction (spring force toward center)
            val toCenter = Offset(centerX, centerY) - bubble.position
            force += toCenter * CENTER_ATTRACTION
            
            // 2. Bubble-to-bubble repulsion
            bubbles.forEach { other ->
                if (other.node.id == bubble.node.id) return@forEach
                
                val diff = bubble.position - other.position
                val distance = diff.getDistance()
                val minDist = bubble.node.radius + other.node.radius + MIN_DISTANCE
                
                if (distance < minDist && distance > 0.1f) {
                    val overlap = minDist - distance
                    val direction = diff / distance
                    force += direction * (overlap * REPULSION_STRENGTH / distance)
                }
            }
            
            // 3. Boundary repulsion
            val margin = bubble.node.radius + 20f
            
            // Left boundary
            if (bubble.position.x < margin) {
                force += Offset(BOUNDARY_REPULSION / max(bubble.position.x, 1f), 0f)
            }
            // Right boundary
            if (bubble.position.x > boundaryWidth - margin) {
                force += Offset(-BOUNDARY_REPULSION / max(boundaryWidth - bubble.position.x, 1f), 0f)
            }
            // Top boundary
            if (bubble.position.y < margin) {
                force += Offset(0f, BOUNDARY_REPULSION / max(bubble.position.y, 1f))
            }
            // Bottom boundary
            if (bubble.position.y > boundaryHeight - margin) {
                force += Offset(0f, -BOUNDARY_REPULSION / max(boundaryHeight - bubble.position.y, 1f))
            }
            
            // Apply force to velocity
            val newVelocity = bubble.velocity + force * dt
            
            // Cap velocity
            val speed = newVelocity.getDistance()
            val cappedVelocity = if (speed > MAX_VELOCITY) {
                newVelocity * (MAX_VELOCITY / speed)
            } else {
                newVelocity
            }
            
            bubble.applyVelocity(cappedVelocity)
        }
        
        // Update positions
        bubbles.forEach { bubble ->
            if (!bubble.isDragging && !bubble.node.isCenter) {
                bubble.physicsStep(dt, DAMPING)
                
                // Clamp to boundaries
                val margin = bubble.node.radius
                bubble.setPosition(
                    Offset(
                        bubble.position.x.coerceIn(margin, boundaryWidth - margin),
                        bubble.position.y.coerceIn(margin, boundaryHeight - margin)
                    )
                )
            }
        }
    }
    
    /**
     * Calculate initial positions for bubbles in a circular arrangement.
     */
    fun calculateInitialPositions(nodes: List<BubbleNode>): List<Offset> {
        val centerNode = nodes.find { it.isCenter }
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
}

/**
 * Extension to calculate distance of an Offset from origin.
 */
private fun Offset.getDistance(): Float = sqrt(x * x + y * y)
