package com.suvojeet.suvmusic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.data.repository.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository
) : ViewModel() {
    
    val downloadedSongs: StateFlow<List<Song>> = downloadRepository.downloadedSongs
    
    fun deleteDownload(songId: String) {
        viewModelScope.launch {
            downloadRepository.deleteDownload(songId)
        }
    }
    
    fun refreshDownloads() {
        downloadRepository.refreshDownloads()
    }
}
