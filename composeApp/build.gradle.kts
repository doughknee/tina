import java.util.Properties
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    jvmToolchain(21)

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets.all {
        languageSettings.optIn("androidx.compose.material3.ExperimentalMaterial3ExpressiveApi")
        languageSettings.optIn("androidx.compose.material3.ExperimentalMaterial3Api")
    }

    androidTarget()
    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            // already in the graph via navigation3; declared so the sheet's BackHandler resolves
            implementation(libs.compose.ui.backhandler)
            implementation(libs.navigation3.ui)
            implementation(libs.material3.adaptive)
            implementation(libs.material3.adaptive.navigation.suite)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.reorderable)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.calendar.compose)
            implementation(libs.richeditor.compose)
            implementation(libs.kotlinx.datetime)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.material.kolor)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.android)
            implementation(libs.androidx.work.runtime)
            implementation(libs.androidx.glance.appwidget)
            implementation(libs.androidx.billing)
            implementation(libs.play.review)
            // sideloaded APKs never get Play's install-time compile: this installs the merged
            // Compose baseline profiles on first launch so the JIT is not cold on every sheet
            implementation(libs.androidx.profileinstaller)
            implementation(libs.ktor.client.okhttp)
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.ktor.client.cio)
            }
        }
    }
}

android {
    namespace = "com.tina.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.peggy.app"
        minSdk = 31
        targetSdk = 36
        versionCode = 17
        versionName = "1.9.0"
        // the maintainer's own builds are Pro without a purchase: tina.proOverride=true in local.properties
        val local = Properties().apply {
            rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
        }
        buildConfigField("boolean", "PRO_OVERRIDE", (local.getProperty("tina.proOverride") == "true").toString())
    }

    signingConfigs {
        create("release") {
            val props = Properties()
            val propsFile = rootProject.file("keystore.properties")
            if (propsFile.exists()) propsFile.inputStream().use { props.load(it) }
            // CI signs from secrets; a developer machine from the gitignored properties file
            fun secret(name: String, env: String) = System.getenv(env) ?: props.getProperty(name)
            secret("storeFile", "TINA_KEYSTORE_FILE")?.let { storeFile = rootProject.file(it) }
            storePassword = secret("storePassword", "TINA_KEYSTORE_PASSWORD")
            keyAlias = secret("keyAlias", "TINA_KEY_ALIAS")
            keyPassword = secret("keyPassword", "TINA_KEY_PASSWORD")
        }
    }

    buildFeatures {
        buildConfig = true
        resValues = true
    }

    buildTypes {
        release {
            // unsigned on a machine without a keystore (CI pull requests); signed everywhere else
            signingConfigs.getByName("release").takeIf { it.storeFile != null }?.let { signingConfig = it }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            resValue("string", "app_name", "Peggy")
        }
        // release code under its own package id, so it installs next to the Play build and never
        // collides with it: `gradlew :composeApp:installDev` (ANDROID_SERIAL picks the device)
        create("dev") {
            initWith(getByName("release"))
            matchingFallbacks += "release"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            resValue("string", "app_name", "Peggy dev")
        }
        debug {
            resValue("string", "app_name", "Peggy debug")
        }
    }
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspDesktop", libs.androidx.room.compiler)
}

configurations.configureEach {
    resolutionStrategy {
        // one material3 for everything: richeditor 1.2.0 was built against this same alpha,
        // which is compiled against foundation 1.12.0-beta01 (our 1.12.0 line), so the
        // AbstractMethodError that the 1.11 alpha caused on foundation 1.12 cannot recur
        force("org.jetbrains.compose.material3:material3:${libs.versions.material3Cmp.get()}")
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

compose.resources {
    packageOfResClass = "com.tina.app.resources"
}

compose.desktop {
    application {
        mainClass = "com.tina.app.MainKt"

        buildTypes.release.proguard {
            // Room/Koin/richeditor reflection trips ProGuard; size is irrelevant for a personal app
            isEnabled.set(false)
        }

        nativeDistributions {
            targetFormats(TargetFormat.Msi)
            packageName = "Peggy"
            packageVersion = "1.8.3"
        }
    }
}
