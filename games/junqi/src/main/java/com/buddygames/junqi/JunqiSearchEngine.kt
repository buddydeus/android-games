package com.buddygames.junqi

import kotlin.math.abs

internal data class JunqiRankedMove(
    val move: JunqiMove,
    val tacticalPriority: Int,
    val averageScore: Int,
    val worstScore: Int,
)

internal data class JunqiSearchResult(
    val rankedMoves: List<JunqiRankedMove>,
    val nodes: Int,
    val samplesCompleted: Int,
    val budgetExhausted: Boolean,
)

internal class JunqiSearchEngine(
    private val nanoTime: () -> Long = System::nanoTime,
) {
    fun search(
        observation: JunqiObservation,
        knowledge: JunqiKnowledge,
        level: JunqiAiLevel,
    ): JunqiSearchResult {
        require(observation.currentSide == observation.viewer) {
            "Junqi search can run only for the observation viewer's turn"
        }
        val budget = SearchBudget(
            nodeLimit = level.nodeBudget,
            deadlineNanos = nanoTime() + level.timeBudgetMillis * NANOS_PER_MILLISECOND,
            nanoTime = nanoTime,
        )
        if (observation.result != null) return JunqiSearchResult(emptyList(), 0, 0, false)

        val baseSeed = observation.deterministicHash() xor AI_PACKAGE_SALT xor level.level.toLong()
        var rootMoves = emptyList<JunqiMove>()
        var priorities = emptyMap<JunqiMove, Int>()
        val fallbackScores = linkedMapOf<JunqiMove, Int>()
        val aggregates = linkedMapOf<JunqiMove, ScoreAggregate>()
        var samplesCompleted = 0

        try {
            val firstAssignment = budget.timed {
                knowledge.sampleTypes(observation, sampleSeed(baseSeed, 0))
            }
            val firstState = observation.toSampleState(firstAssignment)
            rootMoves = budget.timed {
                JunqiRules.legalMoves(firstState, observation.viewer).sortedWith(moveComparator)
            }
            if (rootMoves.isEmpty()) return JunqiSearchResult(emptyList(), 0, 0, false)

            rootMoves.forEach { move -> aggregates[move] = ScoreAggregate() }
            priorities = JunqiTactics.rankedMoves(
                observation = observation,
                knowledge = knowledge,
                sampleCount = minOf(level.sampleCount, TACTICAL_SEARCH_SAMPLE_LIMIT),
                checkDeadline = budget::checkTime,
            ).associate { it.move to it.priority }
            for (move in rootMoves) {
                val moved = budget.timed { JunqiRules.applyMove(firstState, move) }
                fallbackScores[move] = budget.timed { evaluate(moved, observation.viewer) }
            }

            for (sampleIndex in 0 until level.sampleCount) {
                val assignment = budget.timed {
                    knowledge.sampleTypes(observation, sampleSeed(baseSeed, sampleIndex))
                }
                val state = observation.toSampleState(assignment)
                val completedScores = linkedMapOf<JunqiMove, Int>()
                for (move in rootMoves) {
                    val moved = budget.timed { JunqiRules.applyMove(state, move) }
                    val score = alphaBeta(
                        state = moved,
                        depth = level.searchDepth - 1,
                        alpha = -SEARCH_INFINITY,
                        beta = SEARCH_INFINITY,
                        rootSide = observation.viewer,
                        budget = budget,
                    )
                    completedScores[move] = score
                }
                for ((move, score) in completedScores) aggregates.getValue(move).add(score)
                samplesCompleted += 1
            }
        } catch (_: SearchBudgetExceeded) {
            // Return only work completed before the end-to-end deadline.
        }

        val rankedMoves = rootMoves.map { move ->
            val aggregate = aggregates[move]
            val fallbackScore = fallbackScores[move] ?: 0
            JunqiRankedMove(
                move = move,
                tacticalPriority = priorities[move] ?: 0,
                averageScore = aggregate?.averageOr(fallbackScore) ?: fallbackScore,
                worstScore = aggregate?.worstOr(fallbackScore) ?: fallbackScore,
            )
        }.sortedWith(
            compareByDescending<JunqiRankedMove> { it.tacticalPriority }
                .thenByDescending { it.averageScore }
                .thenByDescending { it.worstScore }
                .thenBy { it.move.from.row }
                .thenBy { it.move.from.column }
                .thenBy { it.move.to.row }
                .thenBy { it.move.to.column },
        )
        return JunqiSearchResult(
            rankedMoves = rankedMoves,
            nodes = budget.nodes,
            samplesCompleted = samplesCompleted,
            budgetExhausted = budget.exhausted,
        )
    }

    private fun alphaBeta(
        state: JunqiState,
        depth: Int,
        alpha: Int,
        beta: Int,
        rootSide: JunqiSide,
        budget: SearchBudget,
    ): Int {
        budget.visit()
        if (depth <= 0 || state.result != null) return budget.timed { evaluate(state, rootSide) }
        val moves = budget.timed {
            JunqiRules.legalMoves(state, state.currentSide).sortedWith(moveComparator)
        }
        if (moves.isEmpty()) return budget.timed { evaluate(state, rootSide) }

        return if (state.currentSide == rootSide) {
            var best = -SEARCH_INFINITY
            var lowerBound = alpha
            for (move in moves) {
                val moved = budget.timed { JunqiRules.applyMove(state, move) }
                best = maxOf(
                    best,
                    alphaBeta(moved, depth - 1, lowerBound, beta, rootSide, budget),
                )
                lowerBound = maxOf(lowerBound, best)
                if (lowerBound >= beta) break
            }
            best
        } else {
            var best = SEARCH_INFINITY
            var upperBound = beta
            for (move in moves) {
                val moved = budget.timed { JunqiRules.applyMove(state, move) }
                best = minOf(
                    best,
                    alphaBeta(moved, depth - 1, alpha, upperBound, rootSide, budget),
                )
                upperBound = minOf(upperBound, best)
                if (alpha >= upperBound) break
            }
            best
        }
    }

    private fun evaluate(state: JunqiState, rootSide: JunqiSide): Int {
        state.result?.let { result ->
            return when (result.winner) {
                rootSide -> TERMINAL_SCORE
                null -> 0
                else -> -TERMINAL_SCORE
            }
        }

        var score = 0
        for (piece in state.pieces.values) {
            val sign = if (piece.side == rootSide) 1 else -1
            score += sign * pieceValues.getValue(piece.type)
            if (piece.position in JunqiBoard.camps) score += sign * CAMP_CONTROL_SCORE
            if (JunqiBoard.railNeighbors.getValue(piece.position).isNotEmpty()) score += sign * RAIL_CONTROL_SCORE
        }
        score += mobility(state, rootSide) * MOBILITY_SCORE
        score -= mobility(state, other(rootSide)) * MOBILITY_SCORE
        score += flagPressure(state, rootSide)
        if (state.quietHalfMoves >= QUIET_RISK_START) {
            val risk = (state.quietHalfMoves - QUIET_RISK_START + 1) * QUIET_RISK_SCORE
            score += if (state.currentSide == rootSide) -risk else risk
        }
        return score
    }

    private fun mobility(state: JunqiState, side: JunqiSide): Int = JunqiRules.legalMoves(state, side).size

    private fun flagPressure(state: JunqiState, rootSide: JunqiSide): Int {
        val ownFlag = state.pieces.values.singleOrNull { it.side == rootSide && it.type == JunqiPieceType.FLAG }
        val enemyFlag = state.pieces.values.singleOrNull { it.side != rootSide && it.type == JunqiPieceType.FLAG }
        val ownMovers = state.pieces.values.filter { it.side == rootSide && it.type.movable }
        val enemyMovers = state.pieces.values.filter { it.side != rootSide && it.type.movable }
        var score = 0
        if (enemyFlag != null && ownMovers.isNotEmpty()) {
            score -= ownMovers.minOf { distance(it.position, enemyFlag.position) } * FLAG_DISTANCE_SCORE
        }
        if (ownFlag != null && enemyMovers.isNotEmpty()) {
            score += enemyMovers.minOf { distance(it.position, ownFlag.position) } * FLAG_DISTANCE_SCORE
        }
        return score
    }

    private fun distance(first: JunqiPosition, second: JunqiPosition): Int =
        abs(first.row - second.row) + abs(first.column - second.column)

    private class ScoreAggregate {
        private var total = 0L
        private var count = 0
        private var worst = Int.MAX_VALUE

        fun add(score: Int) {
            total += score
            count += 1
            worst = minOf(worst, score)
        }

        fun averageOr(fallback: Int): Int = if (count == 0) fallback else (total / count).toInt()

        fun worstOr(fallback: Int): Int = if (count == 0) fallback else worst
    }

    private class SearchBudget(
        private val nodeLimit: Int,
        private val deadlineNanos: Long,
        private val nanoTime: () -> Long,
    ) {
        var nodes: Int = 0
            private set
        var exhausted: Boolean = false
            private set

        fun checkTime() {
            if (nanoTime() >= deadlineNanos) {
                exhausted = true
                throw SearchBudgetExceeded
            }
        }

        fun <T> timed(block: () -> T): T {
            checkTime()
            val result = block()
            checkTime()
            return result
        }

        fun visit() {
            if (nodes >= nodeLimit) {
                exhausted = true
                throw SearchBudgetExceeded
            }
            checkTime()
            nodes += 1
        }
    }

    private object SearchBudgetExceeded : RuntimeException()

    private companion object {
        const val NANOS_PER_MILLISECOND = 1_000_000L
        const val SEARCH_INFINITY = 2_000_000
        const val TERMINAL_SCORE = 1_000_000
        const val CAMP_CONTROL_SCORE = 18
        const val RAIL_CONTROL_SCORE = 8
        const val MOBILITY_SCORE = 2
        const val FLAG_DISTANCE_SCORE = 12
        const val QUIET_RISK_START = 24
        const val QUIET_RISK_SCORE = 25
        const val TACTICAL_SEARCH_SAMPLE_LIMIT = 8

        val pieceValues = mapOf(
            JunqiPieceType.COMMANDER to 900,
            JunqiPieceType.ARMY_COMMANDER to 800,
            JunqiPieceType.DIVISION_COMMANDER to 700,
            JunqiPieceType.BRIGADE_COMMANDER to 600,
            JunqiPieceType.REGIMENT to 500,
            JunqiPieceType.BATTALION to 400,
            JunqiPieceType.COMPANY to 300,
            JunqiPieceType.PLATOON to 220,
            JunqiPieceType.ENGINEER to 280,
            JunqiPieceType.BOMB to 520,
            JunqiPieceType.MINE to 260,
            JunqiPieceType.FLAG to 4_000,
        )
    }
}

internal fun JunqiObservation.toSampleState(
    opponentTypes: Map<String, JunqiPieceType>,
): JunqiState {
    val own = ownPieces.map { piece ->
        JunqiPiece(piece.id, viewer, piece.type, piece.position, piece.hasMoved)
    }
    val opponentSide = other(viewer)
    val opponents = opponentPieces.map { piece ->
        val type = requireNotNull(opponentTypes[piece.id]) { "Missing sampled type for ${piece.id}" }
        JunqiPiece(piece.id, opponentSide, type, piece.position, piece.hasMoved)
    }
    return JunqiState(
        pieces = (own + opponents).associateBy { it.position },
        currentSide = currentSide,
        revealedFlags = revealedFlags,
        quietHalfMoves = quietHalfMoves,
        result = result,
    )
}

private fun sampleSeed(baseSeed: Long, sampleIndex: Int): Long {
    var value = baseSeed + (sampleIndex + 1L) * -7_046_029_254_386_353_131L
    value = (value xor (value ushr 30)) * -4_658_895_280_553_007_687L
    value = (value xor (value ushr 27)) * -7_723_592_293_110_705_685L
    return value xor (value ushr 31)
}
