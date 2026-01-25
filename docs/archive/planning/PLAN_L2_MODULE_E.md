# 模块E: 已保留照片列表 - 详细实施方案

> 父文档: [PLAN_L2_INDEX.md](./PLAN_L2_INDEX.md)
> 需求文档: [REQUIREMENTS_LISTING.md](./REQUIREMENTS_LISTING.md)
> 涉及需求: REQ-037~043 (共7个需求点)
> 依赖模块: 模块A, 模块B
> 状态: 📝 规划中

---

## 一、原始需求摘录

### REQ-037: 排序选项
```
- 右上角: 排序按钮、视图模式切换按钮、已分类至相册照片过滤开关
- 排序选项: 照片真实时间正序/倒序、添加至已保留时间正序/倒序、随机排序
- 默认: 照片真实时间倒序
```

### REQ-038: 已分类过滤开关
```
- 过滤已分类至相册的照片
```

### REQ-039: 点击进入全屏预览
```
- 点击照片进入全屏预览
- 可滑动切换范围: 当前展示的全部照片(遵从过滤条件)
```

### REQ-040: 长按进入选择模式
```
- 长按选中并进入选择模式
- 网格视图: 拖动批量选中
- 瀑布流视图: 拖动无多选效果
- 系统返回手势退出选择模式
```

### REQ-041: 底部操作(5项)
```
- 添加到相册、设置为待定、移至回收站、重置为未筛选、彻底删除
```

### REQ-042: 添加到相册弹窗
```
- 展示全部相册列表
- 照片所属相册置灰并提示"这是当前所在相册"
- 提供添加相册按钮，点击后打开"管理快捷相册列表"弹窗
- 弹窗保存后，可选相册列表立即更新
```

### REQ-043: 快速分类模块
```
- 展示在顶部导航栏下方
- 保持当前逻辑不变
```

---

## 二、现有实现分析

### 2.1 现有实现状态

| 功能 | 状态 | 备注 |
|-----|------|------|
| 基础列表 | ✅ PhotoListScreen(status=KEEP) | |
| 排序(3种) | ✅ 时间+随机 | 需增加添加时间排序 |
| 视图切换 | ✅ | |
| 已分类过滤 | ❌ | 需新增 |
| 点击全屏预览 | ❌ | 需集成模块B |
| 长按选择模式 | ✅ | |
| 底部操作 | 部分 | 需补全5项 |
| 添加到相册弹窗 | 部分 | 需优化 |
| 快速分类模块 | ✅ FloatingAlbumTags | |

### 2.2 关键文件

```
ui/screens/photolist/
├── PhotoListScreen.kt         # status=KEEP 时的主界面
├── PhotoListViewModel.kt      # 状态管理
└── components/
    └── FloatingAlbumTags.kt   # 快速分类组件

ui/components/
└── AddToAlbumDialog.kt        # 添加到相册弹窗(需新增/优化)
```

---

## 三、技术方案设计

### 3.1 已分类过滤功能

```kotlin
// PhotoListViewModel.kt 新增
data class KeepListFilters(
    val excludeClassified: Boolean = false  // 排除已分类到相册的照片
)

class PhotoListViewModel {
    private val _filters = MutableStateFlow(KeepListFilters())

    // 组合过滤后的照片列表
    val filteredPhotos: StateFlow<List<PhotoEntity>> = combine(
        allKeepPhotos,
        _filters
    ) { photos, filters ->
        if (filters.excludeClassified) {
            photos.filter { it.albumIds.isEmpty() }
        } else {
            photos
        }
    }.stateIn(...)

    fun toggleClassifiedFilter() {
        _filters.update { it.copy(excludeClassified = !it.excludeClassified) }
    }
}
```

### 3.2 顶部操作栏更新

```kotlin
@Composable
fun KeepListTopBar(
    currentSort: PhotoListSortOrder,
    onSortChange: (PhotoListSortOrder) -> Unit,
    gridMode: PhotoGridMode,
    onGridModeToggle: () -> Unit,
    excludeClassified: Boolean,
    onFilterToggle: () -> Unit
) {
    TopAppBar(
        title = { Text("已保留") },
        actions = {
            // 排序按钮
            SortDropdownButton(
                currentSort = currentSort,
                options = ListSortConfigs.keepList,
                onSortSelected = onSortChange
            )

            // 视图模式切换
            IconButton(onClick = onGridModeToggle) {
                Icon(
                    imageVector = if (gridMode == PhotoGridMode.SQUARE)
                        Icons.Default.GridView else Icons.Default.ViewStream,
                    contentDescription = "切换视图"
                )
            }

            // 已分类过滤开关
            IconButton(onClick = onFilterToggle) {
                Icon(
                    imageVector = if (excludeClassified)
                        Icons.Default.FilterAlt else Icons.Default.FilterAltOff,
                    contentDescription = if (excludeClassified)
                        "显示全部" else "隐藏已分类",
                    tint = if (excludeClassified)
                        MaterialTheme.colorScheme.primary else LocalContentColor.current
                )
            }
        }
    )
}
```

### 3.3 底部操作栏配置

```kotlin
// BottomBarConfigs.kt 更新
object BottomBarConfigs {
    fun keepListActions(
        onAddToAlbum: () -> Unit,
        onMaybe: () -> Unit,
        onTrash: () -> Unit,
        onReset: () -> Unit,
        onDelete: () -> Unit
    ): List<BottomBarAction> = listOf(
        BottomBarAction(
            icon = Icons.Default.PhotoAlbum,
            label = "加相册",
            onClick = onAddToAlbum
        ),
        BottomBarAction(
            icon = Icons.Default.HelpOutline,
            label = "待定",
            onClick = onMaybe,
            tint = MaybeAmber
        ),
        BottomBarAction(
            icon = Icons.Default.Delete,
            label = "回收站",
            onClick = onTrash,
            tint = TrashRed
        ),
        BottomBarAction(
            icon = Icons.Default.Refresh,
            label = "重置",
            onClick = onReset
        ),
        BottomBarAction(
            icon = Icons.Default.DeleteForever,
            label = "彻删",
            onClick = onDelete,
            tint = TrashRed
        )
    )
}
```

### 3.4 添加到相册弹窗优化

```kotlin
/**
 * 添加到相册弹窗
 *
 * 功能:
 * - 显示所有相册列表
 * - 已包含照片的相册置灰
 * - 底部"管理相册"入口
 */
@Composable
fun AddToAlbumDialog(
    photoIds: List<String>,
    albums: List<AlbumEntity>,
    photoAlbumIds: Set<String>,  // 照片已属于的相册
    onAlbumSelected: (String) -> Unit,
    onManageAlbums: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加到相册") },
        text = {
            Column {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    items(albums) { album ->
                        val isCurrentAlbum = album.id in photoAlbumIds
                        AlbumListItem(
                            album = album,
                            enabled = !isCurrentAlbum,
                            hint = if (isCurrentAlbum) "已在此相册" else null,
                            onClick = { onAlbumSelected(album.id) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 管理相册按钮
                OutlinedButton(
                    onClick = onManageAlbums,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Settings, null)
                    Spacer(Modifier.width(8.dp))
                    Text("管理快捷相册列表")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun AlbumListItem(
    album: AlbumEntity,
    enabled: Boolean,
    hint: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp)
            .alpha(if (enabled) 1f else 0.5f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 相册封面缩略图
        AsyncImage(
            model = album.coverUri,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.name,
                style = MaterialTheme.typography.bodyLarge
            )
            hint?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        if (enabled) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}
```

---

## 四、详细实现步骤

### Step E1: 排序选项扩展

**文件**: `ui/screens/photolist/PhotoListViewModel.kt`

```kotlin
// 1. 已保留列表排序配置
val keepListSortOptions = listOf(
    PhotoListSortOrder.DATE_DESC,
    PhotoListSortOrder.DATE_ASC,
    PhotoListSortOrder.ADDED_DESC,
    PhotoListSortOrder.ADDED_ASC,
    PhotoListSortOrder.RANDOM
)

// 2. 需要在 PhotoEntity 中添加 addedToStatusAt 字段(如果没有)
// data class PhotoEntity(
//     ...
//     val addedToStatusAt: Long? = null  // 添加到当前状态的时间戳
// )
```

### Step E2: 已分类过滤开关

**文件**: `ui/screens/photolist/PhotoListScreen.kt`

```kotlin
// 在 TopBar 添加过滤按钮
@Composable
fun KeepListScreen() {
    val viewModel: PhotoListViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            KeepListTopBar(
                currentSort = uiState.sortOrder,
                onSortChange = viewModel::setSortOrder,
                gridMode = uiState.gridMode,
                onGridModeToggle = viewModel::toggleGridMode,
                excludeClassified = uiState.filters.excludeClassified,
                onFilterToggle = viewModel::toggleClassifiedFilter
            )
        }
    ) { padding ->
        // 使用过滤后的照片列表
        val photos = uiState.filteredPhotos

        DragSelectPhotoGrid(
            photos = photos,
            // ...
        )
    }
}
```

### Step E3: 全屏预览集成

**文件**: `ui/screens/photolist/PhotoListScreen.kt`

```kotlin
// 集成全屏预览
@Composable
fun KeepListScreen() {
    var showFullscreen by remember { mutableStateOf(false) }
    var fullscreenIndex by remember { mutableIntStateOf(0) }

    if (showFullscreen) {
        UnifiedFullscreenViewer(
            photos = uiState.filteredPhotos,  // 使用过滤后的列表
            initialIndex = fullscreenIndex,
            onExit = { showFullscreen = false },
            onAction = { actionType, photo ->
                when (actionType) {
                    FullscreenActionType.COPY -> viewModel.copyPhoto(photo.id)
                    FullscreenActionType.SHARE -> viewModel.sharePhoto(photo.id)
                    FullscreenActionType.EDIT -> navController.navigate("edit/${photo.id}")
                    FullscreenActionType.DELETE -> viewModel.permanentDelete(photo.id)
                    FullscreenActionType.OPEN_WITH -> viewModel.openWith(photo.id)
                }
            }
        )
    } else {
        // 列表视图
        DragSelectPhotoGrid(
            photos = uiState.filteredPhotos,
            onPhotoClick = { photoId, index ->
                if (!uiState.isSelectionMode) {
                    fullscreenIndex = index
                    showFullscreen = true
                } else {
                    viewModel.toggleSelection(photoId)
                }
            },
            // ...
        )
    }
}
```

### Step E4: 底部操作栏和弹窗

**文件**: `ui/screens/photolist/PhotoListScreen.kt`

```kotlin
// 底部操作栏
if (uiState.isSelectionMode) {
    var showAddToAlbum by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    SelectionBottomBar(
        actions = BottomBarConfigs.keepListActions(
            onAddToAlbum = { showAddToAlbum = true },
            onMaybe = { viewModel.moveSelectedToStatus(PhotoStatus.MAYBE) },
            onTrash = { viewModel.moveSelectedToStatus(PhotoStatus.TRASH) },
            onReset = { viewModel.moveSelectedToStatus(PhotoStatus.UNSORTED) },
            onDelete = { showDeleteConfirm = true }
        )
    )

    // 添加到相册弹窗
    if (showAddToAlbum) {
        AddToAlbumDialog(
            photoIds = uiState.selectedPhotoIds.toList(),
            albums = uiState.albums,
            photoAlbumIds = uiState.selectedPhotosAlbumIds,
            onAlbumSelected = { albumId ->
                viewModel.addSelectedToAlbum(albumId)
                showAddToAlbum = false
            },
            onManageAlbums = {
                showAddToAlbum = false
                navController.navigate("manage_albums")
            },
            onDismiss = { showAddToAlbum = false }
        )
    }

    // 删除确认
    if (showDeleteConfirm) {
        DeleteConfirmDialog(
            count = uiState.selectedCount,
            onConfirm = {
                viewModel.permanentDeleteSelected()
                showDeleteConfirm = false
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}
```

---

## 五、验证清单

### 需求覆盖检查

| 需求 | 描述 | 实现位置 | 状态 |
|-----|------|---------|------|
| REQ-037 | 排序选项(5种含随机) | PhotoListViewModel.kt | ⏳ |
| REQ-038 | 已分类过滤开关 | KeepListTopBar | ⏳ |
| REQ-039 | 点击进入全屏预览 | PhotoListScreen.kt | ⏳ |
| REQ-040 | 长按进入选择模式 | PhotoListScreen.kt | ✅已有 |
| REQ-041 | 底部操作(5项) | BottomBarConfigs.kt | ⏳ |
| REQ-042 | 添加到相册弹窗 | AddToAlbumDialog.kt | ⏳ |
| REQ-043 | 快速分类模块 | FloatingAlbumTags.kt | ✅已有 |

### 功能测试场景

| 场景 | 测试步骤 | 预期结果 |
|-----|---------|---------|
| 过滤开关 | 点击过滤按钮 | 已分类照片隐藏/显示 |
| 过滤+预览 | 开启过滤后点击照片 | 全屏预览只包含未分类照片 |
| 添加到相册 | 选中后点击"加相册" | 显示相册列表弹窗 |
| 已在相册 | 查看照片已属于的相册 | 置灰并显示"已在此相册" |
| 管理相册 | 点击"管理快捷相册列表" | 跳转到管理页面 |
| 5种排序 | 依次选择每种排序 | 列表按对应方式排序 |

---

## 六、相关文档链接

- 上级文档: [PLAN_L2_INDEX.md](./PLAN_L2_INDEX.md)
- 需求来源: [REQUIREMENTS_LISTING.md](./REQUIREMENTS_LISTING.md) REQ-037~043
- 依赖模块: [模块A](./PLAN_L2_MODULE_A.md), [模块B](./PLAN_L2_MODULE_B.md)
- 相关模块: [模块C+D](./PLAN_L2_MODULE_C_D.md), [模块F+G](./PLAN_L2_MODULE_F_G.md)
