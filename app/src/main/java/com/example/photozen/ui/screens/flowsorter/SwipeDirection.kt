package com.example.photozen.ui.screens.flowsorter

/**
 * Represents the direction of a swipe gesture in Flow Sorter.
 */
enum class SwipeDirection {
    /**
     * Swipe left - Mark photo for Trash
     */
    LEFT,
    
    /**
     * Swipe right - Keep the photo
     */
    RIGHT,
    
    /**
     * Swipe up - Mark as Maybe for later review
     */
    UP,
    
    /**
     * No swipe / neutral position
     */
    NONE
}
