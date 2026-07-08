package com.buddygames.gomoku

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GomokuRulesTest {
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
    fun noWinnerOnShortLine() {
        val state = GomokuState.empty()
            .place(0, 0, Stone.BLACK)
            .place(1, 1, Stone.BLACK)
            .place(2, 2, Stone.BLACK)
            .place(3, 3, Stone.BLACK)

        assertNull(GomokuRules.winner(state))
    }
}

