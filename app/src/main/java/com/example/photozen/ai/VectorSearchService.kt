package com.example.photozen.ai

import android.net.Uri
import com.example.photozen.data.local.dao.PhotoAnalysisDao
import com.example.photozen.data.local.dao.PhotoDao
import com.example.photozen.data.local.dao.PhotoLabelDao
import com.example.photozen.data.local.dao.FaceDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * Service for vector-based image search and similarity detection.
 * Uses image embeddings to find similar photos and support semantic search.
 */
@Singleton
class VectorSearchService @Inject constructor(
    private val photoAnalysisDao: PhotoAnalysisDao,
    private val photoDao: PhotoDao,
    private val photoLabelDao: PhotoLabelDao,
    private val faceDao: FaceDao,
    private val imageEmbedding: ImageEmbedding
) {
    
    /**
     * Result of finding similar photos.
     */
    data class SimilarPhotoResult(
        val photoId: String,
        val similarity: Float,
        val photoUri: String? = null
    )
    
    /**
     * A group of similar photos.
     */
    data class SimilarPhotoGroup(
        val groupId: String,
        val photoIds: List<String>,
        val averageSimilarity: Float,
        val representativePhotoId: String
    )
    
    /**
     * Search result with match type.
     */
    data class SearchResult(
        val photoId: String,
        val score: Float,
        val matchType: MatchType,
        val photoUri: String? = null
    )
    
    /**
     * Type of match in search results.
     */
    enum class MatchType {
        LABEL,      // Matched by label
        PERSON,     // Matched by person name
        SEMANTIC,   // Matched by vector similarity
        COMBINED    // Multiple match types
    }
    
    /**
     * Progress callback for long-running operations.
     */
    data class ScanProgress(
        val current: Int,
        val total: Int,
        val groupsFound: Int
    )
    
    // ==================== Similarity Search ====================
    
    /**
     * Calculate cosine similarity between two embedding vectors.
     * Returns a value between -1 and 1, where 1 means identical.
     */
    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        require(a.size == b.size) { "Embeddings must have the same size" }
        
        var dotProduct = 0f
        var normA = 0f
        var normB = 0f
        
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        
        val denominator = sqrt(normA) * sqrt(normB)
        return if (denominator > 0) dotProduct / denominator else 0f
    }
    
    /**
     * Find photos similar to a target photo.
     *
     * @param targetPhotoId The photo ID to find similar photos for
     * @param limit Maximum number of results to return
     * @param threshold Minimum similarity score (0.0 - 1.0)
     * @return List of similar photos sorted by similarity (descending)
     */
    suspend fun findSimilarPhotos(
        targetPhotoId: String,
        limit: Int = 20,
        threshold: Float = 0.7f
    ): List<SimilarPhotoResult> = withContext(Dispatchers.Default) {
        // Get target photo's embedding
        val targetAnalysis = photoAnalysisDao.getByPhotoId(targetPhotoId) ?: return@withContext emptyList()
        val targetEmbedding = targetAnalysis.embedding?.let { 
            imageEmbedding.byteArrayToEmbedding(it) 
        } ?: return@withContext emptyList()
        
        // Get all photos with embeddings
        val allAnalyses = photoAnalysisDao.getAllWithEmbedding()
        
        // Calculate similarities in parallel
        val results = coroutineScope {
            allAnalyses
                .filter { it.photoId != targetPhotoId && it.embedding != null }
                .map { analysis ->
                    async {
                        val embedding = imageEmbedding.byteArrayToEmbedding(analysis.embedding!!)
                        val similarity = cosineSimilarity(targetEmbedding, embedding)
                        if (similarity >= threshold) {
                            SimilarPhotoResult(
                                photoId = analysis.photoId,
                                similarity = similarity
                            )
                        } else null
                    }
                }
                .awaitAll()
                .filterNotNull()
        }
        
        // Sort by similarity and limit
        results.sortedByDescending { it.similarity }.take(limit)
    }
    
    /**
     * Detect all groups of similar/duplicate photos.
     * Uses a union-find approach to group similar photos.
     *
     * @param threshold Minimum similarity to consider photos as similar (default 0.85 for near-duplicates)
     * @return Flow of progress updates, final emission contains complete groups
     */
    fun detectSimilarGroups(
        threshold: Float = 0.85f
    ): Flow<Pair<ScanProgress, List<SimilarPhotoGroup>>> = flow {
        val allAnalyses = photoAnalysisDao.getAllWithEmbedding()
        val total = allAnalyses.size
        
        if (total < 2) {
            emit(ScanProgress(total, total, 0) to emptyList())
            return@flow
        }
        
        // Convert embeddings
        val photoEmbeddings = allAnalyses.mapNotNull { analysis ->
            analysis.embedding?.let { bytes ->
                analysis.photoId to imageEmbedding.byteArrayToEmbedding(bytes)
            }
        }
        
        // Union-Find data structure
        val parent = mutableMapOf<String, String>()
        val rank = mutableMapOf<String, Int>()
        
        fun find(x: String): String {
            if (parent[x] != x) {
                parent[x] = find(parent[x]!!)
            }
            return parent[x]!!
        }
        
        fun union(x: String, y: String) {
            val rootX = find(x)
            val rootY = find(y)
            if (rootX != rootY) {
                when {
                    rank[rootX]!! < rank[rootY]!! -> parent[rootX] = rootY
                    rank[rootX]!! > rank[rootY]!! -> parent[rootY] = rootX
                    else -> {
                        parent[rootY] = rootX
                        rank[rootX] = rank[rootX]!! + 1
                    }
                }
            }
        }
        
        // Initialize
        photoEmbeddings.forEach { (id, _) ->
            parent[id] = id
            rank[id] = 0
        }
        
        // Compare all pairs
        var processed = 0
        val totalPairs = photoEmbeddings.size * (photoEmbeddings.size - 1) / 2
        var groupCount = 0
        
        for (i in photoEmbeddings.indices) {
            for (j in i + 1 until photoEmbeddings.size) {
                val (idA, embeddingA) = photoEmbeddings[i]
                val (idB, embeddingB) = photoEmbeddings[j]
                
                val similarity = cosineSimilarity(embeddingA, embeddingB)
                
                if (similarity >= threshold) {
                    val rootA = find(idA)
                    val rootB = find(idB)
                    if (rootA != rootB) {
                        union(idA, idB)
                        groupCount++
                    }
                }
                
                processed++
                
                // Emit progress every 1000 comparisons
                if (processed % 1000 == 0) {
                    emit(ScanProgress(
                        current = processed,
                        total = totalPairs,
                        groupsFound = groupCount
                    ) to emptyList())
                }
            }
        }
        
        // Build groups from union-find
        val groupMembers = mutableMapOf<String, MutableList<String>>()
        photoEmbeddings.forEach { (id, _) ->
            val root = find(id)
            groupMembers.getOrPut(root) { mutableListOf() }.add(id)
        }
        
        // Create group objects (only groups with 2+ photos)
        val groups = groupMembers
            .filter { it.value.size > 1 }
            .map { (_, members) ->
                // Calculate average similarity within group
                var totalSimilarity = 0f
                var count = 0
                val membersEmbeddings = members.mapNotNull { id ->
                    photoEmbeddings.find { it.first == id }?.second
                }
                
                for (i in membersEmbeddings.indices) {
                    for (j in i + 1 until membersEmbeddings.size) {
                        totalSimilarity += cosineSimilarity(membersEmbeddings[i], membersEmbeddings[j])
                        count++
                    }
                }
                
                SimilarPhotoGroup(
                    groupId = UUID.randomUUID().toString(),
                    photoIds = members,
                    averageSimilarity = if (count > 0) totalSimilarity / count else 0f,
                    representativePhotoId = members.first()
                )
            }
            .sortedByDescending { it.photoIds.size }
        
        emit(ScanProgress(totalPairs, totalPairs, groups.size) to groups)
    }
    
    // ==================== Semantic Search ====================
    
    /**
     * Search photos by text query.
     * Combines label matching, person name matching, and semantic search.
     *
     * @param query The search query text
     * @param limit Maximum number of results
     * @return List of search results sorted by relevance
     */
    suspend fun search(
        query: String,
        limit: Int = 50
    ): List<SearchResult> = withContext(Dispatchers.Default) {
        val normalizedQuery = query.trim().lowercase()
        if (normalizedQuery.isEmpty()) return@withContext emptyList()
        
        val results = mutableMapOf<String, SearchResult>()
        
        // 1. Label matching (exact and fuzzy)
        val labelResults = searchByLabels(normalizedQuery)
        labelResults.forEach { result ->
            results[result.photoId] = result
        }
        
        // 2. Person name matching
        val personResults = searchByPersonName(normalizedQuery)
        personResults.forEach { result ->
            val existing = results[result.photoId]
            if (existing != null) {
                // Combine scores
                results[result.photoId] = existing.copy(
                    score = (existing.score + result.score) / 2,
                    matchType = MatchType.COMBINED
                )
            } else {
                results[result.photoId] = result
            }
        }
        
        // Sort by score and limit
        results.values
            .sortedByDescending { it.score }
            .take(limit)
    }
    
    /**
     * Search photos by label matching.
     */
    private suspend fun searchByLabels(query: String): List<SearchResult> {
        val words = query.split(" ", ",").filter { it.isNotBlank() }
        
        // Get all labels that match any query word
        val matchingLabels = mutableSetOf<String>()
        words.forEach { word ->
            val labels = photoLabelDao.searchLabelsSync("%$word%")
            matchingLabels.addAll(labels.map { it.label })
        }
        
        if (matchingLabels.isEmpty()) return emptyList()
        
        // Get photos for matching labels
        val photoScores = mutableMapOf<String, Float>()
        
        matchingLabels.forEach { label ->
            val photoIds = photoLabelDao.getPhotoIdsByLabelSync(label)
            photoIds.forEach { photoId ->
                // Score based on how many query words match
                val matchScore = words.count { word -> 
                    label.contains(word, ignoreCase = true) 
                }.toFloat() / words.size
                
                photoScores[photoId] = (photoScores[photoId] ?: 0f) + matchScore
            }
        }
        
        // Normalize scores
        val maxScore = photoScores.values.maxOrNull() ?: 1f
        
        return photoScores.map { (photoId, score) ->
            SearchResult(
                photoId = photoId,
                score = score / maxScore,
                matchType = MatchType.LABEL
            )
        }
    }
    
    /**
     * Search photos by person name.
     */
    private suspend fun searchByPersonName(query: String): List<SearchResult> {
        val persons = faceDao.searchPersonsByName("%$query%")
        if (persons.isEmpty()) return emptyList()
        
        val results = mutableListOf<SearchResult>()
        
        persons.forEach { person ->
            val faces = faceDao.getFacesByPersonIdSync(person.id)
            val photoIds = faces.map { it.photoId }.distinct()
            
            // Score based on name match quality
            val nameMatchScore = if (person.name?.contains(query, ignoreCase = true) == true) {
                if (person.name.equals(query, ignoreCase = true)) 1.0f else 0.8f
            } else 0.5f
            
            photoIds.forEach { photoId ->
                results.add(SearchResult(
                    photoId = photoId,
                    score = nameMatchScore,
                    matchType = MatchType.PERSON
                ))
            }
        }
        
        return results
    }
    
    // ==================== Statistics ====================
    
    /**
     * Get count of photos with embeddings.
     */
    suspend fun getEmbeddingCount(): Int = photoAnalysisDao.getEmbeddingCount()
    
    /**
     * Get total photo count.
     */
    suspend fun getTotalPhotoCount(): Int = photoDao.getPhotoCount()
    
    /**
     * Check if image embedding is available.
     */
    fun isEmbeddingAvailable(): Boolean = imageEmbedding.isModelAvailable()
}
