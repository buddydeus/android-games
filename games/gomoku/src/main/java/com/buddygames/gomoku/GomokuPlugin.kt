package com.buddygames.gomoku

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.buddygames.api.GameContext
import com.buddygames.api.GameManifest
import com.buddygames.api.GameMode
import com.buddygames.api.GamePlugin

class GomokuPlugin : GamePlugin {
    override fun getManifest(): GameManifest = manifest

    @Composable
    override fun MainScreen(context: GameContext) {
        GameMenu(
            title = "五子棋",
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

        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("五子棋", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(statusText(winner, turn, mode), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Column(Modifier.verticalScroll(rememberScrollState())) {
                repeat(GomokuState.SIZE) { row ->
                    Row {
                        repeat(GomokuState.SIZE) { col ->
                            val stone = state.cell(row, col)
                            Text(
                                text = when (stone) {
                                    Stone.BLACK -> "●"
                                    Stone.WHITE -> "○"
                                    null -> ""
                                },
                                modifier = Modifier
                                    .size(30.dp)
                                    .border(1.dp, Color(0xFF9A6A2F))
                                    .background(Color(0xFFF4C979))
                                    .clickable { play(row, col) },
                                color = if (stone == Stone.BLACK) Color.Black else Color.White
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {
                    state = GomokuState.empty()
                    turn = Stone.BLACK
                    winner = null
                }) { Text("重新开始") }
                Button(onClick = context::returnToGameMain) { Text("返回") }
            }
        }
    }

    private fun statusText(winner: Stone?, turn: Stone, mode: GameMode): String {
        winner?.let { return if (it == Stone.BLACK) "黑方胜" else "白方胜" }
        return if (mode == GameMode.SINGLE_PLAYER) "单人游戏：你执黑" else "当前回合：${if (turn == Stone.BLACK) "黑方" else "白方"}"
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
private fun GameMenu(title: String, onSingle: () -> Unit, onTwo: () -> Unit, onExit: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(48.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onSingle) { Text("单人游戏") }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onTwo) { Text("2人游戏") }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onExit) { Text("退出") }
    }
}

