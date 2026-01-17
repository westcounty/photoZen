package com.example.photozen.ui.screens.smartgallery

import android.graphics.PointF
import android.view.Gravity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.example.photozen.util.MapLibreInitializer
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import java.text.SimpleDateFormat
import java.util.*

/**
 * Map View Screen using MapLibre (替代 Google Maps，与 PhotoPrism 一致)
 * 
 * 使用 OpenStreetMap 瓦片，完全开源，无需 API Key。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapLibreScreen(
    onNavigateBack: () -> Unit,
    onPhotoClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Initialize MapLibre lazily when map screen is opened
    var mapLibreInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        mapLibreInitialized = MapLibreInitializer.ensureInitialized(context)
    }
    
    // Map view reference
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    
    // Update map when data changes
    LaunchedEffect(uiState.hasPhotos, uiState.viewMode, mapLibreMap) {
        mapLibreMap?.let { map ->
            if (uiState.hasPhotos) {
                updateMapContent(map, uiState, viewModel, onPhotoClick)
            }
        }
    }
    
    // Update camera position
    LaunchedEffect(uiState.initialCameraPosition, mapLibreMap) {
        mapLibreMap?.let { map ->
            if (uiState.hasPhotos) {
                val position = CameraPosition.Builder()
                    .target(uiState.initialCameraPosition)
                    .zoom(uiState.initialZoom.toDouble() - 1) // MapLibre zoom is slightly different
                    .build()
                map.animateCamera(CameraUpdateFactory.newCameraPosition(position), 1000)
            }
        }
    }
    
    // Show errors
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }
    
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = null,
                            tint = Color(0xFF14B8A6)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "地图视图",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (uiState.totalPhotos > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${uiState.totalPhotos}",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
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
                    // View mode toggle
                    IconButton(onClick = { viewModel.toggleViewMode() }) {
                        Icon(
                            imageVector = when (uiState.viewMode) {
                                MapViewMode.CLUSTER -> Icons.Default.Timeline
                                MapViewMode.TRAJECTORY -> Icons.Default.GridView
                            },
                            contentDescription = when (uiState.viewMode) {
                                MapViewMode.CLUSTER -> "切换到轨迹模式"
                                MapViewMode.TRAJECTORY -> "切换到聚类模式"
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("加载照片位置...")
                    }
                }
            } else if (!uiState.hasPhotos) {
                // Empty state
                EmptyMapState()
            } else {
                // MapLibre Map
                AndroidView(
                    factory = { ctx ->
                        MapView(ctx).apply {
                            mapView = this
                            getMapAsync { map ->
                                mapLibreMap = map
                                
                                // 使用内联 style JSON，国内可用，无需额外配置
                                map.setStyle(Style.Builder().fromJson(MAPLIBRE_STYLE_JSON)) { style ->
                                    // Style loaded
                                    android.util.Log.i("MapLibreScreen", "Map style loaded")
                                    
                                    // Enable zoom controls
                                    map.uiSettings.apply {
                                        isZoomGesturesEnabled = true
                                        isScrollGesturesEnabled = true
                                        isRotateGesturesEnabled = true
                                        isTiltGesturesEnabled = false
                                        isCompassEnabled = true
                                        setCompassGravity(Gravity.TOP or Gravity.END)
                                        setCompassMargins(0, 100, 16, 0)
                                    }
                                    
                                    // Handle map click
                                    map.addOnMapClickListener {
                                        viewModel.selectCluster(null)
                                        viewModel.selectPhoto(null)
                                        true
                                    }
                                    
                                    // Initial content
                                    if (uiState.hasPhotos) {
                                        updateMapContent(map, uiState, viewModel, onPhotoClick)
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    onRelease = { view ->
                        view.onDestroy()
                    }
                )
                
                // View mode indicator chip
                ViewModeChip(
                    viewMode = uiState.viewMode,
                    onToggle = { viewModel.toggleViewMode() },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                )
                
                // Map attribution (OSM 版权要求)
                Text(
                    text = "© OpenStreetMap contributors",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                
                // Selected cluster preview
                AnimatedVisibility(
                    visible = uiState.selectedCluster != null,
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it },
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    uiState.selectedCluster?.let { cluster ->
                        ClusterPreviewCard(
                            cluster = cluster,
                            onPhotoClick = onPhotoClick,
                            onDismiss = { viewModel.selectCluster(null) }
                        )
                    }
                }
                
                // Selected single photo preview
                AnimatedVisibility(
                    visible = uiState.selectedPhoto != null && uiState.selectedCluster == null,
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it },
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    uiState.selectedPhoto?.let { photoWithLocation ->
                        PhotoPreviewCard(
                            photo = photoWithLocation,
                            onPhotoClick = { onPhotoClick(photoWithLocation.photo.id) },
                            onDismiss = { viewModel.selectPhoto(null) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Update map content based on view mode.
 */
private fun updateMapContent(
    map: MapLibreMap,
    uiState: MapUiState,
    viewModel: MapViewModel,
    onPhotoClick: (String) -> Unit
) {
    // Clear existing annotations
    map.clear()
    
    when (uiState.viewMode) {
        MapViewMode.CLUSTER -> {
            // Add cluster markers
            uiState.clusters.forEach { cluster ->
                val markerColor = when {
                    cluster.size > 50 -> android.graphics.Color.parseColor("#EF4444")
                    cluster.size > 20 -> android.graphics.Color.parseColor("#F59E0B")
                    cluster.size > 10 -> android.graphics.Color.parseColor("#8B5CF6")
                    else -> android.graphics.Color.parseColor("#3B82F6")
                }
                
                val marker = MarkerOptions()
                    .position(cluster.center)
                    .title(if (cluster.size == 1) cluster.coverPhoto?.displayName else "${cluster.size} 张照片")
                    .snippet(cluster.coverPhoto?.let { formatDate(it.dateTaken) } ?: "")
                
                map.addMarker(marker)
            }
            
            // Handle marker clicks
            map.setOnMarkerClickListener { marker ->
                val clickedPosition = marker.position
                val cluster = uiState.clusters.find { 
                    it.center.distanceTo(clickedPosition) < 100 
                }
                cluster?.let { viewModel.selectCluster(it) }
                true
            }
        }
        
        MapViewMode.TRAJECTORY -> {
            // Add trajectory line
            if (uiState.trajectoryPoints.size > 1) {
                val polylineOptions = PolylineOptions()
                    .addAll(uiState.trajectoryPoints.map { it })
                    .color(android.graphics.Color.parseColor("#8B5CF6"))
                    .width(4f)
                
                map.addPolyline(polylineOptions)
            }
            
            // Add photo markers
            uiState.allPhotos.forEach { photoWithLocation ->
                val marker = MarkerOptions()
                    .position(photoWithLocation.position)
                    .title(photoWithLocation.photo.displayName)
                    .snippet(formatDate(photoWithLocation.photo.dateTaken))
                
                map.addMarker(marker)
            }
            
            // Handle marker clicks
            map.setOnMarkerClickListener { marker ->
                val clickedPosition = marker.position
                val photo = uiState.allPhotos.find {
                    it.position.distanceTo(clickedPosition) < 100
                }
                photo?.let { viewModel.selectPhoto(it) }
                true
            }
        }
    }
}

// Note: MapViewModel now uses MapLibre's LatLng directly, no conversion needed

/**
 * View mode indicator chip.
 */
@Composable
private fun ViewModeChip(
    viewMode: MapViewMode,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
        shadowElevation = 4.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = when (viewMode) {
                    MapViewMode.CLUSTER -> Icons.Default.GridView
                    MapViewMode.TRAJECTORY -> Icons.Default.Timeline
                },
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = when (viewMode) {
                    MapViewMode.CLUSTER -> "聚类模式"
                    MapViewMode.TRAJECTORY -> "轨迹模式"
                },
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

/**
 * Preview card for selected cluster.
 */
@Composable
private fun ClusterPreviewCard(
    cluster: PhotoCluster,
    onPhotoClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${cluster.size} 张照片",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Photo thumbnails
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cluster.photos.take(10)) { photoWithLocation ->
                    PhotoThumbnail(
                        photo = photoWithLocation.photo,
                        onClick = { onPhotoClick(photoWithLocation.photo.id) }
                    )
                }
                
                // Show more indicator
                if (cluster.photos.size > 10) {
                    item {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { 
                                    cluster.photos.firstOrNull()?.let { 
                                        onPhotoClick(it.photo.id) 
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+${cluster.photos.size - 10}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Preview card for single selected photo.
 */
@Composable
private fun PhotoPreviewCard(
    photo: PhotoWithLocation,
    onPhotoClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable(onClick = onPhotoClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            AsyncImage(
                model = photo.photo.systemUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = photo.photo.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatDate(photo.photo.dateTaken),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatLocation(photo.position),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Close button
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭"
                )
            }
        }
    }
}

/**
 * Photo thumbnail in cluster preview.
 */
@Composable
private fun PhotoThumbnail(
    photo: com.example.photozen.data.local.entity.PhotoEntity,
    onClick: () -> Unit
) {
    AsyncImage(
        model = photo.systemUri,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    )
}

/**
 * Empty state when no photos have GPS data.
 */
@Composable
private fun EmptyMapState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocationOff,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "暂无位置信息",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "您的照片中没有包含 GPS 位置数据。\n拍照时请开启位置服务以记录照片位置。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Format timestamp to readable date string.
 */
private fun formatDate(timestamp: Long): String {
    if (timestamp == 0L) return "未知日期"
    val sdf = SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.CHINA)
    return sdf.format(Date(timestamp))
}

/**
 * Format LatLng to readable location string.
 */
private fun formatLocation(position: LatLng): String {
    val lat = String.format(Locale.US, "%.4f", position.latitude)
    val lng = String.format(Locale.US, "%.4f", position.longitude)
    return "📍 $lat, $lng"
}

/**
 * MapLibre 地图配置
 * 
 * 使用国内可访问的 OpenStreetMap 瓦片服务
 * 注意：不同瓦片服务的可用性可能会变化
 */
object MapTileConfig {
    // 主要瓦片源：OpenStreetMap 官方 (国内可访问，但速度可能较慢)
    const val OSM_TILE_URL = "https://tile.openstreetmap.org/{z}/{x}/{y}.png"
    
    // 备用瓦片源：CARTO (国内通常可访问)
    const val CARTO_LIGHT_URL = "https://basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png"
    const val CARTO_DARK_URL = "https://basemaps.cartocdn.com/dark_all/{z}/{x}/{y}.png"
    
    // 备用：Stamen (国内通常可访问)
    const val STAMEN_TERRAIN_URL = "https://tiles.stadiamaps.com/tiles/stamen_terrain/{z}/{x}/{y}.png"
    
    // 默认使用 CARTO Light (国内访问稳定)
    const val DEFAULT_TILE_URL = CARTO_LIGHT_URL
    
    // 生成 MapLibre 兼容的 style JSON
    fun generateStyleJson(tileUrl: String = DEFAULT_TILE_URL): String {
        return """
        {
            "version": 8,
            "name": "PicZen Map",
            "sources": {
                "osm": {
                    "type": "raster",
                    "tiles": ["$tileUrl"],
                    "tileSize": 256,
                    "attribution": "© OpenStreetMap contributors"
                }
            },
            "layers": [
                {
                    "id": "osm",
                    "type": "raster",
                    "source": "osm"
                }
            ]
        }
        """.trimIndent()
    }
}

// 使用内联 style JSON，无需网络请求 style 文件
private val MAPLIBRE_STYLE_JSON = MapTileConfig.generateStyleJson()
