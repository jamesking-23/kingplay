package com.kingplay.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.kingplay.core.designsystem.component.KpButton
import com.kingplay.core.designsystem.component.KpButtonStyle
import com.kingplay.core.designsystem.theme.KpIcons
import com.kingplay.core.designsystem.theme.KpTheme
import com.kingplay.data.library.platform.MediaPermissions

/**
 * Shows [content] once the app may read media. Until then it explains why access is needed and asks for it.
 * The notification permission (Android 13+) is requested in the same dialog; refusing it does not block the app.
 */
@Composable
fun PermissionGate(
    modifier: Modifier = Modifier,
    appViewModel: AppViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(MediaPermissions.hasAny(context)) }
    var asked by rememberSaveable { mutableStateOf(false) }

    // The user may allow access from system settings and come back.
    LifecycleResumeEffect(Unit) {
        val nowGranted = MediaPermissions.hasAny(context)
        if (nowGranted && !granted) appViewModel.onMediaAccessGranted()
        granted = nowGranted
        onPauseOrDispose { }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        asked = true
        val nowGranted = MediaPermissions.hasAny(context)
        if (nowGranted) appViewModel.onMediaAccessGranted()
        granted = nowGranted
    }

    if (granted) {
        content()
        return
    }

    val spacing = KpTheme.spacing
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = KpIcons.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = spacing.lg).size(64.dp),
            )
            Text(
                text = stringResource(R.string.perm_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(spacing.md))
            Text(
                text = stringResource(if (asked) R.string.perm_denied_hint else R.string.perm_message),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(spacing.xl))
            KpButton(
                text = stringResource(R.string.perm_grant),
                onClick = { launcher.launch(MediaPermissions.required() + notificationPermission()) },
            )
            if (asked) {
                Spacer(Modifier.height(spacing.sm))
                KpButton(
                    text = stringResource(R.string.perm_open_settings),
                    onClick = { context.startActivity(appSettingsIntent(context.packageName)) },
                    style = KpButtonStyle.Outlined,
                )
            }
        }
    }
}

private fun notificationPermission(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        emptyArray()
    }

private fun appSettingsIntent(packageName: String): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
