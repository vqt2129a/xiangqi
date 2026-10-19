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

rootProject.name = "Xiangqi"

// Tầng 4 → 1, phụ thuộc chỉ hướng vào trong:
// app → infrastructure → application → domain
include(":domain")
include(":application")
include(":infrastructure:ai")
include(":infrastructure:persistence")
include(":infrastructure:firebase")
include(":app")
