package com.example.photozen.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.SwipeRight
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.photozen.data.repository.PhotoClassificationMode
import com.example.photozen.ui.theme.KeepGreen
import com.example.photozen.ui.theme.MaybeAmber
import com.example.photozen.ui.theme.TrashRed
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * Quick Start Guide Version
 * Update this version when you want to show the quick start guide again to all users.
 * For example, after a major feature update that requires re-onboarding.
 */
const val QUICK_START_GUIDE_VERSION = "1.0"

/**
 * Quick Start Guide Bottom Sheet
 * A 3-step onboarding flow for new users or after major updates.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickStartSheet(
    onComplete: (
        dailyTaskEnabled: Boolean,
        dailyTaskTarget: Int,
        swipeSensitivity: Float,
        classificationMode: PhotoClassificationMode
    ) -> Unit,
    onDismiss: () -> Unit,
    initialDailyTaskEnabled: Boolean = true,
    initialDailyTaskTarget: Int = 100,
    initialSwipeSensitivity: Float = 1.0f,
    initialClassificationMode: PhotoClassificationMode = PhotoClassificationMode.ALBUM,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    
    // Current step (0-indexed)
    var currentStep by remember { mutableIntStateOf(0) }
    
    // Settings state
    var dailyTaskEnabled by remember { mutableStateOf(initialDailyTaskEnabled) }
    var dailyTaskTarget by remember { mutableIntStateOf(initialDailyTaskTarget) }
    var swipeSensitivity by remember { mutableFloatStateOf(initialSwipeSensitivity) }
    var classificationMode by remember { mutableStateOf(initialClassificationMode) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 32.dp)
        ) {
            // Header with close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "快速上手",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Step indicator
            StepIndicator(
                currentStep = currentStep,
                totalSteps = 3,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            
            // Animated content for steps
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } + fadeIn() togetherWith
                                slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith
                                slideOutHorizontally { it } + fadeOut()
                    }
                },
                label = "step_transition"
            ) { step ->
                when (step) {
                    0 -> DailyTaskStep(
                        enabled = dailyTaskEnabled,
                        onEnabledChange = { dailyTaskEnabled = it },
                        target = dailyTaskTarget,
                        onTargetChange = { dailyTaskTarget = it }
                    )
                    1 -> SwipeSensitivityStep(
                        sensitivity = swipeSensitivity,
                        onSensitivityChange = { swipeSensitivity = it }
                    )
                    2 -> ClassificationModeStep(
                        mode = classificationMode,
                        onModeChange = { classificationMode = it }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (currentStep > 0) {
                    OutlinedButton(
                        onClick = { currentStep-- }
                    ) {
                        Text("上一步")
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
                
                Button(
                    onClick = {
                        if (currentStep < 2) {
                            currentStep++
                        } else {
                            scope.launch {
                                sheetState.hide()
                                onComplete(
                                    dailyTaskEnabled,
                                    dailyTaskTarget,
                                    swipeSensitivity,
                                    classificationMode
                                )
                            }
                        }
                    }
                ) {
                    Text(if (currentStep < 2) "下一步" else "完成设置")
                    if (currentStep < 2) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Step indicator showing progress through the onboarding flow.
 */
@Composable
private fun StepIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalSteps) { index ->
            val isActive = index <= currentStep
            Box(
                modifier = Modifier
                    .size(if (index == currentStep) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
            )
            if (index < totalSteps - 1) {
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

/**
 * Step 1: Daily Task Setup
 */
@Composable
private fun DailyTaskStep(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    target: Int,
    onTargetChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Icon and title
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Assignment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "每日任务",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "化整为零，每天整理一点",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Description card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "📱 每日任务可以帮助你：",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• 把几万张照片的大任务拆解成每天的小目标\n• 养成每天整理的好习惯，积少成多\n• 支持小部件，在桌面上查看进度",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Enable switch
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "开启每日任务",
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange
                )
            }
        }
        
        // Target selection
        AnimatedVisibility(visible = enabled) {
            Column {
                Text(
                    text = "每日目标数量",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(10, 50, 100, 200).forEach { value ->
                        val isSelected = target == value
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onTargetChange(value) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) 
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = if (isSelected) 
                                androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            else null
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = value.toString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) 
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Step 2: Swipe Sensitivity with Virtual Card
 */
@Composable
private fun SwipeSensitivityStep(
    sensitivity: Float,
    onSensitivityChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Icon and title
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SwipeRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "滑动灵敏度",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "调整手感，找到最舒适的操作方式",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Sensitivity slider
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "灵敏",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "迟钝",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Slider(
                    value = sensitivity,
                    onValueChange = onSensitivityChange,
                    valueRange = 0.5f..1.5f,
                    steps = 9,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
                Text(
                    text = "当前: ${String.format("%.1f", sensitivity)}x",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
        
        // Virtual card test area
        Text(
            text = "试试看：拖动下方卡片体验滑动效果",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        
        VirtualSwipeCard(sensitivity = sensitivity)
    }
}

/**
 * Virtual swipe card for testing sensitivity.
 */
@Composable
private fun VirtualSwipeCard(
    sensitivity: Float
) {
    val haptic = LocalHapticFeedback.current
    
    // Calculate thresholds based on sensitivity
    val baseThreshold = 120.dp
    val rightThreshold = baseThreshold * sensitivity
    val leftThreshold = baseThreshold * sensitivity * 0.9f
    val upThreshold = baseThreshold * sensitivity * 0.8f
    val downThreshold = baseThreshold * sensitivity * 0.7f
    
    var cardIndex by remember { mutableIntStateOf(0) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var hasTriggeredFeedback by remember { mutableStateOf(false) }
    
    // Determine current swipe direction and if threshold is met
    val swipeDirection = remember(offsetX, offsetY) {
        when {
            offsetX.absoluteValue > offsetY.absoluteValue -> {
                if (offsetX > 0) SwipeDirection.RIGHT else SwipeDirection.LEFT
            }
            offsetY < 0 -> SwipeDirection.UP
            offsetY > 0 -> SwipeDirection.DOWN
            else -> null
        }
    }
    
    val thresholdMet = remember(offsetX, offsetY, sensitivity) {
        val rightThresholdPx = rightThreshold.value
        val leftThresholdPx = leftThreshold.value
        val upThresholdPx = upThreshold.value
        val downThresholdPx = downThreshold.value
        
        when (swipeDirection) {
            SwipeDirection.RIGHT -> offsetX >= rightThresholdPx
            SwipeDirection.LEFT -> offsetX <= -leftThresholdPx
            SwipeDirection.UP -> offsetY <= -upThresholdPx
            SwipeDirection.DOWN -> offsetY >= downThresholdPx
            null -> false
        }
    }
    
    // Trigger haptic feedback when threshold is first met
    if (thresholdMet && !hasTriggeredFeedback) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        hasTriggeredFeedback = true
    } else if (!thresholdMet) {
        hasTriggeredFeedback = false
    }
    
    val gradientColors = listOf(
        Color(0xFF667EEA),
        Color(0xFF764BA2),
        Color(0xFFF093FB)
    )
    val gradientColorsList = listOf(
        gradientColors,
        listOf(Color(0xFFf093fb), Color(0xFFf5576c)),
        listOf(Color(0xFF4facfe), Color(0xFF00f2fe)),
        listOf(Color(0xFF43e97b), Color(0xFF38f9d7))
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        // Virtual card
        Box(
            modifier = Modifier
                .size(140.dp, 180.dp)
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .graphicsLayer {
                    rotationZ = offsetX / 20f
                }
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        colors = gradientColorsList[cardIndex % gradientColorsList.size]
                    )
                )
                .pointerInput(sensitivity) {
                    detectDragGestures(
                        onDragStart = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDragEnd = {
                            if (thresholdMet) {
                                // Card was swiped - show next card
                                cardIndex++
                            }
                            offsetX = 0f
                            offsetY = 0f
                            hasTriggeredFeedback = false
                        },
                        onDragCancel = {
                            offsetX = 0f
                            offsetY = 0f
                            hasTriggeredFeedback = false
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Direction indicator
            val icon: ImageVector?
            val iconTint: Color
            
            when {
                swipeDirection == SwipeDirection.RIGHT -> {
                    icon = if (thresholdMet) Icons.Default.Favorite else Icons.Default.FavoriteBorder
                    iconTint = KeepGreen
                }
                swipeDirection == SwipeDirection.LEFT -> {
                    icon = if (thresholdMet) Icons.Default.QuestionMark else Icons.Default.HelpOutline
                    iconTint = MaybeAmber
                }
                swipeDirection == SwipeDirection.UP -> {
                    icon = if (thresholdMet) Icons.Default.Delete else Icons.Default.DeleteOutline
                    iconTint = TrashRed
                }
                swipeDirection == SwipeDirection.DOWN -> {
                    icon = if (thresholdMet) Icons.Default.Delete else Icons.Default.DeleteOutline
                    iconTint = TrashRed
                }
                else -> {
                    icon = null
                    iconTint = Color.White
                }
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(48.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "拖动我",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
        
        // Direction hints
        Text(
            text = "👉 保留",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
        )
        Text(
            text = "待定 👈",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
        )
        Text(
            text = "删除 ⬆️",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
        )
        Text(
            text = "⬇️ 删除",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        )
    }
}

private enum class SwipeDirection {
    LEFT, RIGHT, UP, DOWN
}

/**
 * Step 3: Classification Mode Selection
 */
@Composable
private fun ClassificationModeStep(
    mode: PhotoClassificationMode,
    onModeChange: (PhotoClassificationMode) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Icon and title
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Collections,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "分类方式",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "选择适合你的照片整理方式",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Mode cards
        ModeSelectionCard(
            title = "标签模式",
            description = "使用标签灵活分类照片，一张照片可以有多个标签",
            icon = Icons.Default.Sell,
            iconTint = Color(0xFFA78BFA),
            isSelected = mode == PhotoClassificationMode.TAG,
            onClick = { onModeChange(PhotoClassificationMode.TAG) },
            features = listOf(
                "适合需要多维度分类的场景",
                "照片可以同时属于多个分类",
                "标签可以随时编辑和管理"
            )
        )
        
        ModeSelectionCard(
            title = "相册模式",
            description = "直接将照片分类到系统相册，整理后即可在相册应用查看",
            icon = Icons.Default.Collections,
            iconTint = Color(0xFF4FC3F7),
            isSelected = mode == PhotoClassificationMode.ALBUM,
            onClick = { onModeChange(PhotoClassificationMode.ALBUM) },
            features = listOf(
                "与系统相册无缝衔接",
                "整理后直接可用，无需导出",
                "支持移动或复制到相册"
            )
        )
    }
}

/**
 * Mode selection card component.
 */
@Composable
private fun ModeSelectionCard(
    title: String,
    description: String,
    icon: ImageVector,
    iconTint: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    features: List<String>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                iconTint.copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(2.dp, iconTint)
        else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(iconTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            AnimatedVisibility(visible = isSelected) {
                Column(
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    features.forEach { feature ->
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(iconTint)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = feature,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
