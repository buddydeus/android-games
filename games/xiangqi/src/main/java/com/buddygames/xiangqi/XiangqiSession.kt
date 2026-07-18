package com.buddygames.xiangqi

internal data class XiangqiScore(
    val red: Int = 0,
    val black: Int = 0
) {
    val displayText: String
        get() = "$red : $black"

    fun record(winner: Side?): XiangqiScore = when (winner) {
        Side.RED -> copy(red = red + 1)
        Side.BLACK -> copy(black = black + 1)
        null -> this
    }
}

internal data class XiangqiRound(
    val state: XiangqiState,
    val turn: Side,
    val selected: Pair<Int, Int>?,
    val winner: Side?,
    val playerSide: Side,
    val lastMove: XiangqiMove?
)

internal data class XiangqiSnapshot(
    val state: XiangqiState,
    val turn: Side,
    val winner: Side?,
    val score: XiangqiScore,
    val lastMove: XiangqiMove?
)

internal data class XiangqiUndo(
    val snapshot: XiangqiSnapshot,
    val remainingHistory: List<XiangqiSnapshot>
)

internal fun undoXiangqi(history: List<XiangqiSnapshot>): XiangqiUndo? {
    val snapshot = history.lastOrNull() ?: return null
    return XiangqiUndo(snapshot, history.dropLast(1))
}

internal fun nextXiangqiPlayerSide(currentPlayer: Side, winner: Side?): Side = when {
    winner == currentPlayer -> currentPlayer.other()
    winner == currentPlayer.other() -> Side.RED
    else -> currentPlayer
}

internal fun newXiangqiRound(playerSide: Side = Side.RED): XiangqiRound {
    val initial = XiangqiState.initial()
    val opening = if (playerSide == Side.BLACK) {
        requireNotNull(XiangqiRules.robotMove(initial, Side.RED))
    } else null
    val state = opening?.let(initial::apply) ?: initial
    return XiangqiRound(
        state = state,
        turn = playerSide,
        selected = null,
        winner = null,
        playerSide = playerSide,
        lastMove = opening
    )
}
