package com.buddygames.chess

internal enum class ChessSearchStopReason {
    COMPLETED,
    NODE_BUDGET,
    DEADLINE,
    CANCELLED
}

internal data class ChessSearchLimits(
    val nodeBudget: Int,
    val deadlineNanos: Long
)

internal data class ChessSearchStats(
    val completedDepth: Int,
    val visitedNodes: Int,
    val elapsedMillis: Long,
    val stopReason: ChessSearchStopReason
)

internal data class ChessSearchResult(
    val move: ChessMove?,
    val stats: ChessSearchStats
)

private enum class ChessBound {
    EXACT,
    LOWER,
    UPPER
}

private data class ChessTranspositionEntry(
    val hash: Long,
    val depth: Int,
    val score: Int,
    val bound: ChessBound,
    val bestMove: ChessMove?
)

private class ChessTranspositionTable(size: Int) {
    private val entries = arrayOfNulls<ChessTranspositionEntry>(size)
    private val mask = size - 1

    init {
        require(size > 0 && size and mask == 0)
    }

    fun probe(hash: Long): ChessTranspositionEntry? {
        val entry = entries[index(hash)]
        return entry?.takeIf { it.hash == hash }
    }

    fun store(entry: ChessTranspositionEntry) {
        val index = index(entry.hash)
        val existing = entries[index]
        if (existing == null || existing.hash != entry.hash || existing.depth <= entry.depth) {
            entries[index] = entry
        }
    }

    private fun index(hash: Long): Int = ((hash xor (hash ushr 32)).toInt()) and mask
}

internal class ChessSearchEngine(
    state: ChessState,
    private val config: ChessAiLevelConfig,
    private val limits: ChessSearchLimits,
    private val shouldStop: () -> Boolean
) {
    private val position = ChessSearchPosition.from(state)
    private val rootSide = state.sideToMove
    private val context = SearchContext(limits, shouldStop)
    private val transposition = ChessTranspositionTable(TRANSPOSITION_SIZE)
    private val killerMoves = Array(MAX_PLY) { arrayOfNulls<ChessMove>(2) }
    private val history = IntArray(64 * 64)

    fun search(): ChessSearchResult {
        val startedAt = System.nanoTime()
        var rootMoves = orderedMoves(ply = 0)
        if (rootMoves.isEmpty()) {
            return result(null, 0, startedAt, ChessSearchStopReason.COMPLETED)
        }
        findImmediateMate(rootMoves)?.let {
            return result(it, 0, startedAt, ChessSearchStopReason.COMPLETED)
        }

        var bestMove = rootMoves.first()
        var completedDepth = 0
        var completedScores = emptyList<Pair<ChessMove, Int>>()
        for (depth in 1..config.maxDepth) {
            if (context.shouldAbort()) break
            val iteration = searchRoot(rootMoves, depth) ?: break
            completedDepth = depth
            completedScores = iteration
            rootMoves = iteration.map { it.first }
            bestMove = rootMoves.first()
        }
        if (completedScores.isNotEmpty()) {
            bestMove = selectRootMove(completedScores)
        }
        val reason = if (!context.aborted && completedDepth == config.maxDepth) {
            ChessSearchStopReason.COMPLETED
        } else {
            context.stopReason
        }
        return result(bestMove, completedDepth, startedAt, reason)
    }

    private fun findImmediateMate(moves: List<ChessMove>): ChessMove? = moves.firstOrNull { move ->
        val undo = position.makeMove(move)
        val opponent = position.state.sideToMove
        val mate = ChessRules.isInCheck(position.state, opponent) &&
            position.legalMoves().isEmpty()
        position.unmakeMove(undo)
        mate
    }

    private fun searchRoot(
        moves: List<ChessMove>,
        depth: Int
    ): List<Pair<ChessMove, Int>>? {
        val scored = ArrayList<Pair<ChessMove, Int>>(moves.size)
        var alpha = -INFINITY
        for (move in moves) {
            if (context.enterNode()) return null
            val undo = position.makeMove(move)
            val score = -negamax(depth - 1, -INFINITY, -alpha, ply = 1)
            position.unmakeMove(undo)
            if (context.aborted) return null
            scored += move to score
            if (score > alpha) alpha = score
        }
        return scored.sortedWith(
            compareByDescending<Pair<ChessMove, Int>> { it.second }
                .thenBy { it.first.toUci() }
        )
    }

    private fun negamax(depth: Int, alpha: Int, beta: Int, ply: Int): Int {
        if (context.shouldAbort()) return 0
        val side = position.state.sideToMove
        if (depth <= 0) {
            return quiescence(alpha, beta, ply, config.quiescenceDepth)
        }

        val originalAlpha = alpha
        var currentAlpha = alpha
        val hash = position.hash
        val cached = transposition.probe(hash)
        if (cached != null && cached.depth >= depth) {
            when (cached.bound) {
                ChessBound.EXACT -> return cached.score
                ChessBound.LOWER -> currentAlpha = maxOf(currentAlpha, cached.score)
                ChessBound.UPPER -> if (cached.score <= currentAlpha) return cached.score
            }
            if (currentAlpha >= beta) return cached.score
        }

        val moves = orderedMoves(ply, cached?.bestMove)
        if (moves.isEmpty()) {
            return if (ChessRules.isInCheck(position.state, side)) {
                -MATE_SCORE + ply
            } else {
                0
            }
        }
        if (position.state.halfMoveClock >= 100) return 0

        var best = -INFINITY
        var bestMove: ChessMove? = null
        for (move in moves) {
            if (context.enterNode()) return 0
            val capture = position.capturedPieceValue(move) > 0
            val undo = position.makeMove(move)
            val score = -negamax(depth - 1, -beta, -currentAlpha, ply + 1)
            position.unmakeMove(undo)
            if (context.aborted) return 0
            if (score > best) {
                best = score
                bestMove = move
            }
            if (score > currentAlpha) currentAlpha = score
            if (currentAlpha >= beta) {
                if (!capture) {
                    recordKiller(move, ply)
                    history[move.from * 64 + move.to] += depth * depth
                }
                break
            }
        }
        transposition.store(
            ChessTranspositionEntry(
                hash = hash,
                depth = depth,
                score = best,
                bound = when {
                    best <= originalAlpha -> ChessBound.UPPER
                    best >= beta -> ChessBound.LOWER
                    else -> ChessBound.EXACT
                },
                bestMove = bestMove
            )
        )
        return best
    }

    private fun quiescence(alpha: Int, beta: Int, ply: Int, remainingDepth: Int): Int {
        if (context.shouldAbort()) return 0
        val side = position.state.sideToMove
        val inCheck = ChessRules.isInCheck(position.state, side)
        var currentAlpha = alpha
        if (!inCheck) {
            val standPat = position.evaluate(side, config.evaluationProfile)
            if (standPat >= beta) return standPat
            if (standPat > currentAlpha) currentAlpha = standPat
            if (remainingDepth <= 0) return currentAlpha
        } else if (remainingDepth < -2) {
            return position.evaluate(side, config.evaluationProfile)
        }

        var moves = orderedMoves(ply)
        if (moves.isEmpty()) return if (inCheck) -MATE_SCORE + ply else currentAlpha
        if (!inCheck) {
            moves = moves.filter { move ->
                position.capturedPieceValue(move) > 0 || move.promotion != null
            }
        }
        for (move in moves) {
            if (context.enterNode()) return currentAlpha
            val undo = position.makeMove(move)
            val score = -quiescence(-beta, -currentAlpha, ply + 1, remainingDepth - 1)
            position.unmakeMove(undo)
            if (context.aborted) return currentAlpha
            if (score >= beta) return score
            if (score > currentAlpha) currentAlpha = score
        }
        return currentAlpha
    }

    private fun orderedMoves(ply: Int, preferred: ChessMove? = null): List<ChessMove> {
        val moves = position.legalMoves()
        return moves.sortedWith(
            compareByDescending<ChessMove> { move ->
                moveOrderScore(move, ply, preferred)
            }.thenBy { it.toUci() }
        )
    }

    private fun moveOrderScore(move: ChessMove, ply: Int, preferred: ChessMove?): Int {
        if (move == preferred) return PREFERRED_MOVE_SCORE
        val captured = position.capturedPieceValue(move)
        val moving = position.movingPieceValue(move)
        val promotion = move.promotion?.value ?: 0
        val killer = when (move) {
            killerMoves.getOrNull(ply)?.get(0) -> FIRST_KILLER_SCORE
            killerMoves.getOrNull(ply)?.get(1) -> SECOND_KILLER_SCORE
            else -> 0
        }
        return captured * 20 - moving + promotion * 4 + killer +
            history[move.from * 64 + move.to]
    }

    private fun recordKiller(move: ChessMove, ply: Int) {
        val slots = killerMoves.getOrNull(ply) ?: return
        if (slots[0] != move) {
            slots[1] = slots[0]
            slots[0] = move
        }
    }

    private fun selectRootMove(scored: List<Pair<ChessMove, Int>>): ChessMove {
        if (config.candidatePool <= 1 || config.suboptimalPercent <= 0) {
            return scored.first().first
        }
        val seed = position.hash xor (config.level * 101L)
        val roll = positiveModulo(seed, 100)
        if (roll >= config.suboptimalPercent) return scored.first().first
        val pool = minOf(config.candidatePool, scored.size)
        val bestScore = scored.first().second
        val eligible = scored.take(pool).filter { (_, score) ->
            bestScore - score <= MAX_WEAKENING_SCORE
        }
        if (eligible.size <= 1) return scored.first().first
        return eligible[1 + positiveModulo(seed / 101, eligible.size - 1)].first
    }

    private fun positiveModulo(value: Long, modulus: Int): Int =
        ((value % modulus) + modulus).toInt() % modulus

    private fun result(
        move: ChessMove?,
        completedDepth: Int,
        startedAt: Long,
        reason: ChessSearchStopReason
    ): ChessSearchResult = ChessSearchResult(
        move = move,
        stats = ChessSearchStats(
            completedDepth = completedDepth,
            visitedNodes = context.nodes,
            elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000,
            stopReason = reason
        )
    )

    private class SearchContext(
        private val limits: ChessSearchLimits,
        private val shouldStop: () -> Boolean
    ) {
        var nodes: Int = 0
        var aborted: Boolean = false
        var stopReason: ChessSearchStopReason = ChessSearchStopReason.COMPLETED

        fun enterNode(): Boolean {
            nodes++
            return shouldAbort()
        }

        fun shouldAbort(): Boolean {
            if (aborted) return true
            stopReason = when {
                shouldStop() -> ChessSearchStopReason.CANCELLED
                nodes >= limits.nodeBudget -> ChessSearchStopReason.NODE_BUDGET
                System.nanoTime() >= limits.deadlineNanos -> ChessSearchStopReason.DEADLINE
                else -> return false
            }
            aborted = true
            return true
        }
    }

    private companion object {
        const val MAX_PLY = 96
        const val MATE_SCORE = 1_000_000
        const val INFINITY = 1_100_000
        const val PREFERRED_MOVE_SCORE = 2_000_000
        const val FIRST_KILLER_SCORE = 80_000
        const val SECOND_KILLER_SCORE = 60_000
        const val MAX_WEAKENING_SCORE = 180
        const val TRANSPOSITION_SIZE = 1 shl 16
    }
}
