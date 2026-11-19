plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.blasllavero.lectav"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.blasllavero.lectav"
        minSdk = 26
        targetSdk = 36
        versionCode = 33
        versionName = "0.9.15"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    buildFeatures { compose = true }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.09.00"))
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    // Apache 2.0. Local text extraction of officially downloaded PDF curricula.
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    // GPL-3.0: in-process Piper/VITS voice engine (no separate Android TTS app).
    implementation("com.github.techempower-org:VoxSherpa-TTS:v2.11.1")
    // JNI neural inference runtime required by the open engine.
    implementation("com.github.k2-fsa:sherpa-onnx:1.13.4")
    // Apache-2.0: safe streaming extraction of official .tar.bz2 voice packages.
    implementation("org.apache.commons:commons-compress:1.27.1")
    testImplementation("junit:junit:4.13.2")
}
