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
    val winner: Side?
)

internal fun newXiangqiRound(): XiangqiRound = XiangqiRound(
    state = XiangqiState.initial(),
    turn = Side.RED,
    selected = null,
    winner = null
)
