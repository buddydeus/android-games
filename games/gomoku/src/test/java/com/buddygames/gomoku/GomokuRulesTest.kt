package com.buddygames.gomoku

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GomokuRulesTest {
    @Test
    fun gameVersionAndMainMenuLabelStayAligned() {
        assertEquals(3, GomokuPlugin.manifest.versionCode)
        assertEquals("0.0.3", GomokuPlugin.manifest.versionName)
        assertEquals("版本 0.0.3", gomokuVersionLabel(GomokuPlugin.manifest.versionName))
    }

    @Test
    fun undoButtonHidesAfterEitherSideWins() {
        assertTrue(shouldShowGomokuUndo(null))
        assertFalse(shouldShowGomokuUndo(Stone.BLACK))
        assertFalse(shouldShowGomokuUndo(Stone.WHITE))
    }

    @Test
    fun menuUsesUnifiedGameModeLabels() {
        assertEquals(listOf("单人模式", "双人对战", "退出游戏"), gomokuMenuLabels())
    }

    @Test
    fun scoreRecordsEachWinningSide() {
        val score = GomokuScore()
            .record(Stone.BLACK)
            .record(Stone.WHITE)
            .record(Stone.BLACK)

        assertEquals(GomokuScore(black = 2, white = 1), score)
        assertEquals("2 : 1", score.displayText)
    }

    @Test
    fun newRoundResetsBoardAndFirstTurn() {
        val round = newGomokuRound()

        assertEquals(Stone.BLACK, round.playerStone)
        assertEquals(Stone.BLACK, round.turn)
        assertNull(round.winner)
        assertNull(round.lastMove)
        assertEquals(0, round.state.legalMoves().let { GomokuState.SIZE * GomokuState.SIZE - it.size })
    }

    @Test
    fun singlePlayerVictorySwitchesSidesForNextRound() {
        assertEquals(Stone.WHITE, nextGomokuPlayerStone(Stone.BLACK, Stone.BLACK))
        assertEquals(Stone.BLACK, nextGomokuPlayerStone(Stone.WHITE, Stone.WHITE))
    }

    @Test
    fun singlePlayerDefeatAlwaysRestartsAsFirstPlayer() {
        assertEquals(Stone.BLACK, nextGomokuPlayerStone(Stone.BLACK, Stone.WHITE))
        assertEquals(Stone.BLACK, nextGomokuPlayerStone(Stone.WHITE, Stone.BLACK))
    }

    @Test
    fun whitePlayerRoundStartsAfterBlackRobotOpening() {
        val round = newGomokuRound(Stone.WHITE)

        assertEquals(Stone.WHITE, round.playerStone)
        assertEquals(Stone.WHITE, round.turn)
        assertEquals(Stone.BLACK, round.state.cell(7, 7))
        assertEquals(GomokuMove(7, 7), round.lastMove)
        assertEquals(
            1,
            GomokuState.SIZE * GomokuState.SIZE - round.state.legalMoves().size
        )
    }

    @Test
    fun undoRestoresLatestPositionIncludingScore() {
        val first = GomokuSnapshot(
            state = GomokuState.empty(),
            turn = Stone.BLACK,
            winner = null,
            score = GomokuScore(),
            lastMove = null
        )
        val second = first.copy(
            state = first.state.place(7, 7, Stone.BLACK),
            turn = Stone.WHITE,
            score = GomokuScore(black = 2, white = 1),
            lastMove = GomokuMove(7, 7)
        )

        val undo = undoGomoku(listOf(first, second))

        assertEquals(second, undo?.snapshot)
        assertEquals(listOf(first), undo?.remainingHistory)
        assertNull(undoGomoku(emptyList()))
    }

    @Test
    fun lastMoveCellUsesPlacedIntersection() {
        assertEquals(7 to 9, gomokuLastMoveCell(GomokuMove(7, 9)))
        assertNull(gomokuLastMoveCell(null))
    }

    @Test
    fun boardSideUsesAvailableSpaceWithinReferenceBounds() {
        assertEquals(280f, gomokuBoardSide(240f, 500f), 0.001f)
        assertEquals(540f, gomokuBoardSide(720f, 540f), 0.001f)
        assertEquals(680f, gomokuBoardSide(900f, 800f), 0.001f)
    }

    @Test
    fun intersectionFractionAnchorsFirstCenterAndLastGomokuLines() {
        assertEquals(0f, gomokuIntersectionFraction(0), 0f)
        assertEquals(0.5f, gomokuIntersectionFraction(7), 0f)
        assertEquals(1f, gomokuIntersectionFraction(14), 0f)
    }

    @Test
    fun detectsHorizontalWin() {
        val state = GomokuState.empty()
            .place(7, 3, Stone.BLACK)
            .place(7, 4, Stone.BLACK)
            .place(7, 5, Stone.BLACK)
            .place(7, 6, Stone.BLACK)
            .place(7, 7, Stone.BLACK)

        assertEquals(Stone.BLACK, GomokuRules.winner(state))
    }

    @Test
    fun robotTakesImmediateWin() {
        val state = GomokuState.empty()
            .place(4, 4, Stone.WHITE)
            .place(4, 5, Stone.WHITE)
            .place(4, 6, Stone.WHITE)
            .place(4, 7, Stone.WHITE)

        assertEquals(GomokuMove(4, 3), GomokuRules.robotMove(state, Stone.WHITE))
    }

    @Test
    fun robotBlocksImmediateOpponentWin() {
        val state = GomokuState.empty()
            .place(9, 2, Stone.BLACK)
            .place(9, 3, Stone.BLACK)
            .place(9, 4, Stone.BLACK)
            .place(9, 5, Stone.BLACK)

        assertEquals(GomokuMove(9, 1), GomokuRules.robotMove(state, Stone.WHITE))
    }

    @Test
    fun robotBlocksClosedFourAtItsOnlyWinningPoint() {
        val state = GomokuState.empty()
            .place(7, 3, Stone.WHITE)
            .place(7, 4, Stone.BLACK)
            .place(7, 5, Stone.BLACK)
            .place(7, 6, Stone.BLACK)
            .place(7, 7, Stone.BLACK)

        assertEquals(GomokuMove(7, 8), GomokuRules.robotMove(state, Stone.WHITE))
    }

    @Test
    fun robotBlocksOpenThreeBeforeUsingPositionalHeuristic() {
        val state = GomokuState.empty()
            .place(7, 6, Stone.BLACK)
            .place(7, 7, Stone.BLACK)
            .place(7, 8, Stone.BLACK)

        assertEquals(GomokuMove(7, 5), GomokuRules.robotMove(state, Stone.WHITE))
    }

    @Test
    fun robotFillsGapThatWouldTurnBrokenThreeIntoOpenFour() {
        val state = GomokuState.empty()
            .place(7, 5, Stone.BLACK)
            .place(7, 7, Stone.BLACK)
            .place(7, 8, Stone.BLACK)

        assertEquals(GomokuMove(7, 6), GomokuRules.robotMove(state, Stone.WHITE))
    }

    @Test
    fun noWinnerOnShortLine() {
        val state = GomokuState.empty()
            .place(0, 0, Stone.BLACK)
            .place(1, 1, Stone.BLACK)
            .place(2, 2, Stone.BLACK)
            .place(3, 3, Stone.BLACK)

        assertNull(GomokuRules.winner(state))
    }
}
