package com.buddygames.xiangqi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class XiangqiSearchEngineTest {
    @Test
    fun searchReportsLastFullyCompletedDepth() {
        val result = XiangqiAi.search(
            state = XiangqiState.initial(),
            side = Side.RED,
            level = 3,
            limits = XiangqiSearchLimits(
                nodeBudget = 20_000,
                deadlineNanos = Long.MAX_VALUE
            )
        )

        assertNotNull(result.move)
        assertTrue(result.stats.completedDepth >= 2)
        assertTrue(result.stats.visitedNodes > 0)
        assertEquals(XiangqiSearchStopReason.COMPLETED, result.stats.stopReason)
    }

    @Test
    fun searchDistinguishesNodeDeadlineAndCancellationStops() {
        val state = XiangqiState.initial()
        val nodeLimited = XiangqiAi.search(
            state,
            Side.RED,
            level = 5,
            limits = XiangqiSearchLimits(1, Long.MAX_VALUE)
        )
        val deadlineLimited = XiangqiAi.search(
            state,
            Side.RED,
            level = 5,
            limits = XiangqiSearchLimits(Int.MAX_VALUE, 0)
        )
        val cancelled = XiangqiAi.search(
            state,
            Side.RED,
            level = 5,
            limits = XiangqiSearchLimits(Int.MAX_VALUE, Long.MAX_VALUE),
            shouldStop = { true }
        )

        assertEquals(XiangqiSearchStopReason.NODE_BUDGET, nodeLimited.stats.stopReason)
        assertEquals(XiangqiSearchStopReason.DEADLINE, deadlineLimited.stats.stopReason)
        assertEquals(XiangqiSearchStopReason.CANCELLED, cancelled.stats.stopReason)
        listOf(nodeLimited, deadlineLimited, cancelled).forEach { result ->
            assertTrue(
                result.move != null &&
                    XiangqiRules.isLegalMove(state, result.move, Side.RED)
            )
        }
    }

    @Test
    fun materialLeafEvaluationDoesNotSortEveryReplyList() {
        val result = XiangqiAi.search(
            state = XiangqiState.initial(),
            side = Side.RED,
            level = 1,
            limits = XiangqiSearchLimits(
                nodeBudget = 20_000,
                deadlineNanos = Long.MAX_VALUE
            )
        )

        assertEquals(1, result.stats.completedDepth)
        assertEquals(1, result.stats.orderedMoveLists)
    }

    @Test
    fun directEvaluationIsSymmetricForBothSides() {
        val position = XiangqiSearchPosition.from(
            XiangqiState.initial()
                .apply(XiangqiMove(6, 0, 5, 0))
                .apply(XiangqiMove(3, 2, 4, 2))
        )

        XiangqiEvaluationProfile.entries.forEach { profile ->
            assertEquals(
                position.evaluate(Side.RED, profile),
                -position.evaluate(Side.BLACK, profile)
            )
        }
    }

    @Test
    fun transpositionTableKeepsDeepestMatchingEntry() {
        val table = XiangqiTranspositionTable(16)
        val hash = 0x1234L
        val bestMove = encodeSearchMove(XiangqiMove(6, 0, 5, 0))

        table.store(hash, depth = 4, score = 120, XiangqiBound.EXACT, bestMove)
        table.store(hash, depth = 2, score = 50, XiangqiBound.LOWER, XIANGQI_SEARCH_NO_MOVE)

        assertEquals(
            XiangqiTranspositionEntry(hash, 4, 120, XiangqiBound.EXACT, bestMove),
            table.probe(hash)
        )
    }

    @Test
    fun newSearchPrefersCheckmateAndAvoidsPoisonedCapture() {
        val mateState = XiangqiState.empty()
            .put(0, 4, XiangqiPiece(Side.BLACK, PieceType.GENERAL))
            .put(1, 4, XiangqiPiece(Side.BLACK, PieceType.SOLDIER))
            .put(2, 0, XiangqiPiece(Side.BLACK, PieceType.ROOK))
            .put(2, 3, XiangqiPiece(Side.RED, PieceType.ROOK))
            .put(2, 4, XiangqiPiece(Side.RED, PieceType.ROOK))
            .put(2, 5, XiangqiPiece(Side.RED, PieceType.ROOK))
            .put(9, 4, XiangqiPiece(Side.RED, PieceType.GENERAL))
        val poisonedCapture = XiangqiMove(4, 0, 4, 3)
        val poisonState = XiangqiState.empty()
            .put(0, 4, XiangqiPiece(Side.BLACK, PieceType.GENERAL))
            .put(3, 4, XiangqiPiece(Side.BLACK, PieceType.SOLDIER))
            .put(4, 0, XiangqiPiece(Side.BLACK, PieceType.ROOK))
            .put(4, 3, XiangqiPiece(Side.RED, PieceType.SOLDIER))
            .put(5, 3, XiangqiPiece(Side.RED, PieceType.ROOK))
            .put(9, 4, XiangqiPiece(Side.RED, PieceType.GENERAL))

        assertEquals(
            XiangqiMove(2, 4, 1, 4),
            XiangqiAi.search(
                mateState,
                Side.RED,
                level = 7,
                limits = XiangqiSearchLimits(100_000, Long.MAX_VALUE)
            ).move
        )
        assertTrue(
            XiangqiAi.search(
                poisonState,
                Side.BLACK,
                level = 10,
                limits = XiangqiSearchLimits(100_000, Long.MAX_VALUE)
            ).move != poisonedCapture
        )
    }
}
