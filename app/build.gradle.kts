import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Play upload key (keystore.properties at the project root, never committed). Without it the
// release build is unsigned, which is fine for everything except uploading to Play.
val uploadKey = rootProject.file("keystore.properties").takeIf { it.exists() }?.let { f ->
    Properties().apply { f.inputStream().use { load(it) } }
}

// Firebase config (Google sign-in now, order alerts later). The build still works without it;
// Google sign-in then explains it isn't set up (handoff §8 item 2).
if (file("google-services.json").exists()) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
}

// Release builds talk to the production backend on Railway; -Pst.prodBaseUrl=… overrides it.
val prodBaseUrl = (findProperty("st.prodBaseUrl") as String?) ?: "https://sanathanatattava-production-67b1.up.railway.app/"
// Debug builds use the testing server; -Pst.debugBaseUrl=http://localhost:5001/ points them at
// a backend on this computer (with `adb reverse tcp:5001 tcp:5001`).
val debugBaseUrl = (findProperty("st.debugBaseUrl") as String?) ?: "https://sanathanatattava-testing.up.railway.app/"

android {
    namespace = "com.sanathanatattva.partner"
    compileSdk {
        // 37: current AndroidX/Compose need it. targetSdk stays 36.
        version = release(37)
    }

    defaultConfig {
        // Permanent: the app is registered on Play under this ID ("ST Delivery", 2026-10-19).
        // The Kotlin namespace stays com.sanathanatattva.partner; only the store ID differs.
        applicationId = "com.sanathanatattva.delivery"
        // 26: notification channels and full-screen alerts are core to the app.
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "0.1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    androidResources {
        localeFilters += listOf("en", "kn")
        generateLocaleConfig = true
    }

    signingConfigs {
        if (uploadKey != null) {
            create("upload") {
                storeFile = file(uploadKey.getProperty("storeFile"))
                storePassword = uploadKey.getProperty("storePassword")
                keyAlias = uploadKey.getProperty("keyAlias")
                keyPassword = uploadKey.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"$debugBaseUrl\"")
        }
        release {
            buildConfigField("String", "BASE_URL", "\"$prodBaseUrl\"")
            if (uploadKey != null) signingConfig = signingConfigs.getByName("upload")
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        // Most screens are English-only until the Kannada pass (ST_PARTNER_HANDOFF.md §11).
        // Keep the gap visible in every lint report without blocking builds.
        warning += "MissingTranslation"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp.logging)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.messaging)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.exifinterface)
    implementation(libs.play.services.location)
    implementation(libs.maplibre.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
