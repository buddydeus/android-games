package com.buddygames.chess

import com.buddygames.api.GameMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChessSessionTest {
    @Test
    fun gameVersionAndUnifiedMenuLabelsStayAligned() {
        assertEquals(1, ChessPlugin.manifest.versionCode)
        assertEquals("0.0.1", ChessPlugin.manifest.versionName)
        assertEquals("版本 0.0.1", chessVersionLabel(ChessPlugin.manifest.versionName))
        assertEquals(listOf("单人模式", "双人对战", "退出游戏"), chessMenuLabels())
    }

    @Test
    fun singlePlayerScoreFollowsHumanIdentityAcrossColors() {
        val score = ChessScore()
            .record(ChessResult.WHITE_WIN, GameMode.SINGLE_PLAYER, ChessSide.WHITE)
            .record(ChessResult.WHITE_WIN, GameMode.SINGLE_PLAYER, ChessSide.BLACK)
            .record(ChessResult.BLACK_WIN, GameMode.SINGLE_PLAYER, ChessSide.BLACK)
            .record(ChessResult.DRAW_STALEMATE, GameMode.SINGLE_PLAYER, ChessSide.BLACK)

        assertEquals(2, score.player)
        assertEquals(1, score.robot)
        assertEquals("2 : 1", score.displayText(GameMode.SINGLE_PLAYER))
        assertEquals(3, score.intelligenceLevel)
    }

    @Test
    fun twoPlayerScoreUsesWhiteAndBlackIdentities() {
        val score = ChessScore()
            .record(ChessResult.WHITE_WIN, GameMode.TWO_PLAYERS, ChessSide.WHITE)
            .record(ChessResult.BLACK_WIN, GameMode.TWO_PLAYERS, ChessSide.WHITE)
            .record(ChessResult.WHITE_WIN, GameMode.TWO_PLAYERS, ChessSide.WHITE)

        assertEquals(ChessScore(white = 2, black = 1), score)
        assertEquals("2 : 1", score.displayText(GameMode.TWO_PLAYERS))
    }

    @Test
    fun restartSideSwapsAfterWinRestoresWhiteAfterLossAndKeepsDraw() {
        assertEquals(
            ChessSide.BLACK,
            nextChessPlayerSide(ChessSide.WHITE, ChessResult.WHITE_WIN)
        )
        assertEquals(
            ChessSide.WHITE,
            nextChessPlayerSide(ChessSide.BLACK, ChessResult.BLACK_WIN)
        )
        assertEquals(
            ChessSide.WHITE,
            nextChessPlayerSide(ChessSide.BLACK, ChessResult.WHITE_WIN)
        )
        assertEquals(
            ChessSide.BLACK,
            nextChessPlayerSide(ChessSide.BLACK, ChessResult.DRAW_STALEMATE)
        )
    }

    @Test
    fun blackHumanRoundStartsWithPendingWhiteRobotAndNoMarker() {
        val round = newChessRound(ChessSide.BLACK)

        assertEquals(ChessSide.BLACK, round.playerSide)
        assertEquals(ChessSide.WHITE, round.state.sideToMove)
        assertNull(round.selected)
        assertNull(round.result)
        assertNull(round.lastMove)
        assertEquals(1, round.repetitionCounts[chessRepetitionKey(round.state)])
        assertTrue(
            needsChessRobotTurn(
                GameMode.SINGLE_PLAYER,
                round.state,
                round.playerSide,
                round.result
            )
        )
        assertFalse(
            needsChessRobotTurn(
                GameMode.TWO_PLAYERS,
                round.state,
                round.playerSide,
                round.result
            )
        )
    }

    @Test
    fun undoRestoresStateScoreResultRepetitionAndLastMove() {
        val initial = ChessState.initial()
        val priorMove = ChessMove.fromUci("g1f3")
        val snapshot = ChessSnapshot(
            state = initial,
            result = null,
            score = ChessScore(player = 4, robot = 3),
            lastMove = priorMove,
            repetitionCounts = mapOf(chessRepetitionKey(initial) to 2)
        )

        val undo = undoChess(listOf(snapshot))

        assertEquals(snapshot, undo?.snapshot)
        assertTrue(undo?.remainingHistory?.isEmpty() == true)
        assertNull(undoChess(emptyList()))
    }

    @Test
    fun rotatedBoardMapsDisplayAndModelSquaresBothWays() {
        assertTrue(shouldRotateChessBoard(GameMode.SINGLE_PLAYER, ChessSide.BLACK))
        assertFalse(shouldRotateChessBoard(GameMode.SINGLE_PLAYER, ChessSide.WHITE))
        assertFalse(shouldRotateChessBoard(GameMode.TWO_PLAYERS, ChessSide.BLACK))
        assertEquals(63, chessBoardSquare(0, rotated = true))
        assertEquals(0, chessBoardSquare(63, rotated = true))
        assertEquals(chessSquare("e4"), chessBoardSquare(chessSquare("e4"), rotated = false))
        assertEquals(chessSquare("a8"), chessModelSquare(0, 0, rotated = false))
        assertEquals(chessSquare("h1"), chessModelSquare(0, 0, rotated = true))
        assertEquals(chessSquare("a1"), chessModelSquare(7, 0, rotated = false))
        assertEquals(chessSquare("h8"), chessModelSquare(7, 0, rotated = true))
    }

    @Test
    fun terminalVisibilityKeepsUndoForDrawsButNotWins() {
        assertTrue(shouldShowChessUndo(null))
        assertFalse(shouldShowChessRestart(null))
        assertFalse(shouldShowChessUndo(ChessResult.WHITE_WIN))
        assertFalse(shouldShowChessUndo(ChessResult.BLACK_WIN))
        assertTrue(shouldShowChessUndo(ChessResult.DRAW_STALEMATE))
        assertTrue(shouldShowChessUndo(ChessResult.DRAW_REPETITION))
        assertTrue(shouldShowChessRestart(ChessResult.WHITE_WIN))
        assertTrue(shouldShowChessRestart(ChessResult.DRAW_FIFTY_MOVE))
    }

    @Test
    fun latestMoveMarkerUsesSharedBrightBlueGeometry() {
        val move = ChessMove.fromUci("e2e4")

        assertEquals(chessSquare("e4"), chessLastMoveSquare(move))
        assertNull(chessLastMoveSquare(null))
        assertEquals(0.92f, CHESS_LAST_MOVE_MARKER_SCALE, 0f)
        assertEquals(0.04f, CHESS_LAST_MOVE_MARKER_INSET_FRACTION, 0f)
        assertEquals(0.18f, CHESS_LAST_MOVE_MARKER_CORNER_LENGTH_FRACTION, 0f)
        assertEquals(0xB84FCBFFL, CHESS_LAST_MOVE_MARKER_HIGHLIGHT_ARGB)
    }

    @Test
    fun repetitionKeyIgnoresMoveCountersButKeepsPositionRightsAndTurn() {
        val state = ChessState.initial()

        assertEquals(
            chessRepetitionKey(state),
            chessRepetitionKey(state.copy(halfMoveClock = 37, fullMoveNumber = 22))
        )
        assertFalse(
            chessRepetitionKey(state) ==
                chessRepetitionKey(state.copy(sideToMove = ChessSide.BLACK))
        )
    }
}
