package com.suvojeet.suvmusic.data

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.suvojeet.suvmusic.data.model.Session
import com.suvojeet.suvmusic.data.model.SessionSong
import com.suvojeet.suvmusic.data.model.SessionUser
import com.suvojeet.suvmusic.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class CoListenManager @Inject constructor(
    private val sessionManager: SessionManager
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var database: FirebaseDatabase? = null
    private var currentSessionRef: DatabaseReference? = null
    private var sessionListener: ValueEventListener? = null
    private var activityUpdateJob: Job? = null
    private var cleanupCheckJob: Job? = null

    private val _sessionState = MutableStateFlow<Session?>(null)
    val sessionState: StateFlow<Session?> = _sessionState.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // Callback for when session ends (host left or deleted)
    private val _sessionEndedEvent = MutableStateFlow<String?>(null)
    val sessionEndedEvent: StateFlow<String?> = _sessionEndedEvent.asStateFlow()

    // Syncing state - true when waiting for all devices to buffer
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // Event when a new user joins the session
    private val _userJoinedEvent = MutableStateFlow<SessionUser?>(null)
    val userJoinedEvent: StateFlow<SessionUser?> = _userJoinedEvent.asStateFlow()

    // Show syncing dialog state - controlled by ViewModel
    private val _showSyncingDialog = MutableStateFlow(false)
    val showSyncingDialog: StateFlow<Boolean> = _showSyncingDialog.asStateFlow()

    // Track previous user count for join detection
    private var previousUserCount = 0

    private var currentUserId: String = ""
    private var currentUserName: String = ""
    private var currentUserAvatar: String = ""
    private var isHost: Boolean = false

    companion object {
        private const val INACTIVITY_TIMEOUT_MS = 10 * 60 * 1000L // 10 minutes
        private const val ACTIVITY_UPDATE_INTERVAL_MS = 60 * 1000L // 1 minute
    }

    init {
        scope.launch {
            // Initialize user info
            currentUserId = sessionManager.getCookies()?.hashCode()?.toString() ?: "guest_${Random.nextInt(1000, 9999)}"
            currentUserName = sessionManager.getStoredAccounts().firstOrNull()?.name ?: "Guest"
            currentUserAvatar = sessionManager.getStoredAccounts().firstOrNull()?.avatarUrl ?: ""
        }
    }
    
    // Lazy init firebase to avoid startup issues if not configured
    private fun getDb(): FirebaseDatabase {
        if (database == null) {
            database = FirebaseDatabase.getInstance()
        }
        return database!!
    }

    fun createSession(
        currentSong: Song?,
        isPlaying: Boolean,
        position: Long,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            try {
                _connectionState.value = ConnectionState.Connecting
                val code = generateSessionCode()
                val sessionRef = getDb().getReference("sessions").child(code)
                
                val currentTime = System.currentTimeMillis()
                val initialSession = Session(
                    code = code,
                    hostId = currentUserId,
                    users = mapOf(currentUserId to createSessionUser()),
                    currentSong = currentSong?.toSessionSong(),
                    isPlaying = isPlaying,
                    position = position,
                    timestamp = currentTime,
                    lastActivity = currentTime
                )
                
                sessionRef.setValue(initialSession).await()
                
                isHost = true
                subscribeToSession(code)
                startActivityUpdater()
                onSuccess(code)
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.Disconnected
                onError(e.message ?: "Failed to create session")
            }
        }
    }

    fun joinSession(code: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        scope.launch {
            try {
                _connectionState.value = ConnectionState.Connecting
                val sessionRef = getDb().getReference("sessions").child(code)
                
                val snapshot = sessionRef.get().await()
                if (!snapshot.exists()) {
                    _connectionState.value = ConnectionState.Disconnected
                    onError("Session not found")
                    return@launch
                }
                
                // Add user to session and update activity - trigger sync
                val currentTime = System.currentTimeMillis()
                val updates = hashMapOf<String, Any>(
                    "users/${currentUserId}" to createSessionUser(),
                    "lastActivity" to currentTime,
                    "isSyncing" to true // Trigger sync when new user joins
                )
                sessionRef.updateChildren(updates).await()
                
                isHost = false
                subscribeToSession(code)
                startActivityUpdater()
                onSuccess()
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.Disconnected
                onError(e.message ?: "Failed to join session")
            }
        }
    }

    fun leaveSession() {
        scope.launch {
            val wasHost = isHost
            val sessionCode = (_connectionState.value as? ConnectionState.Connected)?.code
            
            currentSessionRef?.let { ref ->
                // Remove listener
                sessionListener?.let { ref.removeEventListener(it) }
                
                if (wasHost && sessionCode != null) {
                    // Host leaves -> Delete entire session
                    ref.removeValue().await()
                } else {
                    // Guest leaves -> Just remove user from session
                    ref.child("users").child(currentUserId).removeValue()
                }
            }
            
            stopActivityUpdater()
            currentSessionRef = null
            sessionListener = null
            _sessionState.value = null
            _connectionState.value = ConnectionState.Disconnected
            isHost = false
        }
    }

    fun updatePlayerState(song: Song?, isPlaying: Boolean, position: Long) {
        val currentSession = _sessionState.value ?: return
        // Only update if we are connected
        if (_connectionState.value !is ConnectionState.Connected) return

        val sessionSong = song?.toSessionSong()
        val currentTime = System.currentTimeMillis()
        
        // Check if song is changing - if so, set syncing state
        val songChanged = sessionSong != null && currentSession.currentSong?.id != sessionSong.id
        
        val updates = hashMapOf<String, Any>(
            "isPlaying" to isPlaying,
            "position" to position,
            "timestamp" to currentTime,
            "lastActivity" to currentTime
        )
        if (sessionSong != null) {
            updates["currentSong"] = sessionSong
            if (songChanged) {
                // Set syncing flag when song changes
                updates["isSyncing"] = true
                _isSyncing.value = true
            }
        }
        
        currentSessionRef?.updateChildren(updates)
    }

    fun clearSessionEndedEvent() {
        _sessionEndedEvent.value = null
    }

    private fun subscribeToSession(code: String) {
        val ref = getDb().getReference("sessions").child(code)
        
        sessionListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    if (!snapshot.exists()) {
                        // Session was deleted (host left or auto-cleanup)
                        handleSessionEnded("Session has ended")
                        return
                    }
                    
                    val session = snapshot.getValue(Session::class.java)
                    _sessionState.value = session
                    
                    if (session != null) {
                        _connectionState.value = ConnectionState.Connected(code)
                        currentSessionRef = ref
                        
                        // Detect new user joins
                        val currentUserCount = session.users.size
                        if (currentUserCount > previousUserCount && previousUserCount > 0) {
                            // Find the new user (not us)
                            val newUser = session.users.values.find { user ->
                                user.id != currentUserId && user.joinedAt > System.currentTimeMillis() - 5000
                            }
                            if (newUser != null) {
                                _userJoinedEvent.value = newUser
                            }
                        }
                        previousUserCount = currentUserCount
                        
                        // Update local syncing state from Firebase
                        _isSyncing.value = session.isSyncing
                        _showSyncingDialog.value = session.isSyncing
                        
                        // Check if all users are ready (only relevant when syncing)
                        if (session.isSyncing) {
                            val allReady = session.users.values.none { it.isBuffering }
                            if (allReady) {
                                // All devices ready - clear syncing flag
                                ref.child("isSyncing").setValue(false)
                                _isSyncing.value = false
                                _showSyncingDialog.value = false
                            }
                        }
                        
                        // Check if session is inactive and we should clean it up
                        if (isHost) {
                            checkAndCleanupInactiveSession(session)
                        }
                    } else {
                        handleSessionEnded("Session was deleted")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                _connectionState.value = ConnectionState.Error(error.message)
            }
        }
        
        ref.addValueEventListener(sessionListener!!)
    }

    private fun handleSessionEnded(reason: String) {
        stopActivityUpdater()
        currentSessionRef?.removeEventListener(sessionListener!!)
        currentSessionRef = null
        sessionListener = null
        _sessionState.value = null
        _connectionState.value = ConnectionState.Disconnected
        _sessionEndedEvent.value = reason
        _showSyncingDialog.value = false
        previousUserCount = 0
        isHost = false
    }

    private fun startActivityUpdater() {
        activityUpdateJob?.cancel()
        activityUpdateJob = scope.launch {
            while (isActive) {
                delay(ACTIVITY_UPDATE_INTERVAL_MS)
                // Update lastActivity periodically if we're still connected
                if (_connectionState.value is ConnectionState.Connected) {
                    currentSessionRef?.child("lastActivity")?.setValue(System.currentTimeMillis())
                }
            }
        }
    }

    private fun stopActivityUpdater() {
        activityUpdateJob?.cancel()
        activityUpdateJob = null
        cleanupCheckJob?.cancel()
        cleanupCheckJob = null
    }

    private fun checkAndCleanupInactiveSession(session: Session) {
        // Only the host can initiate cleanup
        if (!isHost) return
        
        val now = System.currentTimeMillis()
        val timeSinceLastActivity = now - session.lastActivity
        
        // If only host remains and no activity for 10 minutes, delete session
        if (session.users.size <= 1 && timeSinceLastActivity > INACTIVITY_TIMEOUT_MS) {
            scope.launch {
                currentSessionRef?.removeValue()
            }
        }
    }

    private fun generateSessionCode(): String {
        return (100000..999999).random().toString()
    }
    
    private fun createSessionUser(isBuffering: Boolean = false): SessionUser {
        return SessionUser(
            id = currentUserId,
            name = currentUserName,
            avatarUrl = currentUserAvatar,
            isActive = true,
            isBuffering = isBuffering,
            joinedAt = System.currentTimeMillis()
        )
    }

    /**
     * Update this device's buffering state in the session.
     * Call this when player starts/finishes loading a song.
     */
    fun updateBufferingState(isBuffering: Boolean) {
        if (_connectionState.value !is ConnectionState.Connected) return
        
        currentSessionRef?.child("users")?.child(currentUserId)?.child("isBuffering")?.setValue(isBuffering)
        
        // If we just finished buffering, check if all users are ready
        if (!isBuffering) {
            checkAllUsersReady()
        }
    }

    /**
     * Check if all users in the session have finished buffering.
     * If so, clear the syncing state to allow playback.
     */
    private fun checkAllUsersReady() {
        val session = _sessionState.value ?: return
        val allReady = session.users.values.none { it.isBuffering }
        
        if (allReady && session.isSyncing) {
            // All devices ready - clear syncing flag
            currentSessionRef?.child("isSyncing")?.setValue(false)
            _isSyncing.value = false
        }
    }
    
    private fun Song.toSessionSong(): SessionSong {
        return SessionSong(
            id = this.id,
            title = this.title,
            artist = this.artist,
            album = this.album ?: "",
            thumbnailUrl = this.thumbnailUrl ?: "",
            duration = this.duration,
            source = this.source.name
        )
    }

    fun isCurrentUserHost(): Boolean = isHost

    /**
     * Pause playback for sync when song changes or user joins.
     * Sets isSyncing flag on Firebase to pause all devices.
     */
    fun pauseForSync() {
        if (_connectionState.value !is ConnectionState.Connected) return
        _isSyncing.value = true
        _showSyncingDialog.value = true
        
        val updates = hashMapOf<String, Any>(
            "isSyncing" to true,
            "isPlaying" to false
        )
        currentSessionRef?.updateChildren(updates)
    }

    /**
     * Resume playback after all devices are synced.
     */
    fun resumeAfterSync() {
        if (_connectionState.value !is ConnectionState.Connected) return
        
        val updates = hashMapOf<String, Any>(
            "isSyncing" to false,
            "isPlaying" to true
        )
        currentSessionRef?.updateChildren(updates)
        _isSyncing.value = false
        _showSyncingDialog.value = false
    }

    /**
     * Manually control syncing dialog visibility.
     */
    fun setShowSyncingDialog(show: Boolean) {
        _showSyncingDialog.value = show
    }

    /**
     * Clear the user joined event after processing.
     */
    fun clearUserJoinedEvent() {
        _userJoinedEvent.value = null
    }

    /**
     * Get the current user ID for this session.
     */
    fun getCurrentUserId(): String = currentUserId
}

sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data object Connecting : ConnectionState()
    data class Connected(val code: String) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}
