# PhotoZen 手势交互规范 v1.0

> 📌 更新日期: 2026-01-19 | Phase 1-A 基础规范

---

## 1. 照片列表手势规范

所有照片列表页面必须遵循以下手势规范，确保用户在不同页面获得一致的交互体验。

### 1.1 点击

| 场景 | 行为 |
|:----|:----|
| 非选择模式 | 进入全屏预览 |
| 选择模式 | 切换该照片的选中状态 |

### 1.2 长按（不移动）

**触发条件**: 按压时间 > 500ms，移动距离 < 阈值

**行为序列**:
1. 选中当前照片
2. 进入选择模式
3. 显示底部操作栏
4. 触发震动反馈

**震动反馈**:
- 类型: `HapticFeedbackType.LongPress`
- 时长: 约 50ms

### 1.3 长按 + 拖动

**触发条件**: 长按后移动距离 > 阈值

**行为特性**:
- 从长按位置开始批量选择
- 拖动经过的所有照片都被选中
- 支持自动滚动（当拖动到列表边缘时）
- 每新增一个选中项触发轻微震动

**震动反馈**:
- 类型: `HapticFeedbackType.TextHandleMove`
- 触发: 每新增一个选中项

**自动滚动**:
- 边缘检测区域: 距顶部/底部 100dp
- 滚动速度: 根据距离动态调整

### 1.4 退出选择模式

| 操作 | 行为 |
|:----|:----|
| 点击顶栏关闭按钮 | 清除所有选择，退出选择模式 |
| 按返回键 | 清除所有选择，退出选择模式 |
| 选中数归零 | 自动退出选择模式 |

---

## 2. 统一实现

### 2.1 核心组件

使用 `DragSelectPhotoGrid` 组件实现统一手势：

```kotlin
DragSelectPhotoGrid(
    photos = photos,
    selectedIds = selectedIds,
    onSelectionChanged = { newSelection -> 
        viewModel.updateSelection(newSelection) 
    },
    onPhotoClick = { id, index -> 
        // 进入全屏预览
    },
    onPhotoLongPress = { id, uri ->
        // 长按不动回调，已经选中
    },
    columns = columns,
    enableDragSelect = true
)
```

### 2.2 关键参数

| 参数 | 类型 | 说明 |
|:----|:----|:----|
| `photos` | `List<PhotoEntity>` | 照片列表 |
| `selectedIds` | `Set<String>` | 已选中的照片 ID 集合 |
| `onSelectionChanged` | `(Set<String>) -> Unit` | 选中状态变化回调 |
| `onPhotoClick` | `(String, Int) -> Unit` | 点击回调 (ID, 索引) |
| `onPhotoLongPress` | `(String, String) -> Unit` | 长按不动回调 (ID, URI) |
| `columns` | `Int` | 列数 (默认 3) |
| `enableDragSelect` | `Boolean` | 是否启用拖动选择 |

### 2.3 实现细节

```kotlin
// 拖动阈值 - 区分"长按不动"和"长按拖动"
private const val DRAG_THRESHOLD_DP = 10

// 在 detectDragGesturesAfterLongPress 中判断
val distance = sqrt(totalDrag.x.pow(2) + totalDrag.y.pow(2))
if (distance < dragThreshold) {
    // 长按不动
    onPhotoLongPress(photo.id, photo.systemUri)
} else {
    // 长按拖动，批量选择
}
```

---

## 3. 应用页面状态

### 3.1 已统一页面

| 页面 | 文件 | 状态 |
|:----|:----|:----|
| PhotoListScreen | `ui/screens/photolist/` | ✅ 已使用 DragSelectPhotoGrid |
| TrashScreen | `ui/screens/trash/` | ✅ 已使用 DragSelectPhotoGrid |

### 3.2 待改造页面 (Phase 1-B)

| 页面 | 文件 | 当前实现 | 改造方案 |
|:----|:----|:--------|:--------|
| AlbumPhotoListScreen | `ui/screens/albums/` | 自定义 LazyVerticalStaggeredGrid | 改用 DragSelectPhotoGrid |
| TimelineScreen | `ui/screens/timeline/` | 自定义实现 | 在事件组内使用统一手势 |

---

## 4. 滑动整理手势规范

### 4.1 基本手势

| 手势 | 方向 | 操作 | 颜色反馈 |
|:----|:----|:----|:--------|
| 右滑 | → | 保留 (Keep) | 绿色边缘发光 |
| 左滑 | ← | 删除 (Trash) | 红色边缘发光 |
| 上滑 | ↑ | 待定 (Maybe) | 黄色边缘发光 |
| 下滑 | ↓ | 跳过 (Skip) | 灰色边缘发光 |

### 4.2 触发阈值

- **水平阈值**: 屏幕宽度的 30%
- **垂直阈值**: 屏幕高度的 25%
- **临界反馈**: 达到阈值的 80% 时触发震动

### 4.3 点击手势

| 操作 | 行为 |
|:----|:----|
| 单击 | 切换显示/隐藏照片信息 |
| 双击 | 进入/退出缩放模式 |
| 长按 | 显示快捷操作菜单 |

---

## 5. 全屏预览手势规范

### 5.1 缩放手势

| 手势 | 行为 |
|:----|:----|
| 双击 | 切换 1x ↔ 2x 缩放 |
| 双指捏合 | 平滑缩放 (1x - 5x) |
| 双指张开 | 平滑放大 |

### 5.2 平移手势

| 手势 | 条件 | 行为 |
|:----|:----|:----|
| 单指拖动 | 缩放 > 1x | 平移查看细节 |
| 单指拖动 | 缩放 = 1x | 切换上一张/下一张 |

### 5.3 退出手势

| 手势 | 行为 |
|:----|:----|
| 下拉 (缩放=1x) | 退出全屏预览 |
| 返回键 | 退出全屏预览 |
| 点击背景 | 退出全屏预览 |

---

## 6. Light Table 手势规范

### 6.1 同步缩放

- 所有对比照片共享同一个 `TransformState`
- 缩放/平移操作同步应用到所有照片
- 支持 1x - 5x 缩放范围

### 6.2 选择手势

| 手势 | 行为 |
|:----|:----|
| 点击照片 | 切换该照片的选中状态 |
| 长按照片 | 将该照片设为主图 (居中对齐参考) |

---

## 7. 震动反馈规范

### 7.1 反馈类型

| 场景 | HapticFeedbackType | 说明 |
|:----|:-------------------|:----|
| 长按选中 | `LongPress` | 明显的反馈 |
| 拖动多选 | `TextHandleMove` | 轻微的反馈 |
| 滑动临界 | `LongPress` | 提示达到阈值 |
| 操作完成 | `TextHandleMove` | 确认反馈 |

### 7.2 使用方式

```kotlin
val haptic = LocalHapticFeedback.current

// 长按反馈
haptic.performHapticFeedback(HapticFeedbackType.LongPress)

// 轻微反馈
haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
```

---

## 8. 边缘情况处理

### 8.1 空列表

- 显示空状态插画和提示文字
- 禁用所有手势交互

### 8.2 单张照片

- 禁用拖动多选
- 保留点击和长按功能

### 8.3 正在加载

- 显示加载占位符
- 禁用交互直到加载完成

### 8.4 手势冲突

- 嵌套滚动: 使用 `nestedScroll` 协调
- 多手指: 优先处理缩放，忽略额外手指

---

*本规范为 Phase 1-A 基础版本，Phase 1-B 将完成所有页面的手势统一改造。*
