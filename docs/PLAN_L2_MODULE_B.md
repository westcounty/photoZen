# 模块B: 全屏预览界面重构 - 详细实施方案

> 父文档: [PLAN_L2_INDEX.md](./PLAN_L2_INDEX.md)
> 需求文档: [REQUIREMENTS_LISTING.md](./REQUIREMENTS_LISTING.md)
> 涉及需求: REQ-012~021 (共10个需求点)
> 依赖模块: 模块A (基础组件)
> 状态: ✅ L3实现中 (2026-01-25)

---

## 一、原始需求摘录

### REQ-012: 基础布局
```
- 隐藏app原有的顶部导航栏和底部导航栏
- 以撑满显示区域最大宽度或最大高度并保持原图片长宽比不变、无裁切的样式最大化展示图片
```

### REQ-013: 图片信息展示
```
默认在屏幕左上角（注意避开系统状态栏）展示:
- 文件名
- 照片真实时间（年月日时分秒）
- 地理位置（省份+地级市）
- 照片分辨率
- 文件大小（单位MB）
```

### REQ-014: 照片预览条
```
- 高度不要太高
- 每张照片以固定长宽比（高度:宽度=2:1）展示，居中裁切
- 当前预览照片展示在正中间，占据更宽空间
- 预览条可左右滑动，丝滑跟手，有惯性
- 滑动过程中实时将预览中的照片替换为预览条中央的照片
- 点击预览条中的照片切换到该照片
```

### REQ-015: 底部操作栏
```
默认展示5个操作:
1. 复制（保留EXIF）
2. 用其他app打开
3. 编辑（PhotoZen内置）
4. 分享到其他App
5. 彻底删除（系统删除，需二次确认）
```

### REQ-016: 显示隐藏切换
```
- 进入时默认展示图片信息及底部操作区
- 顶部始终展示当前照片序号/总照片张数
- 在照片上点击/上滑，切换显示/隐藏图片信息与底部操作区
```

### REQ-017: 双击缩放
```
- 放大倍率=1时，以双击位置为中心放大2.5倍
- 放大倍率>1时，恢复1倍
```

### REQ-018: 双指缩放
```
- 支持1-10倍倍率
- 显示倍率>1时可滑动照片
- 照片到左右边缘继续滑动，切换到前/后一张照片
```

### REQ-019: 左右滑动切换
```
- 放大倍率=1时，左右滑动切换前/后一张
```

### REQ-020: 下滑退出
```
- 在照片上向下滑或使用系统滑动返回手势退出
```

### REQ-021: 进入过渡动画
```
- 从列表点击照片进入时，从照片区域丝滑放大过渡到全屏
- 动效符合物理规律且自然
```

---

## 二、现有实现分析

### 2.1 现有组件

| 组件 | 文件位置 | 功能 | 复用情况 |
|-----|---------|------|---------|
| FullscreenPhotoViewer | `ui/components/FullscreenPhotoViewer.kt` | 单张照片查看 | 需大幅重构 |
| HorizontalPager | AlbumPhotoListScreen | 相册预览翻页 | 参考实现 |
| ComparisonGrid | LightTableScreen | 多图对比 | 不复用 |

### 2.2 功能差距分析

| 需求 | 现状 | 差距 |
|-----|------|------|
| 基础布局 | 部分实现 | 需优化撑满逻辑 |
| 图片信息展示 | 无 | 完全新增 |
| 照片预览条 | 无 | 完全新增 |
| 底部操作栏 | 部分 | 需增加操作项 |
| 显示隐藏切换 | 无 | 新增 |
| 双击缩放 | 有(1x↔2.5x) | ✅ |
| 双指缩放 | 0.5-5x | 需扩展到1-10x |
| 左右滑动切换 | 有 | ✅ |
| 下滑退出 | 无 | 新增 |
| 进入动画 | 无 | 新增(共享元素) |

---

## 三、技术方案设计

### 3.1 组件架构

```
┌─────────────────────────────────────────────────────────────────┐
│                  UnifiedFullscreenViewer                        │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │  PhotoInfoOverlay (左上角)                                  │ │
│  │  - 文件名、时间、位置、分辨率、大小                           │ │
│  └───────────────────────────────────────────────────────────┘ │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │  PhotoIndexIndicator (顶部居中，始终显示)                    │ │
│  │  - 当前序号 / 总数                                          │ │
│  └───────────────────────────────────────────────────────────┘ │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │                                                           │ │
│  │                   ZoomableImagePager                      │ │
│  │                   (主图区域)                                │ │
│  │  - HorizontalPager + Zoomable                             │ │
│  │  - 双指缩放 + 双击缩放 + 边缘切换                           │ │
│  │                                                           │ │
│  └───────────────────────────────────────────────────────────┘ │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │  BottomPreviewStrip (预览条)                               │ │
│  │  - LazyRow + 居中对齐 + 当前高亮                           │ │
│  └───────────────────────────────────────────────────────────┘ │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │  FullscreenBottomBar (底部操作栏)                          │ │
│  │  - 复制、打开、编辑、分享、删除                              │ │
│  └───────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 新增文件清单

```
ui/components/fullscreen/
├── UnifiedFullscreenViewer.kt     # 主容器组件
├── PhotoInfoOverlay.kt            # 图片信息悬浮层
├── PhotoIndexIndicator.kt         # 序号指示器
├── ZoomableImagePager.kt          # 可缩放的图片翻页器
├── BottomPreviewStrip.kt          # 底部预览条
├── FullscreenBottomBar.kt         # 底部操作栏
└── FullscreenTransition.kt        # 进入/退出过渡动画
```

### 3.3 状态管理

```kotlin
/**
 * 全屏预览状态
 */
data class FullscreenViewerState(
    val photos: List<PhotoEntity>,         // 照片列表
    val currentIndex: Int,                 // 当前索引
    val showOverlay: Boolean = true,       // 是否显示覆盖层(信息+操作栏)
    val zoomScale: Float = 1f,             // 当前缩放倍率
    val offsetX: Float = 0f,               // 平移X
    val offsetY: Float = 0f                // 平移Y
)

/**
 * 全屏预览事件
 */
sealed interface FullscreenViewerEvent {
    data class NavigateTo(val index: Int) : FullscreenViewerEvent
    data object ToggleOverlay : FullscreenViewerEvent
    data object Exit : FullscreenViewerEvent
    data class Action(val type: ActionType) : FullscreenViewerEvent
}
```

---

## 四、详细实现步骤

### Step B1: 主容器组件

**文件**: `ui/components/fullscreen/UnifiedFullscreenViewer.kt`

```kotlin
/**
 * 统一全屏预览组件
 *
 * 设计目标:
 * - 统一所有列表的全屏预览体验
 * - 支持完整的手势交互
 * - 丝滑的进入/退出动画
 *
 * @param photos 照片列表
 * @param initialIndex 初始索引
 * @param onExit 退出回调
 * @param onAction 操作回调(复制、分享等)
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun UnifiedFullscreenViewer(
    photos: List<PhotoEntity>,
    initialIndex: Int,
    onExit: () -> Unit,
    onAction: (ActionType, PhotoEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentIndex by remember { mutableIntStateOf(initialIndex) }
    var showOverlay by remember { mutableStateOf(true) }
    val currentPhoto = photos.getOrNull(currentIndex)

    // 系统UI控制 - 进入时隐藏状态栏和导航栏
    val systemUiController = rememberSystemUiController()
    DisposableEffect(Unit) {
        systemUiController.isSystemBarsVisible = false
        onDispose {
            systemUiController.isSystemBarsVisible = true
        }
    }

    // 下滑退出手势
    val dismissState = rememberDismissState(
        confirmValueChange = { value ->
            if (value == DismissValue.DismissedToEnd) {
                onExit()
                true
            } else false
        }
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { showOverlay = !showOverlay }
                )
            }
    ) {
        // 主图区域 - 可缩放翻页
        ZoomableImagePager(
            photos = photos,
            currentIndex = currentIndex,
            onIndexChange = { currentIndex = it },
            modifier = Modifier.fillMaxSize()
        )

        // 序号指示器 - 始终显示
        PhotoIndexIndicator(
            current = currentIndex + 1,
            total = photos.size,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp)
        )

        // 可隐藏的覆盖层
        AnimatedVisibility(
            visible = showOverlay,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 图片信息 - 左上角
                currentPhoto?.let { photo ->
                    PhotoInfoOverlay(
                        photo = photo,
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(start = 16.dp, top = 48.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // 预览条
                BottomPreviewStrip(
                    photos = photos,
                    currentIndex = currentIndex,
                    onIndexChange = { currentIndex = it },
                    modifier = Modifier.fillMaxWidth()
                )

                // 底部操作栏
                currentPhoto?.let { photo ->
                    FullscreenBottomBar(
                        photo = photo,
                        onAction = { onAction(it, photo) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    )
                }
            }
        }
    }
}
```

### Step B2: 图片信息悬浮层

**文件**: `ui/components/fullscreen/PhotoInfoOverlay.kt`

```kotlin
/**
 * 图片信息悬浮显示
 *
 * 显示内容:
 * - 文件名
 * - 拍摄时间（年-月-日 时:分:秒）
 * - 地理位置（省份+城市）
 * - 分辨率（宽×高）
 * - 文件大小（MB）
 */
@Composable
fun PhotoInfoOverlay(
    photo: PhotoEntity,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 文件名
        Text(
            text = photo.displayName,
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // 拍摄时间
        photo.dateTaken?.let { time ->
            InfoRow(
                icon = Icons.Default.Schedule,
                text = formatDateTime(time)
            )
        }

        // 地理位置
        photo.location?.let { location ->
            InfoRow(
                icon = Icons.Default.LocationOn,
                text = "${location.province} ${location.city}"
            )
        }

        // 分辨率
        if (photo.width > 0 && photo.height > 0) {
            InfoRow(
                icon = Icons.Default.AspectRatio,
                text = "${photo.width} × ${photo.height}"
            )
        }

        // 文件大小
        InfoRow(
            icon = Icons.Default.Storage,
            text = formatFileSize(photo.size)
        )
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = Color.White.copy(alpha = 0.7f)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}

private fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatFileSize(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return String.format("%.2f MB", mb)
}
```

### Step B3: 可缩放图片翻页器

**文件**: `ui/components/fullscreen/ZoomableImagePager.kt`

```kotlin
/**
 * 可缩放的图片翻页器
 *
 * 功能:
 * - 双指缩放(1-10倍)
 * - 双击缩放(1x↔2.5x)
 * - 缩放后平移查看
 * - 边缘穿透切换前后张
 * - 1x时左右滑动切换
 * - 下滑退出
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ZoomableImagePager(
    photos: List<PhotoEntity>,
    currentIndex: Int,
    onIndexChange: (Int) -> Unit,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(
        initialPage = currentIndex,
        pageCount = { photos.size }
    )

    // 同步外部索引变化
    LaunchedEffect(currentIndex) {
        if (pagerState.currentPage != currentIndex) {
            pagerState.animateScrollToPage(currentIndex)
        }
    }

    // 同步内部页面变化到外部
    LaunchedEffect(pagerState.currentPage) {
        onIndexChange(pagerState.currentPage)
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier,
        beyondBoundsPageCount = 1,
        key = { photos[it].id }
    ) { page ->
        val photo = photos[page]

        ZoomableImage(
            photo = photo,
            isCurrentPage = page == pagerState.currentPage,
            onSwipeToNext = {
                if (page < photos.lastIndex) {
                    onIndexChange(page + 1)
                }
            },
            onSwipeToPrevious = {
                if (page > 0) {
                    onIndexChange(page - 1)
                }
            },
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun ZoomableImage(
    photo: PhotoEntity,
    isCurrentPage: Boolean,
    onSwipeToNext: () -> Unit,
    onSwipeToPrevious: () -> Unit,
    onDismiss: (() -> Unit)?
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // 重置状态当切换页面
    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) {
            scale = 1f
            offsetX = 0f
            offsetY = 0f
        }
    }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        // 缩放限制: 1-10倍
        scale = (scale * zoomChange).coerceIn(1f, 10f)

        if (scale > 1f) {
            // 缩放状态下的平移
            offsetX += panChange.x
            offsetY += panChange.y

            // 边界检测 - 到达边缘后触发切换
            val maxOffsetX = (scale - 1) * 500 // 简化的边界计算
            if (offsetX > maxOffsetX) {
                onSwipeToPrevious()
            } else if (offsetX < -maxOffsetX) {
                onSwipeToNext()
            }
        } else {
            // 1x时检测下滑退出
            if (panChange.y > 50 && onDismiss != null) {
                onDismiss()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .transformable(state = transformState)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        // 双击缩放
                        if (scale > 1f) {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            scale = 2.5f
                            // 以点击位置为中心
                            offsetX = (size.width / 2 - tapOffset.x) * 1.5f
                            offsetY = (size.height / 2 - tapOffset.y) * 1.5f
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = photo.systemUri,
            contentDescription = photo.displayName,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                },
            contentScale = ContentScale.Fit
        )
    }
}
```

### Step B4: 底部预览条

**文件**: `ui/components/fullscreen/BottomPreviewStrip.kt`

```kotlin
/**
 * 底部照片预览条
 *
 * 特性:
 * - 固定高宽比 2:1 (高度:宽度)
 * - 当前照片居中并高亮
 * - 丝滑滑动切换
 * - 点击直接跳转
 */
@Composable
fun BottomPreviewStrip(
    photos: List<PhotoEntity>,
    currentIndex: Int,
    onIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val itemWidth = 48.dp
    val currentItemWidth = 64.dp
    val itemHeight = itemWidth * 2  // 高宽比 2:1

    // 自动滚动到当前项居中
    LaunchedEffect(currentIndex) {
        val viewportWidth = listState.layoutInfo.viewportSize.width
        val itemOffset = currentIndex * (itemWidth.value + 4) // 4dp间距
        val centerOffset = (viewportWidth / 2) - (currentItemWidth.value / 2)
        listState.animateScrollToItem(
            index = maxOf(0, currentIndex - 2),
            scrollOffset = 0
        )
    }

    // 滑动时实时更新当前照片
    val firstVisibleIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = layoutInfo.viewportStartOffset + layoutInfo.viewportSize.width / 2
            layoutInfo.visibleItemsInfo.minByOrNull {
                abs(it.offset + it.size / 2 - viewportCenter)
            }?.index ?: currentIndex
        }
    }

    LaunchedEffect(firstVisibleIndex) {
        if (listState.isScrollInProgress && firstVisibleIndex != currentIndex) {
            onIndexChange(firstVisibleIndex)
        }
    }

    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        flingBehavior = rememberSnapFlingBehavior(listState)
    ) {
        itemsIndexed(
            items = photos,
            key = { _, photo -> photo.id }
        ) { index, photo ->
            val isCurrent = index == currentIndex
            val width = if (isCurrent) currentItemWidth else itemWidth
            val height = width * 2

            Box(
                modifier = Modifier
                    .size(width = width, height = height)
                    .clip(RoundedCornerShape(4.dp))
                    .border(
                        width = if (isCurrent) 2.dp else 0.dp,
                        color = if (isCurrent) Color.White else Color.Transparent,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .clickable { onIndexChange(index) }
            ) {
                AsyncImage(
                    model = photo.systemUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}
```

### Step B5: 底部操作栏

**文件**: `ui/components/fullscreen/FullscreenBottomBar.kt`

```kotlin
/**
 * 全屏预览底部操作栏
 *
 * 操作项:
 * 1. 复制 - 保留EXIF
 * 2. 打开 - 用其他app打开
 * 3. 编辑 - PhotoZen内置编辑
 * 4. 分享 - 分享到其他App
 * 5. 删除 - 系统删除(需确认)
 */
enum class FullscreenActionType {
    COPY, OPEN_WITH, EDIT, SHARE, DELETE
}

@Composable
fun FullscreenBottomBar(
    photo: PhotoEntity,
    onAction: (FullscreenActionType) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(vertical = 12.dp, horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // 复制
        ActionButton(
            icon = Icons.Default.ContentCopy,
            label = "复制",
            onClick = { onAction(FullscreenActionType.COPY) }
        )

        // 打开
        ActionButton(
            icon = Icons.Default.OpenInNew,
            label = "打开",
            onClick = { onAction(FullscreenActionType.OPEN_WITH) }
        )

        // 编辑
        ActionButton(
            icon = Icons.Default.Edit,
            label = "编辑",
            onClick = { onAction(FullscreenActionType.EDIT) }
        )

        // 分享
        ActionButton(
            icon = Icons.Default.Share,
            label = "分享",
            onClick = { onAction(FullscreenActionType.SHARE) }
        )

        // 删除
        ActionButton(
            icon = Icons.Default.Delete,
            label = "删除",
            tint = MaterialTheme.colorScheme.error,
            onClick = { showDeleteConfirm = true }
        )
    }

    // 删除确认弹窗
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("此操作将永久删除照片，无法恢复。确定要删除吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onAction(FullscreenActionType.DELETE)
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = Color.White
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint
        )
    }
}
```

### Step B6: 进入/退出过渡动画

**文件**: `ui/components/fullscreen/FullscreenTransition.kt`

```kotlin
/**
 * 全屏预览过渡动画
 *
 * 使用 SharedTransitionLayout 实现:
 * - 从列表照片位置放大到全屏
 * - 退出时缩小回原位置
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FullscreenTransitionContainer(
    isVisible: Boolean,
    photoId: String,
    content: @Composable SharedTransitionScope.(AnimatedVisibilityScope) -> Unit
) {
    SharedTransitionLayout {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(300)) +
                    scaleIn(initialScale = 0.8f, animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)) +
                    scaleOut(targetScale = 0.8f, animationSpec = tween(300))
        ) {
            content(this)
        }
    }
}

// 在列表中的照片使用
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.PhotoThumbnailWithTransition(
    photo: PhotoEntity,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AsyncImage(
        model = photo.systemUri,
        contentDescription = photo.displayName,
        modifier = modifier
            .sharedElement(
                state = rememberSharedContentState(key = "photo-${photo.id}"),
                animatedVisibilityScope = animatedVisibilityScope
            )
            .clickable(onClick = onClick),
        contentScale = ContentScale.Crop
    )
}
```

---

## 五、集成方案

### 5.1 导航图更新

```kotlin
// NavGraph.kt
composable(
    route = "fullscreen/{photoListType}/{initialIndex}",
    arguments = listOf(
        navArgument("photoListType") { type = NavType.StringType },
        navArgument("initialIndex") { type = NavType.IntType }
    )
) { backStackEntry ->
    val photoListType = backStackEntry.arguments?.getString("photoListType")
    val initialIndex = backStackEntry.arguments?.getInt("initialIndex") ?: 0

    val photos = when (photoListType) {
        "maybe" -> maybePhotos
        "trash" -> trashPhotos
        "keep" -> keepPhotos
        "album" -> albumPhotos
        "timeline" -> timelinePhotos
        else -> emptyList()
    }

    UnifiedFullscreenViewer(
        photos = photos,
        initialIndex = initialIndex,
        onExit = { navController.popBackStack() },
        onAction = { actionType, photo ->
            // 处理操作
        }
    )
}
```

### 5.2 各列表页面集成示例

```kotlin
// 在各列表Screen中调用
DragSelectPhotoGrid(
    photos = photos,
    onPhotoClick = { photoId, index ->
        if (!isSelectionMode) {
            // 进入全屏预览
            navController.navigate("fullscreen/${listType}/$index")
        }
    },
    // ...
)
```

---

## 六、验证清单

### 6.1 需求覆盖检查

| 需求 | 描述 | 实现位置 | 状态 |
|-----|------|---------|------|
| REQ-012 | 基础布局撑满 | UnifiedFullscreenViewer.kt | ✅ 已完成 |
| REQ-013 | 图片信息5项 | PhotoInfoOverlay.kt | ✅ 已完成 |
| REQ-014 | 照片预览条 | BottomPreviewStrip.kt | ✅ 已完成 |
| REQ-015 | 底部操作栏5项 | FullscreenBottomBar.kt | ✅ 已完成 |
| REQ-016 | 显示隐藏切换 | UnifiedFullscreenViewer.kt | ✅ 已完成 |
| REQ-017 | 双击缩放 | ZoomableImagePager.kt:210-228 | ✅ 已完成 |
| REQ-018 | 双指缩放1-10x | ZoomableImagePager.kt:162-187 | ✅ 已完成 |
| REQ-019 | 左右滑动切换 | ZoomableImagePager.kt (HorizontalPager) | ✅ 已完成 |
| REQ-020 | 下滑退出 | ZoomableImagePager.kt:189-203 | ✅ 已完成 |
| REQ-021 | 进入过渡动画 | 待集成 (使用AnimatedVisibility) | ⏳ 基础完成 |

**验证结果** (2026-01-25):
```
BUILD SUCCESSFUL in 13s
18 actionable tasks: 2 executed, 16 up-to-date
```
✅ 编译通过

### 6.2 功能测试场景

| 场景 | 测试步骤 | 预期结果 |
|-----|---------|---------|
| 基础显示 | 从列表点击照片 | 全屏显示，隐藏系统UI |
| 信息展示 | 查看左上角 | 显示5项信息 |
| 预览条 | 滑动预览条 | 主图实时切换 |
| 双击放大 | 双击照片 | 放大2.5倍 |
| 双指缩放 | 双指捏合 | 支持1-10倍 |
| 边缘切换 | 缩放后滑到边缘 | 切换到下一张 |
| 显示切换 | 点击照片区域 | 信息和操作栏切换显示 |
| 下滑退出 | 向下滑动 | 退出预览 |
| 删除确认 | 点击删除 | 弹出确认框 |

---

## 七、相关文档链接

- 上级文档: [PLAN_L2_INDEX.md](./PLAN_L2_INDEX.md)
- 需求来源: [REQUIREMENTS_LISTING.md](./REQUIREMENTS_LISTING.md) REQ-012~021
- 依赖模块: [模块A](./PLAN_L2_MODULE_A.md)
- 被依赖模块: 模块C, D, E, F, G, H (所有列表页面)
