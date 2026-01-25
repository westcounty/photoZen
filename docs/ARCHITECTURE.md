# PhotoZen 架构设计

> 📅 文档版本: v2.0.0 | 更新日期: 2026-01-25

本文档描述 PhotoZen 应用的整体架构设计，包括状态管理、批量操作、设计系统、动效系统和性能优化等核心模块。

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Kotlin | 2.0 | 主要开发语言 |
| Jetpack Compose | 1.6+ | UI 框架 |
| Material 3 | 1.2+ | 设计系统基础 |
| PicZenTokens | - | 自定义设计 Token |
| PicZenMotion | - | 自定义动效系统 |
| Hilt | 2.51+ | 依赖注入 |
| Room | 2.6+ | 本地数据库 |
| DataStore | 1.1+ | 偏好设置存储 |
| Coil 3 | 3.0+ | 图片加载 |
| Coroutines + Flow | 1.8+ | 异步编程 |

## 架构概览

```
┌─────────────────────────────────────────────────────────────────────┐
│                           UI Layer                                  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌───────────┐  │
│  │  Screens    │  │ Components  │  │  Dialogs    │  │  Bottom   │  │
│  │  (Compose)  │  │  (Compose)  │  │  (Compose)  │  │  Sheets   │  │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └─────┬─────┘  │
└─────────┼────────────────┼───────────────┼────────────────┼────────┘
          │                │               │                │
          └────────────────┴───────┬───────┴────────────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        ViewModel Layer                              │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐  │
│  │  HomeViewModel   │  │ PhotoListVM      │  │ FlowSorterVM     │  │
│  │  - UiState       │  │ - UiState        │  │ - UiState        │  │
│  │  - SubStates     │  │ - Selection      │  │ - Preloader      │  │
│  └────────┬─────────┘  └────────┬─────────┘  └────────┬─────────┘  │
└───────────┼─────────────────────┼─────────────────────┼────────────┘
            │                     │                     │
            └─────────────────────┼─────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     Shared State Layer                              │
│  ┌────────────────────┐  ┌────────────────────┐  ┌───────────────┐  │
│  │ PhotoSelection     │  │    UndoManager     │  │  Snackbar     │  │
│  │   StateHolder      │  │                    │  │   Manager     │  │
│  │  - selectedIds     │  │  - undoStack       │  │  - messages   │  │
│  │  - isSelectionMode │  │  - redo()          │  │  - show()     │  │
│  └────────────────────┘  └────────────────────┘  └───────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
            │                     │                     │
            └─────────────────────┼─────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        Domain Layer                                 │
│  ┌────────────────────┐  ┌────────────────────┐  ┌───────────────┐  │
│  │ PhotoBatchOperation│  │  GetPhotosUseCase  │  │ SortPhoto     │  │
│  │     UseCase        │  │                    │  │   UseCase     │  │
│  │ - batchUpdateStatus│  │ - getByStatus()    │  │ - keepPhoto() │  │
│  │ - batchRestoreFrom │  │ - getByBucketId()  │  │ - trashPhoto()│  │
│  │   Trash()          │  │ - getTotalCount()  │  │ - maybePhoto()│  │
│  └────────────────────┘  └────────────────────┘  └───────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
            │                     │                     │
            └─────────────────────┼─────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         Data Layer                                  │
│  ┌────────────────────┐  ┌────────────────────┐  ┌───────────────┐  │
│  │   PhotoRepository  │  │ PreferencesRepo    │  │ MediaStore    │  │
│  │                    │  │                    │  │  DataSource   │  │
│  │  - Room Database   │  │  - DataStore       │  │  - System API │  │
│  │  - PhotoDao        │  │  - Settings        │  │  - Albums     │  │
│  └────────────────────┘  └────────────────────┘  └───────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
```

## 状态管理架构

### 概述

PhotoZen 采用分层状态管理架构：

1. **子状态（SubState）**: 将大型 UiState 拆分为职责单一的子状态
2. **共享状态持有者（StateHolder）**: 跨 ViewModel 共享的状态
3. **通用状态封装（AsyncState）**: 统一的异步操作状态表示

### 子状态设计

```
HomeUiState
├── PhotoStatsState (照片统计)
│   ├── totalPhotos
│   ├── unsortedCount
│   ├── keepCount
│   ├── trashCount
│   └── maybeCount
├── UiControlState (UI控制)
│   ├── hasPermission
│   ├── syncState: AsyncState
│   ├── shouldShowChangelog
│   └── showSortModeSheet
├── FeatureConfigState (功能配置)
│   ├── photoFilterMode
│   ├── photoClassificationMode
│   └── dailyTaskStatus
└── SmartGalleryState (智能画廊)
    ├── enabled
    ├── analysisProgress
    └── isAnalyzing

FlowSorterUiState
├── SorterPhotosState (照片列表)
├── SorterCountersState (计数器)
├── SorterConfigState (配置)
├── SorterAlbumState (相册模式)
└── SorterDailyTaskState (每日任务)
```

### 共享状态

| 组件 | 作用域 | 用途 |
|------|--------|------|
| `PhotoSelectionStateHolder` | @Singleton | 跨页面照片选择状态 |
| `UndoManager` | @Singleton | 撤销操作管理 |
| `SnackbarManager` | @Singleton | 全局消息提示 |

### AsyncState 使用

```kotlin
sealed interface AsyncState<out T> {
    data object Idle : AsyncState<Nothing>      // 初始/空闲
    data object Loading : AsyncState<Nothing>   // 加载中
    data class Success<T>(val data: T) : AsyncState<T>  // 成功
    data class Error(val message: String, val cause: Throwable? = null) : AsyncState<Nothing>
}

// 使用示例
private val _syncState = MutableStateFlow<AsyncState<SyncResult>>(AsyncState.Idle)

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

## 批量操作架构

所有批量操作通过 `PhotoBatchOperationUseCase` 统一处理：

```kotlin
@Singleton
class PhotoBatchOperationUseCase @Inject constructor(
    private val sortPhotoUseCase: SortPhotoUseCase,
    private val undoManager: UndoManager,
    private val snackbarManager: SnackbarManager,
    private val statisticsRepository: StatisticsRepository
) {
    // 批量状态更新（自动记录撤销、显示 Snackbar）
    suspend fun batchUpdateStatus(photoIds: List<String>, newStatus: PhotoStatus)
    
    // 从回收站恢复
    suspend fun batchRestoreFromTrash(photoIds: List<String>, targetStatus: PhotoStatus)
    
    // 批量永久删除（仅数据库记录）
    suspend fun batchPermanentDelete(photoIds: List<String>)
}
```

### 特性

- **自动撤销记录**: 每次批量操作自动记录到 UndoManager
- **统一反馈**: 通过 SnackbarManager 显示操作结果
- **统计更新**: 自动更新操作统计数据
- **错误处理**: 统一的异常捕获和错误消息

## 性能优化

### 缩略图尺寸策略

```kotlin
object ThumbnailSizePolicy {
    enum class Context(val size: Int) {
        GRID_4_COLUMN(256),   // 4列网格
        GRID_3_COLUMN(360),   // 3列网格
        GRID_2_COLUMN(512),   // 2列网格
        GRID_1_COLUMN(800),   // 单列/详情
        CARD_PREVIEW(800),    // 卡片预览
        LIST_THUMBNAIL(200),  // 列表缩略图
        FULLSCREEN(0)         // 全屏（原始尺寸）
    }
}

// 使用
AsyncImage(
    model = ImageRequest.Builder(context)
        .data(uri)
        .withThumbnailPolicy(ThumbnailSizePolicy.Context.GRID_3_COLUMN)
        .build(),
    // ...
)
```

### 照片预加载

```kotlin
@Singleton
class PhotoPreloader @Inject constructor(
    private val imageLoader: ImageLoader
) {
    // 滑动场景预加载
    fun preloadForSwipe(photos: List<PhotoEntity>, currentIndex: Int, preloadCount: Int)
    
    // 网格场景预加载
    fun preloadForGrid(photos: List<PhotoEntity>, visibleRange: IntRange, columns: Int, extraRows: Int)
}
```

### 动画优化

```kotlin
object AnimationOptimizations {
    // 使用 graphicsLayer 避免重组
    fun Modifier.optimizedTransform(
        translationX: () -> Float = { 0f },
        translationY: () -> Float = { 0f },
        rotation: () -> Float = { 0f },
        scale: () -> Float = { 1f },
        alpha: () -> Float = { 1f }
    ): Modifier
    
    // 滑动卡片变换
    fun Modifier.swipeTransform(
        offsetX: () -> Float,
        offsetY: () -> Float,
        rotation: () -> Float,
        scale: () -> Float
    ): Modifier
}
```

## UI 组件设计

### 底栏配置

```kotlin
object BottomBarConfigs {
    // 自适应配置（根据选择数量切换）
    fun adaptive(
        selectedCount: Int,
        singleSelectActions: () -> List<BottomBarAction>,
        multiSelectActions: () -> List<BottomBarAction>
    ): List<BottomBarAction>
    
    // 预定义配置
    fun keepListSingleSelect(...): List<BottomBarAction>
    fun keepListMultiSelect(...): List<BottomBarAction>
    fun trashListMultiSelect(...): List<BottomBarAction>
    // ...
}
```

### 选择模式组件

| 组件 | 用途 |
|------|------|
| `SelectionTopBar` | 选择模式顶栏（计数、全选、关闭） |
| `SelectionBottomBar` | 选择模式底栏（操作按钮） |
| `DragSelectPhotoGrid` | 支持拖拽选择的照片网格 |

## 数据流

### 照片状态更新流程

```
用户操作 → ViewModel → UseCase → Repository → Room
    ↑                                           │
    └───── StateFlow ← ViewModel ← Flow ────────┘
```

### 选择状态流程

```
用户点击/拖拽
    │
    ▼
ViewModel.toggleSelection()
    │
    ▼
PhotoSelectionStateHolder.toggle()
    │
    ▼
_selectedIds.update { ... }
    │
    ▼
UI 自动更新（通过 combine 组合到 UiState）
```

## 依赖注入

### Module 结构

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // 数据库
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PhotoZenDatabase
    
    // Repository
    @Provides @Singleton
    fun providePhotoRepository(photoDao: PhotoDao): PhotoRepository
}

@Module
@InstallIn(SingletonComponent::class)
object StateModule {
    // 共享状态
    @Provides @Singleton
    fun providePhotoSelectionStateHolder(): PhotoSelectionStateHolder
    
    @Provides @Singleton
    fun provideUndoManager(): UndoManager
    
    @Provides @Singleton
    fun provideSnackbarManager(): SnackbarManager
}
```

## 测试策略

### 单元测试

- ViewModel 测试：使用 TestCoroutineDispatcher
- UseCase 测试：Mock Repository
- StateHolder 测试：验证状态转换

### UI 测试

- Compose UI 测试：使用 ComposeTestRule
- 截图测试：验证 UI 一致性

## 设计系统架构

### 设计 Token 层

PhotoZen 使用自定义设计 Token 系统确保全应用视觉一致性：

```
ui/theme/
├── DesignTokens.kt      # 核心设计 Token
│   └── PicZenTokens
│       ├── Radius       # 圆角 Token (XS~Full)
│       ├── Spacing      # 间距 Token (XXS~XXXL)
│       ├── Elevation    # 阴影层级 (Level0~Level5)
│       ├── IconSize     # 图标尺寸 (XS~XL)
│       ├── Alpha        # 透明度常量
│       └── ComponentSize # 组件尺寸规范
│
├── MotionTokens.kt      # 动效 Token
│   └── PicZenMotion
│       ├── Duration     # 时长常量 (Instant~Deliberate)
│       ├── Easing       # 缓动曲线 (Standard/Emphasized)
│       ├── Springs      # 弹簧动画 (snappy/playful/gentle)
│       ├── Specs        # 预定义动画规格
│       └── Delay        # 延迟常量 (Stagger)
│
└── Color.kt             # 颜色系统扩展
    ├── PicZenDarkSurfaces  # 6 级表面层次
    └── PicZenActionColors  # Keep/Trash/Maybe 操作色
```

### 增强组件层

基于设计 Token 的增强组件：

| 组件 | 文件 | 功能 |
|------|------|------|
| EnhancedCard | `components/EnhancedCard.kt` | 动态缩放、阴影、光泽效果 |
| FloatingBottomBar | `components/FloatingBottomBar.kt` | 毛玻璃浮动底栏 |
| PressableButton | `components/PressableButton.kt` | 按压微缩放按钮 |
| SelectableListItem | `components/SelectableListItem.kt` | 选中反馈列表项 |
| PhotoStatusPill | `components/PhotoStatusBadge.kt` | 渐变状态胶囊 |
| AnimatedEmptyState | `components/EmptyState.kt` | 浮动空状态 |

### 动画层

```
ui/animation/
├── ListAnimations.kt    # 错落入场动画
│   ├── staggeredEntry() # 列表项错落进入
│   └── AnimatedListItem # 封装组件
│
└── PageTransitions.kt   # 页面过渡动画
    ├── PageTransitions  # 过渡定义
    │   ├── horizontalEnter/Exit  # 水平滑动
    │   ├── modalEnter/Exit       # 模态弹出
    │   ├── detailEnter/Exit      # 详情缩放
    │   └── fullscreenEnter/Exit  # 全屏过渡
    └── NavTransitions   # 导航过渡工厂
```

## 相关文档

- [设计系统规范](DESIGN_SYSTEM.md) - 完整的设计系统文档
- [状态管理指南](STATE_MANAGEMENT.md) - 详细的状态管理使用说明
- [组件使用示例](COMPONENT_USAGE.md) - UI 组件的使用示例
- [手势规范](GESTURE_SPEC.md) - 手势交互设计规范
- [交互规范](INTERACTION_SPEC.md) - 用户交互设计规范
