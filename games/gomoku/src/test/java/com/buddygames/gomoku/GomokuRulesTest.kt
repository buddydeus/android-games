package com.buddygames.gomoku

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GomokuRulesTest {
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

        assertEquals(Stone.BLACK, round.turn)
        assertNull(round.winner)
        assertEquals(0, round.state.legalMoves().let { GomokuState.SIZE * GomokuState.SIZE - it.size })
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
