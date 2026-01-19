# 组件使用示例

本文档提供 Phase 4 新增组件的使用示例和最佳实践。

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

## 8. 最佳实践

### DO ✓

1. **使用 BottomBarConfigs 而非手动构建按钮** - 保证 UI 一致性
2. **根据列数选择正确的 ThumbnailSizePolicy** - 优化内存使用
3. **在滑动操作后调用 preload** - 提升用户体验
4. **使用 optimizedTransform 进行动画** - 避免不必要的重组

### DON'T ✗

1. **不要硬编码缩略图尺寸** - 使用 ThumbnailSizePolicy
2. **不要使用 offset/rotate 进行动画** - 使用 graphicsLayer 系列
3. **不要在每帧重新创建 ImageRequest** - 缓存或使用 remember
4. **不要忽略 BackHandler** - 确保选择模式可正常退出
