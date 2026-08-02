plugins {
    id("com.android.application")
}

val releaseStoreFile = providers.environmentVariable("KEYSTORE_FILE").orNull
val releaseStorePassword = providers.environmentVariable("KEYSTORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("KEY_PASSWORD").orNull
val isReleaseBuild = gradle.startParameter.taskNames.any {
    it.contains("Release", ignoreCase = true)
}

if (isReleaseBuild) {
    require(!releaseStoreFile.isNullOrBlank()) { "KEYSTORE_FILE is required for release builds" }
    require(!releaseStorePassword.isNullOrBlank()) { "KEYSTORE_PASSWORD is required for release builds" }
    require(!releaseKeyAlias.isNullOrBlank()) { "KEY_ALIAS is required for release builds" }
    require(!releaseKeyPassword.isNullOrBlank()) { "KEY_PASSWORD is required for release builds" }
}

android {
    namespace = "io.github.igorcv88.appversionpatcher"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.igorcv88.appversionpatcher"
        minSdk = 27
        targetSdk = 36
        versionCode = 4
        versionName = "2.0.1"
    }

    signingConfigs {
        create("release") {
            if (!releaseStoreFile.isNullOrBlank()) {
                storeFile = file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    compileOnly("io.github.libxposed:api:101.0.0")
    implementation("io.github.libxposed:service:101.0.0")
}
