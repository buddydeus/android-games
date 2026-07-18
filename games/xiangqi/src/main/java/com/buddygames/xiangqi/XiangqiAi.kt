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
    val evaluationProfile: XiangqiEvaluationProfile,
    val quiescenceDepth: Int
)

internal object XiangqiAiGradient {
    private val configs = listOf(
        XiangqiAiLevelConfig(1, 1, 64, 80, 8, 65, XiangqiEvaluationProfile.MATERIAL, 0),
        XiangqiAiLevelConfig(2, 2, 512, 100, 6, 50, XiangqiEvaluationProfile.MATERIAL_AND_SAFETY, 0),
        XiangqiAiLevelConfig(3, 3, 4_096, 150, 5, 32, XiangqiEvaluationProfile.MATERIAL_AND_SAFETY, 0),
        XiangqiAiLevelConfig(4, 4, 13_000, 325, 4, 20, XiangqiEvaluationProfile.BASIC_POSITIONAL, 0),
        XiangqiAiLevelConfig(5, 4, 20_000, 400, 2, 8, XiangqiEvaluationProfile.BASIC_POSITIONAL, 0),
        XiangqiAiLevelConfig(6, 5, 96_000, 1_000, 1, 0, XiangqiEvaluationProfile.FULL_POSITIONAL, 0),
        XiangqiAiLevelConfig(7, 5, 160_000, 1_000, 1, 0, XiangqiEvaluationProfile.FULL_POSITIONAL, 0),
        XiangqiAiLevelConfig(8, 6, 300_000, 1_500, 1, 0, XiangqiEvaluationProfile.FULL_WITH_QUIESCENCE, 1),
        XiangqiAiLevelConfig(9, 7, 700_000, 2_200, 1, 0, XiangqiEvaluationProfile.FULL_WITH_QUIESCENCE, 1),
        XiangqiAiLevelConfig(10, 8, 2_000_000, 3_000, 1, 0, XiangqiEvaluationProfile.FULL_WITH_QUIESCENCE, 2)
    )

    fun config(level: Int): XiangqiAiLevelConfig {
        require(level in 1..10) { "Xiangqi intelligence level must be in 1..10" }
        return configs[level - 1]
    }
}

internal object XiangqiAi {
    internal fun search(
        state: XiangqiState,
        side: Side,
        level: Int,
        limits: XiangqiSearchLimits? = null,
        shouldStop: () -> Boolean = { false }
    ): XiangqiSearchResult {
        val config = XiangqiAiGradient.config(level)
        val effectiveLimits = limits ?: XiangqiSearchLimits(
            nodeBudget = config.nodeBudget,
            deadlineNanos = System.nanoTime() + config.maxThinkTimeMillis * 1_000_000
        )
        return XiangqiSearchEngine(
            state = state,
            rootSide = side,
            config = config,
            limits = effectiveLimits,
            shouldStop = shouldStop
        ).search()
    }

    fun chooseMove(
        state: XiangqiState,
        side: Side,
        level: Int,
        shouldStop: () -> Boolean = { false }
    ): XiangqiMove? = search(
        state = state,
        side = side,
        level = level,
        shouldStop = shouldStop
    ).move

    internal fun quiescenceMoves(state: XiangqiState, side: Side): List<XiangqiMove> {
        val position = XiangqiSearchPosition.from(state)
        val inCheck = position.isInCheck(side)
        return position.legalMoves(side)
            .filter { move ->
                if (inCheck || position.capturedPieceValue(move) > 0) {
                    true
                } else {
                    val captured = position.makeMove(move)
                    val givesCheck = position.isInCheck(side.other())
                    position.unmakeMove(move, captured)
                    givesCheck
                }
            }
            .map(::decodeSearchMove)
    }

    internal fun evaluationScore(
        state: XiangqiState,
        side: Side,
        profile: XiangqiEvaluationProfile
    ): Int = XiangqiSearchPosition.from(state).evaluate(side, profile)

    internal fun supportedAttackerScore(state: XiangqiState, side: Side): Int =
        XiangqiSearchPosition.from(state).supportedAttackerScore(side)
}
