@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.rezon.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.rezon.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "2.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }

        // Enable multidex for large dependency set
        multiDexEnabled = true
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        // Enable desugaring for Java 8+ APIs on older Android versions
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE*"
            excludes += "/META-INF/NOTICE*"
        }
    }

    // Split APKs by ABI for smaller download sizes
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }
}

dependencies {
    // Core Library Desugaring
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // ==================== CORE ====================
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.splashscreen)
    implementation("androidx.documentfile:documentfile:1.0.1")

    // ==================== JETPACK COMPOSE ====================
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // ==================== MEDIA3 (EXOPLAYER) - AUDIO ENGINE ====================
    implementation(libs.bundles.media3)
    implementation(libs.androidx.media3.effect)

    // ==================== ROOM DATABASE ====================
    implementation(libs.bundles.room)
    ksp(libs.androidx.room.compiler)

    // ==================== HILT (DEPENDENCY INJECTION) ====================
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // ==================== NETWORKING ====================
    implementation(libs.bundles.networking)
    ksp(libs.moshi.kotlin)

    // ==================== IMAGE LOADING ====================
    implementation(libs.coil.compose)

    // ==================== CLOUD APIS ====================
    // Google Drive
    implementation(libs.google.api.client)
    implementation(libs.google.drive.api)
    // Dropbox
    implementation(libs.dropbox.sdk)

    // ==================== TORRENT ENGINE ====================
    implementation(libs.bundles.libtorrent)

    // ==================== DOCUMENT PARSING ====================
    implementation(libs.pdfbox.android)
    // TODO: Add EPUB parser (epublib not on Maven, will use alternative)

    // ==================== AUDIO METADATA ====================
    implementation(libs.jaudiotagger)

    // ==================== DATASTORE ====================
    implementation(libs.datastore.preferences)

    // ==================== COROUTINES ====================
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // ==================== ACCOMPANIST ====================
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.accompanist.permissions)

    // ==================== TESTING ====================
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test)
}
