package com.buddygames.junqi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JunqiBattleTest {
    @Test
    fun everyOrdinaryRankPairUsesHigherRankAndEqualRanksMutuallyRemove() {
        for ((attackerRank, attacker) in rankedTypes.withIndex()) {
            for ((defenderRank, defender) in rankedTypes.withIndex()) {
                val expected = when {
                    attackerRank < defenderRank -> JunqiBattleOutcome.ATTACKER_WINS
                    attackerRank > defenderRank -> JunqiBattleOutcome.DEFENDER_WINS
                    else -> JunqiBattleOutcome.BOTH_REMOVED
                }

                assertEquals("$attacker attacking $defender", expected, JunqiRules.battleOutcome(attacker, defender))
            }
        }
    }

    @Test
    fun bombMutuallyRemovesAgainstEveryEnemyTypeFromEitherDirection() {
        for (type in JunqiPieceType.entries) {
            assertEquals(JunqiBattleOutcome.BOTH_REMOVED, JunqiRules.battleOutcome(JunqiPieceType.BOMB, type))
            assertEquals(JunqiBattleOutcome.BOTH_REMOVED, JunqiRules.battleOutcome(type, JunqiPieceType.BOMB))
        }
    }

    @Test
    fun engineerClearsMineAndEveryOtherMovableRankDiesToMine() {
        assertEquals(
            JunqiBattleOutcome.ATTACKER_WINS,
            JunqiRules.battleOutcome(JunqiPieceType.ENGINEER, JunqiPieceType.MINE),
        )
        for (type in rankedTypes - JunqiPieceType.ENGINEER) {
            assertEquals(
                "$type attacking mine",
                JunqiBattleOutcome.DEFENDER_WINS,
                JunqiRules.battleOutcome(type, JunqiPieceType.MINE),
            )
        }
    }

    @Test
    fun everyNonBombMovablePieceCapturesAFlag() {
        for (type in rankedTypes) {
            assertEquals(
                "$type attacking flag",
                JunqiBattleOutcome.ATTACKER_WINS,
                JunqiRules.battleOutcome(type, JunqiPieceType.FLAG),
            )
        }
    }

    @Test
    fun attackerWinMovesSurvivorAndRecordsBattleOutcome() {
        val state = stateOf(
            piece("red", JunqiSide.RED, JunqiPieceType.COMMANDER, 3, 0),
            piece("blue", JunqiSide.BLUE, JunqiPieceType.ENGINEER, 3, 1),
        )

        val moved = JunqiRules.applyMove(state, JunqiMove(at(3, 0), at(3, 1)))

        assertNull(moved.pieces[at(3, 0)])
        assertEquals("red", moved.pieces[at(3, 1)]?.id)
        assertTrue(moved.pieces.getValue(at(3, 1)).hasMoved)
        assertEquals(JunqiBattleOutcome.ATTACKER_WINS, moved.lastBattleOutcome)
        assertEquals(0, moved.quietHalfMoves)
    }

    @Test
    fun defenderWinRemovesOnlyAttacker() {
        val state = stateOf(
            piece("red", JunqiSide.RED, JunqiPieceType.ENGINEER, 3, 0),
            piece("blue", JunqiSide.BLUE, JunqiPieceType.COMMANDER, 3, 1),
        )

        val moved = JunqiRules.applyMove(state, JunqiMove(at(3, 0), at(3, 1)))

        assertFalse(at(3, 0) in moved.pieces)
        assertEquals("blue", moved.pieces[at(3, 1)]?.id)
        assertEquals(JunqiBattleOutcome.DEFENDER_WINS, moved.lastBattleOutcome)
    }

    @Test
    fun equalRankAndBombBattlesRemoveBothPieces() {
        for (attackerType in listOf(JunqiPieceType.COMPANY, JunqiPieceType.BOMB)) {
            val defenderType = if (attackerType == JunqiPieceType.BOMB) JunqiPieceType.COMMANDER else attackerType
            val state = stateOf(
                piece("red", JunqiSide.RED, attackerType, 3, 0),
                piece("blue", JunqiSide.BLUE, defenderType, 3, 1),
            )

            val moved = JunqiRules.applyMove(state, JunqiMove(at(3, 0), at(3, 1)))

            assertFalse(at(3, 0) in moved.pieces)
            assertFalse(at(3, 1) in moved.pieces)
            assertEquals(JunqiBattleOutcome.BOTH_REMOVED, moved.lastBattleOutcome)
        }
    }

    @Test
    fun commanderDeathPermanentlyRevealsItsOwnFlagForEveryDeathDirection() {
        val attackingCommanderDies = stateOf(
            piece("red-commander", JunqiSide.RED, JunqiPieceType.COMMANDER, 3, 0),
            piece("blue-bomb", JunqiSide.BLUE, JunqiPieceType.BOMB, 3, 1),
            piece("red-flag", JunqiSide.RED, JunqiPieceType.FLAG, 11, 1),
            piece("blue-mover", JunqiSide.BLUE, JunqiPieceType.ENGINEER, 0, 0),
        )
        val defendingCommanderDies = JunqiState(
            pieces = listOf(
                piece("blue-bomb", JunqiSide.BLUE, JunqiPieceType.BOMB, 3, 0),
                piece("red-commander", JunqiSide.RED, JunqiPieceType.COMMANDER, 3, 1),
                piece("red-flag", JunqiSide.RED, JunqiPieceType.FLAG, 11, 1),
                piece("blue-mover", JunqiSide.BLUE, JunqiPieceType.ENGINEER, 0, 0),
            ).associateBy { it.position },
            currentSide = JunqiSide.BLUE,
        )

        val first = JunqiRules.applyMove(attackingCommanderDies, JunqiMove(at(3, 0), at(3, 1)))
        val second = JunqiRules.applyMove(defendingCommanderDies, JunqiMove(at(3, 0), at(3, 1)))
        val afterLaterMove = JunqiRules.applyMove(first, JunqiMove(at(0, 0), at(0, 1)))

        assertTrue(JunqiSide.RED in first.revealedFlags)
        assertTrue(JunqiSide.RED in second.revealedFlags)
        assertTrue(JunqiSide.RED in afterLaterMove.revealedFlags)
    }

    private fun stateOf(vararg pieces: JunqiPiece): JunqiState = JunqiState(pieces.associateBy { it.position })

    private fun piece(id: String, side: JunqiSide, type: JunqiPieceType, row: Int, column: Int) =
        JunqiPiece(id, side, type, at(row, column))

    private fun at(row: Int, column: Int) = JunqiPosition(row, column)

    private companion object {
        val rankedTypes = listOf(
            JunqiPieceType.COMMANDER,
            JunqiPieceType.ARMY_COMMANDER,
            JunqiPieceType.DIVISION_COMMANDER,
            JunqiPieceType.BRIGADE_COMMANDER,
            JunqiPieceType.REGIMENT,
            JunqiPieceType.BATTALION,
            JunqiPieceType.COMPANY,
            JunqiPieceType.PLATOON,
            JunqiPieceType.ENGINEER,
        )
    }
}
