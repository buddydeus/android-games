package com.buddygames.doushouqi

import kotlin.math.abs

object DoushouqiSearchEngine {
    fun search(
        state: DoushouqiState,
        level: DoushouqiAiLevel,
        nanoTime: () -> Long = System::nanoTime,
        shouldStop: () -> Boolean = { false },
    ): DoushouqiSearchResult? {
        val position = DoushouqiSearchPosition(state)
        val legalMoves = position.legalMoves()
        if (legalMoves.isEmpty()) return null
        val fallback = orderMoves(position, legalMoves, null, null, null).first()
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
            shouldStop = shouldStop,
        )
        var completedDepth = 0
        var completedScores: List<ScoredMove> = emptyList()
        var timedOut = false
        for (depth in 1..level.maxDepth) {
            try {
                val scores = context.searchRoot(position, depth)
                completedScores = scores
                completedDepth = depth
                context.principalVariation[0] = scores.firstOrNull()?.move
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
        val shouldStop: () -> Boolean,
    ) {
        var nodes: Int = 0
            private set
        val principalVariation = arrayOfNulls<DoushouqiMove>(MAX_PLY)
        private val killerMoves = arrayOfNulls<DoushouqiMove>(MAX_PLY)
        private val history = IntArray(
            DoushouqiState.SQUARES * DoushouqiState.SQUARES,
        )
        private val transpositions = LinkedHashMap<Long, TranspositionEntry>()

        fun searchRoot(
            position: DoushouqiSearchPosition,
            depth: Int,
        ): List<ScoredMove> {
            val scores = mutableListOf<ScoredMove>()
            var alpha = -INFINITY
            val ordered = orderMoves(
                position,
                position.legalMoves(),
                principalVariation[0],
                killerMoves[0],
                history,
            )
            ordered.forEach { move ->
                checkBudget()
                val orderScore = moveOrderScore(
                    position,
                    move,
                    principalVariation[0],
                    killerMoves[0],
                    history,
                )
                val undo = position.make(move)
                val score = try {
                    minimax(
                        position = position,
                        depth = depth - 1,
                        alphaStart = alpha,
                        betaStart = INFINITY,
                        ply = 1,
                        extensionRemaining = level.tacticalExtension,
                    )
                } finally {
                    position.unmake(move, undo)
                }
                scores += ScoredMove(move, score, orderScore)
                if (score > alpha) alpha = score
            }
            return scores.sortedWith(SCORED_MOVE_COMPARATOR)
        }

        private fun minimax(
            position: DoushouqiSearchPosition,
            depth: Int,
            alphaStart: Int,
            betaStart: Int,
            ply: Int,
            extensionRemaining: Int,
        ): Int {
            checkBudget()
            nodes++
            terminalScore(position.result, rootSide, ply)?.let { return it }
            val allMoves = position.legalMoves()
            val tacticalMoves = if (depth <= 0 && extensionRemaining > 0) {
                allMoves.filter { isTactical(position, it) }
            } else {
                emptyList()
            }
            if (depth <= 0 && tacticalMoves.isEmpty()) return evaluate(position, rootSide)
            val effectiveDepth = if (depth > 0) depth else 1
            val nextExtension = if (depth > 0) extensionRemaining else extensionRemaining - 1
            val key = transpositionKey(position, nextExtension)
            transpositions[key]?.takeIf { it.depth >= effectiveDepth }?.let {
                return it.score
            }
            val moves = if (tacticalMoves.isEmpty()) allMoves else tacticalMoves
            if (moves.isEmpty()) return evaluate(position, rootSide)
            val maximizing = position.sideToMove == rootSide
            var alpha = alphaStart
            var beta = betaStart
            var best = if (maximizing) -INFINITY else INFINITY
            var cutoff = false
            val ordered = orderMoves(
                position,
                moves,
                principalVariation.getOrNull(ply),
                killerMoves.getOrNull(ply),
                history,
            )
            for (move in ordered) {
                val undo = position.make(move)
                val score = try {
                    minimax(
                        position = position,
                        depth = effectiveDepth - 1,
                        alphaStart = alpha,
                        betaStart = beta,
                        ply = ply + 1,
                        extensionRemaining = nextExtension,
                    )
                } finally {
                    position.unmake(move, undo)
                }
                if (maximizing) {
                    if (score > best) best = score
                    if (best > alpha) alpha = best
                } else {
                    if (score < best) best = score
                    if (best < beta) beta = best
                }
                if (alpha >= beta) {
                    cutoff = true
                    if (ply < killerMoves.size && position.pieceAt(move.to) == null) {
                        killerMoves[ply] = move
                        val index = move.from.index * DoushouqiState.SQUARES + move.to.index
                        history[index] = (history[index] + effectiveDepth * effectiveDepth)
                            .coerceAtMost(HISTORY_MAX)
                    }
                    break
                }
            }
            if (!cutoff) storeTransposition(key, effectiveDepth, best)
            return best
        }

        private fun storeTransposition(key: Long, depth: Int, score: Int) {
            if (transpositions.size >= TRANSPOSITION_CAPACITY) {
                transpositions.entries.firstOrNull()?.key?.let(transpositions::remove)
            }
            transpositions[key] = TranspositionEntry(depth, score)
        }

        private fun checkBudget() {
            if (shouldStop()) throw SearchStopped(timedOut = false)
            if (nodes >= level.nodeBudget) throw SearchStopped(timedOut = false)
            if (nanoTime() >= deadline) throw SearchStopped(timedOut = true)
        }
    }

    private fun orderMoves(
        position: DoushouqiSearchPosition,
        moves: List<DoushouqiMove>,
        principalVariation: DoushouqiMove?,
        killer: DoushouqiMove?,
        history: IntArray?,
    ): List<DoushouqiMove> = moves.sortedWith(
        compareByDescending<DoushouqiMove> {
            moveOrderScore(position, it, principalVariation, killer, history)
        }.then(MOVE_COMPARATOR),
    )

    private fun moveOrderScore(
        position: DoushouqiSearchPosition,
        move: DoushouqiMove,
        principalVariation: DoushouqiMove?,
        killer: DoushouqiMove?,
        history: IntArray?,
    ): Int {
        val moving = requireNotNull(position.pieceAt(move.from))
        val captured = position.pieceAt(move.to)
        var score = when (move) {
            principalVariation -> 800_000
            killer -> 60_000
            else -> 0
        }
        if (denOwner(move.to) == moving.side.other()) return MATE_SCORE
        score += captured?.let { pieceValue(it.animal) * 1_000 } ?: 0
        if (
            captured?.animal == DoushouqiAnimal.ELEPHANT &&
            moving.animal == DoushouqiAnimal.RAT
        ) {
            score += 100_000
        }
        val enemyDen = enemyDen(moving.side)
        score += (manhattan(move.from, enemyDen) - manhattan(move.to, enemyDen)) * 100
        if (trapOwner(move.to) == moving.side) score += 15_000
        history?.let {
            score += it[move.from.index * DoushouqiState.SQUARES + move.to.index]
        }
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
        position: DoushouqiSearchPosition,
        rootSide: DoushouqiSide,
    ): Int {
        var score = 0
        repeat(DoushouqiState.SQUARES) { index ->
            val side = position.pieceSideAt(index) ?: return@repeat
            val animal = requireNotNull(position.pieceAnimalAt(index))
            val boardPosition = DoushouqiPosition(
                index / DoushouqiState.COLUMNS,
                index % DoushouqiState.COLUMNS,
            )
            val sign = if (side == rootSide) 1 else -1
            var value = pieceValue(animal)
            value += (12 - manhattan(boardPosition, enemyDen(side))) * 12
            if (animal == DoushouqiAnimal.RAT) {
                if (terrainAt(boardPosition) == DoushouqiTerrain.RIVER) value += 45
                if (adjacentEnemyElephant(position, boardPosition, side)) value += 80
            }
            if (
                animal in setOf(DoushouqiAnimal.LION, DoushouqiAnimal.TIGER) &&
                hasOpenJump(position, boardPosition)
            ) {
                value += 35
            }
            if (trapOwner(boardPosition) == side.other()) value -= 120
            if (manhattan(boardPosition, enemyDen(side)) == 1) value += 180
            score += sign * value
        }
        val rootMobility = position.legalMoves(rootSide).size
        val opponentMobility = position.legalMoves(rootSide.other()).size
        return score + (rootMobility - opponentMobility) * 4
    }

    private fun adjacentEnemyElephant(
        position: DoushouqiSearchPosition,
        from: DoushouqiPosition,
        side: DoushouqiSide,
    ): Boolean = neighbors(from).any {
        position.pieceAt(it) == DoushouqiPiece(side.other(), DoushouqiAnimal.ELEPHANT)
    }

    private fun hasOpenJump(
        position: DoushouqiSearchPosition,
        from: DoushouqiPosition,
    ): Boolean = position.legalMoves(position.pieceAt(from)?.side ?: return false).any {
        it.from == from && manhattan(it.from, it.to) > 1
    }

    private fun terminalScore(
        result: DoushouqiResult?,
        rootSide: DoushouqiSide,
        ply: Int,
    ): Int? = when (result) {
        is DoushouqiResult.Win ->
            if (result.winner == rootSide) MATE_SCORE - ply else -MATE_SCORE + ply
        is DoushouqiResult.Draw -> 0
        null -> null
    }

    private fun isTactical(
        position: DoushouqiSearchPosition,
        move: DoushouqiMove,
    ): Boolean {
        if (position.pieceAt(move.to) != null) return true
        val moving = requireNotNull(position.pieceAt(move.from))
        val den = enemyDen(moving.side)
        return move.to == den || manhattan(move.to, den) == 1
    }

    private fun transpositionKey(
        position: DoushouqiSearchPosition,
        extension: Int,
    ): Long = stableMix(
        position.zobristKey xor
            (position.quietHalfMoves.toLong() shl 48) xor
            position.repetitionContextHash() xor
            extension.toLong(),
    )

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

    private fun enemyDen(side: DoushouqiSide): DoushouqiPosition =
        if (side == DoushouqiSide.PINE_GREEN) {
            DoushouqiPosition(0, 3)
        } else {
            DoushouqiPosition(8, 3)
        }

    private fun neighbors(position: DoushouqiPosition): List<DoushouqiPosition> =
        listOfNotNull(
            positionOrNull(position.row - 1, position.column),
            positionOrNull(position.row, position.column - 1),
            positionOrNull(position.row, position.column + 1),
            positionOrNull(position.row + 1, position.column),
        )

    private fun positionOrNull(row: Int, column: Int): DoushouqiPosition? =
        if (row in 0 until DoushouqiState.ROWS &&
            column in 0 until DoushouqiState.COLUMNS
        ) {
            DoushouqiPosition(row, column)
        } else {
            null
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

    private val MOVE_COMPARATOR = compareBy<DoushouqiMove>(
        { it.from.row },
        { it.from.column },
        { it.to.row },
        { it.to.column },
    )
    private val SCORED_MOVE_COMPARATOR =
        compareByDescending<ScoredMove> { it.score }
            .thenByDescending { it.orderScore }
            .then(MOVE_COMPARATOR.compareByMove())

    private fun Comparator<DoushouqiMove>.compareByMove(): Comparator<ScoredMove> =
        Comparator { first, second -> compare(first.move, second.move) }

    private const val MATE_SCORE = 1_000_000
    private const val INFINITY = 2_000_000
    private const val DECISIVE_MARGIN = 5_000
    private const val TRANSPOSITION_CAPACITY = 32_768
    private const val HISTORY_MAX = 50_000
    private const val MAX_PLY = 64
}
