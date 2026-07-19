package com.buddygames.center.ui

import com.buddygames.api.GameManifest
import com.buddygames.api.GamePackage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class HomeGameLogoTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun textLogoComesFromThePackageIconFile() {
        val game = gamePackage("word-game", "文字游戏", "assets/brand.txt")
        game.assetsDir.mkdirs()
        game.assetsDir.resolve("brand.txt").writeText("字")

        assertEquals(HomeGameLogoContent.Text("字"), loadHomeGameLogo(game))
    }

    @Test
    fun textLogoIsBoundedForTheHomeButton() {
        val game = gamePackage("long-word-game", "长名称游戏", "assets/brand.txt")
        game.assetsDir.mkdirs()
        game.assetsDir.resolve("brand.txt").writeText("TOO-LONG-WORD")

        assertEquals(HomeGameLogoContent.Text("TOO-LO"), loadHomeGameLogo(game))
    }

    @Test
    fun bitmapLogoComesFromThePackageIconFile() {
        val game = gamePackage("image-game", "图片游戏", "assets/brand.webp")
        game.assetsDir.mkdirs()
        val icon = game.assetsDir.resolve("brand.webp")
        icon.writeBytes(byteArrayOf(1, 2, 3))

        val content = loadHomeGameLogo(game)

        assertTrue(content is HomeGameLogoContent.BitmapFile)
        assertEquals(icon.canonicalFile, (content as HomeGameLogoContent.BitmapFile).file)
    }

    @Test
    fun missingOrUnsupportedIconFallsBackToPackageDisplayName() {
        val missing = gamePackage("missing", "拼图", "assets/missing.png")
        val unsupported = gamePackage("unsupported", "数独", "assets/icon.svg")
        unsupported.assetsDir.mkdirs()
        unsupported.assetsDir.resolve("icon.svg").writeText("<svg />")

        assertEquals(HomeGameLogoContent.Text("拼"), loadHomeGameLogo(missing))
        assertEquals(HomeGameLogoContent.Text("数"), loadHomeGameLogo(unsupported))
    }

    private fun gamePackage(
        gameId: String,
        displayName: String,
        iconPath: String
    ): GamePackage {
        val root = temporaryFolder.newFolder(gameId)
        return GamePackage(
            manifest = GameManifest(
                gameId = gameId,
                displayName = displayName,
                versionCode = 1,
                versionName = "0.0.1",
                entryClass = "example.Plugin",
                minShellApi = 1,
                icon = iconPath
            ),
            rootDir = root,
            pluginApk = root.resolve("plugin.apk"),
            assetsDir = root.resolve("assets")
        )
    }
}
