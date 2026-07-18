package com.buddygames.xiangqi

internal enum class XiangqiEvaluationProfile {
    MATERIAL,
    MATERIAL_AND_SAFETY,
    BASIC_POSITIONAL,
    FULL_POSITIONAL,
    FULL_WITH_QUIESCENCE
}

internal data class XiangqiAiLevelConfig(
    val level: Int,
    val maxDepth: Int,
    val nodeBudget: Int,
    val maxThinkTimeMillis: Long,
    val candidatePool: Int,
    val suboptimalPercent: Int,
    val evaluationProfile: XiangqiEvaluationProfile
)

internal object XiangqiAiGradient {
    private val configs = listOf(
        XiangqiAiLevelConfig(1, 1, 64, 80, 6, 45, XiangqiEvaluationProfile.MATERIAL),
        XiangqiAiLevelConfig(2, 1, 128, 100, 5, 35, XiangqiEvaluationProfile.MATERIAL_AND_SAFETY),
        XiangqiAiLevelConfig(3, 2, 384, 150, 4, 25, XiangqiEvaluationProfile.MATERIAL_AND_SAFETY),
        XiangqiAiLevelConfig(4, 2, 1_024, 250, 3, 18, XiangqiEvaluationProfile.BASIC_POSITIONAL),
        XiangqiAiLevelConfig(5, 3, 4_096, 400, 2, 10, XiangqiEvaluationProfile.BASIC_POSITIONAL),
        XiangqiAiLevelConfig(6, 4, 16_384, 650, 2, 5, XiangqiEvaluationProfile.FULL_POSITIONAL),
        XiangqiAiLevelConfig(7, 5, 65_536, 1_000, 1, 0, XiangqiEvaluationProfile.FULL_POSITIONAL),
        XiangqiAiLevelConfig(8, 6, 262_144, 1_500, 1, 0, XiangqiEvaluationProfile.FULL_WITH_QUIESCENCE),
        XiangqiAiLevelConfig(9, 7, 786_432, 2_200, 1, 0, XiangqiEvaluationProfile.FULL_WITH_QUIESCENCE),
        XiangqiAiLevelConfig(10, 8, 2_000_000, 3_000, 1, 0, XiangqiEvaluationProfile.FULL_WITH_QUIESCENCE)
    )

    fun config(level: Int): XiangqiAiLevelConfig {
        require(level in 1..10) { "Xiangqi intelligence level must be in 1..10" }
        return configs[level - 1]
    }
}

internal object XiangqiAi {
    fun chooseMove(
        state: XiangqiState,
        side: Side,
        level: Int,
        shouldStop: () -> Boolean = { false }
    ): XiangqiMove? {
        if (!hasGeneral(state, side) || !hasGeneral(state, side.other())) return null
        val config = XiangqiAiGradient.config(level)
        val legalMoves = orderedMoves(state, side)
        if (legalMoves.isEmpty()) return null
        legalMoves.firstOrNull { state.piece(it.toRow, it.toCol)?.type == PieceType.GENERAL }
            ?.let { return it }

        val safeMoves = legalMoves.filterNot { move ->
            allowsImmediateGeneralCapture(state.apply(move), side.other())
        }
        var rootMoves = safeMoves.ifEmpty { legalMoves }
        val context = SearchContext(
            config = config,
            deadlineNanos = System.nanoTime() + config.maxThinkTimeMillis * 1_000_000,
            shouldStop = shouldStop
        )
        var completedScores: List<ScoredMove> = rootMoves.map { ScoredMove(it, 0) }

        for (depth in 1..config.maxDepth) {
            val iteration = searchRoot(state, side, depth, rootMoves, context)
            if (iteration == null) break
            completedScores = iteration
            rootMoves = iteration.map { it.move }
        }

        return selectMove(state, side, config, completedScores)
    }

    internal fun quiescenceMoves(state: XiangqiState, side: Side): List<XiangqiMove> {
        val moves = orderedMoves(state, side)
        if (XiangqiRules.isInCheck(state, side)) return moves
        return moves.filter { move ->
            state.piece(move.toRow, move.toCol) != null ||
                XiangqiRules.isInCheck(state.apply(move), side.other())
        }
    }

    private fun searchRoot(
        state: XiangqiState,
        side: Side,
        depth: Int,
        moves: List<XiangqiMove>,
        context: SearchContext
    ): List<ScoredMove>? {
        val scores = mutableListOf<ScoredMove>()
        var alpha = -INFINITY
        for (move in moves) {
            if (context.shouldAbort()) return null
            val nextState = state.apply(move)
            val score = -negamax(
                state = nextState,
                side = side.other(),
                depth = depth - 1,
                alpha = -INFINITY,
                beta = -alpha,
                ply = 1,
                context = context
            )
            if (context.aborted) return null
            scores += ScoredMove(move, score)
            if (score > alpha) alpha = score
        }
        return scores.sortedWith(
            compareByDescending<ScoredMove> { it.score }
                .thenBy { it.move.fromRow }
                .thenBy { it.move.fromCol }
                .thenBy { it.move.toRow }
                .thenBy { it.move.toCol }
        )
    }

    private fun negamax(
        state: XiangqiState,
        side: Side,
        depth: Int,
        alpha: Int,
        beta: Int,
        ply: Int,
        context: SearchContext
    ): Int {
        terminalGeneralScore(state, side, ply)?.let { return it }
        if (context.enterNode()) return 0
        val moves = orderedMoves(state, side)
        if (moves.isEmpty()) return -MATE_SCORE + ply
        if (depth <= 0) {
            return if (context.config.evaluationProfile == XiangqiEvaluationProfile.FULL_WITH_QUIESCENCE) {
                quiescence(state, side, alpha, beta, ply, 2, context)
            } else {
                evaluationScore(state, side, context.config.evaluationProfile)
            }
        }

        var best = -INFINITY
        var currentAlpha = alpha
        for (move in moves) {
            if (context.shouldAbort()) return 0
            val score = -negamax(
                state.apply(move),
                side.other(),
                depth - 1,
                -beta,
                -currentAlpha,
                ply + 1,
                context
            )
            if (context.aborted) return 0
            if (score > best) best = score
            if (score > currentAlpha) currentAlpha = score
            if (currentAlpha >= beta) break
        }
        return best
    }

    private fun quiescence(
        state: XiangqiState,
        side: Side,
        alpha: Int,
        beta: Int,
        ply: Int,
        remainingDepth: Int,
        context: SearchContext
    ): Int {
        terminalGeneralScore(state, side, ply)?.let { return it }
        val inCheck = XiangqiRules.isInCheck(state, side)
        var currentAlpha = alpha
        if (!inCheck) {
            currentAlpha = maxOf(alpha, evaluationScore(state, side, context.config.evaluationProfile))
            if (currentAlpha >= beta || remainingDepth <= 0) return currentAlpha
        } else if (remainingDepth < -2) {
            return evaluationScore(state, side, context.config.evaluationProfile)
        }
        val forcingMoves = quiescenceMoves(state, side)
        if (forcingMoves.isEmpty()) return -MATE_SCORE + ply
        for (move in forcingMoves) {
            if (context.enterNode()) return currentAlpha
            val nextState = state.apply(move)
            val replies = XiangqiRules.legalMoves(nextState, side.other())
            val score = if (replies.isEmpty()) {
                MATE_SCORE - ply
            } else {
                -quiescence(
                    nextState,
                    side.other(),
                    -beta,
                    -currentAlpha,
                    ply + 1,
                    remainingDepth - 1,
                    context
                )
            }
            if (context.aborted) return currentAlpha
            if (score >= beta) return score
            if (score > currentAlpha) currentAlpha = score
        }
        return currentAlpha
    }

    internal fun evaluationScore(
        state: XiangqiState,
        side: Side,
        profile: XiangqiEvaluationProfile
    ): Int {
        var score = 0
        val pieceCount = state.board.sumOf { row -> row.count { it != null } }
        state.board.forEachIndexed { row, cells ->
            cells.forEachIndexed { col, piece ->
                if (piece == null) return@forEachIndexed
                val direction = if (piece.side == side) 1 else -1
                score += direction * piece.type.value * 100
                if (profile >= XiangqiEvaluationProfile.BASIC_POSITIONAL) {
                    score += direction * positionalValue(piece, row, col, pieceCount)
                }
            }
        }
        if (profile >= XiangqiEvaluationProfile.MATERIAL_AND_SAFETY) {
            if (XiangqiRules.isInCheck(state, side)) score -= CHECK_PENALTY
            if (XiangqiRules.isInCheck(state, side.other())) score += CHECK_PENALTY
            score += capturePressure(state, side) - capturePressure(state, side.other())
        }
        if (profile >= XiangqiEvaluationProfile.FULL_POSITIONAL) {
            score += (
                XiangqiRules.legalMoves(state, side).size -
                    XiangqiRules.legalMoves(state, side.other()).size
                ) * MOBILITY_WEIGHT
            score += generalShield(state, side) - generalShield(state, side.other())
            score += supportedAttackerScore(state, side) -
                supportedAttackerScore(state, side.other())
        }
        return score
    }

    private fun positionalValue(
        piece: XiangqiPiece,
        row: Int,
        col: Int,
        pieceCount: Int
    ): Int {
        val centerDistance = kotlin.math.abs(col - 4)
        val advancement = if (piece.side == Side.RED) 9 - row else row
        val endgame = pieceCount <= 12
        return when (piece.type) {
            PieceType.SOLDIER -> advancement * (if (endgame) 16 else 12) - centerDistance * 2
            PieceType.HORSE -> (if (endgame) 36 else 30) - centerDistance * 5
            PieceType.CANNON -> (if (endgame) 12 else 20) - centerDistance * 3
            PieceType.ROOK -> advancement * if (endgame) 3 else 2
            PieceType.GENERAL -> if (endgame) 12 - centerDistance * 3 else -centerDistance * 4
            PieceType.ADVISOR,
            PieceType.ELEPHANT -> 0
        }
    }

    private fun capturePressure(state: XiangqiState, attacker: Side): Int {
        return XiangqiRules.legalMoves(state, attacker).sumOf { move ->
            (state.piece(move.toRow, move.toCol)?.type?.value ?: 0) * THREAT_WEIGHT
        }
    }

    private fun generalShield(state: XiangqiState, side: Side): Int {
        val general = state.board.indices.firstNotNullOfOrNull { row ->
            state.board[row].indices.firstNotNullOfOrNull { col ->
                if (state.piece(row, col) == XiangqiPiece(side, PieceType.GENERAL)) {
                    row to col
                } else {
                    null
                }
            }
        } ?: return 0
        var shield = 0
        for (row in (general.first - 1)..(general.first + 1)) {
            for (col in (general.second - 1)..(general.second + 1)) {
                val piece = state.piece(row, col)
                if (
                    piece?.side == side &&
                    piece.type in listOf(PieceType.ADVISOR, PieceType.ELEPHANT)
                ) {
                    shield += GENERAL_SHIELD_WEIGHT
                }
            }
        }
        return shield
    }

    internal fun supportedAttackerScore(state: XiangqiState, side: Side): Int {
        return XiangqiRules.legalMoves(state, side)
            .asSequence()
            .filter { move -> state.piece(move.toRow, move.toCol)?.side == side.other() }
            .map { move -> move.fromRow to move.fromCol }
            .distinct()
            .sumOf { (row, col) ->
                val attacker = state.piece(row, col)
                if (attacker != null && XiangqiRules.isDefended(state, row, col, side)) {
                    attacker.type.value * SUPPORTED_ATTACKER_WEIGHT
                } else {
                    0
                }
            }
    }

    private fun orderedMoves(state: XiangqiState, side: Side): List<XiangqiMove> {
        return XiangqiRules.legalMoves(state, side).sortedWith(
            compareByDescending<XiangqiMove> { moveOrderingScore(state, it, side) }
                .thenBy { it.fromRow }
                .thenBy { it.fromCol }
                .thenBy { it.toRow }
                .thenBy { it.toCol }
        )
    }

    private fun moveOrderingScore(state: XiangqiState, move: XiangqiMove, side: Side): Int {
        val captured = state.piece(move.toRow, move.toCol)
        if (captured?.type == PieceType.GENERAL) return MATE_SCORE
        val captureScore = (captured?.type?.value ?: 0) * 100 -
            (state.piece(move.fromRow, move.fromCol)?.type?.value ?: 0)
        val checkScore = if (XiangqiRules.isInCheck(state.apply(move), side.other())) 10_000 else 0
        return checkScore + captureScore
    }

    private fun allowsImmediateGeneralCapture(state: XiangqiState, side: Side): Boolean {
        return XiangqiRules.legalMoves(state, side).any { move ->
            state.piece(move.toRow, move.toCol)?.type == PieceType.GENERAL
        }
    }

    private fun selectMove(
        state: XiangqiState,
        side: Side,
        config: XiangqiAiLevelConfig,
        scores: List<ScoredMove>
    ): XiangqiMove {
        if (scores.size == 1 || config.candidatePool == 1 || config.suboptimalPercent == 0) {
            return scores.first().move
        }
        val seed = stablePositionHash(state, side, config.level)
        val roll = positiveModulo(seed, 100)
        if (roll >= config.suboptimalPercent) return scores.first().move
        val poolSize = minOf(config.candidatePool, scores.size)
        val totalWeight = (1 until poolSize).sumOf { poolSize - it }
        var draw = positiveModulo(seed / 101, totalWeight)
        for (index in 1 until poolSize) {
            draw -= poolSize - index
            if (draw < 0) return scores[index].move
        }
        return scores[poolSize - 1].move
    }

    private fun stablePositionHash(state: XiangqiState, side: Side, level: Int): Long {
        var hash = 1_469_598_103_934_665_603L
        state.board.forEachIndexed { row, cells ->
            cells.forEachIndexed { col, piece ->
                if (piece != null) {
                    val value = ((row * XiangqiState.COLS + col + 1) * 31L) +
                        piece.type.ordinal * 17L +
                        piece.side.ordinal * 7L
                    hash = (hash xor value) * 1_099_511_628_211L
                }
            }
        }
        return hash xor (side.ordinal * 37L) xor (level * 101L)
    }

    private fun positiveModulo(value: Long, modulus: Int): Int =
        ((value % modulus) + modulus).toInt() % modulus

    private fun terminalGeneralScore(state: XiangqiState, side: Side, ply: Int): Int? = when {
        !hasGeneral(state, side) -> -MATE_SCORE + ply
        !hasGeneral(state, side.other()) -> MATE_SCORE - ply
        else -> null
    }

    private fun hasGeneral(state: XiangqiState, side: Side): Boolean =
        state.board.any { row ->
            row.any { piece -> piece == XiangqiPiece(side, PieceType.GENERAL) }
        }

    private data class ScoredMove(val move: XiangqiMove, val score: Int)

    private class SearchContext(
        val config: XiangqiAiLevelConfig,
        private val deadlineNanos: Long,
        private val shouldStop: () -> Boolean
    ) {
        var nodes: Int = 0
        var aborted: Boolean = false

        fun enterNode(): Boolean {
            nodes++
            return shouldAbort()
        }

        fun shouldAbort(): Boolean {
            if (
                aborted ||
                nodes >= config.nodeBudget ||
                System.nanoTime() >= deadlineNanos ||
                shouldStop()
            ) {
                aborted = true
            }
            return aborted
        }
    }

    private const val MATE_SCORE = 10_000_000
    private const val INFINITY = 20_000_000
    private const val CHECK_PENALTY = 8_000
    private const val MOBILITY_WEIGHT = 4
    private const val THREAT_WEIGHT = 18
    private const val GENERAL_SHIELD_WEIGHT = 120
    private const val SUPPORTED_ATTACKER_WEIGHT = 6
}
