package com.buddygames.xiangqi

internal enum class XiangqiSearchStopReason {
    COMPLETED,
    NODE_BUDGET,
    DEADLINE,
    CANCELLED
}

internal data class XiangqiSearchLimits(
    val nodeBudget: Int,
    val deadlineNanos: Long
)

internal data class XiangqiSearchStats(
    val completedDepth: Int,
    val visitedNodes: Int,
    val elapsedMillis: Long,
    val stopReason: XiangqiSearchStopReason,
    val orderedMoveLists: Int
)

internal data class XiangqiSearchResult(
    val move: XiangqiMove?,
    val stats: XiangqiSearchStats
)

internal enum class XiangqiBound {
    EXACT,
    LOWER,
    UPPER
}

internal data class XiangqiTranspositionEntry(
    val hash: Long,
    val depth: Int,
    val score: Int,
    val bound: XiangqiBound,
    val bestMove: Int
)

internal class XiangqiTranspositionTable(size: Int) {
    private val hashes = LongArray(size)
    private val depths = IntArray(size)
    private val scores = IntArray(size)
    private val bounds = ByteArray(size)
    private val bestMoves = IntArray(size) { XIANGQI_SEARCH_NO_MOVE }
    private val occupied = BooleanArray(size)
    private val mask = size - 1

    init {
        require(size > 0 && size and mask == 0) {
            "Xiangqi transposition table size must be a power of two"
        }
    }

    fun probe(hash: Long): XiangqiTranspositionEntry? {
        val index = index(hash)
        if (!occupied[index] || hashes[index] != hash) return null
        return XiangqiTranspositionEntry(
            hash = hash,
            depth = depths[index],
            score = scores[index],
            bound = XiangqiBound.entries[bounds[index].toInt()],
            bestMove = bestMoves[index]
        )
    }

    fun store(hash: Long, depth: Int, score: Int, bound: XiangqiBound, bestMove: Int) {
        val index = index(hash)
        if (occupied[index] && hashes[index] == hash && depths[index] > depth) return
        occupied[index] = true
        hashes[index] = hash
        depths[index] = depth
        scores[index] = score
        bounds[index] = bound.ordinal.toByte()
        bestMoves[index] = bestMove
    }

    private fun index(hash: Long): Int = ((hash xor (hash ushr 32)).toInt()) and mask
}

internal class XiangqiSearchEngine(
    state: XiangqiState,
    private val rootSide: Side,
    private val config: XiangqiAiLevelConfig,
    private val limits: XiangqiSearchLimits,
    private val shouldStop: () -> Boolean
) {
    private val position = XiangqiSearchPosition.from(state)
    private val moveBuffers = Array(MAX_PLY) { IntArray(MAX_MOVES) }
    private val scoreBuffers = Array(MAX_PLY) { IntArray(MAX_MOVES) }
    private val killerMoves = IntArray(MAX_PLY) { XIANGQI_SEARCH_NO_MOVE }
    private val context = SearchContext(limits, shouldStop)
    private val transposition = if (config.level >= 6) {
        XiangqiTranspositionTable(TRANSPOSITION_SIZE)
    } else {
        null
    }

    fun search(): XiangqiSearchResult {
        val startedAt = System.nanoTime()
        if (!position.hasGeneral(rootSide) || !position.hasGeneral(rootSide.other())) {
            return result(
                move = null,
                completedDepth = 0,
                startedAt = startedAt,
                reason = XiangqiSearchStopReason.COMPLETED
            )
        }
        val rootMoves = moveBuffers[0]
        var rootCount = generateOrderedMoves(rootSide, ply = 0)
        if (rootCount == 0) {
            return result(
                move = null,
                completedDepth = 0,
                startedAt = startedAt,
                reason = XiangqiSearchStopReason.COMPLETED
            )
        }
        val immediateGeneral = (0 until rootCount).firstOrNull { index ->
            val move = rootMoves[index]
            position.pieceValueAt(move and 0x7f) == PieceType.GENERAL.value
        }
        if (immediateGeneral != null) {
            return result(
                move = decodeSearchMove(rootMoves[immediateGeneral]),
                completedDepth = 0,
                startedAt = startedAt,
                reason = XiangqiSearchStopReason.COMPLETED
            )
        }
        val safeCount = retainSafeRootMoves(rootMoves, rootCount)
        if (safeCount > 0) rootCount = safeCount

        var bestMove = rootMoves[0]
        var completedDepth = 0
        for (depth in 1..config.maxDepth) {
            if (context.shouldAbort()) break
            val complete = searchRoot(depth, rootMoves, rootCount)
            if (!complete) break
            completedDepth = depth
            bestMove = rootMoves[0]
            rootCount = rootCount.coerceAtMost(MAX_MOVES)
        }
        val reason = if (completedDepth == config.maxDepth && !context.aborted) {
            XiangqiSearchStopReason.COMPLETED
        } else {
            context.stopReason
        }
        bestMove = selectRootMove(rootMoves, rootCount)
        return result(decodeSearchMove(bestMove), completedDepth, startedAt, reason)
    }

    private fun searchRoot(
        depth: Int,
        moves: IntArray,
        count: Int
    ): Boolean {
        val scores = scoreBuffers[0]
        var alpha = -INFINITY
        for (index in 0 until count) {
            if (context.shouldAbort()) return false
            val move = moves[index]
            val captured = position.makeMove(move)
            val score = -negamax(
                side = rootSide.other(),
                depth = depth - 1,
                alpha = -INFINITY,
                beta = -alpha,
                ply = 1
            )
            position.unmakeMove(move, captured)
            if (context.aborted) return false
            scores[index] = score
            if (score > alpha) alpha = score
        }
        sortByScores(moves, scores, count)
        return true
    }

    private fun negamax(
        side: Side,
        depth: Int,
        alpha: Int,
        beta: Int,
        ply: Int
    ): Int {
        terminalGeneralScore(side, ply)?.let { return it }
        if (context.enterNode()) return 0
        if (depth <= 0) {
            return if (
                config.evaluationProfile == XiangqiEvaluationProfile.FULL_WITH_QUIESCENCE
            ) {
                quiescence(side, alpha, beta, ply, config.quiescenceDepth)
            } else {
                if (position.isInCheck(side)) {
                    val count = position.generateLegalMoves(side, moveBuffers[ply])
                    if (count == 0) return -MATE_SCORE + ply
                }
                position.evaluate(side, config.evaluationProfile, moveBuffers[ply + 1])
            }
        }

        val originalAlpha = alpha
        val originalBeta = beta
        var currentAlpha = alpha
        var currentBeta = beta
        val hash = positionHash(side)
        val entry = transposition?.probe(hash)
        if (entry != null && entry.depth >= depth) {
            when (entry.bound) {
                XiangqiBound.EXACT -> return entry.score
                XiangqiBound.LOWER -> currentAlpha = maxOf(currentAlpha, entry.score)
                XiangqiBound.UPPER -> currentBeta = minOf(currentBeta, entry.score)
            }
            if (currentAlpha >= currentBeta) return entry.score
        }
        val count = generateOrderedMoves(
            side,
            ply,
            preferredMove = entry?.bestMove ?: XIANGQI_SEARCH_NO_MOVE
        )
        if (count == 0) return -MATE_SCORE + ply
        var best = -INFINITY
        var bestMove = XIANGQI_SEARCH_NO_MOVE
        val moves = moveBuffers[ply]
        for (index in 0 until count) {
            if (context.shouldAbort()) return 0
            val move = moves[index]
            val captured = position.makeMove(move)
            val score = -negamax(
                side.other(),
                depth - 1,
                -currentBeta,
                -currentAlpha,
                ply + 1
            )
            position.unmakeMove(move, captured)
            if (context.aborted) return 0
            if (score > best) {
                best = score
                bestMove = move
            }
            if (score > currentAlpha) currentAlpha = score
            if (currentAlpha >= currentBeta) {
                if (position.capturedPieceValue(move) == 0) killerMoves[ply] = move
                break
            }
        }
        transposition?.store(
            hash = hash,
            depth = depth,
            score = best,
            bound = when {
                best <= originalAlpha -> XiangqiBound.UPPER
                best >= originalBeta -> XiangqiBound.LOWER
                else -> XiangqiBound.EXACT
            },
            bestMove = bestMove
        )
        return best
    }

    private fun quiescence(
        side: Side,
        alpha: Int,
        beta: Int,
        ply: Int,
        remainingDepth: Int
    ): Int {
        terminalGeneralScore(side, ply)?.let { return it }
        val inCheck = position.isInCheck(side)
        var currentAlpha = alpha
        if (!inCheck) {
            currentAlpha = maxOf(
                currentAlpha,
                position.evaluate(side, config.evaluationProfile, moveBuffers[ply + 1])
            )
            if (currentAlpha >= beta || remainingDepth <= 0) return currentAlpha
        } else if (remainingDepth < -2) {
            return position.evaluate(side, config.evaluationProfile, moveBuffers[ply + 1])
        }

        val moves = moveBuffers[ply]
        var count = position.generateLegalMoves(side, moves)
        if (!inCheck) {
            var forcingCount = 0
            for (index in 0 until count) {
                val move = moves[index]
                val isCapture = position.capturedPieceValue(move) > 0
                val captured = position.makeMove(move)
                val givesCheck = position.isInCheck(side.other())
                position.unmakeMove(move, captured)
                if (isCapture || givesCheck) moves[forcingCount++] = move
            }
            count = forcingCount
        }
        if (count == 0) return if (inCheck) -MATE_SCORE + ply else currentAlpha
        orderExistingMoves(side, ply, count)
        for (index in 0 until count) {
            if (context.enterNode()) return currentAlpha
            val move = moves[index]
            val captured = position.makeMove(move)
            val score = -quiescence(
                side.other(),
                -beta,
                -currentAlpha,
                ply + 1,
                remainingDepth - 1
            )
            position.unmakeMove(move, captured)
            if (context.aborted) return currentAlpha
            if (score >= beta) return score
            if (score > currentAlpha) currentAlpha = score
        }
        return currentAlpha
    }

    private fun generateOrderedMoves(
        side: Side,
        ply: Int,
        preferredMove: Int = XIANGQI_SEARCH_NO_MOVE
    ): Int {
        val moves = moveBuffers[ply]
        val count = position.generateLegalMoves(side, moves)
        context.orderedMoveLists++
        orderExistingMoves(side, ply, count, preferredMove)
        return count
    }

    private fun orderExistingMoves(
        side: Side,
        ply: Int,
        count: Int,
        preferredMove: Int = XIANGQI_SEARCH_NO_MOVE
    ) {
        val moves = moveBuffers[ply]
        val scores = scoreBuffers[ply]
        for (index in 0 until count) {
            val move = moves[index]
            val capturedValue = position.capturedPieceValue(move)
            val movingValue = position.movingPieceValue(move)
            val givesCheck = if (ply <= 1 || capturedValue > 0) {
                val captured = position.makeMove(move)
                val check = position.isInCheck(side.other())
                position.unmakeMove(move, captured)
                check
            } else {
                false
            }
            scores[index] = when {
                move == preferredMove -> PREFERRED_MOVE_SCORE
                capturedValue == PieceType.GENERAL.value -> MATE_SCORE
                move == killerMoves[ply] -> KILLER_MOVE_SCORE
                else -> capturedValue * 100 - movingValue +
                    if (givesCheck) CHECK_ORDER_SCORE else 0
            }
        }
        sortByScores(moves, scores, count)
    }

    private fun retainSafeRootMoves(moves: IntArray, count: Int): Int {
        var safeCount = 0
        for (index in 0 until count) {
            val move = moves[index]
            val captured = position.makeMove(move)
            val general = position.generalIndex(rootSide)
            val replies = moveBuffers[1]
            val replyCount = position.generateLegalMoves(rootSide.other(), replies)
            val unsafe = (0 until replyCount).any { replyIndex ->
                (replies[replyIndex] and 0x7f) == general
            }
            position.unmakeMove(move, captured)
            if (!unsafe) moves[safeCount++] = move
        }
        return safeCount
    }

    private fun selectRootMove(moves: IntArray, count: Int): Int {
        if (count == 1 || config.candidatePool == 1 || config.suboptimalPercent == 0) {
            return moves[0]
        }
        val seed = position.hash xor (rootSide.ordinal * 37L) xor (config.level * 101L)
        val roll = positiveModulo(seed, 100)
        if (roll >= config.suboptimalPercent) return moves[0]
        val poolSize = minOf(config.candidatePool, count)
        val totalWeight = (1 until poolSize).sumOf { poolSize - it }
        var draw = positiveModulo(seed / 101, totalWeight)
        for (index in 1 until poolSize) {
            draw -= poolSize - index
            if (draw < 0) return moves[index]
        }
        return moves[poolSize - 1]
    }

    private fun positiveModulo(value: Long, modulus: Int): Int =
        ((value % modulus) + modulus).toInt() % modulus

    private fun positionHash(side: Side): Long =
        position.hash xor if (side == Side.BLACK) SIDE_TO_MOVE_HASH else 0L

    private fun sortByScores(moves: IntArray, scores: IntArray, count: Int) {
        for (index in 1 until count) {
            val move = moves[index]
            val score = scores[index]
            var target = index
            while (
                target > 0 &&
                (scores[target - 1] < score ||
                    scores[target - 1] == score && moves[target - 1] > move)
            ) {
                moves[target] = moves[target - 1]
                scores[target] = scores[target - 1]
                target--
            }
            moves[target] = move
            scores[target] = score
        }
    }

    private fun terminalGeneralScore(side: Side, ply: Int): Int? = when {
        !position.hasGeneral(side) -> -MATE_SCORE + ply
        !position.hasGeneral(side.other()) -> MATE_SCORE - ply
        else -> null
    }

    private fun result(
        move: XiangqiMove?,
        completedDepth: Int,
        startedAt: Long,
        reason: XiangqiSearchStopReason
    ): XiangqiSearchResult = XiangqiSearchResult(
        move = move,
        stats = XiangqiSearchStats(
            completedDepth = completedDepth,
            visitedNodes = context.nodes,
            elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000,
            stopReason = reason,
            orderedMoveLists = context.orderedMoveLists
        )
    )

    private class SearchContext(
        private val limits: XiangqiSearchLimits,
        private val shouldStop: () -> Boolean
    ) {
        var nodes: Int = 0
        var orderedMoveLists: Int = 0
        var aborted: Boolean = false
        var stopReason: XiangqiSearchStopReason = XiangqiSearchStopReason.COMPLETED

        fun enterNode(): Boolean {
            nodes++
            return shouldAbort()
        }

        fun shouldAbort(): Boolean {
            if (aborted) return true
            stopReason = when {
                shouldStop() -> XiangqiSearchStopReason.CANCELLED
                nodes >= limits.nodeBudget -> XiangqiSearchStopReason.NODE_BUDGET
                System.nanoTime() >= limits.deadlineNanos -> XiangqiSearchStopReason.DEADLINE
                else -> return false
            }
            aborted = true
            return true
        }
    }

    private companion object {
        const val MAX_PLY = 64
        const val MAX_MOVES = 256
        const val MATE_SCORE = 10_000_000
        const val INFINITY = 20_000_000
        const val CHECK_ORDER_SCORE = 10_000
        const val PREFERRED_MOVE_SCORE = 15_000_000
        const val KILLER_MOVE_SCORE = 5_000
        const val TRANSPOSITION_SIZE = 1 shl 18
        const val SIDE_TO_MOVE_HASH = -3335678366873096957L
    }
}
