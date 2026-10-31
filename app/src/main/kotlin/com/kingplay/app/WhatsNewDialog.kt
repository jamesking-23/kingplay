package com.kingplay.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kingplay.core.model.update.WhatsNew

/**
 * After an update, lists what is new once. A fresh install only records the version silently, so the first run
 * shows the welcome and nothing else.
 */
@Composable
fun WhatsNewDialog(lastSeen: String, installed: String, onSeen: () -> Unit) {
    var open by rememberSaveable { mutableStateOf(true) }
    val show = WhatsNew.shouldShow(lastSeen, installed)
    LaunchedEffect(lastSeen, installed) {
        if (lastSeen.isBlank() && installed.isNotBlank() && installed != "dev") onSeen()
    }
    if (!show || !open) return
    AlertDialog(
        onDismissRequest = {
            open = false
            onSeen()
        },
        title = { Text("What\'s new in $installed") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                WhatsNew.highlights.forEach { line ->
                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text("•  ")
                        Text(line)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                open = false
                onSeen()
            }) { Text("Got it") }
        },
    )
}
