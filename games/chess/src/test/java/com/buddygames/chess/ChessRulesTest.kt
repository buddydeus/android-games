package com.buddygames.chess

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChessRulesTest {
    @Test
    fun initialPositionUsesStandardPlacementAndTwentyLegalMoves() {
        val state = ChessState.initial()

        assertEquals(ChessSide.WHITE, state.sideToMove)
        assertEquals(20, ChessRules.legalMoves(state).size)
        assertEquals(
            ChessPiece(ChessSide.WHITE, ChessPieceType.KING),
            state.pieceAt("e1")
        )
        assertEquals(
            ChessPiece(ChessSide.BLACK, ChessPieceType.QUEEN),
            state.pieceAt("d8")
        )
    }

    @Test
    fun algebraicSquaresMapFromAOneToHightEight() {
        assertEquals(0, chessSquare("a1"))
        assertEquals(7, chessSquare("h1"))
        assertEquals(56, chessSquare("a8"))
        assertEquals(63, chessSquare("h8"))
        assertEquals("e4", chessSquareName(chessSquare("e4")))
    }

    @Test
    fun putReturnsAnIndependentPosition() {
        val empty = ChessState.empty()
        val piece = ChessPiece(ChessSide.WHITE, ChessPieceType.KING)
        val changed = empty.put("e1", piece)

        assertNull(empty.pieceAt("e1"))
        assertEquals(piece, changed.pieceAt("e1"))
    }

    @Test
    fun pinnedPieceCannotExposeItsKing() {
        val state = ChessState.empty()
            .put("e1", white(ChessPieceType.KING))
            .put("e2", white(ChessPieceType.ROOK))
            .put("e8", black(ChessPieceType.ROOK))
            .put("a8", black(ChessPieceType.KING))

        assertFalse(ChessMove.fromUci("e2f2") in ChessRules.legalMoves(state))
        assertTrue(ChessMove.fromUci("e2e8") in ChessRules.legalMoves(state))
    }

    @Test
    fun castlingRequiresClearSafeTransitSquares() {
        val clear = ChessState.empty(
            castlingRights = ChessCastlingRights(whiteKingSide = true)
        )
            .put("e1", white(ChessPieceType.KING))
            .put("h1", white(ChessPieceType.ROOK))
            .put("a8", black(ChessPieceType.KING))
        val attacked = clear.put("f8", black(ChessPieceType.ROOK))

        assertTrue(ChessMove.fromUci("e1g1") in ChessRules.legalMoves(clear))
        assertFalse(ChessMove.fromUci("e1g1") in ChessRules.legalMoves(attacked))

        val castled = clear.apply(ChessMove.fromUci("e1g1"))
        assertEquals(white(ChessPieceType.KING), castled.pieceAt("g1"))
        assertEquals(white(ChessPieceType.ROOK), castled.pieceAt("f1"))
        assertNull(castled.pieceAt("h1"))
    }

    @Test
    fun enPassantCapturesThePassedPawn() {
        val state = ChessState.empty(
            sideToMove = ChessSide.WHITE,
            enPassantSquare = chessSquare("d6")
        )
            .put("e1", white(ChessPieceType.KING))
            .put("e8", black(ChessPieceType.KING))
            .put("e5", white(ChessPieceType.PAWN))
            .put("d5", black(ChessPieceType.PAWN))
        val move = ChessMove.fromUci("e5d6")

        assertTrue(move in ChessRules.legalMoves(state))
        val next = state.apply(move)
        assertEquals(white(ChessPieceType.PAWN), next.pieceAt("d6"))
        assertNull(next.pieceAt("d5"))
    }

    @Test
    fun promotionGeneratesAllStandardPieceChoices() {
        val state = ChessState.empty()
            .put("e1", white(ChessPieceType.KING))
            .put("e8", black(ChessPieceType.KING))
            .put("a7", white(ChessPieceType.PAWN))

        assertEquals(
            setOf(
                ChessPieceType.QUEEN,
                ChessPieceType.ROOK,
                ChessPieceType.BISHOP,
                ChessPieceType.KNIGHT
            ),
            ChessRules.legalMoves(state)
                .filter { it.from == chessSquare("a7") && it.to == chessSquare("a8") }
                .mapNotNull { it.promotion }
                .toSet()
        )
    }

    @Test
    fun foolsMateIsBlackCheckmate() {
        val state = ChessState.initial()
            .apply(ChessMove.fromUci("f2f3"))
            .apply(ChessMove.fromUci("e7e5"))
            .apply(ChessMove.fromUci("g2g4"))
            .apply(ChessMove.fromUci("d8h4"))

        assertTrue(ChessRules.isInCheck(state, ChessSide.WHITE))
        assertEquals(ChessResult.BLACK_WIN, ChessRules.result(state))
    }

    @Test
    fun stalemateIsAZeroLegalMoveDrawWithoutCheck() {
        val state = ChessState.empty(sideToMove = ChessSide.BLACK)
            .put("a8", black(ChessPieceType.KING))
            .put("c6", white(ChessPieceType.KING))
            .put("b6", white(ChessPieceType.QUEEN))

        assertFalse(ChessRules.isInCheck(state, ChessSide.BLACK))
        assertTrue(ChessRules.legalMoves(state).isEmpty())
        assertEquals(ChessResult.DRAW_STALEMATE, ChessRules.result(state))
    }

    @Test
    fun fiftyMoveAndRepetitionDrawsAreRecognized() {
        val fiftyMove = ChessState.empty(halfMoveClock = 100)
            .put("e1", white(ChessPieceType.KING))
            .put("e8", black(ChessPieceType.KING))
            .put("a1", white(ChessPieceType.ROOK))

        assertEquals(ChessResult.DRAW_FIFTY_MOVE, ChessRules.result(fiftyMove))
        assertEquals(ChessResult.DRAW_REPETITION, ChessRules.result(fiftyMove.copy(halfMoveClock = 0), 3))
    }

    @Test
    fun insufficientMaterialCoversMinorPieceAndSameColorBishops() {
        val kings = ChessState.empty()
            .put("e1", white(ChessPieceType.KING))
            .put("e8", black(ChessPieceType.KING))
        val knight = kings.put("b1", white(ChessPieceType.KNIGHT))
        val bishop = kings.put("c1", white(ChessPieceType.BISHOP))
        val sameColorBishops = bishop.put("f8", black(ChessPieceType.BISHOP))
        val oppositeColorBishops = bishop.put("c8", black(ChessPieceType.BISHOP))

        listOf(kings, knight, bishop, sameColorBishops).forEach {
            assertEquals(ChessResult.DRAW_INSUFFICIENT_MATERIAL, ChessRules.result(it))
        }
        assertNull(ChessRules.result(oppositeColorBishops))
    }

    private fun white(type: ChessPieceType) = ChessPiece(ChessSide.WHITE, type)

    private fun black(type: ChessPieceType) = ChessPiece(ChessSide.BLACK, type)
}
