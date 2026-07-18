package com.buddygames.xiangqi

import org.junit.Assert.assertEquals
import org.junit.Test

class XiangqiSearchPositionTest {
    @Test
    fun makeAndUnmakeRestoreBoardGeneralsAndHash() {
        val original = XiangqiState.initial()
        val position = XiangqiSearchPosition.from(original)
        val originalHash = position.hash
        val move = encodeSearchMove(XiangqiMove(6, 0, 5, 0))

        val captured = position.makeMove(move)
        position.unmakeMove(move, captured)

        assertEquals(original, position.toState())
        assertEquals(originalHash, position.hash)
    }

    @Test
    fun encodedMoveRoundTrips() {
        val move = XiangqiMove(9, 8, 0, 0)

        assertEquals(move, decodeSearchMove(encodeSearchMove(move)))
    }

    @Test
    fun generatedMovesAndChecksMatchCanonicalRulesAcrossDeterministicPositions() {
        var state = XiangqiState.initial()
        var turn = Side.RED
        var seed = 0x2468ACE1L

        repeat(40) {
            val position = XiangqiSearchPosition.from(state)
            Side.entries.forEach { side ->
                val expected = XiangqiRules.legalMoves(state, side).toSet()
                val actual = position.legalMoves(side).map(::decodeSearchMove).toSet()

                assertEquals(expected, actual)
                assertEquals(XiangqiRules.isInCheck(state, side), position.isInCheck(side))
                actual.forEach { move ->
                    val encoded = encodeSearchMove(move)
                    val originalHash = position.hash
                    val captured = position.makeMove(encoded)
                    position.unmakeMove(encoded, captured)
                    assertEquals(state, position.toState())
                    assertEquals(originalHash, position.hash)
                }
            }

            val continuations = XiangqiRules.legalMoves(state, turn)
                .filter { move -> XiangqiRules.winnerAfterMove(state, move) == null }
            if (continuations.isEmpty()) {
                state = XiangqiState.initial()
                turn = Side.RED
            } else {
                seed = (seed * 1_103_515_245L + 12_345L) and 0x7fffffff
                state = state.apply(continuations[(seed % continuations.size).toInt()])
                turn = turn.other()
            }
        }
    }
}
