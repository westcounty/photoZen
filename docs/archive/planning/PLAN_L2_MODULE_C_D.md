# 模块C+D: 待定照片列表 + 回收站照片列表 - 详细实施方案

> 父文档: [PLAN_L2_INDEX.md](./PLAN_L2_INDEX.md)
> 需求文档: [REQUIREMENTS_LISTING.md](./REQUIREMENTS_LISTING.md)
> 涉及需求: REQ-028~036 (共9个需求点)
> 依赖模块: 模块A, 模块B
> 状态: 📝 规划中

---

## 设计决策: 合并方案原因

模块C(待定列表)和模块D(回收站列表)具有高度相似性：
- 都复用 PhotoListScreen 组件（通过 status 参数区分）
- 排序选项结构相同（只是默认值不同）
- 选择模式和底部操作栏结构相同（只是操作项不同）

因此合并为一个方案文档，减少重复，提高维护效率。

---

## 一、原始需求摘录

### 模块C: 待定照片列表 (REQ-028~032)

```
REQ-028: 排序选项
- 右上角: 排序按钮、视图模式切换按钮
- 排序选项: 照片真实时间正序/倒序、添加至待定列表时间正序/倒序
- 默认: 照片真实时间倒序

REQ-029: 点击选中(最多6张)
- 点击选中，再次点击取消
- 支持多选，最多6张
- 超过6张toast"最多可对比6张照片"

REQ-030: 拖动多选限制
- 网格视图: 按住拖动选中连续照片，不清除之前选中
- 若超过6张，松手后选中从开始的6张并toast
- 瀑布流视图: 拖动无多选效果

REQ-031: 选中后操作
- 展示: 清除、对比
- 对比按钮仅在2-6张时可点击

REQ-032: 对比模式全屏预览
- 进入对比模式后点击全屏预览按钮进入通用全屏预览界面
```

### 模块D: 回收站照片列表 (REQ-033~036)

```
REQ-033: 排序选项
- 右上角: 排序按钮、视图模式切换按钮
- 排序选项: 照片真实时间正序/倒序、添加至回收站时间正序/倒序
- 默认: 添加到回收站时间倒序

REQ-034: 点击进入全屏预览
- 点击照片进入全屏预览
- 可滑动切换范围: 回收站全部照片

REQ-035: 长按进入选择模式
- 长按选中并进入选择模式
- 网格视图: 拖动批量选中
- 瀑布流视图: 拖动无多选效果
- 系统返回手势退出选择模式

REQ-036: 底部操作
- 设置为保留、设置为待定、重置为未筛选、彻底删除
```

---

## 二、现有实现分析

### 2.1 现有实现状态

| 功能 | 待定列表 | 回收站列表 | 备注 |
|-----|---------|-----------|------|
| 基础列表展示 | ✅ PhotoListScreen | ✅ TrashScreen | 各自独立实现 |
| 排序功能 | 部分 | 部分 | 缺少添加时间排序 |
| 视图切换 | ✅ | ✅ | |
| 点击行为 | 进入对比 | 进入选择 | 需修改 |
| 选择模式 | 特殊(6张限制) | ✅ | 需增加限制 |
| 底部操作 | 部分 | 部分 | 需补全 |
| 全屏预览 | ❌ | ❌ | 需集成模块B |

### 2.2 关键文件

```
待定列表:
- ui/screens/photolist/PhotoListScreen.kt (status=MAYBE)
- ui/screens/photolist/PhotoListViewModel.kt
- ui/screens/lighttable/LightTableScreen.kt (对比模式)

回收站列表:
- ui/screens/trash/TrashScreen.kt
- ui/screens/trash/TrashViewModel.kt
```

---

## 三、技术方案设计

### 3.1 待定列表选择限制

```kotlin
// PhotoListViewModel.kt 修改
class PhotoListViewModel {
    // 新增: 选择数量限制
    private val selectionLimit: Int? = when (status) {
        PhotoStatus.MAYBE -> 6  // 待定列表最多6张
        else -> null            // 其他列表无限制
    }

    fun toggleSelection(photoId: String) {
        val current = _selectedIds.value
        if (photoId in current) {
            // 取消选中
            _selectedIds.value = current - photoId
        } else {
            // 选中 - 检查限制
            if (selectionLimit != null && current.size >= selectionLimit) {
                _toastMessage.value = "最多可对比${selectionLimit}张照片"
                return
            }
            _selectedIds.value = current + photoId
        }
    }

    // 拖动多选时的限制处理
    fun updateDragSelection(newSelection: Set<String>) {
        if (selectionLimit != null && newSelection.size > selectionLimit) {
            // 只保留前N张
            val limited = newSelection.take(selectionLimit).toSet()
            _selectedIds.value = limited
            _toastMessage.value = "最多可对比${selectionLimit}张照片"
        } else {
            _selectedIds.value = newSelection
        }
    }
}
```

### 3.2 排序选项扩展

```kotlin
// 排序选项枚举扩展
enum class PhotoListSortOrder(val displayName: String) {
    DATE_DESC("时间倒序"),
    DATE_ASC("时间正序"),
    ADDED_DESC("添加时间倒序"),  // 新增
    ADDED_ASC("添加时间正序"),   // 新增
    RANDOM("随机排序")
}

// 各列表的排序选项配置
object ListSortConfigs {
    val maybeList = listOf(
        PhotoListSortOrder.DATE_DESC,
        PhotoListSortOrder.DATE_ASC,
        PhotoListSortOrder.ADDED_DESC,
        PhotoListSortOrder.ADDED_ASC
    )
    val trashList = listOf(
        PhotoListSortOrder.DATE_DESC,
        PhotoListSortOrder.DATE_ASC,
        PhotoListSortOrder.ADDED_DESC,
        PhotoListSortOrder.ADDED_ASC
    )
    // ... 其他列表
}

// 默认排序配置
object DefaultSortOrders {
    val maybeList = PhotoListSortOrder.DATE_DESC
    val trashList = PhotoListSortOrder.ADDED_DESC  // 按添加时间倒序
}
```

### 3.3 底部操作栏配置

```kotlin
// BottomBarConfigs.kt 更新

object BottomBarConfigs {
    // 待定列表 - 特殊配置(清除+对比)
    fun maybeListActions(
        selectedCount: Int,
        onClear: () -> Unit,
        onCompare: () -> Unit
    ): List<BottomBarAction> = listOf(
        BottomBarAction(
            icon = Icons.Default.Clear,
            label = "清除",
            onClick = onClear,
            enabled = true
        ),
        BottomBarAction(
            icon = Icons.Default.Compare,
            label = "对比",
            onClick = onCompare,
            enabled = selectedCount in 2..6
        )
    )

    // 回收站列表
    fun trashListActions(
        onKeep: () -> Unit,
        onMaybe: () -> Unit,
        onReset: () -> Unit,
        onDelete: () -> Unit
    ): List<BottomBarAction> = listOf(
        BottomBarAction(
            icon = Icons.Default.CheckCircle,
            label = "保留",
            onClick = onKeep,
            tint = KeepGreen
        ),
        BottomBarAction(
            icon = Icons.Default.HelpOutline,
            label = "待定",
            onClick = onMaybe,
            tint = MaybeAmber
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

### 3.4 全屏预览集成

```kotlin
// TrashScreen.kt 修改
@Composable
fun TrashScreen(
    navController: NavController,
    viewModel: TrashViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFullscreen by remember { mutableStateOf(false) }
    var fullscreenInitialIndex by remember { mutableIntStateOf(0) }

    // 全屏预览状态
    if (showFullscreen) {
        UnifiedFullscreenViewer(
            photos = uiState.photos,
            initialIndex = fullscreenInitialIndex,
            onExit = { showFullscreen = false },
            onAction = { actionType, photo ->
                when (actionType) {
                    FullscreenActionType.DELETE -> viewModel.permanentDelete(photo.id)
                    // ... 其他操作
                }
            }
        )
    } else {
        // 正常列表视图
        DragSelectPhotoGrid(
            photos = uiState.photos,
            onPhotoClick = { photoId, index ->
                if (!uiState.isSelectionMode) {
                    // 非选择模式 -> 进入全屏预览
                    fullscreenInitialIndex = index
                    showFullscreen = true
                } else {
                    // 选择模式 -> 切换选中
                    viewModel.toggleSelection(photoId)
                }
            },
            onPhotoLongPress = { photoId, _ ->
                viewModel.enterSelectionMode(photoId)
            },
            // ...
        )
    }
}
```

---

## 四、详细实现步骤

### Step C1: 待定列表排序选项更新

**文件**: `ui/screens/photolist/PhotoListViewModel.kt`

```kotlin
// 1. 更新排序选项获取
private fun getSortOptionsForStatus(status: PhotoStatus): List<PhotoListSortOrder> {
    return when (status) {
        PhotoStatus.MAYBE -> ListSortConfigs.maybeList
        PhotoStatus.TRASH -> ListSortConfigs.trashList
        PhotoStatus.KEEP -> ListSortConfigs.keepList
        else -> listOf(PhotoListSortOrder.DATE_DESC, PhotoListSortOrder.DATE_ASC)
    }
}

// 2. 更新默认排序
private fun getDefaultSortForStatus(status: PhotoStatus): PhotoListSortOrder {
    return when (status) {
        PhotoStatus.MAYBE -> DefaultSortOrders.maybeList
        PhotoStatus.TRASH -> DefaultSortOrders.trashList
        else -> PhotoListSortOrder.DATE_DESC
    }
}

// 3. 实现添加时间排序
private fun sortPhotos(
    photos: List<PhotoEntity>,
    sortOrder: PhotoListSortOrder
): List<PhotoEntity> {
    return when (sortOrder) {
        PhotoListSortOrder.DATE_DESC -> photos.sortedByDescending { it.dateTaken }
        PhotoListSortOrder.DATE_ASC -> photos.sortedBy { it.dateTaken }
        PhotoListSortOrder.ADDED_DESC -> photos.sortedByDescending { it.addedToStatusAt }
        PhotoListSortOrder.ADDED_ASC -> photos.sortedBy { it.addedToStatusAt }
        PhotoListSortOrder.RANDOM -> photos.shuffled(Random(randomSeed))
    }
}
```

### Step C2: 待定列表选择限制

**文件**: `ui/screens/photolist/PhotoListScreen.kt`

```kotlin
// 在 DragSelectPhotoGrid 中处理选择限制
DragSelectPhotoGrid(
    // ...
    onSelectionChanged = { newSelection ->
        if (status == PhotoStatus.MAYBE) {
            // 待定列表有6张限制
            if (newSelection.size > 6) {
                val limited = newSelection.toList().take(6).toSet()
                viewModel.updateSelection(limited)
                // 显示toast
                scope.launch {
                    snackbarHostState.showSnackbar("最多可对比6张照片")
                }
            } else {
                viewModel.updateSelection(newSelection)
            }
        } else {
            viewModel.updateSelection(newSelection)
        }
    },
    // ...
)
```

### Step C3: 待定列表底部操作栏

**文件**: `ui/screens/photolist/PhotoListScreen.kt`

```kotlin
// 待定列表特殊底部栏
if (status == PhotoStatus.MAYBE && uiState.isSelectionMode) {
    MaybeListBottomBar(
        selectedCount = uiState.selectedCount,
        onClear = { viewModel.clearSelection() },
        onCompare = {
            // 进入 LightTable 对比模式
            val selectedIds = uiState.selectedPhotoIds.toList()
            navController.navigate("lighttable/${selectedIds.joinToString(",")}")
        }
    )
}

@Composable
private fun MaybeListBottomBar(
    selectedCount: Int,
    onClear: () -> Unit,
    onCompare: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // 清除按钮
        OutlinedButton(onClick = onClear) {
            Icon(Icons.Default.Clear, null)
            Spacer(Modifier.width(8.dp))
            Text("清除")
        }

        // 对比按钮
        Button(
            onClick = onCompare,
            enabled = selectedCount in 2..6
        ) {
            Icon(Icons.Default.Compare, null)
            Spacer(Modifier.width(8.dp))
            Text("对比 ($selectedCount)")
        }
    }
}
```

### Step D1: 回收站排序和默认值

**文件**: `ui/screens/trash/TrashViewModel.kt`

```kotlin
// 更新默认排序为添加时间倒序
private val _sortOrder = MutableStateFlow(PhotoListSortOrder.ADDED_DESC)

// 添加排序选项
val availableSortOptions = ListSortConfigs.trashList
```

### Step D2: 回收站点击行为修改

**文件**: `ui/screens/trash/TrashScreen.kt`

```kotlin
// 修改: 非选择模式下点击进入全屏预览
DragSelectPhotoGrid(
    photos = uiState.photos,
    onPhotoClick = { photoId, index ->
        if (!uiState.isSelectionMode) {
            // 进入全屏预览 (REQ-034)
            fullscreenInitialIndex = index
            showFullscreen = true
        } else {
            // 选择模式下切换选中
            viewModel.toggleSelection(photoId)
        }
    },
    onPhotoLongPress = { photoId, _ ->
        // 长按进入选择模式 (REQ-035)
        viewModel.enterSelectionMode(photoId)
    },
    // ...
)
```

### Step D3: 回收站底部操作栏

**文件**: `ui/screens/trash/TrashScreen.kt`

```kotlin
// 更新底部操作栏
if (uiState.isSelectionMode) {
    SelectionBottomBar(
        actions = BottomBarConfigs.trashListActions(
            onKeep = { viewModel.moveSelectedToStatus(PhotoStatus.KEEP) },
            onMaybe = { viewModel.moveSelectedToStatus(PhotoStatus.MAYBE) },
            onReset = { viewModel.moveSelectedToStatus(PhotoStatus.UNSORTED) },
            onDelete = {
                // 彻底删除需要确认
                showDeleteConfirmDialog = true
            }
        )
    )
}

// 删除确认弹窗
if (showDeleteConfirmDialog) {
    AlertDialog(
        onDismissRequest = { showDeleteConfirmDialog = false },
        title = { Text("确认彻底删除") },
        text = {
            Text("此操作将永久删除选中的${uiState.selectedCount}张照片，无法恢复。")
        },
        confirmButton = {
            TextButton(
                onClick = {
                    viewModel.permanentDeleteSelected()
                    showDeleteConfirmDialog = false
                }
            ) {
                Text("删除", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = { showDeleteConfirmDialog = false }) {
                Text("取消")
            }
        }
    )
}
```

---

## 五、验证清单

### 模块C: 待定照片列表

| 需求 | 描述 | 实现位置 | 状态 |
|-----|------|---------|------|
| REQ-028 | 排序选项(4种) | PhotoListViewModel.kt | ⏳ |
| REQ-029 | 点击选中(最多6张) | PhotoListScreen.kt | ⏳ |
| REQ-030 | 拖动多选限制6张 | PhotoListScreen.kt | ⏳ |
| REQ-031 | 清除/对比按钮 | MaybeListBottomBar | ⏳ |
| REQ-032 | 对比模式全屏预览 | LightTableScreen.kt | ⏳ |

### 模块D: 回收站照片列表

| 需求 | 描述 | 实现位置 | 状态 |
|-----|------|---------|------|
| REQ-033 | 排序选项(4种) | TrashViewModel.kt | ⏳ |
| REQ-034 | 点击进入全屏预览 | TrashScreen.kt | ⏳ |
| REQ-035 | 长按进入选择模式 | TrashScreen.kt | ⏳ |
| REQ-036 | 底部操作(4项) | TrashScreen.kt | ⏳ |

### 功能测试场景

| 场景 | 列表 | 测试步骤 | 预期结果 |
|-----|------|---------|---------|
| 选择限制 | 待定 | 选中第7张照片 | toast提示最多6张 |
| 拖动限制 | 待定 | 拖动选中超过6张 | 只选中6张并toast |
| 对比可用 | 待定 | 选中2-6张 | 对比按钮可点击 |
| 对比禁用 | 待定 | 选中1张或7张 | 对比按钮禁用 |
| 点击预览 | 回收站 | 非选择模式点击照片 | 进入全屏预览 |
| 长按选择 | 回收站 | 长按照片 | 进入选择模式 |
| 恢复操作 | 回收站 | 选中后点保留 | 照片移至已保留 |
| 彻底删除 | 回收站 | 选中后点彻删 | 弹出确认后删除 |

---

## 六、相关文档链接

- 上级文档: [PLAN_L2_INDEX.md](./PLAN_L2_INDEX.md)
- 需求来源: [REQUIREMENTS_LISTING.md](./REQUIREMENTS_LISTING.md) REQ-028~036
- 依赖模块: [模块A](./PLAN_L2_MODULE_A.md), [模块B](./PLAN_L2_MODULE_B.md)
- 相关模块: 模块E (已保留列表)、模块F (相册列表)
