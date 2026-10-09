plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.nagmo.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nagmo.app"
        minSdk = 26
        targetSdk = 35
        // CI passes the build number so every release installs over the previous one.
        versionCode = (project.findProperty("nagmoVersionCode") as String?)?.toInt() ?: 1
        versionName = "1.0." + ((project.findProperty("nagmoVersionCode") as String?) ?: "0")
    }

    signingConfigs {
        // A fixed key so sideloaded updates install over each other. It is committed to the
        // repo, so it is NOT secret: use your own key (via the env vars) for a store release.
        create("release") {
            storeFile = file(System.getenv("NAGMO_KEYSTORE") ?: "../keystore/nagmo-release.jks")
            storePassword = System.getenv("NAGMO_KEYSTORE_PASSWORD") ?: "nagmo-sideload"
            keyAlias = System.getenv("NAGMO_KEY_ALIAS") ?: "nagmo"
            keyPassword = System.getenv("NAGMO_KEY_PASSWORD") ?: "nagmo-sideload"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    lint {
        abortOnError = false
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}
