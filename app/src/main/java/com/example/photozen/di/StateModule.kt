package com.example.photozen.di

import com.example.photozen.data.repository.PhotoRepository
import com.example.photozen.data.repository.PreferencesRepository
import com.example.photozen.data.repository.StatsRepository
import com.example.photozen.domain.usecase.PhotoBatchOperationUseCase
import com.example.photozen.ui.state.PhotoSelectionStateHolder
import com.example.photozen.ui.state.SnackbarManager
import com.example.photozen.ui.state.UndoManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 状态管理相关依赖注入模块
 * 
 * 提供 Phase 4 新增的状态管理组件的依赖注入配置。
 * 
 * ## 提供的组件
 * 
 * - PhotoSelectionStateHolder: 共享照片选择状态
 * - PhotoBatchOperationUseCase: 批量操作 UseCase
 * 
 * ## 注意事项
 * 
 * - UndoManager 和 SnackbarManager 已在其类上标注 @Singleton @Inject constructor
 *   因此 Hilt 会自动提供，无需在此模块中显式声明
 * - PhotoSelectionStateHolder 同理，已标注 @Singleton
 * 
 * @since Phase 4 - 状态管理重构
 */
@Module
@InstallIn(SingletonComponent::class)
object StateModule {
    
    /**
     * 提供 PhotoBatchOperationUseCase
     * 
     * 注意：UseCase 通常不需要单例，每次注入时创建新实例。
     * 但由于其依赖的 UndoManager 和 SnackbarManager 是单例，
     * 所以 UseCase 也作为单例提供以保持一致性。
     */
    @Provides
    @Singleton
    fun providePhotoBatchOperationUseCase(
        photoRepository: PhotoRepository,
        undoManager: UndoManager,
        snackbarManager: SnackbarManager,
        preferencesRepository: PreferencesRepository,
        statsRepository: StatsRepository
    ): PhotoBatchOperationUseCase {
        return PhotoBatchOperationUseCase(
            photoRepository = photoRepository,
            undoManager = undoManager,
            snackbarManager = snackbarManager,
            preferencesRepository = preferencesRepository,
            statsRepository = statsRepository
        )
    }
    
    // 以下组件已在类定义中使用 @Singleton @Inject constructor 标注，
    // Hilt 会自动提供，无需在此显式声明：
    // - PhotoSelectionStateHolder
    // - UndoManager
    // - SnackbarManager
}

/**
 * 状态组件使用指南
 * 
 * ## PhotoSelectionStateHolder
 * 
 * 用于在多个 ViewModel 间共享照片选择状态。
 * 
 * ```kotlin
 * @HiltViewModel
 * class MyViewModel @Inject constructor(
 *     val selectionStateHolder: PhotoSelectionStateHolder
 * ) : ViewModel() {
 *     // 直接使用 selectionStateHolder.toggle(), .selectAll() 等方法
 * }
 * ```
 * 
 * ## PhotoBatchOperationUseCase
 * 
 * 用于执行批量照片操作，自动处理撤销、Snackbar 和统计。
 * 
 * ```kotlin
 * @HiltViewModel
 * class MyViewModel @Inject constructor(
 *     private val batchOperationUseCase: PhotoBatchOperationUseCase
 * ) : ViewModel() {
 *     fun deleteSelected(ids: List<String>) {
 *         viewModelScope.launch {
 *             batchOperationUseCase.batchTrash(ids)
 *         }
 *     }
 * }
 * ```
 * 
 * ## UndoManager
 * 
 * 用于管理撤销操作。通常通过 PhotoBatchOperationUseCase 自动使用，
 * 也可以直接注入以进行自定义撤销处理。
 * 
 * ```kotlin
 * @HiltViewModel
 * class MyViewModel @Inject constructor(
 *     private val undoManager: UndoManager
 * ) : ViewModel() {
 *     fun undo() {
 *         viewModelScope.launch {
 *             undoManager.undo()
 *         }
 *     }
 * }
 * ```
 * 
 * ## SnackbarManager
 * 
 * 用于显示全局 Snackbar。通常通过 PhotoBatchOperationUseCase 自动使用，
 * 也可以直接注入以显示自定义消息。
 * 
 * ```kotlin
 * @HiltViewModel
 * class MyViewModel @Inject constructor(
 *     private val snackbarManager: SnackbarManager
 * ) : ViewModel() {
 *     fun showMessage() {
 *         snackbarManager.showSuccess("操作成功")
 *     }
 * }
 * ```
 */
