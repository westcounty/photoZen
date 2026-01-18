package com.example.photozen.util

import android.content.Intent
import android.net.Uri
import android.os.Build

/**
 * Share mode enum - determines what action to take with shared photos.
 */
enum class ShareMode {
    COPY,    // Copy photos to album
    COMPARE  // Compare photos in light table
}

/**
 * Data class holding parsed share intent data.
 */
data class ShareData(
    val uris: List<Uri>,
    val mode: ShareMode
)

/**
 * Utility object for parsing share intents from external apps.
 * 
 * Supports two share modes:
 * - COPY: Copy shared photos to a selected album (single or multiple)
 * - COMPARE: Compare 2-6 photos in the light table (multiple only)
 */
object ShareHandler {
    
    // Activity alias class names for determining share mode
    private const val COPY_ALIAS = "com.example.photozen.ShareCopyAlias"
    private const val COMPARE_ALIAS = "com.example.photozen.ShareCompareAlias"
    
    /**
     * Parse a share intent and extract URIs and mode.
     * 
     * @param intent The incoming intent to parse
     * @return ShareData if this is a valid share intent, null otherwise
     */
    fun parseShareIntent(intent: Intent): ShareData? {
        val action = intent.action
        if (action != Intent.ACTION_SEND && action != Intent.ACTION_SEND_MULTIPLE) {
            return null
        }
        
        // Determine mode based on which activity alias was used
        val mode = when (intent.component?.className) {
            COPY_ALIAS -> ShareMode.COPY
            COMPARE_ALIAS -> ShareMode.COMPARE
            else -> ShareMode.COPY // Default to copy mode
        }
        
        val uris = when (action) {
            Intent.ACTION_SEND -> {
                getParcelableExtra<Uri>(intent, Intent.EXTRA_STREAM)?.let { listOf(it) }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                getUriArrayListExtra(intent, Intent.EXTRA_STREAM)?.toList()
            }
            else -> null
        }
        
        return uris?.takeIf { it.isNotEmpty() }?.let { ShareData(it, mode) }
    }
    
    /**
     * Get parcelable extra with backward compatibility.
     */
    @Suppress("DEPRECATION")
    private inline fun <reified T> getParcelableExtra(intent: Intent, name: String): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(name, T::class.java)
        } else {
            intent.getParcelableExtra(name)
        }
    }
    
    /**
     * Get parcelable array list extra with backward compatibility.
     */
    @Suppress("DEPRECATION", "UNCHECKED_CAST")
    private fun getUriArrayListExtra(intent: Intent, name: String): ArrayList<Uri>? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(name, Uri::class.java)
        } else {
            intent.getParcelableArrayListExtra<Uri>(name)
        }
    }
}
