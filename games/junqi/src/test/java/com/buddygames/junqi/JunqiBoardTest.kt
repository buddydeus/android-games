package com.buddygames.junqi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JunqiBoardTest {
    @Test
    fun boardContainsEveryPositionAndSpecialStation() {
        val everyPosition = (0..11).flatMap { row ->
            (0..4).map { column -> JunqiPosition(row, column) }
        }.toSet()

        assertEquals(everyPosition, JunqiBoard.roadNeighbors.keys)
        assertEquals(everyPosition, JunqiBoard.railNeighbors.keys)
        assertEquals(
            setOf(JunqiPosition(0, 1), JunqiPosition(0, 3), JunqiPosition(11, 1), JunqiPosition(11, 3)),
            JunqiBoard.headquarters,
        )
        assertEquals(
            setOf(
                JunqiPosition(2, 1), JunqiPosition(2, 3), JunqiPosition(3, 2), JunqiPosition(4, 1), JunqiPosition(4, 3),
                JunqiPosition(7, 1), JunqiPosition(7, 3), JunqiPosition(8, 2), JunqiPosition(9, 1), JunqiPosition(9, 3),
            ),
            JunqiBoard.camps,
        )
    }

    @Test
    fun roadGraphContainsExactlyTheApprovedEdges() {
        assertEquals(expectedRoadNeighbors(), JunqiBoard.roadNeighbors)
    }

    @Test
    fun railGraphContainsExactlyTheApprovedEdges() {
        assertEquals(expectedRailNeighbors(), JunqiBoard.railNeighbors)
    }

    private fun expectedRoadNeighbors(): Map<JunqiPosition, Set<JunqiPosition>> = expectedNeighbors {
        for (row in 0..11) {
            for (column in 0 until 4) {
                link(JunqiPosition(row, column), JunqiPosition(row, column + 1))
            }
        }
        for (column in 0..4) {
            link(JunqiPosition(0, column), JunqiPosition(1, column))
            link(JunqiPosition(10, column), JunqiPosition(11, column))
        }
        for (row in 1..10) {
            for (column in setOf(0, 2, 4)) {
                link(JunqiPosition(row, column), JunqiPosition(row + 1, column))
            }
        }
        for (camp in setOf(
            JunqiPosition(2, 1), JunqiPosition(2, 3), JunqiPosition(3, 2), JunqiPosition(4, 1), JunqiPosition(4, 3),
            JunqiPosition(7, 1), JunqiPosition(7, 3), JunqiPosition(8, 2), JunqiPosition(9, 1), JunqiPosition(9, 3),
        )) {
            for (rowOffset in setOf(-1, 1)) {
                for (columnOffset in setOf(-1, 1)) {
                    link(camp, JunqiPosition(camp.row + rowOffset, camp.column + columnOffset))
                }
            }
        }
    }

    private fun expectedRailNeighbors(): Map<JunqiPosition, Set<JunqiPosition>> = expectedNeighbors {
        for (row in setOf(1, 5, 6, 10)) {
            for (column in 0 until 4) {
                link(JunqiPosition(row, column), JunqiPosition(row, column + 1))
            }
        }
        for (column in setOf(0, 4)) {
            for (row in 1 until 10) {
                link(JunqiPosition(row, column), JunqiPosition(row + 1, column))
            }
        }
        link(JunqiPosition(5, 2), JunqiPosition(6, 2))
    }

    private fun expectedNeighbors(build: NeighborBuilder.() -> Unit): Map<JunqiPosition, Set<JunqiPosition>> {
        val builder = NeighborBuilder()
        builder.build()
        return builder.neighbors.mapValues { it.value.toSet() }
    }

    private class NeighborBuilder {
        val neighbors = (0..11).flatMap { row ->
            (0..4).map { column -> JunqiPosition(row, column) }
        }.associateWith { mutableSetOf<JunqiPosition>() }

        fun link(first: JunqiPosition, second: JunqiPosition) {
            assertTrue(first in neighbors)
            assertTrue(second in neighbors)
            neighbors.getValue(first) += second
            neighbors.getValue(second) += first
        }
    }
}
