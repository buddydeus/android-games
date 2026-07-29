package com.buddygames.center.packages

import com.buddygames.api.CURRENT_SHELL_API
import com.buddygames.api.GameManifest
import com.buddygames.api.GamePackage
import java.io.File
import java.util.zip.ZipInputStream

/** Installs, validates, and discovers local game packages under the app's private files directory. */
class GamePackageRepository(private val filesDir: File) {
    private val gamesDir = filesDir.resolve("Games")

    /** Returns valid installed packages while ignoring incomplete or invalid directories. */
    fun discoverInstalledGames(): List<GamePackage> {
        if (!gamesDir.isDirectory) return emptyList()
        return gamesDir.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { dir -> runCatching { packageFromDir(dir) }.getOrNull() }
            ?.sortedBy { it.manifest.displayName }
            .orEmpty()
    }

    /**
     * Extracts and installs a game package archive, then removes its temporary extraction tree.
     *
     * @throws IllegalArgumentException when an archive entry escapes the extraction directory or
     * the package manifest is invalid.
     * @throws java.io.IOException when archive or package files cannot be read or written.
     */
    fun installFromZip(zipFile: File): GamePackage {
        val target = filesDir.resolve(".installing/${System.currentTimeMillis()}")
        target.deleteRecursively()
        target.mkdirs()
        return try {
            ZipInputStream(zipFile.inputStream().buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val outFile = resolvePackageEntry(target, entry.name)
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        outFile.outputStream().use { output -> zip.copyTo(output) }
                    }
                }
            }
            installFromDirectory(target)
        } finally {
            target.deleteRecursively()
        }
    }

    /**
     * Validates and installs an unpacked game package.
     *
     * Same-version packages may replace development builds; lower versions are rejected.
     *
     * @throws IllegalArgumentException when the source package is invalid.
     * @throws IllegalStateException when the package is a downgrade or cannot be installed.
     */
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
        destination.resolve("plugin.apk").setReadOnly()
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

/**
 * Resolves one archive entry and enforces that its canonical path remains inside the target tree.
 *
 * @throws IllegalArgumentException when [entryName] resolves outside [targetDirectory].
 * @throws java.io.IOException when either canonical path cannot be resolved.
 */
internal fun resolvePackageEntry(targetDirectory: File, entryName: String): File {
    val canonicalTarget = targetDirectory.canonicalFile
    val candidate = canonicalTarget.resolve(entryName).canonicalFile
    require(candidate.toPath().startsWith(canonicalTarget.toPath())) {
        "Zip entry escapes package directory"
    }
    return candidate
}

/** Minimal JSON codec for the stable local game manifest schema. */
object GameManifestJson {
    /** Parses the required game manifest fields and the optional icon path. */
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

    /** Formats a manifest using the stable schema field order used by local package tooling. */
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
