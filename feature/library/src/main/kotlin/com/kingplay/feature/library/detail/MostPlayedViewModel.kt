package com.kingplay.feature.library.detail

import com.kingplay.core.model.repository.LibraryRepository
import com.kingplay.core.model.repository.PlaybackController
import com.kingplay.feature.library.logic.TrackActions
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.flowOf

/** The automatic "Most played" list: the 100 songs played most often, the top one first. */
@HiltViewModel
class MostPlayedViewModel @Inject constructor(
    library: LibraryRepository,
    actions: TrackActions,
    playback: PlaybackController,
) : DetailViewModel(
    title = flowOf(DetailTitle(MOST_PLAYED_TITLE)),
    trackSource = library.mostPlayed(MOST_PLAYED_LIMIT),
    actions = actions,
    playback = playback,
) {
    companion object {
        const val MOST_PLAYED_LIMIT = 100
        private const val MOST_PLAYED_TITLE = "Most played"
    }
}
