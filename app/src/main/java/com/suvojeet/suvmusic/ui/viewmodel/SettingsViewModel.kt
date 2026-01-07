package com.suvojeet.suvmusic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.data.model.AudioQuality
import com.suvojeet.suvmusic.data.model.DownloadQuality
import com.suvojeet.suvmusic.data.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isLoggedIn: Boolean = false,
    val userAvatarUrl: String? = null,
    val audioQuality: AudioQuality = AudioQuality.HIGH,
    val downloadQuality: DownloadQuality = DownloadQuality.HIGH,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColorEnabled: Boolean = true,
    val gaplessPlaybackEnabled: Boolean = true,
    val automixEnabled: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
        
        viewModelScope.launch {
            sessionManager.audioQualityFlow.collect { quality ->
                _uiState.update { it.copy(audioQuality = quality) }
            }
        }
    }
    
    private fun loadSettings() {
        _uiState.update { 
            it.copy(
                isLoggedIn = sessionManager.isLoggedIn(),
                userAvatarUrl = sessionManager.getUserAvatar(),
                audioQuality = sessionManager.getAudioQuality(),
                downloadQuality = sessionManager.getDownloadQuality(),
                themeMode = sessionManager.getThemeMode(),
                dynamicColorEnabled = sessionManager.isDynamicColorEnabled(),
                gaplessPlaybackEnabled = sessionManager.isGaplessPlaybackEnabled(),
                automixEnabled = sessionManager.isAutomixEnabled()
            )
        }
    }
    
    fun setAudioQuality(quality: AudioQuality) {
        viewModelScope.launch {
            sessionManager.setAudioQuality(quality)
            _uiState.update { it.copy(audioQuality = quality) }
        }
    }
    
    fun setDownloadQuality(quality: DownloadQuality) {
        viewModelScope.launch {
            sessionManager.setDownloadQuality(quality)
            _uiState.update { it.copy(downloadQuality = quality) }
        }
    }
    
    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.setDynamicColor(enabled)
            _uiState.update { it.copy(dynamicColorEnabled = enabled) }
        }
    }
    
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            sessionManager.setThemeMode(mode)
            _uiState.update { it.copy(themeMode = mode) }
        }
    }
    
    fun setGaplessPlayback(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.setGaplessPlayback(enabled)
            _uiState.update { it.copy(gaplessPlaybackEnabled = enabled) }
        }
    }
    
    fun setAutomix(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.setAutomix(enabled)
            _uiState.update { it.copy(automixEnabled = enabled) }
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            sessionManager.clearCookies()
            _uiState.update { 
                it.copy(
                    isLoggedIn = false,
                    userAvatarUrl = null
                )
            }
        }
    }
}

