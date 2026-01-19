package com.example.photozen.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.photozen.domain.model.FilterConfig
import com.example.photozen.domain.model.FilterPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 筛选预设存储仓库
 * 
 * 负责管理筛选预设的持久化存储。
 * 
 * ## 存储格式
 * 使用 JSON 序列化存储预设列表。
 * 
 * ## 预设限制
 * - 最多保存 3 个预设
 * - 超出时自动删除最旧的预设
 * 
 * ## 使用示例
 * ```kotlin
 * // 获取预设列表
 * filterPresetRepository.getPresets().collect { presets ->
 *     // 更新 UI
 * }
 * 
 * // 保存预设
 * filterPresetRepository.savePreset(preset)
 * 
 * // 删除预设
 * filterPresetRepository.deletePreset(presetId)
 * ```
 */
@Singleton
class FilterPresetRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_FILTER_PRESETS = stringPreferencesKey("filter_presets")
        private val KEY_LAST_FILTER_CONFIG = stringPreferencesKey("last_filter_config")
    }
    
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    // ==================== 预设管理 ====================
    
    /**
     * 获取所有筛选预设
     */
    fun getPresets(): Flow<List<FilterPreset>> = dataStore.data.map { prefs ->
        val jsonStr = prefs[KEY_FILTER_PRESETS] ?: return@map emptyList()
        try {
            json.decodeFromString<List<FilterPreset>>(jsonStr)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 同步获取所有预设
     */
    suspend fun getPresetsSync(): List<FilterPreset> = getPresets().first()
    
    /**
     * 保存筛选预设
     * 
     * 如果预设数量达到上限，会自动删除最旧的预设。
     * 
     * @param preset 要保存的预设
     * @return 是否保存成功
     */
    suspend fun savePreset(preset: FilterPreset): Boolean {
        return try {
            dataStore.edit { prefs ->
                val current = getPresetsSync().toMutableList()
                
                // 检查是否已存在同名预设
                current.removeAll { it.name == preset.name }
                
                // 达到上限时移除最旧的
                while (current.size >= FilterPreset.MAX_PRESETS) {
                    val oldest = current.minByOrNull { it.createdAt }
                    oldest?.let { current.remove(it) }
                }
                
                current.add(preset)
                prefs[KEY_FILTER_PRESETS] = json.encodeToString(current)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 删除筛选预设
     * 
     * @param presetId 预设 ID
     */
    suspend fun deletePreset(presetId: String) {
        dataStore.edit { prefs ->
            val current = getPresetsSync()
            val newList = current.filter { it.id != presetId }
            prefs[KEY_FILTER_PRESETS] = json.encodeToString(newList)
        }
    }
    
    /**
     * 清除所有预设
     */
    suspend fun clearAllPresets() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_FILTER_PRESETS)
        }
    }
    
    /**
     * 获取预设数量
     */
    fun getPresetCount(): Flow<Int> = getPresets().map { it.size }
    
    /**
     * 是否可以添加更多预设
     */
    fun canAddPreset(): Flow<Boolean> = 
        getPresetCount().map { it < FilterPreset.MAX_PRESETS }
    
    // ==================== 最近使用的筛选配置 ====================
    
    /**
     * 获取最近使用的筛选配置
     */
    fun getLastFilterConfig(): Flow<FilterConfig?> = dataStore.data.map { prefs ->
        val jsonStr = prefs[KEY_LAST_FILTER_CONFIG] ?: return@map null
        try {
            json.decodeFromString<FilterConfig>(jsonStr)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 保存最近使用的筛选配置
     */
    suspend fun saveLastFilterConfig(config: FilterConfig) {
        dataStore.edit { prefs ->
            prefs[KEY_LAST_FILTER_CONFIG] = json.encodeToString(config)
        }
    }
    
    /**
     * 清除最近使用的筛选配置
     */
    suspend fun clearLastFilterConfig() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_LAST_FILTER_CONFIG)
        }
    }
}
