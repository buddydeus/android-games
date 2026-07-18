import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.bundling.Zip

plugins {
    id("com.android.application") version "9.2.1" apply false
    id("com.android.library") version "9.2.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.0" apply false
}

fun registerGamePackageTask(taskName: String, gameDir: String) {
    val capitalized = gameDir.replaceFirstChar { it.uppercaseChar() }
    val sdkDir = System.getenv("ANDROID_HOME")
        ?: providers.gradleProperty("android.sdk.path").orNull
        ?: "${System.getProperty("user.home")}/Library/Android/sdk"
    val d8 = file("$sdkDir/build-tools/36.0.0/d8")
    val jarFile = layout.projectDirectory.file(
        "games/$gameDir/build/intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar"
    )
    val dexDir = layout.buildDirectory.dir("game-plugin-dex/$gameDir")
    val dexTask = tasks.register<Exec>("dex${capitalized}Game") {
        dependsOn(":games:$gameDir:bundleLibRuntimeToJarDebug")
        inputs.file(jarFile)
        outputs.dir(dexDir)
        doFirst {
            dexDir.get().asFile.deleteRecursively()
            dexDir.get().asFile.mkdirs()
        }
        commandLine(d8.absolutePath, "--min-api", "26", "--output", dexDir.get().asFile.absolutePath, jarFile.asFile.absolutePath)
    }
    val pluginApkTask = tasks.register<Zip>("assemble${capitalized}PluginApk") {
        dependsOn(dexTask)
        archiveFileName.set("plugin.apk")
        destinationDirectory.set(layout.buildDirectory.dir("game-plugin-apks/$gameDir"))
        from(dexDir)
    }
    tasks.register<Zip>(taskName) {
        group = "game packages"
        description = "Builds the $gameDir game package zip."
        dependsOn(pluginApkTask)
        archiveFileName.set("$gameDir.zip")
        destinationDirectory.set(layout.buildDirectory.dir("game-packages"))
        from(layout.projectDirectory.dir("games/$gameDir/package")) {
            exclude("plugin.apk")
        }
        from(pluginApkTask.flatMap { it.archiveFile })
    }
}

registerGamePackageTask("packageGomokuGame", "gomoku")
registerGamePackageTask("packageOthelloGame", "othello")
registerGamePackageTask("packageXiangqiGame", "xiangqi")
registerGamePackageTask("packageChessGame", "chess")
