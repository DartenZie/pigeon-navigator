import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

val generatedMapAssetsDir = layout.buildDirectory.dir("generated/map-assets/androidMain")

val syncMapAssets by tasks.registering(Sync::class) {
    from(rootProject.layout.projectDirectory.dir("map-assets"))
    into(generatedMapAssetsDir)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.android)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.play.services.location)
            implementation(libs.maplibre.android.sdk)
            implementation(libs.compose.material.icons.extended)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.koin.core)
            implementation(projects.shared)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "cz.miroslavpasek.pigeonnavigator"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    androidResources {
        noCompress += "pmtiles"
    }
    sourceSets {
        getByName("main") {
            assets.setSrcDirs(listOf(generatedMapAssetsDir.get().asFile))
        }
    }
    defaultConfig {
        applicationId = "cz.miroslavpasek.pigeonnavigator"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

tasks.named("preBuild") {
    dependsOn(syncMapAssets)
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}
