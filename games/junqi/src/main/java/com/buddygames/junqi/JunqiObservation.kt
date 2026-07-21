package com.buddygames.junqi

import java.util.Collections
import java.util.Random

data class JunqiObservedOwnPiece(
    val id: String,
    val type: JunqiPieceType,
    val position: JunqiPosition,
    val hasMoved: Boolean,
)

data class JunqiOpponentConstraints(
    val mayBeFlag: Boolean,
    val mayBeMine: Boolean,
    val mayBeBomb: Boolean,
    val revealedFlag: Boolean,
)

data class JunqiObservedOpponentPiece(
    val id: String,
    val position: JunqiPosition,
    val hasMoved: Boolean,
    val constraints: JunqiOpponentConstraints,
)

class JunqiObservation private constructor(
    val viewer: JunqiSide,
    val currentSide: JunqiSide,
    ownPieces: List<JunqiObservedOwnPiece>,
    opponentPieces: List<JunqiObservedOpponentPiece>,
    revealedFlags: Set<JunqiSide>,
    val quietHalfMoves: Int,
    val result: JunqiResult?,
    val lastBattleOutcome: JunqiBattleOutcome?,
) {
    val ownPieces: List<JunqiObservedOwnPiece> = Collections.unmodifiableList(ownPieces.toList())
    val opponentPieces: List<JunqiObservedOpponentPiece> = Collections.unmodifiableList(opponentPieces.toList())
    val revealedFlags: Set<JunqiSide> = Collections.unmodifiableSet(revealedFlags.toSet())

    internal fun deterministicHash(): Long {
        var hash = OBSERVATION_HASH_OFFSET
        hash = mix(hash, viewer.ordinal)
        hash = mix(hash, currentSide.ordinal)
        hash = mix(hash, quietHalfMoves)
        hash = mix(hash, result?.ordinal ?: -1)
        hash = mix(hash, lastBattleOutcome?.ordinal ?: -1)
        for (side in revealedFlags.sortedBy { it.ordinal }) hash = mix(hash, side.ordinal)
        for (piece in ownPieces) {
            hash = mix(hash, piece.id.hashCode())
            hash = mix(hash, piece.type.ordinal)
            hash = mix(hash, piece.position.row * 5 + piece.position.column)
            hash = mix(hash, if (piece.hasMoved) 1 else 0)
        }
        for (piece in opponentPieces) {
            hash = mix(hash, piece.id.hashCode())
            hash = mix(hash, piece.position.row * 5 + piece.position.column)
            hash = mix(hash, if (piece.hasMoved) 1 else 0)
            hash = mix(hash, if (piece.constraints.mayBeFlag) 1 else 0)
            hash = mix(hash, if (piece.constraints.mayBeMine) 1 else 0)
            hash = mix(hash, if (piece.constraints.mayBeBomb) 1 else 0)
            hash = mix(hash, if (piece.constraints.revealedFlag) 1 else 0)
        }
        return hash
    }

    override fun equals(other: Any?): Boolean =
        other is JunqiObservation &&
            viewer == other.viewer &&
            currentSide == other.currentSide &&
            ownPieces == other.ownPieces &&
            opponentPieces == other.opponentPieces &&
            revealedFlags == other.revealedFlags &&
            quietHalfMoves == other.quietHalfMoves &&
            result == other.result &&
            lastBattleOutcome == other.lastBattleOutcome

    override fun hashCode(): Int {
        var resultHash = viewer.hashCode()
        resultHash = 31 * resultHash + currentSide.hashCode()
        resultHash = 31 * resultHash + ownPieces.hashCode()
        resultHash = 31 * resultHash + opponentPieces.hashCode()
        resultHash = 31 * resultHash + revealedFlags.hashCode()
        resultHash = 31 * resultHash + quietHalfMoves
        resultHash = 31 * resultHash + (result?.hashCode() ?: 0)
        resultHash = 31 * resultHash + (lastBattleOutcome?.hashCode() ?: 0)
        return resultHash
    }

    override fun toString(): String =
        "JunqiObservation(viewer=$viewer, currentSide=$currentSide, ownPieces=$ownPieces, " +
            "opponentPieces=$opponentPieces, revealedFlags=$revealedFlags, " +
            "quietHalfMoves=$quietHalfMoves, result=$result, lastBattleOutcome=$lastBattleOutcome)"

    companion object {
        fun from(state: JunqiState, viewer: JunqiSide): JunqiObservation {
            val ownPieces = state.pieces.values
                .filter { it.side == viewer }
                .sortedBy { it.id }
                .map { piece ->
                    JunqiObservedOwnPiece(piece.id, piece.type, piece.position, piece.hasMoved)
                }
            val opponentPieces = state.pieces.values
                .filter { it.side != viewer }
                .sortedBy { it.id }
                .map { piece ->
                    val flagRevealed = piece.side in state.revealedFlags && piece.type == JunqiPieceType.FLAG
                    JunqiObservedOpponentPiece(
                        id = piece.id,
                        position = piece.position,
                        hasMoved = piece.hasMoved,
                        constraints = JunqiOpponentConstraints(
                            mayBeFlag = flagRevealed || (!piece.hasMoved && piece.position in headquarters(piece.side)),
                            mayBeMine = !piece.hasMoved && piece.position.row in backRows(piece.side),
                            mayBeBomb = piece.hasMoved || piece.position.row != frontRow(piece.side),
                            revealedFlag = flagRevealed,
                        ),
                    )
                }
            return JunqiObservation(
                viewer = viewer,
                currentSide = state.currentSide,
                ownPieces = ownPieces,
                opponentPieces = opponentPieces,
                revealedFlags = state.revealedFlags,
                quietHalfMoves = state.quietHalfMoves,
                result = state.result,
                lastBattleOutcome = state.lastBattleOutcome,
            )
        }

        private fun headquarters(side: JunqiSide): Set<JunqiPosition> =
            JunqiBoard.headquarters.filterTo(mutableSetOf()) { position ->
                position.row == if (side == JunqiSide.RED) 11 else 0
            }

        private fun backRows(side: JunqiSide): IntRange = if (side == JunqiSide.RED) 10..11 else 0..1

        private fun frontRow(side: JunqiSide): Int = if (side == JunqiSide.RED) 6 else 5

        private fun mix(hash: Long, value: Int): Long = (hash xor value.toLong()) * OBSERVATION_HASH_PRIME

        private const val OBSERVATION_HASH_OFFSET = -3_750_763_034_362_897_557L
        private const val OBSERVATION_HASH_PRIME = 1_099_511_628_211L
    }
}

sealed interface JunqiKnowledgeEvent {
    data class PieceMoved(val pieceId: String) : JunqiKnowledgeEvent

    data class FlagRevealed(val pieceId: String) : JunqiKnowledgeEvent

    data class Battle(
        val enemyPieceId: String,
        val ownPieceType: JunqiPieceType,
        val enemyWasAttacker: Boolean,
        val outcome: JunqiBattleOutcome,
    ) : JunqiKnowledgeEvent
}

class JunqiKnowledge private constructor(
    candidates: Map<String, Set<JunqiPieceType>>,
    activePieceIds: Set<String>,
) {
    val candidatesByPieceId: Map<String, Set<JunqiPieceType>> = immutableCandidates(candidates)
    val activePieceIds: Set<String> = Collections.unmodifiableSet(activePieceIds.toSet())

    fun candidatesFor(pieceId: String): Set<JunqiPieceType> =
        candidatesByPieceId[pieceId] ?: emptySet()

    fun update(event: JunqiKnowledgeEvent): JunqiKnowledge {
        val updatedCandidates = candidatesByPieceId.mapValuesTo(linkedMapOf()) { (_, types) -> types.toMutableSet() }
        val updatedActiveIds = activePieceIds.toMutableSet()

        when (event) {
            is JunqiKnowledgeEvent.PieceMoved -> {
                require(event.pieceId in updatedActiveIds) { "Unknown active enemy piece: ${event.pieceId}" }
                updatedCandidates.getValue(event.pieceId).removeAll(nonMovableTypes)
            }
            is JunqiKnowledgeEvent.FlagRevealed -> {
                require(event.pieceId in updatedActiveIds) { "Unknown active enemy piece: ${event.pieceId}" }
                updatedCandidates.getValue(event.pieceId).retainAll(setOf(JunqiPieceType.FLAG))
            }
            is JunqiKnowledgeEvent.Battle -> {
                require(event.enemyPieceId in updatedActiveIds) {
                    "Unknown active enemy piece: ${event.enemyPieceId}"
                }
                val candidates = updatedCandidates.getValue(event.enemyPieceId)
                if (event.enemyWasAttacker) candidates.removeAll(nonMovableTypes)
                candidates.retainAll { enemyType -> event.matches(enemyType) }
                if (!event.enemySurvives()) updatedActiveIds.remove(event.enemyPieceId)
            }
        }
        return create(updatedCandidates, updatedActiveIds)
    }

    internal fun sampleTypes(
        observation: JunqiObservation,
        seed: Long,
    ): Map<String, JunqiPieceType> {
        val observedById = observation.opponentPieces.associateBy { it.id }
        require(observedById.keys == activePieceIds) {
            "Junqi knowledge active identities must match the current observation"
        }
        val activeCandidates = observedById.mapValuesTo(linkedMapOf()) { (id, observed) ->
            candidatesByPieceId.getValue(id).intersect(candidatesFrom(observed.constraints))
        }
        val assignment = findAssignment(activeCandidates, seed)
        check(assignment != null) { "Junqi knowledge candidates have no inventory-consistent assignment" }
        return Collections.unmodifiableMap(assignment)
    }

    override fun equals(other: Any?): Boolean =
        other is JunqiKnowledge &&
            candidatesByPieceId == other.candidatesByPieceId &&
            activePieceIds == other.activePieceIds

    override fun hashCode(): Int = 31 * candidatesByPieceId.hashCode() + activePieceIds.hashCode()

    override fun toString(): String =
        "JunqiKnowledge(candidatesByPieceId=$candidatesByPieceId, activePieceIds=$activePieceIds)"

    companion object {
        fun from(observation: JunqiObservation): JunqiKnowledge {
            val candidates = observation.opponentPieces.associateTo(linkedMapOf()) { piece ->
                piece.id to candidatesFrom(piece.constraints)
            }
            return create(candidates, candidates.keys)
        }

        private fun create(
            candidates: Map<String, Set<JunqiPieceType>>,
            activePieceIds: Set<String>,
        ): JunqiKnowledge {
            require(activePieceIds.all { it in candidates }) { "Every active enemy must have candidates" }
            val normalized = normalizeCandidates(candidates)
            require(findAssignment(normalized, seed = 0L) != null) {
                "Junqi knowledge exceeds the initial inventory counts"
            }
            return JunqiKnowledge(normalized, activePieceIds)
        }

        private fun candidatesFrom(constraints: JunqiOpponentConstraints): Set<JunqiPieceType> {
            if (constraints.revealedFlag) return setOf(JunqiPieceType.FLAG)
            return JunqiPieceType.entries.filterTo(linkedSetOf()) { type ->
                when (type) {
                    JunqiPieceType.FLAG -> constraints.mayBeFlag
                    JunqiPieceType.MINE -> constraints.mayBeMine
                    JunqiPieceType.BOMB -> constraints.mayBeBomb
                    else -> true
                }
            }
        }

        private fun normalizeCandidates(
            source: Map<String, Set<JunqiPieceType>>,
        ): Map<String, Set<JunqiPieceType>> {
            val normalized = source.mapValuesTo(linkedMapOf()) { (_, candidates) -> candidates.toMutableSet() }
            var changed: Boolean
            do {
                require(normalized.values.none { it.isEmpty() }) { "Every tracked enemy must have a candidate type" }
                val singletonCounts = normalized.values
                    .filter { it.size == 1 }
                    .map { it.single() }
                    .groupingBy { it }
                    .eachCount()
                for ((type, count) in singletonCounts) {
                    require(count <= JUNQI_INITIAL_INVENTORY.getValue(type)) {
                        "Too many enemies are fixed as $type"
                    }
                }

                changed = false
                for ((type, capacity) in JUNQI_INITIAL_INVENTORY) {
                    if (singletonCounts.getOrDefault(type, 0) != capacity) continue
                    for (candidates in normalized.values) {
                        if (candidates.size > 1 && candidates.remove(type)) changed = true
                    }
                }
            } while (changed)
            require(normalized.values.none { it.isEmpty() }) { "Every tracked enemy must have a candidate type" }
            return normalized
        }

        private fun immutableCandidates(
            source: Map<String, Set<JunqiPieceType>>,
        ): Map<String, Set<JunqiPieceType>> = Collections.unmodifiableMap(
            source.entries.associateTo(linkedMapOf()) { (id, candidates) ->
                id to Collections.unmodifiableSet(candidates.toSet())
            },
        )

        private fun findAssignment(
            candidatesById: Map<String, Set<JunqiPieceType>>,
            seed: Long,
        ): LinkedHashMap<String, JunqiPieceType>? {
            val random = Random(seed)
            val idTieBreakers = candidatesById.keys.associateWith { random.nextLong() }
            val typeTieBreakers = candidatesById.flatMap { (id, candidates) ->
                candidates.map { type -> (id to type) to random.nextLong() }
            }.toMap()
            val remaining = JUNQI_INITIAL_INVENTORY.toMutableMap()
            val assigned = linkedMapOf<String, JunqiPieceType>()

            fun assign(): Boolean {
                if (assigned.size == candidatesById.size) return true
                val nextId = candidatesById.keys
                    .asSequence()
                    .filterNot { it in assigned }
                    .map { id ->
                        id to candidatesById.getValue(id).filter { remaining.getValue(it) > 0 }
                    }
                    .minWithOrNull(
                        compareBy<Pair<String, List<JunqiPieceType>>> { it.second.size }
                            .thenBy { idTieBreakers.getValue(it.first) }
                            .thenBy { it.first },
                    )
                    ?: return true
                if (nextId.second.isEmpty()) return false

                val orderedTypes = nextId.second.sortedWith(
                    compareBy<JunqiPieceType> { typeTieBreakers.getValue(nextId.first to it) }
                        .thenBy { it.ordinal },
                )
                for (type in orderedTypes) {
                    assigned[nextId.first] = type
                    remaining[type] = remaining.getValue(type) - 1
                    if (assign()) return true
                    remaining[type] = remaining.getValue(type) + 1
                    assigned.remove(nextId.first)
                }
                return false
            }

            return if (assign()) assigned else null
        }

        private val nonMovableTypes = setOf(JunqiPieceType.MINE, JunqiPieceType.FLAG)
    }
}

internal val JUNQI_INITIAL_INVENTORY: Map<JunqiPieceType, Int> = Collections.unmodifiableMap(
    linkedMapOf(
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
    ),
)

private fun JunqiKnowledgeEvent.Battle.matches(enemyType: JunqiPieceType): Boolean {
    val attacker = if (enemyWasAttacker) enemyType else ownPieceType
    val defender = if (enemyWasAttacker) ownPieceType else enemyType
    if (!attacker.movable) return false
    return JunqiRules.battleOutcome(attacker, defender) == outcome
}

private fun JunqiKnowledgeEvent.Battle.enemySurvives(): Boolean =
    if (enemyWasAttacker) {
        outcome == JunqiBattleOutcome.ATTACKER_WINS
    } else {
        outcome == JunqiBattleOutcome.DEFENDER_WINS
    }
