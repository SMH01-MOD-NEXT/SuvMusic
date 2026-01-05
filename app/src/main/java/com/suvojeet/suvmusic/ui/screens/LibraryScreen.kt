package com.suvojeet.suvmusic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.suvmusic.data.model.PlaylistDisplayItem
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.ui.components.MusicCard
import com.suvojeet.suvmusic.ui.components.PlaylistCard
import com.suvojeet.suvmusic.ui.viewmodel.LibraryViewModel

/**
 * Library screen with playlists, offline music, and liked songs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onSongClick: (List<Song>, Int) -> Unit,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Playlists", "Offline", "Liked")
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // ... (lines 54-81 unchanged)
        Spacer(modifier = Modifier.height(16.dp))
        
        // Title
        Text(
            text = "Your Library",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Tabs
        PrimaryTabRow(
            selectedTabIndex = selectedTab
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Content based on selected tab
        when (selectedTab) {
            0 -> PlaylistsTab(
                playlists = uiState.playlists,
                onPlaylistClick = onPlaylistClick
            )
            1 -> OfflineTab(
                localSongs = uiState.localSongs,
                downloadedSongs = uiState.downloadedSongs,
                onSongClick = onSongClick
            )
            2 -> LikedTab(
                songs = uiState.likedSongs,
                onSongClick = onSongClick
            )
        }
    }
}

@Composable
private fun PlaylistsTab(
    playlists: List<PlaylistDisplayItem>,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(playlists) { playlist ->
            PlaylistCard(
                playlist = playlist,
                onClick = { onPlaylistClick(playlist) }
            )
        }
    }
}

@Composable
private fun OfflineTab(
    localSongs: List<Song>,
    downloadedSongs: List<Song>,
    onSongClick: (List<Song>, Int) -> Unit,
    onDownloadsClick: () -> Unit = {}
) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            bottom = 140.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (localSongs.isEmpty() && downloadedSongs.isEmpty()) {
            item {
                Text(
                    text = "No offline songs yet.\nDownload songs or add music to Downloads/SuvMusic folder.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 32.dp)
                )
            }
        }
        
        // Downloaded Songs Section Card (YouTube Music style)
        if (downloadedSongs.isNotEmpty()) {
            item {
                DownloadedSongsCard(
                    songCount = downloadedSongs.size,
                    totalDuration = downloadedSongs.sumOf { it.duration },
                    onClick = onDownloadsClick
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Show all downloaded songs
            itemsIndexed(downloadedSongs) { index, song ->
                MusicCard(
                    song = song,
                    onClick = { onSongClick(downloadedSongs, index) }
                )
            }
        }
        
        if (localSongs.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Device Files",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            itemsIndexed(localSongs) { index, song ->
                MusicCard(
                    song = song,
                    onClick = { onSongClick(localSongs, index) }
                )
            }
        }
    }
}

@Composable
private fun DownloadedSongsCard(
    songCount: Int,
    totalDuration: Long,
    onClick: () -> Unit
) {
    val durationText = if (totalDuration > 0) {
        val minutes = (totalDuration / 1000 / 60).toInt()
        val seconds = (totalDuration / 1000 % 60).toInt()
        if (minutes >= 60) {
            val hours = minutes / 60
            val remainingMinutes = minutes % 60
            "$hours hr $remainingMinutes min"
        } else {
            "$minutes min $seconds sec"
        }
    } else {
        ""
    }
    
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkmark icon in gradient box
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF7B68EE),
                                Color(0xFF9370DB)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Downloaded songs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = buildString {
                            append("Auto playlist")
                            append(" • ")
                            append("$songCount song${if (songCount != 1) "s" else ""}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (durationText.isNotEmpty()) {
                    Text(
                        text = durationText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            
            IconButton(onClick = { }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LikedTab(
    songs: List<Song>,
    onSongClick: (List<Song>, Int) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            bottom = 140.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (songs.isEmpty()) {
            item {
                Text(
                    text = "No liked songs yet.\nLog in to YouTube Music to see your likes.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 32.dp)
                )
            }
        }
        
        itemsIndexed(songs) { index, song ->
            MusicCard(
                song = song,
                onClick = { onSongClick(songs, index) }
            )
        }
    }
}