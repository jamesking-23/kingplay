plugins {
    id("kingplay.android.library")
    id("kingplay.android.compose")
}

android {
    namespace = "com.kingplay.core.ui"
}

dependencies {
    api(projects.core.designsystem)
    api(projects.core.model)
    api(libs.androidx.lifecycle.viewmodel)
    api(libs.coil.compose)
    implementation(libs.androidx.core.ktx)
    api(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
