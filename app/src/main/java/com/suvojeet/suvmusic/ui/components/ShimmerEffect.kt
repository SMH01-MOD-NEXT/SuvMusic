package com.suvojeet.suvmusic.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shimmer effect colors
 */
private val shimmerColors = listOf(
    Color(0xFF2A2A2A),
    Color(0xFF3A3A3A),
    Color(0xFF2A2A2A)
)

/**
 * Creates a shimmer brush animation
 */
@Composable
fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )
}

/**
 * Shimmer box placeholder
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    width: Dp = 100.dp,
    height: Dp = 20.dp,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp)
) {
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(shape)
            .background(shimmerBrush())
    )
}

/**
 * Featured playlist skeleton
 */
@Composable
fun FeaturedPlaylistSkeleton(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(shimmerBrush())
    )
}

/**
 * Compact music card skeleton (for horizontal scroll)
 */
@Composable
fun CompactMusicCardSkeleton() {
    Column(
        modifier = Modifier.width(140.dp)
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(shimmerBrush())
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Title
        ShimmerBox(width = 120.dp, height = 14.dp)
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Artist
        ShimmerBox(width = 80.dp, height = 12.dp)
    }
}

/**
 * Playlist card skeleton
 */
@Composable
fun PlaylistCardSkeleton() {
    Column(
        modifier = Modifier.width(150.dp)
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(shimmerBrush())
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Title
        ShimmerBox(width = 130.dp, height = 14.dp)
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Subtitle
        ShimmerBox(width = 90.dp, height = 12.dp)
    }
}

/**
 * Music card skeleton (full width)
 */
@Composable
fun MusicCardSkeleton(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(shimmerBrush())
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            // Title
            ShimmerBox(width = 180.dp, height = 16.dp)
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // Artist
            ShimmerBox(width = 120.dp, height = 14.dp)
        }
    }
}

/**
 * Section header skeleton
 */
@Composable
fun SectionHeaderSkeleton(
    modifier: Modifier = Modifier
) {
    ShimmerBox(
        width = 120.dp,
        height = 24.dp,
        modifier = modifier
    )
}

/**
 * Home screen loading skeleton
 */
@Composable
fun HomeLoadingSkeleton() {
    Column(
        modifier = Modifier.padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header skeleton (greeting + avatar)
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(shimmerBrush())
            )
            // Greeting text
            ShimmerBox(width = 140.dp, height = 28.dp)
        }
        
        // Quick Picks grid skeleton (2 columns x 3 rows)
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(2) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(shimmerBrush())
                        )
                    }
                }
            }
        }
        
        // Mixed for You section
        Column {
            SectionHeaderSkeleton(modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                userScrollEnabled = false
            ) {
                items(4) {
                    PlaylistCardSkeleton()
                }
            }
        }
        
        // Listen Again section
        Column {
            SectionHeaderSkeleton(modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                userScrollEnabled = false
            ) {
                items(5) {
                    CompactMusicCardSkeleton()
                }
            }
        }
    }
}
