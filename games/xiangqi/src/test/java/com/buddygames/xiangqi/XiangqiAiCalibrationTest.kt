package com.buddygames.xiangqi

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class XiangqiAiCalibrationTest {
    @Test
    fun representativeMidgameHasOrderedEffectiveDepthFloors() {
        val state = representativeMidgame()
        val minimumDepth = listOf(1, 2, 3, 4, 4, 5, 5, 5, 5, 6)
        val results = (1..10).map { level ->
            XiangqiAi.search(state, Side.RED, level)
        }
        val actualDepth = results.map { it.stats.completedDepth }
        println("xiangqi depth calibration: depth=$actualDepth stats=${results.map { it.stats }}")

        assertTrue(
            "expected depth floors=$minimumDepth actual=$actualDepth stats=" +
                results.map { it.stats },
            actualDepth.zip(minimumDepth).all { (actual, minimum) -> actual >= minimum }
        )
        assertTrue(actualDepth.zipWithNext().all { (lower, higher) -> lower <= higher })
        assertTrue(
            XiangqiAiGradient.config(5).candidatePool <
                XiangqiAiGradient.config(4).candidatePool
        )
        assertTrue(
            XiangqiAiGradient.config(5).suboptimalPercent <
                XiangqiAiGradient.config(4).suboptimalPercent
        )
        assertTrue(
            XiangqiAiGradient.config(7).nodeBudget >
                XiangqiAiGradient.config(6).nodeBudget
        )
        assertTrue(actualDepth[9] >= 6)
    }

    @Test
    fun levelsOneThroughFiveChooseDistinctMovesAcrossPositionCorpus() {
        val positions = deterministicPositions(12)
        val moves = (1..5).associateWith { level ->
            positions.map { (state, side) -> XiangqiAi.chooseMove(state, side, level) }
        }
        val identicalCounts = (1..4).associateWith { lower ->
            moves.getValue(lower)
                .zip(moves.getValue(lower + 1))
                .count { (lowerMove, higherMove) -> lowerMove == higherMove }
        }
        println("xiangqi level 1-5 identical moves: $identicalCounts / ${positions.size}")

        assertTrue(
            "adjacent identical move counts=$identicalCounts out of ${positions.size}",
            identicalCounts.values.all { count -> count * 5 <= positions.size * 4 }
        )
    }

    @Test
    fun colorSwappedHarnessBalancesColorsAndIsDeterministic() {
        val first = runColorSwappedMatches(
            lowerLevel = 1,
            higherLevel = 2,
            games = 2,
            maxPlies = 4
        )
        val second = runColorSwappedMatches(
            lowerLevel = 1,
            higherLevel = 2,
            games = 2,
            maxPlies = 4
        )

        assertEquals(first, second)
        assertEquals(2, first.games)
        assertEquals(1, first.higherAsRed)
        assertEquals(1, first.higherAsBlack)
    }

    @Test
    fun releaseCalibrationRunsOneHundredColorSwappedGamesPerAdjacentPair() {
        assumeTrue(System.getProperty("xiangqi.calibration") == "true")

        val pairFilter = System.getProperty("xiangqi.calibration.pair")?.toInt()
        val lowerLevels = pairFilter?.let(::listOf) ?: (1..9).toList()
        val report = File(
            "build/reports/xiangqi-ai-calibration-" +
                (pairFilter?.let { "pair-$it" } ?: "all") +
                ".txt"
        )
        report.parentFile?.mkdirs()
        report.writeText("Xiangqi adjacent-level calibration\n")
        val summaries = mutableListOf<XiangqiCalibrationSummary>()
        lowerLevels.forEach { lower ->
            val summary = runColorSwappedMatches(
                lowerLevel = lower,
                higherLevel = lower + 1,
                games = 100,
                maxPlies = 80
            )
            summaries += summary
            report.appendText("$summary\n")
        }

        assertTrue(summaries.all { it.games == 100 })
        assertTrue(summaries.all { it.higherAsRed == 50 && it.higherAsBlack == 50 })
        assertTrue(
            "adjacent calibration=$summaries",
            summaries.all { it.higherScorePercent in 65.0..75.0 }
        )
    }

    private fun deterministicPositions(count: Int): List<Pair<XiangqiState, Side>> {
        val positions = mutableListOf<Pair<XiangqiState, Side>>()
        var seed = 0x13579BDFL
        for (sample in 0 until count) {
            var state = XiangqiState.initial()
            var side = Side.RED
            repeat(4 + sample) {
                val moves = XiangqiRules.legalMoves(state, side)
                    .filter { move -> XiangqiRules.winnerAfterMove(state, move) == null }
                if (moves.isNotEmpty()) {
                    seed = (seed * 1_103_515_245L + 12_345L) and 0x7fffffff
                    state = state.apply(moves[(seed % moves.size).toInt()])
                    side = side.other()
                }
            }
            positions += state to side
        }
        return positions
    }

    private fun representativeMidgame(): XiangqiState = XiangqiState.empty()
        .put(0, 3, XiangqiPiece(Side.BLACK, PieceType.ADVISOR))
        .put(0, 4, XiangqiPiece(Side.BLACK, PieceType.GENERAL))
        .put(0, 5, XiangqiPiece(Side.BLACK, PieceType.ADVISOR))
        .put(2, 0, XiangqiPiece(Side.BLACK, PieceType.ROOK))
        .put(2, 2, XiangqiPiece(Side.BLACK, PieceType.HORSE))
        .put(3, 7, XiangqiPiece(Side.BLACK, PieceType.CANNON))
        .put(4, 0, XiangqiPiece(Side.BLACK, PieceType.SOLDIER))
        .put(4, 4, XiangqiPiece(Side.BLACK, PieceType.SOLDIER))
        .put(5, 0, XiangqiPiece(Side.RED, PieceType.SOLDIER))
        .put(5, 4, XiangqiPiece(Side.RED, PieceType.SOLDIER))
        .put(6, 1, XiangqiPiece(Side.RED, PieceType.CANNON))
        .put(7, 6, XiangqiPiece(Side.RED, PieceType.HORSE))
        .put(7, 8, XiangqiPiece(Side.RED, PieceType.ROOK))
        .put(9, 3, XiangqiPiece(Side.RED, PieceType.ADVISOR))
        .put(9, 4, XiangqiPiece(Side.RED, PieceType.GENERAL))
        .put(9, 5, XiangqiPiece(Side.RED, PieceType.ADVISOR))
}
