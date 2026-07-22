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

    fun battleOutcome(attacker: JunqiPieceType, defender: JunqiPieceType): JunqiBattleOutcome {
        require(attacker.movable) { "Immobile Junqi piece cannot attack: $attacker" }
        if (attacker == JunqiPieceType.BOMB || defender == JunqiPieceType.BOMB) {
            return JunqiBattleOutcome.BOTH_REMOVED
        }
        if (defender == JunqiPieceType.MINE) {
            return if (attacker == JunqiPieceType.ENGINEER) {
                JunqiBattleOutcome.ATTACKER_WINS
            } else {
                JunqiBattleOutcome.DEFENDER_WINS
            }
        }
        if (defender == JunqiPieceType.FLAG) return JunqiBattleOutcome.ATTACKER_WINS

        val attackerRank = ranks[attacker]
        val defenderRank = ranks[defender]
        require(attackerRank != null && defenderRank != null) {
            "Illegal Junqi battle: $attacker attacking $defender"
        }
        return when {
            attackerRank > defenderRank -> JunqiBattleOutcome.ATTACKER_WINS
            attackerRank < defenderRank -> JunqiBattleOutcome.DEFENDER_WINS
            else -> JunqiBattleOutcome.BOTH_REMOVED
        }
    }

    fun applyMove(state: JunqiState, move: JunqiMove): JunqiState {
        check(state.result == null) { "Cannot move after the Junqi game has finished" }
        require(move in legalMoves(state, state.currentSide)) { "Illegal Junqi move: $move" }

        val attacker = state.pieces.getValue(move.from)
        val defender = state.pieces[move.to]
        val pieces = state.pieces.toMutableMap()
        pieces.remove(move.from)

        val outcome = defender?.let { battleOutcome(attacker.type, it.type) }
        when (outcome) {
            null -> pieces[move.to] = attacker.copy(position = move.to, hasMoved = true)
            JunqiBattleOutcome.ATTACKER_WINS -> {
                pieces.remove(move.to)
                pieces[move.to] = attacker.copy(position = move.to, hasMoved = true)
            }
            JunqiBattleOutcome.DEFENDER_WINS -> Unit
            JunqiBattleOutcome.BOTH_REMOVED -> pieces.remove(move.to)
        }

        val revealedFlags = state.revealedFlags.toMutableSet()
        if (attacker.type == JunqiPieceType.COMMANDER && outcome != null && outcome != JunqiBattleOutcome.ATTACKER_WINS) {
            revealedFlags += attacker.side
        }
        if (defender?.type == JunqiPieceType.COMMANDER && outcome != JunqiBattleOutcome.DEFENDER_WINS) {
            revealedFlags += defender.side
        }

        val quietHalfMoves = if (defender == null) state.quietHalfMoves + 1 else 0
        val nextSide = other(state.currentSide)
        val immediateResult = when {
            defender?.type == JunqiPieceType.FLAG -> JunqiResult.fromWinner(attacker.side)
            quietHalfMoves >= QUIET_HALF_MOVE_LIMIT -> JunqiResult.fromWinner(nextSide)
            else -> null
        }
        val moved = JunqiState(
            pieces = pieces,
            currentSide = nextSide,
            revealedFlags = revealedFlags,
            quietHalfMoves = quietHalfMoves,
            result = immediateResult,
            lastBattleOutcome = outcome,
        )
        if (immediateResult != null) return moved

        return JunqiState(
            pieces = moved.pieces,
            currentSide = moved.currentSide,
            revealedFlags = moved.revealedFlags,
            quietHalfMoves = moved.quietHalfMoves,
            result = terminalResult(moved),
            lastBattleOutcome = moved.lastBattleOutcome,
        )
    }

    fun terminalResult(state: JunqiState): JunqiResult? {
        state.result?.let { return it }
        if (legalMoves(state, state.currentSide).isNotEmpty()) return null
        return if (legalMoves(state, other(state.currentSide)).isEmpty()) {
            JunqiResult.DRAW
        } else {
            JunqiResult.fromWinner(other(state.currentSide))
        }
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

    private fun other(side: JunqiSide): JunqiSide =
        if (side == JunqiSide.RED) JunqiSide.BLUE else JunqiSide.RED

    private const val QUIET_HALF_MOVE_LIMIT = 31

    private val ranks = mapOf(
        JunqiPieceType.COMMANDER to 9,
        JunqiPieceType.ARMY_COMMANDER to 8,
        JunqiPieceType.DIVISION_COMMANDER to 7,
        JunqiPieceType.BRIGADE_COMMANDER to 6,
        JunqiPieceType.REGIMENT to 5,
        JunqiPieceType.BATTALION to 4,
        JunqiPieceType.COMPANY to 3,
        JunqiPieceType.PLATOON to 2,
        JunqiPieceType.ENGINEER to 1,
    )
}
