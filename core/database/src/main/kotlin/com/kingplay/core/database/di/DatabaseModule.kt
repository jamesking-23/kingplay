package com.kingplay.core.database.di

import android.content.Context
import androidx.room.Room
import com.kingplay.core.database.KingPlayDatabase
import com.kingplay.core.database.Migrations
import com.kingplay.core.database.dao.EqDao
import com.kingplay.core.database.dao.ExcludedFolderDao
import com.kingplay.core.database.dao.LyricsDao
import com.kingplay.core.database.dao.PlayEventDao
import com.kingplay.core.database.dao.PlaylistDao
import com.kingplay.core.database.dao.QueueDao
import com.kingplay.core.database.dao.TrackDao
import com.kingplay.core.database.dao.VideoDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): KingPlayDatabase =
        Room.databaseBuilder(context, KingPlayDatabase::class.java, KingPlayDatabase.NAME)
            // No destructive fallback: playlists live in this DB.
            .addMigrations(*Migrations.ALL)
            .build()

    @Provides
    fun trackDao(db: KingPlayDatabase): TrackDao = db.trackDao()

    @Provides
    fun videoDao(db: KingPlayDatabase): VideoDao = db.videoDao()

    @Provides
    fun playlistDao(db: KingPlayDatabase): PlaylistDao = db.playlistDao()

    @Provides
    fun queueDao(db: KingPlayDatabase): QueueDao = db.queueDao()

    @Provides
    fun eqDao(db: KingPlayDatabase): EqDao = db.eqDao()

    @Provides
    fun excludedFolderDao(db: KingPlayDatabase): ExcludedFolderDao = db.excludedFolderDao()

    @Provides
    fun playEventDao(db: KingPlayDatabase): PlayEventDao = db.playEventDao()

    @Provides
    fun lyricsDao(db: KingPlayDatabase): LyricsDao = db.lyricsDao()
}
