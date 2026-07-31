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
    fun aiTakesWinningCaptureInDefendersOwnTrap() {
        val state = stateOf(
            pos(2, 3) to green(DoushouqiAnimal.CAT),
            pos(1, 3) to red(DoushouqiAnimal.ELEPHANT),
        )

        assertEquals(
            move(pos(2, 3), pos(1, 3)),
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

    @Test
    fun cancelledSearchReturnsLegalFallbackWithoutExpanding() {
        val state = DoushouqiState.initial()
        val result = DoushouqiSearchEngine.search(
            state = state,
            level = DoushouqiAiLevel.forLevel(10),
            nanoTime = { 0L },
            shouldStop = { true },
        )

        requireNotNull(result)
        assertTrue(result.move in DoushouqiRules.legalMoves(state))
        assertEquals(0, result.completedDepth)
        assertEquals(0, result.nodes)
    }

    @Test
    fun cancelledFallbackDoesNotPreferEnteringOwnTrap() {
        val result = requireNotNull(
            DoushouqiSearchEngine.search(
                state = ownTrapOrderingState(),
                level = DoushouqiAiLevel(6, 1, 1_000, 1_000, 1, 0, 0),
                nanoTime = { 0L },
                shouldStop = { true },
            ),
        )

        assertEquals(move(pos(7, 2), pos(6, 2)), result.move)
        assertEquals(0, result.completedDepth)
    }

    @Test
    fun depthOneSearchTreatsOwnTrapAsVulnerable() {
        val result = requireNotNull(
            DoushouqiSearchEngine.search(
                state = ownTrapChoiceState(),
                level = DoushouqiAiLevel(6, 1, 1_000, 1_000, 1, 0, 0),
                nanoTime = { 0L },
            ),
        )

        assertEquals(move(pos(8, 1), pos(8, 0)), result.move)
        assertEquals(1, result.completedDepth)
    }

    private fun ownTrapChoiceState(): DoushouqiState = stateOf(
        pos(8, 1) to green(DoushouqiAnimal.CAT),
        pos(7, 1) to red(DoushouqiAnimal.ELEPHANT),
        pos(4, 6) to red(DoushouqiAnimal.DOG),
    )

    private fun ownTrapOrderingState(): DoushouqiState = stateOf(
        pos(7, 2) to green(DoushouqiAnimal.CAT),
        pos(4, 6) to red(DoushouqiAnimal.DOG),
    )
}
