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
    val playerStone: Stone
)

internal data class GomokuSnapshot(
    val state: GomokuState,
    val turn: Stone,
    val winner: Stone?,
    val score: GomokuScore
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
    val state = if (playerStone == Stone.WHITE) {
        val opening = GomokuRules.robotMove(empty, Stone.BLACK)
        empty.place(opening.row, opening.col, Stone.BLACK)
    } else {
        empty
    }
    return GomokuRound(
        state = state,
        turn = playerStone,
        winner = null,
        playerStone = playerStone
    )
}
