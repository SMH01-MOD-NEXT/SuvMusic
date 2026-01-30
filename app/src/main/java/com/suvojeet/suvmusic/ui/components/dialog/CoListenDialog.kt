package com.suvojeet.suvmusic.ui.components.dialog

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.suvojeet.suvmusic.data.ConnectionState
import com.suvojeet.suvmusic.data.model.Session
import com.suvojeet.suvmusic.ui.viewmodel.PlayerViewModel

@Composable
fun CoListenDialog(
    viewModel: PlayerViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val connectionState by viewModel.coListenConnectionState.collectAsState()
    val sessionState by viewModel.coListenSessionState.collectAsState()
    val sessionEndedEvent by viewModel.coListenSessionEnded.collectAsState()
    val isHost = viewModel.isCoListenHost()

    // Handle session end event
    LaunchedEffect(sessionEndedEvent) {
        sessionEndedEvent?.let { reason ->
            Toast.makeText(context, reason, Toast.LENGTH_LONG).show()
            viewModel.clearSessionEndedEvent()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Listen Together") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (val state = connectionState) {
                    is ConnectionState.Disconnected -> {
                        DisconnectedContent(
                            onCreateSession = { viewModel.createCoListenSession() },
                            onJoinSession = { code -> viewModel.joinCoListenSession(code) }
                        )
                    }
                    is ConnectionState.Connecting -> {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Connecting...")
                    }
                    is ConnectionState.Connected -> {
                        ConnectedContent(
                            sessionCode = state.code,
                            session = sessionState,
                            isHost = isHost,
                            onLeave = { viewModel.leaveCoListenSession() }
                        )
                    }
                    is ConnectionState.Error -> {
                        Text(
                            text = "Error: ${state.message}",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.leaveCoListenSession() }) {
                            Text("Retry")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun DisconnectedContent(
    onCreateSession: () -> Unit,
    onJoinSession: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Host a session or join a friend to listen together in real-time.")
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onCreateSession,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start a Session")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("OR")
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = code,
            onValueChange = { if (it.length <= 6) code = it },
            label = { Text("Enter Session Code") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedButton(
            onClick = { onJoinSession(code) },
            enabled = code.length == 6,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Join Session")
        }
    }
}

@Composable
fun ConnectedContent(
    sessionCode: String,
    session: Session?,
    isHost: Boolean,
    onLeave: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = sessionCode,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text("Share this code with friends", style = MaterialTheme.typography.bodySmall)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "${session?.users?.size ?: 0} Listening",
            style = MaterialTheme.typography.titleMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        // Show host indicator
        if (isHost) {
            Text(
                text = "You are the host",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // List users (simple horizontal row for now)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            session?.users?.values?.take(5)?.forEach { user ->
                AsyncImage(
                    model = user.avatarUrl.ifEmpty { "https://ui-avatars.com/api/?name=${user.name}" },
                    contentDescription = user.name,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .padding(4.dp),
                    contentScale = ContentScale.Crop
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        // Leave/End button with different text for host
        Button(
            onClick = onLeave,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Text(if (isHost) "End Session" else "Leave Session")
        }

        if (isHost) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Ending the session will disconnect all listeners",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
