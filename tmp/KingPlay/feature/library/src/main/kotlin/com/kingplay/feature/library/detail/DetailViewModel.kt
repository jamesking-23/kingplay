package com.kingplay.feature.library.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.kingplay.core.model.MediaRef
import com.kingplay.core.model.Track
import com.kingplay.core.model.repository.PlaybackController
import com.kingplay.core.ui.Async
import com.kingplay.core.ui.MviViewModel
import com.kingplay.core.ui.UiEvent
import com.kingplay.core.ui.UiState
import com.kingplay.feature.library.LibraryEffect
import com.kingplay.feature.library.R
import com.kingplay.feature.library.logic.TrackActions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Navigation argument names shared by the app's nav graph and the view models that read them. */
object DetailArgs {
    const val ALBUM_ID = "albumId"
    const val ARTIST_ID = "artistId"
    const val PLAYLIST_ID = "playlistId"
    const val GENRE = "genre"
    const val FOLDER_PATH = "folderPath"
}

/** Title block of a detail screen. [secondary] is the artist for an album, the path for a folder, and so on. */
data class DetailTitle(val text: String, val secondary: String? = null)

data class DetailHeader(
    val title: String,
    val secondary: String?,
    val artworkUri: String?,
    val artworkKey: String,
)

data class DetailState(
    val header: DetailHeader? = null,
    val tracks: Async<List<Track>> = Async.Loading,
    val currentTrackId: Long? = null,
) : UiState

sealed interface DetailEvent : UiEvent {
    data object PlayAll : DetailEvent

    data object ShuffleAll : DetailEvent

    data class Play(val trackId: Long) : DetailEvent

    data class PlayNext(val track: Track) : DetailEvent

    data class AddToQueue(val track: Track) : DetailEvent

    data class ToggleFavorite(val track: Track) : DetailEvent

    data object Retry : DetailEvent
}

/**
 * Shared behaviour of every "list of songs with a header" screen: album, artist, genre, folder, playlist.
 * Subclasses only say where the title and the songs come from. The flows are passed to the constructor, not read
 * from abstract members, because a base class must not call into a subclass that is not constructed yet.
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class DetailViewModel(
    title: Flow<DetailTitle?>,
    private val trackSource: Flow<List<Track>>,
    private val actions: TrackActions,
    playback: PlaybackController,
) : MviViewModel<DetailState, DetailEvent, LibraryEffect>(DetailState()) {

    private val retryTick = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            retryTick
                .flatMapLatest {
                    trackSource
                        .map { tracks -> if (tracks.isEmpty()) Async.Empty else Async.Content(tracks) }
                        .catch { error -> emit(Async.Error(error.message)) }
                }
                .collect { loaded -> setState { copy(tracks = loaded) } }
        }
        viewModelScope.launch {
            combine(title, trackSource) { name, tracks -> toHeader(name, tracks) }
                .catch { emit(null) }
                .collect { header -> setState { copy(header = header) } }
        }
        viewModelScope.launch {
            playback.state
                .map { (it.current as? MediaRef.Audio)?.id }
                .distinctUntilChanged()
                .collect { id -> setState { copy(currentTrackId = id) } }
        }
    }

    private fun currentIds(): List<Long> =
        (state.value.tracks as? Async.Content)?.data.orEmpty().map { it.id }

    override fun onEvent(event: DetailEvent) {
        when (event) {
            DetailEvent.PlayAll -> currentIds().firstOrNull()?.let { actions.playFrom(currentIds(), it) }
            DetailEvent.ShuffleAll -> actions.shuffle(currentIds())
            is DetailEvent.Play -> actions.playFrom(currentIds(), event.trackId)
            is DetailEvent.PlayNext -> {
                actions.playNext(event.track)
                sendEffect(LibraryEffect.Message(R.string.library_added_next))
            }
            is DetailEvent.AddToQueue -> {
                actions.addToQueue(event.track)
                sendEffect(LibraryEffect.Message(R.string.library_added_queue))
            }
            is DetailEvent.ToggleFavorite -> viewModelScope.launch { actions.toggleFavorite(event.track) }
            DetailEvent.Retry -> retryTick.update { it + 1 }
        }
    }
}

internal fun toHeader(title: DetailTitle?, tracks: List<Track>): DetailHeader? {
    if (title == null) return null
    val first = tracks.firstOrNull()
    return DetailHeader(
        title = title.text,
        secondary = title.secondary,
        artworkUri = first?.uri,
        artworkKey = first?.let { "album:${it.albumId}" }.orEmpty(),
    )
}

/** A required Long argument. A missing one is a programming error in the nav graph, so fail loudly. */
internal fun SavedStateHandle.requireLong(key: String): Long =
    checkNotNull(get<Long>(key)) { "Missing navigation argument '$key'" }

internal fun SavedStateHandle.requireString(key: String): String =
    checkNotNull(get<String>(key)) { "Missing navigation argument '$key'" }
