# PhotoZen 交互设计规范 v2.0

> 📌 更新日期: 2026-01-25 | 设计系统集成版

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
- **动画时长**: `PicZenMotion.Duration.Normal` (200ms)
- **缓动函数**: `PicZenMotion.Easing.Standard`

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

- **入场**: `slideInVertically { it }` + `fadeIn`
- **出场**: `slideOutVertically { it }` + `fadeOut`
- **时长**: `PicZenMotion.Duration.Fast` (150ms)

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
- **圆角**: `PicZenTokens.Radius.XL` (24dp)
- **内边距**: `PicZenTokens.Spacing.XL` (24dp)
- **数字样式**: 72sp，`primary` 颜色，增强阴影
- **按钮高度**: `PicZenTokens.ComponentSize.MainActionButtonHeight` (56dp)
- **按钮圆角**: `PicZenTokens.Radius.L` (16dp)

### 2.3 快捷入口区

- **布局**: `Row` + `SpaceEvenly`
- **图标容器**: 48dp 圆形，15% 透明度背景色
- **图标尺寸**: `PicZenTokens.IconSize.M` (24dp)
- **角标**: 圆形，对应颜色背景，白色文字

### 2.4 每日任务区

- **默认展开**: 未完成时
- **默认折叠**: 已完成时
- **已完成背景**: `PicZenActionColors.Keep.Container`
- **进度条高度**: 6dp
- **进度条圆角**: 3dp

---

## 3. 设计 Token 引用

### 3.1 间距 Token (PicZenTokens.Spacing)

| Token | 值 | 用途 |
|:------|:---|:-----|
| XXS | 2dp | 紧凑间隙 |
| XS | 4dp | 图标内边距 |
| S | 8dp | 列表项间距 |
| M | 12dp | 组件内部间距 |
| L | 16dp | 卡片内边距 |
| XL | 24dp | 大卡片内边距 |
| XXL | 32dp | 区块间距 |
| XXXL | 48dp | 页面边距 |

### 3.2 圆角 Token (PicZenTokens.Radius)

| Token | 值 | 用途 |
|:------|:---|:-----|
| None | 0dp | 无圆角 |
| XS | 4dp | 小按钮、标签 |
| S | 8dp | 普通按钮 |
| M | 12dp | 卡片 |
| L | 16dp | 大按钮 |
| XL | 24dp | 主卡片 |
| Full | 9999dp | 圆形 |

### 3.3 颜色语义

| 语义 | 颜色对象 | 主色 |
|:----|:--------|:-----|
| Keep | `PicZenActionColors.Keep` | #22C55E |
| Trash | `PicZenActionColors.Trash` | #EF4444 |
| Maybe | `PicZenActionColors.Maybe` | #FBBF24 |

---

## 4. 动画规范

### 4.1 时长规范 (PicZenMotion.Duration)

| 类型 | 时长 | 用途 |
|:-----|:-----|:-----|
| Instant | 50ms | 立即响应 |
| Quick | 100ms | 快速反馈、淡出 |
| Fast | 150ms | 短促动画 |
| Normal | 200ms | 标准动画 |
| Moderate | 300ms | 展开/折叠 |
| Slow | 450ms | 强调动画 |
| Deliberate | 600ms | 戏剧化效果 |

### 4.2 弹簧动画 (PicZenMotion.Springs)

| 类型 | 特性 | 用途 |
|:-----|:-----|:-----|
| snappy() | 快速无弹跳 | 按钮按压、选中 |
| default() | 标准弹簧 | 通用动画 |
| playful() | 活泼弹跳 | 手势回弹 |
| gentle() | 柔和弹簧 | 页面过渡 |

### 4.3 展开/折叠动画

- **组件**: `AnimatedVisibility`
- **进入**: `expandVertically` + `fadeIn`
- **退出**: `shrinkVertically` + `fadeOut`
- **时长**: `PicZenMotion.Duration.Moderate` (300ms)

### 4.4 数值变化动画

- **组件**: `animateIntAsState` / `animateFloatAsState`
- **规格**: `PicZenMotion.Specs.countUp`
- **缓动**: `PicZenMotion.Easing.EmphasizedDecelerate`

### 4.5 状态切换动画

- **选中/取消选中**: `PicZenMotion.Springs.snappy()`
- **图标切换**: `crossfade` + `PicZenMotion.Duration.Normal`
- **颜色过渡**: `animateColorAsState` + `PicZenMotion.Duration.Moderate`

---

## 5. 列表动画规范

### 5.1 错落入场动画

- **延迟间隔**: `PicZenMotion.Delay.Stagger` (30ms/项)
- **初始位移**: 16dp 向下
- **初始透明度**: 0
- **动画曲线**: `PicZenMotion.Easing.EmphasizedDecelerate`

### 5.2 使用方式

```kotlin
LazyColumn {
    itemsIndexed(items) { index, item ->
        AnimatedListItem(index = index) {
            ItemContent(item)
        }
    }
}
```

---

## 6. 页面过渡规范

### 6.1 水平导航 (标准页面)

- **进入**: 从右侧滑入 (1/4 屏宽) + 淡入
- **退出**: 向左滑出 (1/4 屏宽) + 淡出
- **返回进入**: 从左侧滑入
- **返回退出**: 向右滑出

### 6.2 模态页面 (底部弹出)

- **进入**: 从底部滑入 (1/3 屏高) + 淡入
- **退出**: 向底部滑出 + 淡出

### 6.3 详情页面 (缩放)

- **进入**: 从 0.92 缩放到 1.0 + 淡入
- **退出**: 从 1.0 缩放到 0.92 + 淡出

### 6.4 全屏预览

- **进入**: 从 0.85 缩放到 1.0 + 淡入
- **退出**: 从 1.0 缩放到 0.85 + 淡出

---

## 7. 响应式设计

### 7.1 屏幕适配

- **最小宽度**: 320dp
- **推荐宽度**: 360dp - 412dp
- **平板适配**: 暂不支持，后续考虑

### 7.2 文字缩放

- 支持系统字体大小设置
- 关键数字使用 72sp (首页待整理数)
- 标签使用 `labelSmall` 或 `labelMedium`
- 正文使用 `bodyMedium` 或 `bodyLarge`

---

## 8. 无障碍规范

### 8.1 内容描述

- 所有图标必须提供 `contentDescription`
- 可点击元素必须有清晰的操作描述
- Badge 数量需要包含在内容描述中

### 8.2 触摸目标

- 最小触摸目标: 48dp x 48dp
- 快捷入口按钮满足此要求

### 8.3 颜色对比度

- 文字与背景对比度 >= 4.5:1 (WCAG AA)
- 重要信息不仅依赖颜色区分

---

## 9. 相关文档

- [DESIGN_SYSTEM.md](DESIGN_SYSTEM.md) - 完整设计系统规范
- [GESTURE_SPEC.md](GESTURE_SPEC.md) - 手势交互规范

---

*本规范基于 PicZenTokens 和 PicZenMotion 设计系统，确保全应用一致性。*
