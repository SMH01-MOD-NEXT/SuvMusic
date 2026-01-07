package com.suvojeet.suvmusic.di

import android.content.Context
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.data.repository.LocalAudioRepository
import com.suvojeet.suvmusic.data.repository.UpdateRepository
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import com.suvojeet.suvmusic.player.MusicPlayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideSessionManager(
        @ApplicationContext context: Context
    ): SessionManager {
        return SessionManager(context)
    }
    
    @Provides
    @Singleton
    fun provideYouTubeRepository(
        sessionManager: SessionManager
    ): YouTubeRepository {
        return YouTubeRepository(sessionManager)
    }
    
    @Provides
    @Singleton
    fun provideLocalAudioRepository(
        @ApplicationContext context: Context
    ): LocalAudioRepository {
        return LocalAudioRepository(context)
    }
    
    @Provides
    @Singleton
    fun provideUpdateRepository(
        @ApplicationContext context: Context
    ): UpdateRepository {
        return UpdateRepository(context)
    }
    
    @Provides
    @Singleton
    fun provideMusicPlayer(
        @ApplicationContext context: Context,
        youTubeRepository: YouTubeRepository,
        sessionManager: SessionManager,
        sleepTimerManager: com.suvojeet.suvmusic.player.SleepTimerManager
    ): MusicPlayer {
        return MusicPlayer(context, youTubeRepository, sessionManager, sleepTimerManager)
    }
}
