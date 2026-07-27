package com.buddygames.doushouqi

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DoushouqiAssetsTest {
    @Test
    fun packageIconIsReadableCircularSafePng() {
        val image = ImageIO.read(
            repositoryRoot().resolve("games/doushouqi/package/assets/icon.png"),
        )
        assertNotNull(image)
        assertEquals(1024, image.width)
        assertEquals(1024, image.height)
        assertTrue(cornersAreTransparentOrCanvasSafe(image))
        assertTrue(subjectCoverage(image) in 0.20f..0.72f)
    }

    @Test
    fun packageOwnsReferenceMatchedBoardTexture() {
        val board = readPackagePng(DoushouqiVisuals.BOARD_TEXTURE)

        assertEquals(1400, board.width)
        assertEquals(1400, board.height)
        assertTrue(cornersAreTransparentOrCanvasSafe(board))
        assertTrue("board should visibly cover its transparent canvas", alphaCoverage(board) > 0.88f)
    }

    @Test
    fun packageOwnsCompleteTransparentPieceFamily() {
        DoushouqiVisuals.PIECE_TEXTURES.forEach { (_, path) ->
            val image = readPackagePng(path)
            assertEquals("$path width", 512, image.width)
            assertEquals("$path height", 512, image.height)
            assertEquals("$path top-left alpha", 0, image.getRGB(0, 0) ushr 24)
            assertEquals("$path top-right alpha", 0, image.getRGB(511, 0) ushr 24)
            assertEquals("$path bottom-left alpha", 0, image.getRGB(0, 511) ushr 24)
            assertEquals("$path bottom-right alpha", 0, image.getRGB(511, 511) ushr 24)
            assertTrue("$path foreground coverage", alphaCoverage(image) in 0.46f..0.82f)
        }
    }

    private fun readPackagePng(path: String): BufferedImage {
        val file = repositoryRoot().resolve("games/doushouqi/package/$path")
        assertTrue("$path must exist", file.isFile)
        val image = ImageIO.read(file)
        assertNotNull("$path must decode", image)
        return image
    }

    private fun cornersAreTransparentOrCanvasSafe(image: BufferedImage): Boolean {
        val corners = listOf(
            image.getRGB(0, 0),
            image.getRGB(image.width - 1, 0),
            image.getRGB(0, image.height - 1),
            image.getRGB(image.width - 1, image.height - 1),
        )
        if (corners.all { color -> color ushr 24 == 0 }) return true
        val reference = corners.first()
        return corners.all { color ->
            listOf(16, 8, 0).all { shift ->
                kotlin.math.abs(
                    ((reference shr shift) and 0xFF) - ((color shr shift) and 0xFF),
                ) <= 12
            }
        }
    }

    private fun subjectCoverage(image: BufferedImage): Float {
        val background = image.getRGB(0, 0)
        var subject = 0
        var samples = 0
        for (y in 0 until image.height step 8) {
            for (x in 0 until image.width step 8) {
                samples++
                val color = image.getRGB(x, y)
                val differs = listOf(16, 8, 0).any { shift ->
                    kotlin.math.abs(
                        ((background shr shift) and 0xFF) -
                            ((color shr shift) and 0xFF),
                    ) > 24
                }
                if (differs) subject++
            }
        }
        return subject.toFloat() / samples
    }

    private fun alphaCoverage(image: BufferedImage): Float {
        var opaque = 0
        var samples = 0
        for (y in 0 until image.height step 4) {
            for (x in 0 until image.width step 4) {
                samples++
                if (image.getRGB(x, y) ushr 24 > 16) opaque++
            }
        }
        return opaque.toFloat() / samples
    }

    private tailrec fun repositoryRoot(
        directory: File = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile,
    ): File {
        if (directory.resolve("settings.gradle.kts").isFile) return directory
        return repositoryRoot(requireNotNull(directory.parentFile))
    }
}
