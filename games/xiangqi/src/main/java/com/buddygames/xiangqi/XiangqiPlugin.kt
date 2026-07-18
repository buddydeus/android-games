package com.buddygames.xiangqi

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.buddygames.api.GameContext
import com.buddygames.api.GameManifest
import com.buddygames.api.GameMode
import com.buddygames.api.GamePlugin
import java.io.File

private val XiangqiPaper = Color(0xFFF1E7D5)
private val XiangqiBoard = Color(0xFFD2AC73)
private val XiangqiWood = Color(0xFF745936)
private val XiangqiInk = Color(0xFF2B2722)
private val XiangqiIvory = Color(0xFFFFFFFC)
private val XiangqiVermilion = Color(0xFF9B2F2F)
private val XiangqiGold = Color(0xFFB88438)

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
    val height = minOf(availableHeight, availableWidth / 1.03f, 820f).coerceAtLeast(360f)
    return XiangqiBoardSize(width = height * 1.03f, height = height)
}

class XiangqiPlugin : GamePlugin {
    override fun getManifest(): GameManifest = manifest

    @Composable
    override fun MainScreen(context: GameContext) {
        val texture = remember(context.gamePackage.assetsDir) {
            loadXiangqiTexture(context.gamePackage.assetsDir.resolve("textures/xiangqi-shelf.png"))
        }
        XiangqiMenu(
            texture = texture,
            versionName = context.gamePackage.manifest.versionName,
            onSingle = { context.startGame(GameMode.SINGLE_PLAYER) },
            onTwo = { context.startGame(GameMode.TWO_PLAYERS) },
            onExit = context::exitGame
        )
    }

    @Composable
    override fun GameScreen(context: GameContext, mode: GameMode) {
        val texture = remember(context.gamePackage.assetsDir) {
            loadXiangqiTexture(context.gamePackage.assetsDir.resolve("textures/xiangqi-shelf.png"))
        }
        val initialRound = remember { newXiangqiRound() }
        var state by remember { mutableStateOf(initialRound.state) }
        var turn by remember { mutableStateOf(initialRound.turn) }
        var selected by remember { mutableStateOf(initialRound.selected) }
        var winner by remember { mutableStateOf(initialRound.winner) }
        var playerSide by remember { mutableStateOf(initialRound.playerSide) }
        var lastMove by remember { mutableStateOf(initialRound.lastMove) }
        var score by remember { mutableStateOf(XiangqiScore()) }
        var history by remember { mutableStateOf(emptyList<XiangqiSnapshot>()) }

        fun applyMove(move: XiangqiMove) {
            winner = XiangqiRules.winnerAfterMove(state, move)
            state = state.apply(move)
            lastMove = move
            selected = null
            if (winner != null) score = score.record(winner)
            if (winner == null && mode == GameMode.SINGLE_PLAYER) {
                val robotSide = playerSide.other()
                val robot = XiangqiRules.robotMove(state, robotSide)
                if (robot != null) {
                    winner = XiangqiRules.winnerAfterMove(state, robot)
                    state = state.apply(robot)
                    lastMove = robot
                    if (winner != null) score = score.record(winner)
                }
                turn = playerSide
            } else if (winner == null) {
                turn = turn.other()
            }
        }

        fun tap(row: Int, col: Int) {
            if (winner != null) return
            if (mode == GameMode.SINGLE_PLAYER && turn != playerSide) return
            val piece = state.piece(row, col)
            val currentSelection = selected
            if (currentSelection == null) {
                if (piece?.side == turn) selected = row to col
                return
            }
            val move = XiangqiMove(currentSelection.first, currentSelection.second, row, col)
            if (XiangqiRules.isLegalMove(state, move, turn)) {
                history = history + XiangqiSnapshot(state, turn, winner, score, lastMove)
                applyMove(move)
            } else {
                selected = if (piece?.side == turn) row to col else null
            }
        }

        fun undo() {
            val undo = undoXiangqi(history) ?: return
            state = undo.snapshot.state
            turn = undo.snapshot.turn
            selected = null
            winner = undo.snapshot.winner
            score = undo.snapshot.score
            lastMove = undo.snapshot.lastMove
            history = undo.remainingHistory
        }

        fun restart() {
            val nextPlayerSide = if (mode == GameMode.SINGLE_PLAYER) {
                nextXiangqiPlayerSide(playerSide, winner)
            } else {
                Side.RED
            }
            val round = newXiangqiRound(nextPlayerSide)
            state = round.state
            turn = round.turn
            selected = round.selected
            winner = round.winner
            playerSide = round.playerSide
            lastMove = round.lastMove
            history = emptyList()
        }

        val inCheck = winner == null && XiangqiRules.isInCheck(state, turn)
        Surface(Modifier.fillMaxSize(), color = XiangqiPaper) {
            XiangqiGameLayout(
                state = state,
                selected = selected,
                lastMove = lastMove,
                status = statusText(winner, turn, playerSide, mode),
                turn = turn,
                score = score.displayText,
                gameOver = winner != null,
                inCheck = inCheck,
                canUndo = history.isNotEmpty(),
                showUndo = shouldShowXiangqiUndo(winner),
                rotateBoard = shouldRotateXiangqiBoard(mode, playerSide),
                onTap = ::tap,
                onUndo = ::undo,
                onRestart = ::restart,
                onExit = context::exitGame,
                texture = texture
            )
        }
    }

    private fun statusText(
        winner: Side?,
        turn: Side,
        playerSide: Side,
        mode: GameMode
    ): String {
        winner?.let { return if (it == Side.RED) "红方胜" else "黑方胜" }
        return if (mode == GameMode.SINGLE_PLAYER) {
            "你的回合 · 执${if (playerSide == Side.RED) "红" else "黑"}"
        } else {
            "当前回合：${if (turn == Side.RED) "红方" else "黑方"}"
        }
    }

    companion object {
        val manifest = GameManifest(
            gameId = "xiangqi",
            displayName = "象棋",
            versionCode = 5,
            versionName = "0.0.5",
            entryClass = "com.buddygames.xiangqi.XiangqiPlugin",
            minShellApi = 1,
            icon = "assets/icon.txt"
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

private fun loadXiangqiTexture(file: File): ImageBitmap? = runCatching {
    requireNotNull(BitmapFactory.decodeFile(file.absolutePath)).asImageBitmap()
}.getOrNull()
