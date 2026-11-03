package com.kingplay.feature.library.options

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kingplay.core.designsystem.theme.KpTheme
import com.kingplay.core.model.Track
import com.kingplay.core.model.VideoItem
import com.kingplay.core.ui.component.LocalTrackOptions
import com.kingplay.core.ui.component.TrackOptions
import com.kingplay.feature.library.R
import com.kingplay.feature.library.list.CreatePlaylistDialog

/**
 * Wraps the app so any song menu can add to a playlist, share the file, or delete it from the device.
 * The dialogs and Android's own confirmation screens live here, once, instead of in every list.
 */
@Composable
fun TrackOptionsHost(
    modifier: Modifier = Modifier,
    viewModel: TrackOptionsViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    var playlistTarget by remember { mutableStateOf<List<Track>?>(null) }
    var creatingFor by remember { mutableStateOf<List<Track>?>(null) }
    var deleteTarget by remember { mutableStateOf<List<FileItem>?>(null) }
    var retryDelete by remember { mutableStateOf<List<FileItem>?>(null) }

    // The launcher is needed inside its own result callback (Android 10 asks once per file), so reach it through this.
    val launch = remember { arrayOf<(android.content.IntentSender) -> Unit>({}) }
    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val again = retryDelete
        retryDelete = null
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        // On Android 10 the screen only grants permission for one file; carry on with it and the rest.
        if (again != null) startDelete(context, again, viewModel, launch[0], setRetry = { retryDelete = it })
        else viewModel.afterDeleted()
    }

    launch[0] = { sender -> deleteLauncher.launch(IntentSenderRequest.Builder(sender).build()) }

    fun deleteFiles(items: List<FileItem>) {
        if (items.isEmpty()) return
        // From Android 11 the system shows its own confirmation, so a second question would be noise.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startDelete(context, items, viewModel, launch[0], setRetry = { retryDelete = it })
        } else {
            deleteTarget = items
        }
    }

    val options = remember {
        object : TrackOptions {
            override fun addToPlaylist(track: Track) {
                playlistTarget = listOf(track)
            }

            override fun share(track: Track) = shareFiles(context, listOf(track.toFile()), "audio/*")

            override fun delete(track: Track) = deleteFiles(listOf(track.toFile()))

            override fun addToPlaylist(tracks: List<Track>) {
                if (tracks.isNotEmpty()) playlistTarget = tracks
            }

            override fun share(tracks: List<Track>) = shareFiles(context, tracks.map { it.toFile() }, "audio/*")

            override fun delete(tracks: List<Track>) = deleteFiles(tracks.map { it.toFile() })

            override fun shareVideos(videos: List<VideoItem>) =
                shareFiles(context, videos.map { FileItem(it.uri, it.title) }, "video/*")

            override fun deleteVideos(videos: List<VideoItem>) =
                deleteFiles(videos.map { FileItem(it.uri, it.title) })
        }
    }
    CompositionLocalProvider(LocalTrackOptions provides options) { content() }

    playlistTarget?.let { chosen ->
        AlertDialog(
            onDismissRequest = { playlistTarget = null },
            title = { Text(stringResource(R.string.options_choose_playlist)) },
            text = {
                LazyColumn(Modifier.heightIn(max = PICKER_MAX_HEIGHT)) {
                    item {
                        Text(
                            stringResource(R.string.playlist_new),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    creatingFor = chosen
                                    playlistTarget = null
                                }
                                .padding(vertical = KpTheme.spacing.md),
                        )
                    }
                    items(playlists, key = { it.id }) { playlist ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.add(playlist.id, chosen)
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.options_added_to, playlist.name),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                    playlistTarget = null
                                }
                                .padding(vertical = KpTheme.spacing.md),
                        ) { Text(playlist.name, style = MaterialTheme.typography.bodyLarge) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { playlistTarget = null }) { Text(stringResource(R.string.playlist_cancel)) } },
        )
    }
    creatingFor?.let { chosen ->
        CreatePlaylistDialog(
            onDismiss = { creatingFor = null },
            onCreate = { name ->
                creatingFor = null
                viewModel.createWith(name, chosen)
                Toast.makeText(context, context.getString(R.string.options_added_to, name), Toast.LENGTH_SHORT).show()
            },
        )
    }
    deleteTarget?.let { items ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = {
                Text(
                    stringResource(
                        if (items.size == 1) R.string.options_delete_title else R.string.options_delete_many_title,
                    ),
                )
            },
            text = {
                Text(
                    if (items.size == 1) {
                        stringResource(R.string.options_delete_message, items.first().title)
                    } else {
                        stringResource(R.string.options_delete_many_message, items.size)
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    deleteTarget = null
                    startDelete(context, items, viewModel, launch[0], setRetry = { retryDelete = it })
                }) { Text(stringResource(R.string.options_delete_confirm)) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.playlist_cancel)) } },
        )
    }
}

private val PICKER_MAX_HEIGHT = 320.dp

/** A file the person picked: a song or a video, reduced to what sharing and deleting need. */
internal data class FileItem(val uri: String, val title: String)

private fun Track.toFile() = FileItem(uri, title)

/** Opens Android's share sheet with the files, so they can go by Bluetooth, Quick Share, chat apps and so on. */
private fun shareFiles(context: Context, files: List<FileItem>, defaultType: String) {
    if (files.isEmpty()) return
    val uris = files.map { Uri.parse(it.uri) }
    val send: Intent
    val title: String
    if (files.size == 1) {
        val type = context.contentResolver.getType(uris.first()) ?: defaultType
        send = Intent(Intent.ACTION_SEND)
            .setType(type)
            .putExtra(Intent.EXTRA_STREAM, uris.first())
            .putExtra(Intent.EXTRA_SUBJECT, files.first().title)
            .putExtra(Intent.EXTRA_TITLE, files.first().title)
        title = context.getString(R.string.options_share_title, files.first().title)
    } else {
        send = Intent(Intent.ACTION_SEND_MULTIPLE)
            .setType(defaultType)
            .putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        // The grant to read every file only reaches the other app when the uris are also in the clip data.
        send.clipData = ClipData.newRawUri("", uris.first()).also { clip ->
            uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
        }
        title = context.getString(R.string.options_share_many_title, files.size)
    }
    send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    val chooser = Intent.createChooser(send, title).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        context.startActivity(chooser)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, R.string.options_share_failed, Toast.LENGTH_SHORT).show()
    }
}

/**
 * Deletes the files through MediaStore. Android 11 and later ask once, in a system screen, for all of them. Android 10
 * may ask per file: the ones left to do are handed to [setRetry] and tried again when permission is granted.
 */
private fun startDelete(
    context: Context,
    files: List<FileItem>,
    viewModel: TrackOptionsViewModel,
    launch: (android.content.IntentSender) -> Unit,
    setRetry: (List<FileItem>?) -> Unit,
) {
    val resolver = context.contentResolver
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val request = MediaStore.createDeleteRequest(resolver, files.map { Uri.parse(it.uri) })
        launch(request.intentSender)
        return
    }
    files.forEachIndexed { index, file ->
        try {
            resolver.delete(Uri.parse(file.uri), null, null)
        } catch (e: RecoverableSecurityException) {
            setRetry(files.drop(index))
            launch(e.userAction.actionIntent.intentSender)
            return
        } catch (e: SecurityException) {
            Toast.makeText(context, R.string.options_delete_unsupported, Toast.LENGTH_LONG).show()
            return
        }
    }
    viewModel.afterDeleted()
}
