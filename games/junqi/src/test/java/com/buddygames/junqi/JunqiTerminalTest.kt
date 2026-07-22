package com.buddygames.junqi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class JunqiTerminalTest {
    @Test
    fun capturingFlagWinsImmediatelyForEitherSide() {
        for (side in JunqiSide.entries) {
            val defender = side.other()
            val row = if (side == JunqiSide.RED) 1 else 10
            val state = JunqiState(
                pieces = listOf(
                    piece("attacker", side, JunqiPieceType.ENGINEER, row, 0),
                    piece("flag", defender, JunqiPieceType.FLAG, row, 1),
                ).associateBy { it.position },
                currentSide = side,
            )

            val moved = JunqiRules.applyMove(state, JunqiMove(at(row, 0), at(row, 1)))

            assertEquals(JunqiResult.fromWinner(side), moved.result)
        }
    }

    @Test
    fun bombRemovingFlagStillCountsAsFlagCapture() {
        val state = stateOf(
            piece("bomb", JunqiSide.RED, JunqiPieceType.BOMB, 1, 0),
            piece("flag", JunqiSide.BLUE, JunqiPieceType.FLAG, 1, 1),
        )

        val moved = JunqiRules.applyMove(state, JunqiMove(at(1, 0), at(1, 1)))

        assertEquals(JunqiResult.RED_WIN, moved.result)
        assertEquals(JunqiBattleOutcome.BOTH_REMOVED, moved.lastBattleOutcome)
    }

    @Test
    fun sideWithNoLegalMoveLoses() {
        val state = JunqiState(
            pieces = listOf(
                piece("red", JunqiSide.RED, JunqiPieceType.ENGINEER, 3, 1),
                piece("blue-mine", JunqiSide.BLUE, JunqiPieceType.MINE, 0, 0),
            ).associateBy { it.position },
            currentSide = JunqiSide.BLUE,
        )

        assertEquals(JunqiResult.RED_WIN, JunqiRules.terminalResult(state))
    }

    @Test
    fun bothImmobileSidesDraw() {
        val state = stateOf(
            piece("red-flag", JunqiSide.RED, JunqiPieceType.FLAG, 11, 1),
            piece("blue-mine", JunqiSide.BLUE, JunqiPieceType.MINE, 0, 0),
        )

        assertEquals(JunqiResult.DRAW, JunqiRules.terminalResult(state))
    }

    @Test
    fun applyMoveAdjudicatesTheNextSidesLackOfLegalMoves() {
        val state = stateOf(
            piece("red", JunqiSide.RED, JunqiPieceType.ENGINEER, 3, 1),
            piece("blue-mine", JunqiSide.BLUE, JunqiPieceType.MINE, 0, 0),
        )

        val moved = JunqiRules.applyMove(state, JunqiMove(at(3, 1), at(3, 2)))

        assertEquals(JunqiResult.RED_WIN, moved.result)
        assertEquals(JunqiSide.BLUE, moved.currentSide)
    }

    @Test
    fun applyMoveAdjudicatesBothImmobileAsDraw() {
        val state = stateOf(
            piece("red", JunqiSide.RED, JunqiPieceType.ENGINEER, 10, 1),
            piece("blue-mine", JunqiSide.BLUE, JunqiPieceType.MINE, 0, 0),
        )

        val moved = JunqiRules.applyMove(state, JunqiMove(at(10, 1), at(11, 1)))

        assertEquals(JunqiResult.DRAW, moved.result)
    }

    @Test
    fun completingTheThirtyFirstQuietHalfMoveMakesMoverLose() {
        for (side in JunqiSide.entries) {
            val opponent = side.other()
            val moverRow = if (side == JunqiSide.RED) 3 else 8
            val opponentRow = if (side == JunqiSide.RED) 8 else 3
            val state = JunqiState(
                pieces = listOf(
                    piece("mover", side, JunqiPieceType.ENGINEER, moverRow, 1),
                    piece("opponent", opponent, JunqiPieceType.ENGINEER, opponentRow, 1),
                ).associateBy { it.position },
                currentSide = side,
                quietHalfMoves = 30,
            )

            val moved = JunqiRules.applyMove(state, JunqiMove(at(moverRow, 1), at(moverRow, 2)))

            assertEquals(JunqiResult.fromWinner(opponent), moved.result)
            assertEquals(31, moved.quietHalfMoves)
        }
    }

    @Test
    fun sl31TakesPrecedenceWhenTheQuietMoveLeavesBothSidesImmobile() {
        val cases = listOf(
            Triple(JunqiSide.RED, at(10, 1), at(11, 1)),
            Triple(JunqiSide.BLUE, at(1, 1), at(0, 1)),
        )
        for ((side, from, to) in cases) {
            val state = JunqiState(
                pieces = listOf(
                    piece("mover-$side", side, JunqiPieceType.ENGINEER, from.row, from.column),
                    piece("opponent-mine-$side", side.other(), JunqiPieceType.MINE, if (side == JunqiSide.RED) 0 else 11, 0),
                ).associateBy { it.position },
                currentSide = side,
                quietHalfMoves = 30,
            )

            val moved = JunqiRules.applyMove(state, JunqiMove(from, to))

            assertEquals(JunqiResult.fromWinner(side.other()), moved.result)
            assertEquals(31, moved.quietHalfMoves)
            assertEquals(emptyList<JunqiMove>(), JunqiRules.legalMoves(moved, JunqiSide.RED))
            assertEquals(emptyList<JunqiMove>(), JunqiRules.legalMoves(moved, JunqiSide.BLUE))
        }
    }

    @Test
    fun collisionResetsQuietCounterAndDoesNotTriggerSl31() {
        val state = JunqiState(
            pieces = listOf(
                piece("red", JunqiSide.RED, JunqiPieceType.COMMANDER, 3, 0),
                piece("blue", JunqiSide.BLUE, JunqiPieceType.ENGINEER, 3, 1),
                piece("blue-spare", JunqiSide.BLUE, JunqiPieceType.ENGINEER, 8, 1),
            ).associateBy { it.position },
            quietHalfMoves = 30,
        )

        val moved = JunqiRules.applyMove(state, JunqiMove(at(3, 0), at(3, 1)))

        assertEquals(0, moved.quietHalfMoves)
        assertNull(moved.result)
    }

    @Test
    fun flagCaptureTakesPrecedenceOverSl31AndNoMoveAdjudication() {
        val state = JunqiState(
            pieces = listOf(
                piece("red", JunqiSide.RED, JunqiPieceType.ENGINEER, 1, 0),
                piece("blue-flag", JunqiSide.BLUE, JunqiPieceType.FLAG, 1, 1),
            ).associateBy { it.position },
            quietHalfMoves = 30,
        )

        val moved = JunqiRules.applyMove(state, JunqiMove(at(1, 0), at(1, 1)))

        assertEquals(JunqiResult.RED_WIN, moved.result)
    }

    @Test
    fun applyMoveRejectsWrongSideIllegalAndFinishedMoves() {
        val red = piece("red", JunqiSide.RED, JunqiPieceType.ENGINEER, 3, 1)
        val blue = piece("blue", JunqiSide.BLUE, JunqiPieceType.ENGINEER, 8, 1)
        val state = stateOf(red, blue)

        assertThrows(IllegalArgumentException::class.java) {
            JunqiRules.applyMove(state, JunqiMove(blue.position, at(8, 2)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            JunqiRules.applyMove(state, JunqiMove(red.position, at(5, 4)))
        }
        assertThrows(IllegalStateException::class.java) {
            JunqiRules.applyMove(
                JunqiState(state.pieces, result = JunqiResult.RED_WIN),
                JunqiMove(red.position, at(3, 2)),
            )
        }
    }

    private fun stateOf(vararg pieces: JunqiPiece): JunqiState = JunqiState(pieces.associateBy { it.position })

    private fun piece(id: String, side: JunqiSide, type: JunqiPieceType, row: Int, column: Int) =
        JunqiPiece(id, side, type, at(row, column))

    private fun at(row: Int, column: Int) = JunqiPosition(row, column)
}

private fun JunqiSide.other(): JunqiSide = if (this == JunqiSide.RED) JunqiSide.BLUE else JunqiSide.RED
