package com.buddygames.othello

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
private val Green = Color(0xFF174F3D)
private val Vermilion = Color(0xFFA2362B)
private val Ivory = Color(0xFFF8F8F4)

@Composable
internal fun OthelloMenu(texture: ImageBitmap?, onSingle: () -> Unit, onTwo: () -> Unit, onExit: () -> Unit) {
    GameBackdrop {
        BoxWithConstraints(Modifier.fillMaxSize().padding(28.dp)) {
            if (maxWidth >= 900.dp) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    OthelloBoard(OthelloState.initial(), Disc.BLACK, { _, _ -> }, Modifier.weight(1f), texture, interactive = false)
                    Spacer(Modifier.width(34.dp))
                    OthelloMenuPanel(onSingle, onTwo, onExit, Modifier.width(310.dp))
                }
            } else {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    OthelloBoard(OthelloState.initial(), Disc.BLACK, { _, _ -> }, Modifier.weight(1f), texture, interactive = false)
                    OthelloMenuPanel(onSingle, onTwo, onExit, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun OthelloMenuPanel(onSingle: () -> Unit, onTwo: () -> Unit, onExit: () -> Unit, modifier: Modifier) {
    MaterialPanel(modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("黑白棋", color = Ink, fontFamily = FontFamily.Serif, fontSize = 44.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("八路 · 反转棋", color = Green, fontSize = 15.sp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MenuButton("开始单人对局", Teal, Color.White, onSingle)
            MenuButton("开始双人对局", Ivory, Ink, onTwo, outlined = true)
            MenuButton("退出游戏", Color.Transparent, Vermilion, onExit, outlined = true)
        }
    }
}

@Composable
internal fun OthelloGameLayout(
    state: OthelloState,
    turn: Disc,
    status: String,
    onPlay: (Int, Int) -> Unit,
    onExit: () -> Unit,
    texture: ImageBitmap?
) {
    GameBackdrop {
        BoxWithConstraints(Modifier.fillMaxSize().padding(28.dp)) {
            if (maxWidth >= 900.dp) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    OthelloBoard(state, turn, onPlay, Modifier.weight(1f), texture)
                    Spacer(Modifier.width(34.dp))
                    OthelloInfoRail(state, status, onExit, Modifier.width(300.dp).fillMaxHeight(0.88f))
                }
            } else {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    OthelloBoard(state, turn, onPlay, Modifier.weight(1f), texture)
                    OthelloInfoRail(state, status, onExit, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun OthelloBoard(
    state: OthelloState,
    turn: Disc,
    onPlay: (Int, Int) -> Unit,
    modifier: Modifier,
    texture: ImageBitmap?,
    interactive: Boolean = true
) {
    val legalMoves = remember(state, turn, interactive) {
        if (interactive) OthelloRules.legalMoves(state, turn).toSet() else emptySet()
    }
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val boardSide = othelloBoardSide(maxWidth.value - 8f, maxHeight.value - 8f).dp
        val surfaceSide = boardSide - 54.dp
        val cellSize = surfaceSide / OthelloState.SIZE.toFloat()
        Box(
            Modifier
                .size(boardSide)
                .shadow(20.dp, RoundedCornerShape(8.dp), clip = false)
                .clip(RoundedCornerShape(8.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF5C4434), Color(0xFF211714))))
                .border(2.dp, Color(0xFF160F0D), RoundedCornerShape(8.dp))
                .padding(15.dp)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .border(4.dp, Color(0xFF140F0D))
                    .background(Color(0xFF2E201A))
                    .padding(8.dp)
            ) {
                Box(Modifier.size(surfaceSide).background(Green)) {
                    if (texture != null) {
                        Image(texture, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = 0.96f)
                    }
                    OthelloGrid(cellSize)
                    Column(Modifier.fillMaxSize()) {
                        repeat(OthelloState.SIZE) { row ->
                            Row {
                                repeat(OthelloState.SIZE) { col ->
                                    val disc = state.cell(row, col)
                                    val legal = OthelloMove(row, col) in legalMoves
                                    Box(
                                        modifier = Modifier
                                            .size(cellSize)
                                            .semantics { contentDescription = "第${row + 1}行第${col + 1}列" }
                                            .clickable(enabled = interactive) { onPlay(row, col) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        when {
                                            disc != null -> DiscPiece(disc, cellSize * 0.76f)
                                            legal -> LegalMoveHint(cellSize * 0.16f)
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
}

@Composable
private fun OthelloGrid(cellSize: Dp) {
    Canvas(Modifier.fillMaxSize()) {
        val cell = cellSize.toPx()
        repeat(OthelloState.SIZE + 1) { index ->
            val position = index * cell
            drawLine(Color(0xB9102821), Offset(0f, position), Offset(size.width, position), 1.5f)
            drawLine(Color(0xB9102821), Offset(position, 0f), Offset(position, size.height), 1.5f)
        }
        listOf(2 to 2, 2 to 6, 6 to 2, 6 to 6).forEach { (row, col) ->
            drawCircle(Color(0xFF0B1714), radius = cell * 0.055f, center = Offset(col * cell, row * cell))
        }
    }
}

@Composable
private fun DiscPiece(disc: Disc, diameter: Dp) {
    Canvas(Modifier.size(diameter)) {
        val radius = size.minDimension * 0.41f
        val center = Offset(size.width / 2f, size.height / 2f - radius * 0.04f)
        drawCircle(Color(0x66000000), radius, center + Offset(radius * 0.11f, radius * 0.16f))
        val colors = if (disc == Disc.BLACK) {
            listOf(Color(0xFF686B6B), Color(0xFF292B2C), Color(0xFF0B0C0D))
        } else {
            listOf(Color(0xFFFFFFFF), Color(0xFFEDEDE6), Color(0xFFB8BAB3))
        }
        drawCircle(
            brush = Brush.radialGradient(colors, center = center - Offset(radius * 0.34f, radius * 0.40f), radius = radius * 1.45f),
            radius = radius,
            center = center
        )
        drawCircle(if (disc == Disc.BLACK) Color(0xFF090A0B) else Color(0xFFA5A8A2), radius, center, style = Stroke(1.15f))
        drawCircle(Color.White.copy(alpha = if (disc == Disc.BLACK) 0.16f else 0.42f), radius * 0.16f, center - Offset(radius * 0.35f, radius * 0.39f))
    }
}

@Composable
private fun LegalMoveHint(size: Dp) {
    Box(Modifier.size(size).background(Color(0x99E7D6A8), CircleShape).border(1.dp, Color(0xFFEEE4C8), CircleShape))
}

@Composable
private fun OthelloInfoRail(state: OthelloState, status: String, onExit: () -> Unit, modifier: Modifier) {
    MaterialPanel(modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("历史比分", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(14.dp))
            Text("0 : 0", color = Ink, fontSize = 48.sp, fontWeight = FontWeight.Light)
        }
        HorizontalDivider(color = Color(0xFFB9C0BD))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            DiscCount(Disc.BLACK, state.count(Disc.BLACK))
            DiscCount(Disc.WHITE, state.count(Disc.WHITE))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("当前回合", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Text(status, color = Vermilion, fontSize = 19.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
        HorizontalDivider(color = Color(0xFFB9C0BD))
        MenuButton("退出游戏", Color.Transparent, Ink, onExit, outlined = true)
    }
}

@Composable
private fun DiscCount(disc: Disc, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DiscPiece(disc, 25.dp)
        Text(count.toString(), color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(PaperTop, PaperBottom)))) { content() }
}
