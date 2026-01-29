package com.suvojeet.suvmusic.model

/**
 * Represents a recently played song with timestamp.
 */
data class RecentlyPlayed(
    val song: Song,
    val playedAt: Long = System.currentTimeMillis()
)
