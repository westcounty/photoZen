# 模块A: 通用视图组件增强 - 详细实施方案

> 父文档: [PLAN_L2_INDEX.md](./PLAN_L2_INDEX.md)
> 需求文档: [REQUIREMENTS_LISTING.md](./REQUIREMENTS_LISTING.md)
> 涉及需求: REQ-001~011, REQ-022~027 (共17个需求点)
> 状态: ✅ L3实现中 (2026-01-25)

---

## 一、原始需求摘录

### 1.1 网格视图 (REQ-001~005)
```
REQ-001: 网格视图基础定义
- 每个照片显示为正方形（居中裁切以保留最多内容）

REQ-002: 网格视图列数切换
- 支持2列、3列、4列、5列切换，默认3列

REQ-003: 双指缩放手势
- 双指扩大减少列数，双指收缩增加列数
- 到最小/最大时继续手势有回弹效果

REQ-004: 列数切换动效
- 改变列数时需要丝滑的过渡动效

REQ-005: 长按拖动多选
- 支持长按后滑动手指多选
- 选中连续的所有照片
- 不清除之前选择，结果累计
```

### 1.2 瀑布流视图 (REQ-006~010)
```
REQ-006: 瀑布流视图基础定义
- 照片宽度一致，保持原长宽比
- 每张照片内容完整显示

REQ-007: 瀑布流视图列数切换
- 支持1列、2列、3列、4列、5列切换，默认3列

REQ-008: 双指缩放手势（同REQ-003）

REQ-009: 列数切换动效（同REQ-004）

REQ-010: 瀑布流多选限制
- 不支持长按拖动多选，只能点击单选
```

### 1.3 其他通用规则 (REQ-011, REQ-022~027)
```
REQ-011: 照片真实时间
- 优先取拍照时间，取不到则用文件创建时间

REQ-022: 排序按钮交互一致性
- 不同页面排序按钮交互形式一致

REQ-023: 排序方式独立记忆
- 每个列表类型独立记忆排序方式

REQ-024: 底部操作栏布局
- 图标+文字上下排列
- 文字精简表意准确

REQ-025: 底部操作栏滑动
- 按钮展示不全时支持左右滑动
- 需有视觉提示告知用户右侧有更多按钮

REQ-026: 复制照片功能
- 保留所有EXIF信息
- Toast提示成功/失败

REQ-027: 视图模式切换按钮
- 在网格和瀑布流之间切换
```

---

## 二、现有实现分析

### 2.1 已有实现 ✅

| 功能 | 文件位置 | 备注 |
|-----|---------|------|
| 网格视图基础 | `DragSelectPhotoGrid.kt` | LazyVerticalGrid |
| 瀑布流视图基础 | `DragSelectPhotoGrid.kt` | LazyVerticalStaggeredGrid |
| 长按拖动多选 | `DragSelectPhotoGrid.kt:182-430` | 完整实现 |
| 瀑布流禁用拖动 | `DragSelectPhotoGrid.kt` | enableDragSelect=false |
| 照片真实时间 | `PhotoEntity.kt` | dateTaken字段 |
| 底部操作栏 | `BottomActionBar.kt` | 支持配置 |
| 复制功能 | `CopyPhotoUseCase.kt` | 保留EXIF |
| 视图模式切换 | `PhotoListViewModel.kt` | toggleGridMode() |

### 2.2 需要新增/修改

| 功能 | 现状 | 目标 |
|-----|------|------|
| 网格列数范围 | 1-4列 | 2-5列 |
| 瀑布流列数范围 | 1-4列 | 1-5列 |
| 双指缩放切换列数 | 无 | 新增 |
| 列数切换动效 | 无 | 新增 |
| 排序按钮交互统一 | 各页面不一致 | 统一下拉菜单 |
| 排序方式记忆 | 部分实现 | 完善DataStore持久化 |
| 底部栏滑动提示 | 无 | 新增渐变遮罩 |

---

## 三、技术方案设计

### 3.1 双指缩放切换列数 - 最佳实践参考

**参考应用**: Google Photos, iOS Photos

**核心实现原理**:
```kotlin
// 使用 transformable modifier 检测缩放手势
Modifier.pointerInput(Unit) {
    detectTransformGestures { centroid, pan, zoom, rotation ->
        // zoom > 1.0 表示双指张开 → 减少列数
        // zoom < 1.0 表示双指收缩 → 增加列数
        // 使用累积缩放值 + 阈值触发
    }
}
```

**关键设计点**:

1. **手势识别与滚动协调**
   - 使用 `nestedScroll` 协调双指缩放与列表滚动
   - 缩放手势优先级高于滚动

2. **阈值触发机制**
   - 累积缩放因子达到阈值(如1.3x或0.7x)时触发列数变更
   - 触发后重置累积值

3. **回弹效果**
   - 到达最大/最小列数后，使用 `animateFloatAsState` 回弹
   - 配合震动反馈提示边界

4. **过渡动效**
   - 使用 `AnimatedContent` 或自定义动画
   - 平滑过渡照片大小和位置

### 3.2 新增文件清单

```
ui/components/
├── ZoomablePhotoGrid.kt          # 新增: 支持双指缩放的网格容器
├── ColumnCountAnimator.kt        # 新增: 列数切换动画控制器
├── SortDropdownMenu.kt           # 新增: 统一排序下拉菜单组件
└── ScrollIndicator.kt            # 新增: 底部栏滑动提示组件

ui/state/
└── GridPreferencesManager.kt     # 新增: 网格偏好设置管理(列数记忆)
```

### 3.3 修改文件清单

```
ui/components/
├── DragSelectPhotoGrid.kt        # 修改: 集成缩放手势
├── StaggeredPhotoGrid.kt         # 修改: 集成缩放手势
└── BottomActionBar.kt            # 修改: 添加滑动提示

ui/screens/photolist/
└── PhotoListViewModel.kt         # 修改: 列数范围调整

data/local/datastore/
└── UserPreferencesDataStore.kt   # 修改: 添加排序记忆字段
```

---

## 四、详细实现步骤

### Step A1: 双指缩放手势组件

**文件**: `ui/components/ZoomablePhotoGrid.kt`

```kotlin
/**
 * 支持双指缩放切换列数的照片网格容器
 *
 * 最佳实践参考: Google Photos
 * - 双指张开: 减少列数(放大照片)
 * - 双指收缩: 增加列数(缩小照片)
 * - 边界回弹: 到达最大/最小列数时有回弹效果
 */
@Composable
fun ZoomablePhotoGrid(
    columns: Int,
    onColumnsChange: (Int) -> Unit,
    minColumns: Int = 2,
    maxColumns: Int = 5,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // 累积缩放因子
    var cumulativeScale by remember { mutableFloatStateOf(1f) }
    // 回弹动画
    val bounceScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.5f)
    )

    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .pointerInput(columns, minColumns, maxColumns) {
                detectTransformGestures { _, _, zoom, _ ->
                    cumulativeScale *= zoom

                    // 阈值检测
                    when {
                        cumulativeScale > ZOOM_OUT_THRESHOLD && columns > minColumns -> {
                            onColumnsChange(columns - 1)
                            cumulativeScale = 1f
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        cumulativeScale < ZOOM_IN_THRESHOLD && columns < maxColumns -> {
                            onColumnsChange(columns + 1)
                            cumulativeScale = 1f
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        // 边界回弹
                        (columns == minColumns && cumulativeScale > ZOOM_OUT_THRESHOLD) ||
                        (columns == maxColumns && cumulativeScale < ZOOM_IN_THRESHOLD) -> {
                            cumulativeScale = 1f
                            // 触发回弹动画
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    }
                }
            }
    ) {
        content()
    }

    companion object {
        private const val ZOOM_OUT_THRESHOLD = 1.3f  // 张开阈值
        private const val ZOOM_IN_THRESHOLD = 0.7f   // 收缩阈值
    }
}
```

### Step A2: 列数切换动画

**文件**: `ui/components/ColumnCountAnimator.kt`

```kotlin
/**
 * 列数切换时的平滑过渡动画
 *
 * 动画效果:
 * - 照片大小平滑变化
 * - 网格重新布局时保持视觉连续性
 * - 使用 AnimatedContent + fadeIn/fadeOut
 */
@Composable
fun AnimatedColumnGrid(
    columns: Int,
    modifier: Modifier = Modifier,
    content: @Composable (columns: Int) -> Unit
) {
    AnimatedContent(
        targetState = columns,
        transitionSpec = {
            fadeIn(animationSpec = tween(200)) togetherWith
            fadeOut(animationSpec = tween(200))
        },
        modifier = modifier
    ) { targetColumns ->
        content(targetColumns)
    }
}

// 更高级的动画: 使用共享元素过渡(Compose 1.7+)
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedElementColumnGrid(
    columns: Int,
    photos: List<PhotoEntity>,
    modifier: Modifier = Modifier
) {
    SharedTransitionLayout {
        // 每张照片使用相同的 sharedElement key
        // 实现列数变化时照片位置的平滑过渡
    }
}
```

### Step A3: 统一排序下拉菜单

**文件**: `ui/components/SortDropdownMenu.kt`

```kotlin
/**
 * 统一的排序下拉菜单组件
 *
 * 设计规范:
 * - 点击按钮弹出下拉菜单
 * - 当前选中项有勾选标记
 * - 支持自定义排序选项
 */
data class SortOption(
    val id: String,
    val displayName: String,
    val icon: ImageVector? = null
)

@Composable
fun SortDropdownButton(
    currentSort: SortOption,
    options: List<SortOption>,
    onSortSelected: (SortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.Sort,
                contentDescription = "排序: ${currentSort.displayName}"
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.displayName) },
                    onClick = {
                        onSortSelected(option)
                        expanded = false
                    },
                    leadingIcon = option.icon?.let { { Icon(it, null) } },
                    trailingIcon = if (option.id == currentSort.id) {
                        { Icon(Icons.Default.Check, "当前选中") }
                    } else null
                )
            }
        }
    }
}

// 预定义排序选项集合
object SortOptions {
    val photoTimeDesc = SortOption("time_desc", "时间倒序", Icons.Default.ArrowDownward)
    val photoTimeAsc = SortOption("time_asc", "时间正序", Icons.Default.ArrowUpward)
    val addedTimeDesc = SortOption("added_desc", "添加时间倒序")
    val addedTimeAsc = SortOption("added_asc", "添加时间正序")
    val random = SortOption("random", "随机排序", Icons.Default.Shuffle)

    // 各列表的排序选项预设
    val maybeListOptions = listOf(photoTimeDesc, photoTimeAsc, addedTimeDesc, addedTimeAsc)
    val trashListOptions = listOf(photoTimeDesc, photoTimeAsc, addedTimeDesc, addedTimeAsc)
    val keepListOptions = listOf(photoTimeDesc, photoTimeAsc, addedTimeDesc, addedTimeAsc, random)
    val albumListOptions = listOf(photoTimeDesc, photoTimeAsc, random)
    val timelineListOptions = listOf(photoTimeDesc, photoTimeAsc, random)
}
```

### Step A4: 排序偏好持久化

**文件**: `data/local/datastore/UserPreferencesDataStore.kt`

```kotlin
// 添加以下字段
data class ListSortPreferences(
    val maybeListSort: String = "time_desc",
    val trashListSort: String = "added_desc",
    val keepListSort: String = "time_desc",
    val albumListSort: String = "time_desc",
    val timelineListSort: String = "time_desc",
    val filterListSort: String = "time_desc"
)

// DataStore扩展
suspend fun DataStore<Preferences>.saveListSortPreference(
    listType: ListType,
    sortOptionId: String
)

fun DataStore<Preferences>.getListSortPreference(
    listType: ListType
): Flow<String>
```

### Step A5: 底部操作栏滑动提示

**文件**: `ui/components/BottomActionBar.kt` (修改)

```kotlin
@Composable
fun SelectionBottomBar(
    actions: List<BottomBarAction>,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val showEndIndicator by remember {
        derivedStateOf {
            scrollState.value < scrollState.maxValue
        }
    }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            actions.forEach { action ->
                BottomBarActionItem(action)
            }
        }

        // 右侧渐变遮罩提示
        if (showEndIndicator) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(32.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                            )
                        )
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "更多操作",
                    modifier = Modifier.align(Alignment.Center),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}
```

### Step A6: 集成到 DragSelectPhotoGrid

**文件**: `ui/components/DragSelectPhotoGrid.kt` (修改)

```kotlin
@Composable
fun DragSelectPhotoGrid(
    // ... 现有参数 ...
    columns: Int = 3,
    onColumnsChange: ((Int) -> Unit)? = null,  // 新增: 列数变更回调
    minColumns: Int = 2,                        // 新增: 最小列数
    maxColumns: Int = 5,                        // 新增: 最大列数
    enablePinchZoom: Boolean = true,            // 新增: 是否启用双指缩放
    // ...
) {
    val gridModifier = if (enablePinchZoom && onColumnsChange != null) {
        Modifier.zoomableGridModifier(
            columns = columns,
            onColumnsChange = onColumnsChange,
            minColumns = minColumns,
            maxColumns = maxColumns
        )
    } else {
        Modifier
    }

    // 使用动画过渡
    AnimatedColumnGrid(columns = columns) { targetColumns ->
        when (gridMode) {
            PhotoGridMode.SQUARE -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(targetColumns),
                    modifier = gridModifier,
                    // ...
                )
            }
            PhotoGridMode.WATERFALL -> {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(targetColumns),
                    modifier = gridModifier,
                    // ...
                )
            }
        }
    }
}
```

---

## 五、验证清单

### 5.1 需求覆盖检查

| 需求 | 描述 | 实现位置 | 状态 |
|-----|------|---------|------|
| REQ-001 | 网格视图正方形裁切 | DragSelectPhotoGrid.kt | ✅ 已有 |
| REQ-002 | 网格2-5列切换 | DragSelectPhotoGrid.kt:456 | ✅ 已完成 |
| REQ-003 | 双指缩放切换列数 | ZoomablePhotoGrid.kt | ✅ 已新增 |
| REQ-004 | 列数切换动效 | ZoomablePhotoGrid.kt (bounce动画) | ✅ 部分完成 |
| REQ-005 | 长按拖动多选 | DragSelectPhotoGrid.kt | ✅ 已有 |
| REQ-006 | 瀑布流原比例 | DragSelectPhotoGrid.kt | ✅ 已有 |
| REQ-007 | 瀑布流1-5列切换 | DragSelectPhotoGrid.kt:472 | ✅ 已完成 |
| REQ-008 | 瀑布流双指缩放 | ZoomablePhotoGrid.kt | ✅ 已新增 |
| REQ-009 | 瀑布流切换动效 | ZoomablePhotoGrid.kt (bounce动画) | ✅ 部分完成 |
| REQ-010 | 瀑布流禁用拖动多选 | DragSelectPhotoGrid.kt | ✅ 已有 |
| REQ-011 | 照片真实时间 | PhotoEntity.kt | ✅ 已有 |
| REQ-022 | 排序按钮交互统一 | SortDropdownMenu.kt | ✅ 已新增 |
| REQ-023 | 排序方式记忆 | PreferencesRepository.kt:1102-1148 | ✅ 已完成 |
| REQ-024 | 底部栏布局规范 | BottomActionBar.kt | ✅ 已有 |
| REQ-025 | 底部栏滑动提示 | BottomActionBar.kt:174-198 | ✅ 已完成 |
| REQ-026 | 复制照片保留EXIF | CopyPhotoUseCase.kt | ✅ 已有 |
| REQ-027 | 视图模式切换 | PhotoListViewModel.kt:677-696 | ✅ 已增强 |

### 5.2 编译验证
```bash
./gradlew compileStandardDebugKotlin
```

**验证结果** (2026-01-25):
```
BUILD SUCCESSFUL in 42s
18 actionable tasks: 8 executed, 10 up-to-date
```
✅ 编译通过

### 5.3 功能测试场景

| 场景 | 测试步骤 | 预期结果 |
|-----|---------|---------|
| 双指缩放-张开 | 在网格上双指张开 | 列数减少，照片变大 |
| 双指缩放-收缩 | 在网格上双指收缩 | 列数增加，照片变小 |
| 边界回弹 | 在2列时继续张开 | 有回弹效果和震动 |
| 动效过渡 | 切换列数 | 平滑过渡，无闪烁 |
| 排序下拉 | 点击排序按钮 | 显示下拉菜单 |
| 排序记忆 | 选择排序后退出再进入 | 保持上次排序 |
| 底部滑动 | 操作按钮超过屏幕宽度 | 可滑动，有右侧提示 |

---

## 六、风险与缓解

| 风险 | 等级 | 缓解措施 |
|-----|------|---------|
| 双指缩放与列表滚动冲突 | 高 | 使用 nestedScroll 协调，设置合理的手势优先级 |
| 列数切换动画卡顿 | 中 | 使用 AnimatedContent + remember 优化重组 |
| 跨页面状态不同步 | 低 | 使用统一的 DataStore 管理偏好 |

---

## 七、相关文档链接

- 上级文档: [PLAN_L2_INDEX.md](./PLAN_L2_INDEX.md)
- 需求来源: [REQUIREMENTS_LISTING.md](./REQUIREMENTS_LISTING.md) REQ-001~011, REQ-022~027
- 依赖模块: 无(基础层)
- 被依赖模块: 模块B, C, D, E, F, G, H
