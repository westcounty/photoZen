# PhotoZen Design System

> Version: 2.0
> Last Updated: 2026-01-25
> Status: Production Ready

---

## Overview

PhotoZen Design System 是一套完整的设计规范，确保应用拥有**丝滑、流畅、优雅**的用户体验。本文档作为团队设计与开发的统一参考。

### 设计原则

```
┌─────────────────────────────────────────────────────────────────────┐
│  1. 丝滑流畅 - 所有交互都应有流畅的动画反馈                          │
│  2. 层次分明 - 通过阴影、透明度、间距建立清晰的视觉层级              │
│  3. 语义化色彩 - 绿色=保留, 红色=删除, 琥珀色=待定                   │
│  4. 统一规范 - 所有组件遵循统一的Token系统                           │
│  5. 触感反馈 - 结合视觉与触觉反馈增强交互感知                        │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 1. Design Tokens

### 1.1 圆角系统 (`PicZenTokens.Radius`)

基于视觉层级的渐进式圆角，确保组件层次感。

| Token | Value | 使用场景 |
|-------|-------|----------|
| `None` | 0dp | 无圆角元素 |
| `XS` | 4dp | 进度条、徽章内角 |
| `S` | 8dp | Chip、标签、小按钮 |
| `M` | 12dp | 按钮、输入框 |
| `L` | 16dp | 卡片、对话框 |
| `XL` | 24dp | 底部Sheet、主卡片 |
| `Full` | 9999dp | 圆形头像、圆形按钮 |

**使用示例:**
```kotlin
Surface(
    shape = RoundedCornerShape(PicZenTokens.Radius.L)
) { ... }
```

### 1.2 间距系统 (`PicZenTokens.Spacing`)

基于 8dp 网格的 Harmonic Scale，确保视觉节奏感。

| Token | Value | 使用场景 |
|-------|-------|----------|
| `XXS` | 2dp | 图标与文字紧贴 |
| `XS` | 4dp | 行内元素间距 |
| `S` | 8dp | 相关元素组间距 |
| `M` | 12dp | 按钮组、列表项间距 |
| `L` | 16dp | 区块分隔 |
| `XL` | 24dp | 卡片内边距 |
| `XXL` | 32dp | 页面边距 |
| `XXXL` | 48dp | 空状态插图边距 |

**使用示例:**
```kotlin
Column(
    modifier = Modifier.padding(PicZenTokens.Spacing.L),
    verticalArrangement = Arrangement.spacedBy(PicZenTokens.Spacing.M)
) { ... }
```

### 1.3 高度/阴影系统 (`PicZenTokens.Elevation`)

层级递进的阴影系统，建立空间深度感。

| Token | Value | 使用场景 |
|-------|-------|----------|
| `Level0` | 0dp | 背景内容 |
| `Level1` | 1dp | 卡片默认态 |
| `Level2` | 3dp | 卡片悬停态 |
| `Level3` | 6dp | 下拉菜单、Chip |
| `Level4` | 8dp | 底部栏、悬浮按钮 |
| `Level5` | 12dp | 对话框、全屏覆盖 |

### 1.4 图标尺寸系统 (`PicZenTokens.IconSize`)

| Token | Value | 使用场景 |
|-------|-------|----------|
| `XS` | 14dp | 徽章内图标 |
| `S` | 18dp | Chip内图标 |
| `M` | 20dp | 底部栏图标 |
| `L` | 24dp | 列表项、顶栏图标 |
| `XL` | 32dp | 状态指示 |
| `XXL` | 48dp | 快捷操作 |
| `XXXL` | 56dp | 空状态主图标 |

### 1.5 透明度系统 (`PicZenTokens.Alpha`)

| Token | Value | 使用场景 |
|-------|-------|----------|
| `Disabled` | 0.38 | 禁用态 |
| `Medium` | 0.60 | 中等强调 |
| `High` | 0.87 | 高强调 |
| `Hover` | 0.08 | 悬停叠加 |
| `Focus` | 0.12 | 焦点叠加 |
| `Pressed` | 0.16 | 按下叠加 |
| `Scrim` | 0.32 | 遮罩层 |
| `Overlay` | 0.80 | 覆盖层背景 |
| `Gloss` | 0.03 | 微妙光泽 |
| `Glow` | 0.25 | 光晕效果 |

### 1.6 组件尺寸系统 (`PicZenTokens.ComponentSize`)

| Token | Value | 使用场景 |
|-------|-------|----------|
| `BottomBarHeight` | 72dp | 底部操作栏高度 |
| `BottomBarIconContainer` | 44dp | 底部栏图标容器 |
| `MainButtonHeight` | 56dp | 主操作按钮高度 |
| `QuickActionIcon` | 48dp | 快捷操作图标 |
| `PreviewStripItem` | 40dp | 预览条缩略图 |
| `PreviewStripCurrentItem` | 52dp | 预览条当前项 |
| `BadgeSmall` | 18dp | 小徽章 |
| `BadgeMedium` | 24dp | 中徽章 |
| `BadgeLarge` | 32dp | 大徽章 |

---

## 2. Motion System (动效规范)

### 2.1 时长系统 (`PicZenMotion.Duration`)

**核心原则**: 快速响应，减少等待感

| Token | Value | 使用场景 |
|-------|-------|----------|
| `Instant` | 50ms | 按钮状态切换 |
| `Quick` | 100ms | 开关切换、微交互 |
| `Fast` | 150ms | 淡入淡出、小元素 |
| `Normal` | 200ms | 尺寸变化、常规过渡 |
| `Moderate` | 300ms | 页面过渡、复杂动画 |
| `Slow` | 400ms | 强调动画、大范围移动 |
| `Emphasis` | 500ms | 庆祝动效、成就解锁 |
| `Extended` | 800ms | 入场序列动画 |

### 2.2 缓动曲线 (`PicZenMotion.Easing`)

#### 标准曲线 (日常交互)
```kotlin
Standard           = CubicBezierEasing(0.2f, 0f, 0f, 1f)
StandardDecelerate = CubicBezierEasing(0f, 0f, 0f, 1f)      // 进入时
StandardAccelerate = CubicBezierEasing(0.3f, 0f, 1f, 1f)    // 退出时
```

#### 强调曲线 (吸引注意)
```kotlin
Emphasized           = CubicBezierEasing(0.2f, 0f, 0f, 1f)
EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
```

#### 特殊曲线
```kotlin
Bounce       = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)  // 弹性
FastOutSlowIn = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)     // 手势跟随
```

### 2.3 Spring配置 (`PicZenMotion.Springs`)

**确保丝滑流畅的弹性动画**

| Spring | 特性 | 使用场景 |
|--------|------|----------|
| `snappy()` | 无弹跳, 高刚度 | 按钮、开关、即时反馈 |
| `default()` | 轻微弹跳, 中等刚度 | 卡片、列表项 |
| `playful()` | 明显弹跳, 活泼感 | 滑动卡片、手势回弹 |
| `gentle()` | 轻微弹跳, 低刚度 | 页面过渡、大范围移动 |
| `noBounce()` | 无弹跳, 中等刚度 | 精确控制场景 |

**使用示例:**
```kotlin
val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.97f else 1f,
    animationSpec = PicZenMotion.Springs.snappy()
)
```

### 2.4 预设动画组合 (`PicZenMotion.Specs`)

| Spec | 效果 | 使用场景 |
|------|------|----------|
| `standardEnter` | 淡入 + 缩放 | 通用进入 |
| `standardExit` | 淡出 + 缩放 | 通用退出 |
| `bottomSheetEnter` | 淡入 + 从下滑入 | 底部弹出 |
| `bottomSheetExit` | 淡出 + 向下滑出 | 底部收起 |
| `topSheetEnter` | 淡入 + 从上滑入 | 顶部弹出 |
| `topSheetExit` | 淡出 + 向上滑出 | 顶部收起 |

### 2.5 延迟系统 (`PicZenMotion.Delay`)

用于序列动画的错开入场

| Token | Value | 使用场景 |
|-------|-------|----------|
| `StaggerItem` | 30ms | 列表项错开延迟 |
| `QuickSequence` | 50ms | 快速序列 |
| `StandardSequence` | 80ms | 标准序列 |
| `EmphasisSequence` | 120ms | 强调序列 |

---

## 3. Color System (色彩规范)

### 3.1 表面层级系统 (`PicZenDarkSurfaces`)

解决深色主题层次扁平问题，建立空间深度感

```
Background (#0A0A0C) ← 最深，纯黑偏冷
    │
    ├── Surface0 (#0F0F12) - 卡片底层
    ├── Surface1 (#1A1A1F) - 默认卡片
    ├── Surface2 (#232329) - 悬浮卡片
    ├── Surface3 (#2C2C35) - 下拉菜单
    ├── Surface4 (#35353F) - 底部栏
    └── Surface5 (#3E3E4A) - 对话框
```

### 3.2 操作反馈色彩 (`PicZenActionColors`)

语义化色彩系统，每种操作都有完整的色彩变体

#### Keep (保留) - 绿色系
```kotlin
Primary   = #22C55E  // 主色
Light     = #4ADE80  // 高亮
Dark      = #16A34A  // 深色
Glow      = #4022C55E // 光晕 (25% alpha)
Container = #0D3320  // 容器背景
OnContainer = #BBF7D0 // 容器上文字
```

#### Trash (删除) - 红色系
```kotlin
Primary   = #EF4444
Light     = #F87171
Dark      = #DC2626
Glow      = #40EF4444
Container = #3D1515
OnContainer = #FECACA
```

#### Maybe (待定) - 琥珀系
```kotlin
Primary   = #FBBF24
Light     = #FCD34D
Dark      = #F59E0B
Glow      = #40FBBF24
Container = #3D3010
OnContainer = #FEF3C7
```

**使用示例:**
```kotlin
// 获取滑动方向对应的光晕色
val glowColor = PicZenActionColors.getGlowColor(offsetX, offsetY)
```

---

## 4. Component Library (组件库)

### 4.1 EnhancedCard

增强型卡片，带动态缩放、微渐变、精细边框

```kotlin
@Composable
fun EnhancedCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    elevation: Dp = PicZenTokens.Elevation.Level1,
    enableGloss: Boolean = true,  // 顶部光泽
    content: @Composable ColumnScope.() -> Unit
)
```

**特性:**
- 按压时缩放至 0.98f
- 动态阴影高度变化
- 0.5dp 精细边框
- 可选顶部光泽层 (3% 白色渐变)

### 4.2 FloatingBottomBar

毛玻璃效果浮动底部栏

```kotlin
@Composable
fun FloatingBottomBar(
    actions: List<FloatingAction>,
    modifier: Modifier = Modifier
)

data class FloatingAction(
    val icon: ImageVector,
    val label: String,
    val tintColor: Color,
    val onClick: () -> Unit
)
```

**特性:**
- 85% 透明度背景
- 24dp 圆角浮动设计
- 按压时图标下沉 2dp + 缩放 0.9f
- 精细 0.5dp 边框

### 4.3 PhotoStatusPill

精致圆角状态徽章

```kotlin
@Composable
fun PhotoStatusPill(
    status: PhotoStatus,
    modifier: Modifier = Modifier,
    size: PillSize = PillSize.Medium  // Small/Medium/Large
)
```

**特性:**
- 渐变背景 (主色 → 85%透明主色)
- 顶部 25% 白色光泽层
- 2dp 彩色阴影
- 语义化图标 (勾号/时钟/关闭)

### 4.4 PressableButton

带按压微交互的按钮

```kotlin
@Composable
fun PressableButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
)
```

**特性:**
- 按压时缩放至 0.97f
- Snappy spring 动画
- 适用于所有需要按压反馈的按钮

### 4.5 SelectableListItem

带选中反馈的列表项

```kotlin
@Composable
fun SelectableListItem(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showCheckIcon: Boolean = true,
    content: @Composable RowScope.() -> Unit
)
```

**特性:**
- 选中时渐变边框 + 背景着色
- 按压时缩放 0.98f
- 动态阴影变化
- 可选勾选图标

### 4.6 AnimatedEmptyState

带浮动动画的空状态

```kotlin
@Composable
fun AnimatedEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
)
```

**特性:**
- 图标 8dp 浮动动画 (2s周期)
- 脉冲光晕效果 (1.5s周期)
- 140dp 渐变光晕背景

---

## 5. Page Transitions (页面过渡)

### 5.1 标准过渡 (`PageTransitions`)

| 过渡类型 | 进入效果 | 退出效果 | 使用场景 |
|----------|----------|----------|----------|
| `horizontal` | 右滑入 + 淡入 | 左滑出 + 淡出 | 标准页面导航 |
| `modal` | 下滑入 + 淡入 | 上滑出 + 淡出 | 模态页面 |
| `detail` | 放大 + 淡入 | 缩小 + 淡出 | 详情页面 |
| `fullscreen` | 放大0.85→1 | 缩小1→0.85 | 全屏预览 |

### 5.2 导航使用 (`NavTransitions`)

```kotlin
NavHost(
    navController = navController,
    enterTransition = NavTransitions.standardEnter(),
    exitTransition = NavTransitions.standardExit(),
    popEnterTransition = NavTransitions.standardPopEnter(),
    popExitTransition = NavTransitions.standardPopExit()
) {
    composable("home") { ... }

    composable(
        "detail/{id}",
        enterTransition = NavTransitions.detailEnter(),
        exitTransition = NavTransitions.detailExit()
    ) { ... }
}
```

---

## 6. Animation Guidelines (动画指南)

### 6.1 列表入场动画

使用 `AnimatedLazyColumn` 或 `animatedItems` 扩展

```kotlin
AnimatedLazyColumn(items = photos) { index, photo ->
    PhotoListItem(photo = photo)
}

// 或使用扩展函数
LazyColumn {
    animatedItems(photos) { photo ->
        PhotoListItem(photo)
    }
}
```

**动画规范:**
- 错开延迟: 30ms/item (最多15项)
- 动画时长: 200ms
- 效果: 淡入 + 向上位移 20dp

### 6.2 滑动卡片动画

SwipeablePhotoCard 增强效果:

```
┌─────────────────────────────────────────────────────────────────────┐
│  方向感知光晕:                                                       │
│  - 右滑: 左侧发光 (Keep绿)                                          │
│  - 左滑/上滑: 右侧/底部发光 (Trash红)                                │
│  - 下滑: 顶部发光 (Maybe琥珀)                                        │
├─────────────────────────────────────────────────────────────────────┤
│  动态效果:                                                          │
│  - rotationZ: 水平偏移 × 12°                                        │
│  - rotationX: 垂直偏移 × 5° (透视)                                  │
│  - shadowElevation: 8dp + 滑动进度 × 12dp                           │
│  - scale: 1 - 偏移比例 × 0.15 (下滑时)                              │
├─────────────────────────────────────────────────────────────────────┤
│  回弹效果:                                                          │
│  - 使用 playful() spring                                            │
│  - DampingRatio: MediumBouncy                                       │
│  - Stiffness: Medium                                                │
└─────────────────────────────────────────────────────────────────────┘
```

### 6.3 按钮微交互

所有可点击按钮应使用按压缩放:

```kotlin
// 方式1: 使用 PressableButton
PressableButton(onClick = { ... }) {
    Text("开始整理")
}

// 方式2: 使用 pressable 修饰符
Box(modifier = Modifier.pressable(onClick = { ... })) {
    // 内容
}
```

---

## 7. Best Practices (最佳实践)

### 7.1 丝滑流畅的关键

```kotlin
// ✅ 正确: 使用 Spring 动画实现流畅交互
val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.97f else 1f,
    animationSpec = PicZenMotion.Springs.snappy()
)

// ❌ 错误: 使用硬编码 tween 时长
val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.97f else 1f,
    animationSpec = tween(100)  // 不够流畅
)
```

### 7.2 统一使用 Token

```kotlin
// ✅ 正确: 使用 Token
Modifier.padding(PicZenTokens.Spacing.L)
RoundedCornerShape(PicZenTokens.Radius.L)

// ❌ 错误: 硬编码值
Modifier.padding(16.dp)
RoundedCornerShape(16.dp)
```

### 7.3 组合动画

```kotlin
// ✅ 正确: 使用预设组合
AnimatedVisibility(
    visible = isVisible,
    enter = PicZenMotion.Specs.standardEnter,
    exit = PicZenMotion.Specs.standardExit
) { ... }

// 或自定义组合
enter = PicZenMotion.Specs.fadeInSpec + PicZenMotion.Specs.scaleInSpec
```

### 7.4 性能考虑

- 使用 `graphicsLayer` 进行变换 (GPU加速)
- 避免在动画中重组
- 列表项错开动画限制最大项数 (15项)
- 使用 `remember` 缓存 Animatable

---

## 8. File Reference (文件索引)

| 文件路径 | 内容 |
|----------|------|
| `ui/theme/DesignTokens.kt` | 设计Token系统 |
| `ui/theme/MotionTokens.kt` | 动效Token系统 |
| `ui/theme/Color.kt` | 色彩系统 (含Surface层级、ActionColors) |
| `ui/components/EnhancedCard.kt` | 增强型卡片 |
| `ui/components/FloatingBottomBar.kt` | 浮动底部栏 |
| `ui/components/PhotoStatusBadge.kt` | 状态徽章 (含PhotoStatusPill) |
| `ui/components/PressableButton.kt` | 按压微交互按钮 |
| `ui/components/SelectableListItem.kt` | 可选列表项 |
| `ui/components/EmptyState.kt` | 空状态 (含AnimatedEmptyState) |
| `ui/animation/ListAnimations.kt` | 列表入场动画 |
| `ui/animation/PageTransitions.kt` | 页面过渡动画 |

---

## 9. Changelog

### v2.0 (2026-01-25)
- 建立完整 Token 系统
- 创建 Motion 规范
- 增强表面层级系统
- 新增操作反馈色彩
- 优化所有核心组件
- 标准化页面过渡
- 添加列表入场动画
- 增强空状态动效
- 优化滑动卡片体验

---

*PhotoZen Design System - 让每一次交互都丝滑流畅*
