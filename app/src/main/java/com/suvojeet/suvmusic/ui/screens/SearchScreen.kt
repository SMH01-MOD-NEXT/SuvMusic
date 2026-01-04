package com.suvojeet.suvmusic.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import com.suvojeet.suvmusic.ui.components.AnimatedSearchBar
import com.suvojeet.suvmusic.ui.components.MusicCard
import com.suvojeet.suvmusic.ui.viewmodel.SearchViewModel

/**
 * Search screen with filter chips and results.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onSongClick: (Song) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        // Title
        Text(
            text = "Search",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Search Bar
        AnimatedSearchBar(
            query = uiState.query,
            onQueryChange = { viewModel.onQueryChange(it) },
            onSearch = { viewModel.search() }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChipItem(
                label = "Songs",
                selected = uiState.filter == YouTubeRepository.FILTER_SONGS,
                onClick = { viewModel.setFilter(YouTubeRepository.FILTER_SONGS) }
            )
            FilterChipItem(
                label = "Albums",
                selected = uiState.filter == YouTubeRepository.FILTER_ALBUMS,
                onClick = { viewModel.setFilter(YouTubeRepository.FILTER_ALBUMS) }
            )
            FilterChipItem(
                label = "Artists",
                selected = uiState.filter == YouTubeRepository.FILTER_ARTISTS,
                onClick = { viewModel.setFilter(YouTubeRepository.FILTER_ARTISTS) }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Results
        LazyColumn(
            contentPadding = PaddingValues(bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.results) { song ->
                MusicCard(
                    song = song,
                    onClick = { onSongClick(song) }
                )
            }
            
            if (uiState.results.isEmpty() && uiState.query.isNotBlank() && !uiState.isLoading) {
                item {
                    Text(
                        text = "No results found for \"${uiState.query}\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 32.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChipItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}
