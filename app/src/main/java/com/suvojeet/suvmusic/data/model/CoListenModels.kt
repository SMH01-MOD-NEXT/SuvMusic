package com.suvojeet.suvmusic.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Session(
    val code: String = "",
    val hostId: String = "",
    val currentSong: SessionSong? = null,
    val isPlaying: Boolean = false,
    val position: Long = 0L,
    val timestamp: Long = 0L,
    val lastActivity: Long = 0L, // For auto-cleanup after inactivity
    val users: Map<String, SessionUser> = emptyMap(),
    val isSyncing: Boolean = false // True when song change is syncing across devices
)

@Serializable
data class SessionSong(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val thumbnailUrl: String = "",
    val duration: Long = 0L,
    val source: String = ""
)

@Serializable
data class SessionUser(
    val id: String = "",
    val name: String = "",
    val avatarUrl: String = "",
    val isActive: Boolean = true,
    val isBuffering: Boolean = false // True when device is loading/buffering current song
)

@Serializable
sealed class SyncEvent {
    @Serializable
    data class Toast(val message: String) : SyncEvent()
}
