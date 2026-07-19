package com.buddygames.chess

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChessPieceAssetsTest {
    @Test
    fun packageIncludesCompleteTransparentPieceTextureSet() {
        val assetDirectory = repositoryRoot().resolve("games/chess/package/assets/pieces")

        PIECE_FILES.forEach { fileName ->
            val file = assetDirectory.resolve(fileName)
            assertTrue("$fileName must exist", file.isFile)
            val image = ImageIO.read(file)
            assertNotNull("$fileName must be a readable PNG", image)
            requireNotNull(image)
            assertEquals("$fileName width", 1024, image.width)
            assertEquals("$fileName height", 1024, image.height)
            assertTrue("$fileName must have an alpha channel", image.colorModel.hasAlpha())
            assertTransparentCorners(fileName, image)
            assertUsefulCoverage(fileName, image)
        }
    }

    private fun assertTransparentCorners(fileName: String, image: BufferedImage) {
        val corners = listOf(
            0 to 0,
            image.width - 1 to 0,
            0 to image.height - 1,
            image.width - 1 to image.height - 1
        )
        corners.forEach { (x, y) ->
            assertEquals("$fileName corner alpha at $x,$y", 0, image.getRGB(x, y) ushr 24)
        }
    }

    private fun assertUsefulCoverage(fileName: String, image: BufferedImage) {
        var visibleSamples = 0
        var totalSamples = 0
        for (y in 0 until image.height step SAMPLE_STEP) {
            for (x in 0 until image.width step SAMPLE_STEP) {
                totalSamples++
                if (image.getRGB(x, y) ushr 24 > 32) visibleSamples++
            }
        }
        val coverage = visibleSamples.toFloat() / totalSamples
        assertTrue("$fileName coverage $coverage is too small", coverage >= MIN_COVERAGE)
        assertTrue("$fileName coverage $coverage is too large", coverage <= MAX_COVERAGE)
    }

    private tailrec fun repositoryRoot(
        directory: File = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
    ): File {
        if (directory.resolve("settings.gradle.kts").isFile) return directory
        return repositoryRoot(
            requireNotNull(directory.parentFile) {
                "Could not locate repository root from ${System.getProperty("user.dir")}"
            }
        )
    }

    private companion object {
        const val SAMPLE_STEP = 4
        const val MIN_COVERAGE = 0.10f
        const val MAX_COVERAGE = 0.55f

        val PIECE_FILES = listOf(
            "white-king.png",
            "white-queen.png",
            "white-rook.png",
            "white-bishop.png",
            "white-knight.png",
            "white-pawn.png",
            "black-king.png",
            "black-queen.png",
            "black-rook.png",
            "black-bishop.png",
            "black-knight.png",
            "black-pawn.png"
        )
    }
}
