plugins {
    id("kingplay.android.library")
    id("kingplay.android.compose")
    id("kingplay.android.hilt")
}

android {
    namespace = "com.kingplay.feature.video"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.ui)
    // The video screen draws and controls the same player the playback service owns.
    implementation(projects.core.player)
    implementation(libs.media3.ui)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    testImplementation(libs.junit)
}
