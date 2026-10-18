package com.kingplay.core.designsystem.theme

import androidx.compose.material.icons.materialIcon
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes

/**
 * Icons that are not in the small core icon set. Drawn here so the app does not pull in the very large
 * extended icon library. Paths are 24x24 Material outlines.
 */
object KpIcons {
    val Pause: ImageVector by lazy {
        icon("Pause") { path("M6 19h4V5H6v14zm8-14v14h4V5h-4z") }
    }

    val SkipNext: ImageVector by lazy {
        icon("SkipNext") { path("M6 18l8.5-6L6 6v12zM16 6v12h2V6h-2z") }
    }

    val SkipPrevious: ImageVector by lazy {
        icon("SkipPrevious") { path("M6 6h2v12H6zm3.5 6l8.5 6V6z") }
    }

    val Shuffle: ImageVector by lazy {
        icon("Shuffle") {
            path(
                "M10.59 9.17L5.41 4 4 5.41l5.17 5.17 1.42-1.41zM14.5 4l2.04 2.04L4 18.59 5.41 20 17.96 7.46 " +
                    "20 9.5V4h-5.5zm.33 9.41l-1.41 1.41 3.13 3.13L14.5 20H20v-5.5l-2.04 2.04-3.13-3.13z",
            )
        }
    }

    val Repeat: ImageVector by lazy {
        icon("Repeat") { path("M7 7h10v3l4-4-4-4v3H5v6h2V7zm10 10H7v-3l-4 4 4 4v-3h12v-6h-2v4z") }
    }

    val RepeatOne: ImageVector by lazy {
        icon("RepeatOne") {
            path("M7 7h10v3l4-4-4-4v3H5v6h2V7zm10 10H7v-3l-4 4 4 4v-3h12v-6h-2v4zm-4-2V9h-1l-2 1v1h1.5v4H13z")
        }
    }

    val Equalizer: ImageVector by lazy {
        icon("Equalizer") { path("M10 20h4V4h-4v16zm-6 0h4v-8H4v8zM16 9v11h4V9h-4z") }
    }

    val GraphicEq: ImageVector by lazy {
        icon("GraphicEq") { path("M7 18h2V6H7v12zm4 4h2V2h-2v20zm-8-8h2v-4H3v4zm12 4h2V6h-2v12zm4-8v4h2v-4h-2z") }
    }

    val Notes: ImageVector by lazy {
        icon("Notes") { path("M3 18h12v-2H3v2zM3 6v2h18V6H3zm0 7h18v-2H3v2z") }
    }

    val Bedtime: ImageVector by lazy {
        icon("Bedtime") {
            path(
                "M12 3c-4.97 0-9 4.03-9 9s4.03 9 9 9 9-4.03 9-9c0-.46-.04-.92-.1-1.36-.98 1.37-2.58 2.26-4.4 " +
                    "2.26-2.98 0-5.4-2.42-5.4-5.4 0-1.81.89-3.42 2.26-4.4-.44-.06-.9-.1-1.36-.1z",
            )
        }
    }

    val Movie: ImageVector by lazy {
        icon("Movie") {
            path(
                "M18 4l2 4h-3l-2-4h-2l2 4h-3l-2-4H8l2 4H7L5 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 " +
                    "2-.9 2-2V4h-4z",
            )
        }
    }

    val Subtitles: ImageVector by lazy {
        icon("Subtitles") {
            path(
                "M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zM4 12h4v2H4v-2" +
                    "zm10 6H4v-2h10v2zm6 0h-4v-2h4v2zm0-4H10v-2h10v2z",
            )
        }
    }

    val AudioTrack: ImageVector by lazy {
        icon("AudioTrack") {
            path(
                "M12 3v9.28c-.47-.17-.97-.28-1.5-.28C8.01 12 6 14.01 6 16.5S8.01 21 10.5 21c2.31 0 4.2-1.75" +
                    " 4.45-4H15V6h4V3h-7z",
            )
        }
    }

    val PictureInPicture: ImageVector by lazy {
        icon("PictureInPicture") {
            path(
                "M19 7h-8v6h8V7zm2-4H3c-1.1 0-2 .9-2 2v14c0 1.1.9 1.98 2 1.98h18c1.1 0 2-.88 2-1.98V5c0-1.1" +
                    "-.9-2-2-2zm0 16.01H3V4.98h18v14.03z",
            )
        }
    }

    val Lock: ImageVector by lazy {
        icon("Lock") {
            path(
                "M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9" +
                    " 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.7" +
                    "1 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z",
            )
        }
    }

    val ScreenRotation: ImageVector by lazy {
        icon("ScreenRotation") {
            path(
                "M16.48 2.52c3.27 1.55 5.61 4.72 5.97 8.48h1.5C23.44 4.84 18.29 0 12 0l-.66.03 3.81 3.81 1." +
                    "33-1.32zm-6.25-.77c-.59-.59-1.54-.59-2.12 0L1.75 8.11c-.59.59-.59 1.54 0 2.12l12.02 12.02c" +
                    ".59.59 1.54.59 2.12 0l6.36-6.36c.59-.59.59-1.54 0-2.12L10.23 1.75zm4.6 19.44L2.81 9.17l6.3" +
                    "6-6.36 12.02 12.02-6.36 6.36zm-7.31.29C4.25 19.94 1.91 16.76 1.55 13H.05C.56 19.16 5.71 24" +
                    " 12 24l.66-.03-3.81-3.81-1.33 1.32z",
            )
        }
    }

    val AspectRatio: ImageVector by lazy {
        icon("AspectRatio") {
            path(
                "M19 12h-2v3h-3v2h5v-5zM7 9h3V7H5v5h2V9zm14-6H3c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h18c1.1 0 2" +
                    "-.9 2-2V5c0-1.1-.9-2-2-2zm0 16.01H3V4.99h18v14.02z",
            )
        }
    }

    val QueueMusic: ImageVector by lazy {
        icon("QueueMusic") {
            path(
                "M15 6H3v2h12V6zm0 4H3v2h12v-2zM3 16h8v-2H3v2zM17 6v8.18c-.31-.11-.65-.18-1-.18-1.66 0-3 1.34-3 " +
                    "3s1.34 3 3 3 3-1.34 3-3V8h3V6h-5z",
            )
        }
    }

    val MusicNote: ImageVector by lazy {
        icon("MusicNote") {
            path("M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z")
        }
    }

    val Album: ImageVector by lazy {
        icon("Album") {
            path(
                "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 14.5c-2.49 0-4.5-2.01-4.5-4.5" +
                    "S9.51 7.5 12 7.5s4.5 2.01 4.5 4.5-2.01 4.5-4.5 4.5zm0-5.5c-.55 0-1 .45-1 1s.45 1 1 1 1-.45 " +
                    "1-1-.45-1-1-1z",
            )
        }
    }

    val Folder: ImageVector by lazy {
        icon("Folder") {
            path("M10 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z")
        }
    }

    private fun icon(name: String, block: IconScope.() -> Unit): ImageVector =
        materialIcon(name = name) {
            IconScope(this).block()
            this
        }

    private class IconScope(private val builder: ImageVector.Builder) {
        fun path(pathData: String) {
            builder.addPath(pathData = addPathNodes(pathData), fill = SolidColor(Color.Black))
        }
    }
}
