package com.suvojeet.suvmusic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.data.repository.LocalAudioRepository
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val filter: String = YouTubeRepository.FILTER_SONGS,
    val results: List<Song> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val showSuggestions: Boolean = false,
    val isLoading: Boolean = false,
    val isSuggestionsLoading: Boolean = false,
    val error: String? = null
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val youTubeRepository: YouTubeRepository,
    private val localAudioRepository: LocalAudioRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    private var suggestionJob: Job? = null
    private var searchJob: Job? = null
    
    init {
        // Observe query changes for debounced suggestions
        viewModelScope.launch {
            _searchQuery
                .debounce(300) // 300ms debounce
                .distinctUntilChanged()
                .filter { it.isNotBlank() }
                .collect { query ->
                    fetchSuggestions(query)
                }
        }
    }
    
    fun onQueryChange(query: String) {
        _uiState.update { 
            it.copy(
                query = query,
                showSuggestions = query.isNotBlank()
            )
        }
        _searchQuery.value = query
        
        // Clear suggestions if query is empty
        if (query.isBlank()) {
            _uiState.update { 
                it.copy(
                    suggestions = emptyList(),
                    showSuggestions = false
                )
            }
        }
    }
    
    private fun fetchSuggestions(query: String) {
        suggestionJob?.cancel()
        suggestionJob = viewModelScope.launch {
            _uiState.update { it.copy(isSuggestionsLoading = true) }
            try {
                val suggestions = youTubeRepository.getSearchSuggestions(query)
                _uiState.update { 
                    it.copy(
                        suggestions = suggestions,
                        isSuggestionsLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSuggestionsLoading = false) }
            }
        }
    }
    
    fun onSuggestionClick(suggestion: String) {
        _uiState.update { 
            it.copy(
                query = suggestion,
                showSuggestions = false
            )
        }
        search()
    }
    
    fun hideSuggestions() {
        _uiState.update { it.copy(showSuggestions = false) }
    }
    
    fun setFilter(filter: String) {
        _uiState.update { it.copy(filter = filter) }
        if (_uiState.value.query.isNotBlank()) {
            search()
        }
    }
    
    fun search() {
        val query = _uiState.value.query
        if (query.isBlank()) return
        
        // Hide suggestions when searching
        _uiState.update { it.copy(showSuggestions = false) }
        
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // Search both YouTube and local
                val youtubeResults = youTubeRepository.search(query, _uiState.value.filter)
                val localResults = localAudioRepository.searchLocalSongs(query)
                
                val combined = localResults + youtubeResults
                
                _uiState.update { 
                    it.copy(
                        results = combined,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        error = e.message,
                        isLoading = false
                    )
                }
            }
        }
    }
}
