package com.suvojeet.suvmusic.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.suvojeet.suvmusic.data.model.AudioQuality
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
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
}
