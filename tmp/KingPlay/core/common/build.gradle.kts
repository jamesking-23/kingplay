plugins {
    id("kingplay.android.library")
    id("kingplay.android.hilt")
}

android {
    namespace = "com.kingplay.core.common"
}

dependencies {
    api(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
