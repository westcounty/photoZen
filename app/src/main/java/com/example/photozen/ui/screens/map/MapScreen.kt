package com.example.photozen.ui.screens.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.photozen.ui.theme.KeepGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/**
 * Map Screen - Displays photo locations on OpenStreetMap.
 * 
 * Features:
 * - Trajectory line connecting photos in time order
 * - Photo thumbnail markers at significant locations
 * - Auto-fit to show all markers
 * 
 * @param tagId Optional tag ID to filter photos by tag
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPhoto: (photoId: String) -> Unit,
    modifier: Modifier = Modifier,
    tagId: String? = null,
    viewModel: MapViewModel = hiltViewModel()
) {
    // Load photos based on whether tagId is provided
    LaunchedEffect(Unit) {
        if (tagId != null) {
            viewModel.loadPhotosForTag(tagId)
        } else {
            viewModel.loadPhotosWithGps()
        }
    }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // MapView reference
    var mapView by remember { mutableStateOf<MapView?>(null) }
    
    // Initialize osmdroid configuration
    LaunchedEffect(Unit) {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidBasePath = context.getExternalFilesDir(null)
            osmdroidTileCache = context.getExternalFilesDir("osmdroid/tiles")
        }
    }
    
    // Show error messages
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }
    
    // Update map when data changes
    LaunchedEffect(uiState.trajectoryPoints, uiState.markerPoints) {
        mapView?.let { map ->
            updateMapOverlays(
                context = context,
                mapView = map,
                trajectoryPoints = uiState.trajectoryPoints,
                markerPoints = uiState.markerPoints,
                onMarkerClick = { photoId -> onNavigateToPhoto(photoId) }
            )
            
            // Zoom to fit all points
            uiState.boundingBox?.let { box ->
                map.zoomToBoundingBox(box, true, 50)
            }
        }
    }
    
    // Determine the title based on whether we're viewing a tag or all photos
    val title = if (tagId != null) "标签足迹" else "足迹地图"
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge
                        )
                        val subtitle = when {
                            uiState.isLoading -> "加载中..."
                            uiState.trajectoryPoints.isNotEmpty() -> 
                                "${uiState.trajectoryPoints.size} 张照片有位置信息"
                            else -> null
                        }
                        subtitle?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            // Reload based on whether we have a tag or not
                            if (tagId != null) {
                                viewModel.loadPhotosForTag(tagId)
                            } else {
                                viewModel.loadPhotosWithGps()
                            }
                        },
                        enabled = !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "刷新"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (uiState.hasData) {
                FloatingActionButton(
                    onClick = {
                        uiState.boundingBox?.let { box ->
                            mapView?.zoomToBoundingBox(box, true, 50)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "显示所有"
                    )
                }
            }
        },
        modifier = modifier
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingContent()
                }
                !uiState.hasData -> {
                    EmptyContent(
                        isTagMode = tagId != null,
                        pendingCount = uiState.pendingGpsScan,
                        isScanning = uiState.isScanning,
                        scanProgress = uiState.scanProgress,
                        onScanClick = { viewModel.triggerGpsScan() },
                        onStopClick = { viewModel.stopGpsScan() }
                    )
                }
                else -> {
                    // OSM Map View
                    OsmdroidMapView(
                        onMapReady = { map ->
                            mapView = map
                            // Initial setup
                            uiState.centerPoint?.let { center ->
                                map.controller.setZoom(10.0)
                                map.controller.setCenter(center)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Stats overlay
                    StatsOverlay(
                        pointCount = uiState.trajectoryPoints.size,
                        markerCount = uiState.markerPoints.size,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Osmdroid MapView wrapper for Compose.
 */
@Composable
private fun OsmdroidMapView(
    onMapReady: (MapView) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(5.0)
            controller.setCenter(GeoPoint(35.0, 105.0)) // Default to China center
        }
    }
    
    // Handle lifecycle
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }
    
    AndroidView(
        factory = {
            onMapReady(mapView)
            mapView
        },
        modifier = modifier
    )
}

/**
 * Update map overlays with trajectory and markers.
 */
private suspend fun updateMapOverlays(
    context: Context,
    mapView: MapView,
    trajectoryPoints: List<TrajectoryPoint>,
    markerPoints: List<TrajectoryPoint>,
    onMarkerClick: (String) -> Unit
) = withContext(Dispatchers.Main) {
    // Clear existing overlays
    mapView.overlays.clear()
    
    if (trajectoryPoints.isEmpty()) return@withContext
    
    // Add trajectory polyline
    val polyline = Polyline().apply {
        outlinePaint.apply {
            color = Color.parseColor("#5EEAD4") // Teal accent
            strokeWidth = 8f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }
        setPoints(trajectoryPoints.map { it.geoPoint })
    }
    mapView.overlays.add(polyline)
    
    // Add markers for selected points
    markerPoints.forEach { point ->
        val marker = Marker(mapView).apply {
            position = point.geoPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = point.displayName
            snippet = formatDate(point.dateTaken)
            
            // Create circular marker icon
            icon = createMarkerIcon(context)
            
            setOnMarkerClickListener { _, _ ->
                onMarkerClick(point.photoId)
                true
            }
        }
        mapView.overlays.add(marker)
    }
    
    mapView.invalidate()
}

/**
 * Create a simple circular marker icon.
 */
private fun createMarkerIcon(context: Context): BitmapDrawable {
    val size = 40
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    val paint = Paint().apply {
        isAntiAlias = true
        color = Color.parseColor("#5EEAD4")
    }
    
    // Draw outer circle
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2, paint)
    
    // Draw inner white circle
    paint.color = Color.WHITE
    canvas.drawCircle(size / 2f, size / 2f, size / 3f, paint)
    
    // Draw center dot
    paint.color = Color.parseColor("#5EEAD4")
    canvas.drawCircle(size / 2f, size / 2f, size / 6f, paint)
    
    return BitmapDrawable(context.resources, bitmap)
}

/**
 * Format timestamp to readable date.
 */
private fun formatDate(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "加载位置数据中...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyContent(
    isTagMode: Boolean,
    pendingCount: Int,
    isScanning: Boolean,
    scanProgress: ScanProgress,
    onScanClick: () -> Unit,
    onStopClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isTagMode) {
                // Tag mode - show tag-specific message
                Text(
                    text = "该标签下的照片暂无位置信息",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "这些照片可能未包含 GPS 数据\n或尚未进行 GPS 扫描",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                
                if (pendingCount > 0 && !isScanning) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "全局有 $pendingCount 张照片待扫描",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onScanClick) {
                        Text("扫描全部照片")
                    }
                }
            } else {
                // Global map mode
                Text(
                    text = "暂无位置信息",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                if (isScanning) {
                    // Show scanning progress
                    Text(
                        text = "正在扫描照片 GPS 信息...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Progress bar
                    LinearProgressIndicator(
                        progress = { scanProgress.progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = KeepGreen
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Progress text
                    Text(
                        text = "${scanProgress.scanned} / ${scanProgress.total}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (scanProgress.withGps > 0) {
                        Text(
                            text = "已找到 ${scanProgress.withGps} 张有位置信息",
                            style = MaterialTheme.typography.bodySmall,
                            color = KeepGreen
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Stop button
                    OutlinedButton(onClick = onStopClick) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("暂停扫描")
                    }
                    
                    Text(
                        text = "暂停后可查看已扫描的照片，稍后继续扫描",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else if (pendingCount > 0) {
                    Text(
                        text = "有 $pendingCount 张照片待扫描 GPS 信息",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onScanClick) {
                        Text("开始扫描")
                    }
                } else {
                    Text(
                        text = "照片中未找到 GPS 数据\n请确保照片拍摄时已开启位置记录",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsOverlay(
    pointCount: Int,
    markerCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = KeepGreen,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "$pointCount 个轨迹点",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$markerCount 个标记",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
