package com.buddygames.doushouqi

import androidx.compose.ui.graphics.Color

internal const val DOUSHOUQI_CANVAS_ARGB = 0xFFEDF3EFL
internal const val DOUSHOUQI_SURFACE_ARGB = 0xFFF7F8F3L
internal const val DOUSHOUQI_BOARD_ARGB = 0xFFE5B85DL
internal const val DOUSHOUQI_BOARD_LIGHT_ARGB = 0xFFF0CB7BL
internal const val DOUSHOUQI_GRID_ARGB = 0xFF5A3A12L
internal const val DOUSHOUQI_RIVER_ARGB = 0xFF075D86L
internal const val DOUSHOUQI_RIVER_LINE_ARGB = 0xFF2A7898L
internal const val DOUSHOUQI_JUMP_LINE_ARGB = 0xFFD9B85FL
internal const val DOUSHOUQI_DEN_ARGB = 0xFF8E372EL
internal const val DOUSHOUQI_TRAP_ARGB = 0xFFB6813FL
internal const val DOUSHOUQI_GREEN_PIECE_ARGB = 0xFF0E5A3AL
internal const val DOUSHOUQI_RED_PIECE_ARGB = 0xFFC63A20L
internal const val DOUSHOUQI_PIECE_TEXT_ARGB = 0xFFFFF3D2L
internal const val DOUSHOUQI_INK_ARGB = 0xFF183D30L
internal const val DOUSHOUQI_MUTED_ARGB = 0xFF6B6D69L

internal const val DOUSHOUQI_OUTER_PADDING_DP = 28f
internal const val DOUSHOUQI_BOARD_RAIL_GAP_DP = 34f
internal const val DOUSHOUQI_GAME_RAIL_WIDTH_DP = 300f
internal const val DOUSHOUQI_GAME_RAIL_HEIGHT_FRACTION = 0.94f
internal const val DOUSHOUQI_MENU_RAIL_WIDTH_DP = 320f
internal const val DOUSHOUQI_MENU_RAIL_HEIGHT_FRACTION = 0.88f
internal const val DOUSHOUQI_WIDE_BREAKPOINT_DP = 900f
internal const val DOUSHOUQI_BOARD_ASPECT_RATIO = 1f
internal const val DOUSHOUQI_PIECE_SCALE = 0.86f

internal const val DOUSHOUQI_LAST_MOVE_MARKER_SCALE = 0.92f
internal const val DOUSHOUQI_LAST_MOVE_MARKER_INSET_FRACTION = 0.04f
internal const val DOUSHOUQI_LAST_MOVE_MARKER_CORNER_LENGTH_FRACTION = 0.18f
internal const val DOUSHOUQI_LAST_MOVE_MARKER_HIGHLIGHT_ARGB = 0xB84FCBFFL
internal const val DOUSHOUQI_LAST_MOVE_MARKER_SHADOW_ARGB = 0x70115C93L

internal val DoushouqiCanvas = Color(DOUSHOUQI_CANVAS_ARGB)
internal val DoushouqiSurface = Color(DOUSHOUQI_SURFACE_ARGB)
internal val DoushouqiBoardColor = Color(DOUSHOUQI_BOARD_ARGB)
internal val DoushouqiBoardLight = Color(DOUSHOUQI_BOARD_LIGHT_ARGB)
internal val DoushouqiGrid = Color(DOUSHOUQI_GRID_ARGB)
internal val DoushouqiRiver = Color(DOUSHOUQI_RIVER_ARGB)
internal val DoushouqiRiverLine = Color(DOUSHOUQI_RIVER_LINE_ARGB)
internal val DoushouqiDen = Color(DOUSHOUQI_DEN_ARGB)
internal val DoushouqiTrap = Color(DOUSHOUQI_TRAP_ARGB)
internal val DoushouqiGreenPiece = Color(DOUSHOUQI_GREEN_PIECE_ARGB)
internal val DoushouqiRedPiece = Color(DOUSHOUQI_RED_PIECE_ARGB)
internal val DoushouqiPieceText = Color(DOUSHOUQI_PIECE_TEXT_ARGB)
internal val DoushouqiInk = Color(DOUSHOUQI_INK_ARGB)
internal val DoushouqiMuted = Color(DOUSHOUQI_MUTED_ARGB)
internal val DoushouqiLastMove = Color(DOUSHOUQI_LAST_MOVE_MARKER_HIGHLIGHT_ARGB)

internal object DoushouqiVisuals {
    const val BOARD_TEXTURE = "assets/board/doushouqi-board.png"
    const val BOARD_TEXTURE_SIZE = 1400
    const val PIECE_TEXTURE_SIZE = 512

    val PIECE_TEXTURES: Map<Pair<DoushouqiSide, DoushouqiAnimal>, String> =
        DoushouqiSide.entries.flatMap { side ->
            DoushouqiAnimal.entries.map { animal ->
                (side to animal) to
                    "assets/pieces/${side.filePrefix}-${animal.name.lowercase()}.png"
            }
        }.toMap()

    fun pieceTexture(piece: DoushouqiPiece): String =
        requireNotNull(PIECE_TEXTURES[piece.side to piece.animal])
}

private val DoushouqiSide.filePrefix: String
    get() = when (this) {
        DoushouqiSide.PINE_GREEN -> "green"
        DoushouqiSide.VERMILION -> "red"
    }
