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
        var state by remember { mutableStateOf(XiangqiState.initial()) }
        var turn by remember { mutableStateOf(Side.RED) }
        var selected by remember { mutableStateOf<Pair<Int, Int>?>(null) }
        var winner by remember { mutableStateOf<Side?>(null) }

        fun applyMove(move: XiangqiMove) {
            winner = XiangqiRules.winnerAfterMove(state, move)
            state = state.apply(move)
            selected = null
            if (winner == null && mode == GameMode.SINGLE_PLAYER) {
                val robot = XiangqiRules.robotMove(state, Side.BLACK)
                if (robot != null) {
                    winner = XiangqiRules.winnerAfterMove(state, robot)
                    state = state.apply(robot)
                }
                turn = Side.RED
            } else if (winner == null) {
                turn = turn.other()
            }
        }

        fun tap(row: Int, col: Int) {
            if (winner != null) return
            val piece = state.piece(row, col)
            val currentSelection = selected
            if (currentSelection == null) {
                if (piece?.side == turn) selected = row to col
                return
            }
            val move = XiangqiMove(currentSelection.first, currentSelection.second, row, col)
            if (XiangqiRules.isLegalMove(state, move, turn)) {
                applyMove(move)
            } else {
                selected = if (piece?.side == turn) row to col else null
            }
        }

        Surface(Modifier.fillMaxSize(), color = XiangqiPaper) {
            XiangqiGameLayout(
                state = state,
                selected = selected,
                status = statusText(winner, turn, mode),
                onTap = ::tap,
                onExit = context::exitGame,
                texture = texture
            )
        }
    }

    private fun statusText(winner: Side?, turn: Side, mode: GameMode): String {
        winner?.let { return if (it == Side.RED) "红方胜" else "黑方胜" }
        return if (mode == GameMode.SINGLE_PLAYER) {
            "你的回合 · 执红"
        } else {
            "当前回合：${if (turn == Side.RED) "红方" else "黑方"}"
        }
    }

    companion object {
        val manifest = GameManifest(
            gameId = "xiangqi",
            displayName = "象棋",
            versionCode = 1,
            versionName = "1.0.0",
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
