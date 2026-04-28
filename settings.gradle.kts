rootProject.name = "PigeonNavigator"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":composeApp")
include(":shared")
include(":core:util")
include(":core:platform")
include(":core:database")
include(":domain")
include(":data:searchData")
project(":data:searchData").projectDir = file("data/search")
include(":data:terrainData")
project(":data:terrainData").projectDir = file("data/terrain")
include(":data:aviationData")
project(":data:aviationData").projectDir = file("data/aviation")
include(":feature:searchFeature")
project(":feature:searchFeature").projectDir = file("feature/search")
include(":feature:terrainWarningFeature")
project(":feature:terrainWarningFeature").projectDir = file("feature/terrainWarning")
include(":feature:searchDockFeature")
project(":feature:searchDockFeature").projectDir = file("feature/searchDock")
