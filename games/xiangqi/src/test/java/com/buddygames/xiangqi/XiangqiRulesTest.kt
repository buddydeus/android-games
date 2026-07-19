package com.buddygames.xiangqi

import com.buddygames.api.GameMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class XiangqiRulesTest {
    @Test
    fun gameVersionAndMainMenuLabelStayAligned() {
        assertEquals(8, XiangqiPlugin.manifest.versionCode)
        assertEquals("0.0.8", XiangqiPlugin.manifest.versionName)
        assertEquals("版本 0.0.8", xiangqiVersionLabel(XiangqiPlugin.manifest.versionName))
    }

    @Test
    fun onlySinglePlayerBlackUsesRotatedBoard() {
        assertTrue(shouldRotateXiangqiBoard(GameMode.SINGLE_PLAYER, Side.BLACK))
        assertFalse(shouldRotateXiangqiBoard(GameMode.SINGLE_PLAYER, Side.RED))
        assertFalse(shouldRotateXiangqiBoard(GameMode.TWO_PLAYERS, Side.BLACK))
    }

    @Test
    fun rotatedBoardMapsVisualAndModelCoordinatesBothWays() {
        assertEquals(9 to 8, xiangqiBoardCoordinate(0, 0, rotated = true))
        assertEquals(0 to 0, xiangqiBoardCoordinate(9, 8, rotated = true))
        assertEquals(4 to 6, xiangqiBoardCoordinate(4, 6, rotated = false))
        assertEquals("漢  界" to "楚  河", xiangqiRiverLabels(rotated = true))
        assertEquals("楚  河" to "漢  界", xiangqiRiverLabels(rotated = false))
    }

    @Test
    fun undoButtonHidesAfterEitherSideWins() {
        assertTrue(shouldShowXiangqiUndo(null))
        assertFalse(shouldShowXiangqiUndo(Side.RED))
        assertFalse(shouldShowXiangqiUndo(Side.BLACK))
    }

    @Test
    fun menuUsesUnifiedGameModeLabels() {
        assertEquals(listOf("单人模式", "双人对战", "退出游戏"), xiangqiMenuLabels())
    }

    @Test
    fun twoPlayerScoreRecordsRedAndBlackWins() {
        val score = XiangqiScore()
            .record(Side.RED, GameMode.TWO_PLAYERS, Side.RED)
            .record(Side.BLACK, GameMode.TWO_PLAYERS, Side.RED)
            .record(Side.RED, GameMode.TWO_PLAYERS, Side.RED)

        assertEquals(XiangqiScore(red = 2, black = 1), score)
        assertEquals("2 : 1", score.displayText(GameMode.TWO_PLAYERS))
    }

    @Test
    fun singlePlayerScoreFollowsPlayerIdentityAcrossSideSwaps() {
        val score = XiangqiScore()
            .record(Side.RED, GameMode.SINGLE_PLAYER, Side.RED)
            .record(Side.RED, GameMode.SINGLE_PLAYER, Side.BLACK)
            .record(Side.BLACK, GameMode.SINGLE_PLAYER, Side.BLACK)

        assertEquals(2, score.player)
        assertEquals(1, score.robot)
        assertEquals("2 : 1", score.displayText(GameMode.SINGLE_PLAYER))
        assertEquals(3, score.intelligenceLevel)
    }

    @Test
    fun intelligenceLevelClampsAtTenAndBudgetsAreMonotonic() {
        assertEquals(1, XiangqiScore().intelligenceLevel)
        assertEquals(10, XiangqiScore(player = 20).intelligenceLevel)
        val configs = (1..10).map(XiangqiAiGradient::config)

        assertEquals((1..10).toList(), configs.map { it.level })
        assertTrue(configs.zipWithNext().all { (lower, higher) ->
            lower.maxDepth <= higher.maxDepth &&
                lower.nodeBudget <= higher.nodeBudget &&
                lower.maxThinkTimeMillis <= higher.maxThinkTimeMillis
        })
        assertEquals(
            listOf(64, 512, 4_096, 13_000, 20_000, 96_000, 160_000, 300_000, 700_000, 2_000_000),
            configs.map { it.nodeBudget }
        )
        assertEquals(listOf(1, 2, 3, 4, 4, 5, 5, 6, 7, 8), configs.map { it.maxDepth })
        assertEquals(listOf(8, 6, 5, 4, 2, 1, 1, 1, 1, 1), configs.map { it.candidatePool })
        assertEquals(listOf(65, 50, 32, 20, 8, 0, 0, 0, 0, 0), configs.map { it.suboptimalPercent })
        assertEquals(listOf(0, 0, 0, 0, 0, 0, 0, 1, 1, 2), configs.map { it.quiescenceDepth })
    }

    @Test
    fun evaluationProfilesAddSafetyPositionAndFullBoardKnowledge() {
        val state = XiangqiState.empty()
            .put(0, 4, XiangqiPiece(Side.BLACK, PieceType.GENERAL))
            .put(4, 0, XiangqiPiece(Side.BLACK, PieceType.ROOK))
            .put(4, 3, XiangqiPiece(Side.RED, PieceType.HORSE))
            .put(5, 4, XiangqiPiece(Side.RED, PieceType.SOLDIER))
            .put(8, 3, XiangqiPiece(Side.RED, PieceType.ADVISOR))
            .put(9, 4, XiangqiPiece(Side.RED, PieceType.GENERAL))

        val material = XiangqiAi.evaluationScore(
            state,
            Side.RED,
            XiangqiEvaluationProfile.MATERIAL
        )
        val safety = XiangqiAi.evaluationScore(
            state,
            Side.RED,
            XiangqiEvaluationProfile.MATERIAL_AND_SAFETY
        )
        val basic = XiangqiAi.evaluationScore(
            state,
            Side.RED,
            XiangqiEvaluationProfile.BASIC_POSITIONAL
        )
        val full = XiangqiAi.evaluationScore(
            state,
            Side.RED,
            XiangqiEvaluationProfile.FULL_POSITIONAL
        )

        assertTrue(safety < material)
        assertNotEquals(safety, basic)
        assertNotEquals(basic, full)
    }

    @Test
    fun fullEvaluationRewardsProtectedAttackingPieces() {
        fun position(protectorCol: Int): XiangqiState = XiangqiState.empty()
            .put(0, 4, XiangqiPiece(Side.BLACK, PieceType.GENERAL))
            .put(3, 4, XiangqiPiece(Side.BLACK, PieceType.SOLDIER))
            .put(4, 0, XiangqiPiece(Side.RED, PieceType.ROOK))
            .put(4, 4, XiangqiPiece(Side.BLACK, PieceType.HORSE))
            .put(5, protectorCol, XiangqiPiece(Side.RED, PieceType.SOLDIER))
            .put(9, 4, XiangqiPiece(Side.RED, PieceType.GENERAL))

        val protected = position(protectorCol = 0)
        val unsupported = position(protectorCol = 1)

        assertTrue(
            XiangqiAi.supportedAttackerScore(protected, Side.RED) >
                XiangqiAi.supportedAttackerScore(unsupported, Side.RED)
        )
    }

    @Test
    fun newRoundRestoresInitialBoardRedTurnAndNoSelection() {
        val round = newXiangqiRound()

        assertEquals(Side.RED, round.playerSide)
        assertEquals(Side.RED, round.turn)
        assertEquals(XiangqiState.initial(), round.state)
        assertEquals(null, round.selected)
        assertEquals(null, round.winner)
        assertEquals(null, round.lastMove)
    }

    @Test
    fun singlePlayerVictorySwitchesSidesForNextRound() {
        assertEquals(Side.BLACK, nextXiangqiPlayerSide(Side.RED, Side.RED))
        assertEquals(Side.RED, nextXiangqiPlayerSide(Side.BLACK, Side.BLACK))
    }

    @Test
    fun singlePlayerDefeatAlwaysRestartsAsFirstPlayer() {
        assertEquals(Side.RED, nextXiangqiPlayerSide(Side.RED, Side.BLACK))
        assertEquals(Side.RED, nextXiangqiPlayerSide(Side.BLACK, Side.RED))
    }

    @Test
    fun blackPlayerRoundBeginsWithPendingRedRobotTurn() {
        val round = newXiangqiRound(Side.BLACK)

        assertEquals(Side.BLACK, round.playerSide)
        assertEquals(Side.RED, round.turn)
        assertEquals(XiangqiState.initial(), round.state)
        assertEquals(null, round.selected)
        assertEquals(null, round.winner)
        assertNull(round.lastMove)
        assertTrue(
            needsXiangqiRobotTurn(
                GameMode.SINGLE_PLAYER,
                round.turn,
                round.playerSide,
                round.winner
            )
        )
        assertFalse(
            needsXiangqiRobotTurn(
                GameMode.TWO_PLAYERS,
                round.turn,
                round.playerSide,
                round.winner
            )
        )
    }

    @Test
    fun intelligenceLabelFormatsFirstAndHighestLevels() {
        assertEquals("智能等级 1", xiangqiIntelligenceLabel(1))
        assertEquals("智能等级 10", xiangqiIntelligenceLabel(10))
    }

    @Test
    fun undoRestoresLatestPositionIncludingScore() {
        val first = XiangqiSnapshot(
            state = XiangqiState.initial(),
            turn = Side.RED,
            winner = null,
            score = XiangqiScore(),
            lastMove = null
        )
        val move = XiangqiMove(6, 0, 5, 0)
        val second = first.copy(
            state = first.state.apply(move),
            turn = Side.BLACK,
            score = XiangqiScore(red = 2, black = 1, player = 4, robot = 3),
            lastMove = move
        )

        val undo = undoXiangqi(listOf(first, second))

        assertEquals(second, undo?.snapshot)
        assertEquals(5, undo?.snapshot?.score?.intelligenceLevel)
        assertEquals(listOf(first), undo?.remainingHistory)
        assertNull(undoXiangqi(emptyList()))
    }

    @Test
    fun lastMoveCellUsesDestinationAndSurvivesRotatedDisplayMapping() {
        val move = XiangqiMove(6, 0, 5, 0)

        assertEquals(5 to 0, xiangqiLastMoveCell(move))
        assertEquals(4 to 8, xiangqiBoardCoordinate(5, 0, rotated = true))
        assertNull(xiangqiLastMoveCell(null))
    }

    @Test
    fun lastMoveMarkerLeavesPieceGapWithTranslucentBrightBlueCorners() {
        assertEquals(0.92f, LAST_MOVE_MARKER_SCALE, 0f)
        assertTrue(LAST_MOVE_MARKER_SCALE < 1f)
        assertEquals(0.04f, LAST_MOVE_MARKER_INSET_FRACTION, 0f)
        assertEquals(0.18f, LAST_MOVE_MARKER_CORNER_LENGTH_FRACTION, 0f)
        assertEquals(0xB84FCBFFL, LAST_MOVE_MARKER_HIGHLIGHT_ARGB)
    }

    @Test
    fun rookGivesCheckOnAnOpenFile() {
        val state = XiangqiState.empty()
            .put(0, 4, XiangqiPiece(Side.BLACK, PieceType.GENERAL))
            .put(5, 4, XiangqiPiece(Side.RED, PieceType.ROOK))

        assertTrue(XiangqiRules.isInCheck(state, Side.BLACK))
    }

    @Test
    fun pieceBetweenRookAndGeneralBlocksCheck() {
        val state = XiangqiState.empty()
            .put(0, 4, XiangqiPiece(Side.BLACK, PieceType.GENERAL))
            .put(3, 4, XiangqiPiece(Side.BLACK, PieceType.SOLDIER))
            .put(5, 4, XiangqiPiece(Side.RED, PieceType.ROOK))

        assertFalse(XiangqiRules.isInCheck(state, Side.BLACK))
    }

    @Test
    fun generalsFacingOnOpenFileGiveCheck() {
        val state = XiangqiState.empty()
            .put(0, 4, XiangqiPiece(Side.BLACK, PieceType.GENERAL))
            .put(9, 4, XiangqiPiece(Side.RED, PieceType.GENERAL))

        assertTrue(XiangqiRules.isInCheck(state, Side.BLACK))
        assertTrue(XiangqiRules.isInCheck(state, Side.RED))
    }

    @Test
    fun legalMoveGeneratorDoesNotReturnDuplicateMoves() {
        val moves = XiangqiRules.legalMoves(XiangqiState.initial(), Side.RED)

        assertEquals(moves.toSet().size, moves.size)
    }

    @Test
    fun optimizedLegalMoveGeneratorMatchesExhaustiveLegalityFilter() {
        val states = listOf(
            XiangqiState.initial(),
            XiangqiState.empty()
                .put(0, 4, XiangqiPiece(Side.BLACK, PieceType.GENERAL))
                .put(2, 1, XiangqiPiece(Side.BLACK, PieceType.CANNON))
                .put(5, 1, XiangqiPiece(Side.RED, PieceType.SOLDIER))
                .put(7, 1, XiangqiPiece(Side.RED, PieceType.ROOK))
                .put(9, 4, XiangqiPiece(Side.RED, PieceType.GENERAL))
        )

        states.forEach { state ->
            Side.entries.forEach { side ->
                val exhaustive = buildSet {
                    for (fromRow in 0 until XiangqiState.ROWS) {
                        for (fromCol in 0 until XiangqiState.COLS) {
                            for (toRow in 0 until XiangqiState.ROWS) {
                                for (toCol in 0 until XiangqiState.COLS) {
                                    val move = XiangqiMove(fromRow, fromCol, toRow, toCol)
                                    if (XiangqiRules.isLegalMove(state, move, side)) add(move)
                                }
                            }
                        }
                    }
                }

                assertEquals(exhaustive, XiangqiRules.legalMoves(state, side).toSet())
            }
        }
    }

    @Test
    fun boardFrameMatchesReferenceAspectWithinBounds() {
        val compact = xiangqiBoardSize(240f, 500f)
        val typical = xiangqiBoardSize(900f, 600f)
        val maximum = xiangqiBoardSize(1200f, 900f)

        assertEquals(370.8f, compact.width, 0.001f)
        assertEquals(360f, compact.height, 0.001f)
        assertEquals(618f, typical.width, 0.001f)
        assertEquals(600f, typical.height, 0.001f)
        assertEquals(844.6f, maximum.width, 0.001f)
        assertEquals(820f, maximum.height, 0.001f)
    }

    @Test
    fun displayLabelsMatchReferencePieces() {
        assertEquals("車", XiangqiPiece(Side.RED, PieceType.ROOK).displayLabel())
        assertEquals("馬", XiangqiPiece(Side.BLACK, PieceType.HORSE).displayLabel())
        assertEquals("帥", XiangqiPiece(Side.RED, PieceType.GENERAL).displayLabel())
        assertEquals("將", XiangqiPiece(Side.BLACK, PieceType.GENERAL).displayLabel())
    }

    @Test
    fun intersectionFractionsAnchorXiangqiFilesAndRanks() {
        assertEquals(0f, xiangqiFileFraction(0), 0f)
        assertEquals(0.5f, xiangqiFileFraction(4), 0f)
        assertEquals(1f, xiangqiFileFraction(8), 0f)
        assertEquals(0f, xiangqiRankFraction(0), 0f)
        assertEquals(1f, xiangqiRankFraction(9), 0f)
    }

    @Test
    fun horseCannotMoveWhenLegIsBlocked() {
        val state = XiangqiState.empty()
            .put(9, 1, XiangqiPiece(Side.RED, PieceType.HORSE))
            .put(8, 1, XiangqiPiece(Side.RED, PieceType.SOLDIER))

        assertFalse(XiangqiRules.isLegalMove(state, XiangqiMove(9, 1, 7, 2), Side.RED))
    }

    @Test
    fun cannonCaptureRequiresExactlyOneScreen() {
        val state = XiangqiState.empty()
            .put(7, 1, XiangqiPiece(Side.RED, PieceType.CANNON))
            .put(5, 1, XiangqiPiece(Side.RED, PieceType.SOLDIER))
            .put(2, 1, XiangqiPiece(Side.BLACK, PieceType.ROOK))

        assertTrue(XiangqiRules.isLegalMove(state, XiangqiMove(7, 1, 2, 1), Side.RED))
        assertFalse(XiangqiRules.isLegalMove(state.remove(5, 1), XiangqiMove(7, 1, 2, 1), Side.RED))
    }

    @Test
    fun horizontalCannonCaptureAlsoRequiresExactlyOneScreen() {
        val state = XiangqiState.empty()
            .put(7, 1, XiangqiPiece(Side.RED, PieceType.CANNON))
            .put(7, 4, XiangqiPiece(Side.BLACK, PieceType.ROOK))

        assertFalse(XiangqiRules.isLegalMove(state, XiangqiMove(7, 1, 7, 4), Side.RED))
    }

    @Test
    fun everyIntelligenceLevelCapturesExposedGeneral() {
        val state = XiangqiState.empty()
            .put(5, 4, XiangqiPiece(Side.RED, PieceType.ROOK))
            .put(0, 4, XiangqiPiece(Side.BLACK, PieceType.GENERAL))
            .put(9, 3, XiangqiPiece(Side.RED, PieceType.GENERAL))

        (1..10).forEach { level ->
            assertEquals(
                XiangqiMove(5, 4, 0, 4),
                XiangqiAi.chooseMove(state, Side.RED, level)
            )
        }
    }

    @Test
    fun sideWithoutGeneralHasNoAiMove() {
        val state = XiangqiState.empty()
            .put(9, 4, XiangqiPiece(Side.RED, PieceType.GENERAL))
            .put(4, 0, XiangqiPiece(Side.BLACK, PieceType.ROOK))

        assertNull(XiangqiAi.chooseMove(state, Side.BLACK, 10))
    }

    @Test
    fun quiescenceIncludesQuietEvasionWhenSideIsInCheck() {
        val block = XiangqiMove(1, 0, 1, 4)
        val state = XiangqiState.empty()
            .put(0, 3, XiangqiPiece(Side.BLACK, PieceType.ADVISOR))
            .put(0, 4, XiangqiPiece(Side.BLACK, PieceType.GENERAL))
            .put(0, 5, XiangqiPiece(Side.BLACK, PieceType.ADVISOR))
            .put(1, 0, XiangqiPiece(Side.BLACK, PieceType.ROOK))
            .put(3, 4, XiangqiPiece(Side.RED, PieceType.ROOK))
            .put(9, 4, XiangqiPiece(Side.RED, PieceType.GENERAL))

        assertTrue(XiangqiRules.isInCheck(state, Side.BLACK))
        assertTrue(XiangqiRules.isLegalMove(state, block, Side.BLACK))
        assertTrue(block in XiangqiAi.quiescenceMoves(state, Side.BLACK))
    }

    @Test
    fun weakenedSelectionIsDeterministicAndLegal() {
        val state = XiangqiState.initial()

        val first = XiangqiAi.chooseMove(state, Side.RED, 1)
        val second = XiangqiAi.chooseMove(state, Side.RED, 1)

        assertEquals(first, second)
        assertTrue(first != null && XiangqiRules.isLegalMove(state, first, Side.RED))
    }

    @Test
    fun cancellationReturnsDeterministicLegalFallback() {
        val state = XiangqiState.initial()

        val move = XiangqiAi.chooseMove(state, Side.RED, 10, shouldStop = { true })

        assertTrue(move != null && XiangqiRules.isLegalMove(state, move, Side.RED))
    }

    @Test
    fun staleRobotResultIsRejectedByGenerationPositionTurnOrWinner() {
        val requestedState = XiangqiState.initial()
        val movedState = requestedState.apply(XiangqiMove(6, 0, 5, 0))

        assertTrue(
            shouldApplyXiangqiRobotResult(
                requestedState,
                Side.RED,
                3,
                requestedState,
                Side.RED,
                3,
                null
            )
        )
        assertFalse(
            shouldApplyXiangqiRobotResult(
                requestedState,
                Side.RED,
                3,
                movedState,
                Side.RED,
                3,
                null
            )
        )
        assertFalse(
            shouldApplyXiangqiRobotResult(
                requestedState,
                Side.RED,
                3,
                requestedState,
                Side.RED,
                4,
                null
            )
        )
        assertFalse(
            shouldApplyXiangqiRobotResult(
                requestedState,
                Side.RED,
                3,
                requestedState,
                Side.BLACK,
                3,
                null
            )
        )
        assertFalse(
            shouldApplyXiangqiRobotResult(
                requestedState,
                Side.RED,
                3,
                requestedState,
                Side.RED,
                3,
                Side.BLACK
            )
        )
    }

    @Test
    fun moveCannotExposeOwnGeneralToCheck() {
        val state = XiangqiState.empty()
            .put(0, 4, XiangqiPiece(Side.BLACK, PieceType.GENERAL))
            .put(5, 4, XiangqiPiece(Side.BLACK, PieceType.ROOK))
            .put(7, 4, XiangqiPiece(Side.RED, PieceType.ROOK))
            .put(9, 4, XiangqiPiece(Side.RED, PieceType.GENERAL))

        assertFalse(
            XiangqiRules.isLegalMove(
                state,
                XiangqiMove(7, 4, 7, 3),
                Side.RED
            )
        )
    }

    @Test
    fun checkmateWinsWithoutCapturingGeneral() {
        val state = XiangqiState.empty()
            .put(0, 4, XiangqiPiece(Side.BLACK, PieceType.GENERAL))
            .put(1, 4, XiangqiPiece(Side.BLACK, PieceType.SOLDIER))
            .put(2, 3, XiangqiPiece(Side.RED, PieceType.ROOK))
            .put(2, 4, XiangqiPiece(Side.RED, PieceType.ROOK))
            .put(2, 5, XiangqiPiece(Side.RED, PieceType.ROOK))
            .put(9, 4, XiangqiPiece(Side.RED, PieceType.GENERAL))
        val mate = XiangqiMove(2, 4, 1, 4)

        assertEquals(Side.RED, XiangqiRules.winnerAfterMove(state, mate))
    }

    @Test
    fun robotChoosesCheckmateOverCapturingValuablePiece() {
        val state = XiangqiState.empty()
            .put(0, 4, XiangqiPiece(Side.BLACK, PieceType.GENERAL))
            .put(1, 4, XiangqiPiece(Side.BLACK, PieceType.SOLDIER))
            .put(2, 0, XiangqiPiece(Side.BLACK, PieceType.ROOK))
            .put(2, 3, XiangqiPiece(Side.RED, PieceType.ROOK))
            .put(2, 4, XiangqiPiece(Side.RED, PieceType.ROOK))
            .put(2, 5, XiangqiPiece(Side.RED, PieceType.ROOK))
            .put(9, 4, XiangqiPiece(Side.RED, PieceType.GENERAL))

        assertEquals(
            XiangqiMove(2, 4, 1, 4),
            XiangqiAi.chooseMove(state, Side.RED, 7)
        )
    }

    @Test
    fun robotAvoidsPawnCaptureThatImmediatelyLosesRook() {
        val trappedCapture = XiangqiMove(4, 0, 4, 3)
        val state = XiangqiState.empty()
            .put(0, 4, XiangqiPiece(Side.BLACK, PieceType.GENERAL))
            .put(3, 4, XiangqiPiece(Side.BLACK, PieceType.SOLDIER))
            .put(4, 0, XiangqiPiece(Side.BLACK, PieceType.ROOK))
            .put(4, 3, XiangqiPiece(Side.RED, PieceType.SOLDIER))
            .put(5, 3, XiangqiPiece(Side.RED, PieceType.ROOK))
            .put(9, 4, XiangqiPiece(Side.RED, PieceType.GENERAL))

        assertNotEquals(trappedCapture, XiangqiAi.chooseMove(state, Side.BLACK, 10))
    }

    @Test
    fun rookCannotJumpOverPieces() {
        val state = XiangqiState.empty()
            .put(9, 0, XiangqiPiece(Side.RED, PieceType.ROOK))
            .put(7, 0, XiangqiPiece(Side.RED, PieceType.SOLDIER))

        assertFalse(XiangqiRules.isLegalMove(state, XiangqiMove(9, 0, 0, 0), Side.RED))
    }
}
