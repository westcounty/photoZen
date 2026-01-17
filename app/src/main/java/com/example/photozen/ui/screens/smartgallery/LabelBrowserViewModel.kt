package com.example.photozen.ui.screens.smartgallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.data.repository.LabelRepository
import com.example.photozen.data.repository.LabelWithCount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for Label Browser screen.
 */
data class LabelBrowserUiState(
    val allLabels: List<LabelWithCount> = emptyList(),
    val filteredLabels: List<LabelWithCount> = emptyList(),
    val topLabels: List<LabelWithCount> = emptyList(),
    val searchQuery: String = "",
    val sortOrder: LabelSortOrder = LabelSortOrder.COUNT_DESC,
    val isLoading: Boolean = true,
    val error: String? = null
) {
    val hasLabels: Boolean
        get() = allLabels.isNotEmpty()
    
    val totalLabelCount: Int
        get() = allLabels.size
}

/**
 * Sort order for labels.
 */
enum class LabelSortOrder {
    COUNT_DESC,     // By count, highest first
    COUNT_ASC,      // By count, lowest first
    NAME_ASC,       // Alphabetically A-Z
    NAME_DESC       // Alphabetically Z-A
}

/**
 * ViewModel for Label Browser screen.
 */
@HiltViewModel
class LabelBrowserViewModel @Inject constructor(
    private val labelRepository: LabelRepository
) : ViewModel() {
    
    private val _searchQuery = MutableStateFlow("")
    private val _sortOrder = MutableStateFlow(LabelSortOrder.COUNT_DESC)
    private val _isLoading = MutableStateFlow(true)
    private val _error = MutableStateFlow<String?>(null)
    
    val uiState: StateFlow<LabelBrowserUiState> = combine(
        labelRepository.getAllLabelsWithCountFlow(),
        _searchQuery,
        _sortOrder,
        _isLoading,
        _error
    ) { allLabels, query, sortOrder, isLoading, error ->
        val sortedLabels = sortLabels(allLabels, sortOrder)
        val filteredLabels = if (query.isBlank()) {
            sortedLabels
        } else {
            sortedLabels.filter { it.label.contains(query, ignoreCase = true) }
        }
        val topLabels = allLabels.sortedByDescending { it.count }.take(10)
        
        LabelBrowserUiState(
            allLabels = sortedLabels,
            filteredLabels = filteredLabels,
            topLabels = topLabels,
            searchQuery = query,
            sortOrder = sortOrder,
            isLoading = false,
            error = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LabelBrowserUiState()
    )
    
    init {
        loadLabels()
    }
    
    private fun loadLabels() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // The Flow will automatically update
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "加载标签失败: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Update search query.
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    /**
     * Update sort order.
     */
    fun setSortOrder(order: LabelSortOrder) {
        _sortOrder.value = order
    }
    
    /**
     * Clear error.
     */
    fun clearError() {
        _error.value = null
    }
    
    /**
     * Sort labels by given order.
     */
    private fun sortLabels(labels: List<LabelWithCount>, order: LabelSortOrder): List<LabelWithCount> {
        return when (order) {
            LabelSortOrder.COUNT_DESC -> labels.sortedByDescending { it.count }
            LabelSortOrder.COUNT_ASC -> labels.sortedBy { it.count }
            LabelSortOrder.NAME_ASC -> labels.sortedBy { it.label.lowercase() }
            LabelSortOrder.NAME_DESC -> labels.sortedByDescending { it.label.lowercase() }
        }
    }
    
    /**
     * Get sample photo URIs for a label.
     */
    suspend fun getSamplePhotos(label: String, limit: Int = 4): List<String> {
        return labelRepository.getSamplePhotosForLabel(label, limit)
    }
}
