// settings.gradle.kts
pluginManagement {
  repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
  }
}

plugins {
  // apply only here (not in build.gradle.kts)
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "NabiUrRahmahApp"
include(":app")
