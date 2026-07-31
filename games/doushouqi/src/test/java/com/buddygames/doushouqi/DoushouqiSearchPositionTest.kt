package com.buddygames.doushouqi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun primitiveAndPublicRulesAllowBothSidesToEnterOwnDen() {
        val cases = listOf(
            stateOf(
                pos(7, 3) to green(DoushouqiAnimal.CAT),
                pos(4, 6) to red(DoushouqiAnimal.DOG),
            ) to move(pos(7, 3), pos(8, 3)),
            stateOf(
                DoushouqiSide.VERMILION,
                pos(1, 3) to red(DoushouqiAnimal.CAT),
                pos(4, 0) to green(DoushouqiAnimal.DOG),
            ) to move(pos(1, 3), pos(0, 3)),
        )

        cases.forEach { (state, ownDenMove) ->
            val publicMoves = DoushouqiRules.legalMoves(state)
            val primitiveMoves = DoushouqiSearchPosition(state).legalMoves()

            assertTrue(ownDenMove in publicMoves)
            assertTrue(ownDenMove in primitiveMoves)
            assertEquals(publicMoves, primitiveMoves)
        }
    }
}
