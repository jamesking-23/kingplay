package com.kingplay.data.library.repo

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kingplay.core.database.dao.TrackDao
import com.kingplay.core.model.repository.PlaybackRecordsData
import com.kingplay.core.model.repository.PlaybackRecordsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

private val Context.recordsStore: DataStore<Preferences> by preferencesDataStore(name = "kingplay_records")

private object RecordKeys {
    val VIDEO_PLAYS = longPreferencesKey("video_plays")
    val AUDIO_MS = longPreferencesKey("audio_listen_ms")
    val VIDEO_MS = longPreferencesKey("video_listen_ms")
}

/** Song plays come from the library's own counters; video plays and listening time are kept here. */
@Singleton
class DataStorePlaybackRecords @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trackDao: TrackDao,
) : PlaybackRecordsRepository {

    override fun records(): Flow<PlaybackRecordsData> = combine(context.recordsStore.data, trackDao.observeTotalPlays()) { p, songs ->
        PlaybackRecordsData(
            songPlays = songs,
            videoPlays = p[RecordKeys.VIDEO_PLAYS] ?: 0,
            audioListenMs = p[RecordKeys.AUDIO_MS] ?: 0,
            videoListenMs = p[RecordKeys.VIDEO_MS] ?: 0,
            installedAtMs = installTime(),
        )
    }

    override suspend fun addListening(audioMs: Long, videoMs: Long) {
        if (audioMs <= 0 && videoMs <= 0) return
        context.recordsStore.edit { p ->
            if (audioMs > 0) p[RecordKeys.AUDIO_MS] = (p[RecordKeys.AUDIO_MS] ?: 0) + audioMs
            if (videoMs > 0) p[RecordKeys.VIDEO_MS] = (p[RecordKeys.VIDEO_MS] ?: 0) + videoMs
        }
    }

    override suspend fun addVideoPlay() {
        context.recordsStore.edit { p -> p[RecordKeys.VIDEO_PLAYS] = (p[RecordKeys.VIDEO_PLAYS] ?: 0) + 1 }
    }

    private fun installTime(): Long = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
    }.getOrDefault(0L)
}
