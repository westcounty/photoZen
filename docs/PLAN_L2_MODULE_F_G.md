# 模块F+G: 相册照片列表 + 时间线照片列表 - 详细实施方案

> 父文档: [PLAN_L2_INDEX.md](./PLAN_L2_INDEX.md)
> 需求文档: [REQUIREMENTS_LISTING.md](./REQUIREMENTS_LISTING.md)
> 涉及需求: REQ-044~056 (共13个需求点)
> 依赖模块: 模块A, 模块B
> 状态: 📝 规划中

---

## 设计决策: 合并方案原因

模块F(相册照片列表)和模块G(时间线照片列表)功能高度相似：
- 排序选项相同（时间正序/倒序 + 随机）
- 选择模式逻辑相同
- 底部操作完全相同（4项通用 + 1项单选专有）
- 顶部整理模块结构相同

主要差异：
- 入口不同（相册tab vs 时间线tab→查看全部）
- 数据来源不同（相册ID vs 时间线分组ID）
- 模块G为新增页面

**实施策略**: 创建通用的 `PhotoCollectionScreen` 组件，通过配置差异化两个页面。

---

## 一、原始需求摘录

### 模块F: 相册照片列表 (REQ-044~050)

```
REQ-044: 排序选项
- 右上角: 排序按钮、视图模式切换按钮
- 排序选项: 照片真实时间正序/倒序、随机排序
- 默认: 照片真实时间倒序

REQ-045: 点击进入全屏预览
- 可滑动切换范围: 该相册的全部照片

REQ-046: 长按进入选择模式
- 网格视图: 拖动批量选中
- 瀑布流视图: 拖动无多选效果
- 系统返回手势退出选择模式

REQ-047: 底部操作
- 通用: 添加到其他相册、批量修改筛选状态、复制照片、彻底删除
- 单选额外: 从此开始筛选

REQ-048: 批量修改筛选状态
- 可选择: 标记为保留、设置为待定、移至回收站、重置为未筛选

REQ-049: 添加到其他相册
- 展示全部相册列表
- 当前相册置灰并提示"这是当前所在相册"
- 提供添加相册按钮
- 弹窗保存后可选列表立即更新

REQ-050: 顶部整理模块
- 顶部开始整理模块
- 照片筛选状态过滤器
- 展示在顶部导航栏下方
```

### 模块G: 时间线照片列表 (REQ-051~056)

```
REQ-051: 排序选项 (同REQ-044)
REQ-052: 点击进入全屏预览 (范围: 时间线分组的全部照片)
REQ-053: 长按进入选择模式 (同REQ-046)
REQ-054: 底部操作 (同REQ-047)
REQ-055: 批量修改筛选状态 (同REQ-048)
REQ-056: 顶部整理模块 (同REQ-050)
```

---

## 二、现有实现分析

### 2.1 现有实现状态

| 功能 | 相册列表(F) | 时间线列表(G) | 备注 |
|-----|-----------|-------------|------|
| 基础列表 | ✅ AlbumPhotoListScreen | ❌ 需新增 | |
| 排序(3种) | ✅ | - | |
| 视图切换 | ✅ | - | |
| 点击全屏预览 | 部分(HorizontalPager) | - | 需集成模块B |
| 长按选择 | ✅ | - | |
| 底部操作(4项) | 部分 | - | 需补全 |
| 批量修改状态 | ❌ | - | 需新增 |
| 添加到其他相册 | 部分 | - | 需优化 |
| 顶部整理模块 | ✅ AlbumStatsCard | - | 可复用 |
| 状态过滤器 | ✅ StatusFilterChips | - | 可复用 |

### 2.2 关键文件

```
相册列表(已有):
ui/screens/albums/
├── AlbumPhotoListScreen.kt
├── AlbumPhotoListViewModel.kt
└── components/
    ├── AlbumStatsCard.kt      # 顶部整理卡片
    └── StatusFilterChips.kt   # 状态过滤器

时间线列表(需新增):
ui/screens/timeline/
├── TimelinePhotoListScreen.kt     # 新增
├── TimelinePhotoListViewModel.kt  # 新增
└── TimelineScreen.kt              # 修改: 添加"查看全部"入口
```

---

## 三、技术方案设计

### 3.1 通用照片集合Screen抽象

```kotlin
/**
 * 照片集合页面配置
 *
 * 用于配置相册列表和时间线列表的差异化行为
 */
data class PhotoCollectionConfig(
    val collectionType: CollectionType,        // 集合类型
    val collectionId: String,                  // 集合ID(相册ID或时间线分组ID)
    val collectionName: String,                // 集合名称
    val showStartSortingCard: Boolean = true,  // 是否显示开始整理卡片
    val showStatusFilter: Boolean = true,      // 是否显示状态过滤器
    val sortOptions: List<PhotoListSortOrder> = listOf(
        PhotoListSortOrder.DATE_DESC,
        PhotoListSortOrder.DATE_ASC,
        PhotoListSortOrder.RANDOM
    ),
    val excludeCurrentFromAlbumPicker: Boolean = true  // 添加到相册时排除当前
)

enum class CollectionType {
    ALBUM,      // 相册
    TIMELINE    // 时间线分组
}
```

### 3.2 底部操作栏配置

```kotlin
// BottomBarConfigs.kt 更新
object BottomBarConfigs {
    /**
     * 相册/时间线照片列表操作
     *
     * 通用操作(4项): 添加到其他相册、批量修改状态、复制、彻删
     * 单选额外(1项): 从此开始筛选
     */
    fun collectionListActions(
        selectedCount: Int,
        onAddToOtherAlbum: () -> Unit,
        onBatchChangeStatus: () -> Unit,
        onCopy: () -> Unit,
        onDelete: () -> Unit,
        onStartSortingFromHere: (() -> Unit)? = null  // 单选时传入
    ): List<BottomBarAction> {
        val actions = mutableListOf(
            BottomBarAction(
                icon = Icons.Default.PhotoAlbum,
                label = "加相册",
                onClick = onAddToOtherAlbum
            ),
            BottomBarAction(
                icon = Icons.Default.SwapVert,
                label = "改状态",
                onClick = onBatchChangeStatus
            ),
            BottomBarAction(
                icon = Icons.Default.ContentCopy,
                label = "复制",
                onClick = onCopy
            ),
            BottomBarAction(
                icon = Icons.Default.DeleteForever,
                label = "彻删",
                onClick = onDelete,
                tint = TrashRed
            )
        )

        // 单选时添加"从此开始筛选"
        if (selectedCount == 1 && onStartSortingFromHere != null) {
            actions.add(1, BottomBarAction(  // 插入到第2位
                icon = Icons.Default.PlayArrow,
                label = "从此筛选",
                onClick = onStartSortingFromHere
            ))
        }

        return actions
    }
}
```

### 3.3 批量修改状态弹窗

```kotlin
/**
 * 批量修改筛选状态弹窗
 *
 * 选项:
 * - 标记为保留 (KEEP)
 * - 设置为待定 (MAYBE)
 * - 移至回收站 (TRASH)
 * - 重置为未筛选 (UNSORTED)
 */
@Composable
fun BatchChangeStatusDialog(
    selectedCount: Int,
    onStatusSelected: (PhotoStatus) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改筛选状态") },
        text = {
            Column {
                Text(
                    text = "将选中的 $selectedCount 张照片改为:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                StatusOptionItem(
                    icon = Icons.Default.CheckCircle,
                    label = "标记为保留",
                    color = KeepGreen,
                    onClick = { onStatusSelected(PhotoStatus.KEEP) }
                )
                StatusOptionItem(
                    icon = Icons.Default.HelpOutline,
                    label = "设置为待定",
                    color = MaybeAmber,
                    onClick = { onStatusSelected(PhotoStatus.MAYBE) }
                )
                StatusOptionItem(
                    icon = Icons.Default.Delete,
                    label = "移至回收站",
                    color = TrashRed,
                    onClick = { onStatusSelected(PhotoStatus.TRASH) }
                )
                StatusOptionItem(
                    icon = Icons.Default.Refresh,
                    label = "重置为未筛选",
                    color = MaterialTheme.colorScheme.outline,
                    onClick = { onStatusSelected(PhotoStatus.UNSORTED) }
                )
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
private fun StatusOptionItem(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
```

### 3.4 时间线照片列表 (新增页面)

```kotlin
/**
 * 时间线照片列表页面
 *
 * 入口: 时间线tab → 展开分组 → 查看全部
 * 功能: 与相册照片列表基本一致
 */
@Composable
fun TimelinePhotoListScreen(
    groupId: String,
    groupName: String,
    navController: NavController,
    viewModel: TimelinePhotoListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFullscreen by remember { mutableStateOf(false) }
    var fullscreenIndex by remember { mutableIntStateOf(0) }

    // 复用相册列表的大部分UI结构
    PhotoCollectionScreen(
        config = PhotoCollectionConfig(
            collectionType = CollectionType.TIMELINE,
            collectionId = groupId,
            collectionName = groupName
        ),
        photos = uiState.photos,
        selectedIds = uiState.selectedIds,
        isSelectionMode = uiState.isSelectionMode,
        sortOrder = uiState.sortOrder,
        gridMode = uiState.gridMode,
        statusFilter = uiState.statusFilter,
        onPhotoClick = { index ->
            if (!uiState.isSelectionMode) {
                fullscreenIndex = index
                showFullscreen = true
            }
        },
        onPhotoLongPress = viewModel::enterSelectionMode,
        onSelectionChange = viewModel::updateSelection,
        onSortChange = viewModel::setSortOrder,
        onGridModeToggle = viewModel::toggleGridMode,
        onStatusFilterChange = viewModel::setStatusFilter,
        onStartSorting = {
            navController.navigate("flowsorter/timeline/$groupId")
        },
        // 底部操作回调
        onAddToAlbum = { /* 显示弹窗 */ },
        onBatchChangeStatus = { /* 显示弹窗 */ },
        onCopy = viewModel::copySelected,
        onDelete = viewModel::deleteSelected,
        onStartSortingFromHere = { photoId ->
            navController.navigate("flowsorter/timeline/$groupId?startFrom=$photoId")
        }
    )

    // 全屏预览
    if (showFullscreen) {
        UnifiedFullscreenViewer(
            photos = uiState.filteredPhotos,
            initialIndex = fullscreenIndex,
            onExit = { showFullscreen = false },
            onAction = { /* 处理操作 */ }
        )
    }
}
```

---

## 四、详细实现步骤

### Step F1: 相册列表底部操作栏更新

**文件**: `ui/screens/albums/AlbumPhotoListScreen.kt`

```kotlin
// 更新底部操作栏
if (uiState.isSelectionMode) {
    var showAddToAlbum by remember { mutableStateOf(false) }
    var showChangeStatus by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    SelectionBottomBar(
        actions = BottomBarConfigs.collectionListActions(
            selectedCount = uiState.selectedCount,
            onAddToOtherAlbum = { showAddToAlbum = true },
            onBatchChangeStatus = { showChangeStatus = true },
            onCopy = { viewModel.copySelected() },
            onDelete = { showDeleteConfirm = true },
            onStartSortingFromHere = if (uiState.selectedCount == 1) {
                { viewModel.startSortingFromSelected() }
            } else null
        )
    )

    // 添加到其他相册弹窗
    if (showAddToAlbum) {
        AddToAlbumDialog(
            photoIds = uiState.selectedIds.toList(),
            albums = uiState.albums,
            currentAlbumId = uiState.currentAlbumId,  // 排除当前相册
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

    // 批量修改状态弹窗
    if (showChangeStatus) {
        BatchChangeStatusDialog(
            selectedCount = uiState.selectedCount,
            onStatusSelected = { newStatus ->
                viewModel.changeSelectedStatus(newStatus)
                showChangeStatus = false
            },
            onDismiss = { showChangeStatus = false }
        )
    }

    // 删除确认弹窗
    if (showDeleteConfirm) {
        DeleteConfirmDialog(
            count = uiState.selectedCount,
            onConfirm = {
                viewModel.deleteSelected()
                showDeleteConfirm = false
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}
```

### Step F2: 相册列表全屏预览集成

**文件**: `ui/screens/albums/AlbumPhotoListScreen.kt`

```kotlin
// 替换现有的 HorizontalPager 预览为统一全屏预览
@Composable
fun AlbumPhotoListScreen() {
    // ...
    var showFullscreen by remember { mutableStateOf(false) }
    var fullscreenIndex by remember { mutableIntStateOf(0) }

    if (showFullscreen) {
        UnifiedFullscreenViewer(
            photos = uiState.filteredPhotos,
            initialIndex = fullscreenIndex,
            onExit = { showFullscreen = false },
            onAction = { actionType, photo ->
                when (actionType) {
                    FullscreenActionType.COPY -> viewModel.copyPhoto(photo.id)
                    FullscreenActionType.SHARE -> viewModel.sharePhoto(photo.id)
                    FullscreenActionType.EDIT -> navController.navigate("edit/${photo.id}")
                    FullscreenActionType.DELETE -> viewModel.deletePhoto(photo.id)
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

### Step G1: 时间线照片列表页面创建

**文件**: `ui/screens/timeline/TimelinePhotoListScreen.kt` (新增)

```kotlin
@Composable
fun TimelinePhotoListScreen(
    groupId: String,
    navController: NavController,
    viewModel: TimelinePhotoListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(groupId) {
        viewModel.loadGroup(groupId)
    }

    // UI结构与相册列表一致
    Scaffold(
        topBar = {
            CollectionTopBar(
                title = uiState.groupName,
                onBack = { navController.popBackStack() },
                sortOrder = uiState.sortOrder,
                onSortChange = viewModel::setSortOrder,
                gridMode = uiState.gridMode,
                onGridModeToggle = viewModel::toggleGridMode
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // 顶部整理卡片
            if (uiState.showStartCard) {
                TimelineStatsCard(
                    totalCount = uiState.totalCount,
                    sortedCount = uiState.sortedCount,
                    onStartSorting = {
                        navController.navigate("flowsorter/timeline/$groupId")
                    }
                )
            }

            // 状态过滤器
            StatusFilterChips(
                currentFilter = uiState.statusFilter,
                onFilterChange = viewModel::setStatusFilter
            )

            // 照片网格
            // ... (与相册列表相同)
        }
    }
}
```

### Step G2: 时间线照片列表ViewModel

**文件**: `ui/screens/timeline/TimelinePhotoListViewModel.kt` (新增)

```kotlin
@HiltViewModel
class TimelinePhotoListViewModel @Inject constructor(
    private val getTimelinePhotosUseCase: GetTimelinePhotosUseCase,
    private val selectionStateHolder: PhotoSelectionStateHolder,
    private val batchOperationUseCase: PhotoBatchOperationUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val groupId: String = savedStateHandle["groupId"] ?: ""

    data class UiState(
        val groupName: String = "",
        val photos: List<PhotoEntity> = emptyList(),
        val filteredPhotos: List<PhotoEntity> = emptyList(),
        val selectedIds: Set<String> = emptySet(),
        val isSelectionMode: Boolean = false,
        val sortOrder: PhotoListSortOrder = PhotoListSortOrder.DATE_DESC,
        val gridMode: PhotoGridMode = PhotoGridMode.WATERFALL,
        val statusFilter: PhotoStatus? = null,
        val totalCount: Int = 0,
        val sortedCount: Int = 0,
        val showStartCard: Boolean = true
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadGroup(groupId)
        observeSelection()
    }

    fun loadGroup(groupId: String) {
        viewModelScope.launch {
            getTimelinePhotosUseCase(groupId).collect { photos ->
                _uiState.update {
                    it.copy(
                        photos = photos,
                        filteredPhotos = applyFilters(photos, it.statusFilter, it.sortOrder),
                        totalCount = photos.size,
                        sortedCount = photos.count { p -> p.status != PhotoStatus.UNSORTED }
                    )
                }
            }
        }
    }

    // 其他方法与 AlbumPhotoListViewModel 类似
    // ...
}
```

### Step G3: 时间线页面添加"查看全部"入口

**文件**: `ui/screens/timeline/TimelineScreen.kt`

```kotlin
// 在时间线分组展开后的照片列表末尾添加"查看全部"
@Composable
fun TimelineEventCard(
    event: TimelineEvent,
    onViewAll: (String) -> Unit,  // 新增回调
    // ...
) {
    // 展开后的照片列表
    if (isExpanded) {
        LazyRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            items(event.photos.take(10)) { photo ->
                // 照片缩略图
            }

            // "查看全部"按钮
            if (event.photos.size > 10) {
                item {
                    ViewAllButton(
                        count = event.photos.size,
                        onClick = { onViewAll(event.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ViewAllButton(
    count: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "查看全部",
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = "$count 张",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

// 在 TimelineScreen 中处理导航
TimelineEventCard(
    event = event,
    onViewAll = { groupId ->
        navController.navigate("timeline_photos/$groupId")
    }
)
```

### Step G4: 导航图更新

**文件**: `navigation/NavGraph.kt`

```kotlin
// 添加时间线照片列表路由
composable(
    route = "timeline_photos/{groupId}",
    arguments = listOf(navArgument("groupId") { type = NavType.StringType })
) { backStackEntry ->
    val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
    TimelinePhotoListScreen(
        groupId = groupId,
        navController = navController
    )
}
```

---

## 五、验证清单

### 模块F: 相册照片列表

| 需求 | 描述 | 实现位置 | 状态 |
|-----|------|---------|------|
| REQ-044 | 排序选项(3种含随机) | AlbumPhotoListViewModel | ✅已有 |
| REQ-045 | 点击进入全屏预览 | AlbumPhotoListScreen | ⏳ |
| REQ-046 | 长按进入选择模式 | AlbumPhotoListScreen | ✅已有 |
| REQ-047 | 底部操作(4+1项) | BottomBarConfigs | ⏳ |
| REQ-048 | 批量修改筛选状态 | BatchChangeStatusDialog | ⏳ |
| REQ-049 | 添加到其他相册 | AddToAlbumDialog | ⏳ |
| REQ-050 | 顶部整理模块 | AlbumStatsCard | ✅已有 |

### 模块G: 时间线照片列表

| 需求 | 描述 | 实现位置 | 状态 |
|-----|------|---------|------|
| REQ-051 | 排序选项(3种) | TimelinePhotoListViewModel | ⏳新增 |
| REQ-052 | 点击进入全屏预览 | TimelinePhotoListScreen | ⏳新增 |
| REQ-053 | 长按进入选择模式 | TimelinePhotoListScreen | ⏳新增 |
| REQ-054 | 底部操作(4+1项) | BottomBarConfigs | ⏳ |
| REQ-055 | 批量修改筛选状态 | BatchChangeStatusDialog | ⏳ |
| REQ-056 | 顶部整理模块 | TimelineStatsCard | ⏳新增 |

### 功能测试场景

| 场景 | 页面 | 测试步骤 | 预期结果 |
|-----|------|---------|---------|
| 查看全部入口 | 时间线 | 展开分组滑到最右 | 显示"查看全部"按钮 |
| 进入列表 | 时间线 | 点击"查看全部" | 进入时间线照片列表 |
| 批量改状态 | 相册/时间线 | 选中后点"改状态" | 显示4个选项弹窗 |
| 添加到相册 | 相册/时间线 | 选中后点"加相册" | 显示相册列表(排除当前) |
| 从此筛选 | 相册/时间线 | 单选后点"从此筛选" | 跳转到筛选页面 |
| 复制照片 | 相册/时间线 | 选中后点"复制" | Toast显示成功 |

---

## 六、相关文档链接

- 上级文档: [PLAN_L2_INDEX.md](./PLAN_L2_INDEX.md)
- 需求来源: [REQUIREMENTS_LISTING.md](./REQUIREMENTS_LISTING.md) REQ-044~056
- 依赖模块: [模块A](./PLAN_L2_MODULE_A.md), [模块B](./PLAN_L2_MODULE_B.md)
- 相关模块: [模块E](./PLAN_L2_MODULE_E.md), [模块H](./PLAN_L2_MODULE_H.md)
