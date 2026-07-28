package com.buddygames.doushouqi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DoushouqiSessionTest {
    @Test
    fun scoresFollowPlayerIdentityAndDriveLevel() {
        val session = DoushouqiSession(
            DoushouqiMode.SINGLE_PLAYER,
            winningState(DoushouqiSide.PINE_GREEN),
        )

        val finished = session.play(move(pos(1, 3), pos(0, 3)))

        assertEquals(1, finished.score.player)
        assertEquals(0, finished.score.robot)
        assertEquals(2, finished.intelligenceLevel.level)
    }

    @Test
    fun twoPlayerScoresUseBoardSides() {
        val session = DoushouqiSession(
            DoushouqiMode.TWO_PLAYERS,
            winningState(DoushouqiSide.PINE_GREEN),
        )

        val finished = session.play(move(pos(1, 3), pos(0, 3)))

        assertEquals(1, finished.score.green)
        assertEquals(0, finished.score.vermilion)
    }

    @Test
    fun twoPlayerRestartAlwaysReturnsToGreenFirst() {
        val session = DoushouqiSession(
            DoushouqiMode.TWO_PLAYERS,
            winningState(DoushouqiSide.PINE_GREEN),
        )
        session.play(move(pos(1, 3), pos(0, 3)))

        val restarted = session.restart()
        assertEquals(DoushouqiSide.PINE_GREEN, restarted.position.sideToMove)
        assertEquals(DoushouqiSide.PINE_GREEN, restarted.playerSide)
    }

    @Test
    fun restartSwapsAfterPlayerWinAndRobotOpensWithoutUndoHistory() {
        val session = DoushouqiSession(
            DoushouqiMode.SINGLE_PLAYER,
            winningState(DoushouqiSide.PINE_GREEN),
        )
        session.play(move(pos(1, 3), pos(0, 3)))

        val restarted = session.restart()

        assertEquals(DoushouqiSide.VERMILION, restarted.playerSide)
        assertEquals(0, restarted.historySize)
        assertNotNull(restarted.robotRequest)
        val opening = requireNotNull(restarted.robotRequest)
        val afterOpening = session.applyRobotMove(
            opening,
            DoushouqiRules.legalMoves(opening.state).first(),
        )
        assertEquals(0, afterOpening.historySize)
    }

    @Test
    fun restartAfterPlayerLossRestoresGreenWhileDrawPreservesSideAndScore() {
        val loss = DoushouqiSession(
            DoushouqiMode.SINGLE_PLAYER,
            winningState(DoushouqiSide.PINE_GREEN),
            playerSide = DoushouqiSide.VERMILION,
        )
        val lossRequest = requireNotNull(loss.state().robotRequest)
        loss.applyRobotMove(lossRequest, move(pos(1, 3), pos(0, 3)))
        assertEquals(DoushouqiSide.PINE_GREEN, loss.restart().playerSide)

        val drawnState = stateOf(
            pos(4, 0) to green(DoushouqiAnimal.CAT),
            pos(4, 6) to red(DoushouqiAnimal.CAT),
        ).copyWith(result = DoushouqiResult.Draw(DoushouqiDrawReason.REPETITION))
        val draw = DoushouqiSession(
            DoushouqiMode.SINGLE_PLAYER,
            drawnState,
            playerSide = DoushouqiSide.VERMILION,
            initialScore = DoushouqiScore(player = 4, robot = 2),
        )

        val restartedDraw = draw.restart()

        assertEquals(DoushouqiSide.VERMILION, restartedDraw.playerSide)
        assertEquals(DoushouqiScore(player = 4, robot = 2), restartedDraw.score)
        assertEquals(5, restartedDraw.intelligenceLevel.level)
    }

    @Test
    fun singlePlayerUndoRestoresBeforeHumanAndRobotPair() {
        val session = DoushouqiSession(DoushouqiMode.SINGLE_PLAYER)
        val initial = session.state()
        val humanMove = DoushouqiRules.legalMoves(initial.position).first()
        val afterHuman = session.play(humanMove)
        val request = requireNotNull(afterHuman.robotRequest)
        session.applyRobotMove(request, DoushouqiRules.legalMoves(request.state).first())

        val restored = session.undo()

        assertEquals(initial.position.positionKey, restored.position.positionKey)
        assertEquals(initial.position.lastMove, restored.position.lastMove)
        assertEquals(initial.position.repetitionCounts, restored.position.repetitionCounts)
        assertEquals(0, restored.historySize)
    }

    @Test
    fun singlePlayerPublishesPlayerAndRobotCapturesOnlyAfterRobotReply() {
        val session = pairedCaptureSession()

        val afterPlayer = session.play(move(pos(4, 0), pos(3, 0)))
        assertEquals(DoushouqiRoundCaptures(), afterPlayer.lastCompletedRoundCaptures)

        val afterRobot = session.applyRobotMove(
            requireNotNull(afterPlayer.robotRequest),
            move(pos(2, 1), pos(2, 2)),
        )

        assertEquals(
            DoushouqiRoundCaptures(
                capturedByGreen = red(DoushouqiAnimal.RAT),
                capturedByRed = green(DoushouqiAnimal.RAT),
            ),
            afterRobot.lastCompletedRoundCaptures,
        )
    }

    @Test
    fun completedQuietSinglePlayerRoundReplacesOlderCaptureSummary() {
        val session = pairedCaptureSession()
        completePairedCaptureRound(session)
        val beforeQuietRound = session.state()

        val playerMove = DoushouqiRules.legalMoves(beforeQuietRound.position)
            .first { beforeQuietRound.position.pieceAt(it.to) == null }
        val afterPlayer = session.play(playerMove)
        assertEquals(
            beforeQuietRound.lastCompletedRoundCaptures,
            afterPlayer.lastCompletedRoundCaptures,
        )
        val request = requireNotNull(afterPlayer.robotRequest)
        val robotMove = DoushouqiRules.legalMoves(request.state)
            .first { request.state.pieceAt(it.to) == null }

        val completed = session.applyRobotMove(request, robotMove)

        assertEquals(DoushouqiRoundCaptures(), completed.lastCompletedRoundCaptures)
    }

    @Test
    fun terminalPlayerMovePublishesOneMoveRoundImmediately() {
        val session = DoushouqiSession(
            DoushouqiMode.SINGLE_PLAYER,
            stateOf(
                sideToMove = DoushouqiSide.PINE_GREEN,
                pos(4, 0) to green(DoushouqiAnimal.CAT),
                pos(3, 0) to red(DoushouqiAnimal.RAT),
            ),
        )

        val finished = session.play(move(pos(4, 0), pos(3, 0)))

        assertEquals(
            DoushouqiRoundCaptures(
                capturedByGreen = red(DoushouqiAnimal.RAT),
            ),
            finished.lastCompletedRoundCaptures,
        )
        assertNull(finished.robotRequest)
    }

    @Test
    fun robotOpeningDoesNotPublishRoundCaptureSummary() {
        val session = DoushouqiSession(
            DoushouqiMode.SINGLE_PLAYER,
            stateOf(
                sideToMove = DoushouqiSide.PINE_GREEN,
                pos(4, 0) to green(DoushouqiAnimal.CAT),
                pos(3, 0) to red(DoushouqiAnimal.RAT),
                pos(6, 6) to red(DoushouqiAnimal.CAT),
            ),
            playerSide = DoushouqiSide.VERMILION,
        )
        val request = requireNotNull(session.state().robotRequest)

        val opened = session.applyRobotMove(request, move(pos(4, 0), pos(3, 0)))

        assertEquals(DoushouqiRoundCaptures(), opened.lastCompletedRoundCaptures)
        assertEquals(0, opened.historySize)
    }

    @Test
    fun twoPlayerPublishesOnlyAfterRedCompletesRound() {
        val session = DoushouqiSession(
            DoushouqiMode.TWO_PLAYERS,
            pairedCaptureState(),
        )

        val afterGreen = session.play(move(pos(4, 0), pos(3, 0)))
        assertEquals(DoushouqiRoundCaptures(), afterGreen.lastCompletedRoundCaptures)

        val afterRed = session.play(move(pos(2, 1), pos(2, 2)))

        assertEquals(
            DoushouqiRoundCaptures(
                capturedByGreen = red(DoushouqiAnimal.RAT),
                capturedByRed = green(DoushouqiAnimal.RAT),
            ),
            afterRed.lastCompletedRoundCaptures,
        )
    }

    @Test
    fun terminalGreenMovePublishesTwoPlayerRoundImmediately() {
        val session = DoushouqiSession(
            DoushouqiMode.TWO_PLAYERS,
            stateOf(
                sideToMove = DoushouqiSide.PINE_GREEN,
                pos(4, 0) to green(DoushouqiAnimal.CAT),
                pos(3, 0) to red(DoushouqiAnimal.RAT),
            ),
        )

        val finished = session.play(move(pos(4, 0), pos(3, 0)))

        assertEquals(
            DoushouqiRoundCaptures(
                capturedByGreen = red(DoushouqiAnimal.RAT),
            ),
            finished.lastCompletedRoundCaptures,
        )
    }

    @Test
    fun undoRestoresCompletedRoundAndRestartClearsIt() {
        val session = pairedCaptureSession()
        completePairedCaptureRound(session)
        assertEquals(
            DoushouqiRoundCaptures(
                capturedByGreen = red(DoushouqiAnimal.RAT),
                capturedByRed = green(DoushouqiAnimal.RAT),
            ),
            session.state().lastCompletedRoundCaptures,
        )

        assertEquals(
            DoushouqiRoundCaptures(),
            session.undo().lastCompletedRoundCaptures,
        )
        val afterPlayer = session.play(move(pos(4, 0), pos(3, 0)))
        session.applyRobotMove(
            requireNotNull(afterPlayer.robotRequest),
            move(pos(2, 1), pos(2, 2)),
        )
        assertEquals(
            DoushouqiRoundCaptures(),
            session.restart().lastCompletedRoundCaptures,
        )
    }

    @Test
    fun staleRobotRequestDoesNotChangeCompletedRoundCaptureSummary() {
        val session = pairedCaptureSession()
        val afterPlayer = session.play(move(pos(4, 0), pos(3, 0)))
        val request = requireNotNull(afterPlayer.robotRequest)
        val stale = request.copy(sourcePositionKey = request.sourcePositionKey + 1)

        val unchanged = session.applyRobotMove(
            stale,
            move(pos(2, 1), pos(2, 2)),
        )

        assertEquals(
            DoushouqiRoundCaptures(),
            unchanged.lastCompletedRoundCaptures,
        )
        assertEquals(afterPlayer.position.positionKey, unchanged.position.positionKey)

        val completed = session.applyRobotMove(
            request,
            move(pos(2, 1), pos(2, 2)),
        )
        assertEquals(
            DoushouqiRoundCaptures(
                capturedByGreen = red(DoushouqiAnimal.RAT),
                capturedByRed = green(DoushouqiAnimal.RAT),
            ),
            completed.lastCompletedRoundCaptures,
        )
    }

    @Test
    fun twoPlayerUndoRestoresExactlyOneMove() {
        val session = DoushouqiSession(
            DoushouqiMode.TWO_PLAYERS,
            pairedCaptureState(),
        )
        val initial = session.state()
        session.play(move(pos(4, 0), pos(3, 0)))

        val restored = session.undo()

        assertEquals(initial.position.positionKey, restored.position.positionKey)
        assertEquals(0, restored.historySize)
        assertEquals(
            DoushouqiRoundCaptures(),
            restored.lastCompletedRoundCaptures,
        )
    }

    @Test
    fun twoPlayerUndoOfRedMoveRestoresPendingGreenHalfRound() {
        val session = DoushouqiSession(
            DoushouqiMode.TWO_PLAYERS,
            pairedCaptureState(),
        )
        session.play(move(pos(4, 0), pos(3, 0)))
        session.play(move(pos(2, 1), pos(2, 2)))

        val afterUndo = session.undo()
        assertEquals(DoushouqiRoundCaptures(), afterUndo.lastCompletedRoundCaptures)

        val replayed = session.play(move(pos(2, 1), pos(2, 2)))
        assertEquals(
            DoushouqiRoundCaptures(
                capturedByGreen = red(DoushouqiAnimal.RAT),
                capturedByRed = green(DoushouqiAnimal.RAT),
            ),
            replayed.lastCompletedRoundCaptures,
        )
    }

    @Test
    fun staleRequestsAreRejectedAfterMoveUndoRestartAndInvalidate() {
        val session = DoushouqiSession(DoushouqiMode.SINGLE_PLAYER)
        val human = DoushouqiRules.legalMoves(session.state().position).first()
        val request = requireNotNull(session.play(human).robotRequest)
        val robotMove = DoushouqiRules.legalMoves(request.state).first()

        session.undo()
        assertEquals(
            session.state().position.positionKey,
            session.applyRobotMove(request, robotMove).position.positionKey,
        )

        val request2 = requireNotNull(session.play(human).robotRequest)
        session.restart()
        val beforeStale = session.state()
        assertEquals(beforeStale, session.applyRobotMove(request2, robotMove))

        session.invalidate()
        assertEquals(session.state(), session.applyRobotMove(request2, robotMove))
    }

    @Test
    fun validRobotRequestAppliesOnlyOnceAndRequiresSourceKey() {
        val session = DoushouqiSession(DoushouqiMode.SINGLE_PLAYER)
        val request = requireNotNull(
            session.play(DoushouqiRules.legalMoves(session.state().position).first())
                .robotRequest,
        )
        val robotMove = DoushouqiRules.legalMoves(request.state).first()
        val wrongKey = request.copy(sourcePositionKey = request.sourcePositionKey + 1)
        val unchanged = session.applyRobotMove(wrongKey, robotMove)
        assertEquals(request.sourcePositionKey, unchanged.position.positionKey)

        val applied = session.applyRobotMove(request, robotMove)
        assertFalse(applied.position.positionKey == request.sourcePositionKey)
        assertEquals(applied, session.applyRobotMove(request, robotMove))
    }

    @Test
    fun viewMappingAndVisibilityFollowModeAndResult() {
        assertEquals(pos(8, 6), modelPosition(0, 0, rotated = true))
        assertEquals(pos(0, 0), modelPosition(0, 0, rotated = false))
        assertEquals(pos(0, 0), displayPosition(pos(8, 6), rotated = true))
        assertTrue(
            shouldRotateDoushouqiBoard(
                DoushouqiMode.SINGLE_PLAYER,
                DoushouqiSide.VERMILION,
            ),
        )
        assertFalse(
            shouldRotateDoushouqiBoard(
                DoushouqiMode.TWO_PLAYERS,
                DoushouqiSide.VERMILION,
            ),
        )
        assertFalse(
            shouldShowDoushouqiUndo(
                DoushouqiResult.Win(
                    DoushouqiSide.PINE_GREEN,
                    DoushouqiWinReason.DEN,
                ),
            ),
        )
        assertTrue(
            shouldShowDoushouqiUndo(
                DoushouqiResult.Draw(DoushouqiDrawReason.QUIET_100),
            ),
        )
    }

    @Test
    fun tapCandidatesAlwaysComeFromCurrentPosition() {
        val initial = DoushouqiState.initial()
        val first = DoushouqiRules.legalMoves(initial).first()
        val after = requireNotNull(DoushouqiRules.apply(initial, first))
        val current = DoushouqiRules.legalMoves(after).first()

        assertNull(doushouqiTapMove(after, first.from, first.to))
        assertEquals(current, doushouqiTapMove(after, current.from, current.to))
    }

    private fun winningState(side: DoushouqiSide): DoushouqiState =
        if (side == DoushouqiSide.PINE_GREEN) {
            stateOf(
                sideToMove = side,
                pos(1, 3) to green(DoushouqiAnimal.CAT),
                pos(4, 6) to red(DoushouqiAnimal.DOG),
            )
        } else {
            stateOf(
                sideToMove = side,
                pos(7, 3) to red(DoushouqiAnimal.CAT),
                pos(4, 0) to green(DoushouqiAnimal.DOG),
            )
        }

    private fun pairedCaptureSession(): DoushouqiSession = DoushouqiSession(
        DoushouqiMode.SINGLE_PLAYER,
        pairedCaptureState(),
    )

    private fun pairedCaptureState(): DoushouqiState = stateOf(
        sideToMove = DoushouqiSide.PINE_GREEN,
        pos(4, 0) to green(DoushouqiAnimal.CAT),
        pos(3, 0) to red(DoushouqiAnimal.RAT),
        pos(2, 2) to green(DoushouqiAnimal.RAT),
        pos(2, 1) to red(DoushouqiAnimal.CAT),
    )

    private fun completePairedCaptureRound(session: DoushouqiSession) {
        val afterPlayer = session.play(move(pos(4, 0), pos(3, 0)))
        session.applyRobotMove(
            requireNotNull(afterPlayer.robotRequest),
            move(pos(2, 1), pos(2, 2)),
        )
    }
}
