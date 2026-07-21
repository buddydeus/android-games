package com.buddygames.junqi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class JunqiObservationTest {
    @Test
    fun observationShowsOwnTypesWithoutGivingOpponentRecordsATypeField() {
        val state = JunqiState(
            pieces = listOf(
                piece("red-commander", JunqiSide.RED, JunqiPieceType.COMMANDER, 6, 0),
                piece("blue-hidden", JunqiSide.BLUE, JunqiPieceType.ARMY_COMMANDER, 5, 0),
            ).associateBy { it.position },
        )

        val observation = JunqiObservation.from(state, JunqiSide.RED)

        assertEquals(JunqiPieceType.COMMANDER, observation.ownPieces.single().type)
        assertEquals("blue-hidden", observation.opponentPieces.single().id)
        assertEquals(at(5, 0), observation.opponentPieces.single().position)
        assertTrue(
            JunqiObservedOpponentPiece::class.java.declaredFields.none { field ->
                field.type == JunqiPieceType::class.java
            },
        )
    }

    @Test
    fun hiddenTypeChangesDoNotChangeTheObservationOrItsSeed() {
        val first = hiddenState(JunqiPieceType.COMMANDER)
        val second = hiddenState(JunqiPieceType.ENGINEER)

        val firstObservation = JunqiObservation.from(first, JunqiSide.RED)
        val secondObservation = JunqiObservation.from(second, JunqiSide.RED)

        assertNotEquals(first.pieces.getValue(at(5, 2)).type, second.pieces.getValue(at(5, 2)).type)
        assertEquals(firstObservation, secondObservation)
        assertEquals(firstObservation.deterministicHash(), secondObservation.deterministicHash())
    }

    @Test
    fun movedOpponentCannotRemainAFlagOrMineCandidate() {
        val state = JunqiState(
            pieces = mapOf(
                at(4, 0) to piece(
                    "blue-moved",
                    JunqiSide.BLUE,
                    JunqiPieceType.COMMANDER,
                    4,
                    0,
                    hasMoved = true,
                ),
            ),
        )

        val knowledge = JunqiKnowledge.from(JunqiObservation.from(state, JunqiSide.RED))

        assertFalse(JunqiPieceType.FLAG in knowledge.candidatesFor("blue-moved"))
        assertFalse(JunqiPieceType.MINE in knowledge.candidatesFor("blue-moved"))
    }

    @Test
    fun revealedOpponentFlagBecomesASingletonCandidate() {
        val state = JunqiState(
            pieces = mapOf(
                at(0, 1) to piece("blue-flag", JunqiSide.BLUE, JunqiPieceType.FLAG, 0, 1),
            ),
            revealedFlags = setOf(JunqiSide.BLUE),
        )

        val observation = JunqiObservation.from(state, JunqiSide.RED)
        val knowledge = JunqiKnowledge.from(observation)

        assertTrue(observation.opponentPieces.single().constraints.revealedFlag)
        assertEquals(setOf(JunqiPieceType.FLAG), knowledge.candidatesFor("blue-flag"))
    }

    @Test
    fun observationExposesOnlyThePublicOutcomeOfTheLastBattle() {
        val state = JunqiState(
            pieces = mapOf(
                at(3, 1) to piece("blue-survivor", JunqiSide.BLUE, JunqiPieceType.COMMANDER, 3, 1),
            ),
            currentSide = JunqiSide.BLUE,
            lastBattleOutcome = JunqiBattleOutcome.DEFENDER_WINS,
        )

        val observation = JunqiObservation.from(state, JunqiSide.RED)

        assertEquals(JunqiBattleOutcome.DEFENDER_WINS, observation.lastBattleOutcome)
        assertEquals("blue-survivor", observation.opponentPieces.single().id)
    }

    @Test
    fun knowledgeUpdatesReturnNewDeeplyImmutableCandidateMaps() {
        val state = JunqiState(
            pieces = mapOf(
                at(1, 0) to piece("blue-hidden", JunqiSide.BLUE, JunqiPieceType.COMMANDER, 1, 0),
            ),
        )
        val observation = JunqiObservation.from(state, JunqiSide.RED)
        val original = JunqiKnowledge.from(observation)

        val updated = original.update(JunqiKnowledgeEvent.PieceMoved("blue-hidden"))

        assertTrue(JunqiPieceType.MINE in original.candidatesFor("blue-hidden"))
        assertFalse(JunqiPieceType.MINE in updated.candidatesFor("blue-hidden"))
        assertThrows(UnsupportedOperationException::class.java) {
            @Suppress("UNCHECKED_CAST")
            (updated.candidatesByPieceId as MutableMap<String, Set<JunqiPieceType>>).clear()
        }
        assertThrows(UnsupportedOperationException::class.java) {
            @Suppress("UNCHECKED_CAST")
            (updated.candidatesFor("blue-hidden") as MutableSet<JunqiPieceType>).clear()
        }
    }

    @Test
    fun battleEvidenceNarrowsASurvivingEnemyWithoutReadingItsTrueType() {
        val state = JunqiState(
            pieces = mapOf(
                at(1, 0) to piece("blue-defender", JunqiSide.BLUE, JunqiPieceType.MINE, 1, 0),
            ),
        )
        val knowledge = JunqiKnowledge.from(JunqiObservation.from(state, JunqiSide.RED))

        val updated = knowledge.update(
            JunqiKnowledgeEvent.Battle(
                enemyPieceId = "blue-defender",
                ownPieceType = JunqiPieceType.COMMANDER,
                enemyWasAttacker = false,
                outcome = JunqiBattleOutcome.DEFENDER_WINS,
            ),
        )

        assertEquals(setOf(JunqiPieceType.MINE), updated.candidatesFor("blue-defender"))
        assertTrue("blue-defender" in updated.activePieceIds)
    }

    @Test
    fun deterministicSamplesRespectEveryInitialInventoryCount() {
        val red = JunqiDeployment.default(JunqiSide.RED)
        val blue = JunqiDeployment.default(JunqiSide.BLUE)
        val observation = JunqiObservation.from(
            JunqiState((red + blue).associateBy { it.position }),
            JunqiSide.RED,
        )
        val knowledge = JunqiKnowledge.from(observation)

        val first = knowledge.sampleTypes(observation, 2048L)
        val second = knowledge.sampleTypes(observation, 2048L)

        assertEquals(first, second)
        assertEquals(25, first.size)
        assertEquals(JUNQI_INITIAL_INVENTORY, first.values.groupingBy { it }.eachCount())
    }

    @Test
    fun postCaptureSamplesContainOnlyActiveIdentitiesAndPreserveActiveCommanderFacts() {
        val state = JunqiState(
            pieces = listOf(
                piece("blue-commander", JunqiSide.BLUE, JunqiPieceType.COMMANDER, 4, 0, hasMoved = true),
                piece("blue-captured", JunqiSide.BLUE, JunqiPieceType.COMPANY, 5, 0, hasMoved = true),
                piece("blue-flag", JunqiSide.BLUE, JunqiPieceType.FLAG, 0, 1),
            ).associateBy { it.position },
        )
        val knowledge = JunqiKnowledge.from(JunqiObservation.from(state, JunqiSide.RED))
            .update(
                JunqiKnowledgeEvent.Battle(
                    enemyPieceId = "blue-commander",
                    ownPieceType = JunqiPieceType.ARMY_COMMANDER,
                    enemyWasAttacker = false,
                    outcome = JunqiBattleOutcome.DEFENDER_WINS,
                ),
            )
            .update(
                JunqiKnowledgeEvent.Battle(
                    enemyPieceId = "blue-captured",
                    ownPieceType = JunqiPieceType.COMMANDER,
                    enemyWasAttacker = false,
                    outcome = JunqiBattleOutcome.ATTACKER_WINS,
                ),
            )

        val postCaptureState = JunqiState(
            pieces = state.pieces.values
                .filterNot { it.id == "blue-captured" }
                .associateBy { it.position },
        )
        val postCaptureObservation = JunqiObservation.from(postCaptureState, JunqiSide.RED)
        val sample = knowledge.sampleTypes(postCaptureObservation, 91L)

        assertEquals(setOf("blue-commander", "blue-flag"), knowledge.activePieceIds)
        assertEquals(knowledge.activePieceIds, sample.keys)
        assertEquals(JunqiPieceType.COMMANDER, sample.getValue("blue-commander"))
    }

    @Test
    fun postCaptureSamplingAppliesNewlyRevealedFlagFactsFromTheObservation() {
        val beforeCapture = JunqiState(
            pieces = listOf(
                piece("blue-captured", JunqiSide.BLUE, JunqiPieceType.COMMANDER, 5, 0, hasMoved = true),
                piece("blue-flag", JunqiSide.BLUE, JunqiPieceType.FLAG, 0, 1),
                piece("blue-mover", JunqiSide.BLUE, JunqiPieceType.COMPANY, 4, 0, hasMoved = true),
            ).associateBy { it.position },
        )
        val knowledgeAfterCapture = JunqiKnowledge.from(JunqiObservation.from(beforeCapture, JunqiSide.RED))
            .update(
                JunqiKnowledgeEvent.Battle(
                    enemyPieceId = "blue-captured",
                    ownPieceType = JunqiPieceType.BOMB,
                    enemyWasAttacker = false,
                    outcome = JunqiBattleOutcome.BOTH_REMOVED,
                ),
            )
        val afterCapture = JunqiState(
            pieces = beforeCapture.pieces.values
                .filterNot { it.id == "blue-captured" }
                .associateBy { it.position },
            revealedFlags = setOf(JunqiSide.BLUE),
        )
        val revealedObservation = JunqiObservation.from(afterCapture, JunqiSide.RED)

        val sample = knowledgeAfterCapture.sampleTypes(revealedObservation, 92L)

        assertEquals(setOf("blue-flag", "blue-mover"), sample.keys)
        assertEquals(JunqiPieceType.FLAG, sample.getValue("blue-flag"))
    }

    @Test
    fun nonterminalSamplesKeepExactlyOneLiveFlagAfterAnUnknownDefenderIsEliminated() {
        val red = JunqiDeployment.default(JunqiSide.RED)
        val blue = JunqiDeployment.default(JunqiSide.BLUE)
        val eliminated = blue.single { piece ->
            piece.position in JunqiBoard.headquarters && piece.type != JunqiPieceType.FLAG
        }
        val initialObservation = JunqiObservation.from(
            JunqiState((red + blue).associateBy { it.position }),
            JunqiSide.RED,
        )
        val knowledge = JunqiKnowledge.from(initialObservation).update(
            JunqiKnowledgeEvent.Battle(
                enemyPieceId = eliminated.id,
                ownPieceType = JunqiPieceType.COMMANDER,
                enemyWasAttacker = false,
                outcome = JunqiBattleOutcome.ATTACKER_WINS,
            ),
        )
        val observation = JunqiObservation.from(
            JunqiState((red + blue.filterNot { it.id == eliminated.id }).associateBy { it.position }),
            JunqiSide.RED,
        )

        repeat(32) { sampleIndex ->
            val sample = knowledge.sampleTypes(observation, sampleIndex.toLong())
            assertEquals(1, sample.values.count { it == JunqiPieceType.FLAG })
        }
    }

    @Test
    fun eliminatedKnownRankConsumesItsExactInventoryCapacity() {
        val red = JunqiDeployment.default(JunqiSide.RED)
        val blue = JunqiDeployment.default(JunqiSide.BLUE)
        val eliminated = blue.first { it.type == JunqiPieceType.DIVISION_COMMANDER }
        val initialObservation = JunqiObservation.from(
            JunqiState((red + blue).associateBy { it.position }),
            JunqiSide.RED,
        )
        val knowledge = JunqiKnowledge.from(initialObservation)
            .update(
                JunqiKnowledgeEvent.Battle(
                    enemyPieceId = eliminated.id,
                    ownPieceType = JunqiPieceType.BRIGADE_COMMANDER,
                    enemyWasAttacker = true,
                    outcome = JunqiBattleOutcome.ATTACKER_WINS,
                ),
            )
            .update(
                JunqiKnowledgeEvent.Battle(
                    enemyPieceId = eliminated.id,
                    ownPieceType = JunqiPieceType.ARMY_COMMANDER,
                    enemyWasAttacker = false,
                    outcome = JunqiBattleOutcome.ATTACKER_WINS,
                ),
            )
        val observation = JunqiObservation.from(
            JunqiState((red + blue.filterNot { it.id == eliminated.id }).associateBy { it.position }),
            JunqiSide.RED,
        )

        assertEquals(setOf(JunqiPieceType.DIVISION_COMMANDER), knowledge.candidatesFor(eliminated.id))
        repeat(32) { sampleIndex ->
            val sample = knowledge.sampleTypes(observation, sampleIndex.toLong())
            assertEquals(1, sample.values.count { it == JunqiPieceType.DIVISION_COMMANDER })
        }
    }

    @Test
    fun revealedFlagProvesCommanderIsAbsentFromEveryActiveSurvivor() {
        val red = JunqiDeployment.default(JunqiSide.RED)
        val blue = JunqiDeployment.default(JunqiSide.BLUE)
        val commander = blue.single { it.type == JunqiPieceType.COMMANDER }
        val flag = blue.single { it.type == JunqiPieceType.FLAG }
        val initialObservation = JunqiObservation.from(
            JunqiState((red + blue).associateBy { it.position }),
            JunqiSide.RED,
        )
        val knowledge = JunqiKnowledge.from(initialObservation)
            .update(
                JunqiKnowledgeEvent.Battle(
                    enemyPieceId = commander.id,
                    ownPieceType = JunqiPieceType.COMMANDER,
                    enemyWasAttacker = true,
                    outcome = JunqiBattleOutcome.BOTH_REMOVED,
                ),
            )
            .update(JunqiKnowledgeEvent.FlagRevealed(flag.id))
        val observation = JunqiObservation.from(
            JunqiState(
                pieces = (red + blue.filterNot { it.id == commander.id }).associateBy { it.position },
                revealedFlags = setOf(JunqiSide.BLUE),
            ),
            JunqiSide.RED,
        )

        assertTrue(
            knowledge.activePieceIds.all { pieceId ->
                JunqiPieceType.COMMANDER !in knowledge.candidatesFor(pieceId)
            },
        )
        repeat(32) { sampleIndex ->
            val sample = knowledge.sampleTypes(observation, sampleIndex.toLong())
            assertEquals(0, sample.values.count { it == JunqiPieceType.COMMANDER })
        }
    }

    @Test
    fun impossibleGlobalCandidateCountsAreRejected() {
        val state = JunqiState(
            pieces = listOf(
                piece("blue-one", JunqiSide.BLUE, JunqiPieceType.FLAG, 0, 1),
                piece("blue-two", JunqiSide.BLUE, JunqiPieceType.MINE, 0, 3),
            ).associateBy { it.position },
        )
        val original = JunqiKnowledge.from(JunqiObservation.from(state, JunqiSide.RED))
        val firstFlag = original.update(JunqiKnowledgeEvent.FlagRevealed("blue-one"))

        assertThrows(IllegalArgumentException::class.java) {
            firstFlag.update(JunqiKnowledgeEvent.FlagRevealed("blue-two"))
        }
    }

    @Test
    fun flagRevealCannotOverrideThePublicFactThatAPieceAlreadyMoved() {
        val state = JunqiState(
            pieces = mapOf(
                at(4, 0) to piece(
                    "blue-moved",
                    JunqiSide.BLUE,
                    JunqiPieceType.COMMANDER,
                    4,
                    0,
                    hasMoved = true,
                ),
            ),
        )
        val knowledge = JunqiKnowledge.from(JunqiObservation.from(state, JunqiSide.RED))

        assertThrows(IllegalArgumentException::class.java) {
            knowledge.update(JunqiKnowledgeEvent.FlagRevealed("blue-moved"))
        }
    }

    private fun hiddenState(hiddenType: JunqiPieceType): JunqiState = JunqiState(
        pieces = listOf(
            piece("red-mover", JunqiSide.RED, JunqiPieceType.COMPANY, 6, 2),
            piece("red-flag", JunqiSide.RED, JunqiPieceType.FLAG, 11, 1),
            piece("blue-hidden", JunqiSide.BLUE, hiddenType, 5, 2),
            piece("blue-flag", JunqiSide.BLUE, JunqiPieceType.FLAG, 0, 1),
        ).associateBy { it.position },
    )

    private fun piece(
        id: String,
        side: JunqiSide,
        type: JunqiPieceType,
        row: Int,
        column: Int,
        hasMoved: Boolean = false,
    ) = JunqiPiece(id, side, type, at(row, column), hasMoved)

    private fun at(row: Int, column: Int) = JunqiPosition(row, column)
}
