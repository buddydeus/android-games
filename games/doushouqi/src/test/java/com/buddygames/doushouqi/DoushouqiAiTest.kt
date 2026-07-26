package com.buddygames.doushouqi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DoushouqiAiTest {
    @Test
    fun levelsUseTheExactMonotonicBudgetTable() {
        val expected = listOf(
            DoushouqiAiLevel(1, 1, 1_000, 60, 6, 60, 0),
            DoushouqiAiLevel(2, 2, 4_000, 90, 5, 45, 0),
            DoushouqiAiLevel(3, 3, 12_000, 140, 4, 30, 0),
            DoushouqiAiLevel(4, 4, 35_000, 220, 3, 20, 1),
            DoushouqiAiLevel(5, 5, 80_000, 350, 2, 10, 1),
            DoushouqiAiLevel(6, 6, 180_000, 550, 1, 0, 1),
            DoushouqiAiLevel(7, 7, 400_000, 850, 1, 0, 2),
            DoushouqiAiLevel(8, 8, 800_000, 1_200, 1, 0, 2),
            DoushouqiAiLevel(9, 9, 1_500_000, 1_800, 1, 0, 3),
            DoushouqiAiLevel(10, 10, 2_500_000, 2_600, 1, 0, 3),
        )

        assertEquals(expected, (1..10).map(DoushouqiAiLevel::forLevel))
        expected.zipWithNext().forEach { (lower, higher) ->
            assertTrue(lower.maxDepth <= higher.maxDepth)
            assertTrue(lower.nodeBudget <= higher.nodeBudget)
            assertTrue(lower.timeBudgetMillis <= higher.timeBudgetMillis)
            assertTrue(lower.tacticalExtension <= higher.tacticalExtension)
        }
    }

    @Test
    fun expiredSearchStillReturnsDeterministicLegalFallback() {
        val state = DoushouqiState.initial()
        fun expiredClock(): () -> Long {
            var calls = 0
            return {
                if (calls++ == 0) 0L else Long.MAX_VALUE / 2
            }
        }

        val first = DoushouqiAi.chooseMove(
            state,
            DoushouqiAiLevel.forLevel(1),
            expiredClock(),
        )
        val second = DoushouqiAi.chooseMove(
            state,
            DoushouqiAiLevel.forLevel(1),
            expiredClock(),
        )

        assertNotNull(first)
        assertTrue(first in DoushouqiRules.legalMoves(state))
        assertEquals(first, second)
    }

    @Test
    fun immediateDenEntryAlwaysWins() {
        val state = stateOf(
            pos(1, 3) to green(DoushouqiAnimal.CAT),
            pos(4, 6) to red(DoushouqiAnimal.DOG),
        )

        assertEquals(
            move(pos(1, 3), pos(0, 3)),
            DoushouqiAi.chooseMove(state, DoushouqiAiLevel.forLevel(1)),
        )
    }

    @Test
    fun finalCaptureAlwaysWins() {
        val state = stateOf(
            pos(4, 0) to green(DoushouqiAnimal.CAT),
            pos(5, 0) to red(DoushouqiAnimal.RAT),
        )

        assertEquals(
            move(pos(4, 0), pos(5, 0)),
            DoushouqiAi.chooseMove(state, DoushouqiAiLevel.forLevel(1)),
        )
    }

    @Test
    fun ratElephantCaptureIsPreferredOverQuietMove() {
        val state = stateOf(
            pos(4, 0) to green(DoushouqiAnimal.RAT),
            pos(5, 0) to red(DoushouqiAnimal.ELEPHANT),
            pos(0, 6) to red(DoushouqiAnimal.CAT),
        )

        assertEquals(
            move(pos(4, 0), pos(5, 0)),
            DoushouqiAi.chooseMove(state, DoushouqiAiLevel.forLevel(2)),
        )
    }
}
