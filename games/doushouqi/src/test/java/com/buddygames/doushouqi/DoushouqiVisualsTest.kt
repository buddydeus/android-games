package com.buddygames.doushouqi

import org.junit.Assert.assertEquals
import org.junit.Test

class DoushouqiVisualsTest {
    @Test
    fun visualTokensMatchApprovedSsot() {
        assertEquals(0xFFEDF3EFL, DOUSHOUQI_CANVAS_ARGB)
        assertEquals(0xFFF7F8F3L, DOUSHOUQI_SURFACE_ARGB)
        assertEquals(0xFFE5B85DL, DOUSHOUQI_BOARD_ARGB)
        assertEquals(0xFFF0CB7BL, DOUSHOUQI_BOARD_LIGHT_ARGB)
        assertEquals(0xFF5A3A12L, DOUSHOUQI_GRID_ARGB)
        assertEquals(0xFF075D86L, DOUSHOUQI_RIVER_ARGB)
        assertEquals(0xFF2A7898L, DOUSHOUQI_RIVER_LINE_ARGB)
        assertEquals(0xFF0E5A3AL, DOUSHOUQI_GREEN_PIECE_ARGB)
        assertEquals(0xFFC63A20L, DOUSHOUQI_RED_PIECE_ARGB)
        assertEquals(0xFFFFF3D2L, DOUSHOUQI_PIECE_TEXT_ARGB)
        assertEquals(0xFF183D30L, DOUSHOUQI_INK_ARGB)
        assertEquals(0xFF6B6D69L, DOUSHOUQI_MUTED_ARGB)
    }

    @Test
    fun layoutAndMarkerTokensMatchGameFamily() {
        assertEquals(28f, DOUSHOUQI_OUTER_PADDING_DP)
        assertEquals(34f, DOUSHOUQI_BOARD_RAIL_GAP_DP)
        assertEquals(300f, DOUSHOUQI_GAME_RAIL_WIDTH_DP)
        assertEquals(0.94f, DOUSHOUQI_GAME_RAIL_HEIGHT_FRACTION)
        assertEquals(320f, DOUSHOUQI_MENU_RAIL_WIDTH_DP)
        assertEquals(0.88f, DOUSHOUQI_MENU_RAIL_HEIGHT_FRACTION)
        assertEquals(900f, DOUSHOUQI_WIDE_BREAKPOINT_DP)
        assertEquals(8f, DOUSHOUQI_RAIL_CORNER_DP)
        assertEquals(24f, DOUSHOUQI_RAIL_HORIZONTAL_PADDING_DP)
        assertEquals(26f, DOUSHOUQI_RAIL_VERTICAL_PADDING_DP)
        assertEquals(54f, DOUSHOUQI_ACTION_HEIGHT_DP)
        assertEquals(1f, DOUSHOUQI_BOARD_ASPECT_RATIO)
        assertEquals(0.86f, DOUSHOUQI_PIECE_SCALE)
        assertEquals(0.92f, DOUSHOUQI_LAST_MOVE_MARKER_SCALE)
        assertEquals(0.04f, DOUSHOUQI_LAST_MOVE_MARKER_INSET_FRACTION)
        assertEquals(0.18f, DOUSHOUQI_LAST_MOVE_MARKER_CORNER_LENGTH_FRACTION)
        assertEquals(0xB84FCBFFL, DOUSHOUQI_LAST_MOVE_MARKER_HIGHLIGHT_ARGB)
        assertEquals(0x70115C93L, DOUSHOUQI_LAST_MOVE_MARKER_SHADOW_ARGB)
    }

    @Test
    fun packageTexturePathsCoverBoardAndEveryPiece() {
        assertEquals("assets/board/doushouqi-board.png", DoushouqiVisuals.BOARD_TEXTURE)
        assertEquals(1400, DoushouqiVisuals.BOARD_TEXTURE_SIZE)
        assertEquals(512, DoushouqiVisuals.PIECE_TEXTURE_SIZE)

        val expected = DoushouqiSide.entries.flatMap { side ->
            DoushouqiAnimal.entries.map { animal ->
                side to animal
            }
        }
        assertEquals(expected.toSet(), DoushouqiVisuals.PIECE_TEXTURES.keys)
        assertEquals(
            "assets/pieces/green-elephant.png",
            DoushouqiVisuals.pieceTexture(
                DoushouqiPiece(DoushouqiSide.PINE_GREEN, DoushouqiAnimal.ELEPHANT),
            ),
        )
        assertEquals(
            "assets/pieces/red-rat.png",
            DoushouqiVisuals.pieceTexture(
                DoushouqiPiece(DoushouqiSide.VERMILION, DoushouqiAnimal.RAT),
            ),
        )
    }
}
