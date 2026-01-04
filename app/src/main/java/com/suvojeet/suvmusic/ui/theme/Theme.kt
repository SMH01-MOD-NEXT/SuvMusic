package com.suvojeet.suvmusic.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Dark color scheme - Primary for SuvMusic
 */
private val DarkColorScheme = darkColorScheme(
    // Primary
    primary = Purple70,
    onPrimary = Purple10,
    primaryContainer = Purple30,
    onPrimaryContainer = Purple90,
    
    // Secondary
    secondary = Cyan70,
    onSecondary = Cyan10,
    secondaryContainer = Cyan30,
    onSecondaryContainer = Cyan90,
    
    // Tertiary
    tertiary = Magenta70,
    onTertiary = Magenta10,
    tertiaryContainer = Magenta30,
    onTertiaryContainer = Magenta90,
    
    // Background & Surface
    background = Neutral10,
    onBackground = Neutral90,
    surface = Neutral10,
    onSurface = Neutral90,
    surfaceVariant = NeutralVar30,
    onSurfaceVariant = NeutralVar80,
    
    // Surface containers
    surfaceDim = SurfaceDim,
    surfaceBright = SurfaceBright,
    surfaceContainerLowest = SurfaceContainerLowest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    
    // Inverse
    inverseSurface = Neutral90,
    inverseOnSurface = Neutral20,
    inversePrimary = Purple40,
    
    // Error
    error = Error80,
    onError = Error20,
    errorContainer = Error30,
    onErrorContainer = Error90,
    
    // Outline
    outline = NeutralVar60,
    outlineVariant = NeutralVar30,
    
    // Scrim
    scrim = Color.Black
)

/**
 * Light color scheme
 */
private val LightColorScheme = lightColorScheme(
    // Primary
    primary = Purple40,
    onPrimary = Color.White,
    primaryContainer = Purple90,
    onPrimaryContainer = Purple10,
    
    // Secondary
    secondary = Cyan40,
    onSecondary = Color.White,
    secondaryContainer = Cyan90,
    onSecondaryContainer = Cyan10,
    
    // Tertiary
    tertiary = Magenta40,
    onTertiary = Color.White,
    tertiaryContainer = Magenta90,
    onTertiaryContainer = Magenta10,
    
    // Background & Surface
    background = Neutral99,
    onBackground = Neutral10,
    surface = Neutral99,
    onSurface = Neutral10,
    surfaceVariant = NeutralVar90,
    onSurfaceVariant = NeutralVar30,
    
    // Inverse
    inverseSurface = Neutral20,
    inverseOnSurface = Neutral95,
    inversePrimary = Purple80,
    
    // Error
    error = Error40,
    onError = Color.White,
    errorContainer = Error90,
    onErrorContainer = Error10,
    
    // Outline
    outline = NeutralVar50,
    outlineVariant = NeutralVar80,
    
    // Scrim
    scrim = Color.Black
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SuvMusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}