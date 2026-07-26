package com.buddygames.doushouqi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class DoushouqiBoardTest {
    @Test
    fun standardTerrainAndInitialInventoryAreExact() {
        val state = DoushouqiState.initial()

        assertEquals(9, DoushouqiState.ROWS)
        assertEquals(7, DoushouqiState.COLUMNS)
        assertEquals(DoushouqiTerrain.DEN, terrainAt(pos(0, 3)))
        assertEquals(DoushouqiTerrain.DEN, terrainAt(pos(8, 3)))
        assertEquals(DoushouqiTerrain.TRAP, terrainAt(pos(0, 2)))
        assertEquals(DoushouqiTerrain.TRAP, terrainAt(pos(1, 3)))
        assertEquals(DoushouqiTerrain.TRAP, terrainAt(pos(7, 3)))
        assertEquals(DoushouqiTerrain.TRAP, terrainAt(pos(8, 4)))
        assertEquals(DoushouqiTerrain.RIVER, terrainAt(pos(3, 1)))
        assertEquals(DoushouqiTerrain.RIVER, terrainAt(pos(5, 5)))
        assertEquals(DoushouqiTerrain.LAND, terrainAt(pos(4, 3)))
        assertEquals(16, state.pieces().size)
        assertEquals(green(DoushouqiAnimal.ELEPHANT), state.pieceAt(pos(6, 0)))
        assertEquals(green(DoushouqiAnimal.LION), state.pieceAt(pos(8, 6)))
        assertEquals(red(DoushouqiAnimal.LION), state.pieceAt(pos(0, 0)))
        assertEquals(red(DoushouqiAnimal.ELEPHANT), state.pieceAt(pos(2, 6)))
        assertEquals(DoushouqiSide.PINE_GREEN, state.sideToMove)
        assertEquals(0, state.quietHalfMoves)
        assertNull(state.lastMove)
        assertNull(state.result)
    }

    @Test
    fun initialStateHasOneOfEveryAnimalForEachSide() {
        val pieces = DoushouqiState.initial().pieces()

        DoushouqiSide.entries.forEach { side ->
            assertEquals(
                DoushouqiAnimal.entries.toSet(),
                pieces.filter { it.second.side == side }.map { it.second.animal }.toSet(),
            )
        }
    }

    @Test
    fun exposedCollectionsCannotMutateState() {
        val state = DoushouqiState.initial()
        val exportedBoard = state.boardSnapshot().toMutableList()
        val exportedRepetitions = state.repetitionCounts.toMutableMap()

        exportedBoard.fill(null)
        exportedRepetitions.clear()

        assertEquals(16, state.pieces().size)
        assertEquals(1, state.repetitionCounts[state.positionKey])
    }

    @Test
    fun invalidCoordinatesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            DoushouqiPosition(-1, 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            terrainAt(DoushouqiPosition(9, 0))
        }
    }
}
