package com.buddygames.doushouqi

data class DoushouqiAiLevel(
    val level: Int,
    val maxDepth: Int,
    val nodeBudget: Int,
    val timeBudgetMillis: Long,
    val weakeningPool: Int,
    val weakeningPercent: Int,
    val tacticalExtension: Int,
) {
    companion object {
        fun forLevel(level: Int): DoushouqiAiLevel {
            require(level in 1..LEVELS.size)
            return LEVELS[level - 1]
        }

        private val LEVELS = listOf(
            DoushouqiAiLevel(1, 1, 1_000, 60, 6, 60, 0),
            DoushouqiAiLevel(2, 2, 4_000, 90, 5, 45, 0),
            DoushouqiAiLevel(3, 3, 12_000, 140, 4, 30, 0),
            DoushouqiAiLevel(4, 4, 35_000, 220, 3, 20, 1),
            DoushouqiAiLevel(5, 5, 80_000, 350, 2, 10, 1),
            DoushouqiAiLevel(6, 6, 180_000, 550, 1, 0, 1),
            DoushouqiAiLevel(7, 7, 400_000, 850, 1, 0, 2),
            DoushouqiAiLevel(8, 8, 800_000, 1_200, 1, 0, 2),
            DoushouqiAiLevel(9, 9, 1_500_000, 1_800, 1, 0, 3),
            DoushouqiAiLevel(10, 10, 2_500_000, 2_600, 1, 0, 3),
        )
    }
}

data class DoushouqiSearchResult(
    val move: DoushouqiMove,
    val completedDepth: Int,
    val nodes: Int,
    val timedOut: Boolean,
)

object DoushouqiAi {
    fun chooseMove(
        state: DoushouqiState,
        level: DoushouqiAiLevel,
        nanoTime: () -> Long = System::nanoTime,
    ): DoushouqiMove? {
        val legalMoves = DoushouqiRules.legalMoves(state)
        if (legalMoves.isEmpty()) return null
        immediateWinningMove(state, legalMoves)?.let { return it }
        return DoushouqiSearchEngine.search(state, level, nanoTime)?.move
    }

    internal fun immediateWinningMove(
        state: DoushouqiState,
        moves: List<DoushouqiMove>,
    ): DoushouqiMove? = moves.firstOrNull { move ->
        val result = DoushouqiRules.apply(state, move)?.result
        result is DoushouqiResult.Win && result.winner == state.sideToMove
    }
}
