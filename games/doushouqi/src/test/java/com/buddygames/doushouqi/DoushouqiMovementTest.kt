package com.buddygames.doushouqi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DoushouqiMovementTest {
    @Test
    fun ordinaryPiecesMoveOneSquareOrthogonallyButNotDiagonally() {
        val from = pos(2, 3)
        val state = stateOf(from to green(DoushouqiAnimal.CAT))
        val moves = legal(state)

        assertTrue(move(from, pos(1, 3)) in moves)
        assertTrue(move(from, pos(2, 2)) in moves)
        assertTrue(move(from, pos(2, 4)) in moves)
        assertTrue(move(from, pos(3, 3)) in moves)
        assertFalse(move(from, pos(1, 2)) in moves)
        assertFalse(move(from, pos(0, 3)) in moves)
    }

    @Test
    fun bothSidesMayEnterTheirOwnDenWithoutWinning() {
        val greenFrom = pos(7, 3)
        val greenDen = pos(8, 3)
        val greenState = stateOf(
            greenFrom to green(DoushouqiAnimal.CAT),
            pos(4, 6) to red(DoushouqiAnimal.DOG),
        )

        assertTrue(move(greenFrom, greenDen) in legal(greenState))
        assertEquals(
            null,
            requireNotNull(
                DoushouqiRules.apply(greenState, move(greenFrom, greenDen)),
            ).result,
        )

        val redFrom = pos(1, 3)
        val redDen = pos(0, 3)
        val redState = stateOf(
            DoushouqiSide.VERMILION,
            redFrom to red(DoushouqiAnimal.CAT),
            pos(4, 0) to green(DoushouqiAnimal.DOG),
        )

        assertTrue(move(redFrom, redDen) in legal(redState))
        assertEquals(
            null,
            requireNotNull(
                DoushouqiRules.apply(redState, move(redFrom, redDen)),
            ).result,
        )
    }

    @Test
    fun friendlyPieceStillBlocksOwnDen() {
        val from = pos(7, 3)
        val ownDen = pos(8, 3)
        val state = stateOf(
            from to green(DoushouqiAnimal.CAT),
            ownDen to green(DoushouqiAnimal.DOG),
        )

        assertFalse(move(from, ownDen) in legal(state))
    }

    @Test
    fun ratEntersAndLeavesRiverWhileOtherAnimalsCannot() {
        val land = pos(3, 0)
        val river = pos(3, 1)

        assertTrue(
            move(land, river) in legal(stateOf(land to green(DoushouqiAnimal.RAT))),
        )
        assertTrue(
            move(river, land) in legal(stateOf(river to green(DoushouqiAnimal.RAT))),
        )
        assertFalse(
            move(land, river) in legal(stateOf(land to green(DoushouqiAnimal.DOG))),
        )
    }

    @Test
    fun lionAndTigerJumpAcrossACompleteRiverSpan() {
        val horizontalFrom = pos(3, 0)
        val horizontalTo = pos(3, 3)
        val verticalFrom = pos(2, 1)
        val verticalTo = pos(6, 1)
        val state = stateOf(
            horizontalFrom to green(DoushouqiAnimal.LION),
            verticalFrom to green(DoushouqiAnimal.TIGER),
        )

        assertTrue(move(horizontalFrom, horizontalTo) in legal(state))
        assertTrue(move(verticalFrom, verticalTo) in legal(state))
        assertFalse(move(horizontalFrom, pos(3, 2)) in legal(state))
        assertFalse(move(verticalFrom, pos(5, 1)) in legal(state))
    }

    @Test
    fun eitherSidesRatBlocksLionAndTigerJumps() {
        listOf(
            green(DoushouqiAnimal.RAT),
            red(DoushouqiAnimal.RAT),
        ).forEach { blocker ->
            val state = stateOf(
                pos(3, 0) to green(DoushouqiAnimal.LION),
                pos(3, 1) to blocker,
            )

            assertFalse(move(pos(3, 0), pos(3, 3)) in legal(state))
        }
    }
}
