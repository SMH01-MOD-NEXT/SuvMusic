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
import com.suvojeet.suvmusic.data.model.SongSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _sessionState = MutableStateFlow<Session?>(null)
    val sessionState: StateFlow<Session?> = _sessionState.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var currentUserId: String = ""
    private var currentUserName: String = ""
    private var currentUserAvatar: String = ""

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

    fun createSession(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        scope.launch {
            try {
                _connectionState.value = ConnectionState.Connecting
                val code = generateSessionCode()
                val sessionRef = getDb().getReference("sessions").child(code)
                
                val initialSession = Session(
                    code = code,
                    hostId = currentUserId,
                    users = mapOf(currentUserId to createSessionUser())
                )
                
                sessionRef.setValue(initialSession).await()
                
                subscribeToSession(code)
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
                
                // Add user to session
                sessionRef.child("users").child(currentUserId).setValue(createSessionUser()).await()
                
                subscribeToSession(code)
                onSuccess()
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.Disconnected
                onError(e.message ?: "Failed to join session")
            }
        }
    }

    fun leaveSession() {
        scope.launch {
            currentSessionRef?.let { ref ->
                // Remove listener
                sessionListener?.let { ref.removeEventListener(it) }
                
                // Remove user from session
                ref.child("users").child(currentUserId).removeValue()
                
                // Check if we were the last user or host, maybe cleanup? 
                // For now just leave. Firebase rules can handle cleanup or TTL.
            }
            
            currentSessionRef = null
            sessionListener = null
            _sessionState.value = null
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    fun updatePlayerState(song: Song?, isPlaying: Boolean, position: Long) {
        val currentSession = _sessionState.value ?: return
        // Only update if we are connected
        if (_connectionState.value !is ConnectionState.Connected) return

        val sessionSong = song?.toSessionSong()
        
        val updates = hashMapOf<String, Any>(
            "isPlaying" to isPlaying,
            "position" to position,
            "timestamp" to System.currentTimeMillis()
        )
        if (sessionSong != null) {
            updates["currentSong"] = sessionSong
        }
        
        currentSessionRef?.updateChildren(updates)
    }

    private fun subscribeToSession(code: String) {
        val ref = getDb().getReference("sessions").child(code)
        
        sessionListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val session = snapshot.getValue(Session::class.java)
                    _sessionState.value = session
                    
                    if (session != null) {
                        _connectionState.value = ConnectionState.Connected(code)
                        currentSessionRef = ref
                    } else {
                        // Session deleted?
                        leaveSession()
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

    private fun generateSessionCode(): String {
        return (100000..999999).random().toString()
    }
    
    private fun createSessionUser(): SessionUser {
        return SessionUser(
            id = currentUserId,
            name = currentUserName,
            avatarUrl = currentUserAvatar,
            isActive = true
        )
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
}

sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data object Connecting : ConnectionState()
    data class Connected(val code: String) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}
