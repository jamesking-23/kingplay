plugins {
    id("kingplay.android.library")
    id("kingplay.android.hilt")
}

android {
    namespace = "com.kingplay.core.player"
}

dependencies {
    api(projects.core.model)
    implementation(projects.core.common)
    implementation(libs.androidx.core.ktx)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.extractor)
    api(libs.media3.session)
    implementation(libs.guava)

    testImplementation(libs.junit)
}
