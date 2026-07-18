package com.buddygames.chess

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CanvasTop = Color(0xFFF1F5F3)
private val CanvasBottom = Color(0xFFD7E3E1)
private val Ink = Color(0xFF202827)
private val MutedInk = Color(0xFF596866)
private val DeepGreen = Color(0xFF315B4C)
private val LightSquare = Color(0xFFE6DFC9)
private val DarkSquare = Color(0xFF63806B)
private val BoardRim = Color(0xFF3A2D20)
private val Ivory = Color(0xFFF8F4E8)
private val Brass = Color(0xFFC49A4B)
private val CheckRed = Color(0xFFAA3B32)
private val BrightBlue = Color(CHESS_LAST_MOVE_MARKER_HIGHLIGHT_ARGB)

internal fun chessModelSquare(displayRow: Int, displayCol: Int, rotated: Boolean): Int {
    require(displayRow in 0..7 && displayCol in 0..7)
    return if (rotated) {
        displayRow * 8 + (7 - displayCol)
    } else {
        (7 - displayRow) * 8 + displayCol
    }
}

internal fun chessUsesSideBySideLayout(
    availableWidthDp: Float,
    availableHeightDp: Float
): Boolean = availableWidthDp >= 700f && availableWidthDp > availableHeightDp

@Composable
internal fun ChessMenu(
    versionName: String,
    onSingle: () -> Unit,
    onTwo: () -> Unit,
    onExit: () -> Unit
) {
    ChessBackdrop {
        BoxWithConstraints(Modifier.fillMaxSize().padding(24.dp)) {
            if (chessUsesSideBySideLayout(maxWidth.value, maxHeight.value)) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    ChessBoard(
                        state = ChessState.initial(),
                        selected = null,
                        lastMove = null,
                        legalDestinations = emptyList(),
                        rotated = false,
                        interactive = false,
                        inCheck = false,
                        onTap = {},
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(34.dp))
                    ChessMenuPanel(
                        versionName,
                        onSingle,
                        onTwo,
                        onExit,
                        Modifier.width(310.dp).fillMaxHeight(0.88f)
                    )
                }
            } else {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    ChessBoard(
                        state = ChessState.initial(),
                        selected = null,
                        lastMove = null,
                        legalDestinations = emptyList(),
                        rotated = false,
                        interactive = false,
                        inCheck = false,
                        onTap = {},
                        modifier = Modifier.weight(1f)
                    )
                    ChessMenuPanel(
                        versionName,
                        onSingle,
                        onTwo,
                        onExit,
                        Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun ChessMenuPanel(
    versionName: String,
    onSingle: () -> Unit,
    onTwo: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier
) {
    val labels = chessMenuLabels()
    ChessPanel(modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "国际象棋",
                color = Ink,
                fontFamily = FontFamily.Serif,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text("CLASSICAL BOARD", color = DeepGreen, fontSize = 13.sp)
            Spacer(Modifier.height(5.dp))
            Text(chessVersionLabel(versionName), color = MutedInk, fontSize = 12.sp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ChessButton(labels[0], DeepGreen, Color.White, onSingle)
            ChessButton(labels[1], Ivory, Ink, onTwo, outlined = true)
            ChessButton(labels[2], Color.Transparent, CheckRed, onExit, outlined = true)
        }
    }
}

@Composable
internal fun ChessGameLayout(
    state: ChessState,
    selected: Int?,
    lastMove: ChessMove?,
    legalDestinations: List<ChessMove>,
    status: String,
    score: String,
    intelligenceLevel: Int?,
    result: ChessResult?,
    inCheck: Boolean,
    canUndo: Boolean,
    rotateBoard: Boolean,
    onTap: (Int) -> Unit,
    onUndo: () -> Unit,
    onRestart: () -> Unit,
    onReturn: () -> Unit
) {
    ChessBackdrop {
        BoxWithConstraints(Modifier.fillMaxSize().padding(24.dp)) {
            if (chessUsesSideBySideLayout(maxWidth.value, maxHeight.value)) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    ChessBoard(
                        state,
                        selected,
                        lastMove,
                        legalDestinations,
                        rotateBoard,
                        true,
                        inCheck,
                        onTap,
                        Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(34.dp))
                    ChessInfoRail(
                        state.sideToMove,
                        status,
                        score,
                        intelligenceLevel,
                        result,
                        inCheck,
                        canUndo,
                        onUndo,
                        onRestart,
                        onReturn,
                        Modifier.width(300.dp).fillMaxHeight(0.88f)
                    )
                }
            } else {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ChessBoard(
                        state,
                        selected,
                        lastMove,
                        legalDestinations,
                        rotateBoard,
                        true,
                        inCheck,
                        onTap,
                        Modifier.weight(1f)
                    )
                    ChessInfoRail(
                        state.sideToMove,
                        status,
                        score,
                        intelligenceLevel,
                        result,
                        inCheck,
                        canUndo,
                        onUndo,
                        onRestart,
                        onReturn,
                        Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun ChessBoard(
    state: ChessState,
    selected: Int?,
    lastMove: ChessMove?,
    legalDestinations: List<ChessMove>,
    rotated: Boolean,
    interactive: Boolean,
    inCheck: Boolean,
    onTap: (Int) -> Unit,
    modifier: Modifier
) {
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val boardSize = minOf(maxWidth, maxHeight, 760.dp)
        val legalByTarget = legalDestinations.groupBy { it.to }
        val checkedKing = if (inCheck) {
            state.board.indexOf(
                ChessPiece(state.sideToMove, ChessPieceType.KING)
            )
        } else {
            -1
        }
        Box(
            Modifier
                .size(boardSize)
                .aspectRatio(1f)
                .shadow(18.dp, RoundedCornerShape(7.dp), clip = false)
                .clip(RoundedCornerShape(7.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF70533A), BoardRim)))
                .border(2.dp, Color(0xFF261B14), RoundedCornerShape(7.dp))
                .padding(14.dp)
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .border(2.dp, Color(0xFF211813))
            ) {
                repeat(8) { displayRow ->
                    Row(Modifier.weight(1f)) {
                        repeat(8) { displayCol ->
                            val square = chessModelSquare(displayRow, displayCol, rotated)
                            val piece = state.board[square]
                            val isLight = (fileOf(square) + rankOf(square)) % 2 == 1
                            val isSelected = selected == square
                            val isLast = chessLastMoveSquare(lastMove) == square
                            val legal = legalByTarget[square].orEmpty()
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(if (isLight) LightSquare else DarkSquare)
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(4.dp, Brass)
                                        } else if (checkedKing == square) {
                                            Modifier.border(4.dp, CheckRed)
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .semantics {
                                        contentDescription = buildString {
                                            append(chessSquareName(square))
                                            piece?.let {
                                                append("，")
                                                append(if (it.side == ChessSide.WHITE) "白方" else "黑方")
                                                append(it.type.chineseName())
                                            }
                                            if (isLast) append("，最后一步")
                                        }
                                    }
                                    .clickable(enabled = interactive) { onTap(square) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (legal.isNotEmpty()) {
                                    if (piece == null) {
                                        Box(
                                            Modifier
                                                .size(boardSize / 54)
                                                .background(Ink.copy(alpha = 0.42f), CircleShape)
                                        )
                                    } else {
                                        Box(
                                            Modifier
                                                .fillMaxSize()
                                                .padding(5.dp)
                                                .border(3.dp, Brass.copy(alpha = 0.82f), CircleShape)
                                        )
                                    }
                                }
                                piece?.let { ChessPieceGlyph(it, boardSize / 12) }
                                if (isLast) LastMoveMarker(boardSize / 8 * CHESS_LAST_MOVE_MARKER_SCALE)
                                CoordinateLabel(square, displayRow, displayCol, isLight)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChessPieceGlyph(piece: ChessPiece, size: Dp) {
    val glyph = when (piece.side) {
        ChessSide.WHITE -> when (piece.type) {
            ChessPieceType.KING -> "♔"
            ChessPieceType.QUEEN -> "♕"
            ChessPieceType.ROOK -> "♖"
            ChessPieceType.BISHOP -> "♗"
            ChessPieceType.KNIGHT -> "♘"
            ChessPieceType.PAWN -> "♙"
        }
        ChessSide.BLACK -> when (piece.type) {
            ChessPieceType.KING -> "♚"
            ChessPieceType.QUEEN -> "♛"
            ChessPieceType.ROOK -> "♜"
            ChessPieceType.BISHOP -> "♝"
            ChessPieceType.KNIGHT -> "♞"
            ChessPieceType.PAWN -> "♟"
        }
    }
    val color = if (piece.side == ChessSide.WHITE) Ivory else Color(0xFF151B1A)
    val shadow = if (piece.side == ChessSide.WHITE) Ink else Color.White.copy(alpha = 0.35f)
    Text(
        text = glyph,
        color = color,
        fontFamily = FontFamily.Serif,
        fontSize = (size.value * 0.72f).sp,
        style = TextStyle(shadow = Shadow(shadow, Offset(1.5f, 2f), 2f)),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun BoxScope.CoordinateLabel(
    square: Int,
    displayRow: Int,
    displayCol: Int,
    lightSquare: Boolean
) {
    val labelColor = if (lightSquare) DeepGreen else Ivory.copy(alpha = 0.86f)
    if (displayCol == 0) {
        Text(
            text = "${rankOf(square) + 1}",
            modifier = Modifier.align(Alignment.TopStart).padding(3.dp),
            color = labelColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
    if (displayRow == 7) {
        val file = ('a'.code + fileOf(square)).toChar()
        Text(
            text = file.toString(),
            modifier = Modifier.align(Alignment.BottomEnd).padding(3.dp),
            color = labelColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LastMoveMarker(size: Dp) {
    Canvas(Modifier.size(size)) {
        val inset = this.size.minDimension * CHESS_LAST_MOVE_MARKER_INSET_FRACTION
        val length = this.size.minDimension * CHESS_LAST_MOVE_MARKER_CORNER_LENGTH_FRACTION
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
                Color(CHESS_LAST_MOVE_MARKER_SHADOW_ARGB),
                start,
                end,
                shadowWidth,
                StrokeCap.Round
            )
            drawLine(BrightBlue, start, end, highlightWidth, StrokeCap.Round)
        }
    }
}

@Composable
private fun ChessInfoRail(
    turn: ChessSide,
    status: String,
    score: String,
    intelligenceLevel: Int?,
    result: ChessResult?,
    inCheck: Boolean,
    canUndo: Boolean,
    onUndo: () -> Unit,
    onRestart: () -> Unit,
    onReturn: () -> Unit,
    modifier: Modifier
) {
    ChessPanel(modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (intelligenceLevel == null) "白方 : 黑方" else "玩家 : 智能",
                color = Ink,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            Text(score, color = Ink, fontSize = 46.sp, fontWeight = FontWeight.Light)
            intelligenceLevel?.let {
                Spacer(Modifier.height(7.dp))
                Text(
                    chessIntelligenceLabel(it),
                    color = DeepGreen,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        HorizontalDivider(color = Color(0xFFB9C3BF))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (result != null) {
                Text("对局结果", color = MutedInk, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    status,
                    color = if (result.winner == null) DeepGreen else CheckRed,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("当前回合：", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    val sideColor = if (turn == ChessSide.WHITE) Color(0xFF56615D) else Ink
                    Text(
                        if (turn == ChessSide.WHITE) "白方" else "黑方",
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(sideColor.copy(alpha = 0.10f))
                            .border(1.dp, sideColor.copy(alpha = 0.36f), RoundedCornerShape(5.dp))
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                        color = sideColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (inCheck) {
                    Text(
                        "将军",
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CheckRed)
                            .padding(horizontal = 18.dp, vertical = 7.dp),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(status, color = MutedInk, fontSize = 15.sp, textAlign = TextAlign.Center)
                }
            }
        }
        HorizontalDivider(color = Color(0xFFB9C3BF))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (shouldShowChessRestart(result)) {
                ChessButton("重新开始", DeepGreen, Color.White, onRestart)
            }
            if (shouldShowChessUndo(result)) {
                ChessButton(
                    "悔棋",
                    Color.Transparent,
                    Ink,
                    onUndo,
                    outlined = true,
                    enabled = canUndo
                )
            }
            ChessButton("返回菜单", Color.Transparent, MutedInk, onReturn, outlined = true)
        }
    }
}

@Composable
private fun ChessPanel(
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .shadow(12.dp, RoundedCornerShape(8.dp), clip = false)
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFFFCFDFC), Color(0xFFE8EEEB))))
            .border(1.dp, Color(0xFFB5C0BC), RoundedCornerShape(8.dp))
            .padding(horizontal = 26.dp, vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        content = content
    )
}

@Composable
private fun ChessButton(
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
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content
        ),
        border = if (outlined) BorderStroke(1.dp, content.copy(alpha = 0.42f)) else null
    ) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ChessBackdrop(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(CanvasTop, CanvasBottom)))
    ) {
        content()
    }
}

private fun ChessPieceType.chineseName(): String = when (this) {
    ChessPieceType.KING -> "王"
    ChessPieceType.QUEEN -> "后"
    ChessPieceType.ROOK -> "车"
    ChessPieceType.BISHOP -> "象"
    ChessPieceType.KNIGHT -> "马"
    ChessPieceType.PAWN -> "兵"
}
