package com.buddygames.center.packages

import com.buddygames.api.CURRENT_SHELL_API
import com.buddygames.api.GameManifest
import com.buddygames.api.GamePackage
import java.io.File
import java.util.zip.ZipInputStream

class GamePackageRepository(private val filesDir: File) {
    private val gamesDir = filesDir.resolve("Games")

    fun discoverInstalledGames(): List<GamePackage> {
        if (!gamesDir.isDirectory) return emptyList()
        return gamesDir.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { dir -> runCatching { packageFromDir(dir) }.getOrNull() }
            ?.sortedBy { it.manifest.displayName }
            .orEmpty()
    }

    fun ensureBuiltInPackagesInstalled(packages: List<BuiltInPackageDefinition>) {
        packages.forEach { definition ->
            val existing = runCatching { packageFromDir(gamesDir.resolve(definition.manifest.gameId)) }.getOrNull()
            if (existing != null && existing.manifest.versionCode > definition.manifest.versionCode) {
                return@forEach
            }
            val source = filesDir.resolve(".builtin/${definition.manifest.gameId}")
            source.deleteRecursively()
            source.mkdirs()
            source.resolve("manifest.json").writeText(definition.manifestJson)
            source.resolve("plugin.apk").writeText("built-in:${definition.manifest.gameId}")
            source.resolve("assets").mkdirs()
            source.resolve("assets/icon.txt").writeText(definition.manifest.displayName)
            installFromDirectory(source)
        }
    }

    fun installFromZip(zipFile: File): GamePackage {
        val target = filesDir.resolve(".installing/${System.currentTimeMillis()}")
        target.deleteRecursively()
        target.mkdirs()
        ZipInputStream(zipFile.inputStream().buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val outFile = target.resolve(entry.name).canonicalFile
                require(outFile.path.startsWith(target.canonicalPath)) { "Zip entry escapes package directory" }
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    outFile.outputStream().use { output -> zip.copyTo(output) }
                }
            }
        }
        return installFromDirectory(target)
    }

    fun installFromDirectory(sourceDir: File): GamePackage {
        val candidate = packageFromDir(sourceDir)
        val destination = gamesDir.resolve(candidate.manifest.gameId)
        val existing = runCatching { packageFromDir(destination) }.getOrNull()
        if (existing != null && candidate.manifest.versionCode < existing.manifest.versionCode) {
            error("Refusing to downgrade ${candidate.manifest.gameId}")
        }

        val staging = gamesDir.resolve(".${candidate.manifest.gameId}.staging")
        gamesDir.mkdirs()
        staging.deleteRecursively()
        sourceDir.copyRecursively(staging, overwrite = true)
        destination.deleteRecursively()
        check(staging.renameTo(destination)) { "Failed to install ${candidate.manifest.gameId}" }
        return packageFromDir(destination)
    }

    private fun packageFromDir(dir: File): GamePackage {
        val manifestFile = dir.resolve("manifest.json")
        val manifest = GameManifestJson.parse(manifestFile.readText())
        require(manifest.isValidForShell(CURRENT_SHELL_API)) { "Invalid manifest in ${dir.path}" }
        val pluginApk = dir.resolve("plugin.apk")
        require(pluginApk.isFile) { "Missing plugin.apk for ${manifest.gameId}" }
        val assetsDir = dir.resolve("assets")
        if (!assetsDir.exists()) assetsDir.mkdirs()
        return GamePackage(manifest, dir, pluginApk, assetsDir)
    }
}

data class BuiltInPackageDefinition(
    val manifest: GameManifest,
    val manifestJson: String
)

object GameManifestJson {
    fun parse(json: String): GameManifest {
        fun string(name: String): String {
            val match = Regex(""""$name"\s*:\s*"([^"]*)"""").find(json)
            return requireNotNull(match) { "Missing $name" }.groupValues[1]
        }

        fun int(name: String): Int {
            val match = Regex(""""$name"\s*:\s*(\d+)""").find(json)
            return requireNotNull(match) { "Missing $name" }.groupValues[1].toInt()
        }

        val icon = Regex(""""icon"\s*:\s*"([^"]*)"""").find(json)?.groupValues?.get(1)
        return GameManifest(
            schemaVersion = int("schemaVersion"),
            gameId = string("gameId"),
            displayName = string("displayName"),
            versionCode = int("versionCode"),
            versionName = string("versionName"),
            entryClass = string("entryClass"),
            minShellApi = int("minShellApi"),
            orientation = string("orientation"),
            icon = icon
        )
    }

    fun format(manifest: GameManifest): String = """
        {
          "schemaVersion": ${manifest.schemaVersion},
          "gameId": "${manifest.gameId}",
          "displayName": "${manifest.displayName}",
          "versionCode": ${manifest.versionCode},
          "versionName": "${manifest.versionName}",
          "entryClass": "${manifest.entryClass}",
          "minShellApi": ${manifest.minShellApi},
          "orientation": "${manifest.orientation}",
          "icon": "${manifest.icon.orEmpty()}"
        }
    """.trimIndent()
}
