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

    androidTarget()
    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.navigation3.ui)
            implementation(libs.material3.adaptive)
            implementation(libs.material3.adaptive.navigation.suite)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.reorderable)
            implementation(libs.kotlinx.serialization.json)
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
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.android)
            implementation(libs.androidx.work.runtime)
            implementation(libs.androidx.glance.appwidget)
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
    }
}

android {
    namespace = "com.tina.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.tina.app"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        create("release") {
            val props = Properties()
            val propsFile = rootProject.file("keystore.properties")
            if (propsFile.exists()) {
                propsFile.inputStream().use { props.load(it) }
                storeFile = rootProject.file(props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspDesktop", libs.androidx.room.compiler)
}

configurations.configureEach {
    resolutionStrategy {
        // richeditor 1.1.0 pulls material3 1.11.0-alpha07, whose Android mapping is
        // compiled against foundation 1.11.0-beta02 and crashes (AbstractMethodError in
        // TextFieldDefaults) on foundation 1.12.0. Pin the stable CMP 1.12 pairing.
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
            packageName = "tina"
            packageVersion = "1.0.0"
        }
    }
}
