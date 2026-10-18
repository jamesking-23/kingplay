import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(projects.core.model)
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.vlcj)
    implementation(libs.jna)
    implementation(libs.jaudiotagger)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

compose.desktop {
    application {
        mainClass = "com.kingplay.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "KingPlay"
            // Windows installers need a major version of at least 1. Keep APP_VERSION in ui/SettingsPage.kt equal to this.
            packageVersion = "1.0.0"
            description = "KingPlay music player"
            vendor = "KingPlay"
            // "Open with" / double-click in Explorer. Windows asks the person before changing a default app.
            listOf(
                Triple("audio/mpeg", "mp3", "MP3 audio"),
                Triple("audio/flac", "flac", "FLAC audio"),
                Triple("audio/mp4", "m4a", "M4A audio"),
                Triple("audio/wav", "wav", "WAV audio"),
                Triple("audio/ogg", "ogg", "Ogg audio"),
                Triple("audio/opus", "opus", "Opus audio"),
                Triple("video/mp4", "mp4", "MP4 video"),
                Triple("video/x-matroska", "mkv", "MKV video"),
            ).forEach { (mime, extension, description) -> fileAssociation(mime, extension, description) }
            windows {
                menuGroup = "KingPlay"
                shortcut = true
                perUserInstall = true
                // Keep this value fixed for the life of the app: Windows uses it to recognise upgrades.
                upgradeUuid = "6f1f2c1e-4f0a-4b52-9c63-2d8f4b7e9a10"
            }
        }
    }
}
