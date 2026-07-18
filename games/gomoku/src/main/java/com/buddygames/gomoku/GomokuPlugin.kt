package com.buddygames.gomoku

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

private val GomokuPaper = Color(0xFFF0E6D2)
private val GomokuWood = Color(0xFFBC9561)
private val GomokuDarkWood = Color(0xFF745936)
private val GomokuInk = Color(0xFF2D3438)
private val GomokuIvory = Color(0xFFFFFFFC)
private val GomokuVermilion = Color(0xFF9B2F2F)

internal fun gomokuIntersectionFraction(index: Int): Float {
    require(index in 0 until GomokuState.SIZE)
    return index.toFloat() / (GomokuState.SIZE - 1)
}

internal fun gomokuBoardSide(availableWidth: Float, availableHeight: Float): Float =
    minOf(availableWidth, availableHeight).coerceIn(280f, 680f)

class GomokuPlugin : GamePlugin {
    override fun getManifest(): GameManifest = manifest

    @Composable
    override fun MainScreen(context: GameContext) {
        val texture = remember(context.gamePackage.assetsDir) {
            loadGomokuTexture(context.gamePackage.assetsDir.resolve("textures/gomoku-shelf.png"))
        }
        GomokuMenu(
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
            loadGomokuTexture(context.gamePackage.assetsDir.resolve("textures/gomoku-shelf.png"))
        }
        val initialRound = remember { newGomokuRound() }
        var state by remember { mutableStateOf(initialRound.state) }
        var turn by remember { mutableStateOf(initialRound.turn) }
        var winner by remember { mutableStateOf(initialRound.winner) }
        var playerStone by remember { mutableStateOf(initialRound.playerStone) }
        var score by remember { mutableStateOf(GomokuScore()) }
        var history by remember { mutableStateOf(emptyList<GomokuSnapshot>()) }

        fun play(row: Int, col: Int) {
            if (winner != null || state.legalMoves().isEmpty() || state.cell(row, col) != null) return
            if (mode == GameMode.SINGLE_PLAYER && turn != playerStone) return
            history = history + GomokuSnapshot(state, turn, winner, score)
            state = state.place(row, col, turn)
            winner = GomokuRules.winner(state)
            if (winner != null) {
                score = score.record(winner)
            } else if (state.legalMoves().isNotEmpty() && mode == GameMode.SINGLE_PLAYER) {
                val robotStone = playerStone.other()
                val robot = GomokuRules.robotMove(state, robotStone)
                state = state.place(robot.row, robot.col, robotStone)
                winner = GomokuRules.winner(state)
                if (winner != null) score = score.record(winner)
                turn = playerStone
            } else if (winner == null) {
                turn = turn.other()
            }
        }

        fun undo() {
            val undo = undoGomoku(history) ?: return
            state = undo.snapshot.state
            turn = undo.snapshot.turn
            winner = undo.snapshot.winner
            score = undo.snapshot.score
            history = undo.remainingHistory
        }

        fun restart() {
            val nextPlayerStone = if (mode == GameMode.SINGLE_PLAYER) {
                nextGomokuPlayerStone(playerStone, winner)
            } else {
                Stone.BLACK
            }
            val round = newGomokuRound(nextPlayerStone)
            state = round.state
            turn = round.turn
            winner = round.winner
            playerStone = round.playerStone
            history = emptyList()
        }

        val gameOver = winner != null || state.legalMoves().isEmpty()
        Surface(Modifier.fillMaxSize(), color = GomokuPaper) {
            GomokuGameLayout(
                state = state,
                status = statusText(winner, turn, playerStone, mode, gameOver),
                score = score.displayText,
                gameOver = gameOver,
                canUndo = history.isNotEmpty(),
                showUndo = shouldShowGomokuUndo(winner),
                onPlay = ::play,
                onUndo = ::undo,
                onRestart = ::restart,
                onExit = context::exitGame,
                texture = texture
            )
        }
    }

    private fun statusText(
        winner: Stone?,
        turn: Stone,
        playerStone: Stone,
        mode: GameMode,
        gameOver: Boolean
    ): String {
        winner?.let { return if (it == Stone.BLACK) "黑方胜" else "白方胜" }
        if (gameOver) return "平局"
        return if (mode == GameMode.SINGLE_PLAYER) {
            "你的回合 · 执${if (playerStone == Stone.BLACK) "黑" else "白"}"
        } else {
            "当前回合：${if (turn == Stone.BLACK) "黑方" else "白方"}"
        }
    }

    companion object {
        val manifest = GameManifest(
            gameId = "gomoku",
            displayName = "五子棋",
            versionCode = 2,
            versionName = "0.0.2",
            entryClass = "com.buddygames.gomoku.GomokuPlugin",
            minShellApi = 1,
            icon = "assets/icon.txt"
        )
    }
}

private fun loadGomokuTexture(file: File): ImageBitmap? = runCatching {
    requireNotNull(BitmapFactory.decodeFile(file.absolutePath)).asImageBitmap()
}.getOrNull()
