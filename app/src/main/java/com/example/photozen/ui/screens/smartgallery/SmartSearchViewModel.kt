package com.example.photozen.ui.screens.smartgallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.ai.VectorSearchService
import com.example.photozen.data.local.dao.PhotoDao
import com.example.photozen.data.local.dao.PhotoLabelDao
import com.example.photozen.data.local.dao.LabelCount
import com.example.photozen.data.local.entity.PhotoEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for Smart Search Screen.
 */
data class SmartSearchUiState(
    val query: String = "",
    val searchResults: List<SearchResultItem> = emptyList(),
    val isSearching: Boolean = false,
    val searchType: SearchType = SearchType.ALL,
    val topLabels: List<LabelCount> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val error: String? = null,
    val totalResults: Int = 0
)

/**
 * Search result item combining photo info with match details.
 */
data class SearchResultItem(
    val photo: PhotoEntity,
    val matchType: VectorSearchService.MatchType,
    val score: Float
)

/**
 * Type of search filter.
 */
enum class SearchType {
    ALL,
    LABELS,
    PEOPLE,
    LOCATION
}

/**
 * ViewModel for Smart Search functionality.
 */
@HiltViewModel
class SmartSearchViewModel @Inject constructor(
    private val vectorSearchService: VectorSearchService,
    private val photoLabelDao: PhotoLabelDao,
    private val photoDao: PhotoDao
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SmartSearchUiState())
    val uiState: StateFlow<SmartSearchUiState> = _uiState.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    private var searchJob: Job? = null
    
    init {
        loadTopLabels()
        observeSearchQuery()
    }
    
    /**
     * Load top labels for search suggestions.
     */
    private fun loadTopLabels() {
        viewModelScope.launch {
            photoLabelDao.getTopLabels(20).collect { labels ->
                _uiState.update { it.copy(topLabels = labels) }
            }
        }
    }
    
    /**
     * Observe search query with debounce for real-time search.
     */
    @OptIn(FlowPreview::class)
    private fun observeSearchQuery() {
        viewModelScope.launch {
            _searchQuery
                .debounce(300) // 300ms debounce
                .distinctUntilChanged()
                .collect { query ->
                    if (query.isNotBlank()) {
                        performSearch(query)
                    } else {
                        clearResults()
                    }
                }
        }
    }
    
    /**
     * Update search query.
     */
    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        _searchQuery.value = query
    }
    
    /**
     * Change search type filter.
     */
    fun onSearchTypeChange(type: SearchType) {
        _uiState.update { it.copy(searchType = type) }
        // Re-run search with new filter
        if (_uiState.value.query.isNotBlank()) {
            performSearch(_uiState.value.query)
        }
    }
    
    /**
     * Perform search with current query and filters.
     */
    private fun performSearch(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, error = null) }
            
            try {
                val results = vectorSearchService.search(query, limit = 100)
                
                // Filter by search type
                val filteredResults = when (_uiState.value.searchType) {
                    SearchType.ALL -> results
                    SearchType.LABELS -> results.filter { 
                        it.matchType == VectorSearchService.MatchType.LABEL ||
                        it.matchType == VectorSearchService.MatchType.COMBINED
                    }
                    SearchType.PEOPLE -> results.filter { 
                        it.matchType == VectorSearchService.MatchType.PERSON ||
                        it.matchType == VectorSearchService.MatchType.COMBINED
                    }
                    SearchType.LOCATION -> emptyList() // TODO: Implement location search
                }
                
                // Get photo details for results
                val searchResultItems = filteredResults.mapNotNull { result ->
                    val photo = photoDao.getById(result.photoId)
                    photo?.let {
                        SearchResultItem(
                            photo = it,
                            matchType = result.matchType,
                            score = result.score
                        )
                    }
                }
                
                _uiState.update { 
                    it.copy(
                        searchResults = searchResultItems,
                        totalResults = searchResultItems.size,
                        isSearching = false
                    )
                }
                
                // Save to recent searches
                if (searchResultItems.isNotEmpty()) {
                    addToRecentSearches(query)
                }
                
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isSearching = false,
                        error = "搜索失败: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Search by a specific label.
     */
    fun searchByLabel(label: String) {
        onQueryChange(label)
        _uiState.update { it.copy(searchType = SearchType.LABELS) }
    }
    
    /**
     * Clear search results.
     */
    private fun clearResults() {
        _uiState.update { 
            it.copy(
                searchResults = emptyList(),
                totalResults = 0,
                isSearching = false
            )
        }
    }
    
    /**
     * Clear the current search.
     */
    fun clearSearch() {
        _uiState.update { 
            it.copy(
                query = "",
                searchResults = emptyList(),
                totalResults = 0,
                isSearching = false
            )
        }
        _searchQuery.value = ""
    }
    
    /**
     * Add query to recent searches.
     */
    private fun addToRecentSearches(query: String) {
        val currentRecent = _uiState.value.recentSearches.toMutableList()
        currentRecent.remove(query) // Remove if exists
        currentRecent.add(0, query) // Add to front
        
        // Keep only last 10
        val updated = currentRecent.take(10)
        _uiState.update { it.copy(recentSearches = updated) }
    }
    
    /**
     * Clear error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
