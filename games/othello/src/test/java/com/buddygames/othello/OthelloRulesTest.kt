package com.buddygames.othello

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OthelloRulesTest {
    @Test
    fun initialBoardHasFourLegalBlackMoves() {
        val moves = OthelloRules.legalMoves(OthelloState.initial(), Disc.BLACK)

        assertEquals(
            setOf(OthelloMove(2, 3), OthelloMove(3, 2), OthelloMove(4, 5), OthelloMove(5, 4)),
            moves.toSet()
        )
    }

    @Test
    fun applyingMoveFlipsCapturedDiscs() {
        val state = OthelloRules.applyMove(OthelloState.initial(), OthelloMove(2, 3), Disc.BLACK)

        assertEquals(Disc.BLACK, state.cell(3, 3))
        assertEquals(4, state.count(Disc.BLACK))
        assertEquals(1, state.count(Disc.WHITE))
    }

    @Test
    fun robotPrefersCorner() {
        val state = OthelloState.empty()
            .put(0, 1, Disc.WHITE)
            .put(0, 2, Disc.BLACK)
            .put(1, 0, Disc.WHITE)
            .put(2, 0, Disc.BLACK)

        assertEquals(OthelloMove(0, 0), OthelloRules.robotMove(state, Disc.BLACK))
    }

    @Test
    fun gameEndsWhenNeitherSideCanMove() {
        val full = OthelloState(List(8) { List(8) { Disc.BLACK } })

        assertTrue(OthelloRules.isGameOver(full))
    }
}

