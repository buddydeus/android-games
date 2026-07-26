package com.buddygames.doushouqi

import org.junit.Assert.assertEquals
import org.junit.Test

class DoushouqiSearchPositionTest {
    @Test
    fun makeUnmakeRestoresEverySearchField() {
        val immutable = DoushouqiState.initial()
        val position = DoushouqiSearchPosition(immutable)
        val before = position.snapshot()
        val move = position.legalMoves().first()

        val undo = position.make(move)
        position.unmake(move, undo)

        assertEquals(before, position.snapshot())
    }

    @Test
    fun primitiveMovesMatchPublicRulesAcrossAContinuation() {
        var immutable = DoushouqiState.initial()
        val position = DoushouqiSearchPosition(immutable)

        repeat(12) {
            assertEquals(DoushouqiRules.legalMoves(immutable), position.legalMoves())
            val move = position.legalMoves().first()
            position.make(move)
            immutable = requireNotNull(DoushouqiRules.apply(immutable, move))
            assertEquals(immutable.positionKey, position.positionKey)
            if (immutable.result != null) return
        }
    }
}
