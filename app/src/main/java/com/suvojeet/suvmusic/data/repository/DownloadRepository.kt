package com.suvojeet.suvmusic.data.repository

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.data.model.SongSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val youTubeRepository: YouTubeRepository
) {
    companion object {
        private const val TAG = "DownloadRepository"
    }
    
    private val gson = Gson()
    private val downloadsFile = File(context.filesDir, "downloads_meta.json")
    private val downloadsDir = File(context.filesDir, "downloads")
    
    private val _downloadedSongs = MutableStateFlow<List<Song>>(emptyList())
    val downloadedSongs: StateFlow<List<Song>> = _downloadedSongs.asStateFlow()

    private val _downloadingIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadingIds: StateFlow<Set<String>> = _downloadingIds.asStateFlow()
    
    // Dedicated HTTP client for downloads with longer timeouts
    private val downloadClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.MINUTES) // 5 minutes for large files
        .writeTimeout(5, TimeUnit.MINUTES)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .build()

    init {
        if (!downloadsDir.exists()) downloadsDir.mkdirs()
        loadDownloads()
    }

    private fun loadDownloads() {
        if (!downloadsFile.exists()) {
            _downloadedSongs.value = emptyList()
            return
        }
        try {
            val json = downloadsFile.readText()
            val type = object : TypeToken<List<Song>>() {}.type
            _downloadedSongs.value = gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading downloads", e)
            _downloadedSongs.value = emptyList()
        }
    }

    private fun saveDownloads() {
        try {
            val json = gson.toJson(_downloadedSongs.value)
            downloadsFile.writeText(json)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving downloads", e)
        }
    }

    suspend fun downloadSong(song: Song): Boolean = withContext(Dispatchers.IO) {
        if (_downloadedSongs.value.any { it.id == song.id }) {
            Log.d(TAG, "Song ${song.id} already downloaded")
            return@withContext true
        }
        
        // Mark as downloading
        _downloadingIds.value = _downloadingIds.value + song.id
        Log.d(TAG, "Starting download for: ${song.title} (${song.id})")
        
        try {
            // Get stream URL
            val streamUrl = youTubeRepository.getStreamUrl(song.id)
            if (streamUrl == null) {
                Log.e(TAG, "Failed to get stream URL for ${song.id}")
                _downloadingIds.value = _downloadingIds.value - song.id
                return@withContext false
            }
            
            Log.d(TAG, "Got stream URL, starting download...")
            
            val request = Request.Builder()
                .url(streamUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "*/*")
                .header("Accept-Encoding", "identity") // Disable compression for streaming
                .header("Connection", "keep-alive")
                .build()
            
            val response = downloadClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.e(TAG, "Download request failed: ${response.code} - ${response.message}")
                response.close()
                _downloadingIds.value = _downloadingIds.value - song.id
                return@withContext false
            }
            
            val contentLength = response.body?.contentLength() ?: -1L
            Log.d(TAG, "Content length: $contentLength bytes")

            val file = File(downloadsDir, "${song.id}.m4a")
            var totalBytesRead = 0L
            
            FileOutputStream(file).use { fos ->
                response.body?.byteStream()?.use { input ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        fos.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                    }
                }
            }
            
            response.close()
            
            Log.d(TAG, "Download complete: $totalBytesRead bytes written to ${file.absolutePath}")
            
            // Verify file was actually created and has content
            if (!file.exists() || file.length() == 0L) {
                Log.e(TAG, "File not created or empty")
                _downloadingIds.value = _downloadingIds.value - song.id
                return@withContext false
            }

            // Create downloaded song entry
            val downloadedSong = song.copy(
                source = SongSource.DOWNLOADED,
                localUri = file.toUri(),
                streamUrl = null
            )

            _downloadedSongs.value = _downloadedSongs.value + downloadedSong
            saveDownloads()
            
            _downloadingIds.value = _downloadingIds.value - song.id
            Log.d(TAG, "Song ${song.title} download successful!")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Download error for ${song.id}", e)
            _downloadingIds.value = _downloadingIds.value - song.id
            false
        }
    }

    suspend fun deleteDownload(songId: String) = withContext(Dispatchers.IO) {
        val song = _downloadedSongs.value.find { it.id == songId } ?: return@withContext
        
        try {
            val file = File(downloadsDir, "${songId}.m4a")
            if (file.exists()) file.delete()
            
            _downloadedSongs.value = _downloadedSongs.value.filter { it.id != songId }
            saveDownloads()
            Log.d(TAG, "Deleted download: $songId")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting download", e)
        }
    }
    
    fun isDownloaded(songId: String): Boolean {
        return _downloadedSongs.value.any { it.id == songId }
    }
    
    fun isDownloading(songId: String): Boolean {
        return _downloadingIds.value.contains(songId)
    }
}
