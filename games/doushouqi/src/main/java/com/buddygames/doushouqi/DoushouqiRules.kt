package com.buddygames.doushouqi

object DoushouqiRules {
    fun legalMoves(state: DoushouqiState): List<DoushouqiMove> {
        if (state.result != null) return emptyList()
        val moves = mutableListOf<DoushouqiMove>()
        state.pieces()
            .asSequence()
            .filter { (_, piece) -> piece.side == state.sideToMove }
            .forEach { (from, piece) ->
                DIRECTIONS.forEach { (rowDelta, columnDelta) ->
                    candidateMove(state, from, piece, rowDelta, columnDelta)
                        ?.let(moves::add)
                }
            }
        return moves.sortedWith(
            compareBy(
                { it.from.row },
                { it.from.column },
                { it.to.row },
                { it.to.column },
            ),
        )
    }

    private fun candidateMove(
        state: DoushouqiState,
        from: DoushouqiPosition,
        piece: DoushouqiPiece,
        rowDelta: Int,
        columnDelta: Int,
    ): DoushouqiMove? {
        val adjacent = positionOrNull(from.row + rowDelta, from.column + columnDelta)
            ?: return null
        val destination = if (
            terrainAt(adjacent) == DoushouqiTerrain.RIVER &&
            piece.animal in JUMPING_ANIMALS
        ) {
            jumpDestination(state, adjacent, rowDelta, columnDelta)
        } else {
            adjacent
        } ?: return null

        if (denOwner(destination) == piece.side) return null
        if (
            terrainAt(destination) == DoushouqiTerrain.RIVER &&
            piece.animal != DoushouqiAnimal.RAT
        ) {
            return null
        }
        val occupant = state.pieceAt(destination)
        if (occupant?.side == piece.side) return null
        if (
            occupant != null &&
            !canCapture(
                attacker = piece,
                from = from,
                defender = occupant,
                to = destination,
            )
        ) {
            return null
        }
        return DoushouqiMove(from, destination)
    }

    private fun jumpDestination(
        state: DoushouqiState,
        firstRiver: DoushouqiPosition,
        rowDelta: Int,
        columnDelta: Int,
    ): DoushouqiPosition? {
        var current = firstRiver
        while (terrainAt(current) == DoushouqiTerrain.RIVER) {
            if (state.pieceAt(current)?.animal == DoushouqiAnimal.RAT) return null
            current = positionOrNull(
                current.row + rowDelta,
                current.column + columnDelta,
            ) ?: return null
        }
        return current
    }

    private fun positionOrNull(row: Int, column: Int): DoushouqiPosition? =
        if (row in 0 until DoushouqiState.ROWS && column in 0 until DoushouqiState.COLUMNS) {
            DoushouqiPosition(row, column)
        } else {
            null
        }

    internal fun canCapture(
        attacker: DoushouqiPiece,
        from: DoushouqiPosition,
        defender: DoushouqiPiece,
        to: DoushouqiPosition,
    ): Boolean {
        if (attacker.side == defender.side) return false
        val fromIsRiver = terrainAt(from) == DoushouqiTerrain.RIVER
        val toIsRiver = terrainAt(to) == DoushouqiTerrain.RIVER
        if (
            (attacker.animal == DoushouqiAnimal.RAT ||
                defender.animal == DoushouqiAnimal.RAT) &&
            fromIsRiver != toIsRiver
        ) {
            return false
        }
        if (trapOwner(to) == attacker.side) return true
        if (
            attacker.animal == DoushouqiAnimal.RAT &&
            defender.animal == DoushouqiAnimal.ELEPHANT
        ) {
            return !fromIsRiver && !toIsRiver
        }
        if (
            attacker.animal == DoushouqiAnimal.ELEPHANT &&
            defender.animal == DoushouqiAnimal.RAT
        ) {
            return false
        }
        return attacker.animal.rank >= defender.animal.rank
    }

    private val DIRECTIONS = listOf(
        -1 to 0,
        0 to -1,
        0 to 1,
        1 to 0,
    )

    private val JUMPING_ANIMALS = setOf(
        DoushouqiAnimal.LION,
        DoushouqiAnimal.TIGER,
    )
}
