# 组件使用示例

本文档提供视觉优化方案 (Phase 1-4) 中增强组件的使用示例和最佳实践。

## 1. BottomBarConfigs

底栏按钮配置工具，提供预定义的按钮组合，确保 UI 一致性。

### 基本用法

```kotlin
// 在 Screen 中使用自适应配置
@Composable
fun PhotoListScreen(...) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        bottomBar = {
            if (uiState.isSelectionMode && uiState.selectedCount > 0) {
                // 根据选择数量自动切换单选/多选配置
                val actions = BottomBarConfigs.adaptive(
                    selectedCount = uiState.selectedCount,
                    singleSelectActions = {
                        BottomBarConfigs.keepListSingleSelect(
                            onEdit = { /* 编辑 */ },
                            onShare = { /* 分享 */ },
                            onAlbum = { /* 添加到相册 */ },
                            onMaybe = { viewModel.moveSelectedToMaybe() },
                            onTrash = { viewModel.moveSelectedToTrash() },
                            onReset = { viewModel.resetSelectedToUnsorted() }
                        )
                    },
                    multiSelectActions = {
                        BottomBarConfigs.keepListMultiSelect(
                            onAlbum = { /* 添加到相册 */ },
                            onMaybe = { viewModel.moveSelectedToMaybe() },
                            onTrash = { viewModel.moveSelectedToTrash() },
                            onReset = { viewModel.resetSelectedToUnsorted() }
                        )
                    }
                )
                
                SelectionBottomBar(actions = actions)
            }
        }
    ) { /* ... */ }
}
```

### 可用配置

| 配置方法 | 适用场景 |
|---------|---------|
| `keepListSingleSelect` | 保留列表 - 单选 |
| `keepListMultiSelect` | 保留列表 - 多选 |
| `maybeListSingleSelect` | 待定列表 - 单选 |
| `maybeListMultiSelect` | 待定列表 - 多选 |
| `trashListSingleSelect` | 回收站 - 单选 |
| `trashListMultiSelect` | 回收站 - 多选 |
| `timelineSingleSelect` | 时间线 - 单选 |
| `timelineMultiSelect` | 时间线 - 多选 |
| `lightTableCompare` | Light Table 对比模式 |
| `flowSorterListSingleSelect` | 滑动整理列表模式 - 单选 |
| `flowSorterListMultiSelect` | 滑动整理列表模式 - 多选 |

### 按状态选择配置

```kotlin
val actions = when (uiState.status) {
    PhotoStatus.KEEP -> BottomBarConfigs.adaptive(
        selectedCount = uiState.selectedCount,
        singleSelectActions = { BottomBarConfigs.keepListSingleSelect(...) },
        multiSelectActions = { BottomBarConfigs.keepListMultiSelect(...) }
    )
    PhotoStatus.MAYBE -> BottomBarConfigs.adaptive(
        selectedCount = uiState.selectedCount,
        singleSelectActions = { BottomBarConfigs.maybeListSingleSelect(...) },
        multiSelectActions = { BottomBarConfigs.maybeListMultiSelect(...) }
    )
    PhotoStatus.TRASH -> BottomBarConfigs.trashListMultiSelect(...)
    else -> emptyList()
}
```

## 2. ThumbnailSizePolicy

缩略图尺寸策略，根据使用场景优化图片加载尺寸。

### 基本用法

```kotlin
@Composable
fun PhotoGridItem(photo: PhotoEntity) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(Uri.parse(photo.systemUri))
            .withThumbnailPolicy(ThumbnailSizePolicy.Context.GRID_4_COLUMN)
            .crossfade(true)
            .build(),
        contentDescription = photo.displayName,
        contentScale = ContentScale.Crop,
        modifier = Modifier.aspectRatio(1f)
    )
}
```

### 可用场景

| 场景 | 尺寸 | 适用位置 |
|------|------|---------|
| `GRID_4_COLUMN` | 256x256 | 4列网格 |
| `GRID_3_COLUMN` | 360x360 | 3列网格 |
| `GRID_2_COLUMN` | 512x512 | 2列网格 |
| `GRID_1_COLUMN` | 800x800 | 单列/详情 |
| `CARD_PREVIEW` | 800x800 | 卡片预览 |
| `LIST_THUMBNAIL` | 200x200 | 列表缩略图 |
| `FULLSCREEN` | 原始尺寸 | 全屏查看 |

### 扩展函数

```kotlin
// 使用扩展函数
ImageRequest.Builder(context)
    .data(uri)
    .withThumbnailPolicy(ThumbnailSizePolicy.Context.CARD_PREVIEW)
    .build()

// 或直接获取尺寸
val size = ThumbnailSizePolicy.getSizeForContext(ThumbnailSizePolicy.Context.GRID_3_COLUMN)
```

## 3. PhotoPreloader

照片预加载器，提前加载即将显示的照片以提升体验。

### 滑动场景预加载

```kotlin
@HiltViewModel
class FlowSorterViewModel @Inject constructor(
    private val photoPreloader: PhotoPreloader
) : ViewModel() {
    
    fun keepPhoto(photoId: String) {
        // ... 处理滑动操作
        
        // 预加载接下来的照片
        preloadNextPhotos()
    }
    
    private fun preloadNextPhotos() {
        val currentState = uiState.value
        photoPreloader.preloadForSwipe(
            photos = currentState.photos,
            currentIndex = currentState.currentIndex,
            preloadCount = 3  // 预加载后3张
        )
    }
}
```

### 网格场景预加载

```kotlin
@Composable
fun PhotoGrid(
    photos: List<PhotoEntity>,
    columns: Int,
    photoPreloader: PhotoPreloader
) {
    val listState = rememberLazyGridState()
    
    // 监听滚动位置进行预加载
    LaunchedEffect(listState.firstVisibleItemIndex) {
        val visibleRange = listState.firstVisibleItemIndex until 
            (listState.firstVisibleItemIndex + columns * 4)
        photoPreloader.preloadForGrid(
            photos = photos,
            visibleRange = visibleRange,
            columns = columns,
            extraRows = 2  // 额外预加载2行
        )
    }
    
    LazyVerticalGrid(columns = GridCells.Fixed(columns)) {
        items(photos) { photo ->
            // ...
        }
    }
}
```

### 可用方法

| 方法 | 说明 |
|------|------|
| `preload(uri, sizeContext)` | 预加载单张照片 |
| `preloadForSwipe(photos, index, count)` | 滑动场景预加载 |
| `preloadForGrid(photos, range, columns, extraRows)` | 网格场景预加载 |
| `cancelAll()` | 取消所有预加载任务 |
| `clearCache()` | 清除预加载缓存 |

## 4. AnimationOptimizations

动画性能优化工具，使用 `graphicsLayer` 避免重组。

### 优化变换动画

```kotlin
@Composable
fun SwipeableCard(
    offsetX: Float,
    offsetY: Float,
    rotation: Float
) {
    // ❌ 旧方式：每次状态变化都会触发重组
    Box(
        modifier = Modifier
            .offset(x = offsetX.dp, y = offsetY.dp)
            .rotate(rotation)
    )
    
    // ✓ 新方式：使用 graphicsLayer 避免重组
    Box(
        modifier = Modifier.optimizedTransform(
            translationX = { offsetX },
            translationY = { offsetY },
            rotation = { rotation }
        )
    )
}
```

### 滑动变换

```kotlin
@Composable
fun SwipeablePhotoCard(
    offsetX: () -> Float,
    offsetY: () -> Float,
    rotation: () -> Float,
    scale: () -> Float
) {
    Card(
        modifier = Modifier.swipeTransform(
            offsetX = offsetX,
            offsetY = offsetY,
            rotation = rotation,
            scale = scale
        )
    ) {
        // 卡片内容
    }
}
```

### 延迟状态读取

```kotlin
@Composable
fun OptimizedItem(isSelected: Boolean) {
    // ✓ 使用 lambda 延迟读取，减少重组
    val selectedProvider = AnimationOptimizations.rememberLambdaState(isSelected)
    
    Box(
        modifier = Modifier
            .alphaTransform(alpha = { if (selectedProvider()) 1f else 0.5f })
    )
}
```

### 可用 Modifier 扩展

| 扩展 | 说明 |
|------|------|
| `optimizedTransform(...)` | 组合变换（位移、旋转、缩放、透明度） |
| `swipeTransform(...)` | 滑动变换（适用于卡片滑动） |
| `scaleTransform(...)` | 缩放变换 |
| `alphaTransform(...)` | 透明度变换 |

## 5. SelectionBottomBar

统一的选择模式底栏组件。

### 基本用法

```kotlin
@Composable
fun MyScreen() {
    Scaffold(
        bottomBar = {
            if (uiState.isSelectionMode) {
                SelectionBottomBar(
                    actions = listOf(
                        keepAction(onClick = { viewModel.keepSelected() }),
                        deleteAction(onClick = { viewModel.deleteSelected() }),
                        resetAction(onClick = { viewModel.resetSelected() })
                    )
                )
            }
        }
    )
}
```

### 预定义 Action 构建器

```kotlin
// 使用预定义构建器
val actions = listOf(
    keepAction(onClick = { /* ... */ }),
    maybeAction(onClick = { /* ... */ }),
    deleteAction(onClick = { /* ... */ }),
    permanentDeleteAction(onClick = { /* ... */ }),
    resetAction(onClick = { /* ... */ }),
    albumAction(onClick = { /* ... */ }),
    editAction(onClick = { /* ... */ }),
    shareAction(onClick = { /* ... */ }),
    moveAction(onClick = { /* ... */ }),
    copyAction(onClick = { /* ... */ }),
    filterAction(onClick = { /* ... */ })
)
```

## 6. SelectionTopBar

统一的选择模式顶栏组件。

### 基本用法

```kotlin
@Composable
fun MyScreen() {
    Scaffold(
        topBar = {
            if (uiState.isSelectionMode) {
                SelectionTopBar(
                    selectedCount = uiState.selectedCount,
                    totalCount = uiState.photos.size,
                    onClose = { viewModel.exitSelectionMode() },
                    onSelectAll = { viewModel.selectAll() },
                    onDeselectAll = { viewModel.deselectAll() }
                )
            } else {
                // 普通顶栏
            }
        }
    )
}
```

## 7. 组合使用示例

### 完整的照片列表页面

```kotlin
@Composable
fun PhotoListScreen(
    viewModel: PhotoListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // BackHandler 处理返回键退出选择模式
    BackHandler(enabled = uiState.isSelectionMode) {
        viewModel.exitSelectionMode()
    }
    
    Scaffold(
        topBar = {
            if (uiState.isSelectionMode) {
                SelectionTopBar(
                    selectedCount = uiState.selectedCount,
                    totalCount = uiState.photos.size,
                    onClose = { viewModel.exitSelectionMode() },
                    onSelectAll = { viewModel.selectAll() },
                    onDeselectAll = { viewModel.deselectAll() }
                )
            } else {
                // 普通顶栏
            }
        },
        bottomBar = {
            if (uiState.isSelectionMode && uiState.selectedCount > 0) {
                val actions = BottomBarConfigs.adaptive(
                    selectedCount = uiState.selectedCount,
                    singleSelectActions = { 
                        BottomBarConfigs.keepListSingleSelect(...) 
                    },
                    multiSelectActions = { 
                        BottomBarConfigs.keepListMultiSelect(...) 
                    }
                )
                SelectionBottomBar(actions = actions)
            }
        }
    ) { padding ->
        DragSelectPhotoGrid(
            photos = uiState.photos,
            selectedIds = uiState.selectedPhotoIds,
            onSelectionChanged = { viewModel.updateSelection(it) },
            onPhotoClick = { /* ... */ },
            onPhotoLongPress = { /* ... */ },
            columns = uiState.gridColumns,
            modifier = Modifier.padding(padding)
        )
    }
}
```

## 8. Phase 1 增强组件 - 基础组件

Phase 1 为核心基础组件添加了按压反馈和状态动画。

### 8.1 BottomActionBar (底部操作栏)

底部操作栏的每个按钮都增强了按压反馈。

**增强效果:**
- 按压缩放: `0.92f`
- 图标下沉: `2.dp`
- 背景透明度动画: 按压时增加

```kotlin
// 底部操作栏按钮已内置按压动画
@Composable
fun BottomBarActionItem(
    action: BottomBarAction,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = PicZenMotion.Springs.snappy()
    )
    val iconOffset by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 0.dp,
        animationSpec = PicZenMotion.Springs.snappy()
    )
    // ...
}
```

### 8.2 EnhancedSettingsItem (设置项)

所有设置项都添加了统一的按压反馈。

**增强效果:**
- 按压缩放: `0.98f`
- 背景色变化: 按压时加深
- 图标右移: `2.dp`

```kotlin
// 在 EnhancedSettingsItem 中自动应用
@Composable
fun EnhancedSettingsItemNavigate(...) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = PicZenMotion.Springs.snappy()
    )
    // 背景色和图标位移动画
}
```

### 8.3 FilterChipRow (筛选标签行)

筛选 Chip 添加了选中状态动画。

**增强效果:**
- 按压缩放: `0.95f`
- 边框动画: `1.dp → 2.dp` (选中时)
- 颜色过渡动画

```kotlin
@Composable
fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String
) {
    val borderWidth by animateDpAsState(
        targetValue = if (selected) 2.dp else 1.dp,
        animationSpec = PicZenMotion.Springs.snappy()
    )
    // ...
}
```

### 8.4 MainBottomNavigation (主底部导航)

底部导航添加了滑动指示器和图标弹跳效果。

**增强效果:**
- 滑动指示器: 跟随选中项平滑移动 (playful spring)
- 选中图标缩放: `1.1f`
- 未选中图标缩放: `0.85f`

```kotlin
@Composable
fun MainBottomNavigation(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    // 指示器位置动画
    val indicatorOffset by animateDpAsState(
        targetValue = calculateIndicatorOffset(currentRoute),
        animationSpec = PicZenMotion.Springs.playful()
    )

    // 图标缩放
    items.forEach { item ->
        val isSelected = item.route == currentRoute
        val iconScale by animateFloatAsState(
            targetValue = if (isSelected) 1.1f else 0.85f,
            animationSpec = PicZenMotion.Springs.playful()
        )
        // ...
    }
}
```

---

## 9. Phase 2 增强组件 - 统计和首页

Phase 2 为首页和统计组件添加了数字动画和卡片交互。

### 9.1 StatsCards (统计卡片)

统计卡片添加了数字滚动和火焰摇曳动画。

**AnimatedCounter (数字滚动)**:
```kotlin
@Composable
fun AnimatedCounter(
    targetValue: Int,
    modifier: Modifier = Modifier
) {
    val animatedValue by animateIntAsState(
        targetValue = targetValue,
        animationSpec = PicZenMotion.Specs.countUp
    )

    Text(
        text = animatedValue.toString(),
        style = MaterialTheme.typography.displayLarge
    )
}
```

**AnimatedFlameIcon (火焰摇曳)**:
```kotlin
@Composable
fun AnimatedFlameIcon(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Icon(
        imageVector = Icons.Default.LocalFireDepartment,
        modifier = modifier.graphicsLayer { rotationZ = rotation }
    )
}
```

**MiniStatsCard (迷你统计卡片)**:
- 按压缩放: `0.98f`
- 阴影动画: Level2 → Level1

### 9.2 HomeComponents (首页组件)

首页各组件都添加了按压和状态动画。

**HomeMainAction (主操作卡片)**:
```kotlin
@Composable
fun HomeMainAction(
    unsortedCount: Int,
    onStartSorting: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = PicZenMotion.Springs.snappy()
    )
    val elevation by animateDpAsState(
        targetValue = if (isPressed) Level1 else Level2,
        animationSpec = tween(PicZenMotion.Duration.Quick)
    )

    EnhancedCard(
        modifier = Modifier.graphicsLayer {
            scaleX = scale; scaleY = scale
        }
    ) {
        // 待整理数量使用 AnimatedCounter
        AnimatedCounter(targetValue = unsortedCount)
        // ...
    }
}
```

**HomeDailyTask (每日任务)**:
- 折叠/展开动画: `expandVertically` + `fadeIn`
- 完成态背景: 渐变为 `KeepGreen.Container`
- 进度条动画

**QuickActionItem (快捷入口)**:
- 按压缩放: `0.92f`
- 图标下沉: `2.dp`

---

## 10. Phase 3 增强组件 - 相册界面

Phase 3 完全重构了相册界面，采用现代卡片网格布局。

### 10.1 AlbumCard (相册卡片)

全新设计的相册卡片组件。

**设计规格:**
| 属性 | 值 | 说明 |
|------|-----|------|
| 宽高比 | 1:1 | 正方形卡片 |
| 圆角 | 16dp | 现代感 |
| 阴影 | Level2 | 适度立体 |
| 按压缩放 | 0.96f | 明显但不夸张 |

**代码示例:**
```kotlin
@Composable
fun AlbumCard(
    album: AlbumWithPhotos,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = PicZenMotion.Springs.snappy()
    )
    val elevation by animateDpAsState(
        targetValue = if (isPressed) Level1 else Level2,
        animationSpec = tween(PicZenMotion.Duration.Quick)
    )

    Card(
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(elevation, RoundedCornerShape(16.dp))
    ) {
        Box {
            // 封面图
            AsyncImage(...)

            // 暗角渐变
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f)
                            ),
                            startY = 0.5f
                        )
                    )
            )

            // 毛玻璃信息层
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(surface.copy(alpha = 0.85f))
            ) {
                Column {
                    Text(album.name)
                    Text("${album.photoCount}张")
                }
            }

            // 进度环 + 状态徽章
            AlbumProgressRing(...)
            AlbumStatusBadge(...)
        }
    }
}
```

### 10.2 AlbumGridView (相册网格视图)

双列网格布局，带错开入场动画。

```kotlin
@Composable
fun AlbumGridView(
    albums: List<AlbumWithPhotos>,
    onAlbumClick: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(albums) { index, album ->
            // 错开入场动画
            var isVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(index * 30L)  // 30ms/项
                isVisible = true
            }

            val alpha by animateFloatAsState(
                targetValue = if (isVisible) 1f else 0f,
                animationSpec = tween(PicZenMotion.Duration.Fast)
            )
            val scale by animateFloatAsState(
                targetValue = if (isVisible) 1f else 0.9f,
                animationSpec = PicZenMotion.Springs.snappy()
            )

            AlbumCard(
                album = album,
                onClick = { onAlbumClick(album.id) },
                modifier = Modifier.graphicsLayer {
                    this.alpha = alpha
                    scaleX = scale; scaleY = scale
                }
            )
        }
    }
}
```

### 10.3 AlbumProgressRing (相册进度环)

显示相册整理进度的圆环指示器。

**增强效果:**
- 进度颜色渐变: 根据进度从灰色过渡到绿色
- 完成脉冲: `1.2f` (完成时触发)

```kotlin
@Composable
fun AlbumProgressRing(
    progress: Float,  // 0f - 1f
    modifier: Modifier = Modifier
) {
    val isComplete = progress >= 1f

    // 完成脉冲
    var triggerPulse by remember { mutableStateOf(false) }
    LaunchedEffect(isComplete) {
        if (isComplete) triggerPulse = true
    }
    val pulseScale by animateFloatAsState(
        targetValue = if (triggerPulse) 1.2f else 1f,
        animationSpec = PicZenMotion.Springs.playful(),
        finishedListener = { triggerPulse = false }
    )

    // 颜色渐变
    val ringColor by animateColorAsState(
        targetValue = when {
            progress < 0.3f -> Color.Gray
            progress < 0.7f -> MaybeAmber
            else -> KeepGreen
        },
        animationSpec = tween(PicZenMotion.Duration.Normal)
    )

    Box(
        modifier = modifier.graphicsLayer {
            scaleX = pulseScale; scaleY = pulseScale
        }
    ) {
        CircularProgressIndicator(
            progress = { progress },
            color = ringColor
        )
    }
}
```

### 10.4 AlbumStatusBadge (相册状态徽章)

显示相册整理状态的徽章。

**三种状态:**
- `NOT_STARTED`: 灰色，未开始图标
- `IN_PROGRESS`: 琥珀色，进行中图标
- `COMPLETED`: 绿色，完成勾选图标

```kotlin
enum class AlbumStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED
}

@Composable
fun AlbumStatusBadge(
    status: AlbumStatus,
    modifier: Modifier = Modifier
) {
    val (icon, color) = when (status) {
        AlbumStatus.NOT_STARTED -> Icons.Outlined.Circle to Color.Gray
        AlbumStatus.IN_PROGRESS -> Icons.Default.Schedule to MaybeAmber
        AlbumStatus.COMPLETED -> Icons.Default.CheckCircle to KeepGreen
    }

    // 颜色过渡
    val animatedColor by animateColorAsState(
        targetValue = color,
        animationSpec = tween(PicZenMotion.Duration.Normal)
    )

    Surface(
        color = animatedColor.copy(alpha = 0.2f),
        shape = CircleShape
    ) {
        Icon(
            imageVector = icon,
            tint = animatedColor,
            modifier = Modifier.padding(4.dp)
        )
    }
}
```

---

## 11. Phase 4 增强组件 - 细节打磨

Phase 4 为多个组件添加了微交互动画，确保极致流畅的用户体验。

### 11.1 按压动画增强

以下组件已内置按压缩放动画，无需额外配置：

| 组件 | 按压缩放 | 附加效果 |
|------|----------|----------|
| `SelectionTopBar` 关闭按钮 | 0.85f | 旋转90° |
| `PhotoActionSheet` 列表项 | 0.98f | 图标右移2dp + 背景色 |
| `AlbumPickerBottomSheet` 网格项 | 0.96f | 选中1.02f + 勾选弹入 |
| `FilterBottomSheet` 按钮 | 0.97f | - |
| `ConfirmDeleteSheet` 危险按钮 | 0.95f (按压时暂停脉冲) | 脉冲1.02f + 红色光晕 |
| `DateRangePicker` Chip | 0.95f | 触觉反馈 |
| `CalendarHeatmap` 单元格 | 0.9f | 颜色过渡 |
| `TimelineEventPhotoRow` 照片 | 0.95f | 阴影动画 + 错开入场 |

### 11.2 状态动画增强

| 组件 | 状态动画效果 |
|------|--------------|
| `SelectionTopBar` | 选中数量变化时脉冲1.15f |
| `PhotoStatusBadge` | 状态切换脉冲1.2f + 图标AnimatedContent + 颜色过渡 |
| `GuideTooltip` | 呼吸脉冲1.02f + 箭头移动±4dp |

### 11.3 入场动画增强

| 组件 | 错开延迟 | 动画效果 |
|------|----------|----------|
| `PhotoActionSheet` | 50ms/项 | 淡入 + 从右滑入30dp |
| `TimelineEventPhotoRow` | 30ms/项 | 淡入 + 从右滑入30dp |
| `GuideTooltip` | - | 缩放0.8→1 (playful spring) |

---

## 12. 最佳实践

### DO ✓

1. **使用 BottomBarConfigs 而非手动构建按钮** - 保证 UI 一致性
2. **根据列数选择正确的 ThumbnailSizePolicy** - 优化内存使用
3. **在滑动操作后调用 preload** - 提升用户体验
4. **使用 optimizedTransform 进行动画** - 避免不必要的重组
5. **使用 PicZenMotion.Springs.snappy() 做按压动画** - 确保丝滑反馈
6. **使用 graphicsLayer 进行变换** - GPU加速避免重组
7. **为可点击组件添加按压缩放** - 提供触觉反馈

### DON'T ✗

1. **不要硬编码缩略图尺寸** - 使用 ThumbnailSizePolicy
2. **不要使用 offset/rotate 进行动画** - 使用 graphicsLayer 系列
3. **不要在每帧重新创建 ImageRequest** - 缓存或使用 remember
4. **不要忽略 BackHandler** - 确保选择模式可正常退出
5. **不要使用 tween 做按压动画** - 使用 Spring 更流畅
6. **不要硬编码动画时长** - 使用 PicZenMotion.Duration
