package com.kingplay.feature.widget

import android.content.Context
import com.kingplay.core.model.repository.NowPlayingSnapshot

/**
 * The widget runs in a separate render pass from the player, so the last snapshot is kept in a tiny
 * SharedPreferences file that both sides can read.
 */
internal class SnapshotStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun save(snapshot: NowPlayingSnapshot) {
        prefs.edit()
            .putString(KEY_ID, snapshot.mediaId)
            .putString(KEY_TITLE, snapshot.title)
            .putString(KEY_ARTIST, snapshot.artist)
            .putBoolean(KEY_PLAYING, snapshot.isPlaying)
            .putString(KEY_ART, snapshot.artworkSource)
            .apply()
    }

    fun load(): NowPlayingSnapshot {
        val id = prefs.getString(KEY_ID, null) ?: return NowPlayingSnapshot.Idle
        return NowPlayingSnapshot(
            mediaId = id,
            title = prefs.getString(KEY_TITLE, "").orEmpty(),
            artist = prefs.getString(KEY_ARTIST, "").orEmpty(),
            isPlaying = prefs.getBoolean(KEY_PLAYING, false),
            artworkSource = prefs.getString(KEY_ART, null),
        )
    }

    private companion object {
        const val FILE = "kingplay_widget"
        const val KEY_ID = "id"
        const val KEY_TITLE = "title"
        const val KEY_ARTIST = "artist"
        const val KEY_PLAYING = "playing"
        const val KEY_ART = "art"
    }
}
