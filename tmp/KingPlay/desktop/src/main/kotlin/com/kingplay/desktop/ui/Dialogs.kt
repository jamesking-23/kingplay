package com.kingplay.desktop.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

class NameDialogState(
    val title: String,
    val initial: String,
    val confirmLabel: String,
    val onConfirm: (String) -> Unit,
)

@Composable
fun NameDialog(state: NameDialogState, onDismiss: () -> Unit) {
    var text by remember(state) { mutableStateOf(state.initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(state.title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Name") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    state.onConfirm(text)
                    onDismiss()
                },
                enabled = text.isNotBlank(),
            ) { Text(state.confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun ConfirmDialog(title: String, message: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismiss()
            }) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Opens the system folder picker. Returns null when the person cancels. */
fun chooseFolder(): String? {
    val chooser = JFileChooser().apply {
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        dialogTitle = "Choose your music folder"
    }
    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile.absolutePath else null
}

/** Lets the person pick a text file and returns its name and content, or null if cancelled or unreadable. */
fun chooseTextFile(title: String, description: String, vararg extensions: String): Pair<String, String>? {
    val chooser = JFileChooser().apply {
        dialogTitle = title
        fileFilter = FileNameExtensionFilter(description, *extensions)
    }
    if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return null
    val file = chooser.selectedFile
    if (file.length() > MAX_TEXT_FILE_BYTES) return null
    val text = runCatching { file.readText(Charsets.UTF_8) }.getOrNull() ?: return null
    return file.nameWithoutExtension to text
}

/** Asks where to save and writes [text]. Returns the file name, or null if cancelled or the write failed. */
fun saveTextFile(title: String, suggestedName: String, text: String): String? {
    val chooser = JFileChooser().apply {
        dialogTitle = title
        selectedFile = File(suggestedName)
    }
    if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return null
    val file = chooser.selectedFile
    return runCatching { file.writeText(text, Charsets.UTF_8) }.map { file.name }.getOrNull()
}

private const val MAX_TEXT_FILE_BYTES = 20L * 1024 * 1024
