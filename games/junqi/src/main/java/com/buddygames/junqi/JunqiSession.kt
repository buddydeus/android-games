package com.buddygames.junqi

import java.util.Collections

enum class JunqiPhase {
    DEPLOYMENT,
    HANDOFF,
    PLAYING,
    BATTLE_RESULT,
    FINISHED,
}

enum class JunqiMode {
    SINGLE_PLAYER,
    TWO_PLAYERS,
}

data class JunqiScore(
    val red: Int = 0,
    val blue: Int = 0,
    val player: Int = 0,
    val robot: Int = 0,
) {
    val intelligenceLevel: Int
        get() = (player + 1).coerceAtMost(10)

    fun record(result: JunqiResult?, mode: JunqiMode, playerSide: JunqiSide): JunqiScore {
        val winner = result?.winner ?: return this
        return when {
            mode == JunqiMode.SINGLE_PLAYER && winner == playerSide -> copy(player = player + 1)
            mode == JunqiMode.SINGLE_PLAYER -> copy(robot = robot + 1)
            winner == JunqiSide.RED -> copy(red = red + 1)
            else -> copy(blue = blue + 1)
        }
    }
}

data class JunqiRobotRequest(
    val generation: Long,
    val observation: JunqiObservation,
    val knowledge: JunqiKnowledge,
    val level: JunqiAiLevel,
)

data class JunqiSessionState(
    val phase: JunqiPhase,
    val mode: JunqiMode,
    val playerSide: JunqiSide,
    val robotSide: JunqiSide?,
    val currentSide: JunqiSide,
    val deployment: List<JunqiPiece>,
    val selectedDeploymentPiece: JunqiPosition?,
    val observation: JunqiObservation?,
    val battleOutcome: JunqiBattleOutcome?,
    val result: JunqiResult?,
    val score: JunqiScore,
    val lastMove: JunqiMove?,
    val canUndo: Boolean,
    val robotRequestGeneration: Long?,
)

private data class JunqiSessionSnapshot(
    val game: JunqiState,
    val knowledge: Map<JunqiSide, JunqiKnowledge>,
    val score: JunqiScore,
    val lastMove: JunqiMove?,
)

class JunqiSession private constructor(
    private val mode: JunqiMode,
    playerSide: JunqiSide,
    initialGame: JunqiState?,
    initialScore: JunqiScore,
) {
    private var playerSide = playerSide
    private var phase = if (initialGame == null) JunqiPhase.DEPLOYMENT else JunqiPhase.PLAYING
    private var surfaceSide = initialGame?.currentSide ?: playerSide
    private var game: JunqiState? = initialGame
    private var score = initialScore
    private var selectedDeploymentPiece: JunqiPosition? = null
    private var deployments = linkedMapOf(
        playerSide to JunqiDeployment.default(playerSide),
    )
    private var knowledge = initialGame?.let(::initialKnowledge) ?: emptyMap()
    private var history = mutableListOf<JunqiSessionSnapshot>()
    private var lastMove: JunqiMove? = null
    private var generation = 0L
    private var roundIndex = 0L
    private var deploymentRandomizationIndex = 0L
    private var pendingRobotRequest: JunqiRobotRequest? = null

    var state: JunqiSessionState = publish()
        private set

    val robotRequest: JunqiRobotRequest?
        get() = pendingRobotRequest

    constructor(
        mode: JunqiMode,
        playerSide: JunqiSide = JunqiSide.RED,
    ) : this(mode, normalizedPlayerSide(mode, playerSide), initialGame = null, initialScore = JunqiScore())

    init {
        if (initialGame != null) prepareRobotRequestIfNeeded()
        state = publish()
    }

    fun selectDeploymentPiece(position: JunqiPosition): JunqiSessionState {
        check(phase == JunqiPhase.DEPLOYMENT) { "Junqi pieces can be selected only during deployment" }
        val deployment = currentDeployment()
        check(deployment.any { it.position == position }) { "Junqi deployment selection must use an occupied own station" }
        val selected = selectedDeploymentPiece
        if (selected == null) {
            selectedDeploymentPiece = position
        } else {
            deployments[surfaceSide] = JunqiDeployment.swapIfLegal(deployment, selected, position)
            selectedDeploymentPiece = null
        }
        return refresh()
    }

    fun randomizeDeployment(seed: Long? = null): JunqiSessionState {
        check(phase == JunqiPhase.DEPLOYMENT) { "Junqi deployment can be randomized only before ready" }
        val resolvedSeed = seed ?: defaultDeploymentSeed().also { deploymentRandomizationIndex += 1 }
        deployments[surfaceSide] = JunqiDeployment.random(surfaceSide, resolvedSeed)
        selectedDeploymentPiece = null
        return refresh()
    }

    fun resetDeployment(): JunqiSessionState {
        check(phase == JunqiPhase.DEPLOYMENT) { "Junqi deployment can be reset only before ready" }
        deployments[surfaceSide] = JunqiDeployment.default(surfaceSide)
        selectedDeploymentPiece = null
        return refresh()
    }

    fun ready(): JunqiSessionState {
        check(phase == JunqiPhase.DEPLOYMENT) { "Junqi ready is available only during deployment" }
        val deployment = currentDeployment()
        check(JunqiDeployment.isLegal(deployment, surfaceSide)) { "Junqi deployment must be legal before ready" }
        selectedDeploymentPiece = null
        invalidateRobotRequest()

        if (mode == JunqiMode.TWO_PLAYERS && surfaceSide == JunqiSide.RED) {
            surfaceSide = JunqiSide.BLUE
            deployments[JunqiSide.BLUE] = JunqiDeployment.default(JunqiSide.BLUE)
            phase = JunqiPhase.HANDOFF
            return refresh()
        }

        if (mode == JunqiMode.SINGLE_PLAYER) {
            val robotSide = other(playerSide)
            deployments[robotSide] = JunqiDeployment.random(robotSide, robotDeploymentSeed())
        }
        startGame()
        if (mode == JunqiMode.SINGLE_PLAYER && playerSide == JunqiSide.RED) {
            phase = JunqiPhase.PLAYING
            surfaceSide = JunqiSide.RED
        } else {
            phase = JunqiPhase.HANDOFF
            surfaceSide = if (mode == JunqiMode.SINGLE_PLAYER) playerSide else JunqiSide.RED
        }
        return refresh()
    }

    fun acceptHandoff(): JunqiSessionState {
        check(phase == JunqiPhase.HANDOFF) { "Junqi handoff can be accepted only from the handoff phase" }
        invalidateRobotRequest()
        if (game == null) {
            phase = JunqiPhase.DEPLOYMENT
        } else {
            phase = JunqiPhase.PLAYING
            surfaceSide = requireNotNull(game).currentSide
            prepareRobotRequestIfNeeded()
        }
        return refresh()
    }

    fun play(move: JunqiMove): JunqiSessionState {
        check(phase == JunqiPhase.PLAYING) { "Junqi moves can be played only during active play" }
        val current = requireNotNull(game)
        if (mode == JunqiMode.SINGLE_PLAYER) {
            check(current.currentSide == playerSide) { "The human cannot play the robot side" }
        }
        require(move in JunqiRules.legalMoves(current, current.currentSide)) { "Illegal Junqi session move: $move" }

        history += snapshot()
        applyMove(current, move)
        return settleMove(movingSide = current.currentSide)
    }

    fun acknowledgeBattle(): JunqiSessionState {
        check(phase == JunqiPhase.BATTLE_RESULT) { "Junqi battle acknowledgement requires a battle result" }
        invalidateRobotRequest()
        val current = requireNotNull(game)
        if (mode == JunqiMode.TWO_PLAYERS) {
            phase = JunqiPhase.HANDOFF
            surfaceSide = current.currentSide
        } else {
            phase = JunqiPhase.PLAYING
            surfaceSide = current.currentSide
            prepareRobotRequestIfNeeded()
        }
        return refresh()
    }

    fun applyRobotMove(request: JunqiRobotRequest, move: JunqiMove?): JunqiSessionState {
        val current = game
        if (
            request != pendingRobotRequest ||
            request.generation != generation ||
            phase != JunqiPhase.PLAYING ||
            mode != JunqiMode.SINGLE_PLAYER ||
            current == null ||
            current.currentSide == playerSide
        ) {
            return state
        }
        if (move == null) return state
        require(move in JunqiRules.legalMoves(current, current.currentSide)) { "Illegal Junqi robot move: $move" }

        pendingRobotRequest = null
        applyMove(current, move)
        return settleMove(movingSide = current.currentSide)
    }

    fun undo(): JunqiSessionState {
        if (!canUndo()) return state
        val snapshot = history.removeAt(history.lastIndex)
        invalidateRobotRequest()
        game = snapshot.game
        knowledge = snapshot.knowledge
        score = snapshot.score
        lastMove = snapshot.lastMove
        surfaceSide = snapshot.game.currentSide
        phase = if (mode == JunqiMode.SINGLE_PLAYER) JunqiPhase.PLAYING else JunqiPhase.HANDOFF
        return refresh()
    }

    fun restart(): JunqiSessionState {
        val previousResult = game?.result
        playerSide = if (mode == JunqiMode.TWO_PLAYERS) {
            JunqiSide.RED
        } else {
            when (previousResult?.winner) {
                playerSide -> other(playerSide)
                null -> playerSide
                else -> JunqiSide.RED
            }
        }
        invalidateRobotRequest()
        roundIndex += 1
        deploymentRandomizationIndex = 0L
        game = null
        knowledge = emptyMap()
        history.clear()
        lastMove = null
        selectedDeploymentPiece = null
        surfaceSide = playerSide
        deployments = linkedMapOf(playerSide to JunqiDeployment.default(playerSide))
        phase = JunqiPhase.DEPLOYMENT
        return refresh()
    }

    private fun startGame() {
        val red = requireNotNull(deployments[JunqiSide.RED]) { "Red Junqi deployment is missing" }
        val blue = requireNotNull(deployments[JunqiSide.BLUE]) { "Blue Junqi deployment is missing" }
        check(JunqiDeployment.isLegal(red, JunqiSide.RED))
        check(JunqiDeployment.isLegal(blue, JunqiSide.BLUE))
        game = JunqiState((red + blue).associateBy { it.position }, currentSide = JunqiSide.RED)
        knowledge = initialKnowledge(requireNotNull(game))
        history.clear()
        lastMove = null
    }

    private fun settleMove(movingSide: JunqiSide): JunqiSessionState {
        val current = requireNotNull(game)
        invalidateRobotRequest()
        surfaceSide = movingSide
        if (current.result != null) {
            score = score.record(current.result, mode, playerSide)
            phase = JunqiPhase.FINISHED
        } else if (current.lastBattleOutcome != null) {
            phase = JunqiPhase.BATTLE_RESULT
        } else if (mode == JunqiMode.TWO_PLAYERS) {
            surfaceSide = current.currentSide
            phase = JunqiPhase.HANDOFF
        } else {
            surfaceSide = current.currentSide
            phase = JunqiPhase.PLAYING
            prepareRobotRequestIfNeeded()
        }
        return refresh()
    }

    private fun applyMove(before: JunqiState, move: JunqiMove) {
        val after = JunqiRules.applyMove(before, move)
        updateKnowledge(before, after, move)
        game = after
        lastMove = move
    }

    private fun updateKnowledge(before: JunqiState, after: JunqiState, move: JunqiMove) {
        val attacker = before.pieces.getValue(move.from)
        val defender = before.pieces[move.to]
        val outcome = after.lastBattleOutcome
        val updated = knowledge.toMutableMap()

        if (defender == null) {
            val observer = other(attacker.side)
            updated[observer] = updated.getValue(observer).update(
                JunqiKnowledgeEvent.PieceMoved(attacker.id),
            )
        } else {
            val battle = requireNotNull(outcome)
            updated[attacker.side] = updated.getValue(attacker.side).update(
                JunqiKnowledgeEvent.Battle(
                    enemyPieceId = defender.id,
                    ownPieceType = attacker.type,
                    enemyWasAttacker = false,
                    outcome = battle,
                ),
            )
            updated[defender.side] = updated.getValue(defender.side).update(
                JunqiKnowledgeEvent.Battle(
                    enemyPieceId = attacker.id,
                    ownPieceType = defender.type,
                    enemyWasAttacker = true,
                    outcome = battle,
                ),
            )
        }

        for (revealedSide in after.revealedFlags - before.revealedFlags) {
            val flag = after.pieces.values.singleOrNull { piece ->
                piece.side == revealedSide && piece.type == JunqiPieceType.FLAG
            } ?: continue
            val observer = other(revealedSide)
            updated[observer] = updated.getValue(observer).update(
                JunqiKnowledgeEvent.FlagRevealed(flag.id),
            )
        }
        knowledge = immutableKnowledge(updated)
    }

    private fun prepareRobotRequestIfNeeded() {
        val current = game ?: return
        if (
            mode != JunqiMode.SINGLE_PLAYER ||
            phase != JunqiPhase.PLAYING ||
            current.result != null ||
            current.currentSide == playerSide
        ) {
            pendingRobotRequest = null
            return
        }
        val robotSide = current.currentSide
        pendingRobotRequest = JunqiRobotRequest(
            generation = generation,
            observation = JunqiObservation.from(current, robotSide),
            knowledge = knowledge.getValue(robotSide),
            level = JunqiAiLevel.forPlayerScore(score.player),
        )
    }

    private fun snapshot(): JunqiSessionSnapshot = JunqiSessionSnapshot(
        game = requireNotNull(game),
        knowledge = knowledge,
        score = score,
        lastMove = lastMove,
    )

    private fun currentDeployment(): List<JunqiPiece> =
        requireNotNull(deployments[surfaceSide]) { "Junqi deployment is missing for $surfaceSide" }

    private fun publish(): JunqiSessionState {
        val currentGame = game
        val observation = when (phase) {
            JunqiPhase.PLAYING -> currentGame?.let { visibleObservation(it) }
            JunqiPhase.FINISHED -> currentGame?.let { visibleObservation(it) }
            else -> null
        }
        val deployment = if (phase == JunqiPhase.DEPLOYMENT) {
            Collections.unmodifiableList(currentDeployment().toList())
        } else {
            emptyList()
        }
        return JunqiSessionState(
            phase = phase,
            mode = mode,
            playerSide = playerSide,
            robotSide = other(playerSide).takeIf { mode == JunqiMode.SINGLE_PLAYER },
            currentSide = surfaceSide,
            deployment = deployment,
            selectedDeploymentPiece = selectedDeploymentPiece,
            observation = observation,
            battleOutcome = currentGame?.lastBattleOutcome.takeIf { phase == JunqiPhase.BATTLE_RESULT },
            result = currentGame?.result,
            score = score,
            lastMove = lastMove.takeIf {
                phase == JunqiPhase.PLAYING || phase == JunqiPhase.FINISHED
            },
            canUndo = canUndo(),
            robotRequestGeneration = pendingRobotRequest?.generation,
        )
    }

    private fun visibleObservation(current: JunqiState): JunqiObservation {
        val viewer = if (mode == JunqiMode.SINGLE_PLAYER) playerSide else surfaceSide
        return JunqiObservation.from(current, viewer)
    }

    private fun refresh(): JunqiSessionState {
        state = publish()
        return state
    }

    private fun invalidateRobotRequest() {
        generation += 1
        pendingRobotRequest = null
    }

    private fun canUndo(): Boolean = history.isNotEmpty() && game?.result?.winner == null

    private fun defaultDeploymentSeed(): Long =
        AI_PACKAGE_SALT xor
            (roundIndex * ROUND_SEED_MULTIPLIER) xor
            (deploymentRandomizationIndex * RANDOMIZATION_SEED_MULTIPLIER) xor
            surfaceSide.ordinal.toLong()

    private fun robotDeploymentSeed(): Long =
        AI_PACKAGE_SALT xor ROBOT_DEPLOYMENT_SALT xor roundIndex xor playerSide.ordinal.toLong()

    companion object {
        internal fun started(
            mode: JunqiMode,
            state: JunqiState,
            playerSide: JunqiSide = JunqiSide.RED,
            score: JunqiScore = JunqiScore(),
        ): JunqiSession = JunqiSession(
            mode = mode,
            playerSide = normalizedPlayerSide(mode, playerSide),
            initialGame = state,
            initialScore = score,
        )

        private fun normalizedPlayerSide(mode: JunqiMode, requested: JunqiSide): JunqiSide =
            if (mode == JunqiMode.TWO_PLAYERS) JunqiSide.RED else requested

        private fun initialKnowledge(state: JunqiState): Map<JunqiSide, JunqiKnowledge> = immutableKnowledge(
            JunqiSide.entries.associateWith { side ->
                JunqiKnowledge.from(JunqiObservation.from(state, side))
            },
        )

        private fun immutableKnowledge(
            source: Map<JunqiSide, JunqiKnowledge>,
        ): Map<JunqiSide, JunqiKnowledge> = Collections.unmodifiableMap(source.toMap())

        private const val ROBOT_DEPLOYMENT_SALT = 0x44_45_50_4C_4F_59_00_01L
        private const val ROUND_SEED_MULTIPLIER = -7_046_029_254_386_353_131L
        private const val RANDOMIZATION_SEED_MULTIPLIER = -4_658_895_280_553_007_687L
    }
}
