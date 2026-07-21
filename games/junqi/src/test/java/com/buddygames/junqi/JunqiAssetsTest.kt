package com.buddygames.junqi

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JunqiAssetsTest {
    @Test
    fun visualConstantsRegisterTheCompleteBoardGeometry() {
        assertEquals("board/junqi-board.png", JunqiVisuals.BOARD_TEXTURE_PATH)
        assertEquals("textures/junqi-shelf.png", JunqiVisuals.SHELF_TEXTURE_PATH)
        assertEquals(1400, JunqiVisuals.BOARD_TEXTURE_WIDTH)
        assertEquals(1680, JunqiVisuals.BOARD_TEXTURE_HEIGHT)
        assertEquals(5, JunqiVisuals.BOARD_GRID.first().size)
        assertEquals(12, JunqiVisuals.BOARD_GRID.size)
        assertEquals(JunqiBitmapPoint(220f, 180f), JunqiVisuals.BOARD_GRID.first().first())
        assertEquals(JunqiBitmapPoint(1180f, 1500f), JunqiVisuals.BOARD_GRID.last().last())
        assertEquals(220f / 1400f, JunqiVisuals.BOARD_GRID_LEFT_FRACTION, 0.0001f)
        assertEquals(180f / 1680f, JunqiVisuals.BOARD_GRID_TOP_FRACTION, 0.0001f)
        assertEquals(1180f / 1400f, JunqiVisuals.BOARD_GRID_RIGHT_FRACTION, 0.0001f)
        assertEquals(1500f / 1680f, JunqiVisuals.BOARD_GRID_BOTTOM_FRACTION, 0.0001f)
    }

    @Test
    fun latestMoveMarkerUsesTheSharedBrightBlueFourCornerTreatment() {
        assertEquals(0.92f, JunqiVisuals.LAST_MOVE_SCALE, 0f)
        assertEquals(0.04f, JunqiVisuals.LAST_MOVE_INSET_FRACTION, 0f)
        assertEquals(0.18f, JunqiVisuals.LAST_MOVE_CORNER_LENGTH_FRACTION, 0f)
        assertEquals(0xB84FCBFFL, JunqiVisuals.LAST_MOVE_COLOR)
        assertEquals(0x70115C93L, JunqiVisuals.LAST_MOVE_SHADOW_COLOR)
    }

    @Test
    fun packageContainsReadableCircularSafeIconBoardAndShelf() {
        val assets = repositoryRoot().resolve("games/junqi/package/assets")
        val icon = readPng(assets.resolve("icon.png"))
        val board = readPng(assets.resolve(JunqiVisuals.BOARD_TEXTURE_PATH))
        val shelf = readPng(assets.resolve(JunqiVisuals.SHELF_TEXTURE_PATH))

        assertImage("icon.png", icon, 1024, 1024, transparentCorners = true)
        assertImage(
            JunqiVisuals.BOARD_TEXTURE_PATH,
            board,
            JunqiVisuals.BOARD_TEXTURE_WIDTH,
            JunqiVisuals.BOARD_TEXTURE_HEIGHT,
            transparentCorners = true
        )
        assertImage(
            JunqiVisuals.SHELF_TEXTURE_PATH,
            shelf,
            JunqiVisuals.SHELF_TEXTURE_WIDTH,
            JunqiVisuals.SHELF_TEXTURE_HEIGHT,
            transparentCorners = false
        )
    }

    @Test
    fun packagedBoardMatchesTheRegisteredRoadRailCampAndHeadquartersGraph() {
        val board = readPng(
            repositoryRoot()
                .resolve("games/junqi/package/assets")
                .resolve(JunqiVisuals.BOARD_TEXTURE_PATH)
        )

        assertColorNear("road", board.getRGB(340, 180), 0xFF536762.toInt(), 18)
        assertColorNear("rail", board.getRGB(340, 300), 0xFF214F45.toInt(), 18)
        assertColorNear("camp", board.getRGB(460, 420), 0xFFDCEAE3.toInt(), 18)
        assertColorNear("headquarters", board.getRGB(460, 200), 0xFFC9B779.toInt(), 18)
        assertColorNear("central boundary", board.getRGB(700, 840), 0xFF20312E.toInt(), 18)
        assertEquals(60, JunqiVisuals.BOARD_GRID.flatten().size)
        assertEquals(10, JunqiBoard.camps.size)
        assertEquals(4, JunqiBoard.headquarters.size)
    }

    private fun readPng(file: File): BufferedImage {
        assertTrue("${file.path} must exist", file.isFile)
        val image = ImageIO.read(file)
        assertNotNull("${file.path} must be a readable PNG", image)
        return requireNotNull(image)
    }

    private fun assertImage(
        fileName: String,
        image: BufferedImage,
        width: Int,
        height: Int,
        transparentCorners: Boolean
    ) {
        assertEquals("$fileName width", width, image.width)
        assertEquals("$fileName height", height, image.height)
        assertTrue("$fileName must have alpha", image.colorModel.hasAlpha())
        if (transparentCorners) {
            listOf(
                0 to 0,
                image.width - 1 to 0,
                0 to image.height - 1,
                image.width - 1 to image.height - 1
            ).forEach { (x, y) ->
                assertEquals("$fileName corner alpha at $x,$y", 0, image.getRGB(x, y) ushr 24)
            }
        }
    }

    private fun assertColorNear(label: String, actual: Int, expected: Int, tolerance: Int) {
        listOf(16, 8, 0).forEach { shift ->
            assertTrue(
                "$label channel $shift expected ${expected ushr shift and 0xff} but was ${actual ushr shift and 0xff}",
                kotlin.math.abs((actual ushr shift and 0xff) - (expected ushr shift and 0xff)) <= tolerance
            )
        }
    }

    private tailrec fun repositoryRoot(
        directory: File = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
    ): File {
        if (directory.resolve("settings.gradle.kts").isFile) return directory
        return repositoryRoot(
            requireNotNull(directory.parentFile) {
                ("Could not locate repository root from ${System.getProperty("user.dir")}")
            }
        )
    }
}
