package com.buddygames.xiangqi

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.buddygames.api.GameContext
import com.buddygames.api.GameManifest
import com.buddygames.api.GameMode
import com.buddygames.api.GamePlugin

private val XiangqiPaper = Color(0xFFF1E7D5)
private val XiangqiBoard = Color(0xFFD2AC73)
private val XiangqiWood = Color(0xFF745936)
private val XiangqiInk = Color(0xFF2B2722)
private val XiangqiIvory = Color(0xFFFFFC)
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

class XiangqiPlugin : GamePlugin {
    override fun getManifest(): GameManifest = manifest

    @Composable
    override fun MainScreen(context: GameContext) {
        XiangqiMenu(
            onSingle = { context.startGame(GameMode.SINGLE_PLAYER) },
            onTwo = { context.startGame(GameMode.TWO_PLAYERS) },
            onExit = context::exitGame
        )
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

        Surface(Modifier.fillMaxSize(), color = XiangqiPaper) {
            XiangqiGameLayout(
                state = state,
                selected = selected,
                status = statusText(winner, turn, mode),
                onTap = ::tap,
                onExit = context::exitGame
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

@Composable
private fun XiangqiMenu(onSingle: () -> Unit, onTwo: () -> Unit, onExit: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = XiangqiPaper) {
        Column(
            modifier = Modifier.fillMaxSize().padding(48.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            XiangqiMark()
            Spacer(Modifier.height(20.dp))
            Text(
                "象棋",
                color = XiangqiInk,
                style = MaterialTheme.typography.displayMedium,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(30.dp))
            XiangqiMenuButton("单人模式", XiangqiVermilion, Color.White, onSingle)
            Spacer(Modifier.height(12.dp))
            XiangqiMenuButton("双人对战", XiangqiIvory, XiangqiInk, onTwo, true)
            Spacer(Modifier.height(12.dp))
            XiangqiMenuButton("退出游戏", Color(0xFFF1DDDD), XiangqiVermilion, onExit, true)
        }
    }
}

@Composable
private fun XiangqiMark() {
    Box(
        modifier = Modifier
            .size(92.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(XiangqiBoard)
            .border(7.dp, XiangqiWood, RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text("楚河", color = XiangqiVermilion, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun XiangqiMenuButton(
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
private fun XiangqiGameLayout(
    state: XiangqiState,
    selected: Pair<Int, Int>?,
    status: String,
    onTap: (Int, Int) -> Unit,
    onExit: () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize().padding(32.dp)) {
        if (maxWidth >= 900.dp) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                XiangqiBoard(state, selected, onTap, Modifier.weight(1f))
                Spacer(Modifier.width(32.dp))
                XiangqiInfoRail(status, onExit, Modifier.width(220.dp).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                XiangqiBoard(state, selected, onTap, Modifier.weight(1f))
                XiangqiInfoRail(status, onExit, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun XiangqiBoard(
    state: XiangqiState,
    selected: Pair<Int, Int>?,
    onTap: (Int, Int) -> Unit,
    modifier: Modifier
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val lineWidth = minOf(
            (maxWidth - 28.dp) * 8f / 9f,
            (maxHeight - 28.dp) * 0.8f,
            620.dp
        ).coerceAtLeast(288.dp)
        val stepX = lineWidth / (XiangqiState.COLS - 1).toFloat()
        val lineHeight = stepX * (XiangqiState.ROWS - 1)
        val stepY = lineHeight / (XiangqiState.ROWS - 1).toFloat()
        val surfaceWidth = lineWidth + stepX
        val surfaceHeight = lineHeight + stepY
        Box(
            modifier = Modifier
                .width(surfaceWidth + 28.dp)
                .height(surfaceHeight + 28.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(XiangqiWood)
                .border(2.dp, Color(0xFF594329), RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(surfaceWidth)
                    .height(surfaceHeight)
                    .background(XiangqiBoard)
            ) {
                repeat(XiangqiState.ROWS) { row ->
                    Box(
                        modifier = Modifier
                            .width(lineWidth)
                            .height(1.dp)
                            .offset(
                                x = stepX / 2,
                                y = stepY / 2 + lineHeight * xiangqiRankFraction(row)
                            )
                            .background(XiangqiWood.copy(alpha = 0.86f))
                    )
                }
                repeat(XiangqiState.COLS) { col ->
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(lineHeight)
                            .offset(
                                x = stepX / 2 + lineWidth * xiangqiFileFraction(col),
                                y = stepY / 2
                            )
                            .background(XiangqiWood.copy(alpha = 0.86f))
                    )
                }
                repeat(XiangqiState.ROWS) { row ->
                    repeat(XiangqiState.COLS) { col ->
                        val piece = state.piece(row, col)
                        val isSelected = selected == row to col
                        Box(
                            modifier = Modifier
                                .size(stepX, stepY)
                                .offset(
                                    x = lineWidth * xiangqiFileFraction(col),
                                    y = lineHeight * xiangqiRankFraction(row)
                                )
                                .background(if (isSelected) Color(0x55EBCB7A) else Color.Transparent)
                                .clickable { onTap(row, col) },
                            contentAlignment = Alignment.Center
                        ) {
                            val decoration = when {
                                row == 4 && col == 4 -> "楚河"
                                row == 5 && col == 4 -> "汉界"
                                row in setOf(0, 2, 7, 9) && col in setOf(3, 5) -> "╲"
                                row in setOf(0, 2, 7, 9) && col == 4 -> "╱"
                                else -> ""
                            }
                            if (decoration.isNotEmpty() && piece == null) {
                                Text(
                                    decoration,
                                    color = XiangqiWood.copy(alpha = 0.72f),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Serif
                                )
                            }
                            if (piece != null) {
                                Box(
                                    modifier = Modifier
                                        .size(stepX * 0.78f)
                                        .background(XiangqiIvory, CircleShape)
                                        .border(2.dp, XiangqiGold, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        piece.label(),
                                        color = if (piece.side == Side.RED) XiangqiVermilion else XiangqiInk,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun XiangqiInfoRail(status: String, onExit: () -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(XiangqiIvory)
            .border(1.dp, Color(0xFFD8C4A0), RoundedCornerShape(14.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("历史比分", color = XiangqiWood, style = MaterialTheme.typography.labelLarge)
            Text("0 : 0", color = XiangqiInk, style = MaterialTheme.typography.headlineMedium, fontFamily = FontFamily.Serif)
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("当前回合", color = XiangqiWood, style = MaterialTheme.typography.labelLarge)
            Text(status, color = XiangqiInk, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = onExit,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1DDDD), contentColor = XiangqiVermilion)
        ) {
            Text("退出游戏", fontWeight = FontWeight.Bold)
        }
    }
}
