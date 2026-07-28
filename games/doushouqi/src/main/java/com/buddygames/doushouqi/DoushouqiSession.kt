package com.buddygames.doushouqi

enum class DoushouqiMode {
    SINGLE_PLAYER,
    TWO_PLAYERS,
}

data class DoushouqiScore(
    val player: Int = 0,
    val robot: Int = 0,
    val green: Int = 0,
    val vermilion: Int = 0,
) {
    val intelligenceLevel: DoushouqiAiLevel
        get() = DoushouqiAiLevel.forLevel((player + 1).coerceAtMost(10))

    internal fun record(
        result: DoushouqiResult?,
        mode: DoushouqiMode,
        playerSide: DoushouqiSide,
    ): DoushouqiScore {
        val winner = (result as? DoushouqiResult.Win)?.winner ?: return this
        return when {
            mode == DoushouqiMode.SINGLE_PLAYER && winner == playerSide ->
                copy(player = player + 1)
            mode == DoushouqiMode.SINGLE_PLAYER ->
                copy(robot = robot + 1)
            winner == DoushouqiSide.PINE_GREEN ->
                copy(green = green + 1)
            else ->
                copy(vermilion = vermilion + 1)
        }
    }
}

data class DoushouqiRobotRequest(
    val generation: Long,
    val sourcePositionKey: Long,
    val state: DoushouqiState,
    val level: DoushouqiAiLevel,
)

data class DoushouqiRoundCaptures(
    val capturedByGreen: DoushouqiPiece? = null,
    val capturedByRed: DoushouqiPiece? = null,
) {
    internal fun recordCapture(
        attacker: DoushouqiSide,
        captured: DoushouqiPiece?,
    ): DoushouqiRoundCaptures = when {
        captured == null -> this
        attacker == DoushouqiSide.PINE_GREEN -> copy(capturedByGreen = captured)
        else -> copy(capturedByRed = captured)
    }
}

data class DoushouqiSessionState(
    val position: DoushouqiState,
    val playerSide: DoushouqiSide,
    val score: DoushouqiScore,
    val historySize: Int,
    val generation: Long,
    val robotRequest: DoushouqiRobotRequest?,
    val lastCompletedRoundCaptures: DoushouqiRoundCaptures,
) {
    val intelligenceLevel: DoushouqiAiLevel
        get() = score.intelligenceLevel

    internal val lastCapturedPiece: DoushouqiPiece?
        get() =
            lastCompletedRoundCaptures.capturedByRed
                ?: lastCompletedRoundCaptures.capturedByGreen
}

private data class DoushouqiSnapshot(
    val position: DoushouqiState,
    val score: DoushouqiScore,
    val lastCompletedRoundCaptures: DoushouqiRoundCaptures,
    val pendingRoundCaptures: DoushouqiRoundCaptures?,
)

class DoushouqiSession internal constructor(
    private val mode: DoushouqiMode,
    initialPosition: DoushouqiState = DoushouqiState.initial(),
    playerSide: DoushouqiSide = DoushouqiSide.PINE_GREEN,
    initialScore: DoushouqiScore = DoushouqiScore(),
) {
    constructor(mode: DoushouqiMode) : this(
        mode = mode,
        initialPosition = DoushouqiState.initial(),
    )

    private var position = initialPosition
    private var playerSide = playerSide
    private var score = initialScore
    private var generation = 0L
    private var history = emptyList<DoushouqiSnapshot>()
    private var lastCompletedRoundCaptures = DoushouqiRoundCaptures()
    private var pendingRoundCaptures: DoushouqiRoundCaptures? = null

    fun state(): DoushouqiSessionState = projection()

    fun play(move: DoushouqiMove): DoushouqiSessionState {
        if (
            position.result != null ||
            mode == DoushouqiMode.SINGLE_PLAYER &&
            position.sideToMove != playerSide
        ) {
            return projection()
        }
        val attacker = position.sideToMove
        val captured = position.pieceAt(move.to)
        val next = DoushouqiRules.apply(position, move) ?: return projection()
        history = history + DoushouqiSnapshot(
            position = position,
            score = score,
            lastCompletedRoundCaptures = lastCompletedRoundCaptures,
            pendingRoundCaptures = pendingRoundCaptures,
        )
        position = next
        val nextPending = when {
            mode == DoushouqiMode.SINGLE_PLAYER ->
                DoushouqiRoundCaptures().recordCapture(attacker, captured)
            attacker == DoushouqiSide.PINE_GREEN ->
                DoushouqiRoundCaptures().recordCapture(attacker, captured)
            else ->
                (pendingRoundCaptures ?: DoushouqiRoundCaptures())
                    .recordCapture(attacker, captured)
        }
        pendingRoundCaptures = nextPending
        val completesRound =
            next.result != null ||
                mode == DoushouqiMode.TWO_PLAYERS &&
                attacker == DoushouqiSide.VERMILION
        if (completesRound) {
            lastCompletedRoundCaptures = nextPending
            pendingRoundCaptures = null
        }
        score = score.record(next.result, mode, playerSide)
        generation++
        return projection()
    }

    fun applyRobotMove(
        request: DoushouqiRobotRequest,
        move: DoushouqiMove,
    ): DoushouqiSessionState {
        if (
            mode != DoushouqiMode.SINGLE_PLAYER ||
            request.generation != generation ||
            request.sourcePositionKey != position.positionKey ||
            request.state.positionKey != position.positionKey ||
            position.sideToMove == playerSide ||
            position.result != null
        ) {
            return projection()
        }
        val attacker = position.sideToMove
        val captured = position.pieceAt(move.to)
        val next = DoushouqiRules.apply(position, move) ?: return projection()
        position = next
        pendingRoundCaptures?.let { pending ->
            lastCompletedRoundCaptures = pending.recordCapture(attacker, captured)
            pendingRoundCaptures = null
        }
        score = score.record(next.result, mode, playerSide)
        generation++
        return projection()
    }

    fun undo(): DoushouqiSessionState {
        val snapshot = history.lastOrNull() ?: return projection()
        position = snapshot.position
        score = snapshot.score
        lastCompletedRoundCaptures = snapshot.lastCompletedRoundCaptures
        pendingRoundCaptures = snapshot.pendingRoundCaptures
        history = history.dropLast(1)
        generation++
        return projection()
    }

    fun restart(): DoushouqiSessionState {
        playerSide = if (mode == DoushouqiMode.TWO_PLAYERS) {
            DoushouqiSide.PINE_GREEN
        } else {
            nextPlayerSide(playerSide, position.result)
        }
        position = DoushouqiState.initial()
        history = emptyList()
        lastCompletedRoundCaptures = DoushouqiRoundCaptures()
        pendingRoundCaptures = null
        generation++
        return projection()
    }

    fun invalidate(): DoushouqiSessionState {
        generation++
        return projection()
    }

    private fun projection(): DoushouqiSessionState = DoushouqiSessionState(
        position = position,
        playerSide = playerSide,
        score = score,
        historySize = history.size,
        generation = generation,
        robotRequest = robotRequest(),
        lastCompletedRoundCaptures = lastCompletedRoundCaptures,
    )

    private fun robotRequest(): DoushouqiRobotRequest? {
        if (
            mode != DoushouqiMode.SINGLE_PLAYER ||
            position.result != null ||
            position.sideToMove == playerSide
        ) {
            return null
        }
        return DoushouqiRobotRequest(
            generation = generation,
            sourcePositionKey = position.positionKey,
            state = position,
            level = score.intelligenceLevel,
        )
    }

    private fun nextPlayerSide(
        current: DoushouqiSide,
        result: DoushouqiResult?,
    ): DoushouqiSide = when (val winner = (result as? DoushouqiResult.Win)?.winner) {
        current -> current.other()
        current.other() -> DoushouqiSide.PINE_GREEN
        else -> current
    }
}

internal fun shouldRotateDoushouqiBoard(
    mode: DoushouqiMode,
    playerSide: DoushouqiSide,
): Boolean = mode == DoushouqiMode.SINGLE_PLAYER &&
    playerSide == DoushouqiSide.VERMILION

internal fun modelPosition(
    displayRow: Int,
    displayColumn: Int,
    rotated: Boolean,
): DoushouqiPosition = if (rotated) {
    DoushouqiPosition(
        DoushouqiState.ROWS - 1 - displayRow,
        DoushouqiState.COLUMNS - 1 - displayColumn,
    )
} else {
    DoushouqiPosition(displayRow, displayColumn)
}

internal fun displayPosition(
    model: DoushouqiPosition,
    rotated: Boolean,
): DoushouqiPosition = modelPosition(model.row, model.column, rotated)

internal fun doushouqiTapMove(
    state: DoushouqiState,
    selected: DoushouqiPosition,
    tapped: DoushouqiPosition,
): DoushouqiMove? = DoushouqiRules.legalMoves(state).firstOrNull {
    it.from == selected && it.to == tapped
}

internal fun shouldShowDoushouqiUndo(result: DoushouqiResult?): Boolean =
    result !is DoushouqiResult.Win

internal fun shouldShowDoushouqiRestart(result: DoushouqiResult?): Boolean =
    result != null

internal fun doushouqiMenuLabels(): List<String> =
    listOf("单人模式", "双人对战", "退出游戏")

internal fun doushouqiVersionLabel(versionName: String): String = "版本 $versionName"
