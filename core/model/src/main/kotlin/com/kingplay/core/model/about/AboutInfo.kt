package com.kingplay.core.model.about

import java.security.SecureRandom
import java.time.Year
import java.util.Base64
import java.util.Random

/** A library or program KingPlay uses, with the licence it comes under. */
data class ThirdPartyItem(val name: String, val license: String, val url: String, val platforms: Set<Platform>)

enum class Platform { ANDROID, DESKTOP }

/** The words and links on the About screen, shared by the phone and PC apps so they always agree. */
object AboutInfo {
    const val PRODUCT = "KingPlay"
    const val COMPANY = "KKINGG Corporation"
    const val FEEDBACK_URL = "https://github.com/jamesking-23/kingplay/issues"
    const val HELP_URL = "https://github.com/jamesking-23/kingplay#readme"
    const val DESKTOP_HELP_URL = "https://github.com/jamesking-23/kingplay/blob/main/docs/DESKTOP_USER_GUIDE.md"

    private const val ID_BYTES = 16

    fun copyright(year: Int = Year.now().value): String = "© $year $COMPANY. All rights reserved."

    fun helpUrl(platform: Platform): String = if (platform == Platform.DESKTOP) DESKTOP_HELP_URL else HELP_URL

    /** A random label for this run of the app, like "vHM1KW8ePEKsbO9zWFAztw.0". It says nothing about the person. */
    fun newSessionId(random: Random = SecureRandom()): String {
        val bytes = ByteArray(ID_BYTES).also(random::nextBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes) + ".0"
    }

    val licenseTerms: String = """
        KingPlay licence terms

        KingPlay is provided by $COMPANY. These terms apply when you install or use KingPlay.

        1. Use. You may install and use KingPlay on your own devices for your own listening and viewing.
        2. Your files. KingPlay only reads the music and video files you point it at. It does not change or delete them.
        3. Ownership. $COMPANY keeps all rights in KingPlay, including its name, design and code, except for
           the third-party software listed under "Third-party software acknowledgements", which stays under
           its own licences.
        4. No copying or selling. You may not sell, rent or redistribute KingPlay, or remove its notices.
        5. No warranty. KingPlay is provided "as is", without any promise that it will be free of faults or suit a
           particular purpose.
        6. Liability. To the extent the law allows, $COMPANY is not liable for loss of files, data or profit
           arising from use of KingPlay. Keep your own backups.
        7. Changes. These terms may change in a new version of KingPlay. Continuing to use it means you accept them.
    """.trimIndent()

    val privacyPolicy: String = """
        KingPlay privacy policy

        KingPlay is built to keep your music and your data on your own device.

        What stays on your device: your library, playlists, favourites, play counts, lyrics, equalizer
        presets, settings, your nickname and profile picture, your playback records and where you stopped in a
        video. On a PC these are kept in your user folder, and on
        a phone in the app's own storage.

        What KingPlay sends: nothing about you. It has no account, and it does not upload your music,
        playlists or listening history. It does not include advertising or tracking. Any usage statistics
        would be off unless you choose to turn them on.

        The lyrics browser (Android): "Find lyrics online" opens a web page inside the app, and the page you visit
        sees your connection like any browser. KingPlay does not read the page; it only reads the text you copy and
        tap Add on, and saves it on your device. The app asks for the Internet permission only for this.

        Backups: when you export a backup, it is saved where you choose. Anyone you give that file to can
        read your playlist and favourite names, so share it with care.

        Links: Help and Feedback open in your web browser. Those websites have their own privacy policies.

        Questions: use the Feedback link in About.
    """.trimIndent()

    private val both = arrayOf(Platform.ANDROID, Platform.DESKTOP)
    private const val APACHE = "Apache License 2.0"

    private fun item(name: String, license: String, url: String, vararg on: Platform) =
        ThirdPartyItem(name, license, url, on.toSet())

    private const val OFL = "SIL Open Font License 1.1"

    val thirdParty: List<ThirdPartyItem> = listOf(
        item("Poppins font", OFL, "https://github.com/itfoundry/poppins", Platform.ANDROID),
        item("Hanken Grotesk font", OFL, "https://github.com/marcologous/hanken-grotesk", Platform.ANDROID),
        item("Source Sans and Source Serif fonts", OFL, "https://github.com/adobe-fonts", Platform.ANDROID),
        item("Lora font", OFL, "https://github.com/cyrealtype/Lora-Cyrillic", Platform.ANDROID),
        item("Fredoka font", OFL, "https://github.com/hafontia-zz/Fredoka-One", Platform.ANDROID),
        item("Caveat font", OFL, "https://github.com/googlefonts/caveat", Platform.ANDROID),
        item("Architects Daughter font", OFL, "https://fonts.google.com/specimen/Architects+Daughter", Platform.ANDROID),
        item("Kotlin and kotlinx.coroutines", APACHE, "https://kotlinlang.org", *both),
        item("Jetpack Compose and AndroidX", APACHE, "https://developer.android.com/jetpack", Platform.ANDROID),
        item("Media3 (ExoPlayer)", APACHE, "https://developer.android.com/media/media3", Platform.ANDROID),
        item("Room", APACHE, "https://developer.android.com/training/data-storage/room", Platform.ANDROID),
        item("Hilt and Dagger", APACHE, "https://dagger.dev", Platform.ANDROID),
        item("Coil", APACHE, "https://coil-kt.github.io/coil", Platform.ANDROID),
        item("Compose Multiplatform", APACHE, "https://www.jetbrains.com/compose-multiplatform", Platform.DESKTOP),
        item("VLC media player (libVLC)", "GNU LGPL 2.1 or later", "https://www.videolan.org/vlc", Platform.DESKTOP),
        item("vlcj", "GNU GPL 3.0", "https://github.com/caprica/vlcj", Platform.DESKTOP),
        item("JAudiotagger", "GNU LGPL 2.1", "https://bitbucket.org/ijabz/jaudiotagger", Platform.DESKTOP),
        item(
            "JNA (Java Native Access)", "$APACHE or GNU LGPL 2.1",
            "https://github.com/java-native-access/jna", Platform.DESKTOP,
        ),
    )

    /** The acknowledgements page for one platform, sorted by name. */
    fun acknowledgements(platform: Platform): String = buildString {
        appendLine("Third-party software acknowledgements")
        appendLine()
        appendLine("$PRODUCT uses the following software. Each item stays under its own licence.")
        thirdParty.filter { platform in it.platforms }.sortedBy { it.name.lowercase() }.forEach {
            appendLine()
            appendLine(it.name)
            appendLine("  ${it.license}")
            appendLine("  ${it.url}")
        }
    }.trimEnd()
}
