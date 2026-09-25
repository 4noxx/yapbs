import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import java.util.Properties

// Release signing - reads from keystore/keystore.properties (never committed - see keystore/README.md).
// Absent locally (e.g. CI without the file), the release build simply falls back to unsigned/debug
// signing instead of failing, so `assembleDebug` and day-to-day dev builds are unaffected.
val keystorePropertiesFile = rootProject.file("keystore/keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) keystorePropertiesFile.inputStream().use { load(it) }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "info.rbuck.billiardscoreboard"
    compileSdk = 37

    defaultConfig {
        applicationId = "info.rbuck.billiardscoreboard"
        // 28 (Android 9), not lower: from Android 9 on, Android Auto Backup is end-to-end encrypted
        // with a key derived from the device lock screen, so the player/club data this app backs up
        // is unreadable by Google. On API 26/27 that client-side encryption doesn't exist yet.
        // Raising this is a deliberate privacy decision - don't lower it without revisiting that.
        minSdk = 28
        targetSdk = 37
        versionCode = 2
        versionName = "0.1.1"
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file("app/" + keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    sourceSets["main"].kotlin.srcDirs("src/main/kotlin")
}

// AGP 9's built-in Kotlin support replaces the separate kotlin-android plugin;
// jvmTarget is configured directly on the Kotlin extension it still registers.
extensions.configure<KotlinAndroidProjectExtension> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)
    // Decodes the Aufbau_*.svg rebuild-rule diagrams bundled in assets/aufbau/ for RebuildRulesDialog.
    implementation(libs.coil.svg)
    // okhttp is the app's ONLY networking library, used solely by ObsWebSocketClient to connect out
    // to OBS. The app deliberately bundles no HTTP *server* - don't reintroduce one without
    // revisiting docs/DATENSCHUTZ.md, which states the app is not reachable over the network.
    implementation(libs.okhttp)
}
