import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

/** Release signing comes from env vars (CI) or local.properties (a developer's machine), never from the repo. */
fun signingValue(name: String): String? = System.getenv(name) ?: localProperties.getProperty(name)

// Single source of truth for the version: gradle.properties. versionCode is derived
// from it (FR-006-03) with the same formula as domain's Version.versionCode.
val auraVersionName = providers.gradleProperty("aura.versionName").get()
val auraVersionCode = run {
    val m = Regex("^(\\d+)\\.(\\d+)\\.(\\d+)(-[0-9A-Za-z.-]+)?$").matchEntire(auraVersionName)
        ?: error("aura.versionName must be semver, got '$auraVersionName'")
    val (major, minor, patch) = m.groupValues.slice(1..3).map { it.toInt() }
    require(minor in 0..99 && patch in 0..99) { "minor and patch must be 0..99" }
    major * 10_000 + minor * 100 + patch
}

android {
    namespace = "io.github.abeleiras.aura"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.abeleiras.aura"
        minSdk = 29
        targetSdk = 35
        versionCode = auraVersionCode
        versionName = auraVersionName
    }

    signingConfigs {
        val storeFile = signingValue("RELEASE_STORE_FILE")
        if (storeFile != null) {
            create("release") {
                this.storeFile = file(storeFile)
                storePassword = signingValue("RELEASE_STORE_PASSWORD")
                keyAlias = signingValue("RELEASE_KEY_ALIAS")
                keyPassword = signingValue("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Without a keystore the release stays unsigned: the release workflow fails on that (see release.yml).
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
        // Print findings in the log: CI artifacts aren't always reachable (agent sandboxes).
        textReport = true
        textOutput = file("stdout")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
