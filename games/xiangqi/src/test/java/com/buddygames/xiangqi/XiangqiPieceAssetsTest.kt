package com.buddygames.xiangqi

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class XiangqiPieceAssetsTest {
    @Test
    fun everyPieceMapsToItsPackageOwnedTexture() {
        val expected = mapOf(
            XiangqiPiece(Side.RED, PieceType.GENERAL) to "pieces/red-general.png",
            XiangqiPiece(Side.RED, PieceType.ROOK) to "pieces/red-rook.png",
            XiangqiPiece(Side.RED, PieceType.HORSE) to "pieces/red-horse.png",
            XiangqiPiece(Side.RED, PieceType.CANNON) to "pieces/red-cannon.png",
            XiangqiPiece(Side.RED, PieceType.ELEPHANT) to "pieces/red-elephant.png",
            XiangqiPiece(Side.RED, PieceType.ADVISOR) to "pieces/red-advisor.png",
            XiangqiPiece(Side.RED, PieceType.SOLDIER) to "pieces/red-soldier.png",
            XiangqiPiece(Side.BLACK, PieceType.GENERAL) to "pieces/black-general.png",
            XiangqiPiece(Side.BLACK, PieceType.ROOK) to "pieces/black-rook.png",
            XiangqiPiece(Side.BLACK, PieceType.HORSE) to "pieces/black-horse.png",
            XiangqiPiece(Side.BLACK, PieceType.CANNON) to "pieces/black-cannon.png",
            XiangqiPiece(Side.BLACK, PieceType.ELEPHANT) to "pieces/black-elephant.png",
            XiangqiPiece(Side.BLACK, PieceType.ADVISOR) to "pieces/black-advisor.png",
            XiangqiPiece(Side.BLACK, PieceType.SOLDIER) to "pieces/black-soldier.png"
        )

        assertEquals(expected, xiangqiPieceTexturePaths())
    }

    @Test
    fun boardTextureOwnsTheCompleteGridGeometry() {
        assertEquals("board/xiangqi-board.png", XIANGQI_BOARD_TEXTURE_PATH)
        assertEquals(1440, XIANGQI_BOARD_TEXTURE_WIDTH)
        assertEquals(1600, XIANGQI_BOARD_TEXTURE_HEIGHT)
        assertEquals(192f / 1440f, XIANGQI_GRID_LEFT_FRACTION, 0.0001f)
        assertEquals(190f / 1600f, XIANGQI_GRID_TOP_FRACTION, 0.0001f)
        assertEquals(1248f / 1440f, XIANGQI_GRID_RIGHT_FRACTION, 0.0001f)
        assertEquals(1378f / 1600f, XIANGQI_GRID_BOTTOM_FRACTION, 0.0001f)
        assertEquals(0.90f, XIANGQI_BOARD_ASPECT_RATIO, 0.0001f)
        assertEquals(0.78f, XIANGQI_PIECE_TEXTURE_SCALE, 0.0001f)
    }

    @Test
    fun packageIncludesReadableBoardAndTransparentPieceTextures() {
        val assetDirectory = repositoryRoot().resolve("games/xiangqi/package/assets")
        val board = readPng(assetDirectory.resolve(XIANGQI_BOARD_TEXTURE_PATH))
        assertEquals(XIANGQI_BOARD_TEXTURE_WIDTH, board.width)
        assertEquals(XIANGQI_BOARD_TEXTURE_HEIGHT, board.height)
        assertTrue("board must have alpha", board.colorModel.hasAlpha())
        assertTransparentCorners("xiangqi-board.png", board)

        PIECE_FILES.forEach { fileName ->
            val image = readPng(assetDirectory.resolve("pieces/$fileName"))
            assertEquals("$fileName width", 1024, image.width)
            assertEquals("$fileName height", 1024, image.height)
            assertTrue("$fileName must have alpha", image.colorModel.hasAlpha())
            assertTransparentCorners(fileName, image)
            assertUsefulCoverage(fileName, image)
        }
    }

    private fun readPng(file: File): BufferedImage {
        assertTrue("${file.path} must exist", file.isFile)
        val image = ImageIO.read(file)
        assertNotNull("${file.path} must be a readable PNG", image)
        return requireNotNull(image)
    }

    private fun assertTransparentCorners(fileName: String, image: BufferedImage) {
        listOf(
            0 to 0,
            image.width - 1 to 0,
            0 to image.height - 1,
            image.width - 1 to image.height - 1
        ).forEach { (x, y) ->
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
        const val MIN_COVERAGE = 0.25f
        const val MAX_COVERAGE = 0.65f

        val PIECE_FILES = listOf(
            "red-general.png",
            "red-rook.png",
            "red-horse.png",
            "red-cannon.png",
            "red-elephant.png",
            "red-advisor.png",
            "red-soldier.png",
            "black-general.png",
            "black-rook.png",
            "black-horse.png",
            "black-cannon.png",
            "black-elephant.png",
            "black-advisor.png",
            "black-soldier.png"
        )
    }
}
