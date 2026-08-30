import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.booru.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.booru.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "3.2"
    }

    signingConfigs {
        create("release") {
            val signingPropsFile = rootProject.file("signing.properties")
            val signingProps = Properties().apply {
                if (signingPropsFile.exists()) {
                    FileInputStream(signingPropsFile).use { load(it) }
                }
            }

            val keyStorePath = providers.environmentVariable("BOORU_KEYSTORE_PATH")
                .orElse(providers.gradleProperty("BOORU_KEYSTORE_PATH"))
                .orNull ?: signingProps.getProperty("BOORU_KEYSTORE_PATH") ?: "release.jks"

            val storePass = providers.environmentVariable("BOORU_KEYSTORE_PASSWORD")
                .orElse(providers.gradleProperty("BOORU_KEYSTORE_PASSWORD"))
                .orNull ?: signingProps.getProperty("BOORU_KEYSTORE_PASSWORD")

            val alias = providers.environmentVariable("BOORU_KEY_ALIAS")
                .orElse(providers.gradleProperty("BOORU_KEY_ALIAS"))
                .orNull ?: signingProps.getProperty("BOORU_KEY_ALIAS")

            val keyPass = providers.environmentVariable("BOORU_KEY_PASSWORD")
                .orElse(providers.gradleProperty("BOORU_KEY_PASSWORD"))
                .orNull ?: signingProps.getProperty("BOORU_KEY_PASSWORD")

            val isReleaseRequested = gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) }

            if (storePass != null && alias != null && keyPass != null && file(keyStorePath).exists()) {
                storeFile = file(keyStorePath)
                storePassword = storePass
                keyAlias = alias
                keyPassword = keyPass
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            } else if (isReleaseRequested) {
                val missing = mutableListOf<String>()
                if (!file(keyStorePath).exists()) missing.add("Keystore file '$keyStorePath' not found")
                if (storePass == null) missing.add("BOORU_KEYSTORE_PASSWORD is not set")
                if (alias == null) missing.add("BOORU_KEY_ALIAS is not set")
                if (keyPass == null) missing.add("BOORU_KEY_PASSWORD is not set")
                throw GradleException("Release signing credentials are missing:\n" + missing.joinToString("\n") { " - $it" } + "\nConfigure BOORU_KEYSTORE_PATH, BOORU_KEYSTORE_PASSWORD, BOORU_KEY_ALIAS, and BOORU_KEY_PASSWORD via environment variables, gradle.properties, or signing.properties.")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures { compose = true }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.05.00"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-gif:2.7.0")
    implementation("io.coil-kt:coil-video:2.7.0")
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-ui:1.5.1")
    implementation("androidx.media3:media3-datasource:1.5.1")
    implementation("androidx.media3:media3-datasource-okhttp:1.5.1")
    implementation("androidx.media3:media3-database:1.5.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.datastore:datastore-preferences:1.1.2")
    implementation("com.google.android.material:material:1.12.0")

    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    implementation("org.jsoup:jsoup:1.18.3")

    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
