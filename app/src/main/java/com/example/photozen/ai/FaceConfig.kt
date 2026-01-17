package com.example.photozen.ai

/**
 * Configuration for face recognition parameters.
 * Inspired by PhotoPrism's face recognition settings.
 * 
 * Reference: https://docs.photoprism.app/user-guide/ai/face-recognition/
 */
data class FaceConfig(
    /**
     * Minimum face size in pixels.
     * Faces smaller than this will be ignored.
     * PhotoPrism default: 50
     */
    val minFaceSize: Int = 50,
    
    /**
     * Minimum face quality/confidence score (0.0-1.0).
     * Faces with lower scores will be ignored.
     * Higher values = stricter filtering = fewer false positives.
     * PhotoPrism default: ~0.9 (scaled to our 0-1 range)
     */
    val minFaceScore: Float = 0.7f,
    
    /**
     * Distance threshold for face clustering (DBSCAN eps parameter).
     * Faces within this distance are considered the same person.
     * Lower values = stricter clustering = more separate persons.
     * PhotoPrism default: 0.64
     */
    val clusterDistance: Float = 0.64f,
    
    /**
     * Distance threshold for face matching.
     * Used when matching a new face to existing persons.
     * Lower values = stricter matching = fewer false matches.
     * PhotoPrism default: 0.46
     */
    val matchDistance: Float = 0.46f,
    
    /**
     * Overlap threshold for detecting duplicate face detections.
     * Detections with IoU > this value are considered duplicates.
     * PhotoPrism default: 0.42
     */
    val overlapThreshold: Float = 0.42f,
    
    /**
     * Minimum number of faces required to form a cluster.
     * DBSCAN minPts parameter.
     */
    val minClusterSize: Int = 2,
    
    /**
     * Maximum number of faces to process per photo.
     * Limits processing time for group photos.
     */
    val maxFacesPerPhoto: Int = 20,
    
    /**
     * Whether to use fast detection mode.
     * Fast mode is quicker but may miss some faces.
     * Similar to PhotoPrism's Pigo vs SCRFD choice.
     */
    val useFastDetection: Boolean = false,
    
    /**
     * Thumbnail size for face detection input.
     * PhotoPrism uses 720px for SCRFD detection.
     */
    val detectionInputSize: Int = 720
) {
    companion object {
        /**
         * Default configuration based on PhotoPrism's defaults.
         */
        val DEFAULT = FaceConfig()
        
        /**
         * High accuracy configuration.
         * More strict thresholds, fewer false positives.
         */
        val HIGH_ACCURACY = FaceConfig(
            minFaceSize = 60,
            minFaceScore = 0.8f,
            clusterDistance = 0.55f,
            matchDistance = 0.40f,
            useFastDetection = false
        )
        
        /**
         * Fast configuration.
         * More lenient thresholds, faster processing.
         */
        val FAST = FaceConfig(
            minFaceSize = 40,
            minFaceScore = 0.6f,
            clusterDistance = 0.70f,
            matchDistance = 0.52f,
            useFastDetection = true,
            detectionInputSize = 480
        )
    }
    
    /**
     * Convert cosine distance to similarity score.
     * Similarity = 1 - distance
     */
    fun distanceToSimilarity(distance: Float): Float = 1f - distance
    
    /**
     * Check if a distance indicates a match.
     */
    fun isMatch(distance: Float): Boolean = distance <= matchDistance
    
    /**
     * Check if a distance indicates same cluster.
     */
    fun isSameCluster(distance: Float): Boolean = distance <= clusterDistance
}
