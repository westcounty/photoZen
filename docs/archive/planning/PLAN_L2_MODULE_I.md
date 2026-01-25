# 模块I: 其他优化需求 - 详细实施方案

> 父文档: [PLAN_L2_INDEX.md](./PLAN_L2_INDEX.md)
> 需求文档: [REQUIREMENTS_LISTING.md](./REQUIREMENTS_LISTING.md)
> 涉及需求: REQ-059~067 (共9个需求点)
> 依赖模块: 模块A~H (收尾优化层)
> 状态: 📝 规划中

---

## 一、需求分类

本模块包含多个独立的优化需求，按功能域分类如下：

| 分类 | 需求 | 优先级 |
|-----|------|-------|
| 快速分类优化 | REQ-059, REQ-060 | P0 |
| 相册列表优化 | REQ-061, REQ-062 | P1 |
| UI样式优化 | REQ-063, REQ-064 | P1 |
| Bug修复 | REQ-065 | P0 |
| 可选功能 | REQ-066, REQ-067 | P2 |

---

## 二、各需求详细方案

### REQ-059: 快速分类 - 编辑相册列表弹窗

**原始需求**:
```
底部添加相册按钮点击后，弹出"编辑快捷相册列表"弹窗
保存后实时更新底部可选相册列表
```

**实现方案**:

```kotlin
/**
 * 编辑快捷相册列表弹窗
 *
 * 功能:
 * - 显示所有相册，可勾选设为快捷相册
 * - 支持拖动排序
 * - 保存后立即更新 FloatingAlbumTags
 */
@Composable
fun EditQuickAlbumsDialog(
    allAlbums: List<AlbumEntity>,
    quickAlbumIds: List<String>,
    onSave: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedIds by remember { mutableStateOf(quickAlbumIds) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑快捷相册") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                items(allAlbums) { album ->
                    val isSelected = album.id in selectedIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedIds = if (isSelected) {
                                    selectedIds - album.id
                                } else {
                                    selectedIds + album.id
                                }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = null
                        )
                        Spacer(Modifier.width(12.dp))
                        AsyncImage(
                            model = album.coverUri,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(album.name)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(selectedIds) }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

// 集成到 FloatingAlbumTags
@Composable
fun FloatingAlbumTags(
    quickAlbums: List<AlbumEntity>,
    onAlbumClick: (String) -> Unit,
    onEditQuickAlbums: () -> Unit,  // 新增
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        quickAlbums.forEach { album ->
            AlbumTag(album = album, onClick = { onAlbumClick(album.id) })
        }

        // 添加相册按钮
        AddAlbumTag(onClick = onEditQuickAlbums)
    }
}
```

**涉及文件**:
- `ui/components/EditQuickAlbumsDialog.kt` (新增)
- `ui/screens/flowsorter/components/FloatingAlbumTags.kt` (修改)

---

### REQ-060: 快速分类 - 撤销功能（架构优化版）

**原始需求**:
```
- 右上角导航栏新增撤销按钮
- 撤销上一张照片的操作(跳过或添加到相册)
- 暂时只支持撤回一步
- 无法撤回时隐藏撤销按钮
- 设计需考虑未来多步撤回扩展性
```

---

#### 现有实现分析

**1. 全局 UndoManager (ui/state/UndoManager.kt)**
```kotlin
@Singleton
class UndoManager {
    // 支持的操作类型
    sealed class UndoAction {
        data class StatusChange(...)      // ✅ 已实现撤销逻辑
        data class MoveToAlbum(...)       // ❌ 撤销逻辑为空 (TODO)
        data class RestoreFromTrash(...)  // ✅ 已实现撤销逻辑
    }
}
```

**2. FlowSorterViewModel 的内部撤销 (独立实现)**
```kotlin
// FlowSorterViewModel.kt
data class SortAction(val photoId: String, val status: PhotoStatus)
private val _lastAction = MutableStateFlow<SortAction?>(null)

fun undoLastAction() {
    val lastAction = _lastAction.value ?: return
    sortPhotoUseCase.resetPhoto(lastAction.photoId)  // 重置为UNSORTED
    _lastAction.value = null
}
```

**3. 问题识别**
| 问题 | 影响 |
|-----|------|
| 两套撤销机制并存 | 架构不统一，维护困难 |
| FlowSorterViewModel 不使用全局 UndoManager | 无法利用已有基础设施 |
| `keepAndAddToAlbum` 未记录撤销 | 添加到相册操作无法撤销 |
| `MoveToAlbum` 撤销逻辑为空 | 需要实现 |
| 只能撤销到 UNSORTED | 应恢复到原状态而非固定值 |

---

#### 优化架构设计

**核心原则**: 统一使用全局 `UndoManager`，废弃 FlowSorterViewModel 内部的撤销逻辑

**Step 1: 扩展 UndoAction 类型**

```kotlin
// domain/model/UndoAction.kt
sealed class UndoAction {
    // ============== 现有类型 ==============
    data class StatusChange(
        val photoIds: List<String>,
        val previousStatus: Map<String, PhotoStatus>,
        val newStatus: PhotoStatus
    ) : UndoAction()

    data class RestoreFromTrash(
        val photoIds: List<String>,
        val previousStatus: Map<String, PhotoStatus>
    ) : UndoAction()

    // ============== 优化类型 ==============

    /**
     * 相册操作 - 统一处理复制和移动
     */
    data class AlbumOperation(
        val photoId: String,
        val targetAlbumId: String,
        val operationType: AlbumOperationType,
        val sourceAlbumId: String? = null,     // 移动操作时的源相册
        val createdFilePath: String? = null,   // 复制操作创建的新文件路径
        val previousStatus: PhotoStatus        // 操作前的照片状态
    ) : UndoAction() {
        enum class AlbumOperationType {
            COPY,   // 复制到相册
            MOVE    // 移动到相册
        }

        fun getDescription(): String = when (operationType) {
            AlbumOperationType.COPY -> "已复制照片到相册"
            AlbumOperationType.MOVE -> "已移动照片到相册"
        }
    }

    /**
     * 复合操作: 标记保留 + 添加到相册
     * 用于 FlowSorter 的 keepAndAddToAlbum
     */
    data class KeepAndAddToAlbum(
        val photoId: String,
        val albumId: String,
        val previousStatus: PhotoStatus,
        val operationType: AlbumOperation.AlbumOperationType,
        val sourceAlbumId: String? = null,
        val createdFilePath: String? = null
    ) : UndoAction() {
        fun getDescription(): String = "已保留并添加到相册"
    }

    /**
     * 筛选操作: 用于 FlowSorter 的单张照片筛选
     * 替代原有的 SortAction
     */
    data class SortPhoto(
        val photoId: String,
        val previousStatus: PhotoStatus,
        val newStatus: PhotoStatus
    ) : UndoAction() {
        fun getDescription(): String = when (newStatus) {
            PhotoStatus.KEEP -> "已保留照片"
            PhotoStatus.TRASH -> "已移入回收站"
            PhotoStatus.MAYBE -> "已标记为待定"
            PhotoStatus.UNSORTED -> "已重置照片"
        }
    }
}
```

**Step 2: 扩展 UndoManager 撤销逻辑**

```kotlin
// ui/state/UndoManager.kt
@Singleton
class UndoManager @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val albumRepository: AlbumRepository,  // 新增
    private val fileOperationHelper: FileOperationHelper  // 新增: 文件操作辅助
) {
    // 改用栈结构支持多步撤销（当前限制为1步）
    private val _actionStack = MutableStateFlow<List<UndoAction>>(emptyList())
    private val maxUndoSteps = 1  // 可配置，未来扩展为多步

    val lastAction: StateFlow<UndoAction?> = _actionStack.map { it.lastOrNull() }
        .stateIn(...)

    val canUndo: StateFlow<Boolean> = _actionStack.map { it.isNotEmpty() }
        .stateIn(...)

    fun recordAction(action: UndoAction) {
        _actionStack.update { stack ->
            val newStack = stack.toMutableList()
            if (newStack.size >= maxUndoSteps) {
                newStack.removeAt(0)
            }
            newStack + action
        }
    }

    suspend fun undo(): Result<Boolean> = runCatching {
        val action = _actionStack.value.lastOrNull() ?: return Result.success(false)

        when (action) {
            is UndoAction.StatusChange -> undoStatusChange(action)
            is UndoAction.RestoreFromTrash -> undoRestoreFromTrash(action)
            is UndoAction.AlbumOperation -> undoAlbumOperation(action)
            is UndoAction.KeepAndAddToAlbum -> undoKeepAndAddToAlbum(action)
            is UndoAction.SortPhoto -> undoSortPhoto(action)
        }

        // 从栈中移除
        _actionStack.update { it.dropLast(1) }
        true
    }

    /**
     * 撤销相册操作
     */
    private suspend fun undoAlbumOperation(action: UndoAction.AlbumOperation) {
        when (action.operationType) {
            AlbumOperation.AlbumOperationType.COPY -> {
                // 删除复制创建的新文件
                action.createdFilePath?.let { path ->
                    fileOperationHelper.deleteFile(path)
                }
                // 从相册中移除照片记录
                albumRepository.removePhotoFromAlbum(action.photoId, action.targetAlbumId)
            }
            AlbumOperation.AlbumOperationType.MOVE -> {
                // 将照片移回原相册
                action.sourceAlbumId?.let { sourceId ->
                    albumRepository.movePhotoToAlbum(action.photoId, sourceId)
                }
            }
        }
        // 恢复原状态
        photoRepository.updatePhotoStatus(action.photoId, action.previousStatus)
    }

    /**
     * 撤销保留+添加到相册复合操作
     */
    private suspend fun undoKeepAndAddToAlbum(action: UndoAction.KeepAndAddToAlbum) {
        // 撤销相册操作
        when (action.operationType) {
            AlbumOperation.AlbumOperationType.COPY -> {
                action.createdFilePath?.let { fileOperationHelper.deleteFile(it) }
                albumRepository.removePhotoFromAlbum(action.photoId, action.albumId)
            }
            AlbumOperation.AlbumOperationType.MOVE -> {
                action.sourceAlbumId?.let {
                    albumRepository.movePhotoToAlbum(action.photoId, it)
                }
            }
        }
        // 恢复原状态（而非固定为 UNSORTED）
        photoRepository.updatePhotoStatus(action.photoId, action.previousStatus)
    }

    /**
     * 撤销单张照片筛选操作
     */
    private suspend fun undoSortPhoto(action: UndoAction.SortPhoto) {
        // 恢复到操作前的状态
        photoRepository.updatePhotoStatus(action.photoId, action.previousStatus)
    }

    /**
     * 清除所有撤销记录
     */
    fun clear() {
        _actionStack.value = emptyList()
    }
}
```

**Step 3: 改造 FlowSorterViewModel**

```kotlin
// ui/screens/flowsorter/FlowSorterViewModel.kt
@HiltViewModel
class FlowSorterViewModel @Inject constructor(
    private val undoManager: UndoManager,  // 使用全局 UndoManager
    private val sortPhotoUseCase: SortPhotoUseCase,
    private val albumRepository: AlbumRepository,
    // ...
) : ViewModel() {

    // ❌ 移除内部撤销状态
    // private val _lastAction = MutableStateFlow<SortAction?>(null)

    // ✅ 使用全局 UndoManager 的状态
    val canUndo: StateFlow<Boolean> = undoManager.canUndo

    /**
     * 标记为保留
     */
    fun keepPhoto(photoId: String) {
        val photo = getPhotoById(photoId) ?: return
        val previousStatus = photo.status

        markPhotoAsSorted(photoId)
        updateCombo()

        viewModelScope.launch {
            // 执行操作
            sortPhotoUseCase.keepPhoto(photoId)

            // 记录撤销（使用全局 UndoManager）
            undoManager.recordAction(
                UndoAction.SortPhoto(
                    photoId = photoId,
                    previousStatus = previousStatus,
                    newStatus = PhotoStatus.KEEP
                )
            )

            // 更新统计...
        }
    }

    /**
     * 保留并添加到相册
     */
    fun keepAndAddToAlbum(bucketId: String) {
        val photo = uiState.value.currentPhoto ?: return
        val previousStatus = photo.status
        val sourceAlbumId = photo.bucketId

        markPhotoAsSorted(photo.id)
        updateCombo()

        viewModelScope.launch {
            val isMove = _albumAddAction.value == AlbumAddAction.MOVE
            var createdFilePath: String? = null

            // 执行操作
            sortPhotoUseCase.keepPhoto(photo.id)
            if (isMove) {
                albumRepository.movePhotoToAlbum(photo.id, bucketId)
            } else {
                createdFilePath = albumRepository.copyPhotoToAlbum(photo.id, bucketId)
            }

            // 记录撤销
            undoManager.recordAction(
                UndoAction.KeepAndAddToAlbum(
                    photoId = photo.id,
                    albumId = bucketId,
                    previousStatus = previousStatus,
                    operationType = if (isMove)
                        UndoAction.AlbumOperation.AlbumOperationType.MOVE
                    else
                        UndoAction.AlbumOperation.AlbumOperationType.COPY,
                    sourceAlbumId = sourceAlbumId,
                    createdFilePath = createdFilePath
                )
            )

            // 更新统计...
        }
    }

    /**
     * 撤销上一步操作
     */
    fun undoLastAction() {
        viewModelScope.launch {
            val lastAction = undoManager.lastAction.value

            // 执行撤销
            undoManager.undo()

            // 如果是筛选相关操作，需要让照片重新出现
            when (lastAction) {
                is UndoAction.SortPhoto -> {
                    _sortedPhotoIds.value = _sortedPhotoIds.value - lastAction.photoId
                    updateCountersAfterUndo(lastAction.newStatus)
                }
                is UndoAction.KeepAndAddToAlbum -> {
                    _sortedPhotoIds.value = _sortedPhotoIds.value - lastAction.photoId
                    updateCountersAfterUndo(PhotoStatus.KEEP)
                }
                else -> {}
            }
        }
    }

    private fun updateCountersAfterUndo(undoneStatus: PhotoStatus) {
        _counters.value = when (undoneStatus) {
            PhotoStatus.KEEP -> _counters.value.copy(keep = (_counters.value.keep - 1).coerceAtLeast(0))
            PhotoStatus.TRASH -> _counters.value.copy(trash = (_counters.value.trash - 1).coerceAtLeast(0))
            PhotoStatus.MAYBE -> _counters.value.copy(maybe = (_counters.value.maybe - 1).coerceAtLeast(0))
            else -> _counters.value
        }
    }
}
```

**Step 4: UI 集成（撤销按钮已存在，只需确保正确绑定）**

```kotlin
// FlowSorterScreen.kt 中已有撤销按钮，确保使用正确的状态
@Composable
fun FlowSorterScreen(
    viewModel: FlowSorterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()  // 从全局 UndoManager

    // TopBar 中的撤销按钮
    if (canUndo) {
        IconButton(onClick = { viewModel.undoLastAction() }) {
            Icon(Icons.Default.Undo, "撤销")
        }
    }
}
```

---

#### 实施步骤汇总

| 步骤 | 文件 | 变更内容 |
|-----|------|---------|
| 1 | `domain/model/UndoAction.kt` | 新增 `AlbumOperation`, `KeepAndAddToAlbum`, `SortPhoto` 类型 |
| 2 | `ui/state/UndoManager.kt` | 添加新类型的撤销逻辑，改用栈结构 |
| 3 | `data/repository/AlbumRepository.kt` | 确保有 `movePhotoToAlbum`, `copyPhotoToAlbum`, `removePhotoFromAlbum` 方法 |
| 4 | `domain/helper/FileOperationHelper.kt` | 新增文件删除辅助方法 |
| 5 | `ui/screens/flowsorter/FlowSorterViewModel.kt` | 移除内部撤销逻辑，使用全局 UndoManager |
| 6 | `ui/screens/flowsorter/FlowSorterScreen.kt` | 确保撤销按钮绑定正确 |

---

#### 验证场景

| 场景 | 操作 | 预期撤销行为 |
|-----|------|------------|
| 标记保留 | 右滑保留 | 恢复到原状态（可能是UNSORTED/MAYBE等） |
| 标记回收站 | 左滑删除 | 恢复到原状态 |
| 标记待定 | 上滑待定 | 恢复到原状态 |
| 复制到相册 | 保留+添加(复制模式) | 删除新文件，恢复原状态 |
| 移动到相册 | 保留+添加(移动模式) | 移回原相册，恢复原状态 |
| 连续操作 | 先保留A，再保留B | 只能撤销B（单步限制） |

---

### REQ-061: 相册列表 - 编辑快捷相册入口

**原始需求**:
```
无论气泡视图还是列表视图，都需要新增全局"编辑快捷相册列表"入口
```

**实现方案**:

```kotlin
// 在 AlbumsScreen 中添加 TopBar action
@Composable
fun AlbumsScreen(
    navController: NavController,
    viewModel: AlbumsViewModel = hiltViewModel()
) {
    var showEditQuickAlbums by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("相册") },
                actions = {
                    // 编辑快捷相册入口
                    IconButton(onClick = { showEditQuickAlbums = true }) {
                        Icon(Icons.Default.Bookmarks, "编辑快捷相册")
                    }
                    // 视图切换按钮
                    IconButton(onClick = viewModel::toggleViewMode) {
                        Icon(
                            if (uiState.isBubbleMode) Icons.Default.ViewList
                            else Icons.Default.BubbleChart,
                            "切换视图"
                        )
                    }
                }
            )
        }
    ) { padding ->
        // 气泡视图或列表视图
        when {
            uiState.isBubbleMode -> AlbumBubbleView(/* ... */)
            else -> AlbumListView(/* ... */)
        }
    }

    // 编辑快捷相册弹窗
    if (showEditQuickAlbums) {
        EditQuickAlbumsDialog(
            allAlbums = uiState.allAlbums,
            quickAlbumIds = uiState.quickAlbumIds,
            onSave = { ids ->
                viewModel.updateQuickAlbums(ids)
                showEditQuickAlbums = false
            },
            onDismiss = { showEditQuickAlbums = false }
        )
    }
}
```

**涉及文件**:
- `ui/screens/albums/AlbumsScreen.kt` (修改)

---

### REQ-062: 相册列表 - 开始整理按钮样式统一

**原始需求**:
```
列表视图的开始整理按钮与时间线列表中的按钮样式统一
```

**实现方案**:

```kotlin
/**
 * 统一的"开始整理"按钮组件
 *
 * 用于: 相册列表视图、时间线分组
 */
@Composable
fun StartSortingButton(
    totalCount: Int,
    sortedCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (totalCount > 0) sortedCount.toFloat() / totalCount else 0f
    val isComplete = sortedCount >= totalCount

    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isComplete)
                KeepGreen.copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isComplete) KeepGreen else MaterialTheme.colorScheme.outline
        )
    ) {
        if (isComplete) {
            Icon(
                Icons.Default.CheckCircle,
                null,
                tint = KeepGreen,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text("已完成", color = KeepGreen)
        } else {
            // 小进度指示器
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp
            )
            Spacer(Modifier.width(4.dp))
            Text("整理 $sortedCount/$totalCount")
        }
    }
}

// 在相册列表视图中使用
@Composable
fun AlbumListItem(
    album: AlbumEntity,
    onAlbumClick: () -> Unit,
    onStartSorting: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAlbumClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 相册封面
        AsyncImage(/* ... */)

        Spacer(Modifier.width(16.dp))

        // 相册信息
        Column(modifier = Modifier.weight(1f)) {
            Text(album.name)
            Text("${album.photoCount} 张照片")
        }

        // 统一样式的开始整理按钮
        StartSortingButton(
            totalCount = album.photoCount,
            sortedCount = album.sortedCount,
            onClick = onStartSorting
        )
    }
}
```

**涉及文件**:
- `ui/components/StartSortingButton.kt` (新增)
- `ui/screens/albums/components/AlbumListItem.kt` (修改)
- `ui/screens/timeline/components/TimelineEventCard.kt` (修改)

---

### REQ-063: 照片状态指示器样式优化

**原始需求**:
```
设计足够现代的样式:
- 例如在照片左上角打上小小的直角三角形角标
- 需要足够小且表意足够明确
- 应用于: 我的相册照片列表、时间线照片列表
```

**实现方案**:

```kotlin
/**
 * 照片状态角标
 *
 * 设计: 左上角直角三角形
 * - 保留: 绿色
 * - 待定: 琥珀色
 * - 回收站: 红色
 * - 未筛选: 不显示角标
 */
@Composable
fun PhotoStatusBadge(
    status: PhotoStatus,
    modifier: Modifier = Modifier
) {
    if (status == PhotoStatus.UNSORTED) return  // 未筛选不显示

    val color = when (status) {
        PhotoStatus.KEEP -> KeepGreen
        PhotoStatus.MAYBE -> MaybeAmber
        PhotoStatus.TRASH -> TrashRed
        else -> return
    }

    Canvas(
        modifier = modifier.size(16.dp)
    ) {
        // 绘制直角三角形
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(0f, size.height)
            close()
        }
        drawPath(path, color)

        // 绘制小图标(可选)
        // 保留: 勾
        // 待定: 问号
        // 回收站: X
    }
}

// 在照片卡片中使用
@Composable
fun PhotoCard(
    photo: PhotoEntity,
    showStatusBadge: Boolean = true,
    // ...
) {
    Box {
        AsyncImage(
            model = photo.systemUri,
            // ...
        )

        // 状态角标
        if (showStatusBadge) {
            PhotoStatusBadge(
                status = photo.status,
                modifier = Modifier.align(Alignment.TopStart)
            )
        }
    }
}
```

**涉及文件**:
- `ui/components/PhotoStatusBadge.kt` (新增)
- `ui/components/DragSelectPhotoGrid.kt` (修改: 集成角标)

---

### REQ-064: 筛选列表标题优化

**原始需求**:
```
- 左上角筛选进度显示优化
- 字号调整
- 避免莫名其妙的换行
- 不和右侧导航栏按钮冲突
```

**实现方案**:

```kotlin
/**
 * 优化后的筛选进度标题
 */
@Composable
fun FlowSorterTitle(
    source: String,          // "今日任务" / "相册名" / "时间线分组名"
    currentIndex: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 来源 - 小字
        Text(
            text = source,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // 进度 - 主标题
        Text(
            text = "$currentIndex / $totalCount",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

// 在 TopBar 中使用
@Composable
fun FlowSorterTopBar(
    source: String,
    currentIndex: Int,
    totalCount: Int,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit
) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "返回")
            }
        },
        title = {
            FlowSorterTitle(
                source = source,
                currentIndex = currentIndex,
                totalCount = totalCount,
                modifier = Modifier.widthIn(max = 150.dp)  // 限制宽度避免冲突
            )
        },
        actions = actions
    )
}
```

**涉及文件**:
- `ui/screens/flowsorter/components/FlowSorterTopBar.kt` (修改)

---

### REQ-065: 每日任务跨天进度修复

**原始需求**:
```
问题: 跨天时进度不会重新更新，还是显示前一天的进度
现状: 必须手动杀掉app进程重新打开才能正常显示
```

**实现方案**:

```kotlin
/**
 * 跨天检测与刷新机制
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getDailyTasksUseCase: GetDailyTasksUseCase,
    private val preferencesDataStore: PreferencesDataStore
) : ViewModel() {

    // 记录上次加载日期
    private var lastLoadDate: LocalDate? = null

    init {
        observeDayChange()
    }

    private fun observeDayChange() {
        viewModelScope.launch {
            // 方案1: 定时检查(每分钟)
            while (true) {
                delay(60_000)  // 1分钟
                checkDayChange()
            }
        }

        // 方案2: 监听系统时间变化广播
        // 需要在 Application 或 Activity 注册 TIME_TICK / DATE_CHANGED receiver
    }

    private fun checkDayChange() {
        val today = LocalDate.now()
        if (lastLoadDate != null && lastLoadDate != today) {
            // 日期已变化，刷新每日任务
            refreshDailyTasks()
        }
        lastLoadDate = today
    }

    fun refreshDailyTasks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDailyTasksLoading = true) }
            val tasks = getDailyTasksUseCase(forceRefresh = true)
            _uiState.update {
                it.copy(
                    dailyTasks = tasks,
                    isDailyTasksLoading = false
                )
            }
        }
    }

    // App 从后台恢复时也检查
    fun onResume() {
        checkDayChange()
    }
}

// 在 HomeScreen 中
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    // 监听生命周期
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // ... 其余UI
}
```

**涉及文件**:
- `ui/screens/home/HomeViewModel.kt` (修改)
- `ui/screens/home/HomeScreen.kt` (修改)
- `domain/usecase/GetDailyTasksUseCase.kt` (修改: 添加forceRefresh参数)

---

### REQ-066: 备选方案 - 列数切换按钮 (P2)

**原始需求**:
```
如果双指缩放实现困难，提供按钮循环切换列数
- 按钮图标需与视图模式切换按钮区分
```

**实现方案**:
参见 [模块A](./PLAN_L2_MODULE_A.md) 备选方案部分。

---

### REQ-067: 新手引导设计 (P2)

**原始需求**:
```
如果实现了双指缩放，设计新手引导让用户知道操作方式
```

**实现方案**:

```kotlin
/**
 * 新手引导覆盖层
 *
 * 首次使用时显示手势提示
 */
@Composable
fun PinchZoomOnboarding(
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 手势动画图示
            Image(
                painter = painterResource(R.drawable.ic_pinch_gesture),
                contentDescription = null,
                modifier = Modifier.size(120.dp)
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = "双指缩放切换列数",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "双指张开放大照片，双指收缩缩小照片",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            Button(onClick = onDismiss) {
                Text("知道了")
            }
        }
    }
}

// 使用 DataStore 记录是否已显示
class OnboardingManager @Inject constructor(
    private val dataStore: PreferencesDataStore
) {
    val hasSeenPinchZoomGuide: Flow<Boolean> = dataStore.data.map {
        it[PINCH_ZOOM_GUIDE_SEEN] ?: false
    }

    suspend fun markPinchZoomGuideSeen() {
        dataStore.edit { it[PINCH_ZOOM_GUIDE_SEEN] = true }
    }

    companion object {
        private val PINCH_ZOOM_GUIDE_SEEN = booleanPreferencesKey("pinch_zoom_guide_seen")
    }
}
```

**涉及文件**:
- `ui/components/onboarding/PinchZoomOnboarding.kt` (新增)
- `data/local/datastore/OnboardingManager.kt` (新增)

---

## 三、验证清单

| 需求 | 描述 | 优先级 | 实现位置 | 状态 |
|-----|------|-------|---------|------|
| REQ-059 | 编辑快捷相册弹窗 | P0 | EditQuickAlbumsDialog | ⏳ |
| REQ-060 | 快速分类撤销 | P0 | ClassificationUndoManager | ⏳ |
| REQ-061 | 相册列表编辑入口 | P1 | AlbumsScreen | ⏳ |
| REQ-062 | 开始整理按钮统一 | P1 | StartSortingButton | ⏳ |
| REQ-063 | 状态角标样式 | P1 | PhotoStatusBadge | ⏳ |
| REQ-064 | 筛选标题优化 | P1 | FlowSorterTitle | ⏳ |
| REQ-065 | 跨天进度修复 | P0 | HomeViewModel | ⏳ |
| REQ-066 | 列数按钮(备选) | P2 | - | 备选 |
| REQ-067 | 新手引导 | P2 | PinchZoomOnboarding | ⏳ |

---

## 四、相关文档链接

- 上级文档: [PLAN_L2_INDEX.md](./PLAN_L2_INDEX.md)
- 需求来源: [REQUIREMENTS_LISTING.md](./REQUIREMENTS_LISTING.md) REQ-059~067
- 依赖模块: 所有前置模块
