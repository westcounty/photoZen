package com.example.photozen.ai

import android.net.Uri
import com.example.photozen.data.local.dao.PhotoAnalysisDao
import com.example.photozen.data.local.dao.PhotoDao
import com.example.photozen.data.local.entity.PhotoAnalysisEntity
import com.example.photozen.data.local.entity.PhotoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Duplicate photo detector using perceptual hashing.
 * 
 * Inspired by PhotoPrism's duplicate detection system.
 * Uses pHash (perceptual hash) to find visually similar images
 * regardless of resolution, compression, or minor edits.
 * 
 * Reference: https://docs.photoprism.app/developer-guide/media-files/fingerprints/
 */
@Singleton
class DuplicateDetector @Inject constructor(
    private val photoAnalysisDao: PhotoAnalysisDao,
    private val photoDao: PhotoDao,
    private val photoHasher: PhotoHasher
) {
    companion object {
        // Hamming distance thresholds
        const val THRESHOLD_EXACT = 5    // Exact duplicates (same image, different quality)
        const val THRESHOLD_SIMILAR = 10  // Very similar (cropped, filtered)
        const val THRESHOLD_RELATED = 15  // Related (same subject, different angle)
        
        // Batch size for processing
        private const val BATCH_SIZE = 100
    }
    
    /**
     * Scan all photos for duplicates.
     * Returns groups of duplicate photos.
     */
    suspend fun findAllDuplicates(
        threshold: Int = THRESHOLD_SIMILAR
    ): List<DuplicateGroup> = withContext(Dispatchers.IO) {
        val analysesWithHash = photoAnalysisDao.getPhotosWithPhashSync()
        findDuplicatesInList(analysesWithHash, threshold)
    }
    
    /**
     * Find duplicates for a specific photo.
     */
    suspend fun findDuplicatesFor(
        photoId: String,
        threshold: Int = THRESHOLD_SIMILAR
    ): List<PhotoWithAnalysis> = withContext(Dispatchers.IO) {
        val analysis = photoAnalysisDao.getByPhotoId(photoId) ?: return@withContext emptyList()
        val phash = analysis.phash ?: return@withContext emptyList()
        
        val candidates = photoAnalysisDao.getPhotosWithPhashSync()
        candidates.filter { candidate ->
            candidate.photoId != photoId &&
            candidate.phash != null &&
            photoHasher.hammingDistance(phash, candidate.phash!!) <= threshold
        }.sortedBy { 
            photoHasher.hammingDistance(phash, it.phash!!) 
        }.mapNotNull { candidateAnalysis ->
            val photo = photoDao.getById(candidateAnalysis.photoId)
            if (photo != null) PhotoWithAnalysis(photo, candidateAnalysis) else null
        }
    }
    
    /**
     * Check if a photo is a duplicate of any existing photo.
     * Useful during import to avoid adding duplicates.
     */
    suspend fun isDuplicate(
        uri: Uri,
        threshold: Int = THRESHOLD_EXACT
    ): PhotoWithAnalysis? = withContext(Dispatchers.IO) {
        val phash = photoHasher.calculatePHash(uri) ?: return@withContext null
        
        val candidates = photoAnalysisDao.getPhotosWithPhashSync()
        val matchingAnalysis = candidates.find { candidate ->
            candidate.phash != null &&
            photoHasher.hammingDistance(phash, candidate.phash!!) <= threshold
        } ?: return@withContext null
        
        val photo = photoDao.getById(matchingAnalysis.photoId) ?: return@withContext null
        PhotoWithAnalysis(photo, matchingAnalysis)
    }
    
    /**
     * Scan and update hashes for photos that haven't been analyzed.
     * Returns Flow of progress updates.
     */
    fun scanPhotosForHashes(): Flow<HashScanProgress> = flow {
        val analysesToScan = photoAnalysisDao.getPhotosWithoutPhashSync(BATCH_SIZE)
        val total = analysesToScan.size
        
        emit(HashScanProgress(0, total, ScanState.STARTED))
        
        analysesToScan.forEachIndexed { index, analysis ->
            try {
                val photo = photoDao.getById(analysis.photoId)
                if (photo != null) {
                    val uri = Uri.parse(photo.systemUri)
                    val result = photoHasher.analyzePhoto(uri)
                    
                    if (result != null) {
                        photoAnalysisDao.updatePhotoAnalysis(
                            photoId = analysis.photoId,
                            phash = result.phash,
                            dominantColor = result.dominantColor,
                            accentColor = result.accentColor,
                            luminance = result.luminance,
                            chroma = result.chroma,
                            quality = result.quality,
                            sharpness = result.sharpness,
                            aspectRatio = result.aspectRatio
                        )
                    }
                }
                
                if ((index + 1) % 10 == 0 || index == total - 1) {
                    emit(HashScanProgress(index + 1, total, ScanState.IN_PROGRESS))
                }
            } catch (e: Exception) {
                android.util.Log.e("DuplicateDetector", "Error scanning photo ${analysis.photoId}: ${e.message}")
            }
        }
        
        emit(HashScanProgress(total, total, ScanState.COMPLETED))
    }
    
    /**
     * Find duplicate groups in a list of photo analyses.
     * Uses Union-Find algorithm for efficient grouping.
     */
    private suspend fun findDuplicatesInList(
        analyses: List<PhotoAnalysisEntity>,
        threshold: Int
    ): List<DuplicateGroup> {
        if (analyses.isEmpty()) return emptyList()
        
        // Union-Find data structure
        val parent = mutableMapOf<String, String>()
        val rank = mutableMapOf<String, Int>()
        
        fun find(id: String): String {
            if (parent[id] != id) {
                parent[id] = find(parent[id]!!)
            }
            return parent[id]!!
        }
        
        fun union(id1: String, id2: String) {
            val root1 = find(id1)
            val root2 = find(id2)
            
            if (root1 != root2) {
                val rank1 = rank[root1] ?: 0
                val rank2 = rank[root2] ?: 0
                
                if (rank1 < rank2) {
                    parent[root1] = root2
                } else if (rank1 > rank2) {
                    parent[root2] = root1
                } else {
                    parent[root2] = root1
                    rank[root1] = rank1 + 1
                }
            }
        }
        
        // Initialize Union-Find
        analyses.forEach { analysis ->
            parent[analysis.photoId] = analysis.photoId
            rank[analysis.photoId] = 0
        }
        
        // Compare all pairs (O(n²) - can be optimized with locality-sensitive hashing)
        for (i in analyses.indices) {
            val analysis1 = analyses[i]
            val hash1 = analysis1.phash ?: continue
            
            for (j in (i + 1) until analyses.size) {
                val analysis2 = analyses[j]
                val hash2 = analysis2.phash ?: continue
                
                val distance = photoHasher.hammingDistance(hash1, hash2)
                if (distance >= 0 && distance <= threshold) {
                    union(analysis1.photoId, analysis2.photoId)
                }
            }
        }
        
        // Group analyses by root
        val groups = mutableMapOf<String, MutableList<PhotoAnalysisEntity>>()
        analyses.forEach { analysis ->
            val root = find(analysis.photoId)
            groups.getOrPut(root) { mutableListOf() }.add(analysis)
        }
        
        // Return only groups with more than one photo
        return groups.values
            .filter { it.size > 1 }
            .mapNotNull { analysisGroup ->
                // Sort by quality (best first)
                val sorted = analysisGroup.sortedByDescending { it.quality }
                
                // Fetch actual PhotoEntity for each analysis
                val photosWithAnalysis = sorted.mapNotNull { analysis ->
                    val photo = photoDao.getById(analysis.photoId)
                    if (photo != null) PhotoWithAnalysis(photo, analysis) else null
                }
                
                if (photosWithAnalysis.size > 1) {
                    DuplicateGroup(
                        bestPhoto = photosWithAnalysis.first(),
                        duplicates = photosWithAnalysis.drop(1),
                        similarity = calculateGroupSimilarity(sorted)
                    )
                } else null
            }
            .sortedByDescending { it.duplicates.size }
    }
    
    /**
     * Calculate average similarity within a duplicate group.
     */
    private fun calculateGroupSimilarity(analyses: List<PhotoAnalysisEntity>): Float {
        if (analyses.size < 2) return 1f
        
        val bestHash = analyses.first().phash ?: return 0f
        var totalSimilarity = 0f
        var count = 0
        
        analyses.drop(1).forEach { analysis ->
            analysis.phash?.let { hash ->
                val distance = photoHasher.hammingDistance(bestHash, hash)
                if (distance >= 0) {
                    // Convert distance (0-64) to similarity (1.0-0.0)
                    totalSimilarity += 1f - (distance / 64f)
                    count++
                }
            }
        }
        
        return if (count > 0) totalSimilarity / count else 0f
    }
    
    /**
     * Get statistics about duplicate photos.
     */
    suspend fun getDuplicateStats(): DuplicateStats = withContext(Dispatchers.IO) {
        val groups = findAllDuplicates(THRESHOLD_SIMILAR)
        val totalDuplicates = groups.sumOf { it.duplicates.size }
        val totalSizeWasted = groups.sumOf { group ->
            group.duplicates.sumOf { it.photo.size }
        }
        
        DuplicateStats(
            totalGroups = groups.size,
            totalDuplicates = totalDuplicates,
            totalSizeWasted = totalSizeWasted,
            groups = groups
        )
    }
}

/**
 * Photo with its analysis data.
 */
data class PhotoWithAnalysis(
    val photo: PhotoEntity,
    val analysis: PhotoAnalysisEntity
) {
    val photoId: String get() = photo.id
    val phash: String? get() = analysis.phash
    val quality: Int get() = analysis.quality
}

/**
 * A group of duplicate photos.
 * @param bestPhoto The highest quality photo in the group (recommended to keep)
 * @param duplicates Other photos that are duplicates of the best photo
 * @param similarity Average similarity score (0.0-1.0)
 */
data class DuplicateGroup(
    val bestPhoto: PhotoWithAnalysis,
    val duplicates: List<PhotoWithAnalysis>,
    val similarity: Float
) {
    val totalCount: Int get() = duplicates.size + 1
    val wastedSize: Long get() = duplicates.sumOf { it.photo.size }
}

/**
 * Statistics about duplicate photos.
 */
data class DuplicateStats(
    val totalGroups: Int,
    val totalDuplicates: Int,
    val totalSizeWasted: Long,
    val groups: List<DuplicateGroup>
)

/**
 * Progress update for hash scanning.
 */
data class HashScanProgress(
    val processed: Int,
    val total: Int,
    val state: ScanState
) {
    val progress: Float get() = if (total > 0) processed.toFloat() / total else 0f
}

enum class ScanState {
    STARTED,
    IN_PROGRESS,
    COMPLETED
}
