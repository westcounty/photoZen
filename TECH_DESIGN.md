# PhotoZen 图禅 - 技术设计文档

> 📅 文档版本: v1.6.0 | 更新日期: 2026-01-18

## 1. 技术架构概览

### 1.1 技术栈

| 层级 | 技术选型 | 说明 |
|:----:|:---------|:-----|
| 语言 | Kotlin 2.0 | 100% Kotlin |
| UI 框架 | Jetpack Compose | 声明式 UI |
| 设计系统 | Material 3 | Material You 动态主题 |
| 架构模式 | MVVM + Clean Architecture | 分层架构 |
| 依赖注入 | Hilt | 编译时 DI |
| 异步处理 | Coroutines + Flow | 响应式数据流 |
| 导航 | Compose Navigation | Type-Safe 导航 |
| 本地存储 | Room + DataStore | 结构化数据 + 偏好设置 |
| 图片加载 | Coil 3 | Kotlin-first 图片库 |
| 最低 SDK | 26 (Android 8.0) | |
| 目标 SDK | 36 (Android 16) | |

### 1.2 项目结构

```
app/src/main/java/com/example/photozen/
├── data/                       # 数据层
│   ├── local/                  # 本地存储
│   │   ├── dao/                # Room DAO
│   │   ├── entity/             # Room Entity
│   │   ├── converter/          # 类型转换器
│   │   └── AppDatabase.kt      # 数据库定义
│   ├── model/                  # 数据模型
│   ├── repository/             # 数据仓库
│   └── source/                 # 数据源
│       └── MediaStoreDataSource.kt
├── domain/                     # 领域层
│   ├── usecase/                # 用例
│   ├── AchievementManager.kt   # 成就管理器
│   └── EventGrouper.kt         # 时间线事件分组算法
├── di/                         # 依赖注入模块
├── navigation/                 # 导航定义
├── ui/                         # UI 层
│   ├── components/             # 通用组件
│   ├── screens/                # 各功能页面
│   │   ├── home/               # 首页
│   │   ├── flowsorter/         # 滑动整理
│   │   ├── lighttable/         # 照片对比
│   │   ├── timeline/           # 时间线
│   │   ├── workflow/           # 一站式整理
│   │   ├── albums/             # 相册管理
│   │   ├── photolist/          # 照片列表
│   │   ├── trash/              # 回收站
│   │   ├── settings/           # 设置
│   │   └── share/              # 系统分享
│   └── theme/                  # 主题定义
├── service/                    # 后台服务
├── receiver/                   # 广播接收器
├── widget/                     # 桌面小组件
├── util/                       # 工具类
├── MainActivity.kt             # 主 Activity
└── PicZenApplication.kt        # Application
```

---

## 2. 数据层设计

### 2.1 数据库 Schema (Room)

#### PhotoEntity - 照片表
```kotlin
@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey val id: String,              // UUID 或 MediaStore ID
    val systemUri: String,                    // content://media/... URI
    val status: PhotoStatus,                  // UNSORTED, KEEP, MAYBE, TRASH
    @Embedded val cropState: CropState,       // 裁切参数（非破坏性）
    val isVirtualCopy: Boolean,               // 是否虚拟副本
    val parentId: String?,                    // 父照片 ID（虚拟副本用）
    val displayName: String,                  // 文件名
    val size: Long,                           // 文件大小
    val width: Int, val height: Int,          // 尺寸
    val mimeType: String,                     // MIME 类型
    val dateTaken: Long,                      // 拍摄时间（毫秒）
    val dateAdded: Long,                      // 添加时间（秒）
    val bucketId: String?,                    // MediaStore 相册 ID
    val latitude: Double?, val longitude: Double?,  // GPS 坐标
    val gpsScanned: Boolean,                  // 是否已扫描 GPS
    val createdAt: Long, val updatedAt: Long  // 记录时间戳
)
```

#### DailyStats - 每日统计表
```kotlin
@Entity(tableName = "daily_stats")
data class DailyStats(
    @PrimaryKey val date: String,             // YYYY-MM-DD
    val sortedCount: Int,                     // 当日整理数
    val keptCount: Int,                       // 当日保留数
    val trashedCount: Int,                    // 当日删除数
    val maybeCount: Int                       // 当日待定数
)
```

#### AlbumBubbleEntity - 用户相册列表
```kotlin
@Entity(tableName = "album_bubbles")
data class AlbumBubbleEntity(
    @PrimaryKey val bucketId: String,         // MediaStore bucket ID
    val displayName: String,                  // 相册名
    val sortOrder: Int,                       // 排序序号
    val addedAt: Long                         // 添加时间
)
```

### 2.2 偏好设置 (DataStore)

**PreferencesRepository** 管理所有用户设置：

```kotlin
// 筛选设置
PhotoFilterMode: ALL, CAMERA_ONLY, EXCLUDE_CAMERA, CUSTOM

// 每日任务
dailyTaskEnabled: Boolean
dailyTaskTarget: Int (1-1000)
dailyTaskMode: FLOW, QUICK
progressNotificationEnabled: Boolean

// 相册分类
cardSortingAlbumEnabled: Boolean        // 滑动时显示相册标签
albumAddAction: COPY, MOVE              // 默认操作
albumTagSize: Float (0.8-1.2)           // 标签大小

// 外观
themeMode: DARK, LIGHT, SYSTEM
swipeSensitivity: Float (0.5-1.5)

// 成就数据
totalSortedCount, maxCombo, consecutiveDays, ...
```

### 2.3 MediaStore 数据源

**MediaStoreDataSource** 负责与系统 MediaStore 交互：

```kotlin
class MediaStoreDataSource {
    // 扫描系统照片到本地数据库
    suspend fun syncPhotosToDatabase()
    
    // 获取系统相册列表
    suspend fun getAlbums(): List<Album>
    
    // 复制照片到相册（保留 EXIF）
    suspend fun copyPhotoToAlbum(uri: Uri, albumPath: String): Result
    
    // 移动照片到相册
    suspend fun movePhotoToAlbum(uri: Uri, albumPath: String): Result
    
    // 删除照片（返回 PendingIntent 用于系统确认）
    suspend fun deletePhotos(uris: List<Uri>): IntentSender?
}
```

---

## 3. 领域层设计

### 3.1 Use Cases

| Use Case | 职责 |
|:---------|:-----|
| `GetUnsortedPhotosUseCase` | 获取未筛选照片列表，支持筛选条件 |
| `SortPhotoUseCase` | 修改照片状态，更新统计 |
| `GetDailyTaskStatusUseCase` | 获取今日任务进度 |
| `AlbumOperationsUseCase` | 相册复制/移动操作 |
| `ManageTrashUseCase` | 回收站恢复/永久删除 |
| `CreateVirtualCopyUseCase` | 创建虚拟副本 |
| `UpdateCropStateUseCase` | 更新裁切参数 |
| `SyncPhotosUseCase` | 同步 MediaStore 变更 |

### 3.2 事件分组算法 (EventGrouper)

时间线智能分组核心逻辑：

```kotlin
class EventGrouper {
    fun groupByEvent(
        photos: List<PhotoEntity>,
        timeGapThreshold: Long = 4 * 60 * 60 * 1000  // 4小时
    ): List<PhotoGroup> {
        // 1. 按拍摄时间排序
        // 2. 相邻照片时间差 > 4小时，则分为新组
        // 3. 如有 GPS 数据，距离变化 > 阈值也分组
        // 4. 返回分组列表，每组包含时间范围和照片列表
    }
}
```

### 3.3 成就管理器 (AchievementManager)

```kotlin
@Singleton
class AchievementManager {
    // 监听偏好设置变化，检测成就解锁
    val achievementUnlockEvents: SharedFlow<AchievementUnlockEvent>
    
    // 当前庆祝状态（用于触发动画）
    val currentCelebration: StateFlow<AchievementUnlockEvent?>
}
```

---

## 4. UI 层设计

### 4.1 导航结构

```kotlin
sealed interface Screen {
    data object Home : Screen
    data class FlowSorter(
        isDailyTask: Boolean,
        targetCount: Int,
        albumBucketId: String?,
        initialListMode: Boolean
    ) : Screen
    data object LightTable : Screen
    data class Workflow(isDailyTask: Boolean, targetCount: Int) : Screen
    data object Timeline : Screen
    data object AlbumBubble : Screen
    data class AlbumPhotoList(bucketId: String, albumName: String) : Screen
    data class PhotoList(statusName: String) : Screen
    data object Trash : Screen
    data object Settings : Screen
    data class ShareCopy(urisJson: String) : Screen
    data class ShareCompare(urisJson: String) : Screen
    // ... 更多
}
```

### 4.2 ViewModel 状态管理模式

所有 ViewModel 采用统一的状态管理模式：

```kotlin
@HiltViewModel
class ExampleViewModel @Inject constructor(
    private val someUseCase: SomeUseCase,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    // UI 状态
    private val _uiState = MutableStateFlow(ExampleUiState())
    val uiState: StateFlow<ExampleUiState> = _uiState.asStateFlow()
    
    // 事件处理
    fun onAction(action: ExampleAction) {
        viewModelScope.launch {
            when (action) {
                is ExampleAction.LoadData -> loadData()
                is ExampleAction.UpdateItem -> updateItem(action.item)
            }
        }
    }
    
    // 更新状态
    private fun updateState(update: (ExampleUiState) -> ExampleUiState) {
        _uiState.update(update)
    }
}
```

### 4.3 关键组件

#### SwipeablePhotoCard - 可滑动照片卡片
```kotlin
@Composable
fun SwipeablePhotoCard(
    photo: PhotoEntity,
    onSwipe: (SwipeDirection) -> Unit,
    showAlbumTags: Boolean,
    albums: List<AlbumBubbleEntity>,
    onAlbumClick: (AlbumBubbleEntity) -> Unit
)
// 使用 Animatable 实现 Spring 动画
// pointerInput 处理手势
// 根据偏移量计算方向和透明度
```

#### StaggeredPhotoGrid - 瀑布流网格
```kotlin
@Composable
fun StaggeredPhotoGrid(
    photos: List<PhotoEntity>,
    columns: Int,
    onPhotoClick: (PhotoEntity) -> Unit,
    selectionEnabled: Boolean,
    selectedIds: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    enableDragSelect: Boolean  // 长按拖动多选
)
```

#### ComboOverlay - 连击显示
```kotlin
@Composable
fun ComboOverlay(
    comboCount: Int,
    modifier: Modifier
)
// 根据 comboCount 调整颜色、大小、动画
// x1-x9: 白色, x10-x19: 橙色, x20+: 红色火焰
```

---

## 5. 一站式整理流程 (Workflow)

### 5.1 状态机设计

```kotlin
enum class WorkflowStage {
    SWIPE,      // 筛选阶段
    COMPARE,    // 对比待定阶段
    CLASSIFY,   // 分类到相册阶段（仅当 cardSortingAlbumEnabled=false）
    TRASH,      // 清理回收站阶段
    VICTORY     // 胜利页面
}
```

### 5.2 动态阶段列表

```kotlin
val stageList: List<WorkflowStage>
    get() = if (cardSortingAlbumEnabled) {
        // 3 阶段模式
        listOf(SWIPE, COMPARE, TRASH, VICTORY)
    } else {
        // 4 阶段模式
        listOf(SWIPE, COMPARE, CLASSIFY, TRASH, VICTORY)
    }
```

### 5.3 阶段跳过逻辑

```kotlin
fun shouldSkipStage(stage: WorkflowStage): Boolean = when (stage) {
    COMPARE -> sessionMaybePhotos.isEmpty()      // 无待定照片
    CLASSIFY -> sessionKeepPhotos.isEmpty()      // 无保留照片
    TRASH -> sessionTrashPhotos.isEmpty()        // 无回收站照片
    else -> false
}
```

### 5.4 会话统计 (WorkflowStats)

```kotlin
data class WorkflowStats(
    val totalSorted: Int,
    val keptCount: Int,
    val trashedCount: Int,
    val maybeCount: Int,
    val maxCombo: Int,
    val startTime: Long,
    val endTime: Long?,
    val sessionMaybePhotoIds: Set<String>,
    val sessionKeepPhotoIds: Set<String>,
    val sessionTrashPhotoIds: Set<String>,
    val classifiedToAlbumCount: Int,
    val permanentlyDeletedCount: Int,
    val restoredFromTrashCount: Int
)
```

---

## 6. 系统分享集成

### 6.1 Manifest 配置

```xml
<activity android:name=".MainActivity">
    <intent-filter>
        <action android:name="android.intent.action.SEND" />
        <action android:name="android.intent.action.SEND_MULTIPLE" />
        <category android:name="android.intent.category.DEFAULT" />
        <data android:mimeType="image/*" />
    </intent-filter>
</activity>
```

### 6.2 Intent 处理

```kotlin
// MainActivity.kt
private fun handleShareIntent(intent: Intent) {
    when (intent.action) {
        Intent.ACTION_SEND -> handleSingleImage(intent)
        Intent.ACTION_SEND_MULTIPLE -> handleMultipleImages(intent)
    }
}
```

---

## 7. 后台服务

### 7.1 DailyProgressService - 进度通知服务

```kotlin
class DailyProgressService : Service() {
    // 前台服务，显示常驻通知
    // 每分钟更新进度显示
    // 点击通知跳转 App
    
    companion object {
        fun start(context: Context)
        fun stop(context: Context)
        fun updateProgress(context: Context, current: Int, target: Int)
    }
}
```

### 7.2 BootReceiver - 开机启动

```kotlin
class BootReceiver : BroadcastReceiver() {
    // 开机后恢复进度通知服务
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // 检查设置，启动服务
        }
    }
}
```

---

## 8. 性能优化

### 8.1 图片加载
- Coil 自动内存/磁盘缓存
- 缩略图使用 `size(200, 200)` 限制解码大小
- 全屏预览使用原图

### 8.2 数据库查询
- 分页加载：`LIMIT 500` + Flow 监听变化
- 索引优化：status, date_added, bucket_id 等字段建索引
- 使用 `distinctUntilChanged()` 避免重复发射

### 8.3 列表渲染
- LazyVerticalStaggeredGrid + key 参数优化重组
- 避免在 Composable 中创建新对象
- 使用 `remember` 缓存计算结果

---

## 9. 开发规范

### 9.1 版本号规则
```
w.x.y.z
├── w: 大版本号（重大功能更新）
├── x: 小版本号（功能更新，需更新 CHANGELOG）
├── y: 修复版本号（Bug 修复）
└── z: 构建号（每次构建自动 +1）
```

### 9.2 分支管理
- `main`: 主分支，所有非 AI 功能开发
- `explore/smart-gallery`: 实验分支，AI 相关功能
- 主分支变更需合并到实验分支
- 实验分支仅在明确要求时合并回主分支

### 9.3 代码风格
- 使用 Kotlin Flow 进行数据观察
- ViewModel 只暴露 StateFlow，不暴露 MutableStateFlow
- Composable 函数遵循 Android 官方命名规范
- 使用 Material 3 组件

---

## 10. 关键文件索引

### 快速定位功能代码

| 功能 | 主要文件 |
|:-----|:--------|
| 滑动整理 | `flowsorter/FlowSorterScreen.kt`, `SwipeablePhotoCard.kt` |
| 照片对比 | `lighttable/LightTableScreen.kt`, `SyncZoomImage.kt` |
| 时间线 | `timeline/TimelineScreen.kt`, `TimelineViewModel.kt` |
| 一站式整理 | `workflow/WorkflowScreen.kt`, `WorkflowViewModel.kt` |
| 相册管理 | `albums/AlbumBubbleScreen.kt`, `AlbumPhotoListScreen.kt` |
| 设置 | `settings/SettingsScreen.kt`, `PreferencesRepository.kt` |
| 成就 | `AchievementManager.kt`, `AchievementSystem.kt` |
| 导航 | `navigation/Screen.kt`, `PicZenNavHost.kt` |
| 数据库 | `data/local/AppDatabase.kt`, `dao/*.kt`, `entity/*.kt` |

### 添加新功能步骤

1. **定义导航**：在 `Screen.kt` 添加新的 Screen
2. **创建页面**：在 `ui/screens/` 下创建 Screen + ViewModel
3. **注册路由**：在 `PicZenNavHost.kt` 添加 composable 路由
4. **数据层**：如需新数据，添加 Entity、DAO、UseCase
5. **测试**：确保正常导航和数据流

---

## 11. 待优化项

### 性能
- [ ] 大量照片时首页加载优化
- [ ] 虚拟列表滚动性能

### 功能
- [ ] 云同步支持
- [ ] 照片编辑功能扩展（滤镜、调色）
- [ ] AI 标签自动分类（实验分支）

### 体验
- [ ] 更多自定义手势
- [ ] 动画效果增强
- [ ] 多语言支持
