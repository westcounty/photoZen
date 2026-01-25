package com.example.photozen.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * PhotoZen Design System Tokens
 * 统一管理所有设计规范值
 *
 * 设计原则:
 * - 基于8dp网格系统
 * - 渐进式层级递增
 * - 语义化命名便于理解
 *
 * @see MotionTokens 动效规范
 * @see PicZenActionColors 操作反馈色彩
 */
object PicZenTokens {

    // ═══════════════════════════════════════════════════════════════════
    // 圆角系统 - 基于视觉层级的渐进式圆角
    // ═══════════════════════════════════════════════════════════════════
    object Radius {
        /** 无圆角 */
        val None: Dp = 0.dp

        /** 极小元素: 进度条、徽章内角 */
        val XS: Dp = 4.dp

        /** 小型元素: Chip、标签 */
        val S: Dp = 8.dp

        /** 中型元素: 按钮、输入框 */
        val M: Dp = 12.dp

        /** 大型元素: 卡片、对话框 */
        val L: Dp = 16.dp

        /** 超大元素: 底部Sheet、主卡片 */
        val XL: Dp = 24.dp

        /** 圆形: 头像、圆形按钮 */
        val Full: Dp = 9999.dp
    }

    // ═══════════════════════════════════════════════════════════════════
    // 间距系统 - 8dp基准的Harmonic Scale
    // ═══════════════════════════════════════════════════════════════════
    object Spacing {
        /** 微间距: 图标与文字紧贴 */
        val XXS: Dp = 2.dp

        /** 极小间距: 行内元素 */
        val XS: Dp = 4.dp

        /** 小间距: 相关元素组 */
        val S: Dp = 8.dp

        /** 中间距: 按钮组、列表项 */
        val M: Dp = 12.dp

        /** 大间距: 区块分隔 */
        val L: Dp = 16.dp

        /** 超大间距: 卡片内边距 */
        val XL: Dp = 24.dp

        /** 特大间距: 页面边距 */
        val XXL: Dp = 32.dp

        /** 巨型间距: 空状态插图 */
        val XXXL: Dp = 48.dp
    }

    // ═══════════════════════════════════════════════════════════════════
    // 阴影/高度系统 - 层级递进
    // ═══════════════════════════════════════════════════════════════════
    object Elevation {
        /** 平面: 背景内容 */
        val Level0: Dp = 0.dp

        /** 微浮: 卡片默认态 */
        val Level1: Dp = 1.dp

        /** 轻浮: 卡片悬停态 */
        val Level2: Dp = 3.dp

        /** 中浮: 下拉菜单、Chip */
        val Level3: Dp = 6.dp

        /** 高浮: 底部栏、悬浮按钮 */
        val Level4: Dp = 8.dp

        /** 顶层: 对话框、全屏覆盖 */
        val Level5: Dp = 12.dp
    }

    // ═══════════════════════════════════════════════════════════════════
    // 图标尺寸系统
    // ═══════════════════════════════════════════════════════════════════
    object IconSize {
        /** 极小: 徽章内图标 */
        val XS: Dp = 14.dp

        /** 小型: Chip内图标、紧凑按钮 */
        val S: Dp = 18.dp

        /** 中型: 底部栏图标 */
        val M: Dp = 20.dp

        /** 标准: 列表项、顶栏图标 */
        val L: Dp = 24.dp

        /** 大型: 状态指示 */
        val XL: Dp = 32.dp

        /** 超大: 空状态、快捷操作 */
        val XXL: Dp = 48.dp

        /** 巨型: 空状态主图标 */
        val XXXL: Dp = 56.dp
    }

    // ═══════════════════════════════════════════════════════════════════
    // 透明度系统 - 语义化命名
    // ═══════════════════════════════════════════════════════════════════
    object Alpha {
        /** 禁用态 */
        const val Disabled: Float = 0.38f

        /** 中等强调 */
        const val Medium: Float = 0.60f

        /** 高强调 */
        const val High: Float = 0.87f

        /** 悬停叠加 */
        const val Hover: Float = 0.08f

        /** 焦点叠加 */
        const val Focus: Float = 0.12f

        /** 按下叠加 */
        const val Pressed: Float = 0.16f

        /** 遮罩层 */
        const val Scrim: Float = 0.32f

        /** 覆盖层背景 */
        const val Overlay: Float = 0.80f

        /** 表面着色 */
        const val SurfaceTint: Float = 0.05f

        /** 容器着色 */
        const val ContainerTint: Float = 0.12f

        /** 微妙光泽 */
        const val Gloss: Float = 0.03f

        /** 光晕效果 */
        const val Glow: Float = 0.25f
    }

    // ═══════════════════════════════════════════════════════════════════
    // 组件尺寸系统
    // ═══════════════════════════════════════════════════════════════════
    object ComponentSize {
        /** 底部操作栏高度 */
        val BottomBarHeight: Dp = 72.dp

        /** 底部操作项图标容器 */
        val BottomBarIconContainer: Dp = 44.dp

        /** 主操作按钮高度 */
        val MainButtonHeight: Dp = 56.dp

        /** 快捷操作图标尺寸 */
        val QuickActionIcon: Dp = 48.dp

        /** 预览条缩略图尺寸 */
        val PreviewStripItem: Dp = 40.dp

        /** 预览条当前项尺寸 */
        val PreviewStripCurrentItem: Dp = 52.dp

        /** 状态徽章小尺寸 */
        val BadgeSmall: Dp = 18.dp

        /** 状态徽章中尺寸 */
        val BadgeMedium: Dp = 24.dp

        /** 状态徽章大尺寸 */
        val BadgeLarge: Dp = 32.dp
    }
}
