package com.buddygames.xiangqi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class XiangqiRulesTest {
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
