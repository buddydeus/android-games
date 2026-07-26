package com.buddygames.doushouqi

import org.junit.Assert.assertEquals
import org.junit.Test

class DoushouqiTerminalTest {
    @Test
    fun denEntryWinsBeforeThirdRepetition() {
        val from = pos(1, 3)
        val den = pos(0, 3)
        val targetBoard = DoushouqiState.fromPieces(
            sideToMove = DoushouqiSide.VERMILION,
            pieces = mapOf(
                den to green(DoushouqiAnimal.CAT),
                pos(4, 6) to red(DoushouqiAnimal.DOG),
            ),
        )
        val source = DoushouqiState.fromPieces(
            sideToMove = DoushouqiSide.PINE_GREEN,
            pieces = mapOf(
                from to green(DoushouqiAnimal.CAT),
                pos(4, 6) to red(DoushouqiAnimal.DOG),
            ),
            repetitionCounts = mapOf(targetBoard.positionKey to 2),
        )

        val result = requireNotNull(DoushouqiRules.apply(source, move(from, den))).result

        assertEquals(
            DoushouqiResult.Win(
                DoushouqiSide.PINE_GREEN,
                DoushouqiWinReason.DEN,
            ),
            result,
        )
    }

    @Test
    fun finalCaptureWinsBeforeHundredthQuietThreshold() {
        val from = pos(4, 0)
        val to = pos(5, 0)
        val source = DoushouqiState.fromPieces(
            sideToMove = DoushouqiSide.PINE_GREEN,
            pieces = mapOf(
                from to green(DoushouqiAnimal.CAT),
                to to red(DoushouqiAnimal.RAT),
            ),
            quietHalfMoves = 99,
        )

        val result = requireNotNull(DoushouqiRules.apply(source, move(from, to))).result

        assertEquals(
            DoushouqiResult.Win(
                DoushouqiSide.PINE_GREEN,
                DoushouqiWinReason.FINAL_CAPTURE,
            ),
            result,
        )
    }

    @Test
    fun completingMoveWinsWhenNextSideHasNoLegalMove() {
        val source = DoushouqiState.fromPieces(
            sideToMove = DoushouqiSide.PINE_GREEN,
            pieces = mapOf(
                pos(8, 6) to green(DoushouqiAnimal.RAT),
                pos(0, 1) to green(DoushouqiAnimal.ELEPHANT),
                pos(1, 0) to green(DoushouqiAnimal.LION),
                pos(0, 0) to red(DoushouqiAnimal.CAT),
            ),
        )

        val result = requireNotNull(
            DoushouqiRules.apply(source, move(pos(8, 6), pos(7, 6))),
        ).result

        assertEquals(
            DoushouqiResult.Win(
                DoushouqiSide.PINE_GREEN,
                DoushouqiWinReason.NO_LEGAL_MOVE,
            ),
            result,
        )
    }

    @Test
    fun thirdLegalEquivalentOccurrenceDraws() {
        var state = DoushouqiState.fromPieces(
            sideToMove = DoushouqiSide.PINE_GREEN,
            pieces = mapOf(
                pos(4, 0) to green(DoushouqiAnimal.CAT),
                pos(4, 6) to red(DoushouqiAnimal.CAT),
            ),
        )
        val cycle = listOf(
            move(pos(4, 0), pos(3, 0)),
            move(pos(4, 6), pos(3, 6)),
            move(pos(3, 0), pos(4, 0)),
            move(pos(3, 6), pos(4, 6)),
        )

        repeat(2) {
            cycle.forEach { nextMove ->
                state = requireNotNull(DoushouqiRules.apply(state, nextMove))
            }
        }

        assertEquals(
            DoushouqiResult.Draw(DoushouqiDrawReason.REPETITION),
            state.result,
        )
        assertEquals(3, state.repetitionCounts[state.positionKey])
    }

    @Test
    fun hundredthConsecutiveQuietHalfMoveDraws() {
        val source = DoushouqiState.fromPieces(
            sideToMove = DoushouqiSide.PINE_GREEN,
            pieces = mapOf(
                pos(4, 0) to green(DoushouqiAnimal.CAT),
                pos(4, 6) to red(DoushouqiAnimal.CAT),
            ),
            quietHalfMoves = 99,
        )

        val next = requireNotNull(
            DoushouqiRules.apply(source, move(pos(4, 0), pos(3, 0))),
        )

        assertEquals(
            DoushouqiResult.Draw(DoushouqiDrawReason.QUIET_100),
            next.result,
        )
        assertEquals(100, next.quietHalfMoves)
    }
}
