# PhotoZen 交互设计规范 v1.0

> 📌 更新日期: 2026-01-19 | Phase 1-A 基础规范

---

## 1. 底部导航规范

### 1.1 Tab 定义

| Tab | 图标 (未选中) | 图标 (选中) | 标签 | 路由 |
|:----|:------------|:-----------|:-----|:-----|
| 首页 | Home (Outlined) | Home (Filled) | 首页 | `main_home` |
| 时间线 | Timeline (Outlined) | Timeline (Filled) | 时间线 | `main_timeline` |
| 相册 | Collections (Outlined) | Collections (Filled) | 相册 | `main_albums` |
| 设置 | Settings (Outlined) | Settings (Filled) | 设置 | `main_settings` |

### 1.2 切换动画

- **动画类型**: 淡入淡出 (crossfade)
- **动画时长**: 200ms
- **缓动函数**: EaseInOutCubic

### 1.3 状态保持

- 使用 `saveState = true` 保持各 Tab 页面状态
- 使用 `restoreState = true` 恢复状态
- 使用 `launchSingleTop = true` 避免重复创建

### 1.4 隐藏场景

底部导航在以下全屏页面中隐藏：

| 页面 | 路由前缀 | 原因 |
|:----|:--------|:-----|
| 滑动整理 | `FlowSorter` | 沉浸式整理体验 |
| 一站式整理 | `Workflow` | 多阶段工作流 |
| 照片编辑 | `PhotoEditor` | 全屏编辑操作 |
| 照片对比 | `LightTable` | 全屏对比操作 |
| 分享复制 | `ShareCopy` | 外部分享场景 |
| 分享对比 | `ShareCompare` | 外部分享场景 |
| 筛选选择 | `PhotoFilterSelection` | 筛选配置页面 |

### 1.5 显示/隐藏动画

- **入场**: `slideInVertically { it }` (从底部滑入)
- **出场**: `slideOutVertically { it }` (向底部滑出)

---

## 2. 首页布局规范

### 2.1 布局结构

```
┌─────────────────────────────┐
│  TopBar: Logo + 刷新 + 头像   │
├─────────────────────────────┤
│                             │
│  ┌───────────────────────┐  │
│  │    主操作区 (Card)      │  │
│  │    待整理数量 + 按钮     │  │
│  └───────────────────────┘  │
│                             │
│  ┌───────────────────────┐  │
│  │    快捷入口区 (Row)     │  │
│  │  📊对比    🗑️回收站    │  │
│  └───────────────────────┘  │
│                             │
│  ┌───────────────────────┐  │
│  │  每日任务 (可折叠 Card)  │  │
│  └───────────────────────┘  │
│                             │
│  ┌───────────────────────┐  │
│  │    成就预览 (Card)      │  │
│  └───────────────────────┘  │
│                             │
└─────────────────────────────┘
```

### 2.2 主操作区

- **背景**: `primaryContainer` 颜色
- **圆角**: 24dp
- **内边距**: 24dp
- **数字样式**: `displayLarge` 字号，`primary` 颜色，粗体
- **按钮高度**: 56dp
- **按钮圆角**: 16dp

### 2.3 快捷入口区

- **布局**: `Row` + `SpaceEvenly`
- **图标容器**: 48dp 圆形，15% 透明度背景色
- **图标尺寸**: 24dp
- **角标**: 圆形，对应颜色背景，白色文字

### 2.4 每日任务区

- **默认展开**: 未完成时
- **默认折叠**: 已完成时
- **已完成背景**: `KeepGreen` 10% 透明度
- **进度条高度**: 6dp
- **进度条圆角**: 3dp

---

## 3. 设计 Token

### 3.1 尺寸 Token

```kotlin
object HomeDesignTokens {
    val CardCornerRadius = 24.dp
    val CardPadding = 24.dp
    val SectionSpacing = 16.dp
    val QuickActionIconSize = 48.dp
    val MainActionButtonHeight = 56.dp
}
```

### 3.2 颜色语义

| 语义 | 颜色 | 用途 |
|:----|:-----|:----|
| Keep | `KeepGreen` (#22C55E) | 保留操作、完成状态 |
| Trash | `TrashRed` (#EF4444) | 删除操作、回收站 |
| Maybe | `MaybeAmber` (#FBBF24) | 待定操作、对比入口 |
| Primary | `primaryContainer` | 主操作区背景 |

### 3.3 间距规范

- **组件间距**: 16dp
- **卡片内边距**: 24dp (大卡片), 16dp (小卡片)
- **图标与文字间距**: 8dp
- **列表项间距**: 12dp

---

## 4. 动画规范

### 4.1 展开/折叠动画

- **组件**: `AnimatedVisibility`
- **进入**: `expandVertically` + `fadeIn`
- **退出**: `shrinkVertically` + `fadeOut`
- **时长**: 300ms

### 4.2 数值变化动画

- **组件**: `animateIntAsState` / `animateFloatAsState`
- **时长**: 300ms
- **缓动**: `FastOutSlowInEasing`

### 4.3 状态切换动画

- **选中/取消选中**: 200ms
- **图标切换**: `crossfade` 200ms
- **颜色过渡**: `animateColorAsState` 300ms

---

## 5. 响应式设计

### 5.1 屏幕适配

- **最小宽度**: 320dp
- **推荐宽度**: 360dp - 412dp
- **平板适配**: 暂不支持，后续 Phase 考虑

### 5.2 文字缩放

- 支持系统字体大小设置
- 关键数字使用 `displayLarge`
- 标签使用 `labelSmall` 或 `labelMedium`
- 正文使用 `bodyMedium` 或 `bodyLarge`

---

## 6. 无障碍规范

### 6.1 内容描述

- 所有图标必须提供 `contentDescription`
- 可点击元素必须有清晰的操作描述
- Badge 数量需要包含在内容描述中

### 6.2 触摸目标

- 最小触摸目标: 48dp x 48dp
- 快捷入口按钮满足此要求

### 6.3 颜色对比度

- 文字与背景对比度 >= 4.5:1 (WCAG AA)
- 重要信息不仅依赖颜色区分

---

*本规范为 Phase 1-A 基础版本，后续根据实施情况持续更新。*
