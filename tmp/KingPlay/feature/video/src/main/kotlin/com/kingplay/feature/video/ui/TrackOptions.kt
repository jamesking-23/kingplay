package com.kingplay.feature.video.ui

import androidx.compose.runtime.Immutable
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import com.kingplay.feature.video.logic.trackLabel
import java.util.Locale

/** One audio or subtitle track the user can pick. */
@Immutable
class TrackOption(
    val label: String,
    val selected: Boolean,
    val group: Tracks.Group,
    val index: Int,
)

fun trackOptions(
    tracks: Tracks,
    type: Int,
    fallbackName: String,
    locale: Locale = Locale.getDefault(),
): List<TrackOption> =
    buildList {
        for (group in tracks.groups) {
            if (group.type != type) continue
            for (index in 0 until group.length) {
                if (!group.isTrackSupported(index)) continue
                val format = group.getTrackFormat(index)
                val channels = if (type == C.TRACK_TYPE_AUDIO) format.channelCount.coerceAtLeast(0) else 0
                add(
                    TrackOption(
                        label = trackLabel(format.label, format.language, channels, size, locale, fallbackName),
                        selected = group.isTrackSelected(index),
                        group = group,
                        index = index,
                    ),
                )
            }
        }
    }

fun Player.select(option: TrackOption) {
    val type = option.group.type
    trackSelectionParameters = trackSelectionParameters.buildUpon()
        .setTrackTypeDisabled(type, false)
        .setOverrideForType(TrackSelectionOverride(option.group.mediaTrackGroup, option.index))
        .build()
}

fun Player.setSubtitlesEnabled(enabled: Boolean) {
    trackSelectionParameters = trackSelectionParameters.buildUpon()
        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !enabled)
        .build()
}

fun Player.subtitlesEnabled(): Boolean = !trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)
