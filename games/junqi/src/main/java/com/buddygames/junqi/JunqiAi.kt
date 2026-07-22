package com.buddygames.junqi

internal typealias JunqiTypeSampler = (
    JunqiKnowledge,
    JunqiObservation,
    Long,
) -> Map<String, JunqiPieceType>

internal val defaultJunqiTypeSampler: JunqiTypeSampler = { knowledge, observation, seed ->
    knowledge.sampleTypes(observation, seed)
}

enum class JunqiAiLevel(
    val level: Int,
    val sampleCount: Int,
    val searchDepth: Int,
    val nodeBudget: Int,
    val timeBudgetMillis: Int,
) {
    LEVEL_1(1, 1, 1, 100, 80),
    LEVEL_2(2, 2, 1, 250, 100),
    LEVEL_3(3, 3, 2, 700, 150),
    LEVEL_4(4, 4, 2, 1_500, 220),
    LEVEL_5(5, 6, 2, 3_000, 320),
    LEVEL_6(6, 8, 3, 7_000, 480),
    LEVEL_7(7, 10, 3, 14_000, 650),
    LEVEL_8(8, 14, 3, 28_000, 850),
    LEVEL_9(9, 18, 4, 55_000, 1_100),
    LEVEL_10(10, 24, 4, 100_000, 1_400),
    ;

    companion object {
        fun forPlayerScore(score: Int): JunqiAiLevel = entries[score.coerceIn(0, 9)]
    }
}

object JunqiAi {
    fun chooseMove(
        observation: JunqiObservation,
        knowledge: JunqiKnowledge,
        level: JunqiAiLevel,
    ): JunqiMove? {
        require(observation.currentSide == observation.viewer) {
            "Junqi AI can choose only for the observation viewer's turn"
        }
        if (observation.result != null) return null

        val rankedMoves = JunqiSearchEngine().search(observation, knowledge, level).rankedMoves.map { it.move }
        if (rankedMoves.isEmpty()) return null

        val seed = observation.deterministicHash() xor AI_PACKAGE_SALT
        return rankedMoves[weakenedIndex(level, rankedMoves.size, seed)]
    }

    internal fun weakenedIndex(level: JunqiAiLevel, candidateCount: Int, seed: Long): Int {
        require(candidateCount > 0) { "Junqi AI needs at least one candidate move" }
        if (level.level >= 6) return 0
        val window = minOf(candidateCount, 7 - level.level)
        if (window <= 1) return 0
        return (((seed and Long.MAX_VALUE) + level.level) % window).toInt()
    }
}

internal object JunqiTactics {
    fun rankedMoves(
        observation: JunqiObservation,
        knowledge: JunqiKnowledge,
        determinizations: List<JunqiDeterminization>,
        checkDeadline: () -> Unit = {},
    ): List<JunqiTacticalMove> {
        require(determinizations.isNotEmpty()) { "Junqi tactics need at least one determinization" }
        val sampledAssignments = determinizations.map { it.opponentTypes }
        val sampledStates = determinizations.map { it.state }
        val rootState = sampledStates.first()
        checkDeadline()
        val legalMoves = JunqiRules.legalMoves(rootState, observation.viewer).sortedWith(moveComparator)
        checkDeadline()
        val ownByPosition = observation.ownPieces.associateBy { it.position }
        val opponentByPosition = observation.opponentPieces.associateBy { it.position }
        val effectiveFlagDefenses = effectiveFlagDefenses(
            observation,
            sampledStates,
            legalMoves,
            checkDeadline,
        )

        return legalMoves.map { move ->
            val ownPiece = ownByPosition.getValue(move.from)
            val opponent = opponentByPosition[move.to]
            val priority = when {
                opponent?.constraints?.revealedFlag == true -> FLAG_CAPTURE_PRIORITY
                move in effectiveFlagDefenses -> FLAG_DEFENSE_PRIORITY
                ownPiece.type == JunqiPieceType.ENGINEER &&
                    opponent != null &&
                    knowledge.candidatesFor(opponent.id) == setOf(JunqiPieceType.MINE) -> MINE_CLEAR_PRIORITY
                ownPiece.type == JunqiPieceType.BOMB &&
                    opponent != null &&
                    highRankSampleShare(opponent.id, sampledAssignments) >=
                    BOMB_EXCHANGE_SAMPLE_SHARE_THRESHOLD -> BOMB_EXCHANGE_PRIORITY
                else -> 0
            }
            JunqiTacticalMove(move, priority)
        }.sortedWith(
            compareByDescending<JunqiTacticalMove> { it.priority }
                .thenBy { it.move.from.row }
                .thenBy { it.move.from.column }
                .thenBy { it.move.to.row }
                .thenBy { it.move.to.column },
        )
    }

    private fun effectiveFlagDefenses(
        observation: JunqiObservation,
        sampledStates: List<JunqiState>,
        legalMoves: List<JunqiMove>,
        checkDeadline: () -> Unit,
    ): Set<JunqiMove> {
        if (observation.viewer !in observation.revealedFlags) return emptySet()
        val flagPosition = observation.ownPieces.singleOrNull { it.type == JunqiPieceType.FLAG }?.position
            ?: return emptySet()
        if (sampledStates.none { state ->
                hasImmediateFlagThreat(state, observation.viewer, flagPosition, checkDeadline)
            }
        ) {
            return emptySet()
        }
        return legalMoves.filterTo(linkedSetOf()) { move ->
            sampledStates.all { state ->
                checkDeadline()
                val resultingState = JunqiRules.applyMove(state, move)
                checkDeadline()
                resultingState.result?.winner == observation.viewer ||
                    resultingState.pieces.values
                        .singleOrNull { piece ->
                            piece.side == observation.viewer && piece.type == JunqiPieceType.FLAG
                        }
                        ?.let { flag ->
                            !hasImmediateFlagThreat(
                                resultingState,
                                observation.viewer,
                                flag.position,
                                checkDeadline,
                            )
                        } == true
            }
        }
    }

    private fun hasImmediateFlagThreat(
        state: JunqiState,
        defendingSide: JunqiSide,
        flagPosition: JunqiPosition,
        checkDeadline: () -> Unit,
    ): Boolean {
        checkDeadline()
        val threatened = JunqiRules.legalMoves(state, other(defendingSide)).any { move -> move.to == flagPosition }
        checkDeadline()
        return threatened
    }

    private fun highRankSampleShare(
        pieceId: String,
        sampledAssignments: List<Map<String, JunqiPieceType>>,
    ): Double = sampledAssignments.count { assignment -> assignment.getValue(pieceId) in highRanks }.toDouble() /
        sampledAssignments.size

    private val highRanks = setOf(
        JunqiPieceType.COMMANDER,
        JunqiPieceType.ARMY_COMMANDER,
        JunqiPieceType.DIVISION_COMMANDER,
        JunqiPieceType.BRIGADE_COMMANDER,
    )

    private const val FLAG_CAPTURE_PRIORITY = 4
    private const val FLAG_DEFENSE_PRIORITY = 3
    private const val MINE_CLEAR_PRIORITY = 2
    private const val BOMB_EXCHANGE_PRIORITY = 1
    private const val BOMB_EXCHANGE_SAMPLE_SHARE_THRESHOLD = 0.5
}

internal data class JunqiTacticalMove(val move: JunqiMove, val priority: Int)

internal val moveComparator = compareBy<JunqiMove>(
    { it.from.row },
    { it.from.column },
    { it.to.row },
    { it.to.column },
)

internal const val AI_PACKAGE_SALT = 0x4A_55_4E_51_49_00_00_08L

internal fun other(side: JunqiSide): JunqiSide =
    if (side == JunqiSide.RED) JunqiSide.BLUE else JunqiSide.RED
