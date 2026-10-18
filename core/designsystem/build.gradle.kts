plugins {
    id("kingplay.android.library")
    id("kingplay.android.compose")
}

android {
    namespace = "com.kingplay.core.designsystem"
}

dependencies {
    implementation(libs.androidx.core.ktx)
}
