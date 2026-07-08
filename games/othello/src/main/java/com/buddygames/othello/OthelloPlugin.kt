package com.buddygames.othello

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

class OthelloPlugin : GamePlugin {
    override fun getManifest(): GameManifest = manifest

    @Composable
    override fun MainScreen(context: GameContext) {
        Column(
            modifier = Modifier.fillMaxSize().padding(48.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("黑白棋", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(32.dp))
            Button(onClick = { context.startGame(GameMode.SINGLE_PLAYER) }) { Text("单人游戏") }
            Spacer(Modifier.height(12.dp))
            Button(onClick = { context.startGame(GameMode.TWO_PLAYERS) }) { Text("2人游戏") }
            Spacer(Modifier.height(12.dp))
            Button(onClick = context::exitGame) { Text("退出") }
        }
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

        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("黑白棋", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(statusText(state, turn, mode), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            repeat(OthelloState.SIZE) { row ->
                Row {
                    repeat(OthelloState.SIZE) { col ->
                        val disc = state.cell(row, col)
                        Text(
                            text = when (disc) {
                                Disc.BLACK -> "●"
                                Disc.WHITE -> "○"
                                null -> ""
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .border(1.dp, Color(0xFF003D2D))
                                .background(Color(0xFF08784F))
                                .clickable { play(row, col) },
                            color = if (disc == Disc.WHITE) Color.White else Color.Black
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {
                    state = OthelloState.initial()
                    turn = Disc.BLACK
                }) { Text("重新开始") }
                Button(onClick = context::returnToGameMain) { Text("返回") }
            }
        }
    }

    private fun statusText(state: OthelloState, turn: Disc, mode: GameMode): String {
        if (OthelloRules.isGameOver(state)) {
            return when (OthelloRules.winner(state)) {
                Disc.BLACK -> "黑方胜 ${state.count(Disc.BLACK)}:${state.count(Disc.WHITE)}"
                Disc.WHITE -> "白方胜 ${state.count(Disc.WHITE)}:${state.count(Disc.BLACK)}"
                null -> "平局 ${state.count(Disc.BLACK)}:${state.count(Disc.WHITE)}"
            }
        }
        return if (mode == GameMode.SINGLE_PLAYER) {
            "单人游戏：你执黑，黑 ${state.count(Disc.BLACK)} / 白 ${state.count(Disc.WHITE)}"
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

