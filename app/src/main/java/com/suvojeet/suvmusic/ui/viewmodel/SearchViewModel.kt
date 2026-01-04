package com.suvojeet.suvmusic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.data.repository.LocalAudioRepository
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val filter: String = YouTubeRepository.FILTER_SONGS,
    val results: List<Song> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val youTubeRepository: YouTubeRepository,
    private val localAudioRepository: LocalAudioRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    
    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
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
        
        viewModelScope.launch {
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
