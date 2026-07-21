package com.buddygames.junqi

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JunqiMovementTest {
    @Test
    fun movablePieceUsesOneRoadEdgeOnly() {
        val state = stateOf(piece("red", JunqiSide.RED, JunqiPieceType.COMMANDER, 3, 1))

        val moves = JunqiRules.legalMoves(state, JunqiSide.RED)

        assertTrue(JunqiMove(JunqiPosition(3, 1), JunqiPosition(3, 2)) in moves)
        assertFalse(JunqiMove(JunqiPosition(3, 1), JunqiPosition(3, 3)) in moves)
    }

    @Test
    fun commanderMovesStraightOnRailButCannotTurn() {
        val state = stateOf(piece("red", JunqiSide.RED, JunqiPieceType.COMMANDER, 1, 0))

        val moves = destinationsFrom(state, JunqiPosition(1, 0))

        assertTrue(JunqiPosition(10, 0) in moves)
        assertFalse(JunqiPosition(3, 4) in moves)
    }

    @Test
    fun moveListDoesNotDuplicateASharedRoadAndRailDestination() {
        val state = stateOf(piece("red", JunqiSide.RED, JunqiPieceType.COMMANDER, 1, 0))

        val moves = JunqiRules.legalMoves(state, JunqiSide.RED)

        assertTrue(moves.size == moves.toSet().size)
    }

    @Test
    fun engineerTurnsOnRailButCommanderCannot() {
        val engineerMoves = destinations(stateOf(piece("engineer", JunqiSide.RED, JunqiPieceType.ENGINEER, 1, 0)))
        val commanderMoves = destinations(stateOf(piece("commander", JunqiSide.RED, JunqiPieceType.COMMANDER, 1, 0)))

        assertTrue(engineerMoves.contains(JunqiPosition(3, 4)))
        assertFalse(commanderMoves.contains(JunqiPosition(3, 4)))
    }

    @Test
    fun railwayMovementStopsBeforeAnyOccupiedPosition() {
        val state = stateOf(
            piece("red", JunqiSide.RED, JunqiPieceType.COMMANDER, 1, 0),
            piece("blue", JunqiSide.BLUE, JunqiPieceType.REGIMENT, 1, 2),
        )

        val moves = destinationsFrom(state, JunqiPosition(1, 0))

        assertTrue(JunqiPosition(1, 2) in moves)
        assertFalse(JunqiPosition(1, 3) in moves)
    }

    @Test
    fun engineerRailwayBfsDoesNotTraverseFriendlyOccupiedPosition() {
        val state = stateOf(
            piece("engineer", JunqiSide.RED, JunqiPieceType.ENGINEER, 1, 0),
            piece("left-blocker", JunqiSide.RED, JunqiPieceType.REGIMENT, 2, 0),
            piece("friendly", JunqiSide.RED, JunqiPieceType.REGIMENT, 1, 2),
        )

        val moves = destinationsFrom(state, JunqiPosition(1, 0))

        assertFalse(JunqiPosition(1, 2) in moves)
        assertFalse(JunqiPosition(1, 3) in moves)
    }

    @Test
    fun engineerRailwayBfsDoesNotTraverseEnemyOccupiedPositionAfterCapture() {
        val state = stateOf(
            piece("engineer", JunqiSide.RED, JunqiPieceType.ENGINEER, 1, 0),
            piece("left-blocker", JunqiSide.RED, JunqiPieceType.REGIMENT, 2, 0),
            piece("enemy", JunqiSide.BLUE, JunqiPieceType.REGIMENT, 1, 2),
        )

        val moves = destinationsFrom(state, JunqiPosition(1, 0))

        assertTrue(JunqiPosition(1, 2) in moves)
        assertFalse(JunqiPosition(1, 3) in moves)
    }

    @Test
    fun legalMovesUsesExplicitlyRequestedBlueSideRegardlessOfCurrentSide() {
        // currentSide is session state; legalMoves always generates moves for its explicit side argument.
        val blue = piece("blue", JunqiSide.BLUE, JunqiPieceType.COMMANDER, 3, 1)
        val red = piece("red", JunqiSide.RED, JunqiPieceType.COMMANDER, 3, 3)
        val state = JunqiState(
            mapOf(blue.position to blue, red.position to red),
            currentSide = JunqiSide.RED,
        )

        val moves = JunqiRules.legalMoves(state, JunqiSide.BLUE)

        assertTrue(moves.isNotEmpty())
        assertTrue(moves.all { it.from == blue.position })
    }

    @Test
    fun occupiedCampCannotBeAttackedButEmptyCampCanBeEntered() {
        val emptyCampState = stateOf(piece("red", JunqiSide.RED, JunqiPieceType.COMMANDER, 1, 0))
        val occupiedCampState = stateOf(
            piece("red", JunqiSide.RED, JunqiPieceType.COMMANDER, 1, 0),
            piece("blue", JunqiSide.BLUE, JunqiPieceType.REGIMENT, 2, 1),
        )

        assertTrue(JunqiMove(JunqiPosition(1, 0), JunqiPosition(2, 1)) in JunqiRules.legalMoves(emptyCampState, JunqiSide.RED))
        assertFalse(JunqiMove(JunqiPosition(1, 0), JunqiPosition(2, 1)) in JunqiRules.legalMoves(occupiedCampState, JunqiSide.RED))
    }

    @Test
    fun headquartersAndImmovablePiecesHaveNoLegalMoves() {
        val state = stateOf(
            piece("headquarters", JunqiSide.RED, JunqiPieceType.COMMANDER, 11, 1),
            piece("mine", JunqiSide.RED, JunqiPieceType.MINE, 1, 1),
            piece("flag", JunqiSide.RED, JunqiPieceType.FLAG, 1, 3),
        )

        assertTrue(JunqiRules.legalMoves(state, JunqiSide.RED).isEmpty())
    }

    private fun destinations(state: JunqiState): Set<JunqiPosition> = JunqiRules.legalMoves(state, JunqiSide.RED)
        .mapTo(mutableSetOf()) { it.to }

    private fun destinationsFrom(state: JunqiState, from: JunqiPosition): Set<JunqiPosition> =
        JunqiRules.legalMoves(state, JunqiSide.RED)
            .asSequence()
            .filter { it.from == from }
            .mapTo(mutableSetOf()) { it.to }

    private fun stateOf(vararg pieces: JunqiPiece): JunqiState = JunqiState(
        pieces.associateBy { it.position },
    )

    private fun piece(
        id: String,
        side: JunqiSide,
        type: JunqiPieceType,
        row: Int,
        column: Int,
    ) = JunqiPiece(id, side, type, JunqiPosition(row, column))
}
