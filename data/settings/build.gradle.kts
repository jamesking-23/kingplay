plugins {
    id("kingplay.android.library")
    id("kingplay.android.hilt")
}

android {
    namespace = "com.kingplay.data.settings"
}

dependencies {
    api(projects.core.model)
    implementation(projects.core.common)
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit)
}
