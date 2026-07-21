package com.buddygames.junqi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class JunqiSessionTest {
    @Test
    fun deploymentSupportsLegalSwapSeededRandomResetAndReady() {
        val session = JunqiSession(JunqiMode.SINGLE_PLAYER)
        val initial = session.state.deployment
        val (first, second) = legalSwap(initial)

        session.selectDeploymentPiece(first)
        val swapped = session.selectDeploymentPiece(second)

        assertEquals(JunqiPhase.DEPLOYMENT, swapped.phase)
        assertTrue(JunqiDeployment.isLegal(swapped.deployment, JunqiSide.RED))
        assertNotEquals(initial, swapped.deployment)
        assertNull(swapped.selectedDeploymentPiece)

        val randomized = session.randomizeDeployment(seed = 42)
        assertEquals(JunqiDeployment.random(JunqiSide.RED, 42), randomized.deployment)
        assertTrue(JunqiDeployment.isLegal(randomized.deployment, JunqiSide.RED))

        val firstUnseeded = session.randomizeDeployment().deployment
        val secondUnseeded = session.randomizeDeployment().deployment
        assertNotEquals(firstUnseeded, secondUnseeded)
        assertTrue(JunqiDeployment.isLegal(firstUnseeded, JunqiSide.RED))
        assertTrue(JunqiDeployment.isLegal(secondUnseeded, JunqiSide.RED))

        val reset = session.resetDeployment()
        assertEquals(initial, reset.deployment)
        assertEquals(JunqiPhase.DEPLOYMENT, reset.phase)

        val ready = session.ready()
        assertEquals(JunqiPhase.PLAYING, ready.phase)
        assertEquals(JunqiSide.RED, ready.currentSide)
        assertEquals(JunqiSide.RED, ready.observation?.viewer)
        assertTrue(ready.deployment.isEmpty())
    }

    @Test
    fun twoPlayerSetupUsesOpaqueHandoffsAndRedAlwaysMovesFirst() {
        val session = JunqiSession(JunqiMode.TWO_PLAYERS)

        val blueHandoff = session.ready()

        assertOpaqueHandoff(blueHandoff, JunqiSide.BLUE)
        val blueDeployment = session.acceptHandoff()
        assertEquals(JunqiPhase.DEPLOYMENT, blueDeployment.phase)
        assertEquals(JunqiSide.BLUE, blueDeployment.currentSide)
        assertTrue(blueDeployment.deployment.all { it.side == JunqiSide.BLUE })

        val redHandoff = session.ready()
        assertOpaqueHandoff(redHandoff, JunqiSide.RED)
        val playing = session.acceptHandoff()
        assertEquals(JunqiPhase.PLAYING, playing.phase)
        assertEquals(JunqiSide.RED, playing.currentSide)
        assertEquals(JunqiSide.RED, playing.observation?.viewer)
        assertNull(session.robotRequest)
    }

    @Test
    fun blueSinglePlayerStartsBehindOneOpaqueHandoffAndRobotGetsOnlyItsObservation() {
        val session = JunqiSession(JunqiMode.SINGLE_PLAYER, playerSide = JunqiSide.BLUE)

        val handoff = session.ready()

        assertOpaqueHandoff(handoff, JunqiSide.BLUE)
        val playing = session.acceptHandoff()
        val request = session.robotRequest
        assertEquals(JunqiPhase.PLAYING, playing.phase)
        assertEquals(JunqiSide.RED, playing.currentSide)
        assertEquals(JunqiSide.BLUE, playing.observation?.viewer)
        assertEquals(JunqiSide.BLUE, playing.playerSide)
        assertEquals(JunqiSide.RED, playing.robotSide)
        assertNotNull(request)
        assertEquals(JunqiSide.RED, request?.observation?.viewer)
        assertEquals(JunqiSide.RED, request?.observation?.currentSide)
        assertEquals(JunqiAiLevel.LEVEL_1, request?.level)
        assertEquals(request?.generation, playing.robotRequestGeneration)
    }

    @Test
    fun twoPlayerQuietMoveRecordsLastMoveThenUndoReturnsMoverBehindHandoff() {
        val session = readyTwoPlayerSession()
        val before = session.state.observation
        val move = quietMove(requireNotNull(before))

        val moved = session.play(move)

        assertEquals(JunqiPhase.HANDOFF, moved.phase)
        assertNull(moved.lastMove)
        assertTrue(moved.canUndo)
        assertNull(moved.observation)
        val nextTurn = session.acceptHandoff()
        assertEquals(move, nextTurn.lastMove)

        val undone = session.undo()
        assertOpaqueHandoff(undone, JunqiSide.RED)
        assertNull(undone.lastMove)
        assertFalse(undone.canUndo)
        assertEquals(before, session.acceptHandoff().observation)
    }

    @Test
    fun battleResultIsGenericAndMustBeAcknowledgedBeforeHandoff() {
        val session = JunqiSession.started(
            mode = JunqiMode.TWO_PLAYERS,
            state = battleState(),
        )

        val result = session.play(JunqiMove(at(3, 0), at(3, 1)))

        assertEquals(JunqiPhase.BATTLE_RESULT, result.phase)
        assertEquals(JunqiBattleOutcome.ATTACKER_WINS, result.battleOutcome)
        assertNull(result.observation)
        assertTrue(result.deployment.isEmpty())
        assertTrue(
            JunqiSessionState::class.java.declaredFields.none { field ->
                field.type == JunqiPieceType::class.java || field.type == JunqiState::class.java
            },
        )

        val handoff = session.acknowledgeBattle()
        assertOpaqueHandoff(handoff, JunqiSide.BLUE)
        assertEquals(JunqiPhase.PLAYING, session.acceptHandoff().phase)
    }

    @Test
    fun singlePlayerBattleAcknowledgementGeneratesOnlyTheRobotPublicRequest() {
        val session = JunqiSession.started(
            mode = JunqiMode.SINGLE_PLAYER,
            playerSide = JunqiSide.RED,
            state = battleState(),
        )
        session.play(JunqiMove(at(3, 0), at(3, 1)))

        val playing = session.acknowledgeBattle()
        val request = requireNotNull(session.robotRequest)

        assertEquals(JunqiPhase.PLAYING, playing.phase)
        assertEquals(JunqiSide.BLUE, playing.currentSide)
        assertEquals(JunqiSide.RED, playing.observation?.viewer)
        assertEquals(JunqiSide.BLUE, request.observation.viewer)
        assertEquals(JunqiSide.BLUE, request.observation.currentSide)
        assertEquals(request.generation, playing.robotRequestGeneration)
        assertTrue(request.knowledge.activePieceIds.contains("red-commander"))
        assertFalse(JunqiPieceType.FLAG in request.knowledge.candidatesFor("red-commander"))
        assertFalse(JunqiPieceType.MINE in request.knowledge.candidatesFor("red-commander"))
    }

    @Test
    fun singlePlayerUndoRestoresSnapshotBeforeHumanAndRobotMoves() {
        val session = JunqiSession.started(
            mode = JunqiMode.SINGLE_PLAYER,
            playerSide = JunqiSide.RED,
            state = quietState(),
        )
        val before = session.state.observation
        val humanMove = JunqiMove(at(3, 1), at(3, 2))

        session.play(humanMove)
        val request = requireNotNull(session.robotRequest)
        val robotMove = JunqiMove(at(8, 1), at(8, 2))
        val afterRobot = session.applyRobotMove(request, robotMove)

        assertEquals(robotMove, afterRobot.lastMove)
        assertTrue(afterRobot.canUndo)
        val undone = session.undo()
        assertEquals(JunqiPhase.PLAYING, undone.phase)
        assertEquals(before, undone.observation)
        assertNull(undone.lastMove)
        assertFalse(undone.canUndo)
        assertNull(session.robotRequest)
    }

    @Test
    fun staleRobotResultIsRejectedAfterUndoChangesGeneration() {
        val session = JunqiSession.started(
            mode = JunqiMode.SINGLE_PLAYER,
            playerSide = JunqiSide.RED,
            state = quietState(),
        )
        session.play(JunqiMove(at(3, 1), at(3, 2)))
        val stale = requireNotNull(session.robotRequest)
        session.undo()
        val before = session.state

        val rejected = session.applyRobotMove(stale, JunqiMove(at(8, 1), at(8, 2)))

        assertSame(before, rejected)
        assertSame(before, session.state)
    }

    @Test
    fun playerWinSwapsSideAndPlayerLossRestoresRed() {
        val won = JunqiSession.started(
            mode = JunqiMode.SINGLE_PLAYER,
            playerSide = JunqiSide.RED,
            state = flagCaptureState(attacker = JunqiSide.RED),
        )
        val wonState = won.play(JunqiMove(at(1, 0), at(1, 1)))
        assertEquals(JunqiResult.RED_WIN, wonState.result)
        assertEquals(JunqiScore(player = 1), wonState.score)
        assertFalse(wonState.canUndo)
        assertEquals(JunqiSide.BLUE, won.restart().playerSide)

        val lost = JunqiSession.started(
            mode = JunqiMode.SINGLE_PLAYER,
            playerSide = JunqiSide.BLUE,
            state = flagCaptureState(attacker = JunqiSide.RED),
        )
        val request = requireNotNull(lost.robotRequest)
        val lostState = lost.applyRobotMove(request, JunqiMove(at(1, 0), at(1, 1)))
        assertEquals(JunqiScore(robot = 1), lostState.score)
        assertFalse(lostState.canUndo)
        assertEquals(JunqiSide.RED, lost.restart().playerSide)
    }

    @Test
    fun drawKeepsScoreSideAndUndoButRestartClearsHistory() {
        val score = JunqiScore(player = 4, robot = 3)
        val session = JunqiSession.started(
            mode = JunqiMode.SINGLE_PLAYER,
            playerSide = JunqiSide.BLUE,
            state = drawOnNextMoveState(currentSide = JunqiSide.BLUE),
            score = score,
        )

        val drawn = session.play(JunqiMove(at(1, 1), at(0, 1)))

        assertEquals(JunqiResult.DRAW, drawn.result)
        assertEquals(score, drawn.score)
        assertEquals(5, drawn.score.intelligenceLevel)
        assertTrue(drawn.canUndo)
        val restarted = session.restart()
        assertEquals(JunqiSide.BLUE, restarted.playerSide)
        assertEquals(score, restarted.score)
        assertEquals(JunqiPhase.DEPLOYMENT, restarted.phase)
        assertFalse(restarted.canUndo)
        assertNull(restarted.lastMove)
    }

    @Test
    fun twoPlayerScoreUsesColorsAndRestartAlwaysReturnsToRedDeployment() {
        val session = JunqiSession.started(
            mode = JunqiMode.TWO_PLAYERS,
            playerSide = JunqiSide.BLUE,
            state = flagCaptureState(attacker = JunqiSide.RED),
        )

        val finished = session.play(JunqiMove(at(1, 0), at(1, 1)))

        assertEquals(JunqiScore(red = 1), finished.score)
        assertFalse(finished.canUndo)
        val restarted = session.restart()
        assertEquals(JunqiSide.RED, restarted.playerSide)
        assertEquals(JunqiSide.RED, restarted.currentSide)
        assertEquals(JunqiPhase.DEPLOYMENT, restarted.phase)
    }

    @Test
    fun everyPhaseIsRepresentedByTheApprovedStateMachine() {
        assertEquals(
            listOf("DEPLOYMENT", "HANDOFF", "PLAYING", "BATTLE_RESULT", "FINISHED"),
            JunqiPhase.entries.map { it.name },
        )
    }

    private fun readyTwoPlayerSession(): JunqiSession = JunqiSession(JunqiMode.TWO_PLAYERS).apply {
        ready()
        acceptHandoff()
        ready()
        acceptHandoff()
    }

    private fun legalSwap(deployment: List<JunqiPiece>): Pair<JunqiPosition, JunqiPosition> {
        for (first in deployment) {
            for (second in deployment) {
                if (first.position == second.position) continue
                if (JunqiDeployment.swapIfLegal(deployment, first.position, second.position) != deployment) {
                    return first.position to second.position
                }
            }
        }
        error("Expected a legal Junqi deployment swap")
    }

    private fun quietMove(observation: JunqiObservation): JunqiMove {
        val occupiedByOpponent = observation.opponentPieces.mapTo(mutableSetOf()) { it.position }
        return observation.visibleLegalMoves().first { it.to !in occupiedByOpponent }
    }

    private fun assertOpaqueHandoff(state: JunqiSessionState, side: JunqiSide) {
        assertEquals(JunqiPhase.HANDOFF, state.phase)
        assertEquals(side, state.currentSide)
        assertNull(state.observation)
        assertTrue(state.deployment.isEmpty())
        assertNull(state.battleOutcome)
        assertNull(state.lastMove)
    }

    private fun quietState(): JunqiState = stateOf(
        piece("red-engineer", JunqiSide.RED, JunqiPieceType.ENGINEER, 3, 1),
        piece("red-flag", JunqiSide.RED, JunqiPieceType.FLAG, 11, 1),
        piece("blue-engineer", JunqiSide.BLUE, JunqiPieceType.ENGINEER, 8, 1),
        piece("blue-flag", JunqiSide.BLUE, JunqiPieceType.FLAG, 0, 1),
    )

    private fun battleState(): JunqiState = stateOf(
        piece("red-commander", JunqiSide.RED, JunqiPieceType.COMMANDER, 3, 0),
        piece("red-flag", JunqiSide.RED, JunqiPieceType.FLAG, 11, 1),
        piece("blue-engineer", JunqiSide.BLUE, JunqiPieceType.ENGINEER, 3, 1),
        piece("blue-spare", JunqiSide.BLUE, JunqiPieceType.ENGINEER, 8, 1),
        piece("blue-flag", JunqiSide.BLUE, JunqiPieceType.FLAG, 0, 1),
    )

    private fun flagCaptureState(attacker: JunqiSide): JunqiState {
        val defender = other(attacker)
        return JunqiState(
            pieces = listOf(
                piece("attacker", attacker, JunqiPieceType.ENGINEER, 1, 0),
                piece("defender-flag", defender, JunqiPieceType.FLAG, 1, 1),
            ).associateBy { it.position },
            currentSide = attacker,
        )
    }

    private fun drawOnNextMoveState(currentSide: JunqiSide): JunqiState {
        val moverRow = if (currentSide == JunqiSide.RED) 10 else 1
        val destinationRow = if (currentSide == JunqiSide.RED) 11 else 0
        return JunqiState(
            pieces = listOf(
                piece("mover", currentSide, JunqiPieceType.ENGINEER, moverRow, 1),
                piece("other-mine", other(currentSide), JunqiPieceType.MINE, if (currentSide == JunqiSide.RED) 0 else 11, 0),
            ).associateBy { it.position },
            currentSide = currentSide,
        ).also {
            assertTrue(JunqiMove(at(moverRow, 1), at(destinationRow, 1)) in JunqiRules.legalMoves(it, currentSide))
        }
    }

    private fun stateOf(vararg pieces: JunqiPiece): JunqiState = JunqiState(pieces.associateBy { it.position })

    private fun piece(id: String, side: JunqiSide, type: JunqiPieceType, row: Int, column: Int) =
        JunqiPiece(id, side, type, at(row, column))

    private fun at(row: Int, column: Int) = JunqiPosition(row, column)
}
