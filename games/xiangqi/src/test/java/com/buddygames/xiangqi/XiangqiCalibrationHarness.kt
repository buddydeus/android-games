package com.buddygames.xiangqi

internal data class XiangqiCalibrationSummary(
    val lowerLevel: Int,
    val higherLevel: Int,
    val games: Int,
    val higherWins: Int,
    val lowerWins: Int,
    val draws: Int,
    val higherAsRed: Int,
    val higherAsBlack: Int,
    val medianDepth: Int,
    val medianNodes: Int,
    val stopReasons: Map<XiangqiSearchStopReason, Int>
) {
    val higherScorePercent: Double
        get() = (higherWins + draws * 0.5) * 100.0 / games
}

internal fun runColorSwappedMatches(
    lowerLevel: Int,
    higherLevel: Int,
    games: Int,
    maxPlies: Int
): XiangqiCalibrationSummary {
    require(games > 0 && games % 2 == 0)
    val results = mutableListOf<Int>()
    val depths = mutableListOf<Int>()
    val nodes = mutableListOf<Int>()
    val stopReasons = XiangqiSearchStopReason.entries.associateWith { 0 }.toMutableMap()
    repeat(games) { game ->
        val higherSide = if (game % 2 == 0) Side.RED else Side.BLACK
        val opening = calibrationOpening(game / 2)
        val result = playCalibrationGame(
            initialState = opening.first,
            initialSide = opening.second,
            lowerLevel = lowerLevel,
            higherLevel = higherLevel,
            higherSide = higherSide,
            maxPlies = maxPlies,
            onSearch = { stats ->
                depths += stats.completedDepth
                nodes += stats.visitedNodes
                stopReasons[stats.stopReason] = stopReasons.getValue(stats.stopReason) + 1
            }
        )
        results += when {
            result == higherSide -> 1
            result == higherSide.other() -> -1
            else -> 0
        }
    }
    return XiangqiCalibrationSummary(
        lowerLevel = lowerLevel,
        higherLevel = higherLevel,
        games = games,
        higherWins = results.count { it > 0 },
        lowerWins = results.count { it < 0 },
        draws = results.count { it == 0 },
        higherAsRed = games / 2,
        higherAsBlack = games / 2,
        medianDepth = median(depths),
        medianNodes = median(nodes),
        stopReasons = stopReasons.toMap()
    )
}

private fun playCalibrationGame(
    initialState: XiangqiState,
    initialSide: Side,
    lowerLevel: Int,
    higherLevel: Int,
    higherSide: Side,
    maxPlies: Int,
    onSearch: (XiangqiSearchStats) -> Unit
): Side? {
    var state = initialState
    var side = initialSide
    val repetitions = mutableMapOf<Int, Int>()
    repeat(maxPlies) {
        val level = if (side == higherSide) higherLevel else lowerLevel
        val search = XiangqiAi.search(state, side, level)
        onSearch(search.stats)
        val move = search.move ?: return side.other()
        XiangqiRules.winnerAfterMove(state, move)?.let { return it }
        state = state.apply(move)
        side = side.other()
        val repetitionKey = 31 * state.hashCode() + side.ordinal
        val count = repetitions.getOrDefault(repetitionKey, 0) + 1
        repetitions[repetitionKey] = count
        if (count >= 3) return null
    }
    return null
}

private fun calibrationOpening(index: Int): Pair<XiangqiState, Side> {
    var state = XiangqiState.initial()
    var side = Side.RED
    var seed = 0x5EED1234L + index
    val plies = (index % 5) * 2
    repeat(plies) {
        val moves = XiangqiRules.legalMoves(state, side)
            .filter { move -> XiangqiRules.winnerAfterMove(state, move) == null }
        if (moves.isNotEmpty()) {
            seed = (seed * 1_103_515_245L + 12_345L) and 0x7fffffff
            state = state.apply(moves[(seed % moves.size).toInt()])
            side = side.other()
        }
    }
    return state to side
}

private fun median(values: List<Int>): Int {
    if (values.isEmpty()) return 0
    return values.sorted()[values.size / 2]
}
