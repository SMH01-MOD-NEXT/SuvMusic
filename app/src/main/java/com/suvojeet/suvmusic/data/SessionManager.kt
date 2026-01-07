package com.suvojeet.suvmusic.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.suvojeet.suvmusic.data.model.AudioQuality
import com.suvojeet.suvmusic.data.model.DownloadQuality
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.data.model.SongSource
import com.suvojeet.suvmusic.data.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "suvmusic_session")

/**
 * Manages session data for YouTube Music authentication.
 */
@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val COOKIES_KEY = stringPreferencesKey("cookies")
        private val USER_AVATAR_KEY = stringPreferencesKey("user_avatar")
        private val AUDIO_QUALITY_KEY = stringPreferencesKey("audio_quality")
        private val GAPLESS_PLAYBACK_KEY = booleanPreferencesKey("gapless_playback")
        private val AUTOMIX_KEY = booleanPreferencesKey("automix")
        private val DOWNLOAD_QUALITY_KEY = stringPreferencesKey("download_quality")
        private val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        private val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")
        // Resume playback
        private val LAST_SONG_ID_KEY = stringPreferencesKey("last_song_id")
        private val LAST_POSITION_KEY = androidx.datastore.preferences.core.longPreferencesKey("last_position")
        private val LAST_QUEUE_KEY = stringPreferencesKey("last_queue")
        private val LAST_INDEX_KEY = intPreferencesKey("last_index")
        // Recent searches
        private val RECENT_SEARCHES_KEY = stringPreferencesKey("recent_searches")
        private const val MAX_RECENT_SEARCHES = 20
    }
    
    // --- Cookies ---
    
    fun getCookies(): String? = runBlocking {
        context.dataStore.data.first()[COOKIES_KEY]
    }
    
    suspend fun saveCookies(cookies: String) {
        context.dataStore.edit { preferences ->
            preferences[COOKIES_KEY] = cookies
        }
    }
    
    suspend fun clearCookies() {
        context.dataStore.edit { preferences ->
            preferences.remove(COOKIES_KEY)
        }
    }
    
    fun isLoggedIn(): Boolean = !getCookies().isNullOrBlank()
    
    // --- User Avatar ---
    
    fun getUserAvatar(): String? = runBlocking {
        context.dataStore.data.first()[USER_AVATAR_KEY]
    }
    
    suspend fun saveUserAvatar(url: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_AVATAR_KEY] = url
        }
    }

    val userAvatarFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_AVATAR_KEY]
    }
    
    // --- Audio Quality ---
    
    fun getAudioQuality(): AudioQuality = runBlocking {
        val qualityName = context.dataStore.data.first()[AUDIO_QUALITY_KEY]
        qualityName?.let { 
            try { AudioQuality.valueOf(it) } catch (e: Exception) { AudioQuality.HIGH }
        } ?: AudioQuality.HIGH
    }
    
    val audioQualityFlow: Flow<AudioQuality> = context.dataStore.data.map { preferences ->
        preferences[AUDIO_QUALITY_KEY]?.let {
            try { AudioQuality.valueOf(it) } catch (e: Exception) { AudioQuality.HIGH }
        } ?: AudioQuality.HIGH
    }
    
    suspend fun setAudioQuality(quality: AudioQuality) {
        context.dataStore.edit { preferences ->
            preferences[AUDIO_QUALITY_KEY] = quality.name
        }
    }
    
    // --- Playback Settings ---
    
    fun isGaplessPlaybackEnabled(): Boolean = runBlocking {
        context.dataStore.data.first()[GAPLESS_PLAYBACK_KEY] ?: true
    }
    
    suspend fun setGaplessPlayback(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[GAPLESS_PLAYBACK_KEY] = enabled
        }
    }
    
    fun isAutomixEnabled(): Boolean = runBlocking {
        context.dataStore.data.first()[AUTOMIX_KEY] ?: true
    }
    
    suspend fun setAutomix(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTOMIX_KEY] = enabled
        }
    }
    
    // --- Download Quality ---
    
    fun getDownloadQuality(): DownloadQuality = runBlocking {
        val qualityName = context.dataStore.data.first()[DOWNLOAD_QUALITY_KEY]
        qualityName?.let { 
            try { DownloadQuality.valueOf(it) } catch (e: Exception) { DownloadQuality.HIGH }
        } ?: DownloadQuality.HIGH
    }
    
    val downloadQualityFlow: Flow<DownloadQuality> = context.dataStore.data.map { preferences ->
        preferences[DOWNLOAD_QUALITY_KEY]?.let {
            try { DownloadQuality.valueOf(it) } catch (e: Exception) { DownloadQuality.HIGH }
        } ?: DownloadQuality.HIGH
    }
    
    suspend fun setDownloadQuality(quality: DownloadQuality) {
        context.dataStore.edit { preferences ->
            preferences[DOWNLOAD_QUALITY_KEY] = quality.name
        }
    }

    // --- Onboarding ---

    fun isOnboardingCompleted(): Boolean = runBlocking {
        context.dataStore.data.first()[ONBOARDING_COMPLETED_KEY] ?: false
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED_KEY] = completed
        }
    }
    
    // --- Theme Mode ---
    
    fun getThemeMode(): ThemeMode = runBlocking {
        val modeName = context.dataStore.data.first()[THEME_MODE_KEY]
        modeName?.let { 
            try { ThemeMode.valueOf(it) } catch (e: Exception) { ThemeMode.SYSTEM }
        } ?: ThemeMode.SYSTEM
    }
    
    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        preferences[THEME_MODE_KEY]?.let {
            try { ThemeMode.valueOf(it) } catch (e: Exception) { ThemeMode.SYSTEM }
        } ?: ThemeMode.SYSTEM
    }
    
    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode.name
        }
    }
    
    // --- Dynamic Color ---
    
    fun isDynamicColorEnabled(): Boolean = runBlocking {
        context.dataStore.data.first()[DYNAMIC_COLOR_KEY] ?: true
    }
    
    val dynamicColorFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DYNAMIC_COLOR_KEY] ?: true
    }
    
    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DYNAMIC_COLOR_KEY] = enabled
        }
    }
    
    // --- Resume Playback ---
    
    /**
     * Save last playback state for resume functionality.
     */
    suspend fun savePlaybackState(songId: String, position: Long, queueJson: String, index: Int) {
        context.dataStore.edit { preferences ->
            preferences[LAST_SONG_ID_KEY] = songId
            preferences[LAST_POSITION_KEY] = position
            preferences[LAST_QUEUE_KEY] = queueJson
            preferences[LAST_INDEX_KEY] = index
        }
    }
    
    /**
     * Get last saved playback state.
     * @return Quadruple of (songId, position, queueJson, index) or null if not saved.
     */
    fun getLastPlaybackState(): LastPlaybackState? = runBlocking {
        val prefs = context.dataStore.data.first()
        val songId = prefs[LAST_SONG_ID_KEY]
        val position = prefs[LAST_POSITION_KEY]
        val queueJson = prefs[LAST_QUEUE_KEY]
        val index = prefs[LAST_INDEX_KEY]
        
        if (songId != null && position != null && queueJson != null && index != null) {
            LastPlaybackState(songId, position, queueJson, index)
        } else null
    }
    
    /**
     * Clear saved playback state.
     */
    suspend fun clearPlaybackState() {
        context.dataStore.edit { preferences ->
            preferences.remove(LAST_SONG_ID_KEY)
            preferences.remove(LAST_POSITION_KEY)
            preferences.remove(LAST_QUEUE_KEY)
            preferences.remove(LAST_INDEX_KEY)
        }
    }
    
    // --- Recent Searches ---
    
    /**
     * Get recent searches list.
     */
    fun getRecentSearches(): List<Song> = runBlocking {
        val json = context.dataStore.data.first()[RECENT_SEARCHES_KEY] ?: return@runBlocking emptyList()
        parseRecentSearchesJson(json)
    }
    
    val recentSearchesFlow: Flow<List<Song>> = context.dataStore.data.map { preferences ->
        val json = preferences[RECENT_SEARCHES_KEY] ?: return@map emptyList()
        parseRecentSearchesJson(json)
    }
    
    private fun parseRecentSearchesJson(json: String): List<Song> {
        return try {
            val jsonArray = JSONArray(json)
            val songs = mutableListOf<Song>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                songs.add(
                    Song(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        artist = obj.getString("artist"),
                        album = obj.optString("album", ""),
                        thumbnailUrl = obj.optString("thumbnailUrl", null),
                        duration = obj.optLong("duration", 0L),
                        source = try {
                            SongSource.valueOf(obj.optString("source", "YOUTUBE"))
                        } catch (e: Exception) {
                            SongSource.YOUTUBE
                        }
                    )
                )
            }
            songs
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Add a song to recent searches.
     */
    suspend fun addRecentSearch(song: Song) {
        val currentSearches = getRecentSearches().toMutableList()
        
        // Remove if already exists (to move to top)
        currentSearches.removeAll { it.id == song.id }
        
        // Add to beginning
        currentSearches.add(0, song)
        
        // Keep only max items
        val trimmed = currentSearches.take(MAX_RECENT_SEARCHES)
        
        // Save
        val jsonArray = JSONArray()
        trimmed.forEach { s ->
            jsonArray.put(JSONObject().apply {
                put("id", s.id)
                put("title", s.title)
                put("artist", s.artist)
                put("album", s.album ?: "")
                put("thumbnailUrl", s.thumbnailUrl ?: "")
                put("duration", s.duration)
                put("source", s.source.name)
            })
        }
        
        context.dataStore.edit { preferences ->
            preferences[RECENT_SEARCHES_KEY] = jsonArray.toString()
        }
    }
    
    /**
     * Clear all recent searches.
     */
    suspend fun clearRecentSearches() {
        context.dataStore.edit { preferences ->
            preferences.remove(RECENT_SEARCHES_KEY)
        }
    }
}

/**
 * Data class for last playback state.
 */
data class LastPlaybackState(
    val songId: String,
    val position: Long,
    val queueJson: String,
    val index: Int
)


