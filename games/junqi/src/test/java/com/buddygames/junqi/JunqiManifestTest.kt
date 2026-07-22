package com.buddygames.junqi

import com.google.gson.JsonParser
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertEquals
import org.junit.Test

class JunqiManifestTest {
    @Test
    fun manifestUsesTheCurrentIndependentVersion() {
        assertEquals(0x4A_55_4E_51_49_00_00_07L, AI_PACKAGE_SALT)
        assertEquals(7, JUNQI_VERSION_CODE)
        assertEquals("0.0.7", JUNQI_VERSION_NAME)
    }

    @Test
    fun packageManifestMatchesThePluginManifest() {
        val packageManifest = JsonParser.parseString(
            String(Files.readAllBytes(Path.of("package", "manifest.json")), StandardCharsets.UTF_8),
        ).asJsonObject
        val pluginManifest = JunqiPlugin.manifest

        assertEquals(pluginManifest.gameId, packageManifest["gameId"].asString)
        assertEquals(pluginManifest.displayName, packageManifest["displayName"].asString)
        assertEquals(pluginManifest.versionCode, packageManifest["versionCode"].asInt)
        assertEquals(pluginManifest.versionName, packageManifest["versionName"].asString)
        assertEquals(pluginManifest.entryClass, packageManifest["entryClass"].asString)
        assertEquals(pluginManifest.icon, packageManifest["icon"].asString)
        assertEquals(1, packageManifest["schemaVersion"].asInt)
        assertEquals(1, packageManifest["minShellApi"].asInt)
        assertEquals("landscape", packageManifest["orientation"].asString)
    }
}
