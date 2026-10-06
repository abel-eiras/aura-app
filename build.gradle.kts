// Kotlin plugins are declared here (apply false) so every module shares one
// classloader copy of the Kotlin Gradle plugin. The Android plugin is NOT declared
// here on purpose: that lets `-Paura.domainOnly=true` build :domain without
// resolving AGP (see settings.gradle.kts).
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
