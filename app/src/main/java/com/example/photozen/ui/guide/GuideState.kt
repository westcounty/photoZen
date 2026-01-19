package com.example.photozen.ui.guide

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.example.photozen.data.repository.GuideRepository
import com.example.photozen.domain.model.GuideKey
import kotlinx.coroutines.launch

/**
 * 引导状态
 * 
 * @property shouldShow 是否应该显示引导
 * @property dismiss 关闭引导的回调
 */
data class GuideState(
    val shouldShow: Boolean,
    val dismiss: () -> Unit
)

/**
 * 引导序列状态
 * 
 * 用于管理多步引导（如 FlowSorter 的 3 步滑动引导）
 * 
 * @property currentGuide 当前应该显示的引导（null 表示序列已完成）
 * @property currentStep 当前步骤（从 1 开始）
 * @property totalSteps 总步骤数
 * @property dismissCurrent 关闭当前引导，进入下一步
 * @property skipAll 跳过所有引导
 */
data class GuideSequenceState(
    val currentGuide: GuideKey?,
    val currentStep: Int,
    val totalSteps: Int,
    val dismissCurrent: () -> Unit,
    val skipAll: () -> Unit
) {
    val isActive: Boolean get() = currentGuide != null
}

/**
 * 记住单个引导状态
 * 
 * 在 Composable 中便捷管理单个引导的显示状态。
 * 
 * @param guideKey 引导点位
 * @param guideRepository 引导仓库
 * @return GuideState 引导状态
 * 
 * ## 使用示例
 * ```kotlin
 * @Composable
 * fun MyScreen(guideRepository: GuideRepository) {
 *     val guideState = rememberGuideState(GuideKey.PHOTO_LIST_LONG_PRESS, guideRepository)
 *     
 *     // 显示引导
 *     if (guideState.shouldShow) {
 *         GuideTooltip(
 *             visible = true,
 *             message = "长按选择照片",
 *             targetBounds = bounds,
 *             onDismiss = guideState.dismiss
 *         )
 *     }
 * }
 * ```
 */
@Composable
fun rememberGuideState(
    guideKey: GuideKey,
    guideRepository: GuideRepository
): GuideState {
    val scope = rememberCoroutineScope()
    val isCompleted by guideRepository.isGuideCompleted(guideKey)
        .collectAsState(initial = true) // 默认为已完成，避免闪烁
    
    return remember(guideKey, isCompleted) {
        GuideState(
            shouldShow = !isCompleted,
            dismiss = {
                scope.launch {
                    guideRepository.markGuideCompleted(guideKey)
                }
            }
        )
    }
}

/**
 * 记住引导序列状态
 * 
 * 用于管理多步引导序列，如 FlowSorter 的滑动引导。
 * 
 * @param guideKeys 引导序列（按顺序）
 * @param guideRepository 引导仓库
 * @return GuideSequenceState 序列状态
 * 
 * ## 使用示例
 * ```kotlin
 * @Composable
 * fun FlowSorterScreen(guideRepository: GuideRepository) {
 *     val guideSequence = rememberGuideSequenceState(
 *         guideKeys = GuideKey.flowSorterSequence,
 *         guideRepository = guideRepository
 *     )
 *     
 *     // 根据当前引导显示不同内容
 *     when (guideSequence.currentGuide) {
 *         GuideKey.SWIPE_RIGHT -> GuideTooltip(message = "右滑保留", ...)
 *         GuideKey.SWIPE_LEFT -> GuideTooltip(message = "左滑删除", ...)
 *         GuideKey.SWIPE_UP -> GuideTooltip(message = "上滑待定", ...)
 *         else -> { /* 引导完成 */ }
 *     }
 * }
 * ```
 */
@Composable
fun rememberGuideSequenceState(
    guideKeys: List<GuideKey>,
    guideRepository: GuideRepository
): GuideSequenceState {
    val scope = rememberCoroutineScope()
    val completedGuides by guideRepository.getCompletedGuides()
        .collectAsState(initial = emptySet())
    
    // 找到第一个未完成的引导
    val currentGuide = remember(completedGuides, guideKeys) {
        guideKeys.firstOrNull { it !in completedGuides }
    }
    
    val currentStep = remember(currentGuide, guideKeys) {
        if (currentGuide != null) {
            guideKeys.indexOf(currentGuide) + 1
        } else {
            guideKeys.size
        }
    }
    
    return remember(currentGuide, currentStep, guideKeys.size) {
        GuideSequenceState(
            currentGuide = currentGuide,
            currentStep = currentStep,
            totalSteps = guideKeys.size,
            dismissCurrent = {
                currentGuide?.let { guide ->
                    scope.launch {
                        guideRepository.markGuideCompleted(guide)
                    }
                }
            },
            skipAll = {
                scope.launch {
                    guideRepository.markGuidesCompleted(guideKeys)
                }
            }
        )
    }
}
