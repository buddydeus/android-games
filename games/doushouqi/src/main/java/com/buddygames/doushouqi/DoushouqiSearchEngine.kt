package com.buddygames.doushouqi

import kotlin.math.abs

object DoushouqiSearchEngine {
    fun search(
        state: DoushouqiState,
        level: DoushouqiAiLevel,
        nanoTime: () -> Long = System::nanoTime,
    ): DoushouqiSearchResult? {
        val legalMoves = DoushouqiRules.legalMoves(state)
        if (legalMoves.isEmpty()) return null
        val fallback = orderedMoves(state, legalMoves).first()
        val start = nanoTime()
        val budgetNanos = level.timeBudgetMillis * 1_000_000L
        val deadline = if (Long.MAX_VALUE - start < budgetNanos) {
            Long.MAX_VALUE
        } else {
            start + budgetNanos
        }
        val context = SearchContext(
            rootSide = state.sideToMove,
            level = level,
            deadline = deadline,
            nanoTime = nanoTime,
        )
        var completedDepth = 0
        var completedScores: List<ScoredMove> = emptyList()
        var timedOut = false
        for (depth in 1..level.maxDepth) {
            try {
                val scores = context.searchRoot(state, depth)
                completedScores = scores
                completedDepth = depth
            } catch (stopped: SearchStopped) {
                timedOut = stopped.timedOut
                break
            }
        }
        val selected = if (completedScores.isEmpty()) {
            fallback
        } else {
            selectWeakenedMove(state, level, completedScores)
        }
        return DoushouqiSearchResult(
            move = selected,
            completedDepth = completedDepth,
            nodes = context.nodes,
            timedOut = timedOut,
        )
    }

    private class SearchContext(
        val rootSide: DoushouqiSide,
        val level: DoushouqiAiLevel,
        val deadline: Long,
        val nanoTime: () -> Long,
    ) {
        var nodes: Int = 0
            private set
        private val transpositions = LinkedHashMap<Long, TranspositionEntry>()

        fun searchRoot(
            state: DoushouqiState,
            depth: Int,
        ): List<ScoredMove> {
            val scores = mutableListOf<ScoredMove>()
            var alpha = -INFINITY
            val ordered = orderedMoves(state, DoushouqiRules.legalMoves(state))
            ordered.forEach { move ->
                checkBudget()
                val child = requireNotNull(DoushouqiRules.apply(state, move))
                val score = minimax(
                    state = child,
                    depth = depth - 1,
                    alphaStart = alpha,
                    betaStart = INFINITY,
                    ply = 1,
                    extensionRemaining = level.tacticalExtension,
                )
                scores += ScoredMove(move, score, moveOrderScore(state, move))
                if (score > alpha) alpha = score
            }
            return scores.sortedWith(
                compareByDescending<ScoredMove> { it.score }
                    .thenByDescending { it.orderScore }
                    .thenBy { it.move.from.row }
                    .thenBy { it.move.from.column }
                    .thenBy { it.move.to.row }
                    .thenBy { it.move.to.column },
            )
        }

        private fun minimax(
            state: DoushouqiState,
            depth: Int,
            alphaStart: Int,
            betaStart: Int,
            ply: Int,
            extensionRemaining: Int,
        ): Int {
            checkBudget()
            nodes++
            terminalScore(state, rootSide, ply)?.let { return it }
            val tacticalMoves = if (depth <= 0 && extensionRemaining > 0) {
                orderedMoves(state, DoushouqiRules.legalMoves(state))
                    .filter { isTactical(state, it) }
            } else {
                emptyList()
            }
            if (depth <= 0 && tacticalMoves.isEmpty()) return evaluate(state, rootSide)
            val effectiveDepth = if (depth > 0) depth else 1
            val nextExtension = if (depth > 0) extensionRemaining else extensionRemaining - 1
            val key = transpositionKey(state, effectiveDepth, nextExtension)
            transpositions[key]?.takeIf { it.depth >= effectiveDepth }?.let { return it.score }
            val moves = if (tacticalMoves.isEmpty()) {
                orderedMoves(state, DoushouqiRules.legalMoves(state))
            } else {
                tacticalMoves
            }
            if (moves.isEmpty()) return evaluate(state, rootSide)
            val maximizing = state.sideToMove == rootSide
            var alpha = alphaStart
            var beta = betaStart
            var best = if (maximizing) -INFINITY else INFINITY
            for (move in moves) {
                val child = requireNotNull(DoushouqiRules.apply(state, move))
                val score = minimax(
                    state = child,
                    depth = effectiveDepth - 1,
                    alphaStart = alpha,
                    betaStart = beta,
                    ply = ply + 1,
                    extensionRemaining = nextExtension,
                )
                if (maximizing) {
                    if (score > best) best = score
                    if (best > alpha) alpha = best
                } else {
                    if (score < best) best = score
                    if (best < beta) beta = best
                }
                if (alpha >= beta) break
            }
            if (transpositions.size >= TRANSPOSITION_CAPACITY) {
                val first = transpositions.entries.firstOrNull()?.key
                if (first != null) transpositions.remove(first)
            }
            transpositions[key] = TranspositionEntry(effectiveDepth, best)
            return best
        }

        private fun checkBudget() {
            if (nodes >= level.nodeBudget) throw SearchStopped(timedOut = false)
            if (nanoTime() >= deadline) throw SearchStopped(timedOut = true)
        }
    }

    private fun orderedMoves(
        state: DoushouqiState,
        moves: List<DoushouqiMove>,
    ): List<DoushouqiMove> = moves.sortedWith(
        compareByDescending<DoushouqiMove> { moveOrderScore(state, it) }
            .thenBy { it.from.row }
            .thenBy { it.from.column }
            .thenBy { it.to.row }
            .thenBy { it.to.column },
    )

    private fun moveOrderScore(
        state: DoushouqiState,
        move: DoushouqiMove,
    ): Int {
        val moving = requireNotNull(state.pieceAt(move.from))
        val captured = state.pieceAt(move.to)
        val applied = requireNotNull(DoushouqiRules.apply(state, move))
        val result = applied.result
        if (result is DoushouqiResult.Win && result.winner == moving.side) return MATE_SCORE
        var score = captured?.let { pieceValue(it.animal) * 1_000 } ?: 0
        if (captured?.animal == DoushouqiAnimal.ELEPHANT &&
            moving.animal == DoushouqiAnimal.RAT
        ) {
            score += 100_000
        }
        val enemyDen = if (moving.side == DoushouqiSide.PINE_GREEN) {
            DoushouqiPosition(0, 3)
        } else {
            DoushouqiPosition(8, 3)
        }
        score += (
            manhattan(move.from, enemyDen) -
                manhattan(move.to, enemyDen)
            ) * 100
        if (trapOwner(move.to) == moving.side) score += 15_000
        return score
    }

    private fun selectWeakenedMove(
        state: DoushouqiState,
        level: DoushouqiAiLevel,
        scores: List<ScoredMove>,
    ): DoushouqiMove {
        val best = scores.first()
        if (
            level.weakeningPercent == 0 ||
            level.weakeningPool <= 1 ||
            scores.size == 1 ||
            best.score - scores[1].score >= DECISIVE_MARGIN
        ) {
            return best.move
        }
        val pool = minOf(level.weakeningPool, scores.size)
        val mixed = stableMix(state.positionKey xor level.level.toLong())
        val roll = ((mixed ushr 1) % 100).toInt()
        if (roll >= level.weakeningPercent) return best.move
        val rank = 1 + (((mixed ushr 9) % (pool - 1)).toInt())
        return scores[rank].move
    }

    private fun evaluate(
        state: DoushouqiState,
        rootSide: DoushouqiSide,
    ): Int {
        var score = 0
        state.pieces().forEach { (position, piece) ->
            val sign = if (piece.side == rootSide) 1 else -1
            val enemyDen = if (piece.side == DoushouqiSide.PINE_GREEN) {
                DoushouqiPosition(0, 3)
            } else {
                DoushouqiPosition(8, 3)
            }
            var value = pieceValue(piece.animal)
            value += (12 - manhattan(position, enemyDen)) * 12
            if (
                piece.animal == DoushouqiAnimal.RAT &&
                terrainAt(position) == DoushouqiTerrain.RIVER
            ) {
                value += 45
            }
            if (trapOwner(position) == piece.side.other()) value -= 120
            score += sign * value
        }
        val rootMobility = DoushouqiRules.legalMoves(
            state.copyWith(sideToMove = rootSide, result = null),
        ).size
        val opponentMobility = DoushouqiRules.legalMoves(
            state.copyWith(sideToMove = rootSide.other(), result = null),
        ).size
        return score + (rootMobility - opponentMobility) * 4
    }

    private fun terminalScore(
        state: DoushouqiState,
        rootSide: DoushouqiSide,
        ply: Int,
    ): Int? = when (val result = state.result) {
        is DoushouqiResult.Win ->
            if (result.winner == rootSide) MATE_SCORE - ply else -MATE_SCORE + ply
        is DoushouqiResult.Draw -> 0
        null -> null
    }

    private fun isTactical(
        state: DoushouqiState,
        move: DoushouqiMove,
    ): Boolean {
        if (state.pieceAt(move.to) != null) return true
        val moving = requireNotNull(state.pieceAt(move.from))
        val enemyDen = if (moving.side == DoushouqiSide.PINE_GREEN) {
            DoushouqiPosition(0, 3)
        } else {
            DoushouqiPosition(8, 3)
        }
        return move.to == enemyDen || manhattan(move.to, enemyDen) == 1
    }

    private fun transpositionKey(
        state: DoushouqiState,
        depth: Int,
        extension: Int,
    ): Long {
        var hash = state.positionKey xor (state.quietHalfMoves.toLong() shl 48)
        state.repetitionCounts.entries.sortedBy { it.key }.forEach { (key, count) ->
            hash = stableMix(hash xor key xor count.toLong())
        }
        return stableMix(hash xor (depth.toLong() shl 8) xor extension.toLong())
    }

    private fun stableMix(value: Long): Long {
        var mixed = value
        mixed = (mixed xor (mixed ushr 30)) * -4658895280553007687L
        mixed = (mixed xor (mixed ushr 27)) * -7723592293110705685L
        return mixed xor (mixed ushr 31)
    }

    private fun pieceValue(animal: DoushouqiAnimal): Int = when (animal) {
        DoushouqiAnimal.ELEPHANT -> 900
        DoushouqiAnimal.LION -> 760
        DoushouqiAnimal.TIGER -> 700
        DoushouqiAnimal.LEOPARD -> 520
        DoushouqiAnimal.WOLF -> 430
        DoushouqiAnimal.DOG -> 340
        DoushouqiAnimal.CAT -> 250
        DoushouqiAnimal.RAT -> 220
    }

    private fun manhattan(
        first: DoushouqiPosition,
        second: DoushouqiPosition,
    ): Int = abs(first.row - second.row) + abs(first.column - second.column)

    private data class ScoredMove(
        val move: DoushouqiMove,
        val score: Int,
        val orderScore: Int,
    )

    private data class TranspositionEntry(
        val depth: Int,
        val score: Int,
    )

    private class SearchStopped(val timedOut: Boolean) : RuntimeException()

    private const val MATE_SCORE = 1_000_000
    private const val INFINITY = 2_000_000
    private const val DECISIVE_MARGIN = 5_000
    private const val TRANSPOSITION_CAPACITY = 32_768
}
