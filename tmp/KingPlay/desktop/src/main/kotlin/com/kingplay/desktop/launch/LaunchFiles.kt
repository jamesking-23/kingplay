package com.kingplay.desktop.launch

import com.kingplay.desktop.library.isAudioFile
import com.kingplay.desktop.video.isVideoFile
import java.io.File

/** Files handed to the app by "Open with" or a double-click in Explorer. */
data class LaunchRequest(val audio: List<String> = emptyList(), val videos: List<String> = emptyList()) {
    val isEmpty: Boolean get() = audio.isEmpty() && videos.isEmpty()

    val all: List<String> get() = audio + videos
}

/**
 * Keeps only real, readable audio and video files, as absolute paths, once each and in the order given.
 * Anything else (options, folders, missing files, other file types) is ignored, so this is also the safety filter
 * for paths that arrive from another instance of the app.
 */
fun parseLaunchFiles(args: List<String>): LaunchRequest {
    val audio = LinkedHashSet<String>()
    val videos = LinkedHashSet<String>()
    for (arg in args.take(MAX_FILES)) {
        val file = File(arg.trim().trim('"'))
        if (!file.isFile || !file.canRead()) continue
        when {
            isAudioFile(file.name) -> audio += file.absolutePath
            isVideoFile(file.name) -> videos += file.absolutePath
        }
    }
    return LaunchRequest(audio.toList(), videos.toList())
}

private const val MAX_FILES = 500
