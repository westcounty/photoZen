package com.example.photozen.data.model

/**
 * Sort order for photos in Flow Sorter.
 * Used for database-level sorting before pagination.
 */
enum class PhotoSortOrder(val displayName: String) {
    DATE_DESC("时间倒序"),  // Newest first (default)
    DATE_ASC("时间正序"),   // Oldest first
    SIZE_DESC("大小倒序"),  // Largest first
    SIZE_ASC("大小正序"),   // Smallest first
    RANDOM("随机排序")      // Random shuffle using seeded pseudo-random
}
