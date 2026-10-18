plugins {
    id("kingplay.android.library")
    id("kingplay.android.compose")
    id("kingplay.android.hilt")
}

android {
    namespace = "com.kingplay.feature.nowplaying"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.ui)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    testImplementation(libs.junit)
}
