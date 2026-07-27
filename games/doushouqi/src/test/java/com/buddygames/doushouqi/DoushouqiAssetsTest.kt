package com.buddygames.doushouqi

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

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
    fun packageBoardUsesOctagonalHuntingNetTrapEmblems() {
        val board = readPackagePng(DoushouqiVisuals.BOARD_TEXTURE)
        trapCells.forEach { (row, column) ->
            val centerX = (gridLeft + (column + 0.5f) * gridWidth / 7f).roundToInt()
            val centerY = (gridTop + (row + 0.5f) * gridHeight / 9f).roundToInt()
            val shortEdge = gridHeight / 9f

            assertRingHasEightDarkSectors(
                board = board,
                centerX = centerX,
                centerY = centerY,
                radius = shortEdge * 0.34f,
                angleOffsetDegrees = 22.5f,
                label = "trap ($row,$column) outer octagon",
            )
            assertRingHasEightDarkSectors(
                board = board,
                centerX = centerX,
                centerY = centerY,
                radius = shortEdge * 0.28f,
                angleOffsetDegrees = 22.5f,
                label = "trap ($row,$column) inner octagon",
            )
            assertRingHasEightDarkSectors(
                board = board,
                centerX = centerX,
                centerY = centerY,
                radius = shortEdge * 0.20f,
                angleOffsetDegrees = 0f,
                label = "trap ($row,$column) radial net",
            )
            assertTrue(
                "trap ($row,$column) center knot",
                hasDarkPixelNear(board, centerX, centerY, radius = 7),
            )

            val washPixel = board.getRGB(
                centerX + (shortEdge * 0.25f).roundToInt(),
                centerY + (shortEdge * 0.11f).roundToInt(),
            )
            val landPixel = board.getRGB(
                (gridLeft + (0.5f) * gridWidth / 7f).roundToInt(),
                (gridTop + (2.5f) * gridHeight / 9f).roundToInt(),
            )
            assertTrue(
                "trap ($row,$column) needs a visible ochre wash",
                colorDistance(washPixel, landPixel) >= 18,
            )
        }
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

    private fun assertRingHasEightDarkSectors(
        board: BufferedImage,
        centerX: Int,
        centerY: Int,
        radius: Float,
        angleOffsetDegrees: Float,
        label: String,
    ) {
        repeat(8) { sector ->
            val angle = (angleOffsetDegrees + sector * 45f) * PI / 180.0
            val x = centerX + (cos(angle) * radius).roundToInt()
            val y = centerY + (sin(angle) * radius).roundToInt()
            assertTrue(label, hasDarkPixelNear(board, x, y, radius = 4))
        }
    }

    private fun hasDarkPixelNear(
        image: BufferedImage,
        centerX: Int,
        centerY: Int,
        radius: Int,
    ): Boolean =
        ((centerY - radius)..(centerY + radius)).any { y ->
            ((centerX - radius)..(centerX + radius)).any { x ->
                isDarkWood(image.getRGB(x, y))
            }
        }

    private fun isDarkWood(color: Int): Boolean {
        val alpha = color ushr 24
        val red = color shr 16 and 0xFF
        val green = color shr 8 and 0xFF
        val blue = color and 0xFF
        return alpha > 200 && red < 145 && green < 110 && blue < 70
    }

    private fun colorDistance(first: Int, second: Int): Int =
        listOf(16, 8, 0).sumOf { shift ->
            kotlin.math.abs(
                ((first shr shift) and 0xFF) - ((second shr shift) and 0xFF),
            )
        }

    private tailrec fun repositoryRoot(
        directory: File = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile,
    ): File {
        if (directory.resolve("settings.gradle.kts").isFile) return directory
        return repositoryRoot(requireNotNull(directory.parentFile))
    }

    private companion object {
        const val gridLeft = 64
        const val gridTop = 64
        const val gridWidth = 1272
        const val gridHeight = 1272

        val trapCells = listOf(
            0 to 2,
            0 to 4,
            1 to 3,
            8 to 2,
            8 to 4,
            7 to 3,
        )
    }
}
