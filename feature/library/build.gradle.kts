plugins {
    id("kingplay.android.library")
    id("kingplay.android.compose")
    id("kingplay.android.hilt")
}

android {
    namespace = "com.kingplay.feature.library"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.ui)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    testImplementation(libs.junit)
}
