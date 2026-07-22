package com.buddygames.junqi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class JunqiStateTest {
    @Test
    fun stateDefensivelyCopiesCallerOwnedCollections() {
        val red = piece("red", JunqiSide.RED, JunqiPieceType.ENGINEER, 3, 1)
        val sourcePieces = mutableMapOf(red.position to red)
        val sourceRevealedFlags = mutableSetOf(JunqiSide.RED)

        val state = JunqiState(sourcePieces, revealedFlags = sourceRevealedFlags)
        sourcePieces.clear()
        sourceRevealedFlags.clear()

        assertEquals(mapOf(red.position to red), state.pieces)
        assertEquals(setOf(JunqiSide.RED), state.revealedFlags)
    }

    @Test
    fun stateCollectionsCannotBeMutatedThroughMutableCasts() {
        val state = JunqiState(
            pieces = mapOf(at(3, 1) to piece("red", JunqiSide.RED, JunqiPieceType.ENGINEER, 3, 1)),
            revealedFlags = setOf(JunqiSide.RED),
        )

        assertThrows(UnsupportedOperationException::class.java) {
            @Suppress("UNCHECKED_CAST")
            (state.pieces as MutableMap<JunqiPosition, JunqiPiece>).clear()
        }
        assertThrows(UnsupportedOperationException::class.java) {
            @Suppress("UNCHECKED_CAST")
            (state.revealedFlags as MutableSet<JunqiSide>).clear()
        }
    }

    @Test
    fun applyMoveReturnsANewStateWithoutMutatingTheOriginal() {
        val original = JunqiState(
            pieces = mapOf(
                at(3, 1) to piece("red", JunqiSide.RED, JunqiPieceType.ENGINEER, 3, 1),
                at(8, 1) to piece("blue", JunqiSide.BLUE, JunqiPieceType.ENGINEER, 8, 1),
            ),
        )
        val originalPieces = original.pieces

        val moved = JunqiRules.applyMove(original, JunqiMove(at(3, 1), at(3, 2)))

        assertFalse(original === moved)
        assertSame(originalPieces, original.pieces)
        assertEquals("red", original.pieces[at(3, 1)]?.id)
        assertFalse(original.pieces.getValue(at(3, 1)).hasMoved)
        assertFalse(at(3, 2) in original.pieces)
        assertEquals("red", moved.pieces[at(3, 2)]?.id)
    }

    private fun piece(id: String, side: JunqiSide, type: JunqiPieceType, row: Int, column: Int) =
        JunqiPiece(id, side, type, at(row, column))

    private fun at(row: Int, column: Int) = JunqiPosition(row, column)
}
