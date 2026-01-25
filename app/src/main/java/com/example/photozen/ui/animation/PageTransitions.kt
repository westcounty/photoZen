package com.example.photozen.ui.animation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.navigation.NavBackStackEntry
import com.example.photozen.ui.theme.PicZenMotion

/**
 * 页面过渡动画系统 (DES-031, DES-032)
 *
 * 提供标准化的页面过渡动画，确保全应用一致的过渡体验。
 *
 * ## 动画类型
 *
 * - 水平滑动: 主要页面导航
 * - 垂直滑动: 模态页面、底部弹出
 * - 渐变缩放: 详情页面
 * - 共享元素: 照片预览
 */
object PageTransitions {

    // ═══════════════════════════════════════════════════════════════════
    // 水平滑动过渡 - 标准页面导航
    // ═══════════════════════════════════════════════════════════════════

    /**
     * 标准水平进入动画
     * 从右侧滑入 + 淡入
     */
    val horizontalEnter: EnterTransition = slideInHorizontally(
        initialOffsetX = { fullWidth -> fullWidth / 4 },
        animationSpec = tween(
            durationMillis = PicZenMotion.Duration.Moderate,
            easing = PicZenMotion.Easing.EmphasizedDecelerate
        )
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = PicZenMotion.Duration.Fast,
            easing = PicZenMotion.Easing.StandardDecelerate
        )
    )

    /**
     * 标准水平退出动画
     * 向左滑出 + 淡出
     */
    val horizontalExit: ExitTransition = slideOutHorizontally(
        targetOffsetX = { fullWidth -> -fullWidth / 4 },
        animationSpec = tween(
            durationMillis = PicZenMotion.Duration.Fast,
            easing = PicZenMotion.Easing.StandardAccelerate
        )
    ) + fadeOut(
        animationSpec = tween(
            durationMillis = PicZenMotion.Duration.Quick,
            easing = PicZenMotion.Easing.StandardAccelerate
        )
    )

    /**
     * 水平返回进入动画
     * 从左侧滑入
     */
    val horizontalPopEnter: EnterTransition = slideInHorizontally(
        initialOffsetX = { fullWidth -> -fullWidth / 4 },
        animationSpec = tween(
            durationMillis = PicZenMotion.Duration.Moderate,
            easing = PicZenMotion.Easing.EmphasizedDecelerate
        )
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = PicZenMotion.Duration.Fast,
            easing = PicZenMotion.Easing.StandardDecelerate
        )
    )

    /**
     * 水平返回退出动画
     * 向右滑出
     */
    val horizontalPopExit: ExitTransition = slideOutHorizontally(
        targetOffsetX = { fullWidth -> fullWidth / 4 },
        animationSpec = tween(
            durationMillis = PicZenMotion.Duration.Fast,
            easing = PicZenMotion.Easing.StandardAccelerate
        )
    ) + fadeOut(
        animationSpec = tween(
            durationMillis = PicZenMotion.Duration.Quick,
            easing = PicZenMotion.Easing.StandardAccelerate
        )
    )

    // ═══════════════════════════════════════════════════════════════════
    // 垂直滑动过渡 - 模态页面
    // ═══════════════════════════════════════════════════════════════════

    /**
     * 模态进入动画
     * 从底部滑入 + 淡入
     */
    val modalEnter: EnterTransition = slideInVertically(
        initialOffsetY = { fullHeight -> fullHeight / 3 },
        animationSpec = tween(
            durationMillis = PicZenMotion.Duration.Moderate,
            easing = PicZenMotion.Easing.EmphasizedDecelerate
        )
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = PicZenMotion.Duration.Fast,
            easing = PicZenMotion.Easing.StandardDecelerate
        )
    )

    /**
     * 模态退出动画
     * 向下滑出 + 淡出
     */
    val modalExit: ExitTransition = slideOutVertically(
        targetOffsetY = { fullHeight -> fullHeight / 3 },
        animationSpec = tween(
            durationMillis = PicZenMotion.Duration.Fast,
            easing = PicZenMotion.Easing.StandardAccelerate
        )
    ) + fadeOut(
        animationSpec = tween(
            durationMillis = PicZenMotion.Duration.Quick,
            easing = PicZenMotion.Easing.StandardAccelerate
        )
    )

    // ═══════════════════════════════════════════════════════════════════
    // 渐变缩放过渡 - 详情页面
    // ═══════════════════════════════════════════════════════════════════

    /**
     * 详情页进入动画
     * 缩放放大 + 淡入
     */
    val detailEnter: EnterTransition = scaleIn(
        initialScale = 0.92f,
        animationSpec = tween(
            durationMillis = PicZenMotion.Duration.Moderate,
            easing = PicZenMotion.Easing.EmphasizedDecelerate
        )
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = PicZenMotion.Duration.Fast,
            easing = PicZenMotion.Easing.StandardDecelerate
        )
    )

    /**
     * 详情页退出动画
     * 缩放缩小 + 淡出
     */
    val detailExit: ExitTransition = scaleOut(
        targetScale = 0.92f,
        animationSpec = tween(
            durationMillis = PicZenMotion.Duration.Fast,
            easing = PicZenMotion.Easing.StandardAccelerate
        )
    ) + fadeOut(
        animationSpec = tween(
            durationMillis = PicZenMotion.Duration.Quick,
            easing = PicZenMotion.Easing.StandardAccelerate
        )
    )

    // ═══════════════════════════════════════════════════════════════════
    // 全屏预览过渡 - 照片查看器
    // ═══════════════════════════════════════════════════════════════════

    /**
     * 全屏进入动画
     * 放大 + 淡入
     */
    val fullscreenEnter: EnterTransition = scaleIn(
        initialScale = 0.85f,
        animationSpec = tween(
            durationMillis = PicZenMotion.Duration.Moderate,
            easing = PicZenMotion.Easing.EmphasizedDecelerate
        )
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = PicZenMotion.Duration.Normal,
            easing = PicZenMotion.Easing.StandardDecelerate
        )
    )

    /**
     * 全屏退出动画
     * 缩小 + 淡出
     */
    val fullscreenExit: ExitTransition = scaleOut(
        targetScale = 0.85f,
        animationSpec = tween(
            durationMillis = PicZenMotion.Duration.Fast,
            easing = PicZenMotion.Easing.StandardAccelerate
        )
    ) + fadeOut(
        animationSpec = tween(
            durationMillis = PicZenMotion.Duration.Quick,
            easing = PicZenMotion.Easing.StandardAccelerate
        )
    )

    // ═══════════════════════════════════════════════════════════════════
    // 淡入淡出过渡 - 简单切换
    // ═══════════════════════════════════════════════════════════════════

    /**
     * 简单淡入
     */
    val fadeEnter: EnterTransition = fadeIn(
        animationSpec = tween(
            durationMillis = PicZenMotion.Duration.Fast,
            easing = PicZenMotion.Easing.StandardDecelerate
        )
    )

    /**
     * 简单淡出
     */
    val fadeExit: ExitTransition = fadeOut(
        animationSpec = tween(
            durationMillis = PicZenMotion.Duration.Quick,
            easing = PicZenMotion.Easing.StandardAccelerate
        )
    )
}

/**
 * 预定义的导航过渡组合
 *
 * 用于 NavHost 的 enterTransition/exitTransition 等参数。
 */
object NavTransitions {

    /**
     * 标准页面过渡
     */
    fun standardEnter(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        PageTransitions.horizontalEnter
    }

    fun standardExit(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        PageTransitions.horizontalExit
    }

    fun standardPopEnter(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        PageTransitions.horizontalPopEnter
    }

    fun standardPopExit(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        PageTransitions.horizontalPopExit
    }

    /**
     * 模态页面过渡
     */
    fun modalEnter(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        PageTransitions.modalEnter
    }

    fun modalExit(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        PageTransitions.modalExit
    }

    /**
     * 详情页过渡
     */
    fun detailEnter(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        PageTransitions.detailEnter
    }

    fun detailExit(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        PageTransitions.detailExit
    }
}
