dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }
}

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    plugins {
        id("com.android.application") version "7.4.2"
        id("com.android.library") version "7.4.2"
        id("org.jetbrains.kotlin.android") version "1.8.10"
    }
}

rootProject.name = "Xposed Example"

include(":app")
