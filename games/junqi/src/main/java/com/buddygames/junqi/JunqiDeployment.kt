package com.buddygames.junqi

import java.util.Random

object JunqiDeployment {
    fun default(side: JunqiSide): List<JunqiPiece> = build(side, random = null)

    fun random(side: JunqiSide, seed: Long): List<JunqiPiece> = build(side, Random(seed))

    fun isLegal(deployment: List<JunqiPiece>, side: JunqiSide): Boolean {
        if (deployment.size != PIECE_COUNT) return false
        if (deployment.any { it.side != side }) return false
        if (deployment.map { it.id }.toSet().size != PIECE_COUNT) return false
        if (deployment.map { it.position }.toSet() != deploymentPositions(side).toSet()) return false
        if (deployment.groupingBy { it.type }.eachCount() != inventoryCounts) return false

        val flag = deployment.single { it.type == JunqiPieceType.FLAG }
        if (flag.position !in headquarters(side)) return false
        if (deployment.filter { it.type == JunqiPieceType.MINE }.any { it.position.row !in backRows(side) }) return false
        if (deployment.filter { it.type == JunqiPieceType.BOMB }.any { it.position.row == frontRow(side) }) return false
        return true
    }

    fun swapIfLegal(
        deployment: List<JunqiPiece>,
        first: JunqiPosition,
        second: JunqiPosition,
    ): List<JunqiPiece> {
        val side = deployment.firstOrNull()?.side ?: return deployment
        if (deployment.none { it.position == first } || deployment.none { it.position == second }) return deployment

        val swapped = deployment.map { piece ->
            when (piece.position) {
                first -> piece.copy(position = second)
                second -> piece.copy(position = first)
                else -> piece
            }
        }
        return if (isLegal(swapped, side)) swapped else deployment
    }

    private fun build(side: JunqiSide, random: Random?): List<JunqiPiece> {
        val remainingPositions = deploymentPositions(side).toMutableList()
        val placements = mutableListOf<Pair<JunqiPieceType, JunqiPosition>>()

        fun place(type: JunqiPieceType, count: Int, allowed: (JunqiPosition) -> Boolean) {
            val candidates = remainingPositions.filter(allowed).toMutableList()
            if (random != null) candidates.shuffle(random)
            repeat(count) {
                val position = candidates[it]
                placements += type to position
                remainingPositions.remove(position)
            }
        }

        place(JunqiPieceType.FLAG, 1) { it in headquarters(side) }
        place(JunqiPieceType.MINE, 3) { it.row in backRows(side) }
        place(JunqiPieceType.BOMB, 2) { it.row != frontRow(side) }

        val remainingTypes = inventoryCounts.flatMap { (type, count) ->
            if (type in constrainedTypes) emptyList() else List(count) { type }
        }.toMutableList()
        if (random != null) {
            remainingTypes.shuffle(random)
            remainingPositions.shuffle(random)
        }
        placements += remainingTypes.zip(remainingPositions)

        val typeOccurrences = mutableMapOf<JunqiPieceType, Int>()
        return placements.sortedWith(compareBy({ it.second.row }, { it.second.column })).map { (type, position) ->
            val occurrence = typeOccurrences.getOrDefault(type, 0) + 1
            typeOccurrences[type] = occurrence
            JunqiPiece(
                id = "${side.name.lowercase()}-${type.name.lowercase()}-$occurrence",
                side = side,
                type = type,
                position = position,
            )
        }
    }

    private fun deploymentPositions(side: JunqiSide): List<JunqiPosition> {
        val rows = if (side == JunqiSide.RED) 6..11 else 0..5
        return rows.flatMap { row -> (0..4).map { column -> JunqiPosition(row, column) } }
            .filterNot { it in JunqiBoard.camps }
    }

    private fun headquarters(side: JunqiSide): Set<JunqiPosition> =
        JunqiBoard.headquarters.filterTo(mutableSetOf()) { position ->
            position.row == if (side == JunqiSide.RED) 11 else 0
        }

    private fun backRows(side: JunqiSide): IntRange = if (side == JunqiSide.RED) 10..11 else 0..1

    private fun frontRow(side: JunqiSide): Int = if (side == JunqiSide.RED) 6 else 5

    private const val PIECE_COUNT = 25

    private val constrainedTypes = setOf(JunqiPieceType.FLAG, JunqiPieceType.MINE, JunqiPieceType.BOMB)

    private val inventoryCounts = linkedMapOf(
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
