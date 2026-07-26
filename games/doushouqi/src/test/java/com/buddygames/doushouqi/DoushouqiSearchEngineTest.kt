package com.buddygames.doushouqi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DoushouqiSearchEngineTest {
    @Test
    fun searchReportsCompletedDepthWithinNodeBudget() {
        val level = DoushouqiAiLevel.forLevel(2)
        val result = DoushouqiSearchEngine.search(
            state = DoushouqiState.initial(),
            level = level,
            nanoTime = { 0L },
        )

        requireNotNull(result)
        assertTrue(result.move in DoushouqiRules.legalMoves(DoushouqiState.initial()))
        assertTrue(result.completedDepth in 1..level.maxDepth)
        assertTrue(result.nodes <= level.nodeBudget)
        assertFalse(result.timedOut)
    }

    @Test
    fun strongerSearchCapturesImmediateDenThreat() {
        val state = stateOf(
            pos(7, 2) to green(DoushouqiAnimal.DOG),
            pos(7, 3) to red(DoushouqiAnimal.CAT),
            pos(0, 6) to red(DoushouqiAnimal.TIGER),
        )

        assertEquals(
            move(pos(7, 2), pos(7, 3)),
            DoushouqiAi.chooseMove(state, DoushouqiAiLevel.forLevel(3)),
        )
    }

    @Test
    fun identicalPositionAndLevelProduceIdenticalSearchMove() {
        val state = DoushouqiState.initial()
        val level = DoushouqiAiLevel.forLevel(2)

        val first = DoushouqiAi.chooseMove(state, level, { 0L })
        val second = DoushouqiAi.chooseMove(state, level, { 0L })

        assertEquals(first, second)
    }
}
