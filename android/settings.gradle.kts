pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
    // The `libs` catalog is auto-discovered from gradle/libs.versions.toml.
    // Do not redeclare it here — Gradle 9.x rejects the duplicate `from(...)` call.
}

rootProject.name = "PolstEmbeddingAndroid"

include(":sdk", ":example")
