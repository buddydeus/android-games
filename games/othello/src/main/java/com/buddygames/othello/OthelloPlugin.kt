package com.buddygames.othello

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

private val OthelloPaper = Color(0xFFEAF0E8)
private val OthelloGreen = Color(0xFF2F6F56)
private val OthelloBoard = Color(0xFF3F815F)
private val OthelloDarkWood = Color(0xFF493927)
private val OthelloIvory = Color(0xFFFFFFFC)
private val OthelloInk = Color(0xFF1F2A2D)
private val OthelloVermilion = Color(0xFF9B2F2F)

internal fun othelloBoardSide(availableWidth: Float, availableHeight: Float): Float =
    minOf(availableWidth, availableHeight).coerceIn(288f, 672f)

internal fun showOthelloLegalMoveHints(mode: GameMode): Boolean = mode == GameMode.SINGLE_PLAYER

class OthelloPlugin : GamePlugin {
    override fun getManifest(): GameManifest = manifest

    @Composable
    override fun MainScreen(context: GameContext) {
        val texture = remember(context.gamePackage.assetsDir) {
            loadOthelloTexture(context.gamePackage.assetsDir.resolve("textures/othello-shelf.png"))
        }
        OthelloMenu(
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
            loadOthelloTexture(context.gamePackage.assetsDir.resolve("textures/othello-shelf.png"))
        }
        val initialRound = remember { newOthelloRound() }
        var state by remember { mutableStateOf(initialRound.state) }
        var turn by remember { mutableStateOf(initialRound.turn) }
        var playerDisc by remember { mutableStateOf(initialRound.playerDisc) }
        var score by remember { mutableStateOf(OthelloScore()) }
        var history by remember { mutableStateOf(emptyList<OthelloSnapshot>()) }

        fun advanceTwoPlayer(nextState: OthelloState, nextTurn: Disc): OthelloTurnState {
            val legalForNext = OthelloRules.legalMoves(nextState, nextTurn)
            val resolvedTurn = if (legalForNext.isEmpty() && !OthelloRules.isGameOver(nextState)) {
                nextTurn.other()
            } else {
                nextTurn
            }
            return OthelloTurnState(nextState, resolvedTurn)
        }

        fun play(row: Int, col: Int) {
            if (OthelloRules.isGameOver(state)) return
            if (mode == GameMode.SINGLE_PLAYER && turn != playerDisc) return
            val move = OthelloMove(row, col)
            if (move !in OthelloRules.legalMoves(state, turn)) return
            history = history + OthelloSnapshot(state, turn, score)
            val nextState = OthelloRules.applyMove(state, move, turn)
            val advanced = if (mode == GameMode.SINGLE_PLAYER) {
                advanceOthelloSinglePlayer(nextState, turn.other(), playerDisc)
            } else {
                advanceTwoPlayer(nextState, turn.other())
            }
            state = advanced.state
            turn = advanced.turn
            if (OthelloRules.isGameOver(state)) {
                score = score.record(OthelloRules.winner(state))
            }
        }

        fun undo() {
            val undo = undoOthello(history) ?: return
            state = undo.snapshot.state
            turn = undo.snapshot.turn
            score = undo.snapshot.score
            history = undo.remainingHistory
        }

        fun restart() {
            val nextPlayerDisc = if (mode == GameMode.SINGLE_PLAYER) {
                nextOthelloPlayerDisc(playerDisc, OthelloRules.winner(state))
            } else {
                Disc.BLACK
            }
            val round = newOthelloRound(nextPlayerDisc)
            state = round.state
            turn = round.turn
            playerDisc = round.playerDisc
            history = emptyList()
        }

        val gameOver = OthelloRules.isGameOver(state)
        Surface(Modifier.fillMaxSize(), color = OthelloPaper) {
            OthelloGameLayout(
                state = state,
                turn = turn,
                status = statusText(state, turn, playerDisc, mode),
                score = score.displayText,
                gameOver = gameOver,
                canUndo = history.isNotEmpty(),
                showUndo = shouldShowOthelloUndo(gameOver, OthelloRules.winner(state)),
                showLegalMoveHints = showOthelloLegalMoveHints(mode),
                onPlay = ::play,
                onUndo = ::undo,
                onRestart = ::restart,
                onExit = context::exitGame,
                texture = texture
            )
        }
    }

    private fun statusText(
        state: OthelloState,
        turn: Disc,
        playerDisc: Disc,
        mode: GameMode
    ): String {
        if (OthelloRules.isGameOver(state)) {
            return when (OthelloRules.winner(state)) {
                Disc.BLACK -> "黑方胜"
                Disc.WHITE -> "白方胜"
                null -> "平局"
            }
        }
        return if (mode == GameMode.SINGLE_PLAYER) {
            "你的回合 · 执${if (playerDisc == Disc.BLACK) "黑" else "白"}"
        } else {
            "当前回合：${if (turn == Disc.BLACK) "黑方" else "白方"}"
        }
    }

    companion object {
        val manifest = GameManifest(
            gameId = "othello",
            displayName = "黑白棋",
            versionCode = 2,
            versionName = "0.0.2",
            entryClass = "com.buddygames.othello.OthelloPlugin",
            minShellApi = 1,
            icon = "assets/icon.txt"
        )
    }
}

private fun loadOthelloTexture(file: File): ImageBitmap? = runCatching {
    requireNotNull(BitmapFactory.decodeFile(file.absolutePath)).asImageBitmap()
}.getOrNull()
