package com.buddygames.junqi

import java.util.Collections

object JunqiBoard {
    val headquarters: Set<JunqiPosition> = setOf(
        JunqiPosition(0, 1),
        JunqiPosition(0, 3),
        JunqiPosition(11, 1),
        JunqiPosition(11, 3),
    )

    val camps: Set<JunqiPosition> = setOf(
        JunqiPosition(2, 1), JunqiPosition(2, 3), JunqiPosition(3, 2), JunqiPosition(4, 1), JunqiPosition(4, 3),
        JunqiPosition(7, 1), JunqiPosition(7, 3), JunqiPosition(8, 2), JunqiPosition(9, 1), JunqiPosition(9, 3),
    )

    val roadNeighbors: Map<JunqiPosition, Set<JunqiPosition>> = buildGraph {
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
        for (camp in camps) {
            for (rowOffset in setOf(-1, 1)) {
                for (columnOffset in setOf(-1, 1)) {
                    link(camp, JunqiPosition(camp.row + rowOffset, camp.column + columnOffset))
                }
            }
        }
    }

    val railNeighbors: Map<JunqiPosition, Set<JunqiPosition>> = buildGraph {
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

    private fun buildGraph(build: GraphBuilder.() -> Unit): Map<JunqiPosition, Set<JunqiPosition>> {
        val builder = GraphBuilder()
        builder.build()
        return Collections.unmodifiableMap(
            builder.neighbors.mapValues { (_, neighbors) -> Collections.unmodifiableSet(neighbors.toSet()) },
        )
    }

    private class GraphBuilder {
        val neighbors = (0..11).flatMap { row ->
            (0..4).map { column -> JunqiPosition(row, column) }
        }.associateWith { mutableSetOf<JunqiPosition>() }

        fun link(first: JunqiPosition, second: JunqiPosition) {
            neighbors.getValue(first) += second
            neighbors.getValue(second) += first
        }
    }
}
