package com.example.photozen.ui.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically

/**
 * PhotoZen Motion System
 * 统一管理所有动画规范
 *
 * 设计原则:
 * - 快速响应，减少等待感
 * - 自然流畅，符合物理规律
 * - 有意义的动效，传达状态变化
 *
 * @see PicZenTokens 设计Token系统
 */
object PicZenMotion {

    // ═══════════════════════════════════════════════════════════════════
    // 时长系统 - 基于交互类型 (毫秒)
    // ═══════════════════════════════════════════════════════════════════
    object Duration {
        /** 即时反馈: 按钮状态切换 */
        const val Instant: Int = 50

        /** 快速: 开关切换、微交互 */
        const val Quick: Int = 100

        /** 较快: 淡入淡出、小元素 */
        const val Fast: Int = 150

        /** 标准: 尺寸变化、常规过渡 */
        const val Normal: Int = 200

        /** 中等: 页面过渡、复杂动画 */
        const val Moderate: Int = 300

        /** 较慢: 强调动画、大范围移动 */
        const val Slow: Int = 400

        /** 强调: 庆祝动效、成就解锁 */
        const val Emphasis: Int = 500

        /** 延长: 入场序列动画 */
        const val Extended: Int = 800
    }

    // ═══════════════════════════════════════════════════════════════════
    // 缓动曲线 - Material 3 标准 + 自定义
    // ═══════════════════════════════════════════════════════════════════
    object Easing {
        // ─────────────────────────────────────────────────────────────
        // 标准曲线 - 大多数过渡使用
        // ─────────────────────────────────────────────────────────────

        /** 标准曲线: 平滑进出 */
        val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)

        /** 标准减速: 进入时使用 */
        val StandardDecelerate = CubicBezierEasing(0f, 0f, 0f, 1f)

        /** 标准加速: 退出时使用 */
        val StandardAccelerate = CubicBezierEasing(0.3f, 0f, 1f, 1f)

        // ─────────────────────────────────────────────────────────────
        // 强调曲线 - 引入注意、重要动效
        // ─────────────────────────────────────────────────────────────

        /** 强调曲线: 吸引注意 */
        val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)

        /** 强调减速: 元素进入 */
        val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

        /** 强调加速: 元素退出 */
        val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

        // ─────────────────────────────────────────────────────────────
        // 自定义曲线 - 特殊效果
        // ─────────────────────────────────────────────────────────────

        /** 弹性曲线: 活泼反馈 */
        val Bounce = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

        /** 快出慢入: 手势跟随 */
        val FastOutSlowIn = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

        /** 线性: 持续动画 */
        val Linear = CubicBezierEasing(0f, 0f, 1f, 1f)
    }

    // ═══════════════════════════════════════════════════════════════════
    // Spring配置 - 统一弹性动画参数
    // ═══════════════════════════════════════════════════════════════════
    object Springs {
        /**
         * 快速响应 - 按钮、开关、即时反馈
         * 无弹跳，高刚度
         */
        fun <T> snappy() = spring<T>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        )

        /**
         * 标准弹性 - 卡片、列表项、一般交互
         * 轻微弹跳，中等刚度
         */
        fun <T> default() = spring<T>(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        )

        /**
         * 灵动弹性 - 滑动卡片、手势回弹
         * 明显弹跳，活泼感
         */
        fun <T> playful() = spring<T>(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )

        /**
         * 柔和弹性 - 页面过渡、大范围移动
         * 轻微弹跳，低刚度
         */
        fun <T> gentle() = spring<T>(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        )

        /**
         * 无弹跳 - 精确控制场景
         */
        fun <T> noBounce() = spring<T>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        )
    }

    // ═══════════════════════════════════════════════════════════════════
    // 预设动画Spec - 常用动画效果
    // ═══════════════════════════════════════════════════════════════════
    object Specs {
        // ─────────────────────────────────────────────────────────────
        // 淡入淡出
        // ─────────────────────────────────────────────────────────────

        val fadeInSpec: EnterTransition = fadeIn(
            animationSpec = tween(
                durationMillis = Duration.Fast,
                easing = Easing.StandardDecelerate
            )
        )

        val fadeOutSpec: ExitTransition = fadeOut(
            animationSpec = tween(
                durationMillis = Duration.Quick,
                easing = Easing.StandardAccelerate
            )
        )

        // ─────────────────────────────────────────────────────────────
        // 缩放
        // ─────────────────────────────────────────────────────────────

        val scaleInSpec: EnterTransition = scaleIn(
            initialScale = 0.92f,
            animationSpec = tween(
                durationMillis = Duration.Normal,
                easing = Easing.EmphasizedDecelerate
            )
        )

        val scaleOutSpec: ExitTransition = scaleOut(
            targetScale = 0.92f,
            animationSpec = tween(
                durationMillis = Duration.Quick,
                easing = Easing.StandardAccelerate
            )
        )

        // ─────────────────────────────────────────────────────────────
        // 滑入滑出 - 垂直方向
        // ─────────────────────────────────────────────────────────────

        val slideInFromBottomSpec: EnterTransition = slideInVertically(
            initialOffsetY = { it / 4 },
            animationSpec = tween(
                durationMillis = Duration.Moderate,
                easing = Easing.EmphasizedDecelerate
            )
        )

        val slideOutToBottomSpec: ExitTransition = slideOutVertically(
            targetOffsetY = { it / 4 },
            animationSpec = tween(
                durationMillis = Duration.Fast,
                easing = Easing.StandardAccelerate
            )
        )

        val slideInFromTopSpec: EnterTransition = slideInVertically(
            initialOffsetY = { -it / 4 },
            animationSpec = tween(
                durationMillis = Duration.Moderate,
                easing = Easing.EmphasizedDecelerate
            )
        )

        val slideOutToTopSpec: ExitTransition = slideOutVertically(
            targetOffsetY = { -it / 4 },
            animationSpec = tween(
                durationMillis = Duration.Fast,
                easing = Easing.StandardAccelerate
            )
        )

        // ─────────────────────────────────────────────────────────────
        // 组合效果
        // ─────────────────────────────────────────────────────────────

        /** 标准进入: 淡入 + 缩放 */
        val standardEnter: EnterTransition = fadeInSpec + scaleInSpec

        /** 标准退出: 淡出 + 缩放 */
        val standardExit: ExitTransition = fadeOutSpec + scaleOutSpec

        /** 底部弹出进入: 淡入 + 从下滑入 */
        val bottomSheetEnter: EnterTransition = fadeInSpec + slideInFromBottomSpec

        /** 底部弹出退出: 淡出 + 向下滑出 */
        val bottomSheetExit: ExitTransition = fadeOutSpec + slideOutToBottomSpec

        /** 顶部弹出进入 */
        val topSheetEnter: EnterTransition = fadeInSpec + slideInFromTopSpec

        /** 顶部弹出退出 */
        val topSheetExit: ExitTransition = fadeOutSpec + slideOutToTopSpec
    }

    // ═══════════════════════════════════════════════════════════════════
    // 延迟系统 - 序列动画
    // ═══════════════════════════════════════════════════════════════════
    object Delay {
        /** 列表项错开延迟 */
        const val StaggerItem: Long = 30L

        /** 快速序列 */
        const val QuickSequence: Long = 50L

        /** 标准序列 */
        const val StandardSequence: Long = 80L

        /** 强调序列 */
        const val EmphasisSequence: Long = 120L
    }
}
