package com.suvojeet.suvmusic.data.model

/**
 * Represents a recently played song with timestamp.
 */
data class RecentlyPlayed(
    val song: Song,
    val playedAt: Long = System.currentTimeMillis()
)
