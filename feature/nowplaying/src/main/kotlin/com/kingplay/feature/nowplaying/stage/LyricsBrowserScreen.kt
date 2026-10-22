package com.kingplay.feature.nowplaying.stage

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kingplay.core.designsystem.component.KpButton
import com.kingplay.core.designsystem.component.KpIconButton
import com.kingplay.core.designsystem.theme.KpTheme
import com.kingplay.core.model.navigation.AppNavigator
import com.kingplay.core.ui.R as UiR
import com.kingplay.feature.nowplaying.R

private const val SEARCH_URL = "https://duckduckgo.com/?q="
private const val MAX_PREVIEW_CHARS = 1_500
private const val FULL_PROGRESS = 100

/**
 * A browser inside KingPlay. The person finds the lyrics, selects and copies them, then taps "Add copied lyrics".
 * KingPlay never reads the page itself; it only reads what the person copied, and keeps it on this device.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsBrowserScreen(
    navigator: AppNavigator,
    modifier: Modifier = Modifier,
    viewModel: LyricsBrowserViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var progress by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(webView) {
        val view = webView ?: return@LaunchedEffect
        if (view.url == null) {
            val query = viewModel.searchQuery()
            view.loadUrl(SEARCH_URL + Uri.encode(query))
        }
    }
    BackHandler(enabled = canGoBack) { webView?.goBack() }

    Scaffold(
        modifier = modifier,
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    scrolledContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                ),
                title = { Text(stringResource(R.string.lyrics_browser_title)) },
                navigationIcon = {
                    KpIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(UiR.string.ui_back),
                        onClick = navigator::back,
                    )
                },
            )
        },
        bottomBar = {
            Column(Modifier.padding(KpTheme.spacing.md), verticalArrangement = Arrangement.spacedBy(KpTheme.spacing.xs)) {
                message?.let {
                    Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    stringResource(R.string.lyrics_browser_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                KpButton(
                    text = stringResource(R.string.lyrics_browser_add),
                    onClick = {
                        val copied = readClipboard(context)
                        if (copied.isBlank()) {
                            message = R.string.lyrics_browser_nothing_copied
                        } else {
                            message = null
                            pending = copied
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            if (progress in 1 until FULL_PROGRESS) {
                LinearProgressIndicator(progress = { progress / FULL_PROGRESS.toFloat() }, modifier = Modifier.fillMaxWidth())
            }
            AndroidView(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                factory = { ctx ->
                    createWebView(ctx, onProgress = { progress = it }, onNav = { canGoBack = it.canGoBack() }).also { webView = it }
                },
                onRelease = { it.destroy() },
            )
        }
    }

    pending?.let { text ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(stringResource(R.string.lyrics_browser_preview_title)) },
            text = {
                Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    Text(text.take(MAX_PREVIEW_CHARS) + if (text.length > MAX_PREVIEW_CHARS) "\u2026" else "")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.save(text) { ok ->
                        if (ok) navigator.back() else message = R.string.lyrics_browser_not_lyrics
                        pending = null
                    }
                }) { Text(stringResource(R.string.lyrics_browser_save)) }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text(stringResource(R.string.lyrics_browser_cancel)) } },
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createWebView(context: Context, onProgress: (Int) -> Unit, onNav: (WebView) -> Unit): WebView =
    WebView(context).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        settings.setSupportMultipleWindows(false)
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                request.url.scheme?.lowercase() !in setOf("http", "https")

            override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) = onNav(view)
        }
        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) = onProgress(newProgress)
        }
    }

private fun readClipboard(context: Context): String {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = manager.primaryClip ?: return ""
    if (clip.itemCount == 0) return ""
    return clip.getItemAt(0).coerceToText(context)?.toString().orEmpty().trim()
}
