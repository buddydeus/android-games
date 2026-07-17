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
    val winner: Stone?
)

internal fun newGomokuRound(): GomokuRound = GomokuRound(
    state = GomokuState.empty(),
    turn = Stone.BLACK,
    winner = null
)
