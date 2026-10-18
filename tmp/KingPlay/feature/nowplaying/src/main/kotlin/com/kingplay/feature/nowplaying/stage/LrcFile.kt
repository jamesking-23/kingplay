package com.kingplay.feature.nowplaying.stage

import android.content.Context
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.InputStream

private const val MAX_LRC_BYTES = 1_000_000
private const val BUFFER_BYTES = 8_192

/** Reads a picked lyrics file as text, or returns null when it cannot be read or is much too big to be lyrics. */
fun readLrcFile(context: Context, uri: Uri): String? =
    runCatching {
        context.contentResolver.openInputStream(uri)?.use { readLimited(it, MAX_LRC_BYTES) }
    }.getOrNull()

internal fun readLimited(stream: InputStream, maxBytes: Int): String? {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(BUFFER_BYTES)
    var total = 0
    while (true) {
        val read = stream.read(buffer)
        if (read < 0) break
        total += read
        if (total > maxBytes) return null
        out.write(buffer, 0, read)
    }
    return out.toString(Charsets.UTF_8.name())
}
