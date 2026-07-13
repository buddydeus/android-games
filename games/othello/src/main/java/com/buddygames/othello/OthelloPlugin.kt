package com.buddygames.othello

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.buddygames.api.GameContext
import com.buddygames.api.GameManifest
import com.buddygames.api.GameMode
import com.buddygames.api.GamePlugin

private val OthelloPaper = Color(0xFFEAF0E8)
private val OthelloGreen = Color(0xFF2F6F56)
private val OthelloBoard = Color(0xFF3F815F)
private val OthelloDarkWood = Color(0xFF493927)
private val OthelloIvory = Color(0xFFFFFC)
private val OthelloInk = Color(0xFF1F2A2D)
private val OthelloVermilion = Color(0xFF9B2F2F)

class OthelloPlugin : GamePlugin {
    override fun getManifest(): GameManifest = manifest

    @Composable
    override fun MainScreen(context: GameContext) {
        OthelloMenu(
            onSingle = { context.startGame(GameMode.SINGLE_PLAYER) },
            onTwo = { context.startGame(GameMode.TWO_PLAYERS) },
            onExit = context::exitGame
        )
    }

    @Composable
    override fun GameScreen(context: GameContext, mode: GameMode) {
        var state by remember { mutableStateOf(OthelloState.initial()) }
        var turn by remember { mutableStateOf(Disc.BLACK) }

        fun advanceAfterMove(nextState: OthelloState, nextTurn: Disc): Pair<OthelloState, Disc> {
            if (mode == GameMode.SINGLE_PLAYER && nextTurn == Disc.WHITE && !OthelloRules.isGameOver(nextState)) {
                val robot = OthelloRules.robotMove(nextState, Disc.WHITE)
                val robotState = if (robot == null) nextState else OthelloRules.applyMove(nextState, robot, Disc.WHITE)
                return robotState to Disc.BLACK
            }
            val legalForNext = OthelloRules.legalMoves(nextState, nextTurn)
            return if (legalForNext.isEmpty() && !OthelloRules.isGameOver(nextState)) nextState to nextTurn.other() else nextState to nextTurn
        }

        fun play(row: Int, col: Int) {
            if (OthelloRules.isGameOver(state)) return
            val move = OthelloMove(row, col)
            if (move !in OthelloRules.legalMoves(state, turn)) return
            val nextState = OthelloRules.applyMove(state, move, turn)
            val advanced = advanceAfterMove(nextState, turn.other())
            state = advanced.first
            turn = advanced.second
        }

        Surface(Modifier.fillMaxSize(), color = OthelloPaper) {
            OthelloGameLayout(
                state = state,
                status = statusText(state, turn, mode),
                onPlay = ::play,
                onExit = context::exitGame
            )
        }
    }

    private fun statusText(state: OthelloState, turn: Disc, mode: GameMode): String {
        if (OthelloRules.isGameOver(state)) {
            return when (OthelloRules.winner(state)) {
                Disc.BLACK -> "黑方胜"
                Disc.WHITE -> "白方胜"
                null -> "平局"
            }
        }
        return if (mode == GameMode.SINGLE_PLAYER) {
            "你的回合 · 执黑"
        } else {
            "当前回合：${if (turn == Disc.BLACK) "黑方" else "白方"}"
        }
    }

    companion object {
        val manifest = GameManifest(
            gameId = "othello",
            displayName = "黑白棋",
            versionCode = 1,
            versionName = "1.0.0",
            entryClass = "com.buddygames.othello.OthelloPlugin",
            minShellApi = 1,
            icon = "assets/icon.txt"
        )
    }
}

@Composable
private fun OthelloMenu(onSingle: () -> Unit, onTwo: () -> Unit, onExit: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = OthelloPaper) {
        Column(
            modifier = Modifier.fillMaxSize().padding(48.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OthelloMark()
            Spacer(Modifier.height(20.dp))
            Text(
                "黑白棋",
                color = OthelloInk,
                style = MaterialTheme.typography.displayMedium,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(30.dp))
            OthelloMenuButton("单人模式", OthelloGreen, Color.White, onSingle)
            Spacer(Modifier.height(12.dp))
            OthelloMenuButton("双人对战", OthelloIvory, OthelloInk, onTwo, true)
            Spacer(Modifier.height(12.dp))
            OthelloMenuButton("退出游戏", Color(0xFFF1DDDD), OthelloVermilion, onExit, true)
        }
    }
}

@Composable
private fun OthelloMark() {
    Box(
        modifier = Modifier
            .size(92.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(OthelloBoard)
            .border(7.dp, OthelloDarkWood, RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(25.dp).background(Color.Black, CircleShape))
            Box(
                Modifier
                    .size(25.dp)
                    .background(Color.White, CircleShape)
                    .border(1.dp, Color(0xFFC7C3B9), CircleShape)
            )
        }
    }
}

@Composable
private fun OthelloMenuButton(
    label: String,
    container: Color,
    content: Color,
    onClick: () -> Unit,
    outlined: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = Modifier.width(260.dp).height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
        border = if (outlined) androidx.compose.foundation.BorderStroke(1.dp, content.copy(alpha = 0.28f)) else null
    ) {
        Text(label, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun OthelloGameLayout(
    state: OthelloState,
    status: String,
    onPlay: (Int, Int) -> Unit,
    onExit: () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize().padding(32.dp)) {
        if (maxWidth >= 900.dp) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                OthelloBoard(state, onPlay, Modifier.weight(1f))
                Spacer(Modifier.width(32.dp))
                OthelloInfoRail(status, onExit, Modifier.width(220.dp).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                OthelloBoard(state, onPlay, Modifier.weight(1f))
                OthelloInfoRail(status, onExit, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun OthelloBoard(state: OthelloState, onPlay: (Int, Int) -> Unit, modifier: Modifier) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val cellSize = (maxWidth / 8f).coerceIn(44.dp, 72.dp)
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(OthelloDarkWood)
                .border(2.dp, Color(0xFF2F241A), RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            repeat(OthelloState.SIZE) { row ->
                Row {
                    repeat(OthelloState.SIZE) { col ->
                        val disc = state.cell(row, col)
                        Box(
                            modifier = Modifier
                                .size(cellSize)
                                .border(0.8.dp, Color(0xFF24513F))
                                .background(OthelloBoard)
                                .clickable { onPlay(row, col) },
                            contentAlignment = Alignment.Center
                        ) {
                            when (disc) {
                                Disc.BLACK -> Box(Modifier.size(cellSize * 0.72f).background(Color(0xFF141A19), CircleShape))
                                Disc.WHITE -> Box(
                                    Modifier
                                        .size(cellSize * 0.72f)
                                        .background(Color.White, CircleShape)
                                        .border(1.dp, Color(0xFFC7C3B9), CircleShape)
                                )
                                null -> Unit
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OthelloInfoRail(status: String, onExit: () -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(OthelloIvory)
            .border(1.dp, Color(0xFFD2D8CF), RoundedCornerShape(14.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("历史比分", color = OthelloGreen, style = MaterialTheme.typography.labelLarge)
            Text("0 : 0", color = OthelloInk, style = MaterialTheme.typography.headlineMedium, fontFamily = FontFamily.Serif)
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("当前回合", color = OthelloGreen, style = MaterialTheme.typography.labelLarge)
            Text(status, color = OthelloInk, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = onExit,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1DDDD), contentColor = OthelloVermilion)
        ) {
            Text("退出游戏", fontWeight = FontWeight.Bold)
        }
    }
}

