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
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.android)
            implementation(libs.androidx.work.runtime)
            implementation(libs.androidx.glance.appwidget)
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
        applicationId = "com.tina.app"
        minSdk = 31
        targetSdk = 36
        versionCode = 6
        versionName = "1.4.0"
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
            packageName = "tina"
            packageVersion = "1.4.0"
        }
    }
}
