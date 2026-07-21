package com.buddygames.junqi

import java.lang.reflect.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JunqiAiTest {
    @Test
    fun publicAiEntryPointCannotAcceptTrueStateOrRawPieces() {
        val chooseMove = JunqiAi::class.java.methods.single { method -> method.name == "chooseMove" }

        assertEquals(
            listOf(JunqiObservation::class.java, JunqiKnowledge::class.java, JunqiAiLevel::class.java),
            chooseMove.parameterTypes.toList(),
        )
        assertTrue(
            JunqiAi::class.java.methods
                .filter { Modifier.isPublic(it.modifiers) }
                .flatMap { it.parameterTypes.toList() }
                .none { it == JunqiState::class.java || it == JunqiPiece::class.java },
        )
    }

    @Test
    fun identicalObservationsWithDifferentHiddenTruthChooseIdentically() {
        val commanderObservation = observationWithHiddenType(JunqiPieceType.COMMANDER)
        val engineerObservation = observationWithHiddenType(JunqiPieceType.ENGINEER)
        val commanderKnowledge = JunqiKnowledge.from(commanderObservation)
        val engineerKnowledge = JunqiKnowledge.from(engineerObservation)

        val first = JunqiAi.chooseMove(commanderObservation, commanderKnowledge, JunqiAiLevel.LEVEL_1)
        val second = JunqiAi.chooseMove(engineerObservation, engineerKnowledge, JunqiAiLevel.LEVEL_1)

        assertEquals(commanderObservation, engineerObservation)
        assertEquals(commanderKnowledge, engineerKnowledge)
        assertEquals(first, second)
    }

    @Test
    fun realRandomDeploymentsWithDifferentHiddenTruthChooseIdentically() {
        val red = JunqiDeployment.default(JunqiSide.RED)
        val firstBlue = JunqiDeployment.random(JunqiSide.BLUE, 42)
        val secondBlue = JunqiDeployment.random(JunqiSide.BLUE, 43)
        val firstTruth = firstBlue.associate { piece -> piece.position to piece.type }
        val secondTruth = secondBlue.associate { piece -> piece.position to piece.type }
        val firstObservation = JunqiObservation.from(
            JunqiState((red + firstBlue).associateBy { it.position }),
            JunqiSide.RED,
        )
        val secondObservation = JunqiObservation.from(
            JunqiState((red + secondBlue).associateBy { it.position }),
            JunqiSide.RED,
        )
        val firstKnowledge = JunqiKnowledge.from(firstObservation)
        val secondKnowledge = JunqiKnowledge.from(secondObservation)

        assertNotEquals(firstTruth, secondTruth)
        assertEquals(firstObservation, secondObservation)
        assertEquals(firstObservation.deterministicHash(), secondObservation.deterministicHash())
        assertEquals(firstKnowledge, secondKnowledge)
        assertEquals(
            JunqiAi.chooseMove(firstObservation, firstKnowledge, JunqiAiLevel.LEVEL_1),
            JunqiAi.chooseMove(secondObservation, secondKnowledge, JunqiAiLevel.LEVEL_1),
        )
    }

    @Test
    fun levelsMatchTheApprovedExactMonotonicTableAndPlayerScoreMapping() {
        val expected = listOf(
            listOf(1, 1, 100, 80),
            listOf(2, 1, 250, 100),
            listOf(3, 2, 700, 150),
            listOf(4, 2, 1_500, 220),
            listOf(6, 2, 3_000, 320),
            listOf(8, 3, 7_000, 480),
            listOf(10, 3, 14_000, 650),
            listOf(14, 3, 28_000, 850),
            listOf(18, 4, 55_000, 1_100),
            listOf(24, 4, 100_000, 1_400),
        )

        assertEquals(10, JunqiAiLevel.entries.size)
        assertEquals(
            expected,
            JunqiAiLevel.entries.map { level ->
                listOf(level.sampleCount, level.searchDepth, level.nodeBudget, level.timeBudgetMillis)
            },
        )
        for (index in 1 until JunqiAiLevel.entries.size) {
            val previous = JunqiAiLevel.entries[index - 1]
            val current = JunqiAiLevel.entries[index]
            assertTrue(current.sampleCount >= previous.sampleCount)
            assertTrue(current.searchDepth >= previous.searchDepth)
            assertTrue(current.nodeBudget >= previous.nodeBudget)
            assertTrue(current.timeBudgetMillis >= previous.timeBudgetMillis)
        }
        assertEquals(JunqiAiLevel.LEVEL_1, JunqiAiLevel.forPlayerScore(-4))
        assertEquals(JunqiAiLevel.LEVEL_1, JunqiAiLevel.forPlayerScore(0))
        assertEquals(JunqiAiLevel.LEVEL_6, JunqiAiLevel.forPlayerScore(5))
        assertEquals(JunqiAiLevel.LEVEL_10, JunqiAiLevel.forPlayerScore(99))
    }

    @Test
    fun immediateRevealedFlagCaptureHasFirstPriority() {
        val state = stateOf(
            piece("red-engineer", JunqiSide.RED, JunqiPieceType.ENGINEER, 5, 2),
            piece("red-flag", JunqiSide.RED, JunqiPieceType.FLAG, 11, 1),
            piece("blue-flag", JunqiSide.BLUE, JunqiPieceType.FLAG, 6, 2),
            piece("blue-mover", JunqiSide.BLUE, JunqiPieceType.COMPANY, 0, 0),
            revealedFlags = setOf(JunqiSide.BLUE),
        )
        val observation = JunqiObservation.from(state, JunqiSide.RED)

        assertEquals(
            JunqiMove(at(5, 2), at(6, 2)),
            JunqiAi.chooseMove(observation, JunqiKnowledge.from(observation), JunqiAiLevel.LEVEL_10),
        )
    }

    @Test
    fun exposedFlagDefenseCapturesAnImmediateThreatBeforeOtherMoves() {
        val state = stateOf(
            piece("red-defender", JunqiSide.RED, JunqiPieceType.COMMANDER, 10, 2),
            piece("red-other", JunqiSide.RED, JunqiPieceType.COMPANY, 6, 4),
            piece("red-flag", JunqiSide.RED, JunqiPieceType.FLAG, 11, 1),
            piece("blue-threat", JunqiSide.BLUE, JunqiPieceType.ENGINEER, 10, 1, hasMoved = true),
            piece("blue-flag", JunqiSide.BLUE, JunqiPieceType.FLAG, 0, 1),
            revealedFlags = setOf(JunqiSide.RED, JunqiSide.BLUE),
        )
        val observation = JunqiObservation.from(state, JunqiSide.RED)

        assertEquals(
            JunqiMove(at(10, 2), at(10, 1)),
            JunqiAi.chooseMove(observation, JunqiKnowledge.from(observation), JunqiAiLevel.LEVEL_10),
        )
    }

    @Test
    fun exposedFlagDefenseRejectsAWeakSacrificeWhenAnEffectiveDefenseExists() {
        val state = stateOf(
            piece("red-weak", JunqiSide.RED, JunqiPieceType.ENGINEER, 10, 0),
            piece("red-bomb", JunqiSide.RED, JunqiPieceType.BOMB, 10, 2),
            piece("red-other", JunqiSide.RED, JunqiPieceType.COMPANY, 6, 4),
            piece("red-flag", JunqiSide.RED, JunqiPieceType.FLAG, 11, 1),
            piece("blue-threat", JunqiSide.BLUE, JunqiPieceType.COMMANDER, 10, 1, hasMoved = true),
            piece("blue-flag", JunqiSide.BLUE, JunqiPieceType.FLAG, 0, 1),
            revealedFlags = setOf(JunqiSide.RED, JunqiSide.BLUE),
        )
        val observation = JunqiObservation.from(state, JunqiSide.RED)
        val knowledge = JunqiKnowledge.from(observation).update(
            JunqiKnowledgeEvent.Battle(
                enemyPieceId = "blue-threat",
                ownPieceType = JunqiPieceType.ARMY_COMMANDER,
                enemyWasAttacker = false,
                outcome = JunqiBattleOutcome.DEFENDER_WINS,
            ),
        )

        assertEquals(setOf(JunqiPieceType.COMMANDER), knowledge.candidatesFor("blue-threat"))
        assertEquals(
            JunqiMove(at(10, 2), at(10, 1)),
            JunqiAi.chooseMove(observation, knowledge, JunqiAiLevel.LEVEL_10),
        )
    }

    @Test
    fun engineerAttacksAMineConfirmedByPublicBattleEvidence() {
        val state = stateOf(
            piece("red-engineer", JunqiSide.RED, JunqiPieceType.ENGINEER, 1, 1),
            piece("red-other", JunqiSide.RED, JunqiPieceType.COMPANY, 6, 4),
            piece("red-flag", JunqiSide.RED, JunqiPieceType.FLAG, 11, 1),
            piece("blue-mine", JunqiSide.BLUE, JunqiPieceType.MINE, 1, 0),
            piece("blue-flag", JunqiSide.BLUE, JunqiPieceType.FLAG, 0, 1),
        )
        val observation = JunqiObservation.from(state, JunqiSide.RED)
        val knowledge = JunqiKnowledge.from(observation).update(
            JunqiKnowledgeEvent.Battle(
                enemyPieceId = "blue-mine",
                ownPieceType = JunqiPieceType.COMMANDER,
                enemyWasAttacker = false,
                outcome = JunqiBattleOutcome.DEFENDER_WINS,
            ),
        )

        assertEquals(setOf(JunqiPieceType.MINE), knowledge.candidatesFor("blue-mine"))
        assertEquals(
            JunqiMove(at(1, 1), at(1, 0)),
            JunqiAi.chooseMove(observation, knowledge, JunqiAiLevel.LEVEL_10),
        )
    }

    @Test
    fun bombPrefersExchangeWithAnEnemyConstrainedToHighRanks() {
        val state = stateOf(
            piece("red-bomb", JunqiSide.RED, JunqiPieceType.BOMB, 5, 2),
            piece("red-other", JunqiSide.RED, JunqiPieceType.COMPANY, 6, 4),
            piece("red-flag", JunqiSide.RED, JunqiPieceType.FLAG, 11, 1),
            piece("blue-large", JunqiSide.BLUE, JunqiPieceType.COMMANDER, 6, 2, hasMoved = true),
            piece("blue-flag", JunqiSide.BLUE, JunqiPieceType.FLAG, 0, 1),
        )
        val observation = JunqiObservation.from(state, JunqiSide.RED)
        val knowledge = JunqiKnowledge.from(observation).update(
            JunqiKnowledgeEvent.Battle(
                enemyPieceId = "blue-large",
                ownPieceType = JunqiPieceType.REGIMENT,
                enemyWasAttacker = false,
                outcome = JunqiBattleOutcome.DEFENDER_WINS,
            ),
        )

        assertTrue(knowledge.candidatesFor("blue-large").all { it in highRanks })
        assertEquals(
            JunqiMove(at(5, 2), at(6, 2)),
            JunqiAi.chooseMove(observation, knowledge, JunqiAiLevel.LEVEL_10),
        )
    }

    @Test
    fun bombExchangeEstimateRespectsGloballyConsumedHighRankCapacity() {
        val highOnlyPieces = listOf(
            piece("blue-high-1", JunqiSide.BLUE, JunqiPieceType.COMMANDER, 1, 0, hasMoved = true),
            piece("blue-high-2", JunqiSide.BLUE, JunqiPieceType.ARMY_COMMANDER, 1, 1, hasMoved = true),
            piece("blue-high-3", JunqiSide.BLUE, JunqiPieceType.DIVISION_COMMANDER, 1, 2, hasMoved = true),
            piece("blue-high-4", JunqiSide.BLUE, JunqiPieceType.DIVISION_COMMANDER, 1, 3, hasMoved = true),
            piece("blue-high-5", JunqiSide.BLUE, JunqiPieceType.BRIGADE_COMMANDER, 1, 4, hasMoved = true),
            piece("blue-high-6", JunqiSide.BLUE, JunqiPieceType.BRIGADE_COMMANDER, 2, 0, hasMoved = true),
        )
        val state = stateOf(
            piece("red-bomb", JunqiSide.RED, JunqiPieceType.BOMB, 5, 2),
            piece("red-other", JunqiSide.RED, JunqiPieceType.COMPANY, 6, 4),
            piece("red-flag", JunqiSide.RED, JunqiPieceType.FLAG, 11, 1),
            piece("blue-target", JunqiSide.BLUE, JunqiPieceType.REGIMENT, 6, 2, hasMoved = true),
            piece("blue-flag", JunqiSide.BLUE, JunqiPieceType.FLAG, 0, 1),
            *highOnlyPieces.toTypedArray(),
        )
        val observation = JunqiObservation.from(state, JunqiSide.RED)
        var knowledge = JunqiKnowledge.from(observation)
        for (piece in highOnlyPieces) {
            knowledge = knowledge.update(
                JunqiKnowledgeEvent.Battle(
                    enemyPieceId = piece.id,
                    ownPieceType = JunqiPieceType.REGIMENT,
                    enemyWasAttacker = false,
                    outcome = JunqiBattleOutcome.DEFENDER_WINS,
                ),
            )
        }
        knowledge = knowledge.update(
            JunqiKnowledgeEvent.Battle(
                enemyPieceId = "blue-target",
                ownPieceType = JunqiPieceType.BATTALION,
                enemyWasAttacker = false,
                outcome = JunqiBattleOutcome.DEFENDER_WINS,
            ),
        )
        val bombMove = JunqiMove(at(5, 2), at(6, 2))

        repeat(8) { sampleIndex ->
            assertEquals(
                JunqiPieceType.REGIMENT,
                knowledge.sampleTypes(observation, sampleIndex.toLong()).getValue("blue-target"),
            )
        }
        assertEquals(
            0,
            JunqiTactics.rankedMoves(observation, knowledge)
                .single { tactical -> tactical.move == bombMove }
                .priority,
        )
    }

    @Test
    fun deterministicWeakeningIsLimitedToLevelsOneThroughFive() {
        val seed = 0x1234_5678L
        val candidateCount = 8

        for (level in JunqiAiLevel.entries.take(5)) {
            val first = JunqiAi.weakenedIndex(level, candidateCount, seed)
            val second = JunqiAi.weakenedIndex(level, candidateCount, seed)
            assertEquals(first, second)
            assertTrue(first in 0 until candidateCount)
        }
        for (level in JunqiAiLevel.entries.drop(5)) {
            assertEquals(0, JunqiAi.weakenedIndex(level, candidateCount, seed))
        }
        assertTrue(JunqiAiLevel.entries.take(5).any { JunqiAi.weakenedIndex(it, candidateCount, seed) > 0 })
    }

    @Test
    fun sampledSearchStopsAtTheNodeBudget() {
        val observation = fullDeploymentObservation()
        val level = JunqiAiLevel.LEVEL_1

        val result = JunqiSearchEngine().search(observation, JunqiKnowledge.from(observation), level)

        assertNotNull(result.rankedMoves.firstOrNull())
        assertTrue(result.nodes <= level.nodeBudget)
        assertTrue(result.samplesCompleted <= level.sampleCount)
    }

    @Test
    fun sampledSearchStartsTheDeadlineBeforeSamplingPreprocessing() {
        val readings = longArrayOf(0L, 0L, 81_000_000L)
        var index = 0
        val clock = {
            readings[minOf(index++, readings.lastIndex)]
        }
        val observation = fullDeploymentObservation()

        val result = JunqiSearchEngine(clock).search(
            observation,
            JunqiKnowledge.from(observation),
            JunqiAiLevel.LEVEL_1,
        )

        assertTrue(result.budgetExhausted)
        assertEquals(0, result.nodes)
        assertEquals(0, result.samplesCompleted)
        assertTrue(result.rankedMoves.isEmpty())
    }

    @Test
    fun sampledSearchChecksTheDeadlineAfterRootApplyBeforeEnteringTheTree() {
        var calls = 0
        val clock = {
            calls += 1
            if (calls >= 11) 81_000_000L else 0L
        }
        val observation = fullDeploymentObservation()

        val result = JunqiSearchEngine(clock).search(
            observation,
            JunqiKnowledge.from(observation),
            JunqiAiLevel.LEVEL_1,
        )

        assertTrue(result.budgetExhausted)
        assertEquals(0, result.nodes)
        assertEquals(0, result.samplesCompleted)
        assertNotNull(result.rankedMoves.firstOrNull())
    }

    private fun observationWithHiddenType(type: JunqiPieceType): JunqiObservation = JunqiObservation.from(
        stateOf(
            piece("red-mover", JunqiSide.RED, JunqiPieceType.COMPANY, 6, 2),
            piece("red-flag", JunqiSide.RED, JunqiPieceType.FLAG, 11, 1),
            piece("blue-hidden", JunqiSide.BLUE, type, 5, 2),
            piece("blue-flag", JunqiSide.BLUE, JunqiPieceType.FLAG, 0, 1),
        ),
        JunqiSide.RED,
    )

    private fun fullDeploymentObservation(): JunqiObservation {
        val pieces = JunqiDeployment.default(JunqiSide.RED) + JunqiDeployment.default(JunqiSide.BLUE)
        return JunqiObservation.from(JunqiState(pieces.associateBy { it.position }), JunqiSide.RED)
    }

    private fun stateOf(
        vararg pieces: JunqiPiece,
        revealedFlags: Set<JunqiSide> = emptySet(),
    ) = JunqiState(pieces.associateBy { it.position }, revealedFlags = revealedFlags)

    private fun piece(
        id: String,
        side: JunqiSide,
        type: JunqiPieceType,
        row: Int,
        column: Int,
        hasMoved: Boolean = false,
    ) = JunqiPiece(id, side, type, at(row, column), hasMoved)

    private fun at(row: Int, column: Int) = JunqiPosition(row, column)

    private companion object {
        val highRanks = setOf(
            JunqiPieceType.COMMANDER,
            JunqiPieceType.ARMY_COMMANDER,
            JunqiPieceType.DIVISION_COMMANDER,
            JunqiPieceType.BRIGADE_COMMANDER,
        )
    }
}
