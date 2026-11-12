plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val releaseKeystorePath = providers.environmentVariable("ANDROID_KEYSTORE_FILE").orNull
val releaseStorePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD").orNull
val hasReleaseSigning = listOf(
    releaseKeystorePath,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }

android {
    namespace = "dev.still.scriptures"
    compileSdk = 35
    defaultConfig {
        applicationId = "dev.still.scriptures"
        minSdk = 30
        targetSdk = 35
        versionCode = 7
        versionName = "0.6.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseKeystorePath!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }
    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Local builds retain the debug certificate when no release key is
            // configured. Tagged release workflows require the stable release key.
            signingConfig = if (providers.environmentVariable("RELEASE_UNSIGNED").orNull == "true") {
                null // CI builds without a signing key; isolated job signs the APK.
            } else if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
dependencies {
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui:1.8.2")
    implementation("androidx.compose.foundation:foundation:1.8.2")
    implementation("androidx.wear.compose:compose-material:1.5.0")
    implementation("androidx.wear.compose:compose-foundation:1.5.0")
    implementation("androidx.wear.compose:compose-navigation:1.5.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jsoup:jsoup:1.18.3")
    implementation("com.google.zxing:core:3.5.3")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.8.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.8.2")
}
