package com.buddygames.center.packages

import java.io.File
import javax.imageio.ImageIO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GamePackageLogoAssetsTest {
    @Test
    fun everyBuiltInGameSuppliesTheDeclaredSquarePngLogo() {
        GAME_IDS.forEach { gameId ->
            val packageRoot = repositoryRoot().resolve("games/$gameId/package")
            val manifest = GameManifestJson.parse(packageRoot.resolve("manifest.json").readText())

            assertEquals("$gameId must declare the package PNG", "assets/icon.png", manifest.icon)
            val icon = packageRoot.resolve(requireNotNull(manifest.icon))
            assertTrue("$gameId logo must exist", icon.isFile)
            val image = ImageIO.read(icon)
            assertNotNull("$gameId logo must be a readable PNG", image)
            assertEquals("$gameId logo width", 1024, image.width)
            assertEquals("$gameId logo height", 1024, image.height)
        }
    }

    private tailrec fun repositoryRoot(
        directory: File = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
    ): File {
        if (directory.resolve("settings.gradle.kts").isFile) return directory
        return repositoryRoot(
            requireNotNull(directory.parentFile) {
                "Could not locate the repository root from ${System.getProperty("user.dir")}"
            }
        )
    }

    private companion object {
        val GAME_IDS = listOf("gomoku", "othello", "xiangqi", "chess")
    }
}
