import org.gradle.api.tasks.testing.Test

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.buddygames.xiangqi"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":game-api"))
    implementation(platform("androidx.compose:compose-bom:2026.06.01"))
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    testImplementation("junit:junit:4.13.2")
}

tasks.withType<Test>().configureEach {
    systemProperty(
        "xiangqi.calibration",
        providers.gradleProperty("xiangqiCalibration").orNull ?: "false"
    )
    providers.gradleProperty("xiangqiCalibrationPair").orNull?.let { pair ->
        systemProperty("xiangqi.calibration.pair", pair)
    }
}
