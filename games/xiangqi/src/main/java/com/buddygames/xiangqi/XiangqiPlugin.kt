package com.buddygames.xiangqi

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.buddygames.api.GameContext
import com.buddygames.api.GameManifest
import com.buddygames.api.GameMode
import com.buddygames.api.GamePlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

private val XiangqiPaper = Color(0xFFF1E7D5)

internal fun xiangqiFileFraction(column: Int): Float {
    require(column in 0 until XiangqiState.COLS)
    return column.toFloat() / (XiangqiState.COLS - 1)
}

internal fun xiangqiRankFraction(row: Int): Float {
    require(row in 0 until XiangqiState.ROWS)
    return row.toFloat() / (XiangqiState.ROWS - 1)
}

internal data class XiangqiBoardSize(val width: Float, val height: Float)

internal fun xiangqiBoardSize(availableWidth: Float, availableHeight: Float): XiangqiBoardSize {
    val height = minOf(
        availableHeight.coerceAtLeast(1f),
        availableWidth.coerceAtLeast(1f) / XIANGQI_BOARD_ASPECT_RATIO,
        820f
    )
    return XiangqiBoardSize(width = height * XIANGQI_BOARD_ASPECT_RATIO, height = height)
}

class XiangqiPlugin : GamePlugin {
    override fun getManifest(): GameManifest = manifest

    @Composable
    override fun MainScreen(context: GameContext) {
        val textures = remember(context.gamePackage.assetsDir) {
            loadXiangqiTextures(context.gamePackage.assetsDir)
        }
        XiangqiMenu(
            textures = textures,
            versionName = context.gamePackage.manifest.versionName,
            onSingle = { context.startGame(GameMode.SINGLE_PLAYER) },
            onTwo = { context.startGame(GameMode.TWO_PLAYERS) },
            onExit = context::exitGame
        )
    }

    @Composable
    override fun GameScreen(context: GameContext, mode: GameMode) {
        val textures = remember(context.gamePackage.assetsDir) {
            loadXiangqiTextures(context.gamePackage.assetsDir)
        }
        val initialRound = remember { newXiangqiRound() }
        var state by remember { mutableStateOf(initialRound.state) }
        var turn by remember { mutableStateOf(initialRound.turn) }
        var selected by remember { mutableStateOf(initialRound.selected) }
        var result by remember { mutableStateOf(initialRound.result) }
        var playerSide by remember { mutableStateOf(initialRound.playerSide) }
        var lastMove by remember { mutableStateOf(initialRound.lastMove) }
        var score by remember { mutableStateOf(XiangqiScore()) }
        var history by remember { mutableStateOf(emptyList<XiangqiSnapshot>()) }
        var searchGeneration by remember { mutableStateOf(0) }

        fun applyMove(move: XiangqiMove) {
            result = XiangqiRules.winnerAfterMove(state, move)?.let(XiangqiResult::fromWinner)
            state = state.apply(move)
            lastMove = move
            selected = null
            if (result != null) score = score.record(result, mode, playerSide)
            if (result == null && mode == GameMode.SINGLE_PLAYER) {
                turn = playerSide.other()
            } else if (result == null) {
                turn = turn.other()
            }
        }

        fun tap(row: Int, col: Int) {
            if (result != null) return
            if (needsXiangqiRobotTurn(mode, turn, playerSide, result)) return
            val piece = state.piece(row, col)
            val currentSelection = selected
            if (currentSelection == null) {
                if (piece?.side == turn) selected = row to col
                return
            }
            val move = XiangqiMove(currentSelection.first, currentSelection.second, row, col)
            if (XiangqiRules.isLegalMove(state, move, turn)) {
                history = history + XiangqiSnapshot(state, turn, result, score, lastMove)
                applyMove(move)
            } else {
                selected = if (piece?.side == turn) row to col else null
            }
        }

        fun undo() {
            val undo = undoXiangqi(history) ?: return
            searchGeneration++
            state = undo.snapshot.state
            turn = undo.snapshot.turn
            selected = null
            result = undo.snapshot.result
            score = undo.snapshot.score
            lastMove = undo.snapshot.lastMove
            history = undo.remainingHistory
        }

        fun restart() {
            searchGeneration++
            val nextPlayerSide = if (mode == GameMode.SINGLE_PLAYER) {
                nextXiangqiPlayerSide(playerSide, result)
            } else {
                Side.RED
            }
            val round = newXiangqiRound(nextPlayerSide)
            state = round.state
            turn = round.turn
            selected = round.selected
            result = round.result
            playerSide = round.playerSide
            lastMove = round.lastMove
            history = emptyList()
        }

        val robotThinking = needsXiangqiRobotTurn(mode, turn, playerSide, result)
        val intelligenceLevel = score.intelligenceLevel
        LaunchedEffect(
            state,
            turn,
            playerSide,
            result,
            intelligenceLevel,
            mode,
            searchGeneration
        ) {
            if (!needsXiangqiRobotTurn(mode, turn, playerSide, result)) return@LaunchedEffect
            val requestedState = state
            val requestedSide = turn
            val requestedGeneration = searchGeneration
            val robot = withContext(Dispatchers.Default) {
                XiangqiAi.chooseMove(
                    state = requestedState,
                    side = requestedSide,
                    level = intelligenceLevel,
                    shouldStop = { !isActive }
                )
            }
            if (
                robot == null ||
                !shouldApplyXiangqiRobotResult(
                    requestedState,
                    requestedSide,
                    requestedGeneration,
                    state,
                    turn,
                    searchGeneration,
                    result
                )
            ) {
                return@LaunchedEffect
            }
            result = XiangqiRules.winnerAfterMove(state, robot)?.let(XiangqiResult::fromWinner)
            state = state.apply(robot)
            lastMove = robot
            selected = null
            if (result != null) score = score.record(result, mode, playerSide)
            turn = playerSide
        }

        val inCheck = result == null && XiangqiRules.isInCheck(state, turn)
        Surface(Modifier.fillMaxSize(), color = XiangqiPaper) {
            XiangqiGameLayout(
                state = state,
                selected = selected,
                lastMove = lastMove,
                status = statusText(result, turn, playerSide, mode, robotThinking, intelligenceLevel),
                turn = turn,
                score = score.displayText(mode),
                intelligenceLevel = if (mode == GameMode.SINGLE_PLAYER) intelligenceLevel else null,
                gameOver = result != null,
                inCheck = inCheck,
                canUndo = history.isNotEmpty(),
                showUndo = shouldShowXiangqiUndo(result),
                rotateBoard = shouldRotateXiangqiBoard(mode, playerSide),
                onTap = ::tap,
                onUndo = ::undo,
                onRestart = ::restart,
                onExit = context::exitGame,
                textures = textures
            )
        }
    }

    private fun statusText(
        result: XiangqiResult?,
        turn: Side,
        playerSide: Side,
        mode: GameMode,
        robotThinking: Boolean,
        intelligenceLevel: Int
    ): String {
        result?.let {
            return when (it) {
                XiangqiResult.RED_WIN -> "红方胜"
                XiangqiResult.BLACK_WIN -> "黑方胜"
                XiangqiResult.DRAW -> "和棋"
            }
        }
        return if (mode == GameMode.SINGLE_PLAYER) {
            if (robotThinking) {
                "智能思考中 · 等级 $intelligenceLevel"
            } else {
                "你的回合 · 执${if (playerSide == Side.RED) "红" else "黑"}"
            }
        } else {
            "当前回合：${if (turn == Side.RED) "红方" else "黑方"}"
        }
    }

    companion object {
        val manifest = GameManifest(
            gameId = "xiangqi",
            displayName = "象棋",
            versionCode = 10,
            versionName = "0.0.10",
            entryClass = "com.buddygames.xiangqi.XiangqiPlugin",
            minShellApi = 1,
            icon = "assets/icon.png"
        )
    }
}

internal fun XiangqiPiece.displayLabel(): String {
    return when (type) {
        PieceType.GENERAL -> if (side == Side.RED) "帥" else "將"
        PieceType.ROOK -> "車"
        PieceType.HORSE -> "馬"
        PieceType.CANNON -> "炮"
        PieceType.ELEPHANT -> if (side == Side.RED) "相" else "象"
        PieceType.ADVISOR -> if (side == Side.RED) "仕" else "士"
        PieceType.SOLDIER -> if (side == Side.RED) "兵" else "卒"
    }
}
