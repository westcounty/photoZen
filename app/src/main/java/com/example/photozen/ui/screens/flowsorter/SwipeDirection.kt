package com.example.photozen.ui.screens.flowsorter

/**
 * Represents the direction of a swipe gesture in Flow Sorter.
 * 
 * Gesture mapping:
 * - LEFT/RIGHT → Keep (preserve the photo)
 * - UP → Trash (delete the photo)
 * - DOWN → Maybe (review later, with sinking animation)
 */
enum class SwipeDirection {
    /**
     * Swipe left - Keep the photo
     */
    LEFT,
    
    /**
     * Swipe right - Keep the photo
     */
    RIGHT,
    
    /**
     * Swipe up - Mark for Trash/Delete
     */
    UP,
    
    /**
     * Swipe down - Mark as Maybe for later review (sinks into pool)
     */
    DOWN,
    
    /**
     * No swipe / neutral position
     */
    NONE
}
