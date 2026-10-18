package com.kingplay.data.library.di

import com.kingplay.core.model.repository.BackupRepository
import com.kingplay.core.model.repository.EqRepository
import com.kingplay.core.model.repository.LibraryFolderSettings
import com.kingplay.core.model.repository.LibraryRepository
import com.kingplay.core.model.repository.LibraryScanner
import com.kingplay.core.model.repository.LyricsStore
import com.kingplay.core.model.repository.PlaylistRepository
import com.kingplay.core.model.repository.QueueRepository
import com.kingplay.core.model.repository.SearchRepository
import com.kingplay.core.model.repository.VideoRepository
import com.kingplay.data.library.platform.MediaExtractorMetadataReader
import com.kingplay.data.library.platform.MediaStoreSource
import com.kingplay.data.library.platform.WorkManagerLibraryScanner
import com.kingplay.data.library.repo.DefaultBackupRepository
import com.kingplay.data.library.repo.RoomEqRepository
import com.kingplay.data.library.repo.RoomLibraryFolderSettings
import com.kingplay.data.library.repo.RoomLibraryRepository
import com.kingplay.data.library.repo.RoomLibraryStore
import com.kingplay.data.library.repo.RoomLyricsStore
import com.kingplay.data.library.repo.RoomPlaylistRepository
import com.kingplay.data.library.repo.RoomQueueRepository
import com.kingplay.data.library.repo.RoomSearchRepository
import com.kingplay.data.library.repo.RoomVideoRepository
import com.kingplay.data.library.scan.AudioMetadataReader
import com.kingplay.data.library.scan.LibraryStore
import com.kingplay.data.library.scan.MediaSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class LibraryDataModule {
    @Binds abstract fun library(impl: RoomLibraryRepository): LibraryRepository

    @Binds abstract fun videos(impl: RoomVideoRepository): VideoRepository

    @Binds abstract fun playlists(impl: RoomPlaylistRepository): PlaylistRepository

    @Binds abstract fun queue(impl: RoomQueueRepository): QueueRepository

    @Binds abstract fun search(impl: RoomSearchRepository): SearchRepository

    @Binds abstract fun eq(impl: RoomEqRepository): EqRepository

    @Binds abstract fun folderSettings(impl: RoomLibraryFolderSettings): LibraryFolderSettings

    @Binds abstract fun lyricsStore(impl: RoomLyricsStore): LyricsStore

    @Binds abstract fun backup(impl: DefaultBackupRepository): BackupRepository

    @Binds abstract fun scanner(impl: WorkManagerLibraryScanner): LibraryScanner

    @Binds abstract fun store(impl: RoomLibraryStore): LibraryStore

    @Binds abstract fun mediaSource(impl: MediaStoreSource): MediaSource

    @Binds abstract fun metadataReader(impl: MediaExtractorMetadataReader): AudioMetadataReader
}
