package com.example.photozen.data.model

/**
 * Enum representing the sorting status of a photo.
 * Used in Flow Sorter to categorize photos by swipe direction.
 */
enum class PhotoStatus {
    /**
     * Photo has not been sorted yet.
     * This is the initial state for all photos.
     */
    UNSORTED,
    
    /**
     * Photo marked to keep (swipe right).
     * User wants to preserve this photo.
     */
    KEEP,
    
    /**
     * Photo marked for trash (swipe left).
     * Non-destructive: only marks in DB, doesn't delete file.
     */
    TRASH,
    
    /**
     * Photo marked for later review (swipe up).
     * Will appear in Light Table for comparison.
     */
    MAYBE
}
