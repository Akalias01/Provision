pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // JitPack for additional libraries
        maven { url = uri("https://jitpack.io") }
        // Sherpa-ONNX repository (only for sherpa packages)
        maven {
            url = uri("https://maven.k2fsa.org/repository/maven-public/")
            content {
                includeGroupByRegex("com\\.k2fsa.*")
            }
        }
    }
}

rootProject.name = "REZON8"
include(":app")
