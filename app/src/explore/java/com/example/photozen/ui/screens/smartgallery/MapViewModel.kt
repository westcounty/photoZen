package com.example.photozen.ui.screens.smartgallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.data.local.dao.PhotoDao
import com.example.photozen.data.local.entity.PhotoEntity
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Photo with its GPS location for map display.
 */
data class PhotoWithLocation(
    val photo: PhotoEntity,
    val position: LatLng
)

/**
 * A cluster of photos at nearby locations.
 */
data class PhotoCluster(
    val id: String,
    val center: LatLng,
    val photos: List<PhotoWithLocation>,
    val bounds: LatLngBounds?
) {
    val size: Int get() = photos.size
    val coverPhoto: PhotoEntity? get() = photos.firstOrNull()?.photo
}

/**
 * Map view display modes.
 */
enum class MapViewMode {
    CLUSTER,    // Show clustered markers
    TRAJECTORY  // Show photo trajectory line
}

/**
 * UI State for Map View screen.
 */
data class MapUiState(
    val allPhotos: List<PhotoWithLocation> = emptyList(),
    val clusters: List<PhotoCluster> = emptyList(),
    val selectedCluster: PhotoCluster? = null,
    val selectedPhoto: PhotoWithLocation? = null,
    val isLoading: Boolean = true,
    val initialCameraPosition: LatLng = LatLng(39.9042, 116.4074), // Default: Beijing
    val initialZoom: Float = 4f,
    val viewMode: MapViewMode = MapViewMode.CLUSTER,
    val trajectoryPoints: List<LatLng> = emptyList(),
    val error: String? = null
) {
    val totalPhotos: Int get() = allPhotos.size
    val hasPhotos: Boolean get() = allPhotos.isNotEmpty()
}

/**
 * ViewModel for Map View screen.
 * Handles loading GPS photos and clustering logic.
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    private val photoDao: PhotoDao
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()
    
    // Cluster radius in degrees (approximately 50km at equator)
    private val clusterRadius = 0.5
    
    init {
        loadPhotosWithGps()
    }
    
    /**
     * Load all photos with GPS data.
     */
    private fun loadPhotosWithGps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                photoDao.getPhotosWithGps().collect { photos ->
                    val photosWithLocation = photos.mapNotNull { photo ->
                        if (photo.latitude != null && photo.longitude != null) {
                            PhotoWithLocation(
                                photo = photo,
                                position = LatLng(photo.latitude, photo.longitude)
                            )
                        } else null
                    }
                    
                    // Calculate clusters
                    val clusters = calculateClusters(photosWithLocation)
                    
                    // Calculate trajectory (sorted by date taken)
                    val trajectoryPoints = photosWithLocation
                        .sortedBy { it.photo.dateTaken }
                        .map { it.position }
                    
                    // Calculate initial camera position (center of all photos)
                    val initialPosition = calculateCenterPosition(photosWithLocation)
                    val initialZoom = calculateInitialZoom(photosWithLocation)
                    
                    _uiState.update { state ->
                        state.copy(
                            allPhotos = photosWithLocation,
                            clusters = clusters,
                            trajectoryPoints = trajectoryPoints,
                            initialCameraPosition = initialPosition,
                            initialZoom = initialZoom,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = "加载照片位置失败: ${e.message}"
                    ) 
                }
            }
        }
    }
    
    /**
     * Calculate photo clusters using a simple grid-based algorithm.
     */
    private fun calculateClusters(photos: List<PhotoWithLocation>): List<PhotoCluster> {
        if (photos.isEmpty()) return emptyList()
        
        val clusters = mutableListOf<PhotoCluster>()
        val assigned = mutableSetOf<Int>()
        
        for (i in photos.indices) {
            if (i in assigned) continue
            
            val clusterPhotos = mutableListOf(photos[i])
            assigned.add(i)
            
            // Find nearby photos
            for (j in (i + 1) until photos.size) {
                if (j in assigned) continue
                
                val distance = calculateDistance(photos[i].position, photos[j].position)
                if (distance < clusterRadius) {
                    clusterPhotos.add(photos[j])
                    assigned.add(j)
                }
            }
            
            // Calculate cluster center
            val centerLat = clusterPhotos.map { it.position.latitude }.average()
            val centerLng = clusterPhotos.map { it.position.longitude }.average()
            
            // Calculate bounds
            val bounds = if (clusterPhotos.size > 1) {
                val builder = LatLngBounds.Builder()
                clusterPhotos.forEach { builder.include(it.position) }
                builder.build()
            } else null
            
            clusters.add(
                PhotoCluster(
                    id = UUID.randomUUID().toString(),
                    center = LatLng(centerLat, centerLng),
                    photos = clusterPhotos.sortedByDescending { it.photo.dateTaken },
                    bounds = bounds
                )
            )
        }
        
        return clusters.sortedByDescending { it.size }
    }
    
    /**
     * Calculate distance between two LatLng points in degrees.
     * Uses simplified Euclidean distance (sufficient for clustering).
     */
    private fun calculateDistance(a: LatLng, b: LatLng): Double {
        val latDiff = a.latitude - b.latitude
        // Adjust longitude difference for latitude
        val lngDiff = (a.longitude - b.longitude) * cos(Math.toRadians(a.latitude))
        return sqrt(latDiff.pow(2) + lngDiff.pow(2))
    }
    
    /**
     * Calculate center position of all photos.
     */
    private fun calculateCenterPosition(photos: List<PhotoWithLocation>): LatLng {
        if (photos.isEmpty()) return LatLng(39.9042, 116.4074) // Default: Beijing
        
        val avgLat = photos.map { it.position.latitude }.average()
        val avgLng = photos.map { it.position.longitude }.average()
        return LatLng(avgLat, avgLng)
    }
    
    /**
     * Calculate initial zoom level based on photo spread.
     */
    private fun calculateInitialZoom(photos: List<PhotoWithLocation>): Float {
        if (photos.size <= 1) return 12f
        
        val lats = photos.map { it.position.latitude }
        val lngs = photos.map { it.position.longitude }
        
        val latSpread = (lats.maxOrNull() ?: 0.0) - (lats.minOrNull() ?: 0.0)
        val lngSpread = (lngs.maxOrNull() ?: 0.0) - (lngs.minOrNull() ?: 0.0)
        val maxSpread = maxOf(latSpread, lngSpread)
        
        return when {
            maxSpread > 100 -> 2f
            maxSpread > 50 -> 3f
            maxSpread > 20 -> 4f
            maxSpread > 10 -> 5f
            maxSpread > 5 -> 6f
            maxSpread > 2 -> 7f
            maxSpread > 1 -> 8f
            maxSpread > 0.5 -> 9f
            maxSpread > 0.2 -> 10f
            maxSpread > 0.1 -> 11f
            else -> 12f
        }
    }
    
    /**
     * Select a cluster to show its photos.
     */
    fun selectCluster(cluster: PhotoCluster?) {
        _uiState.update { it.copy(selectedCluster = cluster, selectedPhoto = null) }
    }
    
    /**
     * Select a single photo to show preview.
     */
    fun selectPhoto(photo: PhotoWithLocation?) {
        _uiState.update { it.copy(selectedPhoto = photo) }
    }
    
    /**
     * Toggle between cluster and trajectory view modes.
     */
    fun toggleViewMode() {
        _uiState.update { state ->
            state.copy(
                viewMode = when (state.viewMode) {
                    MapViewMode.CLUSTER -> MapViewMode.TRAJECTORY
                    MapViewMode.TRAJECTORY -> MapViewMode.CLUSTER
                },
                selectedCluster = null,
                selectedPhoto = null
            )
        }
    }
    
    /**
     * Set view mode explicitly.
     */
    fun setViewMode(mode: MapViewMode) {
        _uiState.update { it.copy(viewMode = mode, selectedCluster = null, selectedPhoto = null) }
    }
    
    /**
     * Clear error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
