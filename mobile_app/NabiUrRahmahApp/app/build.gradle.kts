import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val siteUrl = providers.gradleProperty("nur.siteUrl").get().split(',').first().trim()
val publishedBase = providers.gradleProperty("nur.publishedBase").get()
val youtubePlaylists = providers.gradleProperty("nur.youtubePlaylists").getOrElse("")
fun shelf(name: String) = providers.gradleProperty("nur.shelf.$name").getOrElse("")

// Release signing is read from keystore.properties (never committed) or from
// environment variables in CI. Without either, release builds are unsigned.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use(::load)
}
fun signingValue(key: String, env: String): String? =
    keystoreProperties.getProperty(key) ?: System.getenv(env)

android {
    namespace = "org.darulhuda.nabiurrahmah"
    compileSdk = 36

    defaultConfig {
        // Kept from the published app so existing installs keep updating.
        applicationId = "org.darulhuda.udupi"
        minSdk = 24
        targetSdk = 36
        versionCode = 4
        versionName = "2.0.0"

        buildConfigField("String", "SITE_URL", "\"$siteUrl\"")
        buildConfigField("String", "PUBLISHED_CATALOG_URL", "\"${publishedBase}catalog.json\"")
        buildConfigField("String", "YOUTUBE_PLAYLISTS", "\"$youtubePlaylists\"")
        buildConfigField("String", "SHELF_BIOGRAPHY", "\"${shelf("biography")}\"")
        buildConfigField("String", "SHELF_TESTIMONIES", "\"${shelf("testimonies")}\"")
        buildConfigField("String", "SHELF_BOOKS", "\"${shelf("books")}\"")
    }

    signingConfigs {
        val storeFile = signingValue("storeFile", "NUR_KEYSTORE_FILE")
        if (storeFile != null) {
            create("release") {
                this.storeFile = file(storeFile)
                storePassword = signingValue("storePassword", "NUR_KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "NUR_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "NUR_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    lint {
        warningsAsErrors = false
        abortOnError = true
        checkDependencies = false
        // New library versions ship constantly; we update deliberately, not on lint's schedule.
        disable += listOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.jsoup)
    implementation(libs.coil.compose)
    implementation(libs.telephoto.zoomable.image.coil)
    implementation(libs.youtube.player)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
