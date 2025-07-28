package com.example.promptpilot.di

import android.content.Context
import androidx.room.Room
import com.example.promptpilot.data.remote.AppDatabase
import com.example.promptpilot.data.remote.PendingAttachmentDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "promptpilot_db"
        ).build()
    }

    @Provides
    fun providePendingAttachmentDao(appDatabase: AppDatabase): PendingAttachmentDao {
        return appDatabase.pendingAttachmentDao()
    }
}