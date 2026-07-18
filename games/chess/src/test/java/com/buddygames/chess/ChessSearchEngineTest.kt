package com.buddygames.chess

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChessSearchEngineTest {
    @Test
    fun intelligenceGradientUsesExactMonotonicTenLevelBudgets() {
        val configs = (1..10).map(ChessAiGradient::config)

        assertEquals((1..10).toList(), configs.map { it.level })
        assertEquals(listOf(1, 2, 3, 3, 4, 5, 5, 6, 7, 8), configs.map { it.maxDepth })
        assertEquals(
            listOf(
                2_000,
                6_000,
                18_000,
                45_000,
                100_000,
                220_000,
                450_000,
                800_000,
                1_500_000,
                2_500_000
            ),
            configs.map { it.nodeBudget }
        )
        assertEquals(
            listOf(80L, 120L, 220L, 350L, 550L, 850L, 1_200L, 1_700L, 2_400L, 3_200L),
            configs.map { it.maxThinkTimeMillis }
        )
        assertEquals(listOf(6, 5, 4, 3, 1, 1, 1, 1, 1, 1), configs.map { it.candidatePool })
        assertEquals(listOf(60, 45, 30, 15, 0, 0, 0, 0, 0, 0), configs.map { it.suboptimalPercent })
        assertEquals(listOf(0, 0, 0, 1, 1, 1, 2, 2, 3, 4), configs.map { it.quiescenceDepth })
        assertTrue(configs.zipWithNext().all { (lower, higher) ->
            lower.maxDepth <= higher.maxDepth &&
                lower.nodeBudget <= higher.nodeBudget &&
                lower.maxThinkTimeMillis <= higher.maxThinkTimeMillis &&
                lower.quiescenceDepth <= higher.quiescenceDepth
        })
    }

    @Test
    fun searchAlwaysReturnsALegalMoveAndReportsWork() {
        val state = ChessState.initial()
        val result = ChessAi.search(
            state = state,
            level = 3,
            limits = ChessSearchLimits(80_000, Long.MAX_VALUE)
        )

        assertNotNull(result.move)
        assertTrue(result.move in ChessRules.legalMoves(state))
        assertTrue(result.stats.completedDepth >= 2)
        assertTrue(result.stats.visitedNodes > 0)
        assertEquals(ChessSearchStopReason.COMPLETED, result.stats.stopReason)
    }

    @Test
    fun everyLevelTakesImmediateCheckmate() {
        val mateInOne = ChessState.empty()
            .put("f6", white(ChessPieceType.KING))
            .put("g6", white(ChessPieceType.QUEEN))
            .put("h8", black(ChessPieceType.KING))
        val expected = ChessMove.fromUci("g6g7")

        listOf(1, 5, 10).forEach { level ->
            assertEquals(
                expected,
                ChessAi.search(
                    mateInOne,
                    level,
                    ChessSearchLimits(120_000, Long.MAX_VALUE)
                ).move
            )
        }
    }

    @Test
    fun strongerSearchAvoidsPoisonedPawnThatLosesQueen() {
        val state = ChessState.empty()
            .put("e1", white(ChessPieceType.KING))
            .put("d1", white(ChessPieceType.QUEEN))
            .put("e8", black(ChessPieceType.KING))
            .put("d8", black(ChessPieceType.ROOK))
            .put("d5", black(ChessPieceType.PAWN))
        val poisoned = ChessMove.fromUci("d1d5")

        assertTrue(poisoned in ChessRules.legalMoves(state))
        assertNotEquals(
            poisoned,
            ChessAi.search(
                state,
                level = 8,
                limits = ChessSearchLimits(180_000, Long.MAX_VALUE)
            ).move
        )
    }

    @Test
    fun searchIsDeterministicForTheSamePositionAndLevel() {
        val state = ChessState.initial()
            .apply(ChessMove.fromUci("e2e4"))
            .apply(ChessMove.fromUci("e7e5"))

        val first = ChessAi.search(
            state,
            level = 4,
            limits = ChessSearchLimits(50_000, Long.MAX_VALUE)
        ).move
        val second = ChessAi.search(
            state,
            level = 4,
            limits = ChessSearchLimits(50_000, Long.MAX_VALUE)
        ).move

        assertEquals(first, second)
    }

    @Test
    fun nodeDeadlineAndCancellationStopsKeepALegalFallback() {
        val state = ChessState.initial()
        val results = listOf(
            ChessAi.search(
                state,
                level = 6,
                limits = ChessSearchLimits(1, Long.MAX_VALUE)
            ),
            ChessAi.search(
                state,
                level = 6,
                limits = ChessSearchLimits(Int.MAX_VALUE, 0)
            ),
            ChessAi.search(
                state,
                level = 6,
                limits = ChessSearchLimits(Int.MAX_VALUE, Long.MAX_VALUE),
                shouldStop = { true }
            )
        )

        assertEquals(
            listOf(
                ChessSearchStopReason.NODE_BUDGET,
                ChessSearchStopReason.DEADLINE,
                ChessSearchStopReason.CANCELLED
            ),
            results.map { it.stats.stopReason }
        )
        results.forEach { result ->
            assertTrue(result.move in ChessRules.legalMoves(state))
        }
    }

    @Test
    fun searchPositionMatchesRulesAndEvaluationIsSymmetric() {
        val states = listOf(
            ChessState.initial(),
            ChessState.initial()
                .apply(ChessMove.fromUci("e2e4"))
                .apply(ChessMove.fromUci("c7c5")),
            ChessState.empty(
                castlingRights = ChessCastlingRights(
                    whiteKingSide = true,
                    whiteQueenSide = true
                )
            )
                .put("e1", white(ChessPieceType.KING))
                .put("a1", white(ChessPieceType.ROOK))
                .put("h1", white(ChessPieceType.ROOK))
                .put("e8", black(ChessPieceType.KING))
        )

        states.forEach { state ->
            val position = ChessSearchPosition.from(state)
            assertEquals(ChessRules.legalMoves(state).toSet(), position.legalMoves().toSet())
            ChessEvaluationProfile.entries.forEach { profile ->
                assertEquals(
                    position.evaluate(ChessSide.WHITE, profile),
                    -position.evaluate(ChessSide.BLACK, profile)
                )
            }
        }
    }

    private fun white(type: ChessPieceType) = ChessPiece(ChessSide.WHITE, type)

    private fun black(type: ChessPieceType) = ChessPiece(ChessSide.BLACK, type)
}
