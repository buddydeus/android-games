package com.buddygames.othello

internal data class OthelloScore(
    val black: Int = 0,
    val white: Int = 0
) {
    val displayText: String
        get() = "$black : $white"

    fun record(winner: Disc?): OthelloScore = when (winner) {
        Disc.BLACK -> copy(black = black + 1)
        Disc.WHITE -> copy(white = white + 1)
        null -> this
    }
}

internal data class OthelloRound(
    val state: OthelloState,
    val turn: Disc
)

internal fun newOthelloRound(): OthelloRound = OthelloRound(
    state = OthelloState.initial(),
    turn = Disc.BLACK
)
