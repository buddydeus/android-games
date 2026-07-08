package com.buddygames.xiangqi

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

class XiangqiPlugin : GamePlugin {
    override fun getManifest(): GameManifest = manifest

    @Composable
    override fun MainScreen(context: GameContext) {
        Column(
            modifier = Modifier.fillMaxSize().padding(48.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("象棋", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
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

        Column(Modifier.fillMaxSize().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("象棋", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(statusText(winner, turn, mode), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            repeat(XiangqiState.ROWS) { row ->
                Row {
                    repeat(XiangqiState.COLS) { col ->
                        val piece = state.piece(row, col)
                        val isSelected = selected == row to col
                        Text(
                            text = piece?.label().orEmpty(),
                            modifier = Modifier
                                .size(48.dp)
                                .border(1.dp, Color(0xFF8B5E34))
                                .background(if (isSelected) Color(0xFFFFD166) else Color(0xFFF2D39B))
                                .clickable { tap(row, col) },
                            color = if (piece?.side == Side.RED) Color(0xFFC62828) else Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {
                    state = XiangqiState.initial()
                    turn = Side.RED
                    selected = null
                    winner = null
                }) { Text("重新开始") }
                Button(onClick = context::returnToGameMain) { Text("返回") }
            }
        }
    }

    private fun statusText(winner: Side?, turn: Side, mode: GameMode): String {
        winner?.let { return if (it == Side.RED) "红方胜" else "黑方胜" }
        return if (mode == GameMode.SINGLE_PLAYER) "单人游戏：你执红" else "当前回合：${if (turn == Side.RED) "红方" else "黑方"}"
    }

    private fun XiangqiPiece.label(): String {
        return when (type) {
            PieceType.GENERAL -> if (side == Side.RED) "帅" else "将"
            PieceType.ROOK -> "车"
            PieceType.HORSE -> "马"
            PieceType.CANNON -> "炮"
            PieceType.ELEPHANT -> if (side == Side.RED) "相" else "象"
            PieceType.ADVISOR -> if (side == Side.RED) "仕" else "士"
            PieceType.SOLDIER -> if (side == Side.RED) "兵" else "卒"
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

