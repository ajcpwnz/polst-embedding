plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.dokka)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.paparazzi)
    id("maven-publish")
    id("signing")
}

android {
    namespace = "com.polst.sdk"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            consumerProguardFiles("consumer-rules.pro")
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    explicitApi()
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.coroutines.android)
    implementation(libs.serialization.json)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.androidx.paging.runtime.ktx)
    implementation(libs.androidx.paging.compose)

    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.work.testing)

    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.accessibility.test.framework)
    androidTestImplementation(libs.bundles.compose.test)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.polst"
            artifactId = "sdk"
            version = file("$rootDir/VERSION").readText().trim()

            afterEvaluate {
                from(components["release"])
            }

            pom {
                name.set("PolstSDK")
                description.set("Polst Android SDK — first-party Kotlin Android library.")
                url.set("https://github.com/polst/polst-android")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("polst")
                        name.set("Polst")
                        email.set("dev@polst.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/polst/polst-android.git")
                    developerConnection.set("scm:git:ssh://github.com:polst/polst-android.git")
                    url.set("https://github.com/polst/polst-android")
                }
            }
        }
    }

    repositories {
        maven {
            name = "sonatype"
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = providers.gradleProperty("ossrhUsername").orNull
                password = providers.gradleProperty("ossrhPassword").orNull
            }
        }
    }
}

gradle.taskGraph.whenReady {
    val publishingInGraph = allTasks.any { it.name.startsWith("publish") }
    if (publishingInGraph) {
        signing {
            useInMemoryPgpKeys(
                providers.gradleProperty("signingKey").orNull,
                providers.gradleProperty("signingPassword").orNull,
            )
            sign(publishing.publications)
        }
    }
}

tasks.register("checkAarSize") {
    group = "verification"
    description = "Fails if the release AAR exceeds the 600 KB size budget."
    dependsOn("assembleRelease")
    doLast {
        val aarFile = layout.buildDirectory.file("outputs/aar/sdk-release.aar").get().asFile
        if (!aarFile.exists()) {
            throw GradleException("Release AAR not found at ${aarFile.absolutePath}")
        }
        val maxBytes = 600L * 1024L
        val actualBytes = aarFile.length()
        if (actualBytes > maxBytes) {
            throw GradleException(
                "AAR size ${actualBytes} bytes exceeds budget of ${maxBytes} bytes " +
                    "(${actualBytes - maxBytes} bytes over).",
            )
        }
        logger.lifecycle("AAR size OK: ${actualBytes} bytes (budget ${maxBytes} bytes).")
    }
}
