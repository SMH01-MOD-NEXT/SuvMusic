package com.suvojeet.suvmusic.ui.components.dialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.suvojeet.suvmusic.data.model.SessionUser

/**
 * Full-screen syncing overlay that shows when devices are synchronizing.
 * Displays which users are ready vs buffering.
 */
@Composable
fun SyncingDialog(
    isVisible: Boolean,
    users: List<SessionUser>,
    currentUserId: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.85f),
                            Color.Black.copy(alpha = 0.95f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(24.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Animated sync icon
                    val infiniteTransition = rememberInfiniteTransition(label = "sync")
                    val rotation by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "rotation"
                    )
                    
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        modifier = Modifier
                            .size(64.dp)
                            .rotate(rotation),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "Syncing Devices",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Waiting for all devices to be ready...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // User sync status
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        users.forEach { user ->
                            UserSyncStatus(
                                user = user,
                                isCurrentUser = user.id == currentUserId
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Status summary
                    val readyCount = users.count { !it.isBuffering }
                    val totalCount = users.size
                    
                    Text(
                        text = "$readyCount of $totalCount ready",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (readyCount == totalCount) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun UserSyncStatus(
    user: SessionUser,
    isCurrentUser: Boolean
) {
    val isReady = !user.isBuffering
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isReady) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                },
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with status indicator
        Box {
            AsyncImage(
                model = user.avatarUrl.ifEmpty { 
                    "https://ui-avatars.com/api/?name=${user.name}&background=random" 
                },
                contentDescription = user.name,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            
            // Status indicator
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .align(Alignment.BottomEnd)
                    .background(
                        color = if (isReady) {
                            Color(0xFF4CAF50) // Green
                        } else {
                            Color(0xFFFFA726) // Orange
                        },
                        shape = CircleShape
                    )
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isReady) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Ready",
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(8.dp),
                        strokeWidth = 1.dp,
                        color = Color.White
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isCurrentUser) "${user.name} (You)" else user.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (isReady) "Ready" else "Buffering...",
                style = MaterialTheme.typography.bodySmall,
                color = if (isReady) {
                    Color(0xFF4CAF50)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}
