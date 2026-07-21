package com.buddygames.junqi

import java.util.Collections

enum class JunqiSide {
    RED,
    BLUE,
}

enum class JunqiPieceType(val movable: Boolean) {
    COMMANDER(true),
    ARMY_COMMANDER(true),
    DIVISION_COMMANDER(true),
    BRIGADE_COMMANDER(true),
    REGIMENT(true),
    BATTALION(true),
    COMPANY(true),
    PLATOON(true),
    ENGINEER(true),
    BOMB(true),
    MINE(false),
    FLAG(false),
}

data class JunqiPosition(val row: Int, val column: Int) {
    init {
        require(row in 0..11) { "Junqi row must be in 0..11: $row" }
        require(column in 0..4) { "Junqi column must be in 0..4: $column" }
    }
}

data class JunqiPiece(
    val id: String,
    val side: JunqiSide,
    val type: JunqiPieceType,
    val position: JunqiPosition,
    val hasMoved: Boolean = false,
)

data class JunqiMove(val from: JunqiPosition, val to: JunqiPosition)

class JunqiState(pieces: Map<JunqiPosition, JunqiPiece>, val currentSide: JunqiSide = JunqiSide.RED) {
    val pieces: Map<JunqiPosition, JunqiPiece> = Collections.unmodifiableMap(pieces.toMap())

    init {
        require(this.pieces.all { (position, piece) -> position == piece.position }) {
            "Every Junqi board key must match its piece position"
        }
    }
}
