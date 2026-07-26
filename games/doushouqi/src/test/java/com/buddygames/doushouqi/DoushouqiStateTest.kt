package com.buddygames.doushouqi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class DoushouqiStateTest {
    @Test
    fun illegalMoveReturnsNullAndLeavesSourceUnchanged() {
        val source = DoushouqiState.initial()
        val illegal = move(pos(6, 0), pos(5, 1))

        assertNull(DoushouqiRules.apply(source, illegal))
        assertEquals(green(DoushouqiAnimal.ELEPHANT), source.pieceAt(pos(6, 0)))
        assertEquals(DoushouqiSide.PINE_GREEN, source.sideToMove)
        assertEquals(0, source.quietHalfMoves)
        assertNull(source.lastMove)
        assertNull(source.result)
    }

    @Test
    fun legalQuietMoveCreatesIndependentSuccessorAndUpdatesContext() {
        val source = DoushouqiState.initial()
        val selected = move(pos(6, 0), pos(5, 0))
        val next = requireNotNull(DoushouqiRules.apply(source, selected))

        assertEquals(green(DoushouqiAnimal.ELEPHANT), source.pieceAt(pos(6, 0)))
        assertNull(source.pieceAt(pos(5, 0)))
        assertNull(next.pieceAt(pos(6, 0)))
        assertEquals(green(DoushouqiAnimal.ELEPHANT), next.pieceAt(pos(5, 0)))
        assertEquals(DoushouqiSide.VERMILION, next.sideToMove)
        assertEquals(1, next.quietHalfMoves)
        assertEquals(selected, next.lastMove)
        assertEquals(1, next.repetitionCounts[next.positionKey])
        assertNull(next.result)
    }

    @Test
    fun captureRemovesOnlyDefenderAndResetsQuietCounter() {
        val from = pos(4, 0)
        val to = pos(5, 0)
        val source = DoushouqiState.fromPieces(
            sideToMove = DoushouqiSide.PINE_GREEN,
            quietHalfMoves = 17,
            pieces = mapOf(
                from to green(DoushouqiAnimal.DOG),
                to to red(DoushouqiAnimal.CAT),
                pos(0, 6) to red(DoushouqiAnimal.TIGER),
            ),
        )
        val next = requireNotNull(DoushouqiRules.apply(source, move(from, to)))

        assertEquals(green(DoushouqiAnimal.DOG), next.pieceAt(to))
        assertNull(next.pieceAt(from))
        assertEquals(2, next.pieces().size)
        assertEquals(0, next.quietHalfMoves)
        assertNull(next.result)
    }

    @Test
    fun terminalStateRejectsFurtherMovesWithoutAllocatingSuccessor() {
        val terminal = DoushouqiState.fromPieces(
            sideToMove = DoushouqiSide.PINE_GREEN,
            pieces = mapOf(pos(4, 0) to green(DoushouqiAnimal.CAT)),
            result = DoushouqiResult.Draw(DoushouqiDrawReason.QUIET_100),
        )

        val result = DoushouqiRules.apply(terminal, move(pos(4, 0), pos(5, 0)))

        assertNull(result)
        assertSame(terminal.result, terminal.result)
    }
}
