package com.buddygames.chess

import com.buddygames.api.GameMode

internal data class ChessScore(
    val white: Int = 0,
    val black: Int = 0,
    val player: Int = 0,
    val robot: Int = 0
) {
    val intelligenceLevel: Int
        get() = (player + 1).coerceAtMost(10)

    fun displayText(mode: GameMode): String =
        if (mode == GameMode.SINGLE_PLAYER) "$player : $robot" else "$white : $black"

    fun record(result: ChessResult?, mode: GameMode, playerSide: ChessSide): ChessScore {
        val winner = result?.winner ?: return this
        return when {
            mode == GameMode.SINGLE_PLAYER && winner == playerSide ->
                copy(player = player + 1)
            mode == GameMode.SINGLE_PLAYER ->
                copy(robot = robot + 1)
            winner == ChessSide.WHITE ->
                copy(white = white + 1)
            else ->
                copy(black = black + 1)
        }
    }
}

internal data class ChessRound(
    val state: ChessState,
    val selected: Int?,
    val result: ChessResult?,
    val playerSide: ChessSide,
    val lastMove: ChessMove?,
    val repetitionCounts: Map<Long, Int>
)

internal data class ChessSnapshot(
    val state: ChessState,
    val result: ChessResult?,
    val score: ChessScore,
    val lastMove: ChessMove?,
    val repetitionCounts: Map<Long, Int>
)

internal data class ChessUndo(
    val snapshot: ChessSnapshot,
    val remainingHistory: List<ChessSnapshot>
)

internal fun undoChess(history: List<ChessSnapshot>): ChessUndo? {
    val snapshot = history.lastOrNull() ?: return null
    return ChessUndo(snapshot, history.dropLast(1))
}

internal fun nextChessPlayerSide(
    currentPlayer: ChessSide,
    result: ChessResult?
): ChessSide = when {
    result?.winner == currentPlayer -> currentPlayer.other()
    result?.winner == currentPlayer.other() -> ChessSide.WHITE
    else -> currentPlayer
}

internal fun needsChessRobotTurn(
    mode: GameMode,
    state: ChessState,
    playerSide: ChessSide,
    result: ChessResult?
): Boolean =
    mode == GameMode.SINGLE_PLAYER &&
        result == null &&
        state.sideToMove != playerSide

internal fun shouldApplyChessRobotResult(
    requestedState: ChessState,
    requestedGeneration: Int,
    currentState: ChessState,
    currentGeneration: Int,
    result: ChessResult?
): Boolean =
    result == null &&
        requestedGeneration == currentGeneration &&
        requestedState == currentState

internal fun chessRepetitionKey(state: ChessState): Long {
    val enPassant = state.enPassantSquare
    val hasLegalEnPassant = enPassant != null && ChessRules.legalMoves(state).any { move ->
        move.to == enPassant &&
            state.board[move.from]?.type == ChessPieceType.PAWN &&
            state.board[move.to] == null &&
            fileOf(move.from) != fileOf(move.to)
    }
    return positionHash(
        state.copy(
            enPassantSquare = enPassant.takeIf { hasLegalEnPassant },
            halfMoveClock = 0,
            fullMoveNumber = 1
        )
    )
}

internal fun recordChessPosition(
    counts: Map<Long, Int>,
    state: ChessState
): Map<Long, Int> {
    val key = chessRepetitionKey(state)
    return counts + (key to ((counts[key] ?: 0) + 1))
}

internal fun newChessRound(playerSide: ChessSide = ChessSide.WHITE): ChessRound {
    val state = ChessState.initial()
    return ChessRound(
        state = state,
        selected = null,
        result = null,
        playerSide = playerSide,
        lastMove = null,
        repetitionCounts = mapOf(chessRepetitionKey(state) to 1)
    )
}

internal fun chessMenuLabels(): List<String> = listOf("单人模式", "双人对战", "退出游戏")

internal fun chessVersionLabel(versionName: String): String = "版本 $versionName"

internal fun chessIntelligenceLabel(level: Int): String {
    require(level in 1..10)
    return "智能等级 $level"
}

internal fun shouldRotateChessBoard(mode: GameMode, playerSide: ChessSide): Boolean =
    mode == GameMode.SINGLE_PLAYER && playerSide == ChessSide.BLACK

internal fun chessBoardSquare(square: Int, rotated: Boolean): Int {
    require(square in 0..63)
    return if (rotated) 63 - square else square
}

internal fun chessLastMoveSquare(move: ChessMove?): Int? = move?.to

internal fun shouldShowChessUndo(result: ChessResult?): Boolean = result?.winner == null

internal fun shouldShowChessRestart(result: ChessResult?): Boolean = result != null

internal const val CHESS_LAST_MOVE_MARKER_SCALE = 0.92f
internal const val CHESS_LAST_MOVE_MARKER_INSET_FRACTION = 0.04f
internal const val CHESS_LAST_MOVE_MARKER_CORNER_LENGTH_FRACTION = 0.18f
internal const val CHESS_LAST_MOVE_MARKER_SHADOW_ARGB = 0x70115C93L
internal const val CHESS_LAST_MOVE_MARKER_HIGHLIGHT_ARGB = 0xB84FCBFFL
