package com.kingplay.feature.library.options

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ActivityNotFoundException
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
    var playlistTarget by remember { mutableStateOf<Track?>(null) }
    var creatingFor by remember { mutableStateOf<Track?>(null) }
    var deleteTarget by remember { mutableStateOf<Track?>(null) }
    var retryDelete by remember { mutableStateOf<Track?>(null) }

    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val again = retryDelete
        retryDelete = null
        // On Android 10 the screen only grants permission; the file is deleted by trying again.
        if (again != null) deleteFile(context, again, onNeedsPermission = {}, onDone = viewModel::afterDeleted)
        else viewModel.afterDeleted()
    }

    fun startDelete(track: Track) {
        deleteFile(
            context = context,
            track = track,
            onNeedsPermission = { sender ->
                retryDelete = track.takeIf { Build.VERSION.SDK_INT < Build.VERSION_CODES.R }
                deleteLauncher.launch(IntentSenderRequest.Builder(sender).build())
            },
            onDone = viewModel::afterDeleted,
        )
    }

    val options = remember {
        object : TrackOptions {
            override fun addToPlaylist(track: Track) {
                playlistTarget = track
            }

            override fun share(track: Track) = shareTrack(context, track)

            override fun delete(track: Track) {
                // From Android 11 the system shows its own confirmation, so a second question would be noise.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) startDelete(track) else deleteTarget = track
            }
        }
    }
    CompositionLocalProvider(LocalTrackOptions provides options) { content() }

    playlistTarget?.let { track ->
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
                                    creatingFor = track
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
                                    viewModel.add(playlist.id, track)
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
    creatingFor?.let { track ->
        CreatePlaylistDialog(
            onDismiss = { creatingFor = null },
            onCreate = { name ->
                creatingFor = null
                viewModel.createWith(name, track)
                Toast.makeText(context, context.getString(R.string.options_added_to, name), Toast.LENGTH_SHORT).show()
            },
        )
    }
    deleteTarget?.let { track ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.options_delete_title)) },
            text = { Text(stringResource(R.string.options_delete_message, track.title)) },
            confirmButton = {
                TextButton(onClick = {
                    deleteTarget = null
                    startDelete(track)
                }) { Text(stringResource(R.string.options_delete_confirm)) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.playlist_cancel)) } },
        )
    }
}

private val PICKER_MAX_HEIGHT = 320.dp

/** Opens Android's share sheet with the song file, so it can go by Bluetooth, Quick Share, chat apps and so on. */
private fun shareTrack(context: Context, track: Track) {
    val uri = Uri.parse(track.uri)
    val type = context.contentResolver.getType(uri) ?: "audio/*"
    val send = Intent(Intent.ACTION_SEND)
        .setType(type)
        .putExtra(Intent.EXTRA_STREAM, uri)
        .putExtra(Intent.EXTRA_SUBJECT, track.title)
        .putExtra(Intent.EXTRA_TITLE, track.title)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    val chooser = Intent.createChooser(send, context.getString(R.string.options_share_title, track.title))
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        context.startActivity(chooser)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, R.string.options_share_failed, Toast.LENGTH_SHORT).show()
    }
}

/**
 * Deletes the file through MediaStore. Android 11 and later ask the person to confirm in a system screen
 * (handed back through [onNeedsPermission]); Android 10 may ask for permission first. [onDone] runs once it is gone.
 */
private fun deleteFile(
    context: Context,
    track: Track,
    onNeedsPermission: (android.content.IntentSender) -> Unit,
    onDone: () -> Unit,
) {
    val resolver = context.contentResolver
    val uri = Uri.parse(track.uri)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        onNeedsPermission(MediaStore.createDeleteRequest(resolver, listOf(uri)).intentSender)
        return
    }
    try {
        resolver.delete(uri, null, null)
        onDone()
    } catch (e: RecoverableSecurityException) {
        onNeedsPermission(e.userAction.actionIntent.intentSender)
    } catch (e: SecurityException) {
        Toast.makeText(context, R.string.options_delete_unsupported, Toast.LENGTH_LONG).show()
    }
}
