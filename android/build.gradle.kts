plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.binary.compatibility.validator)
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.paparazzi) apply false
}

apiValidation {
    ignoredPackages.add("com.polst.sdk.internal")
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
