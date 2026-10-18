package com.kingplay.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.kingplay.core.designsystem.theme.KpTheme
import com.kingplay.core.model.about.AboutInfo
import com.kingplay.core.model.about.Platform

private enum class AboutPage(val titleRes: Int) {
    License(R.string.about_license),
    Privacy(R.string.about_privacy),
    ThirdParty(R.string.about_third_party),
}

/** One label per run of the app, shown in About so a person can quote it when asking for help. */
private val sessionId: String by lazy { AboutInfo.newSessionId() }

/** Name, copyright and version, with details and legal pages that open on demand. */
@Composable
internal fun AboutSection(versionName: String) {
    var expanded by remember { mutableStateOf(false) }
    var page by remember { mutableStateOf<AboutPage?>(null) }
    val links = LocalUriHandler.current
    Column(verticalArrangement = Arrangement.spacedBy(KpTheme.spacing.sm)) {
        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant) {
            Column {
                AboutHeader(versionName, expanded) { expanded = !expanded }
                if (expanded) {
                    HorizontalDivider()
                    SelectionContainer {
                        Text(
                            text = stringResource(R.string.about_session, sessionId),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(KpTheme.spacing.md),
                        )
                    }
                    HorizontalDivider()
                    Column(modifier = Modifier.padding(horizontal = KpTheme.spacing.sm)) {
                        AboutPage.entries.forEach { entry ->
                            TextButton(onClick = { page = entry }) { Text(stringResource(entry.titleRes)) }
                        }
                    }
                }
            }
        }
        Row {
            TextButton(onClick = { links.openUri(AboutInfo.helpUrl(Platform.ANDROID)) }) {
                Text(stringResource(R.string.about_help))
            }
            TextButton(onClick = { links.openUri(AboutInfo.FEEDBACK_URL) }) {
                Text(stringResource(R.string.about_feedback))
            }
        }
    }
    page?.let { AboutTextDialog(stringResource(it.titleRes), pageText(it), onClose = { page = null }) }
}

@Composable
private fun AboutHeader(versionName: String, expanded: Boolean, onToggle: () -> Unit) {
    val toggleLabel = stringResource(if (expanded) R.string.about_hide else R.string.about_show)
    val stateLabel = stringResource(if (expanded) R.string.about_expanded else R.string.about_collapsed)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClickLabel = toggleLabel, onClick = onToggle)
            .semantics { stateDescription = stateLabel }
            .padding(KpTheme.spacing.md),
        horizontalArrangement = Arrangement.spacedBy(KpTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(AboutInfo.PRODUCT, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = AboutInfo.copyright(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(stringResource(R.string.settings_version, versionName), style = MaterialTheme.typography.bodySmall)
        }
        Icon(if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, contentDescription = null)
    }
}

private fun pageText(page: AboutPage): String = when (page) {
    AboutPage.License -> AboutInfo.licenseTerms
    AboutPage.Privacy -> AboutInfo.privacyPolicy
    AboutPage.ThirdParty -> AboutInfo.acknowledgements(Platform.ANDROID)
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
        confirmButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.about_close)) } },
    )
}
