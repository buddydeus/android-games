package com.buddygames.xiangqi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class XiangqiRulesTest {
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

        assertEquals(Side.RED, round.turn)
        assertEquals(XiangqiState.initial(), round.state)
        assertEquals(null, round.selected)
        assertEquals(null, round.winner)
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
    fun rookCannotJumpOverPieces() {
        val state = XiangqiState.empty()
            .put(9, 0, XiangqiPiece(Side.RED, PieceType.ROOK))
            .put(7, 0, XiangqiPiece(Side.RED, PieceType.SOLDIER))

        assertFalse(XiangqiRules.isLegalMove(state, XiangqiMove(9, 0, 0, 0), Side.RED))
    }
}
