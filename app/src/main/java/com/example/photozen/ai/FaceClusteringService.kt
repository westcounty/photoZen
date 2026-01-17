package com.example.photozen.ai

import com.example.photozen.data.local.dao.FaceDao
import com.example.photozen.data.local.entity.FaceEntity
import com.example.photozen.data.local.entity.PersonEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of face clustering.
 */
data class ClusterResult(
    val personId: String,
    val faceIds: List<String>,
    val averageEmbedding: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ClusterResult
        if (personId != other.personId) return false
        if (faceIds != other.faceIds) return false
        if (!averageEmbedding.contentEquals(other.averageEmbedding)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = personId.hashCode()
        result = 31 * result + faceIds.hashCode()
        result = 31 * result + averageEmbedding.contentHashCode()
        return result
    }
}

/**
 * Progress callback for clustering operations.
 */
typealias ClusteringProgressCallback = (current: Int, total: Int, message: String) -> Unit

/**
 * Service for face clustering using DBSCAN algorithm.
 * Groups similar faces into persons based on their embedding vectors.
 */
@Singleton
class FaceClusteringService @Inject constructor(
    private val faceDao: FaceDao,
    private val faceEmbedding: FaceEmbedding
) {
    companion object {
        // DBSCAN parameters
        private const val DEFAULT_EPS = 0.4f // Cosine distance threshold (1 - similarity)
        private const val DEFAULT_MIN_PTS = 2 // Minimum faces to form a cluster
        
        // Embedding size
        private const val EMBEDDING_SIZE = 128
    }
    
    /**
     * Run DBSCAN clustering on all faces with embeddings.
     * Creates PersonEntity for each cluster and updates FaceEntity.personId.
     * 
     * @param eps Maximum cosine distance for neighbors (default 0.4)
     * @param minPts Minimum points to form a cluster (default 2)
     * @param progressCallback Optional callback for progress updates
     * @return List of cluster results
     */
    suspend fun runClustering(
        eps: Float = DEFAULT_EPS,
        minPts: Int = DEFAULT_MIN_PTS,
        progressCallback: ClusteringProgressCallback? = null
    ): List<ClusterResult> = withContext(Dispatchers.Default) {
        progressCallback?.invoke(0, 100, "加载人脸数据...")
        
        // Get all faces with embeddings
        val allFaces = faceDao.getUnassignedFacesSync() + 
            faceDao.getAllPersonsSync().flatMap { person ->
                // Include already assigned faces for re-clustering
                val photoIds = faceDao.getPhotoIdsForPersonSync(person.id)
                photoIds.flatMap { photoId -> 
                    faceDao.getFacesByPhotoIdSync(photoId).filter { it.personId == person.id }
                }
            }
        
        val facesWithEmbedding = allFaces
            .filter { it.embedding != null }
            .distinctBy { it.id }
        
        if (facesWithEmbedding.isEmpty()) {
            return@withContext emptyList()
        }
        
        progressCallback?.invoke(10, 100, "解析特征向量...")
        
        // Convert embeddings from ByteArray to FloatArray
        val faceEmbeddings = facesWithEmbedding.mapNotNull { face ->
            face.embedding?.let { bytes ->
                try {
                    Pair(face, faceEmbedding.byteArrayToEmbedding(bytes))
                } catch (e: Exception) {
                    null
                }
            }
        }
        
        if (faceEmbeddings.isEmpty()) {
            return@withContext emptyList()
        }
        
        progressCallback?.invoke(20, 100, "计算相似度矩阵...")
        
        // Run DBSCAN
        val clusters = dbscan(faceEmbeddings, eps, minPts) { progress ->
            val scaledProgress = 20 + (progress * 0.5).toInt()
            progressCallback?.invoke(scaledProgress, 100, "聚类分析中...")
        }
        
        progressCallback?.invoke(70, 100, "创建人物档案...")
        
        // Clear existing person assignments (for re-clustering)
        for (face in facesWithEmbedding) {
            if (face.personId != null) {
                faceDao.unassignFace(face.id)
            }
        }
        
        // Delete existing persons that will be replaced
        val existingPersons = faceDao.getAllPersonsSync()
        for (person in existingPersons) {
            val faceCount = faceDao.getFaceCountForPerson(person.id)
            if (faceCount == 0) {
                faceDao.deletePerson(person.id)
            }
        }
        
        progressCallback?.invoke(80, 100, "保存聚类结果...")
        
        // Create persons and assign faces
        val results = mutableListOf<ClusterResult>()
        var processedClusters = 0
        val totalClusters = clusters.size
        
        for ((clusterFaces, clusterEmbeddings) in clusters) {
            if (clusterFaces.isEmpty()) continue
            
            val personId = UUID.randomUUID().toString()
            val faceIds = clusterFaces.map { it.id }
            
            // Calculate average embedding
            val avgEmbedding = faceEmbedding.calculateAverageEmbedding(clusterEmbeddings)
            val avgEmbeddingBytes = faceEmbedding.embeddingToByteArray(avgEmbedding)
            
            // Create person entity
            val coverFace = clusterFaces.maxByOrNull { it.confidence }!!
            val person = PersonEntity(
                id = personId,
                name = null,
                coverFaceId = coverFace.id,
                faceCount = clusterFaces.size,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                averageEmbedding = avgEmbeddingBytes
            )
            
            faceDao.insertPerson(person)
            
            // Assign faces to person
            faceDao.assignFacesToPerson(faceIds, personId)
            
            results.add(ClusterResult(personId, faceIds, avgEmbedding))
            
            processedClusters++
            val progress = 80 + ((processedClusters.toFloat() / totalClusters) * 20).toInt()
            progressCallback?.invoke(progress, 100, "保存人物 $processedClusters / $totalClusters...")
        }
        
        // Handle noise points (faces that didn't fit any cluster)
        val assignedFaceIds = results.flatMap { it.faceIds }.toSet()
        val noiseFaces = faceEmbeddings.filter { it.first.id !in assignedFaceIds }
        
        // Create individual persons for noise faces (optional - can also leave them unassigned)
        for ((face, embedding) in noiseFaces) {
            val personId = UUID.randomUUID().toString()
            val person = PersonEntity(
                id = personId,
                name = null,
                coverFaceId = face.id,
                faceCount = 1,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                averageEmbedding = faceEmbedding.embeddingToByteArray(embedding)
            )
            faceDao.insertPerson(person)
            faceDao.assignFaceToPerson(face.id, personId)
            
            results.add(ClusterResult(personId, listOf(face.id), embedding))
        }
        
        progressCallback?.invoke(100, 100, "完成！共识别 ${results.size} 个人物")
        
        results
    }
    
    /**
     * DBSCAN clustering algorithm implementation.
     * 
     * @param data List of (FaceEntity, embedding) pairs
     * @param eps Maximum cosine distance for neighbors
     * @param minPts Minimum points to form a cluster
     * @param progressCallback Progress callback (0.0 - 1.0)
     * @return List of clusters, each containing faces and their embeddings
     */
    private fun dbscan(
        data: List<Pair<FaceEntity, FloatArray>>,
        eps: Float,
        minPts: Int,
        progressCallback: ((Float) -> Unit)?
    ): List<Pair<List<FaceEntity>, List<FloatArray>>> {
        val n = data.size
        if (n == 0) return emptyList()
        
        // Labels: -1 = noise, 0 = unvisited, > 0 = cluster ID
        val labels = IntArray(n) { 0 }
        var currentCluster = 0
        
        // Pre-compute distance matrix for small datasets
        // For large datasets, compute on-demand
        val useDistanceMatrix = n <= 500
        val distanceMatrix: Array<FloatArray>? = if (useDistanceMatrix) {
            Array(n) { i ->
                FloatArray(n) { j ->
                    if (i == j) 0f
                    else faceEmbedding.cosineDistance(data[i].second, data[j].second)
                }
            }
        } else null
        
        fun getDistance(i: Int, j: Int): Float {
            return distanceMatrix?.get(i)?.get(j)
                ?: faceEmbedding.cosineDistance(data[i].second, data[j].second)
        }
        
        fun getNeighbors(pointIdx: Int): List<Int> {
            val neighbors = mutableListOf<Int>()
            for (j in data.indices) {
                if (getDistance(pointIdx, j) <= eps) {
                    neighbors.add(j)
                }
            }
            return neighbors
        }
        
        // Main DBSCAN loop
        for (i in data.indices) {
            progressCallback?.invoke(i.toFloat() / n)
            
            if (labels[i] != 0) continue // Already processed
            
            val neighbors = getNeighbors(i)
            
            if (neighbors.size < minPts) {
                labels[i] = -1 // Mark as noise
                continue
            }
            
            // Start a new cluster
            currentCluster++
            labels[i] = currentCluster
            
            // Expand cluster
            val seedSet = neighbors.toMutableList()
            var seedIdx = 0
            
            while (seedIdx < seedSet.size) {
                val q = seedSet[seedIdx]
                
                if (labels[q] == -1) {
                    labels[q] = currentCluster // Change noise to border point
                }
                
                if (labels[q] != 0) {
                    seedIdx++
                    continue
                }
                
                labels[q] = currentCluster
                
                val qNeighbors = getNeighbors(q)
                if (qNeighbors.size >= minPts) {
                    for (neighbor in qNeighbors) {
                        if (neighbor !in seedSet) {
                            seedSet.add(neighbor)
                        }
                    }
                }
                
                seedIdx++
            }
        }
        
        // Group by cluster
        val clusters = mutableMapOf<Int, MutableList<Int>>()
        for (i in data.indices) {
            val label = labels[i]
            if (label > 0) { // Exclude noise (-1) and unvisited (0)
                clusters.getOrPut(label) { mutableListOf() }.add(i)
            }
        }
        
        // Convert to result format
        return clusters.values.map { indices ->
            Pair(
                indices.map { data[it].first },
                indices.map { data[it].second }
            )
        }
    }
    
    /**
     * Assign a face to an existing person.
     */
    suspend fun assignFaceToPerson(faceId: String, personId: String) = withContext(Dispatchers.IO) {
        faceDao.assignFaceToPerson(faceId, personId)
        
        // Update person's face count and average embedding
        updatePersonAfterChange(personId)
    }
    
    /**
     * Remove a face from its person (unassign).
     */
    suspend fun removeFaceFromPerson(faceId: String) = withContext(Dispatchers.IO) {
        val face = faceDao.getFace(faceId) ?: return@withContext
        val oldPersonId = face.personId ?: return@withContext
        
        faceDao.unassignFace(faceId)
        
        // Update old person or delete if no faces left
        val remainingFaceCount = faceDao.getFaceCountForPerson(oldPersonId)
        if (remainingFaceCount == 0) {
            faceDao.deletePerson(oldPersonId)
        } else {
            updatePersonAfterChange(oldPersonId)
        }
    }
    
    /**
     * Merge two persons into one.
     * All faces from person2 will be moved to person1.
     */
    suspend fun mergePersons(personId1: String, personId2: String) = withContext(Dispatchers.IO) {
        if (personId1 == personId2) return@withContext
        
        // Get all faces from person2
        val person2Faces = faceDao.getPhotoIdsForPersonSync(personId2)
            .flatMap { photoId -> 
                faceDao.getFacesByPhotoIdSync(photoId).filter { it.personId == personId2 }
            }
        
        // Assign all faces to person1
        for (face in person2Faces) {
            faceDao.assignFaceToPerson(face.id, personId1)
        }
        
        // Delete person2
        faceDao.deletePerson(personId2)
        
        // Update person1
        updatePersonAfterChange(personId1)
    }
    
    /**
     * Split a face from a person to create a new person.
     */
    suspend fun splitFaceToNewPerson(faceId: String): String? = withContext(Dispatchers.IO) {
        val face = faceDao.getFace(faceId) ?: return@withContext null
        val oldPersonId = face.personId ?: return@withContext null
        
        // Create new person
        val newPersonId = UUID.randomUUID().toString()
        val embedding = face.embedding?.let { faceEmbedding.byteArrayToEmbedding(it) }
        
        val newPerson = PersonEntity(
            id = newPersonId,
            name = null,
            coverFaceId = faceId,
            faceCount = 1,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            averageEmbedding = embedding?.let { faceEmbedding.embeddingToByteArray(it) }
        )
        
        faceDao.insertPerson(newPerson)
        faceDao.assignFaceToPerson(faceId, newPersonId)
        
        // Update old person or delete if no faces left
        val remainingFaceCount = faceDao.getFaceCountForPerson(oldPersonId)
        if (remainingFaceCount == 0) {
            faceDao.deletePerson(oldPersonId)
        } else {
            updatePersonAfterChange(oldPersonId)
        }
        
        newPersonId
    }
    
    /**
     * Find similar persons to a given person.
     * Useful for suggesting merges.
     */
    suspend fun findSimilarPersons(
        personId: String,
        threshold: Float = 0.6f,
        maxResults: Int = 5
    ): List<Pair<PersonEntity, Float>> = withContext(Dispatchers.Default) {
        val person = faceDao.getPerson(personId) ?: return@withContext emptyList()
        val personEmbedding = person.averageEmbedding?.let { 
            faceEmbedding.byteArrayToEmbedding(it) 
        } ?: return@withContext emptyList()
        
        val allPersons = faceDao.getAllPersonsSync()
        
        allPersons
            .filter { it.id != personId && it.averageEmbedding != null }
            .mapNotNull { otherPerson ->
                val otherEmbedding = faceEmbedding.byteArrayToEmbedding(otherPerson.averageEmbedding!!)
                val similarity = faceEmbedding.cosineSimilarity(personEmbedding, otherEmbedding)
                if (similarity >= threshold) {
                    Pair(otherPerson, similarity)
                } else {
                    null
                }
            }
            .sortedByDescending { it.second }
            .take(maxResults)
    }
    
    /**
     * Update person's face count and average embedding after changes.
     */
    private suspend fun updatePersonAfterChange(personId: String) {
        val faceCount = faceDao.getFaceCountForPerson(personId)
        faceDao.updatePersonFaceCount(personId, faceCount)
        
        // Recalculate average embedding
        val faces = faceDao.getPhotoIdsForPersonSync(personId)
            .flatMap { photoId -> 
                faceDao.getFacesByPhotoIdSync(photoId).filter { it.personId == personId }
            }
        
        val embeddings = faces.mapNotNull { face ->
            face.embedding?.let { faceEmbedding.byteArrayToEmbedding(it) }
        }
        
        if (embeddings.isNotEmpty()) {
            val avgEmbedding = faceEmbedding.calculateAverageEmbedding(embeddings)
            val person = faceDao.getPerson(personId)
            if (person != null) {
                faceDao.updatePerson(
                    person.copy(
                        averageEmbedding = faceEmbedding.embeddingToByteArray(avgEmbedding),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
        
        // Update cover face if current cover is no longer assigned
        val person = faceDao.getPerson(personId)
        if (person != null) {
            val coverFace = faceDao.getFace(person.coverFaceId)
            if (coverFace == null || coverFace.personId != personId) {
                // Select new cover face (highest confidence)
                val bestFace = faces.maxByOrNull { it.confidence }
                if (bestFace != null) {
                    faceDao.updatePersonCover(personId, bestFace.id)
                }
            }
        }
    }
}
