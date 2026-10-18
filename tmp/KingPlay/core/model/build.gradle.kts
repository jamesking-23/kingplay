plugins {
    id("kingplay.jvm.library")
}

dependencies {
    api(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
}
