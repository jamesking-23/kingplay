package com.kingplay.desktop.video

import com.kingplay.desktop.library.stableId
import java.io.File
import java.io.IOException
import java.nio.file.FileVisitOption
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "avi", "mov", "wmv", "webm", "m4v", "mpg", "mpeg", "ts", "flv", "3gp")

fun isVideoFile(name: String): Boolean = name.substringAfterLast('.', "").lowercase() in VIDEO_EXTENSIONS

/** A video found in a library folder. No tags are read: the title is the file name. */
data class VideoFile(
    val id: Long,
    val path: String,
    val title: String,
    val folder: String,
    val sizeBytes: Long,
    val modifiedMs: Long,
)

/** "My.Holiday_2024.mp4" is shown as "My.Holiday_2024"; only the real extension is removed. */
fun videoTitle(fileName: String): String {
    val dot = fileName.lastIndexOf('.')
    return if (dot > 0) fileName.substring(0, dot) else fileName
}

/** A single video opened from outside the library folders. Null when it is not a readable video file. */
fun videoFileFor(file: File): VideoFile? {
    if (!file.isFile || !isVideoFile(file.name)) return null
    val path = file.absolutePath
    return VideoFile(
        id = stableId("video:$path"),
        path = path,
        title = videoTitle(file.name),
        folder = file.absoluteFile.parent.orEmpty(),
        sizeBytes = file.length(),
        modifiedMs = file.lastModified(),
    )
}

object VideoScanner {
    private const val MAX_DEPTH = 32

    /** Walks [roots] like the song scan does (no links followed, unreadable folders skipped). Sorted by title. */
    fun scan(roots: List<String>): List<VideoFile> {
        val found = LinkedHashMap<String, VideoFile>()
        for (root in roots.distinct()) {
            val start = Path.of(root)
            if (!Files.isDirectory(start)) continue
            Files.walkFileTree(
                start,
                emptySet<FileVisitOption>(),
                MAX_DEPTH,
                object : SimpleFileVisitor<Path>() {
                    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                        val name = file.fileName?.toString().orEmpty()
                        if (attrs.isRegularFile && isVideoFile(name)) {
                            val path = file.toFile().absolutePath
                            found.putIfAbsent(
                                path,
                                VideoFile(
                                    id = stableId("video:$path"),
                                    path = path,
                                    title = videoTitle(name),
                                    folder = File(path).parent.orEmpty(),
                                    sizeBytes = attrs.size(),
                                    modifiedMs = attrs.lastModifiedTime().toMillis(),
                                ),
                            )
                        }
                        return FileVisitResult.CONTINUE
                    }

                    override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult =
                        FileVisitResult.CONTINUE
                },
            )
        }
        return found.values.sortedWith(compareBy({ it.title.lowercase() }, { it.path }))
    }
}

/** How far a video was watched. Only a meaningful middle part is worth resuming. */
data class VideoProgress(val positionMs: Long, val durationMs: Long)

private const val MIN_RESUME_MS = 5_000L
private const val END_MARGIN_MS = 10_000L

/** Where to start: the saved position when it is past the first seconds and not at the end, else the beginning. */
fun resumePositionMs(progress: VideoProgress?): Long {
    if (progress == null || progress.durationMs <= 0) return 0
    val p = progress.positionMs
    return if (p >= MIN_RESUME_MS && p <= progress.durationMs - END_MARGIN_MS) p else 0
}

/** "1.4 GB", "812 MB", "37 KB". */
fun formatSize(bytes: Long): String {
    val kb = 1024.0
    return when {
        bytes >= kb * kb * kb -> "%.1f GB".format(bytes / (kb * kb * kb))
        bytes >= kb * kb -> "%.0f MB".format(bytes / (kb * kb))
        else -> "%.0f KB".format(bytes.coerceAtLeast(0) / kb)
    }
}
