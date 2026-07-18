package com.buddygames.xiangqi

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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import com.buddygames.api.GameMode

private val PaperTop = Color(0xFFF3F7F6)
private val PaperBottom = Color(0xFFD7E3E4)
private val Ink = Color(0xFF252B2B)
private val Teal = Color(0xFF123F48)
private val Cinnabar = Color(0xFFA7372B)
private val LacquerBlack = Color(0xFF171B1B)
private val Gold = Color(0xFFC79645)
private val PaleGold = Color(0xFFFFD58B)
private val Ivory = Color(0xFFF9FAF7)

@Composable
internal fun XiangqiMenu(
    texture: ImageBitmap?,
    versionName: String,
    onSingle: () -> Unit,
    onTwo: () -> Unit,
    onExit: () -> Unit
) {
    GameBackdrop {
        BoxWithConstraints(Modifier.fillMaxSize().padding(28.dp)) {
            if (maxWidth >= 900.dp) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    XiangqiBoard(XiangqiState.initial(), null, { _, _ -> }, Modifier.weight(1f), texture, false)
                    Spacer(Modifier.width(34.dp))
                    XiangqiMenuPanel(versionName, onSingle, onTwo, onExit, Modifier.width(310.dp))
                }
            } else {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    XiangqiBoard(XiangqiState.initial(), null, { _, _ -> }, Modifier.weight(1f), texture, false)
                    XiangqiMenuPanel(versionName, onSingle, onTwo, onExit, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun XiangqiMenuPanel(
    versionName: String,
    onSingle: () -> Unit,
    onTwo: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier
) {
    val labels = xiangqiMenuLabels()
    MaterialPanel(modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("象棋", color = Ink, fontFamily = FontFamily.Serif, fontSize = 44.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("楚河 · 漢界", color = Cinnabar, fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            Text(xiangqiVersionLabel(versionName), color = Cinnabar, fontSize = 12.sp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MenuButton(labels[0], Cinnabar, Color.White, onSingle)
            MenuButton(labels[1], Ivory, Ink, onTwo, outlined = true)
            MenuButton(labels[2], Color.Transparent, Cinnabar, onExit, outlined = true)
        }
    }
}

internal fun xiangqiMenuLabels(): List<String> = listOf("单人模式", "双人对战", "退出游戏")
internal fun xiangqiVersionLabel(versionName: String): String = "版本 $versionName"
internal fun shouldShowXiangqiUndo(winner: Side?): Boolean = winner == null
internal fun shouldRotateXiangqiBoard(mode: GameMode, playerSide: Side): Boolean =
    mode == GameMode.SINGLE_PLAYER && playerSide == Side.BLACK

internal fun xiangqiBoardCoordinate(row: Int, col: Int, rotated: Boolean): Pair<Int, Int> {
    require(row in 0 until XiangqiState.ROWS)
    require(col in 0 until XiangqiState.COLS)
    return if (rotated) {
        XiangqiState.ROWS - 1 - row to XiangqiState.COLS - 1 - col
    } else {
        row to col
    }
}

internal fun xiangqiRiverLabels(rotated: Boolean): Pair<String, String> =
    if (rotated) "漢  界" to "楚  河" else "楚  河" to "漢  界"

@Composable
internal fun XiangqiGameLayout(
    state: XiangqiState,
    selected: Pair<Int, Int>?,
    status: String,
    turn: Side,
    score: String,
    gameOver: Boolean,
    inCheck: Boolean,
    canUndo: Boolean,
    showUndo: Boolean,
    rotateBoard: Boolean,
    onTap: (Int, Int) -> Unit,
    onUndo: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit,
    texture: ImageBitmap?
) {
    GameBackdrop {
        BoxWithConstraints(Modifier.fillMaxSize().padding(28.dp)) {
            if (maxWidth >= 900.dp) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    XiangqiBoard(
                        state,
                        selected,
                        onTap,
                        Modifier.weight(1f),
                        texture,
                        rotated = rotateBoard
                    )
                    Spacer(Modifier.width(34.dp))
                    XiangqiInfoRail(
                        score,
                        status,
                        turn,
                        gameOver,
                        inCheck,
                        canUndo,
                        showUndo,
                        onUndo,
                        onRestart,
                        onExit,
                        Modifier.width(300.dp).fillMaxHeight(0.88f)
                    )
                }
            } else {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    XiangqiBoard(
                        state,
                        selected,
                        onTap,
                        Modifier.weight(1f),
                        texture,
                        rotated = rotateBoard
                    )
                    XiangqiInfoRail(
                        score,
                        status,
                        turn,
                        gameOver,
                        inCheck,
                        canUndo,
                        showUndo,
                        onUndo,
                        onRestart,
                        onExit,
                        Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun XiangqiBoard(
    state: XiangqiState,
    selected: Pair<Int, Int>?,
    onTap: (Int, Int) -> Unit,
    modifier: Modifier,
    texture: ImageBitmap?,
    interactive: Boolean = true,
    rotated: Boolean = false
) {
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val board = xiangqiBoardSize(maxWidth.value - 8f, maxHeight.value - 8f)
        val boardWidth = board.width.dp
        val boardHeight = board.height.dp
        val surfaceWidth = boardWidth - 48.dp
        val surfaceHeight = boardHeight - 48.dp
        val hitWidth = surfaceWidth - 20.dp
        val hitHeight = surfaceHeight - 16.dp
        val stepX = hitWidth / XiangqiState.COLS.toFloat()
        val stepY = hitHeight / XiangqiState.ROWS.toFloat()

        Box(
            Modifier
                .size(boardWidth, boardHeight)
                .shadow(20.dp, RoundedCornerShape(7.dp), clip = false)
                .clip(RoundedCornerShape(7.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF7A5432), Color(0xFF3A2415))))
                .border(2.dp, Color(0xFF29180E), RoundedCornerShape(7.dp))
                .padding(15.dp)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .border(3.dp, Color(0xFF2B190F))
                    .background(Color(0xFF95633A))
                    .padding(9.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.size(surfaceWidth, surfaceHeight).background(Color(0xFFD3A15E)), contentAlignment = Alignment.Center) {
                    if (texture != null) {
                        Image(texture, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = 0.93f)
                    }
                    Box(Modifier.size(hitWidth, hitHeight)) {
                        XiangqiGrid(stepX, stepY)
                        RiverLabels(stepX, stepY, rotated)
                        Column(Modifier.fillMaxSize()) {
                            repeat(XiangqiState.ROWS) { row ->
                                Row {
                                    repeat(XiangqiState.COLS) { col ->
                                        val (modelRow, modelCol) =
                                            xiangqiBoardCoordinate(row, col, rotated)
                                        val piece = state.piece(modelRow, modelCol)
                                        val isSelected = selected == modelRow to modelCol
                                        Box(
                                            modifier = Modifier
                                                .size(stepX, stepY)
                                                .semantics {
                                                    contentDescription =
                                                        "第${modelRow + 1}行第${modelCol + 1}列"
                                                }
                                                .clickable(enabled = interactive) {
                                                    onTap(modelRow, modelCol)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) SelectionHalo(stepY * 0.90f)
                                            if (piece != null) XiangqiPieceView(piece, stepY * 0.82f)
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
private fun XiangqiGrid(stepX: Dp, stepY: Dp) {
    Canvas(Modifier.fillMaxSize()) {
        val dx = stepX.toPx()
        val dy = stepY.toPx()
        val left = dx / 2f
        val right = size.width - dx / 2f
        val top = dy / 2f
        val bottom = size.height - dy / 2f
        val line = Color(0xDD252016)
        val stroke = 1.55f

        repeat(XiangqiState.ROWS) { row ->
            val y = top + row * dy
            drawLine(line, Offset(left, y), Offset(right, y), stroke)
        }
        repeat(XiangqiState.COLS) { col ->
            val x = left + col * dx
            drawLine(line, Offset(x, top), Offset(x, top + 4f * dy), stroke)
            drawLine(line, Offset(x, top + 5f * dy), Offset(x, bottom), stroke)
        }

        drawLine(line, Offset(left + 3f * dx, top), Offset(left + 5f * dx, top + 2f * dy), stroke)
        drawLine(line, Offset(left + 5f * dx, top), Offset(left + 3f * dx, top + 2f * dy), stroke)
        drawLine(line, Offset(left + 3f * dx, top + 7f * dy), Offset(left + 5f * dx, bottom), stroke)
        drawLine(line, Offset(left + 5f * dx, top + 7f * dy), Offset(left + 3f * dx, bottom), stroke)
    }
}

@Composable
private fun RiverLabels(stepX: Dp, stepY: Dp, rotated: Boolean) {
    val (leftLabel, rightLabel) = xiangqiRiverLabels(rotated)
    Text(
        leftLabel,
        modifier = Modifier.offset(x = stepX * 1.30f, y = stepY * 5f - 17.dp),
        color = Color(0xFF292117),
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 23.sp
    )
    Text(
        rightLabel,
        modifier = Modifier.offset(x = stepX * 5.25f, y = stepY * 5f - 17.dp),
        color = Color(0xFF292117),
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 23.sp
    )
}

@Composable
private fun SelectionHalo(diameter: Dp) {
    Box(Modifier.size(diameter).border(3.dp, Color(0xFFF4D27D), CircleShape))
}

@Composable
private fun XiangqiPieceView(piece: XiangqiPiece, diameter: Dp) {
    Box(Modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension * 0.43f
            val center = Offset(size.width / 2f, size.height / 2f - radius * 0.03f)
            drawCircle(Color(0x66000000), radius, center + Offset(radius * 0.10f, radius * 0.16f))
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFFF8D58E), Gold, Color(0xFF674016)),
                    center = center - Offset(radius * 0.25f, radius * 0.32f),
                    radius = radius * 1.30f
                ),
                radius = radius,
                center = center
            )
            val lacquer = if (piece.side == Side.RED) {
                listOf(Color(0xFFC94A35), Cinnabar, Color(0xFF661C17))
            } else {
                listOf(Color(0xFF555B58), LacquerBlack, Color(0xFF050707))
            }
            drawCircle(
                brush = Brush.radialGradient(
                    lacquer,
                    center = center - Offset(radius * 0.25f, radius * 0.32f),
                    radius = radius * 1.22f
                ),
                radius = radius * 0.78f,
                center = center
            )
            drawCircle(Color(0xFF38210D), radius * 0.88f, center, style = Stroke(1.2f))
            drawCircle(PaleGold.copy(alpha = 0.52f), radius * 0.73f, center, style = Stroke(1.05f))
            drawCircle(Color.White.copy(alpha = 0.18f), radius * 0.12f, center - Offset(radius * 0.30f, radius * 0.36f))
        }
        Text(
            piece.displayLabel(),
            color = PaleGold,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp
        )
    }
}

@Composable
private fun XiangqiInfoRail(
    score: String,
    status: String,
    turn: Side,
    gameOver: Boolean,
    inCheck: Boolean,
    canUndo: Boolean,
    showUndo: Boolean,
    onUndo: () -> Unit,
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (gameOver) {
                Text("对局结果", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text(status, color = Cinnabar, fontSize = 19.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("当前回合：", color = Ink, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                    val sideColor = if (turn == Side.RED) Cinnabar else LacquerBlack
                    Text(
                        if (turn == Side.RED) "红方" else "黑方",
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(sideColor.copy(alpha = 0.10f))
                            .border(1.dp, sideColor.copy(alpha = 0.38f), RoundedCornerShape(5.dp))
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                        color = sideColor,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (inCheck) {
                    Text(
                        "将军",
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Cinnabar)
                            .padding(horizontal = 18.dp, vertical = 7.dp),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        HorizontalDivider(color = Color(0xFFB9C0BD))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (gameOver) MenuButton("重新开始", Cinnabar, Color.White, onRestart)
            if (showUndo) {
                MenuButton("悔棋", Color.Transparent, Ink, onUndo, outlined = true, enabled = canUndo)
            }
            ExitButton(onExit)
        }
    }
}

@Composable
private fun ExitButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(7.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Ink),
        border = BorderStroke(1.dp, Ink.copy(alpha = 0.42f))
    ) {
        ExitGlyph()
        Spacer(Modifier.width(10.dp))
        Text("退出游戏", fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ExitGlyph() {
    Canvas(Modifier.size(24.dp)) {
        val color = Ink
        drawRoundRect(
            color = color,
            topLeft = Offset(2f, 3f),
            size = Size(size.width * 0.55f, size.height - 6f),
            cornerRadius = CornerRadius(2.5f, 2.5f),
            style = Stroke(2.2f)
        )
        val y = size.height / 2f
        drawLine(color, Offset(size.width * 0.38f, y), Offset(size.width - 2f, y), 2.2f)
        drawLine(color, Offset(size.width - 7f, y - 5f), Offset(size.width - 2f, y), 2.2f)
        drawLine(color, Offset(size.width - 7f, y + 5f), Offset(size.width - 2f, y), 2.2f)
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
private fun MenuButton(
    label: String,
    container: Color,
    content: Color,
    onClick: () -> Unit,
    outlined: Boolean = false,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(7.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
        border = if (outlined) {
            BorderStroke(1.dp, content.copy(alpha = if (enabled) 0.42f else 0.16f))
        } else {
            null
        }
    ) { Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun GameBackdrop(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(PaperTop, PaperBottom)))) { content() }
}
