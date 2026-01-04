package com.suvojeet.suvmusic.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmusic.navigation.Destination
import com.suvojeet.suvmusic.ui.theme.GradientEnd
import com.suvojeet.suvmusic.ui.theme.GradientMiddle
import com.suvojeet.suvmusic.ui.theme.GradientStart
import com.suvojeet.suvmusic.ui.theme.GlassWhite

/**
 * Material 3 Expressive Bottom Navigation.
 * Features animated selection indicator and gradient backgrounds.
 */
@Composable
fun ExpressiveBottomNav(
    currentDestination: Destination,
    onDestinationChange: (Destination) -> Unit,
    modifier: Modifier = Modifier
) {
    val navItems = listOf(
        NavItem(Destination.Home, "Home", Icons.Outlined.Home, Icons.Filled.Home),
        NavItem(Destination.Search, "Search", Icons.Outlined.Search, Icons.Filled.Search),
        NavItem(Destination.Library, "Library", Icons.Outlined.LibraryMusic, Icons.Filled.LibraryMusic),
        NavItem(Destination.Settings, "Settings", Icons.Outlined.Settings, Icons.Filled.Settings)
    )
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEach { item ->
                val isSelected = currentDestination == item.destination
                
                NavItemView(
                    item = item,
                    isSelected = isSelected,
                    onClick = { onDestinationChange(item.destination) }
                )
            }
        }
    }
}

@Composable
private fun NavItemView(
    item: NavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    val iconSize by animateDpAsState(
        targetValue = if (isSelected) 26.dp else 24.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "iconSize"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) 
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) 
        else 
            Color.Transparent,
        label = "backgroundColor"
    )
    
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) 
            MaterialTheme.colorScheme.primary 
        else 
            MaterialTheme.colorScheme.onSurfaceVariant,
        label = "contentColor"
    )
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    // Gradient indicator behind icon
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        GradientStart.copy(alpha = 0.3f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
                
                Icon(
                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = item.label,
                    modifier = Modifier.size(iconSize),
                    tint = contentColor
                )
            }
            
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor
            )
        }
    }
}

private data class NavItem(
    val destination: Destination,
    val label: String,
    val unselectedIcon: ImageVector,
    val selectedIcon: ImageVector
)
