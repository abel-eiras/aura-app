pluginManagement {
    repositories {
        mavenCentral()
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
    }
}

rootProject.name = "aura-app"
include(":domain")

// `-Paura.domainOnly=true` compila solo el módulo Kotlin puro: útil en entornos
// sin acceso al SDK/plugin de Android (p. ej. sesiones de agente en la nube).
if (!providers.gradleProperty("aura.domainOnly").isPresent) {
    include(":app")
}
