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
        assertEquals(1600, XIANGQI_BOARD_TEXTURE_WIDTH)
        assertEquals(1500, XIANGQI_BOARD_TEXTURE_HEIGHT)
        assertEquals(128f / 1600f, XIANGQI_GRID_LEFT_FRACTION, 0.0001f)
        assertEquals(110f / 1500f, XIANGQI_GRID_TOP_FRACTION, 0.0001f)
        assertEquals(1472f / 1600f, XIANGQI_GRID_RIGHT_FRACTION, 0.0001f)
        assertEquals(1360f / 1500f, XIANGQI_GRID_BOTTOM_FRACTION, 0.0001f)
        assertEquals(1600f / 1500f, XIANGQI_BOARD_ASPECT_RATIO, 0.0001f)
        assertEquals(0.80f, XIANGQI_PIECE_TEXTURE_SCALE, 0.0001f)
    }

    @Test
    fun bottomRowLeavesVisibleClearanceInsideTheBoardFrame() {
        val gridHeight =
            XIANGQI_BOARD_TEXTURE_HEIGHT *
                (XIANGQI_GRID_BOTTOM_FRACTION - XIANGQI_GRID_TOP_FRACTION)
        val cellHeight = gridHeight / 9f
        val bottomInset =
            XIANGQI_BOARD_TEXTURE_HEIGHT * (1f - XIANGQI_GRID_BOTTOM_FRACTION)
        val visibleClearance = bottomInset - cellHeight * XIANGQI_PIECE_TEXTURE_SCALE / 2f

        assertTrue("bottom-row pieces need clear space above the frame", visibleClearance >= 80f)
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

    @Test
    fun boardUsesTheApprovedBrightPorcelainAndCeladonPalette() {
        val board = readPng(
            repositoryRoot()
                .resolve("games/xiangqi/package/assets")
                .resolve(XIANGQI_BOARD_TEXTURE_PATH)
        )

        assertColorNear(
            "porcelain field",
            board.getRGB(750, 420),
            expected = 0xFFFAF7EF.toInt(),
            tolerance = 20
        )
        assertColorNear(
            "celadon rim",
            board.getRGB(40, 750),
            expected = 0xFFA9C8BE.toInt(),
            tolerance = 24
        )
        assertColorNear(
            "celadon river",
            board.getRGB(800, 750),
            expected = 0xFFE2ECE7.toInt(),
            tolerance = 20
        )
        assertColorNear(
            "registered top grid line",
            board.getRGB(700, 110),
            expected = 0xFF27443F.toInt(),
            tolerance = 16
        )
        assertColorNear(
            "registered bottom grid line",
            board.getRGB(700, 1360),
            expected = 0xFF27443F.toInt(),
            tolerance = 16
        )
    }

    @Test
    fun pieceTexturesPreserveCeramicGlazeAndCastShadow() {
        val pieceDirectory = repositoryRoot().resolve("games/xiangqi/package/assets/pieces")

        PIECE_FILES.forEach { fileName ->
            val image = readPng(pieceDirectory.resolve(fileName))
            val bodyLuminance = mutableListOf<Int>()
            var translucentShadowSamples = 0
            for (y in 0 until image.height step SAMPLE_STEP) {
                for (x in 0 until image.width step SAMPLE_STEP) {
                    val argb = image.getRGB(x, y)
                    val alpha = argb ushr 24 and 0xff
                    val red = argb ushr 16 and 0xff
                    val green = argb ushr 8 and 0xff
                    val blue = argb and 0xff
                    val radius = kotlin.math.hypot(x - 512.0, y - 500.0)
                    if (
                        alpha >= 245 &&
                        radius < 335.0 &&
                        maxOf(red, green, blue) - minOf(red, green, blue) < 45
                    ) {
                        bodyLuminance += (red * 3 + green * 6 + blue) / 10
                    }
                    if (alpha in 18..190 && radius in 350.0..455.0 && y > 470) {
                        translucentShadowSamples++
                    }
                }
            }

            assertTrue("$fileName must retain enough ceramic body samples", bodyLuminance.size > 900)
            assertTrue(
                "$fileName glaze needs visible highlight and shade",
                bodyLuminance.max() - bodyLuminance.min() >= 46
            )
            assertTrue(
                "$fileName needs a soft external cast shadow",
                translucentShadowSamples >= 90
            )
        }
    }

    @Test
    fun pieceTexturesShareOneCenteredDiscGeometry() {
        val pieceDirectory = repositoryRoot().resolve("games/xiangqi/package/assets/pieces")
        val bounds = PIECE_FILES.map { fileName ->
            fileName to visibleBounds(readPng(pieceDirectory.resolve(fileName)))
        }
        val reference = bounds.first().second

        bounds.forEach { (fileName, current) ->
            assertTrue(
                "$fileName left bound ${current.left} differs from ${reference.left}",
                kotlin.math.abs(current.left - reference.left) <= BOUNDS_TOLERANCE
            )
            assertTrue(
                "$fileName top bound ${current.top} differs from ${reference.top}",
                kotlin.math.abs(current.top - reference.top) <= BOUNDS_TOLERANCE
            )
            assertTrue(
                "$fileName right bound ${current.right} differs from ${reference.right}",
                kotlin.math.abs(current.right - reference.right) <= BOUNDS_TOLERANCE
            )
            assertTrue(
                "$fileName bottom bound ${current.bottom} differs from ${reference.bottom}",
                kotlin.math.abs(current.bottom - reference.bottom) <= BOUNDS_TOLERANCE
            )
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

    private fun assertColorNear(
        label: String,
        actual: Int,
        expected: Int,
        tolerance: Int
    ) {
        listOf(16, 8, 0).forEach { shift ->
            val actualChannel = actual ushr shift and 0xff
            val expectedChannel = expected ushr shift and 0xff
            assertTrue(
                "$label channel $shift expected $expectedChannel but was $actualChannel",
                kotlin.math.abs(actualChannel - expectedChannel) <= tolerance
            )
        }
    }

    private fun visibleBounds(image: BufferedImage): Bounds {
        var left = image.width
        var top = image.height
        var right = -1
        var bottom = -1
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                if (image.getRGB(x, y) ushr 24 <= ALPHA_THRESHOLD) continue
                left = minOf(left, x)
                top = minOf(top, y)
                right = maxOf(right, x)
                bottom = maxOf(bottom, y)
            }
        }
        assertTrue("texture must contain visible pixels", right >= left && bottom >= top)
        return Bounds(left, top, right, bottom)
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
        const val ALPHA_THRESHOLD = 32
        const val BOUNDS_TOLERANCE = 3

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

    private data class Bounds(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int
    )
}
