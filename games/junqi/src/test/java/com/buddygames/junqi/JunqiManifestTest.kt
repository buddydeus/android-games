package com.buddygames.junqi

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

class JunqiManifestTest {
    @Test
    fun manifestUsesTheCurrentIndependentVersion() {
        assertEquals(0x4A_55_4E_51_49_00_00_08L, AI_PACKAGE_SALT)
        assertEquals(8, JUNQI_VERSION_CODE)
        assertEquals("0.0.8", JUNQI_VERSION_NAME)
    }

    @Test
    fun packageManifestMatchesThePluginManifest() {
        val packageManifest = StrictJsonParser.parseObject(
            String(
                Files.readAllBytes(
                    repositoryRoot().resolve("games/junqi/package/manifest.json").toPath(),
                ),
                StandardCharsets.UTF_8,
            ),
        )
        val pluginManifest = JunqiPlugin.manifest

        assertEquals(pluginManifest.schemaVersion, packageManifest.int("schemaVersion"))
        assertEquals(pluginManifest.gameId, packageManifest.string("gameId"))
        assertEquals(pluginManifest.displayName, packageManifest.string("displayName"))
        assertEquals(pluginManifest.versionCode, packageManifest.int("versionCode"))
        assertEquals(pluginManifest.versionName, packageManifest.string("versionName"))
        assertEquals(pluginManifest.entryClass, packageManifest.string("entryClass"))
        assertEquals(pluginManifest.minShellApi, packageManifest.int("minShellApi"))
        assertEquals(pluginManifest.orientation, packageManifest.string("orientation"))
        assertEquals(pluginManifest.icon, packageManifest.string("icon"))
    }

    private tailrec fun repositoryRoot(
        directory: File = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile,
    ): File {
        if (directory.resolve("settings.gradle.kts").isFile) return directory
        return repositoryRoot(
            requireNotNull(directory.parentFile) {
                ("Could not locate repository root from ${System.getProperty("user.dir")}")
            },
        )
    }
}
