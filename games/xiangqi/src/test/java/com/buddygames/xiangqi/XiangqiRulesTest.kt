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
        assertEquals(3, XiangqiPlugin.manifest.versionCode)
        assertEquals("0.0.3", XiangqiPlugin.manifest.versionName)
        assertEquals("版本 0.0.3", xiangqiVersionLabel(XiangqiPlugin.manifest.versionName))
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
    fun scoreRecordsRedAndBlackWins() {
        val score = XiangqiScore()
            .record(Side.RED)
            .record(Side.BLACK)
            .record(Side.RED)

        assertEquals(XiangqiScore(red = 2, black = 1), score)
        assertEquals("2 : 1", score.displayText)
    }

    @Test
    fun newRoundRestoresInitialBoardRedTurnAndNoSelection() {
        val round = newXiangqiRound()

        assertEquals(Side.RED, round.playerSide)
        assertEquals(Side.RED, round.turn)
        assertEquals(XiangqiState.initial(), round.state)
        assertEquals(null, round.selected)
        assertEquals(null, round.winner)
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
    fun blackPlayerRoundStartsAfterRedRobotOpening() {
        val round = newXiangqiRound(Side.BLACK)

        assertEquals(Side.BLACK, round.playerSide)
        assertEquals(Side.BLACK, round.turn)
        assertFalse(round.state == XiangqiState.initial())
        assertEquals(null, round.selected)
        assertEquals(null, round.winner)
    }

    @Test
    fun undoRestoresLatestPositionIncludingScore() {
        val first = XiangqiSnapshot(
            state = XiangqiState.initial(),
            turn = Side.RED,
            winner = null,
            score = XiangqiScore()
        )
        val move = XiangqiMove(6, 0, 5, 0)
        val second = first.copy(
            state = first.state.apply(move),
            turn = Side.BLACK,
            score = XiangqiScore(red = 2, black = 1)
        )

        val undo = undoXiangqi(listOf(first, second))

        assertEquals(second, undo?.snapshot)
        assertEquals(listOf(first), undo?.remainingHistory)
        assertNull(undoXiangqi(emptyList()))
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
    fun robotCapturesGeneralFirst() {
        val state = XiangqiState.empty()
            .put(5, 4, XiangqiPiece(Side.RED, PieceType.ROOK))
            .put(0, 4, XiangqiPiece(Side.BLACK, PieceType.GENERAL))

        assertEquals(XiangqiMove(5, 4, 0, 4), XiangqiRules.robotMove(state, Side.RED))
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
            XiangqiRules.robotMove(state, Side.RED)
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

        assertNotEquals(trappedCapture, XiangqiRules.robotMove(state, Side.BLACK))
    }

    @Test
    fun rookCannotJumpOverPieces() {
        val state = XiangqiState.empty()
            .put(9, 0, XiangqiPiece(Side.RED, PieceType.ROOK))
            .put(7, 0, XiangqiPiece(Side.RED, PieceType.SOLDIER))

        assertFalse(XiangqiRules.isLegalMove(state, XiangqiMove(9, 0, 0, 0), Side.RED))
    }
}
