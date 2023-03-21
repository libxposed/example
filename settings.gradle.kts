import java.net.URI

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    plugins {
        id("com.android.application") version "7.4.2"
        id("org.jetbrains.kotlin.android") version "1.8.10"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }

    versionCatalogs {
        create("libs") {
            library("libxposed-api", "io.github.libxposed", "api").version {
                branch = "master"
            }
            library("libxposed-service", "io.github.libxposed", "service").version {
                branch = "master"
            }
        }
    }
}

sourceControl {
    gitRepository(URI.create("https://github.com/libxposed/api.git")) {
        producesModule("io.github.libxposed:api")
    }
    gitRepository(URI.create("https://github.com/libxposed/service.git")) {
        producesModule("io.github.libxposed:service")
    }
}

rootProject.name = "Xposed Example"

include(":app")
