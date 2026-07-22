package com.buddygames.junqi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class JunqiDeploymentTest {
    @Test
    fun defaultDeploymentContainsTheCompleteLegalInventoryForBothSides() {
        for (side in JunqiSide.entries) {
            val deployment = JunqiDeployment.default(side)

            assertEquals(25, deployment.size)
            assertEquals(expectedInventory, deployment.groupingBy { it.type }.eachCount())
            assertEquals(deploymentPositions(side), deployment.mapTo(mutableSetOf()) { it.position })
            assertTrue(JunqiDeployment.isLegal(deployment, side))
        }
    }

    @Test
    fun randomDeploymentIsRepeatableAndLegalAcrossMultipleSeedsForBothSides() {
        for (side in JunqiSide.entries) {
            for (seed in listOf(0L, 1L, 42L, 43L)) {
                val deployment = JunqiDeployment.random(side, seed)

                assertEquals(deployment, JunqiDeployment.random(side, seed))
                assertTrue(JunqiDeployment.isLegal(deployment, side))
            }
        }
    }

    @Test
    fun defaultDeploymentIsDeterministic() {
        for (side in JunqiSide.entries) {
            assertEquals(JunqiDeployment.default(side), JunqiDeployment.default(side))
        }
    }

    @Test
    fun deploymentIdsAreOpaqueAndIndependentOfHiddenRanks() {
        for (side in JunqiSide.entries) {
            val first = JunqiDeployment.random(side, 42).associateBy { it.position }
            val second = JunqiDeployment.random(side, 43).associateBy { it.position }

            assertTrue(first.keys.any { position -> first.getValue(position).type != second.getValue(position).type })
            assertEquals(
                first.mapValues { (_, piece) -> piece.id },
                second.mapValues { (_, piece) -> piece.id },
            )
            assertTrue(
                first.values.all { piece ->
                    JunqiPieceType.entries.none { type ->
                        piece.id.contains(type.name, ignoreCase = true) ||
                            piece.id.contains(type.name.lowercase().replace("_", "-"))
                    }
                },
            )
        }
    }

    @Test
    fun deploymentRejectsEveryInventoryAndOwnershipViolation() {
        val deployment = JunqiDeployment.default(JunqiSide.RED)

        assertFalse(JunqiDeployment.isLegal(deployment.dropLast(1), JunqiSide.RED))
        assertFalse(
            JunqiDeployment.isLegal(
                deployment.mapIndexed { index, piece ->
                    if (index == 0) piece.copy(type = JunqiPieceType.FLAG) else piece
                },
                JunqiSide.RED,
            ),
        )
        assertFalse(
            JunqiDeployment.isLegal(
                deployment.mapIndexed { index, piece ->
                    if (index == 0) piece.copy(side = JunqiSide.BLUE) else piece
                },
                JunqiSide.RED,
            ),
        )
        assertFalse(
            JunqiDeployment.isLegal(
                deployment.mapIndexed { index, piece ->
                    if (index == 1) piece.copy(id = deployment.first().id) else piece
                },
                JunqiSide.RED,
            ),
        )
    }

    @Test
    fun deploymentRejectsCampForeignDuplicateAndMissingStations() {
        val deployment = JunqiDeployment.default(JunqiSide.RED)
        val first = deployment.first()
        val second = deployment[1]

        assertFalse(JunqiDeployment.isLegal(replace(deployment, first, first.copy(position = JunqiPosition(7, 1))), JunqiSide.RED))
        assertFalse(JunqiDeployment.isLegal(replace(deployment, first, first.copy(position = JunqiPosition(5, 0))), JunqiSide.RED))
        assertFalse(JunqiDeployment.isLegal(replace(deployment, second, second.copy(position = first.position)), JunqiSide.RED))
    }

    @Test
    fun flagMustUseOwnHeadquarters() {
        for (side in JunqiSide.entries) {
            val deployment = JunqiDeployment.default(side)
            val flag = deployment.single { it.type == JunqiPieceType.FLAG }
            val nonHeadquarters = deployment.first { it.position !in JunqiBoard.headquarters }

            assertFalse(
                JunqiDeployment.isLegal(
                    swapPositions(deployment, flag.position, nonHeadquarters.position),
                    side,
                ),
            )
        }
    }

    @Test
    fun minesMustStayInTheBackTwoRows() {
        for (side in JunqiSide.entries) {
            val deployment = JunqiDeployment.default(side)
            val mine = deployment.first { it.type == JunqiPieceType.MINE }
            val front = deployment.first { it.position.row == if (side == JunqiSide.RED) 6 else 5 }

            assertFalse(JunqiDeployment.isLegal(swapPositions(deployment, mine.position, front.position), side))
        }
    }

    @Test
    fun bombsCannotUseTheFirstLine() {
        for (side in JunqiSide.entries) {
            val deployment = JunqiDeployment.default(side)
            val bomb = deployment.first { it.type == JunqiPieceType.BOMB }
            val front = deployment.first { it.position.row == if (side == JunqiSide.RED) 6 else 5 }

            assertFalse(JunqiDeployment.isLegal(swapPositions(deployment, bomb.position, front.position), side))
        }
    }

    @Test
    fun swapChangesHiddenTypesWithoutMovingOpaqueSlotIdentities() {
        val deployment = JunqiDeployment.default(JunqiSide.RED)
        val first = deployment.single { it.type == JunqiPieceType.COMMANDER }
        val second = deployment.single { it.type == JunqiPieceType.ARMY_COMMANDER }
        val legal = JunqiDeployment.swapIfLegal(deployment, first.position, second.position)
        val flag = deployment.single { it.type == JunqiPieceType.FLAG }
        val illegalTarget = deployment.first { it.position !in JunqiBoard.headquarters }
        val illegal = JunqiDeployment.swapIfLegal(deployment, flag.position, illegalTarget.position)
        val originalByPosition = deployment.associateBy { it.position }
        val legalByPosition = legal.associateBy { it.position }

        assertTrue(JunqiDeployment.isLegal(legal, JunqiSide.RED))
        assertEquals(second.type, legalByPosition.getValue(first.position).type)
        assertEquals(first.type, legalByPosition.getValue(second.position).type)
        for ((position, original) in originalByPosition) {
            val swapped = legalByPosition.getValue(position)
            assertEquals(original.id, swapped.id)
            assertEquals(original.side, swapped.side)
            assertEquals(original.position, swapped.position)
            assertEquals(original.hasMoved, swapped.hasMoved)
        }
        assertEquals(legal, JunqiDeployment.swapIfLegal(deployment, first.position, second.position))
        assertSame(deployment, illegal)
        assertSame(
            deployment,
            JunqiDeployment.swapIfLegal(deployment, JunqiPosition(5, 0), first.position),
        )
    }

    private fun replace(
        deployment: List<JunqiPiece>,
        original: JunqiPiece,
        replacement: JunqiPiece,
    ): List<JunqiPiece> = deployment.map { if (it.id == original.id) replacement else it }

    private fun swapPositions(
        deployment: List<JunqiPiece>,
        first: JunqiPosition,
        second: JunqiPosition,
    ): List<JunqiPiece> = deployment.map { piece ->
        when (piece.position) {
            first -> piece.copy(position = second)
            second -> piece.copy(position = first)
            else -> piece
        }
    }

    private fun deploymentPositions(side: JunqiSide): Set<JunqiPosition> {
        val rows = if (side == JunqiSide.RED) 6..11 else 0..5
        return rows.flatMap { row -> (0..4).map { column -> JunqiPosition(row, column) } }
            .filterNotTo(mutableSetOf()) { it in JunqiBoard.camps }
    }

    private companion object {
        val expectedInventory = mapOf(
            JunqiPieceType.COMMANDER to 1,
            JunqiPieceType.ARMY_COMMANDER to 1,
            JunqiPieceType.DIVISION_COMMANDER to 2,
            JunqiPieceType.BRIGADE_COMMANDER to 2,
            JunqiPieceType.REGIMENT to 2,
            JunqiPieceType.BATTALION to 2,
            JunqiPieceType.COMPANY to 3,
            JunqiPieceType.PLATOON to 3,
            JunqiPieceType.ENGINEER to 3,
            JunqiPieceType.MINE to 3,
            JunqiPieceType.BOMB to 2,
            JunqiPieceType.FLAG to 1,
        )
    }
}
