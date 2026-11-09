plugins {
    alias(libs.plugins.android.application)
}

android {
    // Phone and watch apps MUST share the same applicationId and signing key,
    // otherwise the Wear Data Layer won't deliver between them.
    namespace = "io.github.joemk88.showonwatch"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "io.github.joemk88.showonwatch"
        minSdk = 30
        targetSdk = 36
        versionCode = 3
        versionName = "1.2"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.play.services.wearable)
}
