package com.example.photozen.ui.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.example.photozen.data.local.dao.PhotoDao
import com.example.photozen.data.local.dao.TagDao
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.worker.LocationScannerWorker
import com.example.photozen.di.LocationScanScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.sqrt

/**
 * Represents a point on the trajectory map.
 */
data class TrajectoryPoint(
    val photoId: String,
    val geoPoint: GeoPoint,
    val dateTaken: Long,
    val displayName: String,
    val systemUri: String,
    val isMarker: Boolean = false // Whether to show as a marker (not all points are markers)
)

/**
 * Scan progress data.
 */
data class ScanProgress(
    val total: Int = 0,
    val scanned: Int = 0,
    val withGps: Int = 0,
    val progress: Int = 0 // 0-100
)

/**
 * UI State for the Map screen.
 */
data class MapUiState(
    val isLoading: Boolean = true,
    val trajectoryPoints: List<TrajectoryPoint> = emptyList(),
    val markerPoints: List<TrajectoryPoint> = emptyList(),
    val boundingBox: BoundingBox? = null,
    val centerPoint: GeoPoint? = null,
    val totalPhotosWithGps: Int = 0,
    val pendingGpsScan: Int = 0,
    val isScanning: Boolean = false,
    val scanProgress: ScanProgress = ScanProgress(),
    val error: String? = null
) {
    val hasData: Boolean
        get() = trajectoryPoints.isNotEmpty()
    
    val needsScan: Boolean
        get() = pendingGpsScan > 0
}

/**
 * ViewModel for the Map screen.
 * 
 * Handles loading photos with GPS data and generating trajectory points.
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    private val photoDao: PhotoDao,
    private val tagDao: TagDao,
    private val locationScanScheduler: LocationScanScheduler
) : ViewModel() {
    
    companion object {
        // Distance threshold in meters for showing markers
        private const val MARKER_DISTANCE_THRESHOLD = 500.0
        // Maximum markers to show for performance
        private const val MAX_MARKERS = 50
        // Minimum distance between consecutive points to include
        private const val MIN_POINT_DISTANCE = 10.0
    }
    
    private val _isLoading = MutableStateFlow(true)
    private val _isScanning = MutableStateFlow(false)
    private val _scanProgress = MutableStateFlow(ScanProgress())
    private val _error = MutableStateFlow<String?>(null)
    private val _trajectoryPoints = MutableStateFlow<List<TrajectoryPoint>>(emptyList())
    private val _markerPoints = MutableStateFlow<List<TrajectoryPoint>>(emptyList())
    private val _boundingBox = MutableStateFlow<BoundingBox?>(null)
    private val _centerPoint = MutableStateFlow<GeoPoint?>(null)
    
    // Combine map data flows
    private val mapDataFlow = combine(
        _trajectoryPoints,
        _markerPoints,
        _boundingBox,
        _centerPoint
    ) { trajectory, markers, box, center ->
        MapDataHolder(trajectory, markers, box, center)
    }
    
    // Combine state flows
    private val stateFlow = combine(
        _isLoading,
        _isScanning,
        _scanProgress,
        _error
    ) { isLoading, isScanning, scanProgress, error ->
        StateHolder(isLoading, isScanning, scanProgress, error)
    }
    
    // Data class holders
    private data class MapDataHolder(
        val trajectoryPoints: List<TrajectoryPoint>,
        val markerPoints: List<TrajectoryPoint>,
        val boundingBox: BoundingBox?,
        val centerPoint: GeoPoint?
    )
    
    private data class StateHolder(
        val isLoading: Boolean,
        val isScanning: Boolean,
        val scanProgress: ScanProgress,
        val error: String?
    )
    
    /**
     * UI State exposed to the screen.
     */
    val uiState: StateFlow<MapUiState> = combine(
        stateFlow,
        mapDataFlow,
        photoDao.getPhotosWithGpsCount(),
        photoDao.getPendingGpsScanCount()
    ) { state, mapData, gpsCount, pendingCount ->
        MapUiState(
            isLoading = state.isLoading,
            isScanning = state.isScanning,
            scanProgress = state.scanProgress,
            trajectoryPoints = mapData.trajectoryPoints,
            markerPoints = mapData.markerPoints,
            boundingBox = mapData.boundingBox,
            centerPoint = mapData.centerPoint,
            totalPhotosWithGps = gpsCount,
            pendingGpsScan = pendingCount,
            error = state.error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MapUiState()
    )
    
    // Track current collection job to cancel when loading different data
    private var currentLoadJob: Job? = null
    private var currentTagId: String? = null
    
    // Note: We don't load in init - let the Screen call the appropriate load method
    
    /**
     * Load photos with GPS data and generate trajectory.
     */
    fun loadPhotosWithGps() {
        // Cancel any previous load job
        currentLoadJob?.cancel()
        currentTagId = null
        
        currentLoadJob = viewModelScope.launch {
            _isLoading.value = true
            try {
                photoDao.getPhotosWithGps().collect { photos ->
                    processPhotos(photos)
                    _isLoading.value = false
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = "加载位置数据失败: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Load photos for a specific tag that have GPS data.
     */
    fun loadPhotosForTag(tagId: String) {
        // If already loading this tag, skip
        if (currentTagId == tagId && currentLoadJob?.isActive == true) return
        
        // Cancel any previous load job
        currentLoadJob?.cancel()
        currentTagId = tagId
        
        currentLoadJob = viewModelScope.launch {
            _isLoading.value = true
            _trajectoryPoints.value = emptyList()
            _markerPoints.value = emptyList()
            
            try {
                tagDao.getPhotoIdsWithTag(tagId).collect { photoIds ->
                    if (photoIds.isEmpty()) {
                        _trajectoryPoints.value = emptyList()
                        _markerPoints.value = emptyList()
                        _boundingBox.value = null
                        _centerPoint.value = null
                        _isLoading.value = false
                        return@collect
                    }
                    
                    // Get photos with GPS for this tag
                    val photos = photoIds.mapNotNull { photoId ->
                        photoDao.getById(photoId)
                    }.filter { it.latitude != null && it.longitude != null }
                    
                    processPhotos(photos)
                    _isLoading.value = false
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = "加载标签照片位置失败: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Process photos and generate trajectory points.
     */
    private suspend fun processPhotos(photos: List<PhotoEntity>) = withContext(Dispatchers.Default) {
        if (photos.isEmpty()) {
            _trajectoryPoints.value = emptyList()
            _markerPoints.value = emptyList()
            _boundingBox.value = null
            _centerPoint.value = null
            return@withContext
        }
        
        // Convert to trajectory points
        val allPoints = photos.mapNotNull { photo ->
            val lat = photo.latitude ?: return@mapNotNull null
            val lng = photo.longitude ?: return@mapNotNull null
            
            TrajectoryPoint(
                photoId = photo.id,
                geoPoint = GeoPoint(lat, lng),
                dateTaken = photo.dateTaken,
                displayName = photo.displayName ?: "Photo",
                systemUri = photo.systemUri
            )
        }.sortedBy { it.dateTaken }
        
        // Filter out points too close together
        val filteredPoints = filterClosePoints(allPoints)
        
        // Select points to show as markers
        val markers = selectMarkerPoints(filteredPoints)
        
        // Calculate bounding box
        val box = calculateBoundingBox(filteredPoints)
        
        // Calculate center point
        val center = if (filteredPoints.isNotEmpty()) {
            val avgLat = filteredPoints.map { it.geoPoint.latitude }.average()
            val avgLng = filteredPoints.map { it.geoPoint.longitude }.average()
            GeoPoint(avgLat, avgLng)
        } else null
        
        _trajectoryPoints.value = filteredPoints
        _markerPoints.value = markers
        _boundingBox.value = box
        _centerPoint.value = center
    }
    
    /**
     * Filter out points that are too close together.
     */
    private fun filterClosePoints(points: List<TrajectoryPoint>): List<TrajectoryPoint> {
        if (points.size <= 2) return points
        
        val result = mutableListOf(points.first())
        
        for (i in 1 until points.size) {
            val current = points[i]
            val last = result.last()
            val distance = calculateDistance(
                last.geoPoint.latitude, last.geoPoint.longitude,
                current.geoPoint.latitude, current.geoPoint.longitude
            )
            
            if (distance >= MIN_POINT_DISTANCE) {
                result.add(current)
            }
        }
        
        // Always include the last point
        if (result.last() != points.last()) {
            result.add(points.last())
        }
        
        return result
    }
    
    /**
     * Select which points to show as markers (every Nth point or significant distance change).
     */
    private fun selectMarkerPoints(points: List<TrajectoryPoint>): List<TrajectoryPoint> {
        if (points.isEmpty()) return emptyList()
        if (points.size <= MAX_MARKERS) {
            // If few points, show all as markers
            return points.map { it.copy(isMarker = true) }
        }
        
        val markers = mutableListOf<TrajectoryPoint>()
        markers.add(points.first().copy(isMarker = true)) // Always include first
        
        var lastMarkerPoint = points.first()
        
        for (i in 1 until points.size - 1) {
            val current = points[i]
            val distance = calculateDistance(
                lastMarkerPoint.geoPoint.latitude, lastMarkerPoint.geoPoint.longitude,
                current.geoPoint.latitude, current.geoPoint.longitude
            )
            
            if (distance >= MARKER_DISTANCE_THRESHOLD) {
                markers.add(current.copy(isMarker = true))
                lastMarkerPoint = current
                
                if (markers.size >= MAX_MARKERS - 1) break
            }
        }
        
        // Always include last point
        if (markers.last().photoId != points.last().photoId) {
            markers.add(points.last().copy(isMarker = true))
        }
        
        return markers
    }
    
    /**
     * Calculate bounding box for all points.
     */
    private fun calculateBoundingBox(points: List<TrajectoryPoint>): BoundingBox? {
        if (points.isEmpty()) return null
        
        var minLat = Double.MAX_VALUE
        var maxLat = Double.MIN_VALUE
        var minLng = Double.MAX_VALUE
        var maxLng = Double.MIN_VALUE
        
        points.forEach { point ->
            minLat = minOf(minLat, point.geoPoint.latitude)
            maxLat = maxOf(maxLat, point.geoPoint.latitude)
            minLng = minOf(minLng, point.geoPoint.longitude)
            maxLng = maxOf(maxLng, point.geoPoint.longitude)
        }
        
        // Add some padding
        val latPadding = (maxLat - minLat) * 0.1
        val lngPadding = (maxLng - minLng) * 0.1
        
        return BoundingBox(
            maxLat + latPadding,
            maxLng + lngPadding,
            minLat - latPadding,
            minLng - lngPadding
        )
    }
    
    /**
     * Calculate distance between two coordinates in meters (Haversine formula).
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // meters
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        
        val c = 2 * kotlin.math.atan2(sqrt(a), sqrt(1 - a))
        
        return earthRadius * c
    }
    
    /**
     * Trigger GPS location scan for photos.
     * Observes WorkManager state to properly track progress.
     */
    fun triggerGpsScan() {
        _isScanning.value = true
        _scanProgress.value = ScanProgress()
        _error.value = null

        // Schedule the scan
        locationScanScheduler.scheduleOneTimeScan()

        // Observe the work progress
        viewModelScope.launch {
            try {
                locationScanScheduler.getWorkInfoFlow().collect { workInfos ->
                    val workInfo = workInfos.firstOrNull()

                    when (workInfo?.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            val total = workInfo.outputData.getInt(LocationScannerWorker.KEY_TOTAL, 0)
                            val scanned = workInfo.outputData.getInt(LocationScannerWorker.KEY_SCANNED, 0)
                            val withGps = workInfo.outputData.getInt(LocationScannerWorker.KEY_WITH_GPS, 0)
                            _scanProgress.value = ScanProgress(total, scanned, withGps, 100)
                            _isScanning.value = false
                            if (withGps == 0 && scanned > 0) {
                                _error.value = "扫描了 $scanned 张照片，未找到 GPS 信息"
                            }
                            // Reload photos with GPS data
                            loadPhotosWithGps()
                            return@collect
                        }
                        WorkInfo.State.FAILED -> {
                            _isScanning.value = false
                            _error.value = "GPS 扫描失败"
                            return@collect
                        }
                        WorkInfo.State.CANCELLED -> {
                            // Scan was stopped, but progress is preserved
                            val scanned = workInfo.outputData.getInt(LocationScannerWorker.KEY_SCANNED, 0)
                            val withGps = workInfo.outputData.getInt(LocationScannerWorker.KEY_WITH_GPS, 0)
                            _isScanning.value = false
                            if (scanned > 0) {
                                _error.value = "扫描已暂停，已扫描 $scanned 张（含 $withGps 张有位置信息）"
                            }
                            // Reload photos with GPS data
                            loadPhotosWithGps()
                            return@collect
                        }
                        WorkInfo.State.RUNNING -> {
                            // Update progress from worker
                            val progress = workInfo.progress
                            val total = progress.getInt(LocationScannerWorker.KEY_TOTAL, 0)
                            val scanned = progress.getInt(LocationScannerWorker.KEY_SCANNED, 0)
                            val withGps = progress.getInt(LocationScannerWorker.KEY_WITH_GPS, 0)
                            val percent = progress.getInt(LocationScannerWorker.KEY_PROGRESS, 0)
                            _scanProgress.value = ScanProgress(total, scanned, withGps, percent)
                        }
                        else -> {
                            // ENQUEUED or BLOCKED - waiting to run
                        }
                    }
                }
            } catch (e: Exception) {
                _isScanning.value = false
                _error.value = "扫描出错: ${e.message}"
            }
        }
    }
    
    /**
     * Stop the GPS scan.
     * Progress is preserved and can be resumed later.
     */
    fun stopGpsScan() {
        locationScanScheduler.cancelScans()
        // The observer in triggerGpsScan will handle the CANCELLED state
    }
    
    /**
     * Clear error message.
     */
    fun clearError() {
        _error.value = null
    }
}
