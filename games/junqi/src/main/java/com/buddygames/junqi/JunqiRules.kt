package com.buddygames.junqi

import java.util.ArrayDeque

object JunqiRules {
    fun legalMoves(state: JunqiState, side: JunqiSide): List<JunqiMove> {
        val moves = linkedSetOf<JunqiMove>()
        for ((position, piece) in state.pieces) {
            if (piece.side != side || !piece.type.movable || position in JunqiBoard.headquarters) continue

            JunqiBoard.roadNeighbors.getValue(position)
                .filter { destination -> canLand(state, side, destination) }
                .forEach { destination -> moves += JunqiMove(position, destination) }

            val railwayDestinations = if (piece.type == JunqiPieceType.ENGINEER) {
                engineerRailwayDestinations(state, side, position)
            } else {
                straightRailwayDestinations(state, side, position)
            }
            railwayDestinations.forEach { destination -> moves += JunqiMove(position, destination) }
        }
        return moves.toList()
    }

    private fun engineerRailwayDestinations(
        state: JunqiState,
        side: JunqiSide,
        from: JunqiPosition,
    ): Set<JunqiPosition> {
        val destinations = linkedSetOf<JunqiPosition>()
        val visited = mutableSetOf(from)
        val queue = ArrayDeque<JunqiPosition>()
        queue.addLast(from)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            for (next in JunqiBoard.railNeighbors.getValue(current)) {
                if (!visited.add(next)) continue
                if (!canLand(state, side, next)) continue

                destinations += next
                if (next !in state.pieces) queue.addLast(next)
            }
        }
        return destinations
    }

    private fun straightRailwayDestinations(
        state: JunqiState,
        side: JunqiSide,
        from: JunqiPosition,
    ): Set<JunqiPosition> {
        val destinations = linkedSetOf<JunqiPosition>()
        for (next in JunqiBoard.railNeighbors.getValue(from)) {
            val rowDirection = next.row - from.row
            val columnDirection = next.column - from.column
            var current = next

            while (true) {
                if (!canLand(state, side, current)) break
                destinations += current
                if (current in state.pieces) break

                current = JunqiBoard.railNeighbors.getValue(current).firstOrNull { candidate ->
                    candidate.row - current.row == rowDirection &&
                        candidate.column - current.column == columnDirection
                } ?: break
            }
        }
        return destinations
    }

    private fun canLand(state: JunqiState, side: JunqiSide, destination: JunqiPosition): Boolean {
        val occupant = state.pieces[destination] ?: return true
        return occupant.side != side && destination !in JunqiBoard.camps
    }
}
