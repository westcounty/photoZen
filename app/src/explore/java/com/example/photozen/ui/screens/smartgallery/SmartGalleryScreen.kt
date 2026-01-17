package com.example.photozen.ui.screens.smartgallery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Smart Gallery Screen - Entry point for AI-powered features.
 * Shows analysis progress and provides navigation to all smart features.
 * 
 * Based on plans:
 * - 智能画廊功能整合
 * - photoprism_全面对齐优化
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartGalleryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLabels: () -> Unit = {},
    onNavigateToPersons: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToSimilar: () -> Unit = {},
    onNavigateToMap: () -> Unit = {},
    onNavigateToTimeline: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SmartGalleryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Handle errors
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "智能画廊",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    ) 
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
                    // Analysis control button
                    if (uiState.unanalyzedPhotos > 0) {
                        IconButton(
                            onClick = {
                                if (uiState.isAnalyzing) {
                                    viewModel.stopAnalysis()
                                } else {
                                    viewModel.startAnalysis()
                                }
                            }
                        ) {
                            if (uiState.isAnalyzing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "开始分析"
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Analysis Progress Section
            AnalysisProgressSection(
                uiState = uiState,
                onStartAnalysis = { viewModel.startAnalysis() },
                onStopAnalysis = { viewModel.stopAnalysis() }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Quick Stats
            QuickStatsRow(uiState = uiState)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Browse Section - Timeline & Map
            FeatureSectionHeader(
                icon = Icons.Default.Folder,
                title = "浏览"
            )
            
            BrowseFeatureRow(
                features = viewModel.getFeatures(uiState).filter { it.id in listOf("timeline", "map") },
                onFeatureClick = { featureId ->
                    when (featureId) {
                        "map" -> onNavigateToMap()
                        "timeline" -> onNavigateToTimeline()
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // AI Features Section
            FeatureSectionHeader(
                icon = Icons.Default.AutoAwesome,
                title = "AI 功能"
            )
            
            AIFeatureGrid(
                features = viewModel.getFeatures(uiState).filter { it.id in listOf("labels", "faces", "search", "similar") },
                onFeatureClick = { featureId ->
                    when (featureId) {
                        "faces" -> onNavigateToPersons()
                        "labels" -> onNavigateToLabels()
                        "search" -> onNavigateToSearch()
                        "similar" -> onNavigateToSimilar()
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Help tip
            HelpTipCard()
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Analysis progress section with start/stop controls.
 */
@Composable
private fun AnalysisProgressSection(
    uiState: SmartGalleryUiState,
    onStartAnalysis: () -> Unit,
    onStopAnalysis: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (uiState.isAnalyzing) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (uiState.isAnalyzing) "正在分析照片..." else "AI 分析",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (uiState.unanalyzedPhotos > 0) {
                            "${uiState.analyzedPhotos} / ${uiState.totalPhotos} 张已分析"
                        } else {
                            "所有照片已分析完成"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Progress percentage badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (uiState.analysisPercentage == 100) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            } else {
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                            }
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${uiState.analysisPercentage}%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.analysisPercentage == 100) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondary
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Progress bar
            LinearProgressIndicator(
                progress = { uiState.analysisProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (uiState.analysisPercentage == 100) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondary
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            
            // Start/Stop button for unanalyzed photos
            if (uiState.unanalyzedPhotos > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = if (uiState.isAnalyzing) onStopAnalysis else onStartAnalysis,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isAnalyzing) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                ) {
                    Icon(
                        imageVector = if (uiState.isAnalyzing) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.isAnalyzing) {
                            "停止分析"
                        } else {
                            "开始分析 (${uiState.unanalyzedPhotos} 张待分析)"
                        }
                    )
                }
            }
        }
    }
}

/**
 * Quick stats row showing key metrics.
 */
@Composable
private fun QuickStatsRow(uiState: SmartGalleryUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatChip(
            icon = Icons.Default.Person,
            value = uiState.totalPersons.toString(),
            label = "人物",
            modifier = Modifier.weight(1f)
        )
        StatChip(
            icon = Icons.Default.Face,
            value = uiState.totalFaces.toString(),
            label = "人脸",
            modifier = Modifier.weight(1f)
        )
        StatChip(
            icon = Icons.Default.Place,
            value = uiState.photosWithGps.toString(),
            label = "有位置",
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Single stat chip.
 */
@Composable
private fun StatChip(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Section header with icon and title.
 */
@Composable
private fun FeatureSectionHeader(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Browse feature row - Timeline & Map (horizontal layout).
 */
@Composable
private fun BrowseFeatureRow(
    features: List<SmartGalleryFeature>,
    onFeatureClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        features.forEach { feature ->
            BrowseFeatureCard(
                feature = feature,
                onClick = { onFeatureClick(feature.id) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Browse feature card - larger card for timeline/map.
 */
@Composable
private fun BrowseFeatureCard(
    feature: SmartGalleryFeature,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (feature.iconName) {
        "Map" -> Icons.Default.Map
        "Timeline" -> Icons.Default.DateRange
        else -> Icons.Default.Folder
    }
    
    val iconTint = when (feature.id) {
        "timeline" -> androidx.compose.ui.graphics.Color(0xFF3B82F6) // Blue
        "map" -> androidx.compose.ui.graphics.Color(0xFFF59E0B) // Amber
        else -> MaterialTheme.colorScheme.primary
    }
    
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable(enabled = feature.isEnabled, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (feature.isEnabled) {
                iconTint.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (feature.isEnabled) iconTint.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (feature.isEnabled) iconTint else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = feature.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (feature.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = feature.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * AI Feature grid - 2x2 layout for AI features.
 */
@Composable
private fun AIFeatureGrid(
    features: List<SmartGalleryFeature>,
    onFeatureClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // First row: labels and faces
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            features.filter { it.id in listOf("labels", "faces") }.forEach { feature ->
                FeatureCard(
                    feature = feature,
                    onClick = { onFeatureClick(feature.id) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        // Second row: search and similar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            features.filter { it.id in listOf("search", "similar") }.forEach { feature ->
                FeatureCard(
                    feature = feature,
                    onClick = { onFeatureClick(feature.id) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Help tip card shown at the bottom.
 */
@Composable
private fun HelpTipCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "提示",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "首次使用需要进行 AI 分析，分析期间可正常使用其他功能。分析结果将自动保存，无需重复分析。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

/**
 * Single feature card.
 */
@Composable
private fun FeatureCard(
    feature: SmartGalleryFeature,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (feature.iconName) {
        "Face" -> Icons.Default.Face
        "Label" -> Icons.Default.Label
        "Search" -> Icons.Default.Search
        "ContentCopy" -> Icons.Default.ContentCopy
        "Map" -> Icons.Default.Map
        "Timeline" -> Icons.Default.DateRange
        else -> Icons.Default.AutoAwesome
    }
    
    Card(
        modifier = modifier
            .aspectRatio(1.2f)
            .clickable(enabled = feature.isEnabled, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (feature.isEnabled) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (feature.isEnabled) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (feature.isEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = feature.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = if (feature.isEnabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                }
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = feature.subtitle,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
