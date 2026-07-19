package com.buddygames.xiangqi

import com.buddygames.api.GameMode

internal enum class XiangqiResult(val winner: Side?) {
    RED_WIN(Side.RED),
    BLACK_WIN(Side.BLACK),
    DRAW(null);

    companion object {
        fun fromWinner(winner: Side): XiangqiResult =
            if (winner == Side.RED) RED_WIN else BLACK_WIN
    }
}

internal data class XiangqiScore(
    val red: Int = 0,
    val black: Int = 0,
    val player: Int = 0,
    val robot: Int = 0
) {
    val intelligenceLevel: Int
        get() = (player + 1).coerceAtMost(10)

    fun displayText(mode: GameMode): String =
        if (mode == GameMode.SINGLE_PLAYER) "$player : $robot" else "$red : $black"

    fun record(result: XiangqiResult?, mode: GameMode, playerSide: Side): XiangqiScore {
        val winner = result?.winner ?: return this
        return when {
            mode == GameMode.SINGLE_PLAYER && winner == playerSide -> copy(player = player + 1)
            mode == GameMode.SINGLE_PLAYER -> copy(robot = robot + 1)
            winner == Side.RED -> copy(red = red + 1)
            else -> copy(black = black + 1)
        }
    }
}

internal data class XiangqiRound(
    val state: XiangqiState,
    val turn: Side,
    val selected: Pair<Int, Int>?,
    val result: XiangqiResult?,
    val playerSide: Side,
    val lastMove: XiangqiMove?
)

internal data class XiangqiSnapshot(
    val state: XiangqiState,
    val turn: Side,
    val result: XiangqiResult?,
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

internal fun nextXiangqiPlayerSide(currentPlayer: Side, result: XiangqiResult?): Side = when {
    result?.winner == currentPlayer -> currentPlayer.other()
    result?.winner == currentPlayer.other() -> Side.RED
    else -> currentPlayer
}

internal fun needsXiangqiRobotTurn(
    mode: GameMode,
    turn: Side,
    playerSide: Side,
    result: XiangqiResult?
): Boolean = mode == GameMode.SINGLE_PLAYER && result == null && turn != playerSide

internal fun shouldApplyXiangqiRobotResult(
    requestedState: XiangqiState,
    requestedSide: Side,
    requestedGeneration: Int,
    currentState: XiangqiState,
    currentTurn: Side,
    currentGeneration: Int,
    result: XiangqiResult?
): Boolean =
    result == null &&
        requestedGeneration == currentGeneration &&
        requestedState == currentState &&
        requestedSide == currentTurn

internal fun newXiangqiRound(playerSide: Side = Side.RED): XiangqiRound {
    return XiangqiRound(
        state = XiangqiState.initial(),
        turn = Side.RED,
        selected = null,
        result = null,
        playerSide = playerSide,
        lastMove = null
    )
}
