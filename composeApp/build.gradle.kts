plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "cz.miroslavpasek.pigeonnavigator"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    sourceSets {
        getByName("main") {
            manifest.srcFile("src/androidMain/AndroidManifest.xml")
            kotlin.directories.add("src/androidMain/kotlin")
            res.directories.add("src/androidMain/res")
            assets.directories.add("src/androidMain/assets")
            assets.directories.add("../map-assets")
        }
    }

    androidResources {
        noCompress += "pmtiles"
        noCompress += "ofpkg"
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

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.uiToolingPreview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodelCompose)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.play.services.location)
    implementation(libs.maplibre.android.sdk)
    implementation(libs.compose.material.icons.extended)
    implementation(projects.shared)
    implementation(projects.core.platform)
    implementation(projects.domain)
    implementation(projects.data.searchData)
    implementation(projects.data.terrainData)
    implementation(projects.data.aviationData)
    implementation(projects.feature.searchFeature)
    implementation(projects.feature.searchDockFeature)
    implementation(projects.feature.terrainWarningFeature)

    debugImplementation(libs.compose.uiTooling)
}
