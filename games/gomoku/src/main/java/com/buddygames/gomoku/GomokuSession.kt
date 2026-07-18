package com.buddygames.gomoku

internal data class GomokuScore(
    val black: Int = 0,
    val white: Int = 0
) {
    val displayText: String
        get() = "$black : $white"

    fun record(winner: Stone?): GomokuScore = when (winner) {
        Stone.BLACK -> copy(black = black + 1)
        Stone.WHITE -> copy(white = white + 1)
        null -> this
    }
}

internal data class GomokuRound(
    val state: GomokuState,
    val turn: Stone,
    val winner: Stone?,
    val playerStone: Stone,
    val lastMove: GomokuMove?
)

internal data class GomokuSnapshot(
    val state: GomokuState,
    val turn: Stone,
    val winner: Stone?,
    val score: GomokuScore,
    val lastMove: GomokuMove?
)

internal data class GomokuUndo(
    val snapshot: GomokuSnapshot,
    val remainingHistory: List<GomokuSnapshot>
)

internal fun undoGomoku(history: List<GomokuSnapshot>): GomokuUndo? {
    val snapshot = history.lastOrNull() ?: return null
    return GomokuUndo(snapshot, history.dropLast(1))
}

internal fun nextGomokuPlayerStone(currentPlayer: Stone, winner: Stone?): Stone = when {
    winner == currentPlayer -> currentPlayer.other()
    winner == currentPlayer.other() -> Stone.BLACK
    else -> currentPlayer
}

internal fun newGomokuRound(playerStone: Stone = Stone.BLACK): GomokuRound {
    val empty = GomokuState.empty()
    val opening = if (playerStone == Stone.WHITE) {
        GomokuRules.robotMove(empty, Stone.BLACK)
    } else null
    val state = opening?.let { empty.place(it.row, it.col, Stone.BLACK) } ?: empty
    return GomokuRound(
        state = state,
        turn = playerStone,
        winner = null,
        playerStone = playerStone,
        lastMove = opening
    )
}
