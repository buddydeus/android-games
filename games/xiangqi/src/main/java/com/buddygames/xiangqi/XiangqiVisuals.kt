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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
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

internal const val XIANGQI_CANVAS_ARGB = 0xFFEAF1EFL
internal const val XIANGQI_CANVAS_LINE_ARGB = 0xFFD3E0DCL
internal const val XIANGQI_RAIL_ARGB = 0xFFFBFCFAL
internal const val XIANGQI_INK_ARGB = 0xFF213431L
internal const val XIANGQI_MUTED_INK_ARGB = 0xFF61736FL
internal const val XIANGQI_CELADON_ARGB = 0xFFA9C7BEL
internal const val XIANGQI_CINNABAR_ARGB = 0xFFB83A32L
internal const val XIANGQI_BLUE_BLACK_ARGB = 0xFF263C42L

private val CanvasColor = Color(XIANGQI_CANVAS_ARGB)
private val CanvasLine = Color(XIANGQI_CANVAS_LINE_ARGB)
private val RailSurface = Color(XIANGQI_RAIL_ARGB)
private val Ink = Color(XIANGQI_INK_ARGB)
private val MutedInk = Color(XIANGQI_MUTED_INK_ARGB)
private val Celadon = Color(XIANGQI_CELADON_ARGB)
private val CeladonEdge = Color(0xFF6E9489)
private val Cinnabar = Color(XIANGQI_CINNABAR_ARGB)
private val BlueBlack = Color(XIANGQI_BLUE_BLACK_ARGB)
private val FocusBlue = Color(0xFF08758A)
private val PieceBody = Color(0xFFF3E7CE)
private val PieceEdge = Color(0xFFB89B6D)

@Composable
internal fun XiangqiMenu(
    textures: XiangqiTextureSet,
    versionName: String,
    onSingle: () -> Unit,
    onTwo: () -> Unit,
    onExit: () -> Unit
) {
    GameBackdrop {
        BoxWithConstraints(Modifier.fillMaxSize().padding(28.dp)) {
            if (maxWidth >= 900.dp) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    XiangqiBoard(
                        XiangqiState.initial(),
                        null,
                        { _, _ -> },
                        Modifier.weight(1f),
                        textures,
                        false
                    )
                    Spacer(Modifier.width(34.dp))
                    XiangqiMenuPanel(
                        versionName,
                        onSingle,
                        onTwo,
                        onExit,
                        Modifier.width(310.dp).fillMaxHeight(0.88f)
                    )
                }
            } else {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    XiangqiBoard(
                        XiangqiState.initial(),
                        null,
                        { _, _ -> },
                        Modifier.weight(1f),
                        textures,
                        false
                    )
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
            Text("象棋", color = Ink, fontFamily = FontFamily.Serif, fontSize = 42.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("楚河 · 漢界", color = MutedInk, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(xiangqiVersionLabel(versionName), color = MutedInk, fontSize = 12.sp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MenuButton(labels[0], Cinnabar, Color.White, onSingle)
            MenuButton(labels[1], Color.Transparent, MutedInk, onTwo, outlined = true)
            MenuButton(labels[2], Color.Transparent, Cinnabar, onExit, outlined = true)
        }
    }
}

internal fun xiangqiMenuLabels(): List<String> = listOf("单人模式", "双人对战", "退出游戏")
internal fun xiangqiVersionLabel(versionName: String): String = "版本 $versionName"
internal fun shouldShowXiangqiUndo(result: XiangqiResult?): Boolean = result?.winner == null
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
internal fun xiangqiLastMoveCell(move: XiangqiMove?): Pair<Int, Int>? =
    move?.let { it.toRow to it.toCol }
internal fun xiangqiIntelligenceLabel(level: Int): String {
    require(level in 1..10)
    return "智能等级 $level"
}
internal const val LAST_MOVE_MARKER_SCALE = 0.92f
internal const val LAST_MOVE_MARKER_INSET_FRACTION = 0.04f
internal const val LAST_MOVE_MARKER_CORNER_LENGTH_FRACTION = 0.18f
internal const val LAST_MOVE_MARKER_SHADOW_ARGB = 0x70115C93L
internal const val LAST_MOVE_MARKER_HIGHLIGHT_ARGB = 0xB84FCBFFL

@Composable
internal fun XiangqiGameLayout(
    state: XiangqiState,
    selected: Pair<Int, Int>?,
    lastMove: XiangqiMove?,
    status: String,
    turn: Side,
    score: String,
    intelligenceLevel: Int?,
    gameOver: Boolean,
    inCheck: Boolean,
    canUndo: Boolean,
    showUndo: Boolean,
    rotateBoard: Boolean,
    onTap: (Int, Int) -> Unit,
    onUndo: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit,
    textures: XiangqiTextureSet
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
                        textures,
                        rotated = rotateBoard,
                        lastMove = lastMove
                    )
                    Spacer(Modifier.width(34.dp))
                    XiangqiInfoRail(
                        score,
                        intelligenceLevel,
                        status,
                        turn,
                        gameOver,
                        inCheck,
                        canUndo,
                        showUndo,
                        onUndo,
                        onRestart,
                        onExit,
                        Modifier.width(300.dp).fillMaxHeight(0.94f)
                    )
                }
            } else {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    XiangqiBoard(
                        state,
                        selected,
                        onTap,
                        Modifier.weight(1f),
                        textures,
                        rotated = rotateBoard,
                        lastMove = lastMove
                    )
                    XiangqiInfoRail(
                        score,
                        intelligenceLevel,
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
    textures: XiangqiTextureSet,
    interactive: Boolean = true,
    rotated: Boolean = false,
    lastMove: XiangqiMove? = null
) {
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val board = xiangqiBoardSize(maxWidth.value - 8f, maxHeight.value - 8f)
        val boardWidth = board.width.dp
        val boardHeight = board.height.dp
        val gridLeft = boardWidth * XIANGQI_GRID_LEFT_FRACTION
        val gridTop = boardHeight * XIANGQI_GRID_TOP_FRACTION
        val gridWidth =
            boardWidth * (XIANGQI_GRID_RIGHT_FRACTION - XIANGQI_GRID_LEFT_FRACTION)
        val gridHeight =
            boardHeight * (XIANGQI_GRID_BOTTOM_FRACTION - XIANGQI_GRID_TOP_FRACTION)
        val stepX = gridWidth / (XiangqiState.COLS - 1).toFloat()
        val stepY = gridHeight / (XiangqiState.ROWS - 1).toFloat()
        val touchWidth = stepX * XiangqiState.COLS.toFloat()
        val touchHeight = stepY * XiangqiState.ROWS.toFloat()
        val touchLeft = gridLeft - stepX / 2f
        val touchTop = gridTop - stepY / 2f
        val cellSize = minOf(stepX.value, stepY.value).dp
        val lastMoveCell = xiangqiLastMoveCell(lastMove)

        Box(
            Modifier
                .size(boardWidth, boardHeight)
        ) {
            if (textures.board != null) {
                Image(
                    bitmap = textures.board,
                    contentDescription = "象棋棋盘",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .shadow(18.dp, RoundedCornerShape(7.dp), clip = false)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color(0xFFF5F1E8))
                        .border(5.dp, CeladonEdge, RoundedCornerShape(7.dp))
                )
            }

            Box(
                Modifier
                    .offset(x = touchLeft, y = touchTop)
                    .size(touchWidth, touchHeight)
            ) {
                if (textures.board == null) {
                    Box(Modifier.fillMaxSize()) {
                        XiangqiGrid(stepX, stepY)
                        RiverLabels(stepX, stepY, rotated)
                    }
                }
                Column(Modifier.fillMaxSize()) {
                    repeat(XiangqiState.ROWS) { row ->
                        Row {
                            repeat(XiangqiState.COLS) { col ->
                                val (modelRow, modelCol) =
                                    xiangqiBoardCoordinate(row, col, rotated)
                                val piece = state.piece(modelRow, modelCol)
                                val isSelected = selected == modelRow to modelCol
                                val isLastMove = lastMoveCell == modelRow to modelCol
                                Box(
                                    modifier = Modifier
                                        .size(stepX, stepY)
                                        .semantics {
                                            contentDescription =
                                                "第${modelRow + 1}行第${modelCol + 1}列" +
                                                    if (isLastMove) "，最后一步" else ""
                                        }
                                        .clickable(enabled = interactive) {
                                            onTap(modelRow, modelCol)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) SelectionHalo(cellSize * 0.90f)
                                    if (piece != null) {
                                        XiangqiPieceView(
                                            piece = piece,
                                            diameter = cellSize * XIANGQI_PIECE_TEXTURE_SCALE,
                                            texture = textures.pieces[piece]
                                        )
                                    }
                                    if (isLastMove) {
                                        LastMoveMarker(cellSize * LAST_MOVE_MARKER_SCALE)
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
        val line = Color(0xEE263C38)
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
        color = Ink,
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 23.sp
    )
    Text(
        rightLabel,
        modifier = Modifier.offset(x = stepX * 5.25f, y = stepY * 5f - 17.dp),
        color = Ink,
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 23.sp
    )
}

@Composable
private fun SelectionHalo(diameter: Dp) {
    Box(Modifier.size(diameter).border(3.dp, FocusBlue.copy(alpha = 0.84f), CircleShape))
}

@Composable
private fun LastMoveMarker(size: Dp) {
    Canvas(Modifier.size(size)) {
        val inset = this.size.minDimension * LAST_MOVE_MARKER_INSET_FRACTION
        val length = this.size.minDimension * LAST_MOVE_MARKER_CORNER_LENGTH_FRACTION
        val edge = this.size.minDimension - inset
        val shadowWidth = this.size.minDimension * 0.068f
        val highlightWidth = this.size.minDimension * 0.045f
        val segments = listOf(
            Offset(inset, inset + length) to Offset(inset, inset),
            Offset(inset, inset) to Offset(inset + length, inset),
            Offset(edge - length, inset) to Offset(edge, inset),
            Offset(edge, inset) to Offset(edge, inset + length),
            Offset(inset, edge - length) to Offset(inset, edge),
            Offset(inset, edge) to Offset(inset + length, edge),
            Offset(edge - length, edge) to Offset(edge, edge),
            Offset(edge, edge) to Offset(edge, edge - length)
        )
        segments.forEach { (start, end) ->
            drawLine(
                Color(LAST_MOVE_MARKER_SHADOW_ARGB),
                start,
                end,
                shadowWidth,
                StrokeCap.Round
            )
            drawLine(
                Color(LAST_MOVE_MARKER_HIGHLIGHT_ARGB),
                start,
                end,
                highlightWidth,
                StrokeCap.Round
            )
        }
    }
}

@Composable
private fun XiangqiPieceView(
    piece: XiangqiPiece,
    diameter: Dp,
    texture: ImageBitmap?
) {
    Box(Modifier.size(diameter), contentAlignment = Alignment.Center) {
        if (texture != null) {
            Image(
                bitmap = texture,
                contentDescription = piece.displayLabel(),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            return@Box
        }
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension * 0.43f
            val center = Offset(size.width / 2f, size.height / 2f - radius * 0.03f)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFFFFFBF1), PieceBody, PieceEdge),
                    center = center - Offset(radius * 0.25f, radius * 0.32f),
                    radius = radius * 1.30f
                ),
                radius = radius,
                center = center
            )
            val sideColor = if (piece.side == Side.RED) Cinnabar else BlueBlack
            drawCircle(PieceEdge, radius * 0.88f, center, style = Stroke(1.2f))
            drawCircle(sideColor.copy(alpha = 0.72f), radius * 0.73f, center, style = Stroke(1.05f))
            drawCircle(Color.White.copy(alpha = 0.18f), radius * 0.12f, center - Offset(radius * 0.30f, radius * 0.36f))
        }
        Text(
            piece.displayLabel(),
            color = if (piece.side == Side.RED) Cinnabar else BlueBlack,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp
        )
    }
}

@Composable
private fun XiangqiInfoRail(
    score: String,
    intelligenceLevel: Int?,
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
            Text(
                if (intelligenceLevel == null) "红方 : 黑方" else "玩家 : 智能",
                color = Ink,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(14.dp))
            Text(score, color = Ink, fontSize = 48.sp, fontWeight = FontWeight.Light)
            if (intelligenceLevel != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    xiangqiIntelligenceLabel(intelligenceLevel),
                    color = Cinnabar,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        HorizontalDivider(color = Celadon.copy(alpha = 0.72f))
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
                    val sideColor = if (turn == Side.RED) Cinnabar else BlueBlack
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
                            .border(1.dp, Cinnabar, RoundedCornerShape(6.dp))
                            .padding(horizontal = 18.dp, vertical = 7.dp),
                        color = Cinnabar,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        HorizontalDivider(color = Celadon.copy(alpha = 0.72f))
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
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Cinnabar),
        border = BorderStroke(1.dp, Cinnabar.copy(alpha = 0.76f))
    ) {
        ExitGlyph()
        Spacer(Modifier.width(10.dp))
        Text("退出游戏", fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ExitGlyph() {
    Canvas(Modifier.size(24.dp)) {
        val color = Cinnabar
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
            .clip(RoundedCornerShape(6.dp))
            .background(RailSurface)
            .border(1.dp, Celadon.copy(alpha = 0.82f), RoundedCornerShape(6.dp))
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
            BorderStroke(1.dp, content.copy(alpha = if (enabled) 0.78f else 0.16f))
        } else {
            null
        }
    ) { Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun GameBackdrop(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(CanvasColor)) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(CanvasColor)
            repeat(3) { index ->
                val y = size.height * (index + 1) / 4f
                drawLine(
                    color = CanvasLine.copy(alpha = 0.70f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.5f
                )
            }
        }
        content()
    }
}
