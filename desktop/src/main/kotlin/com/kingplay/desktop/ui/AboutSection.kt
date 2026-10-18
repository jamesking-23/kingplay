package com.kingplay.desktop.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.kingplay.core.model.about.AboutInfo
import com.kingplay.core.model.about.Platform
import java.awt.Desktop
import java.net.URI

/** Keep equal to `packageVersion` in desktop/build.gradle.kts. */
internal const val APP_VERSION = "1.0.0"

/** One label per run of the program, shown in About so a person can quote it when asking for help. */
private val sessionId: String by lazy { AboutInfo.newSessionId() }

private enum class AboutPage(val title: String) {
    License("License Terms"),
    Privacy("Privacy Policy"),
    ThirdParty("Third-Party Software Acknowledgements"),
}

/** The About block: name, copyright and version, with details and legal pages that open on demand. */
@Composable
fun AboutSection() {
    var expanded by remember { mutableStateOf(false) }
    var page by remember { mutableStateOf<AboutPage?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant) {
            Column {
                AboutHeader(expanded = expanded, onToggle = { expanded = !expanded })
                if (expanded) {
                    HorizontalDivider()
                    SelectionContainer {
                        Text(
                            "Session identifier: $sessionId",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                    HorizontalDivider()
                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        AboutPage.entries.forEach { entry ->
                            TextButton(onClick = { page = entry }) { Text(entry.title) }
                        }
                    }
                }
            }
        }
        Row {
            TextButton(onClick = { openLink(AboutInfo.helpUrl(Platform.DESKTOP)) }) { Text("Help") }
            TextButton(onClick = { openLink(AboutInfo.FEEDBACK_URL) }) { Text("Feedback") }
        }
    }
    page?.let { AboutTextDialog(it.title, pageText(it), onClose = { page = null }) }
}

@Composable
private fun AboutHeader(expanded: Boolean, onToggle: () -> Unit) {
    val toggleLabel = if (expanded) "Hide details" else "Show details"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClickLabel = toggleLabel) { onToggle() }
            .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.PlayCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(36.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(AboutInfo.PRODUCT, style = MaterialTheme.typography.bodyLarge)
            Text(
                AboutInfo.copyright(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text("Version $APP_VERSION", style = MaterialTheme.typography.bodyMedium)
        Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
    }
}

private fun pageText(page: AboutPage): String = when (page) {
    AboutPage.License -> AboutInfo.licenseTerms
    AboutPage.Privacy -> AboutInfo.privacyPolicy
    AboutPage.ThirdParty -> AboutInfo.acknowledgements(Platform.DESKTOP)
}

@Composable
private fun AboutTextDialog(title: String, text: String, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                SelectionContainer { Text(text, style = MaterialTheme.typography.bodyMedium) }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Close") } },
    )
}

/** Opens a web address in the default browser; does nothing if this PC has none. */
private fun openLink(url: String) {
    runCatching {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI(url))
        }
    }
}
