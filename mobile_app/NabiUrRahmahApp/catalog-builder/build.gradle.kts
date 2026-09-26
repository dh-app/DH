import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

// The app's data layer is plain Kotlin, so the builder runs exactly the same
// website, YouTube and language code the phones use.
sourceSets {
    main {
        kotlin.srcDir("../app/src/main/kotlin/org/darulhuda/nabiurrahmah/data")
    }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(libs.okhttp)
    implementation(libs.jsoup)
    implementation(libs.pdfbox)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

application {
    mainClass.set("org.darulhuda.nabiurrahmah.builder.MainKt")
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir.resolve("../..")
}
