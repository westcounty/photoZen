package com.example.photozen.ui.screens.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.data.local.dao.TagDao
import com.example.photozen.data.local.dao.TagWithCount
import com.example.photozen.data.local.entity.TagEntity
import com.example.photozen.ui.components.bubble.BubbleNode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * UI State for the Tag Bubble screen.
 * Simplified: Only one level of tags (no hierarchy for now).
 */
data class TagBubbleUiState(
    val isLoading: Boolean = true,
    val bubbleNodes: List<BubbleNode> = emptyList(),
    val error: String? = null
) {
    val currentTitle: String = "标签"
}

/**
 * ViewModel for the Tag Bubble screen.
 * 
 * Manages the bubble graph visualization of tags with physics simulation.
 * Simplified: Only one level of tags for now.
 */
@HiltViewModel
class TagBubbleViewModel @Inject constructor(
    private val tagDao: TagDao
) : ViewModel() {
    
    companion object {
        // Bubble size constants - increased for better touch targets
        private const val MIN_BUBBLE_RADIUS = 80f
        private const val MAX_BUBBLE_RADIUS = 140f
    }
    
    private val _uiState = MutableStateFlow(TagBubbleUiState())
    val uiState: StateFlow<TagBubbleUiState> = _uiState.asStateFlow()
    
    private var collectionJob: Job? = null
    
    init {
        loadTags()
    }
    
    /**
     * Load all root tags.
     */
    private fun loadTags() {
        collectionJob?.cancel()
        
        _uiState.update { it.copy(isLoading = true, error = null) }
        
        collectionJob = viewModelScope.launch {
            try {
                tagDao.getRootTagsWithPhotoCount().collect { tags ->
                    val nodes = createBubbleNodes(tags)
                    _uiState.update { 
                        it.copy(isLoading = false, bubbleNodes = nodes, error = null) 
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(isLoading = false, error = "加载标签失败: ${e.message}") 
                }
            }
        }
    }
    
    /**
     * Create a new tag.
     */
    fun createTag(name: String, color: Int) {
        viewModelScope.launch {
            try {
                val tag = TagEntity(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    parentId = null, // Root level only
                    color = color
                )
                tagDao.insert(tag)
                // Flow collection will auto-update the UI
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "创建标签失败: ${e.message}") }
            }
        }
    }
    
    /**
     * Delete a tag.
     */
    fun deleteTag(tagId: String) {
        viewModelScope.launch {
            try {
                tagDao.deleteById(tagId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "删除标签失败: ${e.message}") }
            }
        }
    }
    
    /**
     * Clear error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    /**
     * Convert TagWithCount list to BubbleNode list.
     */
    private fun createBubbleNodes(tags: List<TagWithCount>): List<BubbleNode> {
        if (tags.isEmpty()) return emptyList()
        
        val maxCount = tags.maxOfOrNull { it.photoCount }?.coerceAtLeast(1) ?: 1
        
        return tags.map { tag ->
            val sizeRatio = if (maxCount > 0) {
                (tag.photoCount.toFloat() / maxCount).coerceIn(0.3f, 1f)
            } else {
                0.5f
            }
            
            val radius = MIN_BUBBLE_RADIUS + 
                (MAX_BUBBLE_RADIUS - MIN_BUBBLE_RADIUS) * sizeRatio
            
            BubbleNode(
                id = tag.id,
                label = tag.name,
                radius = radius,
                color = tag.color,
                photoCount = tag.photoCount,
                isCenter = false,
                hasChildren = false
            )
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        collectionJob?.cancel()
    }
}
