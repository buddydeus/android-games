package com.buddygames.center.packages

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class GamePackageRepositoryTest {
    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun installsEachGameIntoIndependentDirectory() {
        val repository = GamePackageRepository(temp.newFolder("files"))
        val source = temp.newFolder("gomoku-source").also {
            it.resolve("manifest.json").writeText(gomokuManifest(versionCode = 1))
            it.resolve("plugin.apk").writeText("plugin")
            it.resolve("assets").mkdir()
        }

        val installed = repository.installFromDirectory(source)

        assertEquals("gomoku", installed.manifest.gameId)
        assertTrue(installed.rootDir.path.endsWith("Games/gomoku"))
        assertTrue(installed.pluginApk.exists())
        assertFalse(installed.pluginApk.canWrite())
        assertTrue(installed.assetsDir.isDirectory)
    }

    @Test
    fun rejectsLowerVersionUpdates() {
        val repository = GamePackageRepository(temp.newFolder("files"))
        repository.installFromDirectory(packageDir("gomoku-v2", versionCode = 2))

        val result = runCatching {
            repository.installFromDirectory(packageDir("gomoku-v1", versionCode = 1))
        }

        assertTrue(result.isFailure)
        assertEquals(2, repository.discoverInstalledGames().single().manifest.versionCode)
    }

    @Test
    fun allowsSameVersionOverwriteForDevelopment() {
        val repository = GamePackageRepository(temp.newFolder("files"))
        repository.installFromDirectory(packageDir("gomoku-first", versionCode = 1, versionName = "1.0.0"))
        repository.installFromDirectory(packageDir("gomoku-second", versionCode = 1, versionName = "1.0.1-dev"))

        assertEquals("1.0.1-dev", repository.discoverInstalledGames().single().manifest.versionName)
    }

    @Test
    fun installsFromZipIntoIndependentGameDirectory() {
        val repository = GamePackageRepository(temp.newFolder("files"))
        val zip = temp.newFile("gomoku.zip")
        ZipOutputStream(zip.outputStream()).use { output ->
            output.putNextEntry(ZipEntry("manifest.json"))
            output.write(gomokuManifest(versionCode = 3, versionName = "3.0.0").toByteArray())
            output.closeEntry()
            output.putNextEntry(ZipEntry("plugin.apk"))
            output.write("real-dex-apk".toByteArray())
            output.closeEntry()
            output.putNextEntry(ZipEntry("assets/icon.txt"))
            output.write("五".toByteArray())
            output.closeEntry()
        }

        val installed = repository.installFromZip(zip)

        assertEquals("gomoku", installed.manifest.gameId)
        assertEquals("real-dex-apk", installed.pluginApk.readText())
        assertEquals("五", installed.assetsDir.resolve("icon.txt").readText())
        assertTrue(installed.rootDir.path.endsWith("Games/gomoku"))
    }

    private fun packageDir(name: String, versionCode: Int, versionName: String = "$versionCode.0.0") =
        temp.newFolder(name).also {
            it.resolve("manifest.json").writeText(gomokuManifest(versionCode, versionName))
            it.resolve("plugin.apk").writeText("plugin")
            it.resolve("assets").mkdir()
        }

    private fun gomokuManifest(versionCode: Int = 1, versionName: String = "1.0.0") = """
        {
          "schemaVersion": 1,
          "gameId": "gomoku",
          "displayName": "五子棋",
          "versionCode": $versionCode,
          "versionName": "$versionName",
          "entryClass": "com.buddygames.gomoku.GomokuPlugin",
          "minShellApi": 1,
          "orientation": "landscape",
          "icon": "assets/icon.txt"
        }
    """.trimIndent()
}
