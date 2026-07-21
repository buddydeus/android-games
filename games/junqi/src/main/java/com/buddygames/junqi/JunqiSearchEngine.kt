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
        if (observation.result != null) return JunqiSearchResult(emptyList(), 0, 0, false)

        val baseSeed = observation.deterministicHash() xor AI_PACKAGE_SALT xor level.level.toLong()
        val firstAssignment = knowledge.sampleTypes(sampleSeed(baseSeed, 0))
        val firstState = observation.toSampleState(firstAssignment)
        val rootMoves = JunqiRules.legalMoves(firstState, observation.viewer).sortedWith(moveComparator)
        if (rootMoves.isEmpty()) return JunqiSearchResult(emptyList(), 0, 0, false)

        val priorities = JunqiTactics.rankedMoves(observation, knowledge).associate { it.move to it.priority }
        val fallbackScores = rootMoves.associateWith { move ->
            evaluate(JunqiRules.applyMove(firstState, move), observation.viewer)
        }
        val aggregates = rootMoves.associateWith { ScoreAggregate() }
        val budget = SearchBudget(
            nodeLimit = level.nodeBudget,
            deadlineNanos = nanoTime() + level.timeBudgetMillis * NANOS_PER_MILLISECOND,
            nanoTime = nanoTime,
        )
        var samplesCompleted = 0

        sampleLoop@ for (sampleIndex in 0 until level.sampleCount) {
            val assignment = knowledge.sampleTypes(sampleSeed(baseSeed, sampleIndex))
            val state = observation.toSampleState(assignment)
            val completedScores = linkedMapOf<JunqiMove, Int>()
            for (move in rootMoves) {
                val score = try {
                    alphaBeta(
                        state = JunqiRules.applyMove(state, move),
                        depth = level.searchDepth - 1,
                        alpha = -SEARCH_INFINITY,
                        beta = SEARCH_INFINITY,
                        rootSide = observation.viewer,
                        budget = budget,
                    )
                } catch (_: SearchBudgetExceeded) {
                    break@sampleLoop
                }
                completedScores[move] = score
            }
            for ((move, score) in completedScores) aggregates.getValue(move).add(score)
            samplesCompleted += 1
        }

        val rankedMoves = rootMoves.map { move ->
            val aggregate = aggregates.getValue(move)
            JunqiRankedMove(
                move = move,
                tacticalPriority = priorities.getValue(move),
                averageScore = aggregate.averageOr(fallbackScores.getValue(move)),
                worstScore = aggregate.worstOr(fallbackScores.getValue(move)),
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
        if (depth <= 0 || state.result != null) return evaluate(state, rootSide)
        val moves = JunqiRules.legalMoves(state, state.currentSide).sortedWith(moveComparator)
        if (moves.isEmpty()) return evaluate(state, rootSide)

        return if (state.currentSide == rootSide) {
            var best = -SEARCH_INFINITY
            var lowerBound = alpha
            for (move in moves) {
                best = maxOf(
                    best,
                    alphaBeta(JunqiRules.applyMove(state, move), depth - 1, lowerBound, beta, rootSide, budget),
                )
                lowerBound = maxOf(lowerBound, best)
                if (lowerBound >= beta) break
            }
            best
        } else {
            var best = SEARCH_INFINITY
            var upperBound = beta
            for (move in moves) {
                best = minOf(
                    best,
                    alphaBeta(JunqiRules.applyMove(state, move), depth - 1, alpha, upperBound, rootSide, budget),
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

        fun visit() {
            if (nodes >= nodeLimit || nanoTime() >= deadlineNanos) {
                exhausted = true
                throw SearchBudgetExceeded
            }
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
