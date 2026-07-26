package com.buddygames.doushouqi

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DoushouqiCaptureTest {
    @Test
    fun equalOrHigherRankCapturesWhileLowerRankCannot() {
        val from = pos(4, 0)
        val to = pos(5, 0)

        assertTrue(
            move(from, to) in legal(
                stateOf(
                    from to green(DoushouqiAnimal.CAT),
                    to to red(DoushouqiAnimal.CAT),
                ),
            ),
        )
        assertTrue(
            move(from, to) in legal(
                stateOf(
                    from to green(DoushouqiAnimal.DOG),
                    to to red(DoushouqiAnimal.CAT),
                ),
            ),
        )
        assertFalse(
            move(from, to) in legal(
                stateOf(
                    from to green(DoushouqiAnimal.CAT),
                    to to red(DoushouqiAnimal.DOG),
                ),
            ),
        )
    }

    @Test
    fun landRatCapturesElephantButElephantCannotCaptureRat() {
        val from = pos(4, 0)
        val to = pos(5, 0)

        assertTrue(
            move(from, to) in legal(
                stateOf(
                    from to green(DoushouqiAnimal.RAT),
                    to to red(DoushouqiAnimal.ELEPHANT),
                ),
            ),
        )
        assertFalse(
            move(from, to) in legal(
                stateOf(
                    from to green(DoushouqiAnimal.ELEPHANT),
                    to to red(DoushouqiAnimal.RAT),
                ),
            ),
        )
    }

    @Test
    fun ratsCaptureEachOtherOnlyWithinTheSameTerrainDomain() {
        val landA = pos(3, 0)
        val landB = pos(4, 0)
        val waterA = pos(3, 1)
        val waterB = pos(4, 1)

        assertTrue(
            move(landA, landB) in legal(
                stateOf(landA to green(DoushouqiAnimal.RAT), landB to red(DoushouqiAnimal.RAT)),
            ),
        )
        assertTrue(
            move(waterA, waterB) in legal(
                stateOf(waterA to green(DoushouqiAnimal.RAT), waterB to red(DoushouqiAnimal.RAT)),
            ),
        )
        assertFalse(
            move(landA, waterA) in legal(
                stateOf(landA to green(DoushouqiAnimal.RAT), waterA to red(DoushouqiAnimal.RAT)),
            ),
        )
        assertFalse(
            move(waterA, landA) in legal(
                stateOf(waterA to green(DoushouqiAnimal.RAT), landA to red(DoushouqiAnimal.RAT)),
            ),
        )
    }

    @Test
    fun lionJumpMayCaptureLowerRankLandingPiece() {
        val from = pos(3, 0)
        val to = pos(3, 3)
        val state = stateOf(
            from to green(DoushouqiAnimal.LION),
            to to red(DoushouqiAnimal.TIGER),
        )

        assertTrue(move(from, to) in legal(state))
    }

    @Test
    fun enemyPieceInOwnedTrapHasEffectiveRankZero() {
        val from = pos(6, 3)
        val trap = pos(7, 3)
        val state = stateOf(
            from to green(DoushouqiAnimal.RAT),
            trap to red(DoushouqiAnimal.ELEPHANT),
        )

        assertTrue(move(from, trap) in legal(state))
    }

    @Test
    fun trappedAttackerRegainsNormalRankWhenLeavingTrap() {
        val enemyTrap = pos(1, 3)
        val destination = pos(2, 3)
        val state = stateOf(
            enemyTrap to green(DoushouqiAnimal.DOG),
            destination to red(DoushouqiAnimal.CAT),
        )

        assertTrue(move(enemyTrap, destination) in legal(state))
    }
}
