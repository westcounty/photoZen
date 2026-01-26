package com.example.photozen.ui.components.fullscreen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.photozen.data.local.entity.PhotoEntity

/**
 * PhotoZen 统一全屏预览组件 (REQ-012 ~ REQ-021)
 * =============================================
 *
 * 设计目标:
 * - 统一所有列表的全屏预览体验
 * - 支持完整的手势交互
 * - 丝滑的进入/退出动画
 *
 * 组件架构:
 * ```
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  PhotoIndexIndicator (顶部居中，始终显示)                         │
 * ├─────────────────────────────────────────────────────────────────┤
 * │  PhotoInfoOverlay (左上角，可隐藏)                                │
 * │                                                                 │
 * │                   ZoomableImagePager                            │
 * │                   (主图区域)                                     │
 * │                                                                 │
 * ├─────────────────────────────────────────────────────────────────┤
 * │  BottomPreviewStrip (预览条，可隐藏)                              │
 * ├─────────────────────────────────────────────────────────────────┤
 * │  FullscreenBottomBar (底部操作栏，可隐藏)                         │
 * └─────────────────────────────────────────────────────────────────┘
 * ```
 *
 * @param photos 照片列表
 * @param initialIndex 初始索引
 * @param onExit 退出回调
 * @param onAction 操作回调 (复制、分享等)
 * @param overlayContent 可选的覆盖层内容，如删除确认面板等
 * @param showPhotoInfo 是否显示照片信息（左上角），默认 true
 * @param showBottomBar 是否显示底部操作栏，默认 true
 * @param modifier Modifier
 */
@Composable
fun UnifiedFullscreenViewer(
    photos: List<PhotoEntity>,
    initialIndex: Int,
    onExit: () -> Unit,
    onAction: (FullscreenActionType, PhotoEntity) -> Unit,
    overlayContent: @Composable (() -> Unit)? = null,
    showPhotoInfo: Boolean = true,
    showBottomBar: Boolean = true,
    modifier: Modifier = Modifier
) {
    // 当前照片索引 - 使用 initialIndex 作为 key，确保每次打开时都能正确定位
    var currentIndex by remember(initialIndex) {
        mutableIntStateOf(initialIndex.coerceIn(0, (photos.size - 1).coerceAtLeast(0)))
    }

    // 覆盖层显示状态 (REQ-016: 点击切换显示/隐藏)
    var showOverlay by remember { mutableStateOf(true) }

    // 当前照片
    val currentPhoto = photos.getOrNull(currentIndex)

    // DES-037: 进入动画状态
    var isEntering by remember { mutableStateOf(true) }

    // DES-037: 进入动画 - 从略小缩放到正常大小
    val enterScale by animateFloatAsState(
        targetValue = if (isEntering) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "enterScale"
    )

    // DES-037: 进入透明度动画
    val enterAlpha by animateFloatAsState(
        targetValue = if (isEntering) 0f else 1f,
        animationSpec = tween(200),
        label = "enterAlpha"
    )

    // 启动进入动画
    LaunchedEffect(Unit) {
        isEntering = false
    }

    // 系统UI控制 - 隐藏状态栏和导航栏 (REQ-012)
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? android.app.Activity)?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        onDispose {
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // 返回键处理
    BackHandler {
        onExit()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 主图区域 - 可缩放翻页 (REQ-017, REQ-018, REQ-019, REQ-020)
        // DES-037: 应用进入动画效果
        ZoomableImagePager(
            photos = photos,
            currentIndex = currentIndex,
            onIndexChange = { currentIndex = it },
            onDismiss = onExit,
            onTap = {
                // REQ-016: 点击切换显示/隐藏覆盖层
                showOverlay = !showOverlay
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = enterScale
                    scaleY = enterScale
                    alpha = enterAlpha
                }
        )

        // 序号指示器 - 始终显示 (REQ-016)
        // 增加顶部间距避免与状态栏区域重叠
        PhotoIndexIndicator(
            current = currentIndex + 1,
            total = photos.size,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 24.dp)
        )

        // 可隐藏的覆盖层 (REQ-016) - 仅在 showPhotoInfo 为 true 时显示
        if (showPhotoInfo) {
            AnimatedVisibility(
                visible = showOverlay,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // 图片信息 - 左上角 (REQ-013)
                    currentPhoto?.let { photo ->
                        PhotoInfoOverlay(
                            photo = photo,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .statusBarsPadding()
                                .padding(start = 16.dp, top = 56.dp) // 避开序号指示器
                        )
                    }
                }
            }
        }

        // 底部区域 (预览条 + 操作栏)
        AnimatedVisibility(
            visible = showOverlay,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 预览条 (REQ-014) - 始终显示
                BottomPreviewStrip(
                    photos = photos,
                    currentIndex = currentIndex,
                    onIndexChange = { currentIndex = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            // 如果不显示底部操作栏，预览条需要自己处理导航栏间距
                            if (!showBottomBar) Modifier.navigationBarsPadding() else Modifier
                        )
                )

                // 底部操作栏 (REQ-015) - 仅在 showBottomBar 为 true 时显示
                if (showBottomBar) {
                    currentPhoto?.let { photo ->
                        FullscreenBottomBar(
                            photo = photo,
                            onAction = { actionType ->
                                onAction(actionType, photo)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                        )
                    }
                }
            }
        }

        // 可选的覆盖层内容（如删除确认面板）
        overlayContent?.invoke()
    }
}

/**
 * 简化版全屏预览 - 仅展示单张照片，无预览条
 *
 * 适用场景:
 * - 快速预览单张照片
 * - 比较模式中的照片详情
 *
 * @param photo 照片
 * @param onExit 退出回调
 * @param onAction 操作回调
 * @param modifier Modifier
 */
@Composable
fun SinglePhotoViewer(
    photo: PhotoEntity,
    onExit: () -> Unit,
    onAction: (FullscreenActionType, PhotoEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    UnifiedFullscreenViewer(
        photos = listOf(photo),
        initialIndex = 0,
        onExit = onExit,
        onAction = onAction,
        modifier = modifier
    )
}
