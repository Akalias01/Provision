// ============================================================================
// REZON8 - App Module Build Configuration
// Bleeding Edge 2026 Standard
// ============================================================================

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.mossglen.lithos"
    compileSdk = 36  // Android 16 (VanillaIceCream) - Bleeding Edge

    defaultConfig {
        applicationId = "com.mossglen.lithos"
        minSdk = 29    // Android 10 - RenderEffect blur support
        targetSdk = 36 // Android 16
        versionCode = 3143
        versionName = "3.1.43"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // JNI Packaging for Torrent Library
    packaging {
        jniLibs {
            useLegacyPackaging = true
            pickFirsts.add("lib/arm64-v8a/libjlibtorrent.so")
            pickFirsts.add("lib/armeabi-v7a/libjlibtorrent.so")
            pickFirsts.add("lib/x86/libjlibtorrent.so")
            pickFirsts.add("lib/x86_64/libjlibtorrent.so")
        }
        resources {
            excludes.add("META-INF/DEPENDENCIES")
            excludes.add("META-INF/LICENSE")
            excludes.add("META-INF/LICENSE.txt")
            excludes.add("META-INF/license.txt")
            excludes.add("META-INF/NOTICE")
            excludes.add("META-INF/NOTICE.txt")
            excludes.add("META-INF/notice.txt")
            excludes.add("META-INF/ASL2.0")
            excludes.add("META-INF/*.kotlin_module")
            excludes.add("META-INF/INDEX.LIST")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // ========================================================================
    // COMPOSE - December 2025 Release (BOM 2025.12.00)
    // Compose 1.10, Material 3 1.4, Pausable Composition
    // ========================================================================
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.text.google.fonts)
    implementation(libs.androidx.activity.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // ========================================================================
    // NAVIGATION - Type-Safe with Kotlin Serialization
    // ========================================================================
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    // ========================================================================
    // LIFECYCLE - Latest with Compose integration
    // ========================================================================
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // ========================================================================
    // HILT - Dependency Injection with KSP (2x faster than KAPT)
    // ========================================================================
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // ========================================================================
    // MEDIA3 - The 2026 Audio Standard (v1.6.0)
    // Gapless playback, Compose UI module, improved ANR handling
    // ========================================================================
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.ui)
    // implementation(libs.media3.ui.compose)  // Enable when needed

    // ========================================================================
    // MEDIA COMPAT - Legacy Android Auto Support
    // ========================================================================
    implementation(libs.androidx.media)

    // ========================================================================
    // DATA PERSISTENCE - Room + DataStore
    // ========================================================================
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)

    // ========================================================================
    // IMAGE LOADING - Coil with Compose
    // ========================================================================
    implementation(libs.coil.compose)

    // ========================================================================
    // ANIMATION - Lottie for complex animations
    // ========================================================================
    implementation(libs.lottie.compose)

    // ========================================================================
    // GLASSMORPHISM - Haze by Chris Banes for iOS-style blur effects
    // ========================================================================
    implementation("dev.chrisbanes.haze:haze:1.0.2")

    // ========================================================================
    // NETWORKING - Retrofit + OkHttp + Gson
    // ========================================================================
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.gson)

    // ========================================================================
    // GOOGLE SERVICES - Auth + Drive API
    // ========================================================================
    implementation(libs.play.services.auth)
    implementation(libs.google.api.client.android)
    implementation(libs.google.api.services.drive)
    implementation(libs.google.http.client.gson)

    // ========================================================================
    // CLOUD STORAGE - Dropbox SDK
    // ========================================================================
    implementation(libs.dropbox.core.sdk)

    // ========================================================================
    // UTILITIES
    // ========================================================================
    implementation(libs.accompanist.permissions)
    implementation(libs.commons.compress)
    implementation(libs.jsoup)
    implementation(libs.kotlinx.coroutines.guava)
    implementation(libs.androidx.core.ktx)
    implementation("androidx.documentfile:documentfile:1.0.1")

    // ========================================================================
    // AUDIO METADATA - JAudioTagger for M4B/MP3/OGG/FLAC chapters
    // ========================================================================
    implementation("net.jthink:jaudiotagger:3.0.1")

    // ========================================================================
    // TTS - Sherpa-ONNX for Kokoro AI Voice (Official k2-fsa AAR)
    // Downloaded from: https://github.com/k2-fsa/sherpa-onnx/releases
    // Version: 1.12.20 (sherpa-onnx-1.12.20.aar in app/libs/)
    // ========================================================================
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))

    // ========================================================================
    // TORRENT - jLibTorrent
    // ========================================================================
    implementation(libs.libtorrent4j)
    implementation(libs.libtorrent4j.android.arm64)
    implementation(libs.libtorrent4j.android.arm)

    // ========================================================================
    // AUDIO PROCESSING - FFmpeg for Split/Merge operations
    // DISABLED: FFmpegKit retired Jan 2025, removed from Maven Central
    // TODO: Find replacement library (VideoKit-FFmpeg-Android or build locally)
    // ========================================================================
    // implementation("com.arthenica:ffmpeg-kit-full:6.0-2")

    // ========================================================================
    // BACKGROUND WORK - WorkManager for long-running tasks
    // ========================================================================
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // ========================================================================
    // TESTING
    // ========================================================================
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
