package com.buddygames.junqi

internal data class JunqiBitmapPoint(val x: Float, val y: Float)

/** Package-local bitmap registration and Compose fallback tokens for the Junqi board. */
internal object JunqiVisuals {
    const val ICON_TEXTURE_PATH = "icon.png"
    const val BOARD_TEXTURE_PATH = "board/junqi-board.png"
    const val SHELF_TEXTURE_PATH = "textures/junqi-shelf.png"

    const val BOARD_TEXTURE_WIDTH = 1400
    const val BOARD_TEXTURE_HEIGHT = 1680
    const val SHELF_TEXTURE_WIDTH = 1400
    const val SHELF_TEXTURE_HEIGHT = 360

    const val BOARD_GRID_LEFT_FRACTION = 220f / BOARD_TEXTURE_WIDTH
    const val BOARD_GRID_TOP_FRACTION = 180f / BOARD_TEXTURE_HEIGHT
    const val BOARD_GRID_RIGHT_FRACTION = 1180f / BOARD_TEXTURE_WIDTH
    const val BOARD_GRID_BOTTOM_FRACTION = 1500f / BOARD_TEXTURE_HEIGHT

    const val FALLBACK_CANVAS_COLOR = 0xFFE9EFECL
    const val FALLBACK_BOARD_COLOR = 0xFFF7F6F0L
    const val FALLBACK_ROAD_COLOR = 0xFF536762L
    const val FALLBACK_RAIL_COLOR = 0xFF214F45L
    const val FALLBACK_CAMP_COLOR = 0xFFDCEAE3L
    const val FALLBACK_HEADQUARTERS_COLOR = 0xFFC9B779L
    const val FALLBACK_BOUNDARY_COLOR = 0xFF20312EL

    const val LAST_MOVE_SCALE = 0.92f
    const val LAST_MOVE_INSET_FRACTION = 0.04f
    const val LAST_MOVE_CORNER_LENGTH_FRACTION = 0.18f
    const val LAST_MOVE_COLOR = 0xB84FCBFFL
    const val LAST_MOVE_SHADOW_COLOR = 0x70115C93L

    val BOARD_GRID: List<List<JunqiBitmapPoint>> = List(12) { row ->
        List(5) { column ->
            JunqiBitmapPoint(
                x = 220f + column * 240f,
                y = 180f + row * 120f
            )
        }
    }

    fun pointAt(position: JunqiPosition): JunqiBitmapPoint {
        require(position.row in BOARD_GRID.indices)
        require(position.column in BOARD_GRID.first().indices)
        return BOARD_GRID[position.row][position.column]
    }
}
