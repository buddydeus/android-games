package com.buddygames.gomoku

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

private val GomokuPaper = Color(0xFFF0E6D2)
private val GomokuWood = Color(0xFFBC9561)
private val GomokuDarkWood = Color(0xFF745936)
private val GomokuInk = Color(0xFF2D3438)
private val GomokuIvory = Color(0xFFFFFC)
private val GomokuVermilion = Color(0xFF9B2F2F)

class GomokuPlugin : GamePlugin {
    override fun getManifest(): GameManifest = manifest

    @Composable
    override fun MainScreen(context: GameContext) {
        GomokuMenu(
            onSingle = { context.startGame(GameMode.SINGLE_PLAYER) },
            onTwo = { context.startGame(GameMode.TWO_PLAYERS) },
            onExit = context::exitGame
        )
    }

    @Composable
    override fun GameScreen(context: GameContext, mode: GameMode) {
        var state by remember { mutableStateOf(GomokuState.empty()) }
        var turn by remember { mutableStateOf(Stone.BLACK) }
        var winner by remember { mutableStateOf<Stone?>(null) }

        fun play(row: Int, col: Int) {
            if (winner != null || state.cell(row, col) != null) return
            state = state.place(row, col, turn)
            winner = GomokuRules.winner(state)
            if (winner == null && mode == GameMode.SINGLE_PLAYER) {
                val robot = GomokuRules.robotMove(state, Stone.WHITE)
                state = state.place(robot.row, robot.col, Stone.WHITE)
                winner = GomokuRules.winner(state)
                turn = Stone.BLACK
            } else if (winner == null) {
                turn = turn.other()
            }
        }

        Surface(Modifier.fillMaxSize(), color = GomokuPaper) {
            GomokuGameLayout(
                state = state,
                status = statusText(winner, turn, mode),
                onPlay = ::play,
                onExit = context::exitGame
            )
        }
    }

    private fun statusText(winner: Stone?, turn: Stone, mode: GameMode): String {
        winner?.let { return if (it == Stone.BLACK) "黑方胜" else "白方胜" }
        return if (mode == GameMode.SINGLE_PLAYER) {
            "你的回合 · 执黑"
        } else {
            "当前回合：${if (turn == Stone.BLACK) "黑方" else "白方"}"
        }
    }

    companion object {
        val manifest = GameManifest(
            gameId = "gomoku",
            displayName = "五子棋",
            versionCode = 1,
            versionName = "1.0.0",
            entryClass = "com.buddygames.gomoku.GomokuPlugin",
            minShellApi = 1,
            icon = "assets/icon.txt"
        )
    }
}

@Composable
private fun GomokuMenu(onSingle: () -> Unit, onTwo: () -> Unit, onExit: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = GomokuPaper) {
        Column(
            modifier = Modifier.fillMaxSize().padding(48.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GomokuMark(92.dp)
            Spacer(Modifier.height(20.dp))
            Text(
                "五子棋",
                color = GomokuInk,
                style = MaterialTheme.typography.displayMedium,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(30.dp))
            GomokuMenuButton("单人模式", GomokuInk, Color.White, onSingle)
            Spacer(Modifier.height(12.dp))
            GomokuMenuButton("双人对战", GomokuIvory, GomokuInk, onTwo, outlined = true)
            Spacer(Modifier.height(12.dp))
            GomokuMenuButton("退出游戏", Color(0xFFF1DDDD), GomokuVermilion, onExit, outlined = true)
        }
    }
}

@Composable
private fun GomokuMark(size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(18.dp))
            .background(GomokuWood)
            .border(7.dp, GomokuDarkWood, RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(24.dp).background(GomokuInk, CircleShape))
            Box(
                Modifier
                    .size(24.dp)
                    .background(Color.White, CircleShape)
                    .border(1.dp, Color(0xFFC7C3B9), CircleShape)
            )
        }
    }
}

@Composable
private fun GomokuMenuButton(
    label: String,
    container: Color,
    content: Color,
    onClick: () -> Unit,
    outlined: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(260.dp)
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
        border = if (outlined) androidx.compose.foundation.BorderStroke(1.dp, content.copy(alpha = 0.28f)) else null
    ) {
        Text(label, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun GomokuGameLayout(
    state: GomokuState,
    status: String,
    onPlay: (Int, Int) -> Unit,
    onExit: () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize().padding(32.dp)) {
        if (maxWidth >= 900.dp) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                GomokuBoard(
                    state = state,
                    onPlay = onPlay,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(32.dp))
                GomokuInfoRail(status, onExit, Modifier.width(220.dp).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                GomokuBoard(
                    state = state,
                    onPlay = onPlay,
                    modifier = Modifier.weight(1f)
                )
                GomokuInfoRail(status, onExit, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun GomokuBoard(
    state: GomokuState,
    onPlay: (Int, Int) -> Unit,
    modifier: Modifier
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val cellSize = (maxWidth / 15f).coerceIn(20.dp, 34.dp)
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(GomokuDarkWood)
                .border(2.dp, Color(0xFF594329), RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            repeat(GomokuState.SIZE) { row ->
                Row {
                    repeat(GomokuState.SIZE) { col ->
                        val stone = state.cell(row, col)
                        Box(
                            modifier = Modifier
                                .size(cellSize)
                                .border(0.7.dp, GomokuDarkWood.copy(alpha = 0.8f))
                                .background(GomokuWood)
                                .clickable { onPlay(row, col) },
                            contentAlignment = Alignment.Center
                        ) {
                            when (stone) {
                                Stone.BLACK -> Box(Modifier.size(cellSize * 0.72f).background(GomokuInk, CircleShape))
                                Stone.WHITE -> Box(
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
private fun GomokuInfoRail(status: String, onExit: () -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(GomokuIvory)
            .border(1.dp, Color(0xFFD4C5A8), RoundedCornerShape(14.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("历史比分", color = GomokuDarkWood, style = MaterialTheme.typography.labelLarge)
            Text("0 : 0", color = GomokuInk, style = MaterialTheme.typography.headlineMedium, fontFamily = FontFamily.Serif)
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("当前回合", color = GomokuDarkWood, style = MaterialTheme.typography.labelLarge)
            Text(status, color = GomokuInk, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = onExit,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1DDDD), contentColor = GomokuVermilion)
        ) {
            Text("退出游戏", fontWeight = FontWeight.Bold)
        }
    }
}
