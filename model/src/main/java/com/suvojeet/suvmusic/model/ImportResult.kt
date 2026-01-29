package com.suvojeet.suvmusic.model

data class ImportResult(
    val originalTitle: String,
    val originalArtist: String,
    val matchedSong: Song?
)
