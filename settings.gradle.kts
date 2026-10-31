pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "UciAssistant"
include(":app")
include(":core:designsystem")
include(":feature:home")
include(":feature:protocols")
include(":feature:alerts")
include(":feature:calculators")
