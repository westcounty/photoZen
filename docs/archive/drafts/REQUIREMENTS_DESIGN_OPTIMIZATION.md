# PhotoZen 设计优化需求规格

> 生成日期: 2026-01-25
> 完成日期: 2026-01-25
> 来源文档: docs/设计优化建议.md
> 状态: ✅ 已完成

---

## 需求清单

### Phase 1: 基础设施层 (高优先级)

| 编号 | 需求描述 | 涉及文件 | 状态 |
|------|---------|---------|------|
| DES-001 | 创建统一设计Token系统 - 圆角规范 | ui/theme/DesignTokens.kt | ✅ |
| DES-002 | 创建统一设计Token系统 - 间距规范 | ui/theme/DesignTokens.kt | ✅ |
| DES-003 | 创建统一设计Token系统 - 高度/阴影规范 | ui/theme/DesignTokens.kt | ✅ |
| DES-004 | 创建统一设计Token系统 - 图标尺寸规范 | ui/theme/DesignTokens.kt | ✅ |
| DES-005 | 创建统一设计Token系统 - 透明度规范 | ui/theme/DesignTokens.kt | ✅ |
| DES-006 | 创建动效Token系统 - 时长规范 | ui/theme/MotionTokens.kt | ✅ |
| DES-007 | 创建动效Token系统 - 缓动曲线规范 | ui/theme/MotionTokens.kt | ✅ |
| DES-008 | 创建动效Token系统 - Spring配置规范 | ui/theme/MotionTokens.kt | ✅ |
| DES-009 | 创建动效Token系统 - 预设动画Spec | ui/theme/MotionTokens.kt | ✅ |
| DES-010 | 优化深色主题表面层级色彩 | ui/theme/Color.kt | ✅ |
| DES-011 | 创建操作反馈色彩系统(渐变/光晕) | ui/theme/Color.kt | ✅ |

### Phase 2: 核心组件层 (高优先级)

| 编号 | 需求描述 | 涉及文件 | 状态 |
|------|---------|---------|------|
| DES-012 | 增强型卡片组件 - 动态缩放/高度 | ui/components/EnhancedCard.kt | ✅ |
| DES-013 | 增强型卡片组件 - 微渐变背景 | ui/components/EnhancedCard.kt | ✅ |
| DES-014 | 增强型卡片组件 - 精细边框 | ui/components/EnhancedCard.kt | ✅ |
| DES-015 | 毛玻璃底部操作栏 - 浮动圆角设计 | ui/components/FloatingBottomBar.kt | ✅ |
| DES-016 | 毛玻璃底部操作栏 - 微动效反馈 | ui/components/FloatingBottomBar.kt | ✅ |
| DES-017 | 照片状态徽章 - 圆角胶囊设计 | ui/components/PhotoStatusBadge.kt | ✅ |
| DES-018 | 照片状态徽章 - 渐变光泽效果 | ui/components/PhotoStatusBadge.kt | ✅ |
| DES-019 | 滑动卡片增强 - 方向感知光晕 | ui/components/SwipeablePhotoCard.kt | ✅ |
| DES-020 | 滑动卡片增强 - 动态阴影 | ui/components/SwipeablePhotoCard.kt | ✅ |
| DES-021 | 滑动卡片增强 - 倾斜透视效果 | ui/components/SwipeablePhotoCard.kt | ✅ |

### Phase 3: 页面级优化 (中优先级)

| 编号 | 需求描述 | 涉及文件 | 状态 |
|------|---------|---------|------|
| DES-022 | 首页数字放大展示 | ui/screens/home/HomeScreen.kt | ✅ |
| DES-023 | 首页卡片间距调整 | ui/screens/home/HomeScreen.kt | ✅ |
| DES-024 | 首页主卡片阴影增强 | ui/screens/home/HomeScreen.kt | ✅ |
| DES-025 | 全屏预览页面指示器优化 | ui/components/fullscreen/ | ✅ |
| DES-026 | 全屏预览底部条放大效果 | ui/components/fullscreen/ | ✅ |
| DES-027 | 列表入场动画 | ui/animation/ListAnimations.kt | ✅ |
| DES-028 | 空状态动效优化 | ui/components/EmptyState.kt | ✅ |

### Phase 4: 细节打磨 (持续迭代)

| 编号 | 需求描述 | 涉及文件 | 状态 |
|------|---------|---------|------|
| DES-029 | 按钮按压微交互 | 多个组件 | ✅ |
| DES-030 | 列表项选中反馈 | 多个组件 | ✅ |
| DES-031 | 页面过渡动画标准化 | ui/theme/MotionTokens.kt | ✅ |
| DES-032 | 共享元素过渡 | ui/animation/ | ✅ |
| DES-033 | 手势回弹增强 | ui/components/SwipeablePhotoCard.kt | ✅ |

---

## 实施优先级

```
Phase 1 (基础设施) ──► Phase 2 (核心组件) ──► Phase 3 (页面优化) ──► Phase 4 (细节打磨)
     DES-001~011           DES-012~021           DES-022~028           DES-029~033
```

## 验证标准

- [x] 所有Token值统一使用，无硬编码
- [x] 动效时长/曲线遵循规范
- [x] 组件视觉层次明显提升
- [x] 编译通过无错误
- [ ] 运行时无性能问题 (需要实际设备测试)

## 实施摘要

### Phase 1: 基础设施层 (已完成)
- 创建 `DesignTokens.kt` - 统一设计规范参数
- 创建 `MotionTokens.kt` - 统一动效规范
- 增强 `Color.kt` - 添加 PicZenDarkSurfaces 和 PicZenActionColors

### Phase 2: 核心组件层 (已完成)
- 创建 `EnhancedCard.kt` - 增强型卡片组件
- 创建 `FloatingBottomBar.kt` - 毛玻璃底部操作栏
- 增强 `PhotoStatusBadge.kt` - 添加 PhotoStatusPill 组件
- 增强 `SwipeablePhotoCard.kt` - 倾斜透视、动态阴影

### Phase 3: 页面级优化 (已完成)
- 增强 `HomeComponents.kt` - 使用设计Token、数字放大、阴影增强
- 创建 `ListAnimations.kt` - 列表入场动画
- 增强 `EmptyState.kt` - 添加浮动动画
- 增强 `PhotoIndexIndicator.kt` - 渐变背景、突出数字
- 增强 `BottomPreviewStrip.kt` - Token尺寸、渐变边框

### Phase 4: 细节打磨 (已完成)
- 创建 `PressableButton.kt` - 按压微交互按钮
- 创建 `SelectableListItem.kt` - 选中反馈列表项
- 创建 `PageTransitions.kt` - 页面过渡动画标准化
- 增强 `SwipeablePhotoCard.kt` - 使用 playful spring 回弹
