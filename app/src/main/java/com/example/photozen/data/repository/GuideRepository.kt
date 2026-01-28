package com.example.photozen.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.example.photozen.domain.model.GuideKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 引导状态存储仓库
 *
 * 负责管理引导的完成状态，使用 DataStore 持久化存储。
 *
 * ## 存储格式
 * 使用 StringSet 存储已完成的引导 Key 名称。
 *
 * ## 使用方式
 * ```kotlin
 * // 检查引导是否完成
 * val isCompleted = guideRepository.isGuideCompleted(GuideKey.STATS_CALENDAR).first()
 *
 * // 标记引导完成
 * guideRepository.markGuideCompleted(GuideKey.STATS_CALENDAR)
 *
 * // 重置所有引导
 * guideRepository.resetAllGuides()
 * ```
 */
@Singleton
class GuideRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_COMPLETED_GUIDES = stringSetPreferencesKey("completed_guides")
    }
    
    /**
     * 获取已完成的引导列表
     * 
     * @return Flow<Set<GuideKey>> 已完成的引导集合
     */
    fun getCompletedGuides(): Flow<Set<GuideKey>> = dataStore.data.map { prefs ->
        prefs[KEY_COMPLETED_GUIDES]
            ?.mapNotNull { GuideKey.fromString(it) }
            ?.toSet()
            ?: emptySet()
    }
    
    /**
     * 检查某个引导是否已完成
     * 
     * @param key 引导点位
     * @return Flow<Boolean> 是否已完成
     */
    fun isGuideCompleted(key: GuideKey): Flow<Boolean> = 
        getCompletedGuides().map { key in it }
    
    /**
     * 同步检查某个引导是否已完成
     * 
     * @param key 引导点位
     * @return Boolean 是否已完成
     */
    suspend fun isGuideCompletedSync(key: GuideKey): Boolean = 
        isGuideCompleted(key).first()
    
    /**
     * 标记引导为已完成
     * 
     * @param key 引导点位
     */
    suspend fun markGuideCompleted(key: GuideKey) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_COMPLETED_GUIDES] ?: emptySet()
            prefs[KEY_COMPLETED_GUIDES] = current + key.name
        }
    }
    
    /**
     * 批量标记引导为已完成
     * 
     * @param keys 引导点位列表
     */
    suspend fun markGuidesCompleted(keys: List<GuideKey>) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_COMPLETED_GUIDES] ?: emptySet()
            prefs[KEY_COMPLETED_GUIDES] = current + keys.map { it.name }
        }
    }
    
    /**
     * 重置所有引导
     * 
     * 清除所有已完成状态，下次进入页面会重新显示引导。
     * 主要用于调试和用户主动重置。
     */
    suspend fun resetAllGuides() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_COMPLETED_GUIDES)
        }
    }
    
    /**
     * 重置指定引导
     * 
     * @param key 要重置的引导点位
     */
    suspend fun resetGuide(key: GuideKey) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_COMPLETED_GUIDES] ?: emptySet()
            prefs[KEY_COMPLETED_GUIDES] = current - key.name
        }
    }
    
    /**
     * 获取已完成引导数量
     */
    fun getCompletedCount(): Flow<Int> = 
        getCompletedGuides().map { it.size }
    
    /**
     * 获取总引导数量
     */
    val totalGuideCount: Int = GuideKey.entries.size
}
