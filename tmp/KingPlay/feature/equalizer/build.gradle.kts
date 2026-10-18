plugins {
    id("kingplay.android.library")
    id("kingplay.android.compose")
    id("kingplay.android.hilt")
}

android {
    namespace = "com.kingplay.feature.equalizer"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.ui)
    // The response curve uses the same filter math the audio engine runs, so the picture cannot drift from the sound.
    implementation(projects.core.player)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    testImplementation(libs.junit)
}
