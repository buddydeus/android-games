package com.buddygames.junqi

import java.util.Collections

enum class JunqiSide {
    RED,
    BLUE,
}

enum class JunqiBattleOutcome {
    ATTACKER_WINS,
    DEFENDER_WINS,
    BOTH_REMOVED,
}

enum class JunqiResult(val winner: JunqiSide?) {
    RED_WIN(JunqiSide.RED),
    BLUE_WIN(JunqiSide.BLUE),
    DRAW(null);

    companion object {
        fun fromWinner(winner: JunqiSide): JunqiResult =
            if (winner == JunqiSide.RED) RED_WIN else BLUE_WIN
    }
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

class JunqiState(
    pieces: Map<JunqiPosition, JunqiPiece>,
    val currentSide: JunqiSide = JunqiSide.RED,
    revealedFlags: Set<JunqiSide> = emptySet(),
    val quietHalfMoves: Int = 0,
    val result: JunqiResult? = null,
    val lastBattleOutcome: JunqiBattleOutcome? = null,
) {
    val pieces: Map<JunqiPosition, JunqiPiece> = Collections.unmodifiableMap(pieces.toMap())
    val revealedFlags: Set<JunqiSide> = Collections.unmodifiableSet(revealedFlags.toSet())

    init {
        require(quietHalfMoves >= 0) { "Quiet half-move count cannot be negative" }
        require(this.pieces.all { (position, piece) -> position == piece.position }) {
            "Every Junqi board key must match its piece position"
        }
        require(this.pieces.values.map { it.id }.toSet().size == this.pieces.size) {
            "Every Junqi piece ID must be unique"
        }
    }
}
