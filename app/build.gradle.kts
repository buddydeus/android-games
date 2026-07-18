import org.gradle.api.tasks.Copy

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.buddygames.center"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.buddygames.center"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.0.1"
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(project(":game-api"))
    implementation(platform("androidx.compose:compose-bom:2026.06.01"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}

val builtinAssetsDir = layout.buildDirectory.asFile.get().resolve("generated/assets")
val copyBuiltinGamePackages = tasks.register<Copy>("copyBuiltinGamePackages") {
    dependsOn(
        rootProject.tasks.named("packageGomokuGame"),
        rootProject.tasks.named("packageOthelloGame"),
        rootProject.tasks.named("packageXiangqiGame")
    )
    from(rootProject.layout.buildDirectory.dir("game-packages")) {
        include("*.zip")
    }
    into(builtinAssetsDir.resolve("builtin-games"))
}

android.sourceSets["main"].assets.srcDir(builtinAssetsDir)

tasks.matching { it.name == "mergeDebugAssets" || it.name == "mergeReleaseAssets" }.configureEach {
    dependsOn(copyBuiltinGamePackages)
}
