package com.suvojeet.suvmusic.model

/**
 * Represents an app update available from GitHub Releases.
 */
data class AppUpdate(
    val versionName: String,
    val versionCode: Int,
    val releaseNotes: String,
    val downloadUrl: String,
    val publishedAt: String,
    val isPreRelease: Boolean = false
)

/**
 * Sealed class representing update check states.
 */
sealed class UpdateState {
    data object Idle : UpdateState()
    data object Checking : UpdateState()
    data class UpdateAvailable(val update: AppUpdate) : UpdateState()
    data object NoUpdate : UpdateState()
    data class Downloading(val progress: Int) : UpdateState()
    data object Downloaded : UpdateState()
    data class Error(val message: String) : UpdateState()
}
