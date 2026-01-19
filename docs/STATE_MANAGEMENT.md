# 状态管理指南

PhotoZen 采用分层状态管理架构，本指南说明何时以及如何使用各种状态管理组件。

## 架构概览

```
┌─────────────────────────────────────────────────────────────┐
│                         UI Layer                            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │   Screen    │  │  BottomBar  │  │   SelectionTopBar   │ │
│  └──────┬──────┘  └──────┬──────┘  └──────────┬──────────┘ │
└─────────┼────────────────┼─────────────────────┼────────────┘
          │                │                     │
          ▼                ▼                     ▼
┌─────────────────────────────────────────────────────────────┐
│                      ViewModel Layer                        │
│  ┌─────────────────────────────────────────────────────────┐│
│  │                    UiState (子状态组合)                  ││
│  │  ├── PhotoStatsState (统计)                             ││
│  │  ├── UiControlState (UI控制)                            ││
│  │  └── ConfigState (配置)                                 ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────┐
│                   Shared State Layer                        │
│  ┌───────────────────────┐  ┌───────────────────────────┐  │
│  │ PhotoSelectionState   │  │      UndoManager          │  │
│  │      Holder           │  │                           │  │
│  └───────────────────────┘  └───────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────┐
│                     UseCase Layer                           │
│  ┌───────────────────────────────────────────────────────┐  │
│  │           PhotoBatchOperationUseCase                  │  │
│  │  - 批量状态更新                                         │  │
│  │  - 自动撤销记录                                         │  │
│  │  - Snackbar 反馈                                       │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

## 1. 子状态设计

### 何时使用子状态

当 UiState 字段超过 10 个时，考虑拆分为子状态：

```kotlin
// ❌ 不推荐：字段过多，难以维护
data class BigUiState(
    val photos: List<PhotoEntity> = emptyList(),
    val totalCount: Int = 0,
    val keepCount: Int = 0,
    val trashCount: Int = 0,
    val maybeCount: Int = 0,
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val error: String? = null,
    val hasPermission: Boolean = false,
    // ... 更多字段
)

// ✓ 推荐：拆分为子状态
data class CleanUiState(
    val stats: PhotoStatsState = PhotoStatsState(),
    val uiControl: UiControlState = UiControlState(),
    val config: ConfigState = ConfigState()
) {
    // 便捷访问器（向后兼容）
    val totalCount: Int get() = stats.totalPhotos
    val isLoading: Boolean get() = uiControl.isLoading
}
```

### 子状态示例

```kotlin
// 照片统计子状态
data class PhotoStatsState(
    val totalPhotos: Int = 0,
    val unsortedCount: Int = 0,
    val keepCount: Int = 0,
    val trashCount: Int = 0,
    val maybeCount: Int = 0
) {
    companion object {
        val EMPTY = PhotoStatsState()
    }
}

// UI 控制子状态
data class UiControlState(
    val isLoading: Boolean = false,
    val syncState: AsyncState<String?> = AsyncState.Idle,
    val hasPermission: Boolean = false
) {
    companion object {
        val EMPTY = UiControlState()
    }
}
```

## 2. PhotoSelectionStateHolder

### 何时使用

当页面需要照片选择功能时，使用 `PhotoSelectionStateHolder` 而非在 ViewModel 中自己管理选择状态。

### 使用方式

```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val selectionStateHolder: PhotoSelectionStateHolder
) : ViewModel() {
    
    // 在 UiState combine 中引入选择状态
    val uiState: StateFlow<MyUiState> = combine(
        _dataFlow,
        selectionStateHolder.selectedIds,
        selectionStateHolder.isSelectionMode
    ) { data, selectedIds, isSelectionMode ->
        MyUiState(
            data = data,
            selectedIds = selectedIds,
            isSelectionMode = isSelectionMode
        )
    }.stateIn(...)
    
    // 选择操作直接委托
    fun toggleSelection(id: String) = selectionStateHolder.toggle(id)
    fun selectAll(ids: List<String>) = selectionStateHolder.selectAll(ids)
    fun clearSelection() = selectionStateHolder.clear()
    
    // 进入页面时清空选择，避免与其他页面冲突
    init {
        selectionStateHolder.clear()
    }
    
    // 离开页面时清空选择
    override fun onCleared() {
        super.onCleared()
        selectionStateHolder.clear()
    }
}
```

### 可用 API

| 方法 | 说明 |
|------|------|
| `toggle(id: String)` | 切换单个照片的选中状态 |
| `select(id: String)` | 选中单个照片 |
| `deselect(id: String)` | 取消选中单个照片 |
| `selectAll(ids: List<String>)` | 全选 |
| `selectMultiple(ids: Collection<String>)` | 选中多个 |
| `setSelection(ids: Set<String>)` | 设置选中集合（用于拖拽选择） |
| `clear()` | 清空选择 |
| `hasSelection()` | 是否有选中 |
| `isSelected(id: String)` | 某照片是否选中 |
| `getSelectedList()` | 获取选中 ID 列表 |

## 3. PhotoBatchOperationUseCase

### 何时使用

当执行批量照片操作时，使用此 UseCase 获得：
- 自动撤销记录
- 统一 Snackbar 反馈
- 统计更新

### 使用方式

```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val selectionStateHolder: PhotoSelectionStateHolder,
    private val batchOperationUseCase: PhotoBatchOperationUseCase
) : ViewModel() {
    
    fun moveSelectedToKeep() {
        val selectedIds = selectionStateHolder.getSelectedList()
        if (selectedIds.isEmpty()) return
        
        viewModelScope.launch {
            // UseCase 内部处理撤销、Snackbar、统计
            batchOperationUseCase.batchUpdateStatus(selectedIds, PhotoStatus.KEEP)
            selectionStateHolder.clear()
        }
    }
}
```

### 可用操作

| 方法 | 说明 |
|------|------|
| `batchUpdateStatus(ids, newStatus)` | 批量更新状态 |
| `batchKeep(ids)` | 批量保留 |
| `batchMaybe(ids)` | 批量待定 |
| `batchTrash(ids)` | 批量移入回收站 |
| `batchReset(ids)` | 批量重置为未整理 |
| `batchRestoreFromTrash(ids, targetStatus)` | 从回收站恢复 |
| `batchPermanentDelete(ids)` | 批量永久删除（仅数据库记录） |

## 4. AsyncState

### 何时使用

当需要表示异步操作状态（加载中、成功、失败）时：

```kotlin
sealed interface AsyncState<out T> {
    data object Idle : AsyncState<Nothing>      // 初始/空闲状态
    data object Loading : AsyncState<Nothing>   // 加载中
    data class Success<T>(val data: T) : AsyncState<T>  // 成功
    data class Error(val message: String, val cause: Throwable? = null) : AsyncState<Nothing>
}
```

### 在 ViewModel 中使用

```kotlin
private val _syncState = MutableStateFlow<AsyncState<SyncResult>>(AsyncState.Idle)
val syncState: StateFlow<AsyncState<SyncResult>> = _syncState.asStateFlow()

fun sync() {
    viewModelScope.launch {
        _syncState.value = AsyncState.Loading
        try {
            val result = syncUseCase()
            _syncState.value = AsyncState.Success(result)
        } catch (e: Exception) {
            _syncState.value = AsyncState.Error(e.message ?: "同步失败", e)
        }
    }
}
```

### 在 Composable 中使用

```kotlin
when (val state = uiState.syncState) {
    is AsyncState.Idle -> { /* 初始状态，不显示任何内容 */ }
    is AsyncState.Loading -> { 
        CircularProgressIndicator() 
    }
    is AsyncState.Success -> { 
        Text("同步完成: ${state.data}") 
    }
    is AsyncState.Error -> { 
        Text("错误: ${state.message}", color = Color.Red) 
    }
}

// 或使用便捷扩展
state.Render(
    onIdle = { /* ... */ },
    onLoading = { CircularProgressIndicator() },
    onSuccess = { data -> Text("成功: $data") },
    onError = { message, _ -> Text("错误: $message") }
)
```

## 5. 命名规范

### StateFlow 命名

```kotlin
// ✓ 正确
private val _photoStatsState = MutableStateFlow(PhotoStatsState())
val photoStatsState: StateFlow<PhotoStatsState> = _photoStatsState.asStateFlow()

private val _syncState = MutableStateFlow<AsyncState<SyncResult>>(AsyncState.Idle)
val syncState: StateFlow<AsyncState<SyncResult>> = _syncState.asStateFlow()

// ✗ 避免
private val _state = MutableStateFlow(...) // 太模糊
private val _data = MutableStateFlow(...)  // 不明确
```

### UiState 结构

```kotlin
data class XxxUiState(
    // 1. 核心数据
    val photos: List<PhotoEntity> = emptyList(),
    
    // 2. 子状态（如果需要）
    val stats: StatsSubState = StatsSubState(),
    
    // 3. UI 控制
    val isLoading: Boolean = false,
    val message: String? = null,
    
    // 4. 配置
    val gridColumns: Int = 2
) {
    // 计算属性
    val isEmpty: Boolean get() = photos.isEmpty()
    val photoCount: Int get() = photos.size
}
```

## 6. 最佳实践

### DO ✓

1. **使用 StateHolder 共享选择状态** - 避免在每个 ViewModel 中重复选择逻辑
2. **使用 BatchUseCase 执行批量操作** - 获得撤销、反馈、统计支持
3. **使用 AsyncState 表示异步状态** - 比 Boolean + String 更清晰
4. **在 init 和 onCleared 中管理选择状态** - 确保页面间状态隔离

### DON'T ✗

1. **不要在 InternalState 中存储选择相关字段** - 使用 StateHolder
2. **不要手动实现批量操作的撤销逻辑** - 使用 BatchUseCase
3. **不要使用 `isLoading: Boolean` + `error: String?`** - 使用 AsyncState
4. **不要在 Composable 中创建 ViewModel** - 使用 hiltViewModel()

## 7. 迁移指南

### 从旧选择逻辑迁移

```kotlin
// 旧代码
private data class InternalState(
    val selectedIds: Set<String> = emptySet(),  // ❌ 删除
    val isSelectionMode: Boolean = false         // ❌ 删除
)

// 新代码
@HiltViewModel
class MyViewModel @Inject constructor(
    private val selectionStateHolder: PhotoSelectionStateHolder  // ✓ 注入
) {
    // 在 combine 中使用 selectionStateHolder.selectedIds
    // 选择操作委托给 selectionStateHolder
}
```

### 从旧批量操作迁移

```kotlin
// 旧代码
fun moveSelectedToKeep() {
    selectedIds.forEach { sortPhotoUseCase.keepPhoto(it) }  // ❌ 手动循环
}

// 新代码
fun moveSelectedToKeep() {
    val ids = selectionStateHolder.getSelectedList()
    batchOperationUseCase.batchUpdateStatus(ids, PhotoStatus.KEEP)  // ✓ 使用 UseCase
}
```
