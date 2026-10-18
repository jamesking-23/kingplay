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
