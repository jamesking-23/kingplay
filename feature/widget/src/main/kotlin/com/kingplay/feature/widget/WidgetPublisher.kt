package com.kingplay.feature.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.kingplay.core.model.repository.NowPlayingPublisher
import com.kingplay.core.model.repository.NowPlayingSnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Saves the snapshot and asks every placed widget to redraw. Does nothing visible if no widget is placed. */
@Singleton
internal class WidgetPublisher @Inject constructor(
    @ApplicationContext private val context: Context,
) : NowPlayingPublisher {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val store = SnapshotStore(context)

    override fun publish(snapshot: NowPlayingSnapshot) {
        store.save(snapshot)
        scope.launch {
            runCatching { PlaybackWidget().updateAll(context) }
        }
    }
}
