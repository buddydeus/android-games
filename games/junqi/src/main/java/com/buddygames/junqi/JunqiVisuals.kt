package com.buddygames.junqi

internal data class JunqiBitmapPoint(val x: Float, val y: Float)

internal data class JunqiTextureContract(
    val path: String,
    val width: Int,
    val height: Int,
)

/** Package-local bitmap registration and Compose fallback tokens for the Junqi board. */
internal object JunqiVisuals {
    const val ICON_TEXTURE_PATH = "icon.png"
    const val BOARD_TEXTURE_PATH = "board/junqi-board.png"
    const val SHELF_TEXTURE_PATH = "textures/junqi-shelf.png"

    const val ICON_TEXTURE_WIDTH = 1024
    const val ICON_TEXTURE_HEIGHT = 1024
    const val BOARD_TEXTURE_WIDTH = 1400
    const val BOARD_TEXTURE_HEIGHT = 1680
    const val SHELF_TEXTURE_WIDTH = 1400
    const val SHELF_TEXTURE_HEIGHT = 360

    val ICON_TEXTURE_CONTRACT = JunqiTextureContract(
        path = ICON_TEXTURE_PATH,
        width = ICON_TEXTURE_WIDTH,
        height = ICON_TEXTURE_HEIGHT,
    )
    val BOARD_TEXTURE_CONTRACT = JunqiTextureContract(
        path = BOARD_TEXTURE_PATH,
        width = BOARD_TEXTURE_WIDTH,
        height = BOARD_TEXTURE_HEIGHT,
    )
    val SHELF_TEXTURE_CONTRACT = JunqiTextureContract(
        path = SHELF_TEXTURE_PATH,
        width = SHELF_TEXTURE_WIDTH,
        height = SHELF_TEXTURE_HEIGHT,
    )
    val textureContracts = listOf(
        ICON_TEXTURE_CONTRACT,
        BOARD_TEXTURE_CONTRACT,
        SHELF_TEXTURE_CONTRACT,
    )

    const val BOARD_GRID_LEFT_FRACTION = 220f / BOARD_TEXTURE_WIDTH
    const val BOARD_GRID_TOP_FRACTION = 180f / BOARD_TEXTURE_HEIGHT
    const val BOARD_GRID_RIGHT_FRACTION = 1180f / BOARD_TEXTURE_WIDTH
    const val BOARD_GRID_BOTTOM_FRACTION = 1500f / BOARD_TEXTURE_HEIGHT

    const val FALLBACK_CANVAS_COLOR = 0xFFE8EDE9L
    const val FALLBACK_BOARD_COLOR = 0xFFD8C98FL
    const val FALLBACK_ROAD_COLOR = 0xFF494233L
    const val FALLBACK_RAIL_DARK_COLOR = 0xFF2D2A23L
    const val FALLBACK_RAIL_LIGHT_COLOR = 0xFFF1E5B9L
    const val FALLBACK_CAMP_COLOR = 0xFFBBC2A6L
    const val FALLBACK_STATION_COLOR = 0xFFE9DCAAL
    const val FALLBACK_HEADQUARTERS_LABEL_COLOR = 0xFFB4312BL
    const val FALLBACK_BOUNDARY_COLOR = 0xFF68745DL

    const val GREEN_PIECE_COLOR = 0xFF23704BL
    const val ORANGE_PIECE_COLOR = 0xFFC65012L
    const val PIECE_TEXT_COLOR = 0xFFFFFFFFL
    const val PIECE_LABEL_HEIGHT_FRACTION = 0.64f
    const val PIECE_LABEL_MIN_INSET_FRACTION = 0.06f

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
