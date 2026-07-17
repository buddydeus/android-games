package com.buddygames.gomoku

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PaperTop = Color(0xFFF4F7F5)
private val PaperBottom = Color(0xFFDCE7E7)
private val Ink = Color(0xFF202A2B)
private val Teal = Color(0xFF123F48)
private val Vermilion = Color(0xFFA2362B)
private val Walnut = Color(0xFF55371F)
private val Maple = Color(0xFFD8A35F)
private val Ivory = Color(0xFFF8F8F4)

@Composable
internal fun GomokuMenu(
    texture: ImageBitmap?,
    onSingle: () -> Unit,
    onTwo: () -> Unit,
    onExit: () -> Unit
) {
    val preview = remember {
        GomokuState.empty()
            .place(7, 7, Stone.BLACK)
            .place(6, 7, Stone.WHITE)
            .place(8, 8, Stone.BLACK)
            .place(7, 8, Stone.WHITE)
            .place(8, 6, Stone.BLACK)
            .place(6, 6, Stone.WHITE)
            .place(9, 7, Stone.BLACK)
    }
    GameBackdrop {
        BoxWithConstraints(Modifier.fillMaxSize().padding(28.dp)) {
            if (maxWidth >= 900.dp) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    GomokuBoard(preview, { _, _ -> }, Modifier.weight(1f), texture, interactive = false)
                    Spacer(Modifier.width(34.dp))
                    GomokuMenuPanel(onSingle, onTwo, onExit, Modifier.width(310.dp))
                }
            } else {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    GomokuBoard(preview, { _, _ -> }, Modifier.weight(1f), texture, interactive = false)
                    GomokuMenuPanel(onSingle, onTwo, onExit, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun GomokuMenuPanel(
    onSingle: () -> Unit,
    onTwo: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier
) {
    val labels = gomokuMenuLabels()
    MaterialPanel(modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("五子棋", color = Ink, fontFamily = FontFamily.Serif, fontSize = 44.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("十五路 · 连珠", color = Walnut, fontSize = 15.sp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MenuButton(labels[0], Teal, Color.White, onSingle)
            MenuButton(labels[1], Ivory, Ink, onTwo, outlined = true)
            MenuButton(labels[2], Color.Transparent, Vermilion, onExit, outlined = true)
        }
    }
}

internal fun gomokuMenuLabels(): List<String> = listOf("单人模式", "双人对战", "退出游戏")

@Composable
internal fun GomokuGameLayout(
    state: GomokuState,
    status: String,
    score: String,
    gameOver: Boolean,
    onPlay: (Int, Int) -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit,
    texture: ImageBitmap?
) {
    GameBackdrop {
        BoxWithConstraints(Modifier.fillMaxSize().padding(28.dp)) {
            if (maxWidth >= 900.dp) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    GomokuBoard(state, onPlay, Modifier.weight(1f), texture)
                    Spacer(Modifier.width(34.dp))
                    GomokuInfoRail(score, status, gameOver, onRestart, onExit, Modifier.width(300.dp).fillMaxHeight(0.88f))
                }
            } else {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    GomokuBoard(state, onPlay, Modifier.weight(1f), texture)
                    GomokuInfoRail(score, status, gameOver, onRestart, onExit, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun GomokuBoard(
    state: GomokuState,
    onPlay: (Int, Int) -> Unit,
    modifier: Modifier,
    texture: ImageBitmap?,
    interactive: Boolean = true
) {
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val boardSide = gomokuBoardSide(maxWidth.value - 8f, maxHeight.value - 8f).dp
        val surfaceSide = boardSide - 48.dp
        val step = surfaceSide / GomokuState.SIZE.toFloat()

        Box(
            Modifier
                .size(boardSide)
                .shadow(18.dp, RoundedCornerShape(7.dp), clip = false)
                .clip(RoundedCornerShape(7.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF7A5432), Color(0xFF3B2516))))
                .border(2.dp, Color(0xFF2D1C10), RoundedCornerShape(7.dp))
                .padding(15.dp)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .border(3.dp, Color(0xFF2F1C0F))
                    .background(Color(0xFF9A693B))
                    .padding(9.dp)
            ) {
                Box(Modifier.size(surfaceSide).background(Maple)) {
                    if (texture != null) {
                        Image(texture, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = 0.94f)
                    }
                    GomokuGrid(step)
                    Column(Modifier.fillMaxSize()) {
                        repeat(GomokuState.SIZE) { row ->
                            Row {
                                repeat(GomokuState.SIZE) { col ->
                                    val stone = state.cell(row, col)
                                    Box(
                                        modifier = Modifier
                                            .size(step)
                                            .semantics { contentDescription = "第${row + 1}行第${col + 1}列" }
                                            .clickable(enabled = interactive) { onPlay(row, col) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (stone != null) GomokuStone(stone, step * 0.86f)
                                    }
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
private fun GomokuGrid(step: Dp) {
    Canvas(Modifier.fillMaxSize()) {
        val stepPx = step.toPx()
        val start = stepPx / 2f
        val end = size.width - start
        repeat(GomokuState.SIZE) { index ->
            val position = start + index * stepPx
            drawLine(Color(0xB82C2116), Offset(start, position), Offset(end, position), 1.25f)
            drawLine(Color(0xB82C2116), Offset(position, start), Offset(position, end), 1.25f)
        }
        listOf(3 to 3, 3 to 11, 7 to 7, 11 to 3, 11 to 11).forEach { (row, col) ->
            drawCircle(Color(0xFF342719), radius = stepPx * 0.09f, center = Offset(start + col * stepPx, start + row * stepPx))
        }
    }
}

@Composable
private fun GomokuStone(stone: Stone, diameter: Dp) {
    Canvas(Modifier.size(diameter)) {
        val radius = size.minDimension * 0.41f
        val center = Offset(size.width / 2f, size.height / 2f - radius * 0.04f)
        drawCircle(Color(0x55000000), radius, center + Offset(radius * 0.10f, radius * 0.15f))
        val colors = if (stone == Stone.BLACK) {
            listOf(Color(0xFF6A716E), Color(0xFF252B2B), Color(0xFF070909))
        } else {
            listOf(Color(0xFFFFFFFF), Color(0xFFF0F0E6), Color(0xFFBFC2B8))
        }
        drawCircle(
            brush = Brush.radialGradient(colors, center = center - Offset(radius * 0.35f, radius * 0.40f), radius = radius * 1.45f),
            radius = radius,
            center = center
        )
        drawCircle(if (stone == Stone.BLACK) Color(0xFF080B0B) else Color(0xFFABAEA5), radius, center, style = Stroke(1.1f))
        drawCircle(Color.White.copy(alpha = if (stone == Stone.BLACK) 0.18f else 0.42f), radius * 0.18f, center - Offset(radius * 0.36f, radius * 0.38f))
    }
}

@Composable
private fun GomokuInfoRail(
    score: String,
    status: String,
    gameOver: Boolean,
    onRestart: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier
) {
    MaterialPanel(modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("历史比分", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(14.dp))
            Text(score, color = Ink, fontSize = 48.sp, fontWeight = FontWeight.Light)
        }
        HorizontalDivider(color = Color(0xFFB9C0BD))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (gameOver) "对局结果" else "当前回合", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(14.dp))
            Text(status, color = Vermilion, fontSize = 19.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
        HorizontalDivider(color = Color(0xFFB9C0BD))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (gameOver) MenuButton("重新开始", Teal, Color.White, onRestart)
            MenuButton("退出游戏", Color.Transparent, Ink, onExit, outlined = true)
        }
    }
}

@Composable
private fun MaterialPanel(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .shadow(13.dp, RoundedCornerShape(8.dp), clip = false)
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFFFDFEFD), Color(0xFFE9EEEC))))
            .border(1.dp, Color(0xFFBAC3C0), RoundedCornerShape(8.dp))
            .padding(horizontal = 28.dp, vertical = 34.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

@Composable
private fun MenuButton(label: String, container: Color, content: Color, onClick: () -> Unit, outlined: Boolean = false) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(7.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
        border = if (outlined) BorderStroke(1.dp, content.copy(alpha = 0.42f)) else null
    ) { Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun GameBackdrop(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PaperTop, PaperBottom)))
    ) { content() }
}
