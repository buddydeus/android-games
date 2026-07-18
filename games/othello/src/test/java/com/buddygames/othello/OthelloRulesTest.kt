package com.buddygames.othello

import com.buddygames.api.GameMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OthelloRulesTest {
    @Test
    fun gameVersionAndMainMenuLabelStayAligned() {
        assertEquals(3, OthelloPlugin.manifest.versionCode)
        assertEquals("0.0.3", OthelloPlugin.manifest.versionName)
        assertEquals("版本 0.0.3", othelloVersionLabel(OthelloPlugin.manifest.versionName))
    }

    @Test
    fun undoButtonHidesAfterWinButRemainsForDraw() {
        assertTrue(shouldShowOthelloUndo(gameOver = false, winner = Disc.BLACK))
        assertFalse(shouldShowOthelloUndo(gameOver = true, winner = Disc.BLACK))
        assertFalse(shouldShowOthelloUndo(gameOver = true, winner = Disc.WHITE))
        assertTrue(shouldShowOthelloUndo(gameOver = true, winner = null))
    }

    @Test
    fun menuUsesUnifiedGameModeLabels() {
        assertEquals(listOf("单人模式", "双人对战", "退出游戏"), othelloMenuLabels())
    }

    @Test
    fun legalMoveHintsOnlyShowInSinglePlayerMode() {
        assertTrue(showOthelloLegalMoveHints(GameMode.SINGLE_PLAYER))
        assertFalse(showOthelloLegalMoveHints(GameMode.TWO_PLAYERS))
    }

    @Test
    fun scoreRecordsWinnersAndIgnoresDraw() {
        val score = OthelloScore()
            .record(Disc.BLACK)
            .record(null)
            .record(Disc.WHITE)

        assertEquals(OthelloScore(black = 1, white = 1), score)
        assertEquals("1 : 1", score.displayText)
    }

    @Test
    fun newRoundRestoresInitialBoardAndBlackTurn() {
        val round = newOthelloRound()

        assertEquals(Disc.BLACK, round.playerDisc)
        assertEquals(Disc.BLACK, round.turn)
        assertEquals(OthelloState.initial(), round.state)
        assertNull(round.lastMove)
    }

    @Test
    fun singlePlayerVictorySwitchesSidesForNextRound() {
        assertEquals(Disc.WHITE, nextOthelloPlayerDisc(Disc.BLACK, Disc.BLACK))
        assertEquals(Disc.BLACK, nextOthelloPlayerDisc(Disc.WHITE, Disc.WHITE))
    }

    @Test
    fun singlePlayerDefeatAlwaysRestartsAsFirstPlayer() {
        assertEquals(Disc.BLACK, nextOthelloPlayerDisc(Disc.BLACK, Disc.WHITE))
        assertEquals(Disc.BLACK, nextOthelloPlayerDisc(Disc.WHITE, Disc.BLACK))
    }

    @Test
    fun whitePlayerRoundStartsAfterBlackRobotOpening() {
        val round = newOthelloRound(Disc.WHITE)

        assertEquals(Disc.WHITE, round.playerDisc)
        assertEquals(Disc.WHITE, round.turn)
        assertEquals(4, round.state.count(Disc.BLACK))
        assertEquals(1, round.state.count(Disc.WHITE))
        val lastMove = requireNotNull(round.lastMove)
        assertEquals(Disc.BLACK, round.state.cell(lastMove.row, lastMove.col))
    }

    @Test
    fun undoRestoresLatestPositionIncludingScore() {
        val first = OthelloSnapshot(
            state = OthelloState.initial(),
            turn = Disc.BLACK,
            score = OthelloScore(),
            lastMove = null
        )
        val moved = OthelloRules.applyMove(first.state, OthelloMove(2, 3), Disc.BLACK)
        val second = first.copy(
            state = moved,
            turn = Disc.WHITE,
            score = OthelloScore(black = 3, white = 2),
            lastMove = OthelloMove(2, 3)
        )

        val undo = undoOthello(listOf(first, second))

        assertEquals(second, undo?.snapshot)
        assertEquals(listOf(first), undo?.remainingHistory)
        assertNull(undoOthello(emptyList()))
    }

    @Test
    fun lastMoveCellUsesNewlyPlacedDiscRatherThanFlippedDiscs() {
        assertEquals(2 to 3, othelloLastMoveCell(OthelloMove(2, 3)))
        assertNull(othelloLastMoveCell(null))
    }

    @Test
    fun boardSideUsesAvailableSpaceWithinReferenceBounds() {
        assertEquals(288f, othelloBoardSide(240f, 500f), 0.001f)
        assertEquals(560f, othelloBoardSide(720f, 560f), 0.001f)
        assertEquals(672f, othelloBoardSide(900f, 800f), 0.001f)
    }

    @Test
    fun initialBoardHasFourLegalBlackMoves() {
        val moves = OthelloRules.legalMoves(OthelloState.initial(), Disc.BLACK)

        assertEquals(
            setOf(OthelloMove(2, 3), OthelloMove(3, 2), OthelloMove(4, 5), OthelloMove(5, 4)),
            moves.toSet()
        )
    }

    @Test
    fun applyingMoveFlipsCapturedDiscs() {
        val state = OthelloRules.applyMove(OthelloState.initial(), OthelloMove(2, 3), Disc.BLACK)

        assertEquals(Disc.BLACK, state.cell(3, 3))
        assertEquals(4, state.count(Disc.BLACK))
        assertEquals(1, state.count(Disc.WHITE))
    }

    @Test
    fun robotPrefersCorner() {
        val state = OthelloState.empty()
            .put(0, 1, Disc.WHITE)
            .put(0, 2, Disc.BLACK)
            .put(1, 0, Disc.WHITE)
            .put(2, 0, Disc.BLACK)

        assertEquals(OthelloMove(0, 0), OthelloRules.robotMove(state, Disc.BLACK))
    }

    @Test
    fun gameEndsWhenNeitherSideCanMove() {
        val full = OthelloState(List(8) { List(8) { Disc.BLACK } })

        assertTrue(OthelloRules.isGameOver(full))
    }
}
