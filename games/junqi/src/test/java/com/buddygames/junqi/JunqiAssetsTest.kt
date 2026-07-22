package com.buddygames.junqi

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.roundToInt
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JunqiAssetsTest {
    @Test
    fun visualConstantsRegisterTheCompleteBoardGeometry() {
        assertEquals("icon.png", JunqiVisuals.ICON_TEXTURE_PATH)
        assertEquals("board/junqi-board.png", JunqiVisuals.BOARD_TEXTURE_PATH)
        assertEquals("textures/junqi-shelf.png", JunqiVisuals.SHELF_TEXTURE_PATH)
        assertEquals(1024, JunqiVisuals.ICON_TEXTURE_WIDTH)
        assertEquals(1024, JunqiVisuals.ICON_TEXTURE_HEIGHT)
        assertEquals(1400, JunqiVisuals.BOARD_TEXTURE_WIDTH)
        assertEquals(1680, JunqiVisuals.BOARD_TEXTURE_HEIGHT)
        assertEquals(
            listOf(
                JunqiTextureContract("icon.png", 1024, 1024),
                JunqiTextureContract("board/junqi-board.png", 1400, 1680),
                JunqiTextureContract("textures/junqi-shelf.png", 1400, 360),
            ),
            JunqiVisuals.textureContracts,
        )
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
    fun visualConstantsExposeTheClassicBoardAndHighContrastFactionPieces() {
        assertEquals(0xFFE8EDE9L, JunqiVisuals.FALLBACK_CANVAS_COLOR)
        assertEquals(0xFFD8C98FL, JunqiVisuals.FALLBACK_BOARD_COLOR)
        assertEquals(0xFF494233L, JunqiVisuals.FALLBACK_ROAD_COLOR)
        assertEquals(0xFF2D2A23L, JunqiVisuals.FALLBACK_RAIL_DARK_COLOR)
        assertEquals(0xFFF1E5B9L, JunqiVisuals.FALLBACK_RAIL_LIGHT_COLOR)
        assertEquals(0xFFBBC2A6L, JunqiVisuals.FALLBACK_CAMP_COLOR)
        assertEquals(0xFFE9DCAAL, JunqiVisuals.FALLBACK_STATION_COLOR)
        assertEquals(0xFFB4312BL, JunqiVisuals.FALLBACK_HEADQUARTERS_LABEL_COLOR)
        assertEquals(0xFF68745DL, JunqiVisuals.FALLBACK_BOUNDARY_COLOR)
        assertEquals(0xFF23704BL, JunqiVisuals.GREEN_PIECE_COLOR)
        assertEquals(0xFFC65012L, JunqiVisuals.ORANGE_PIECE_COLOR)
        assertEquals(0xFFFFFFFFL, JunqiVisuals.PIECE_TEXT_COLOR)
        assertEquals(0.64f, JunqiVisuals.PIECE_LABEL_HEIGHT_FRACTION, 0f)
        assertEquals(0.06f, JunqiVisuals.PIECE_LABEL_MIN_INSET_FRACTION, 0f)
    }

    @Test
    fun packageContainsReadableCircularSafeIconBoardAndShelf() {
        val assets = repositoryRoot().resolve("games/junqi/package/assets")
        val icon = readPng(assets.resolve(JunqiVisuals.ICON_TEXTURE_PATH))
        val board = readPng(assets.resolve(JunqiVisuals.BOARD_TEXTURE_PATH))
        val shelf = readPng(assets.resolve(JunqiVisuals.SHELF_TEXTURE_PATH))

        assertImage(JunqiVisuals.ICON_TEXTURE_PATH, icon, 1024, 1024, transparentCorners = true)
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

        assertColorNear("road", board.getRGB(340, 180), ROAD_COLOR, 18)
        assertColorNear("rail dark", board.getRGB(318, 300), RAIL_DARK_COLOR, 18)
        assertColorNear("rail light", board.getRGB(350, 300), RAIL_LIGHT_COLOR, 18)
        assertColorNear("camp", board.getRGB(460, 420), CAMP_COLOR, 18)
        assertColorNear("station", board.getRGB(700, 180), STATION_COLOR, 18)
        assertColorNear("headquarters", board.getRGB(460, 200), HEADQUARTERS_COLOR, 18)
        assertColorNear("central boundary", board.getRGB(820, 840), BOUNDARY_COLOR, 18)
        assertEquals(60, JunqiVisuals.BOARD_GRID.flatten().size)
        assertEquals(10, JunqiBoard.camps.size)
        assertEquals(4, JunqiBoard.headquarters.size)
    }

    @Test
    fun packagedBoardDrawsEveryRegisteredRoadAndRailEdgeAtItsBitmapCenters() {
        val board = readBoard()
        val roadEdges = undirectedEdges(JunqiBoard.roadNeighbors)
        val railEdges = undirectedEdges(JunqiBoard.railNeighbors)

        roadEdges.forEach { edge ->
            assertEdgeColor(
                board = board,
                edge = edge,
                expectedColor = if (edge in railEdges) RAIL_DARK_COLOR else ROAD_COLOR,
                label = "road $edge"
            )
        }
        railEdges.forEach { edge ->
            assertEdgeColor(board, edge, RAIL_DARK_COLOR, "rail $edge")
        }
    }

    @Test
    fun packagedBoardRegistersEveryGridCenterAndDrawsEveryCampAndHeadquartersStyle() {
        val board = readBoard()
        val positions = (0..11).flatMap { row ->
            (0..4).map { column -> JunqiPosition(row, column) }
        }
        val centers = positions.map(JunqiVisuals::pointAt)

        assertEquals(60, positions.size)
        assertEquals(60, centers.toSet().size)
        centers.forEachIndexed { index, center ->
            assertTrue("center $index x must be in bounds", center.x >= 0f && center.x < board.width)
            assertTrue("center $index y must be in bounds", center.y >= 0f && center.y < board.height)
            assertTrue("center $index x must be integral", center.x == center.x.roundToInt().toFloat())
            assertTrue("center $index y must be integral", center.y == center.y.roundToInt().toFloat())
        }

        positions.forEach { position ->
            val expectedColor = when {
                position in JunqiBoard.camps -> CAMP_COLOR
                position in JunqiBoard.headquarters -> HEADQUARTERS_DETAIL_COLOR
                else -> STATION_COLOR
            }
            assertColorNear(
                "registered center $position",
                colorAt(board, JunqiVisuals.pointAt(position)),
                expectedColor,
                COLOR_TOLERANCE
            )
            assertRegionContainsColor(
                label = "registered label $position",
                image = board,
                center = JunqiVisuals.pointAt(position),
                expectedColor = if (position in JunqiBoard.headquarters) {
                    HEADQUARTERS_DETAIL_COLOR
                } else {
                    ROAD_COLOR
                },
                xRadius = 50,
                yRadius = 17,
            )
        }

        JunqiBoard.camps.forEach { camp ->
            val center = JunqiVisuals.pointAt(camp)
            assertColorNear("camp $camp center", colorAt(board, center), CAMP_COLOR, COLOR_TOLERANCE)
            listOf(-55 to 0, 55 to 0, 0 to -25, 0 to 25).forEach { (x, y) ->
                assertColorNear("camp $camp fill at $x,$y", colorAt(board, center, x, y), CAMP_COLOR, COLOR_TOLERANCE)
            }
            listOf(-74 to 0, 74 to 0, 0 to -40, 0 to 40).forEach { (x, y) ->
                assertColorNear("camp $camp outline at $x,$y", colorAt(board, center, x, y), CAMP_OUTLINE_COLOR, COLOR_TOLERANCE)
            }
        }

        JunqiBoard.headquarters.forEach { headquarters ->
            val center = JunqiVisuals.pointAt(headquarters)
            listOf(-20, 20).forEach { y ->
                assertColorNear(
                    "headquarters $headquarters fill at 0,$y",
                    colorAt(board, center, 0, y),
                    HEADQUARTERS_COLOR,
                    COLOR_TOLERANCE
                )
            }
            listOf(-88 to 0, 88 to 0, 0 to -36, 0 to 36).forEach { (x, y) ->
                assertColorNear(
                    "headquarters $headquarters outline at $x,$y",
                    colorAt(board, center, x, y),
                    HEADQUARTERS_OUTLINE_COLOR,
                    COLOR_TOLERANCE
                )
            }
        }
    }

    private fun readBoard(): BufferedImage = readPng(
        repositoryRoot()
            .resolve("games/junqi/package/assets")
            .resolve(JunqiVisuals.BOARD_TEXTURE_PATH)
    )

    private fun undirectedEdges(
        neighbors: Map<JunqiPosition, Set<JunqiPosition>>
    ): Set<BoardEdge> {
        val edges = linkedSetOf<BoardEdge>()
        neighbors.forEach { (first, linkedPositions) ->
            linkedPositions.forEach { second -> edges += BoardEdge.of(first, second) }
        }
        return edges
    }

    private fun assertEdgeColor(
        board: BufferedImage,
        edge: BoardEdge,
        expectedColor: Int,
        label: String
    ) {
        val first = JunqiVisuals.pointAt(edge.first)
        val second = JunqiVisuals.pointAt(edge.second)
        val deltaX = second.x - first.x
        val deltaY = second.y - first.y
        val length = sqrt(deltaX * deltaX + deltaY * deltaY)
        val normalX = -deltaY / length
        val normalY = deltaX / length

        listOf(-10f, 10f).forEach { alongOffset ->
            val midpointX = (first.x + second.x) / 2f + deltaX / length * alongOffset
            val midpointY = (first.y + second.y) / 2f + deltaY / length * alongOffset
            val hasExpectedColor = (-8..8).any { perpendicularOffset ->
                val x = (midpointX + normalX * perpendicularOffset).roundToInt()
                val y = (midpointY + normalY * perpendicularOffset).roundToInt()
                isColorNear(board.getRGB(x, y), expectedColor, COLOR_TOLERANCE)
            }
            assertTrue(
                "$label must be painted near midpoint offset $alongOffset with color ${expectedColor.toUInt().toString(16)}",
                hasExpectedColor
            )
        }
    }

    private fun colorAt(image: BufferedImage, point: JunqiBitmapPoint, xOffset: Int = 0, yOffset: Int = 0): Int =
        image.getRGB(point.x.roundToInt() + xOffset, point.y.roundToInt() + yOffset)

    private fun cardinalOffsets(distance: Int): List<Pair<Int, Int>> = listOf(
        0 to -distance,
        distance to 0,
        0 to distance,
        -distance to 0
    )

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
        assertTrue(
            "$label expected ${expected.toUInt().toString(16)} but was ${actual.toUInt().toString(16)}",
            isColorNear(actual, expected, tolerance)
        )
    }

    private fun assertRegionContainsColor(
        label: String,
        image: BufferedImage,
        center: JunqiBitmapPoint,
        expectedColor: Int,
        xRadius: Int,
        yRadius: Int,
    ) {
        val centerX = center.x.roundToInt()
        val centerY = center.y.roundToInt()
        val found = (centerY - yRadius..centerY + yRadius).any { y ->
            (centerX - xRadius..centerX + xRadius).any { x ->
                isColorNear(image.getRGB(x, y), expectedColor, COLOR_TOLERANCE)
            }
        }
        assertTrue("$label must contain ${expectedColor.toUInt().toString(16)}", found)
    }

    private fun isColorNear(actual: Int, expected: Int, tolerance: Int): Boolean {
        listOf(16, 8, 0).forEach { shift ->
            if (kotlin.math.abs((actual ushr shift and 0xff) - (expected ushr shift and 0xff)) > tolerance) {
                return false
            }
        }
        return true
    }

    private data class BoardEdge(val first: JunqiPosition, val second: JunqiPosition) {
        companion object {
            fun of(first: JunqiPosition, second: JunqiPosition): BoardEdge =
                if (first.row < second.row || first.row == second.row && first.column <= second.column) {
                    BoardEdge(first, second)
                } else {
                    BoardEdge(second, first)
                }
        }
    }

    private companion object {
        const val COLOR_TOLERANCE = 18
        const val ROAD_COLOR = 0xFF494233.toInt()
        const val RAIL_DARK_COLOR = 0xFF2D2A23.toInt()
        const val RAIL_LIGHT_COLOR = 0xFFF1E5B9.toInt()
        const val CAMP_COLOR = 0xFFBBC2A6.toInt()
        const val CAMP_OUTLINE_COLOR = 0xFF68745D.toInt()
        const val STATION_COLOR = 0xFFE9DCAA.toInt()
        const val HEADQUARTERS_COLOR = 0xFFE9DCAA.toInt()
        const val HEADQUARTERS_OUTLINE_COLOR = 0xFFB4312B.toInt()
        const val HEADQUARTERS_DETAIL_COLOR = 0xFFB4312B.toInt()
        const val BOUNDARY_COLOR = 0xFF68745D.toInt()
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
