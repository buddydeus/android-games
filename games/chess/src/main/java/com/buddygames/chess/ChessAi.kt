package com.buddygames.chess

internal enum class ChessEvaluationProfile {
    MATERIAL,
    PIECE_SQUARE,
    ACTIVITY,
    FULL,
    FULL_WITH_QUIESCENCE
}

internal data class ChessAiLevelConfig(
    val level: Int,
    val maxDepth: Int,
    val nodeBudget: Int,
    val maxThinkTimeMillis: Long,
    val candidatePool: Int,
    val suboptimalPercent: Int,
    val evaluationProfile: ChessEvaluationProfile,
    val quiescenceDepth: Int
)

internal object ChessAiGradient {
    private val configs = listOf(
        ChessAiLevelConfig(1, 1, 2_000, 80, 6, 60, ChessEvaluationProfile.MATERIAL, 0),
        ChessAiLevelConfig(2, 2, 6_000, 120, 5, 45, ChessEvaluationProfile.PIECE_SQUARE, 0),
        ChessAiLevelConfig(3, 3, 18_000, 220, 4, 30, ChessEvaluationProfile.ACTIVITY, 0),
        ChessAiLevelConfig(4, 3, 45_000, 350, 3, 15, ChessEvaluationProfile.FULL, 1),
        ChessAiLevelConfig(5, 4, 100_000, 550, 1, 0, ChessEvaluationProfile.FULL, 1),
        ChessAiLevelConfig(6, 5, 220_000, 850, 1, 0, ChessEvaluationProfile.FULL, 1),
        ChessAiLevelConfig(7, 5, 450_000, 1_200, 1, 0, ChessEvaluationProfile.FULL, 2),
        ChessAiLevelConfig(8, 6, 800_000, 1_700, 1, 0, ChessEvaluationProfile.FULL_WITH_QUIESCENCE, 2),
        ChessAiLevelConfig(9, 7, 1_500_000, 2_400, 1, 0, ChessEvaluationProfile.FULL_WITH_QUIESCENCE, 3),
        ChessAiLevelConfig(10, 8, 2_500_000, 3_200, 1, 0, ChessEvaluationProfile.FULL_WITH_QUIESCENCE, 4)
    )

    fun config(level: Int): ChessAiLevelConfig {
        require(level in 1..10) { "Chess intelligence level must be in 1..10" }
        return configs[level - 1]
    }
}

internal object ChessAi {
    fun search(
        state: ChessState,
        level: Int,
        limits: ChessSearchLimits? = null,
        shouldStop: () -> Boolean = { false }
    ): ChessSearchResult {
        val config = ChessAiGradient.config(level)
        val effectiveLimits = limits ?: ChessSearchLimits(
            nodeBudget = config.nodeBudget,
            deadlineNanos = System.nanoTime() + config.maxThinkTimeMillis * 1_000_000
        )
        return ChessSearchEngine(
            state = state,
            config = config,
            limits = effectiveLimits,
            shouldStop = shouldStop
        ).search()
    }

    fun chooseMove(
        state: ChessState,
        level: Int,
        shouldStop: () -> Boolean = { false }
    ): ChessMove? = search(state, level, shouldStop = shouldStop).move
}
