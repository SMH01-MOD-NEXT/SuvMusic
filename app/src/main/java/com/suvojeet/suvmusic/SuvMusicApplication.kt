package com.suvojeet.suvmusic

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for SuvMusic.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 */
@HiltAndroidApp
class SuvMusicApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // Initialize any app-wide components here
    }
}
