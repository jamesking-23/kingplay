plugins {
    id("kingplay.android.application")
    id("kingplay.android.compose")
    id("kingplay.android.hilt")
}

android {
    namespace = "com.kingplay.app"

    defaultConfig {
        applicationId = "com.kingplay.app"
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.model)
    implementation(projects.core.designsystem)
    implementation(projects.core.ui)
    implementation(projects.core.database)
    implementation(projects.data.library)
    implementation(projects.data.settings)
    implementation(projects.core.player)
    implementation(projects.feature.equalizer)
    implementation(projects.feature.library)
    implementation(projects.feature.nowplaying)
    implementation(projects.feature.settings)
    implementation(projects.feature.video)
    implementation(projects.feature.widget)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.navigation.compose)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}

dependencies {
    androidTestImplementation(libs.media3.session)
    androidTestImplementation(libs.media3.exoplayer)
    androidTestImplementation(libs.room.runtime)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation(libs.kotlinx.coroutines.android)
}
