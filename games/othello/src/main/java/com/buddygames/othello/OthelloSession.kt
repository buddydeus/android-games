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
    val turn: Disc,
    val playerDisc: Disc,
    val lastMove: OthelloMove?
)

internal data class OthelloTurnState(
    val state: OthelloState,
    val turn: Disc,
    val lastMove: OthelloMove?
)

internal data class OthelloSnapshot(
    val state: OthelloState,
    val turn: Disc,
    val score: OthelloScore,
    val lastMove: OthelloMove?
)

internal data class OthelloUndo(
    val snapshot: OthelloSnapshot,
    val remainingHistory: List<OthelloSnapshot>
)

internal fun undoOthello(history: List<OthelloSnapshot>): OthelloUndo? {
    val snapshot = history.lastOrNull() ?: return null
    return OthelloUndo(snapshot, history.dropLast(1))
}

internal fun nextOthelloPlayerDisc(currentPlayer: Disc, winner: Disc?): Disc = when {
    winner == currentPlayer -> currentPlayer.other()
    winner == currentPlayer.other() -> Disc.BLACK
    else -> currentPlayer
}

internal fun advanceOthelloSinglePlayer(
    state: OthelloState,
    nextTurn: Disc,
    playerDisc: Disc,
    lastMove: OthelloMove? = null
): OthelloTurnState {
    var currentState = state
    var currentTurn = nextTurn
    var currentLastMove = lastMove
    while (!OthelloRules.isGameOver(currentState)) {
        val legalMoves = OthelloRules.legalMoves(currentState, currentTurn)
        if (legalMoves.isEmpty()) {
            currentTurn = currentTurn.other()
        } else if (currentTurn == playerDisc) {
            return OthelloTurnState(currentState, currentTurn, currentLastMove)
        } else {
            val robotMove = requireNotNull(OthelloRules.robotMove(currentState, currentTurn))
            currentState = OthelloRules.applyMove(currentState, robotMove, currentTurn)
            currentLastMove = robotMove
            currentTurn = currentTurn.other()
        }
    }
    return OthelloTurnState(currentState, currentTurn, currentLastMove)
}

internal fun newOthelloRound(playerDisc: Disc = Disc.BLACK): OthelloRound {
    val opening = advanceOthelloSinglePlayer(
        state = OthelloState.initial(),
        nextTurn = Disc.BLACK,
        playerDisc = playerDisc
    )
    return OthelloRound(
        state = opening.state,
        turn = opening.turn,
        playerDisc = playerDisc,
        lastMove = opening.lastMove
    )
}
