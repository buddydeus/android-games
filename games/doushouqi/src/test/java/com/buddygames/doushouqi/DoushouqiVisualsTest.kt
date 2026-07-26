package com.buddygames.doushouqi

import org.junit.Assert.assertEquals
import org.junit.Test

class DoushouqiVisualsTest {
    @Test
    fun visualTokensMatchApprovedSsot() {
        assertEquals(0xFFE7ECE8L, DOUSHOUQI_CANVAS_ARGB)
        assertEquals(0xFFF5F6F1L, DOUSHOUQI_SURFACE_ARGB)
        assertEquals(0xFFD5B875L, DOUSHOUQI_BOARD_ARGB)
        assertEquals(0xFF315F78L, DOUSHOUQI_RIVER_ARGB)
        assertEquals(0xFF25664EL, DOUSHOUQI_GREEN_PIECE_ARGB)
        assertEquals(0xFFA64332L, DOUSHOUQI_RED_PIECE_ARGB)
        assertEquals(0xFFFFF9E8L, DOUSHOUQI_PIECE_TEXT_ARGB)
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
        assertEquals(0.78f, DOUSHOUQI_PIECE_SCALE)
        assertEquals(0.92f, DOUSHOUQI_LAST_MOVE_MARKER_SCALE)
        assertEquals(0.04f, DOUSHOUQI_LAST_MOVE_MARKER_INSET_FRACTION)
        assertEquals(0.18f, DOUSHOUQI_LAST_MOVE_MARKER_CORNER_LENGTH_FRACTION)
        assertEquals(0xB84FCBFFL, DOUSHOUQI_LAST_MOVE_MARKER_HIGHLIGHT_ARGB)
        assertEquals(0x70115C93L, DOUSHOUQI_LAST_MOVE_MARKER_SHADOW_ARGB)
    }
}
