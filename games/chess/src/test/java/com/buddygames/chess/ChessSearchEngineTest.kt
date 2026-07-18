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
        assertEquals(
            listOf(
                ChessEvaluationProfile.MATERIAL,
                ChessEvaluationProfile.PIECE_SQUARE,
                ChessEvaluationProfile.ACTIVITY,
                ChessEvaluationProfile.FULL,
                ChessEvaluationProfile.FULL,
                ChessEvaluationProfile.FULL,
                ChessEvaluationProfile.FULL,
                ChessEvaluationProfile.FULL_WITH_QUIESCENCE,
                ChessEvaluationProfile.FULL_WITH_QUIESCENCE,
                ChessEvaluationProfile.FULL_WITH_QUIESCENCE
            ),
            configs.map { it.evaluationProfile }
        )
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

        (1..10).forEach { level ->
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
    fun everyLevelReturnsALegalMoveWithinItsConfiguredDepthCeiling() {
        val state = ChessState.initial()
            .apply(ChessMove.fromUci("e2e4"))
            .apply(ChessMove.fromUci("c7c5"))

        (1..10).forEach { level ->
            val result = ChessAi.search(
                state,
                level,
                ChessSearchLimits(30_000, Long.MAX_VALUE)
            )
            assertTrue("level $level returned an illegal move", result.move in ChessRules.legalMoves(state))
            assertTrue(result.stats.completedDepth <= ChessAiGradient.config(level).maxDepth)
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
    fun higherLevelSearchesDeeperAndAvoidsThePoisonedCapture() {
        val state = ChessState.empty()
            .put("e1", white(ChessPieceType.KING))
            .put("d1", white(ChessPieceType.QUEEN))
            .put("e8", black(ChessPieceType.KING))
            .put("d8", black(ChessPieceType.ROOK))
            .put("d5", black(ChessPieceType.PAWN))
        val poisoned = ChessMove.fromUci("d1d5")

        val levelOne = ChessAi.search(
            state,
            level = 1,
            limits = ChessSearchLimits(50_000, Long.MAX_VALUE)
        )
        val levelEight = ChessAi.search(
            state,
            level = 8,
            limits = ChessSearchLimits(180_000, Long.MAX_VALUE)
        )

        assertEquals(1, levelOne.stats.completedDepth)
        assertTrue(levelEight.stats.completedDepth > levelOne.stats.completedDepth)
        assertNotEquals(levelOne.move, levelEight.move)
        assertNotEquals(poisoned, levelEight.move)
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
    fun cancellationWinsOverImmediateMateScan() {
        val mateInOne = ChessState.empty()
            .put("f6", white(ChessPieceType.KING))
            .put("g6", white(ChessPieceType.QUEEN))
            .put("h8", black(ChessPieceType.KING))

        val cancelled = ChessAi.search(
            mateInOne,
            level = 10,
            limits = ChessSearchLimits(Int.MAX_VALUE, Long.MAX_VALUE),
            shouldStop = { true }
        )

        assertEquals(ChessSearchStopReason.CANCELLED, cancelled.stats.stopReason)
        assertTrue(cancelled.move in ChessRules.legalMoves(mateInOne))
    }

    @Test
    fun interruptedSearchPreservesLastCompletedIteration() {
        val result = ChessAi.search(
            ChessState.initial(),
            level = 8,
            limits = ChessSearchLimits(2_000, Long.MAX_VALUE)
        )

        assertEquals(ChessSearchStopReason.NODE_BUDGET, result.stats.stopReason)
        assertTrue(result.stats.completedDepth >= 1)
        assertTrue(result.move in ChessRules.legalMoves(ChessState.initial()))
    }

    @Test
    fun searchHashSeparatesFiftyMoveClockAndRestoresNestedSpecialMoves() {
        val base = ChessState.empty(
            castlingRights = ChessCastlingRights(whiteKingSide = true),
            enPassantSquare = chessSquare("d6")
        )
            .put("e1", white(ChessPieceType.KING))
            .put("h1", white(ChessPieceType.ROOK))
            .put("e8", black(ChessPieceType.KING))
            .put("e5", white(ChessPieceType.PAWN))
            .put("d5", black(ChessPieceType.PAWN))
        assertNotEquals(
            ChessSearchPosition.from(base).hash,
            ChessSearchPosition.from(base.copy(halfMoveClock = 99)).hash
        )

        val position = ChessSearchPosition.from(base)
        val initialHash = position.hash
        val initialState = position.toState()
        val enPassantUndo = position.makeMove(ChessMove.fromUci("e5d6"))
        val afterEnPassant = position.toState()
        val blackKingUndo = position.makeMove(ChessMove.fromUci("e8f7"))
        position.unmakeMove(blackKingUndo)
        assertEquals(afterEnPassant, position.toState())
        position.unmakeMove(enPassantUndo)
        assertEquals(initialState, position.toState())
        assertEquals(initialHash, position.hash)

        val promotion = ChessState.empty()
            .put("e1", white(ChessPieceType.KING))
            .put("e8", black(ChessPieceType.KING))
            .put("a7", white(ChessPieceType.PAWN))
        val promotionPosition = ChessSearchPosition.from(promotion)
        val promotionHash = promotionPosition.hash
        val promotionUndo = promotionPosition.makeMove(ChessMove.fromUci("a7a8q"))
        promotionPosition.unmakeMove(promotionUndo)
        assertEquals(promotion, promotionPosition.toState())
        assertEquals(promotionHash, promotionPosition.hash)
    }

    @Test
    fun automaticDrawStatesAreVisibleToTheSearchPosition() {
        val kingsOnly = ChessState.empty()
            .put("e1", white(ChessPieceType.KING))
            .put("e8", black(ChessPieceType.KING))
        val fiftyMove = kingsOnly
            .put("a1", white(ChessPieceType.ROOK))
            .copy(halfMoveClock = 100)

        assertTrue(ChessSearchPosition.from(kingsOnly).isAutomaticDraw())
        assertTrue(ChessSearchPosition.from(fiftyMove).isAutomaticDraw())
    }

    @Test
    fun repetitionKeyMatchesSessionRulesAndSearchHonorsThreeOccurrences() {
        val state = ChessState.initial()
            .apply(ChessMove.fromUci("g1f3"))
            .apply(ChessMove.fromUci("g8f6"))
            .apply(ChessMove.fromUci("f3g1"))
            .apply(ChessMove.fromUci("f6g8"))
        val key = chessRepetitionKey(state)
        val uncapturableEnPassant = ChessState.initial().apply(ChessMove.fromUci("e2e4"))

        assertEquals(key, ChessSearchPosition.from(state).repetitionKey())
        assertEquals(
            chessRepetitionKey(uncapturableEnPassant),
            ChessSearchPosition.from(uncapturableEnPassant).repetitionKey()
        )
        assertEquals(
            null,
            ChessAi.search(
                state = state,
                level = 8,
                limits = ChessSearchLimits(100_000, Long.MAX_VALUE),
                repetitionCounts = mapOf(key to 3)
            ).move
        )
    }

    @Test
    fun mateTakesPriorityWhenQuietMoveReachesFiftyMoveThreshold() {
        val mateInOne = ChessState.empty(halfMoveClock = 99)
            .put("f6", white(ChessPieceType.KING))
            .put("g6", white(ChessPieceType.QUEEN))
            .put("h8", black(ChessPieceType.KING))

        assertEquals(
            ChessMove.fromUci("g6g7"),
            ChessAi.search(
                mateInOne,
                level = 8,
                limits = ChessSearchLimits(120_000, Long.MAX_VALUE)
            ).move
        )
    }

    @Test
    fun kingPieceSquareEvaluationIsActive() {
        val state = ChessState.empty()
            .put("d4", white(ChessPieceType.KING))
            .put("d8", black(ChessPieceType.KING))
        val position = ChessSearchPosition.from(state)

        assertNotEquals(
            position.evaluate(ChessSide.WHITE, ChessEvaluationProfile.MATERIAL),
            position.evaluate(ChessSide.WHITE, ChessEvaluationProfile.PIECE_SQUARE)
        )
    }

    @Test
    fun initialPositionMatchesStandardPerftThroughDepthThree() {
        val position = ChessSearchPosition.from(ChessState.initial())

        assertEquals(20L, perft(position, 1))
        assertEquals(400L, perft(position, 2))
        assertEquals(8_902L, perft(position, 3))
    }

    @Test
    fun specialMovePressurePositionsMatchStandardPerft() {
        assertPerft(
            "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
            48L,
            2_039L
        )
        assertPerft(
            "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1",
            14L,
            191L
        )
        assertPerft(
            "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8",
            44L,
            1_486L
        )
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
            assertEquals(state, position.toState())
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

    private fun perft(position: ChessSearchPosition, depth: Int): Long {
        if (depth == 0) return 1
        var nodes = 0L
        position.legalMoves().forEach { move ->
            val undo = position.makeMove(move)
            nodes += perft(position, depth - 1)
            position.unmakeMove(undo)
        }
        return nodes
    }

    private fun assertPerft(fen: String, depthOne: Long, depthTwo: Long) {
        val position = ChessSearchPosition.from(stateFromFen(fen))
        assertEquals(depthOne, perft(position, 1))
        assertEquals(depthTwo, perft(position, 2))
    }

    private fun stateFromFen(fen: String): ChessState {
        val fields = fen.split(" ")
        val side = if (fields[1] == "w") ChessSide.WHITE else ChessSide.BLACK
        val rights = fields[2]
        var state = ChessState.empty(
            sideToMove = side,
            castlingRights = ChessCastlingRights(
                whiteKingSide = 'K' in rights,
                whiteQueenSide = 'Q' in rights,
                blackKingSide = 'k' in rights,
                blackQueenSide = 'q' in rights
            ),
            enPassantSquare = fields[3].takeUnless { it == "-" }?.let(::chessSquare),
            halfMoveClock = fields[4].toInt(),
            fullMoveNumber = fields[5].toInt()
        )
        fields[0].split("/").forEachIndexed { row, rankText ->
            var file = 0
            rankText.forEach { symbol ->
                if (symbol.isDigit()) {
                    file += symbol.digitToInt()
                } else {
                    val sideOfPiece = if (symbol.isUpperCase()) ChessSide.WHITE else ChessSide.BLACK
                    val type = when (symbol.lowercaseChar()) {
                        'k' -> ChessPieceType.KING
                        'q' -> ChessPieceType.QUEEN
                        'r' -> ChessPieceType.ROOK
                        'b' -> ChessPieceType.BISHOP
                        'n' -> ChessPieceType.KNIGHT
                        else -> ChessPieceType.PAWN
                    }
                    val square = (7 - row) * 8 + file
                    state = state.put(chessSquareName(square), ChessPiece(sideOfPiece, type))
                    file++
                }
            }
        }
        return state
    }
}
