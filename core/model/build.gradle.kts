plugins {
    id("kingplay.jvm.library")
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    api(libs.androidx.paging.common)

    testImplementation(libs.junit)
}
