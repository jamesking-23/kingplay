package com.kingplay.desktop.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingplay.core.model.settings.AppSettings
import com.kingplay.desktop.DesktopAppState
import com.kingplay.desktop.settings.DesktopSettings
import com.kingplay.desktop.userdata.ProfilePhoto
import com.kingplay.desktop.userdata.UserData
import com.kingplay.desktop.userdata.formatListening
import java.io.File
import java.text.DateFormat
import java.util.Date
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

private fun loadPicture(path: String?): ImageBitmap? =
    path?.let { p -> runCatching { ImageIO.read(File(p))?.toComposeImageBitmap() }.getOrNull() }

/** The round profile picture, or the first letter of the nickname on a coloured disc. */
@Composable
fun Avatar(photoPath: String?, nickname: String, size: Dp, modifier: Modifier = Modifier) {
    val picture = remember(photoPath) { loadPicture(photoPath) }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .semantics { contentDescription = "Profile picture of ${nickname.ifBlank { "you" }}" },
        contentAlignment = Alignment.Center,
    ) {
        if (picture != null) {
            Image(picture, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(size))
        } else {
            Text(
                nickname.trim().take(1).uppercase().ifEmpty { "?" },
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = (size.value * 0.42f).sp,
            )
        }
    }
}

private fun choosePicture(): File? {
    val chooser = JFileChooser().apply {
        dialogTitle = "Choose a profile picture"
        fileFilter = FileNameExtensionFilter("Pictures", "jpg", "jpeg", "png", "gif", "bmp")
    }
    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
}

private fun saveNickname(app: DesktopAppState, name: String) {
    val clean = name.trim().take(AppSettings.MAX_NICKNAME)
    if (clean.isNotEmpty()) app.updateSettings { it.copy(nickname = clean) }
}

private fun pickPhoto(app: DesktopAppState) {
    val file = choosePicture() ?: return
    val saved = ProfilePhoto.save(file, DesktopAppState.dataDirectory()) ?: return
    app.updateSettings { it.copy(profilePhoto = saved.absolutePath) }
}

private fun removePhoto(app: DesktopAppState) {
    app.updateSettings { it.copy(profilePhoto = null) }
    ProfilePhoto.remove(DesktopAppState.dataDirectory())
}

/** Shown once, the first time KingPlay opens: asks what to call the person, with an optional picture. */
@Composable
fun WelcomeScreen(app: DesktopAppState, settings: DesktopSettings) {
    var name by remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.widthIn(max = 420.dp).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Welcome to KingPlay", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
            Text(
                "What should we call you? You can add a picture too. Both stay on this PC.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Avatar(settings.profilePhoto, name, 120.dp)
            TextButton(onClick = { pickPhoto(app) }) { Text(if (settings.profilePhoto == null) "Add a picture" else "Change picture") }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(AppSettings.MAX_NICKNAME) },
                label = { Text("Nickname") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = { saveNickname(app, name) }, enabled = name.isNotBlank()) { Text("Let\u2019s go") }
            Text(
                "Your nickname and picture are never uploaded anywhere.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Who you are in KingPlay, plus the lifetime numbers: songs played, videos watched, and time spent listening. */
@Composable
fun ProfilePage(app: DesktopAppState, settings: DesktopSettings, user: UserData) {
    var name by remember { mutableStateOf(settings.nickname) }
    LaunchedEffect(settings.nickname) { name = settings.nickname }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Profile", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(settings.profilePhoto, settings.nickname, 96.dp)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(onClick = { pickPhoto(app) }) { Text(if (settings.profilePhoto == null) "Add a picture" else "Change picture") }
                if (settings.profilePhoto != null) TextButton(onClick = { removePhoto(app) }) { Text("Remove picture") }
            }
        }
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(AppSettings.MAX_NICKNAME) },
            label = { Text("Nickname") },
            singleLine = true,
            modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth(),
        )
        Button(
            onClick = { saveNickname(app, name) },
            enabled = name.isNotBlank() && name.trim() != settings.nickname,
        ) { Text("Save nickname") }

        Text("Your records", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp).semantics { heading() })
        Stat("Songs played", user.songPlays.toString())
        Stat("Videos played", user.videoPlays.toString())
        Stat("Time listening", formatListening(user.audioListenMs + user.videoListenMs))
        if (user.audioListenMs > 0 || user.videoListenMs > 0) {
            Text(
                "Music ${formatListening(user.audioListenMs)} \u00b7 Video ${formatListening(user.videoListenMs)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (user.installedAtMs > 0) {
            Text(
                "Installed on ${DateFormat.getDateInstance(DateFormat.LONG).format(Date(user.installedAtMs))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            "Song plays include everything counted so far. Videos and listening time are counted from this version onward.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Card(modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(value, style = MaterialTheme.typography.headlineSmall)
        }
    }
}
