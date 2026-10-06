// Plugins are declared in each module (not here) so :domain can build without
// resolving the Android Gradle plugin (-Paura.domainOnly=true, see settings.gradle.kts).
// Gradle warns that the Kotlin plugin is "loaded multiple times"; that is expected here:
// putting it in the root classloader breaks AGP integration (BaseVariant not found).
