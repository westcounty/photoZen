# 模块H: 筛选列表优化 - 详细实施方案

> 父文档: [PLAN_L2_INDEX.md](./PLAN_L2_INDEX.md)
> 需求文档: [REQUIREMENTS_LISTING.md](./REQUIREMENTS_LISTING.md)
> 涉及需求: REQ-057~058 (共2个需求点)
> 依赖模块: 模块A
> 状态: 📝 规划中

---

## 一、原始需求摘录

### REQ-057: 全局操作按钮
```
卡片筛选模式:
- 筛选器按钮、排序按钮、筛选模式切换按钮

列表筛选模式:
- 筛选器按钮、排序按钮、列表模式切换按钮、筛选模式切换按钮

排序选项:
- 照片真实时间正序/倒序、随机排序
- 默认: 时间倒序
```

### REQ-058: 深度整理模式布局
```
- 在深度整理（筛选-对比-分类一条龙整理）模式下
- 把全局操作悬浮在图片展示区域
- 避免和顶部导航栏冲突（显示整体进度和下一步按钮）
```

---

## 二、现有实现分析

### 2.1 现有实现状态

| 功能 | 状态 | 备注 |
|-----|------|------|
| 卡片筛选模式 | ✅ FlowSorterScreen | CardStack |
| 列表筛选模式 | ✅ FlowSorterScreen | SelectableStaggeredPhotoGrid |
| 筛选器按钮 | ✅ FilterSelectionScreen | |
| 排序按钮 | 部分 | 仅卡片模式有 |
| 模式切换按钮 | ✅ | |
| 视图模式切换 | ❌ | 列表模式缺少 |
| 深度整理布局 | ❌ | 需新增 |

### 2.2 关键文件

```
ui/screens/flowsorter/
├── FlowSorterScreen.kt           # 主筛选界面
├── FlowSorterViewModel.kt        # 状态管理
├── components/
│   ├── CardStack.kt              # 卡片堆栈
│   ├── SortingProgressBar.kt     # 进度条
│   └── FloatingAlbumTags.kt      # 快捷相册
└── FilterSelectionScreen.kt      # 筛选器选择页

ui/screens/workflow/
└── WorkflowScreen.kt             # 深度整理工作流(一站式)
```

---

## 三、技术方案设计

### 3.1 全局操作按钮统一

```kotlin
/**
 * 筛选列表顶部操作栏
 *
 * 根据当前模式显示不同按钮组合:
 * - 卡片模式: 筛选器 + 排序 + 切换到列表
 * - 列表模式: 筛选器 + 排序 + 视图模式 + 切换到卡片
 */
@Composable
fun FlowSorterTopActions(
    isCardMode: Boolean,
    sortOrder: PhotoListSortOrder,
    gridMode: PhotoGridMode,
    onFilterClick: () -> Unit,
    onSortChange: (PhotoListSortOrder) -> Unit,
    onGridModeToggle: () -> Unit,
    onModeSwitch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 筛选器按钮
        IconButton(onClick = onFilterClick) {
            Icon(Icons.Default.FilterList, "筛选条件")
        }

        // 排序按钮
        SortDropdownButton(
            currentSort = sortOrder,
            options = listOf(
                PhotoListSortOrder.DATE_DESC,
                PhotoListSortOrder.DATE_ASC,
                PhotoListSortOrder.RANDOM
            ),
            onSortSelected = onSortChange
        )

        // 列表模式特有: 视图模式切换
        if (!isCardMode) {
            IconButton(onClick = onGridModeToggle) {
                Icon(
                    imageVector = if (gridMode == PhotoGridMode.SQUARE)
                        Icons.Default.GridView else Icons.Default.ViewStream,
                    contentDescription = "切换视图"
                )
            }
        }

        // 模式切换按钮
        IconButton(onClick = onModeSwitch) {
            Icon(
                imageVector = if (isCardMode)
                    Icons.Default.ViewList else Icons.Default.ViewCarousel,
                contentDescription = if (isCardMode) "切换到列表" else "切换到卡片"
            )
        }
    }
}
```

### 3.2 深度整理模式布局

```kotlin
/**
 * 深度整理模式下的悬浮操作区
 *
 * 特点:
 * - 悬浮在图片展示区域上方
 * - 不与顶部导航栏(进度+下一步)冲突
 * - 半透明背景
 */
@Composable
fun WorkflowFloatingActions(
    isCardMode: Boolean,
    sortOrder: PhotoListSortOrder,
    gridMode: PhotoGridMode,
    onFilterClick: () -> Unit,
    onSortChange: (PhotoListSortOrder) -> Unit,
    onGridModeToggle: () -> Unit,
    onModeSwitch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        FlowSorterTopActions(
            isCardMode = isCardMode,
            sortOrder = sortOrder,
            gridMode = gridMode,
            onFilterClick = onFilterClick,
            onSortChange = onSortChange,
            onGridModeToggle = onGridModeToggle,
            onModeSwitch = onModeSwitch
        )
    }
}

/**
 * 深度整理工作流布局
 */
@Composable
fun WorkflowSortingScreen(
    viewModel: WorkflowViewModel,
    // ...
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            // 顶部: 进度 + 下一步按钮
            WorkflowTopBar(
                currentStep = uiState.currentStep,
                totalSteps = uiState.totalSteps,
                progress = uiState.progress,
                onNextStep = viewModel::nextStep,
                onExit = { /* 退出工作流 */ }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 主内容区: 卡片或列表
            when {
                uiState.isCardMode -> {
                    CardStack(
                        photos = uiState.photos,
                        // ...
                    )
                }
                else -> {
                    SelectableStaggeredPhotoGrid(
                        photos = uiState.photos,
                        // ...
                    )
                }
            }

            // 悬浮操作区 - 位于图片区域上方
            WorkflowFloatingActions(
                isCardMode = uiState.isCardMode,
                sortOrder = uiState.sortOrder,
                gridMode = uiState.gridMode,
                onFilterClick = { /* 打开筛选器 */ },
                onSortChange = viewModel::setSortOrder,
                onGridModeToggle = viewModel::toggleGridMode,
                onModeSwitch = viewModel::toggleMode,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp)
            )
        }
    }
}
```

---

## 四、详细实现步骤

### Step H1: 列表模式添加排序和视图切换

**文件**: `ui/screens/flowsorter/FlowSorterScreen.kt`

```kotlin
@Composable
fun FlowSorterScreen(
    navController: NavController,
    viewModel: FlowSorterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            FlowSorterTopBar(
                title = uiState.title,
                progress = uiState.progress,
                onBack = { navController.popBackStack() },
                actions = {
                    // 使用统一的操作按钮组件
                    FlowSorterTopActions(
                        isCardMode = uiState.isCardMode,
                        sortOrder = uiState.sortOrder,
                        gridMode = uiState.gridMode,
                        onFilterClick = {
                            navController.navigate("filter_selection")
                        },
                        onSortChange = viewModel::setSortOrder,
                        onGridModeToggle = viewModel::toggleGridMode,
                        onModeSwitch = viewModel::toggleMode
                    )
                }
            )
        }
    ) { padding ->
        // ... 内容区域
    }
}
```

### Step H2: ViewModel更新

**文件**: `ui/screens/flowsorter/FlowSorterViewModel.kt`

```kotlin
@HiltViewModel
class FlowSorterViewModel @Inject constructor(
    // ...
) : ViewModel() {

    // 新增: 列表模式的视图模式
    private val _gridMode = MutableStateFlow(PhotoGridMode.WATERFALL)

    data class UiState(
        // ... 现有字段
        val gridMode: PhotoGridMode = PhotoGridMode.WATERFALL,  // 新增
        val sortOrder: PhotoListSortOrder = PhotoListSortOrder.DATE_DESC
    )

    fun toggleGridMode() {
        _gridMode.update {
            if (it == PhotoGridMode.SQUARE) PhotoGridMode.WATERFALL
            else PhotoGridMode.SQUARE
        }
    }

    fun setSortOrder(order: PhotoListSortOrder) {
        _sortOrder.value = order
        // 重新排序照片
        resortPhotos()
    }
}
```

### Step H3: 深度整理模式集成

**文件**: `ui/screens/workflow/WorkflowScreen.kt`

```kotlin
@Composable
fun WorkflowScreen(
    navController: NavController,
    viewModel: WorkflowViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // 判断当前步骤
    when (uiState.currentStep) {
        WorkflowStep.SORTING -> {
            // 筛选步骤 - 使用带悬浮操作的布局
            WorkflowSortingScreen(
                viewModel = viewModel,
                onComplete = { viewModel.nextStep() }
            )
        }
        WorkflowStep.COMPARING -> {
            // 对比步骤
            WorkflowComparingScreen(viewModel = viewModel)
        }
        WorkflowStep.CLASSIFYING -> {
            // 分类步骤
            WorkflowClassifyingScreen(viewModel = viewModel)
        }
    }
}
```

---

## 五、验证清单

### 需求覆盖检查

| 需求 | 描述 | 实现位置 | 状态 |
|-----|------|---------|------|
| REQ-057 | 全局操作按钮(两种模式) | FlowSorterTopActions | ⏳ |
| REQ-058 | 深度整理悬浮布局 | WorkflowFloatingActions | ⏳ |

### 功能测试场景

| 场景 | 测试步骤 | 预期结果 |
|-----|---------|---------|
| 卡片模式按钮 | 进入卡片筛选 | 显示筛选器+排序+切换按钮 |
| 列表模式按钮 | 切换到列表模式 | 额外显示视图模式按钮 |
| 排序功能 | 在列表模式选择排序 | 列表按选择方式排序 |
| 视图切换 | 列表模式点击视图按钮 | 在网格和瀑布流间切换 |
| 深度整理 | 进入一站式整理 | 悬浮操作不遮挡顶部进度 |

---

## 六、相关文档链接

- 上级文档: [PLAN_L2_INDEX.md](./PLAN_L2_INDEX.md)
- 需求来源: [REQUIREMENTS_LISTING.md](./REQUIREMENTS_LISTING.md) REQ-057~058
- 依赖模块: [模块A](./PLAN_L2_MODULE_A.md)
- 相关模块: [模块F+G](./PLAN_L2_MODULE_F_G.md)
