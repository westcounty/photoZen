import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    FileInputStream(keystorePropertiesFile).use { stream ->
        keystoreProperties.load(stream)
    }
}

android {
    namespace = "com.example.photozen"
    compileSdk = 36

    val versionPropsFile = rootProject.file("build_version.properties")
    val versionProps = Properties()
    if (versionPropsFile.exists()) {
        FileInputStream(versionPropsFile).use { stream ->
            versionProps.load(stream)
        }
    }
    val major = versionProps["major"]?.toString()?.toInt() ?: 1
    val minor = versionProps["minor"]?.toString()?.toInt() ?: 0
    val patch = versionProps["patch"]?.toString()?.toInt() ?: 0
    val build = versionProps["build"]?.toString()?.toInt() ?: 1
    
    defaultConfig {
        applicationId = "com.example.photozen"
        minSdk = 26
        targetSdk = 36
        versionCode = major * 1000000 + minor * 10000 + patch * 100 + build
        versionName = "$major.$minor.$patch.${String.format("%03d", build)}"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Room schema export
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose BOM & UI
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.material.icons.extended)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Hilt (Dependency Injection)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)
    
    // WorkManager
    implementation(libs.work.runtime.ktx)

    // Room (Database)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.room.paging)
    ksp(libs.room.compiler)

    // Coil (Image Loading)
    implementation(libs.coil.compose)

    // DataStore (Preferences)
    implementation(libs.datastore.preferences)

    // Paging
    implementation(libs.paging.runtime)
    implementation(libs.paging.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // Serialization (for type-safe navigation)
    implementation(libs.kotlinx.serialization.json)

    // Reorderable for drag-to-reorder in LazyColumn
    implementation("sh.calvin.reorderable:reorderable:2.4.3")

    // Logging
    implementation(libs.timber)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// ==================== 自动递增 Build 号 ====================

/**
 * 自动递增 build_version.properties 中的 build 号
 * 每次执行 assembleDebug 或 assembleRelease 时自动 +1
 * 保持文件格式整洁（不使用 Properties.store 以避免时间戳和乱序）
 */
tasks.register("incrementBuildNumber") {
    doLast {
        val versionPropsFile = rootProject.file("build_version.properties")
        if (versionPropsFile.exists()) {
            val versionProps = Properties()
            FileInputStream(versionPropsFile).use { stream ->
                versionProps.load(stream)
            }

            val major = versionProps["major"]?.toString() ?: "1"
            val minor = versionProps["minor"]?.toString() ?: "0"
            val patch = versionProps["patch"]?.toString() ?: "0"
            val currentBuild = versionProps["build"]?.toString()?.toInt() ?: 0
            val newBuild = currentBuild + 1

            // 直接写入保持格式整洁
            versionPropsFile.writeText("""
                |major=$major
                |minor=$minor
                |patch=$patch
                |build=${String.format("%03d", newBuild)}
            """.trimMargin() + "\n")

            println("Build 号已递增: $currentBuild -> $newBuild")
        }
    }
}

// 让 assemble 任务依赖 incrementBuildNumber
tasks.matching { it.name.startsWith("assemble") }.configureEach {
    dependsOn("incrementBuildNumber")
}
