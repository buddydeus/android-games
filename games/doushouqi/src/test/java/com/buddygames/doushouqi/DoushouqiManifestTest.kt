package com.buddygames.doushouqi

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

class DoushouqiManifestTest {
    @Test
    fun packageIdentityMatchesReferenceArtworkRelease() {
        val manifest = DoushouqiManifest.gameManifest

        assertEquals("doushouqi", manifest.gameId)
        assertEquals("斗兽棋", manifest.displayName)
        assertEquals(4, manifest.versionCode)
        assertEquals("0.0.4", manifest.versionName)
        assertEquals("com.buddygames.doushouqi.DoushouqiPlugin", manifest.entryClass)
        assertEquals("assets/icon.png", manifest.icon)
    }

    @Test
    fun jsonManifestMatchesCodeManifest() {
        val json = StrictJsonParser.parseObject(
            repositoryRoot().resolve("games/doushouqi/package/manifest.json").readText(),
        )
        val code = DoushouqiManifest.gameManifest

        assertEquals(code.schemaVersion, json.int("schemaVersion"))
        assertEquals(code.gameId, json.string("gameId"))
        assertEquals(code.displayName, json.string("displayName"))
        assertEquals(code.versionCode, json.int("versionCode"))
        assertEquals(code.versionName, json.string("versionName"))
        assertEquals(code.entryClass, json.string("entryClass"))
        assertEquals(code.minShellApi, json.int("minShellApi"))
        assertEquals(code.orientation, json.string("orientation"))
        assertEquals(code.icon, json.string("icon"))
    }

    private tailrec fun repositoryRoot(
        directory: File = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile,
    ): File {
        if (directory.resolve("settings.gradle.kts").isFile) return directory
        return repositoryRoot(
            requireNotNull(directory.parentFile) {
                "Could not locate repository root from ${System.getProperty("user.dir")}"
            },
        )
    }
}
