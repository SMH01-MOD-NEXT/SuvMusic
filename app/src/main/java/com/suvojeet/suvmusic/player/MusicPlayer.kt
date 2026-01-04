package com.suvojeet.suvmusic.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.suvojeet.suvmusic.data.model.PlayerState
import com.suvojeet.suvmusic.data.model.RepeatMode
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.data.model.SongSource
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrapper around ExoPlayer with queue management and state flow.
 */
@Singleton
@OptIn(UnstableApi::class)
class MusicPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val youTubeRepository: YouTubeRepository
) {
    
    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()
    
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    
    private val exoPlayer: ExoPlayer by lazy {
        ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true // Handle audio focus
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                addListener(playerListener)
            }
    }
    
    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playerState.update { it.copy(isPlaying = isPlaying) }
        }
        
        override fun onPlaybackStateChanged(playbackState: Int) {
            _playerState.update { 
                it.copy(
                    isLoading = playbackState == Player.STATE_BUFFERING,
                    error = null
                )
            }
            
            if (playbackState == Player.STATE_READY) {
                startPositionUpdates()
            }
        }
        
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.let { item ->
                val index = exoPlayer.currentMediaItemIndex
                val song = _playerState.value.queue.getOrNull(index)
                _playerState.update { 
                    it.copy(
                        currentSong = song,
                        currentIndex = index,
                        currentPosition = 0L,
                        duration = exoPlayer.duration.coerceAtLeast(0L)
                    )
                }
            }
        }
        
        override fun onPlayerError(error: PlaybackException) {
            _playerState.update { 
                it.copy(
                    error = error.message ?: "Playback error",
                    isLoading = false
                )
            }
        }
    }
    
    private var positionUpdateJob: Job? = null
    
    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = scope.launch {
            while (true) {
                _playerState.update { 
                    it.copy(
                        currentPosition = exoPlayer.currentPosition.coerceAtLeast(0L),
                        duration = exoPlayer.duration.coerceAtLeast(0L),
                        bufferedPercentage = exoPlayer.bufferedPercentage
                    )
                }
                delay(500)
            }
        }
    }
    
    /**
     * Play a single song.
     */
    fun playSong(song: Song, queue: List<Song> = listOf(song), startIndex: Int = 0) {
        scope.launch {
            _playerState.update { 
                it.copy(
                    queue = queue,
                    currentIndex = startIndex,
                    currentSong = song,
                    isLoading = true
                )
            }
            
            try {
                val mediaItems = queue.mapIndexed { index, s -> createMediaItem(s, index == startIndex) }
                exoPlayer.setMediaItems(mediaItems, startIndex, 0L)
                exoPlayer.prepare()
                exoPlayer.play()
            } catch (e: Exception) {
                _playerState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
    
    private suspend fun createMediaItem(song: Song, resolveStream: Boolean = true): MediaItem {
        val uri = when (song.source) {
            SongSource.LOCAL -> song.localUri.toString()
            else -> {
                if (resolveStream) {
                    youTubeRepository.getStreamUrl(song.id) ?: "https://youtube.com/watch?v=${song.id}"
                } else {
                    "https://youtube.com/watch?v=${song.id}"
                }
            }
        }
        
        return MediaItem.Builder()
            .setUri(uri)
            .setMediaId(song.id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .setArtworkUri(song.thumbnailUrl?.let { android.net.Uri.parse(it) })
                    .build()
            )
            .build()
    }
    
    fun play() {
        exoPlayer.play()
    }
    
    fun pause() {
        exoPlayer.pause()
    }
    
    fun togglePlayPause() {
        if (exoPlayer.isPlaying) pause() else play()
    }
    
    fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
    }
    
    fun seekToNext() {
        if (exoPlayer.hasNextMediaItem()) {
            exoPlayer.seekToNextMediaItem()
        }
    }
    
    fun seekToPrevious() {
        if (exoPlayer.currentPosition > 3000) {
            exoPlayer.seekTo(0)
        } else if (exoPlayer.hasPreviousMediaItem()) {
            exoPlayer.seekToPreviousMediaItem()
        }
    }
    
    fun setRepeatMode(mode: RepeatMode) {
        exoPlayer.repeatMode = when (mode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
        _playerState.update { it.copy(repeatMode = mode) }
    }
    
    fun toggleShuffle() {
        val newShuffleState = !exoPlayer.shuffleModeEnabled
        exoPlayer.shuffleModeEnabled = newShuffleState
        _playerState.update { it.copy(shuffleEnabled = newShuffleState) }
    }
    
    fun getPlayer(): ExoPlayer = exoPlayer
    
    fun release() {
        positionUpdateJob?.cancel()
        exoPlayer.release()
    }
}
