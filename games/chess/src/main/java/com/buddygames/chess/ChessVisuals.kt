package com.buddygames.chess

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CanvasTop = Color(0xFFE6ECEA)
private val CanvasBottom = Color(0xFFCDD9D6)
private val Ink = Color(0xFF1D2927)
private val MutedInk = Color(0xFF5B6966)
private val DeepGreen = Color(0xFF344842)
private val LightSquare = Color(0xFFD8D4C6)
private val DarkSquare = Color(0xFF557161)
private val BoardRim = Color(0xFF344842)
private val BoardRimEdge = Color(0xFF1F2E2A)
private val Ivory = Color(0xFFF2EEE2)
private val RailSurface = Color(0xFFF7F9F7)
private val RailOutline = Color(0xFFAAB8B4)
private val Brass = Color(0xFFB38A45)
private val CheckRed = Color(0xFFB0443D)
private val FocusTeal = Color(0xFF08758A)
private val BrightBlue = Color(CHESS_LAST_MOVE_MARKER_HIGHLIGHT_ARGB)

internal const val CHESS_LAYOUT_PADDING_DP = 28f
internal const val CHESS_LAYOUT_GAP_DP = 34f
internal const val CHESS_MENU_RAIL_WIDTH_DP = 320f
internal const val CHESS_MENU_RAIL_HEIGHT_FRACTION = 0.88f
internal const val CHESS_GAME_RAIL_WIDTH_DP = 300f
internal const val CHESS_GAME_RAIL_HEIGHT_FRACTION = 0.94f
internal const val CHESS_WIDE_LAYOUT_MIN_WIDTH_DP = 900f

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
    _availableHeightDp: Float
): Boolean = availableWidthDp >= CHESS_WIDE_LAYOUT_MIN_WIDTH_DP

@Composable
internal fun ChessMenu(
    versionName: String,
    pieceTextures: Map<ChessPiece, ImageBitmap>,
    onSingle: () -> Unit,
    onTwo: () -> Unit,
    onExit: () -> Unit
) {
    ChessBackdrop {
        BoxWithConstraints(Modifier.fillMaxSize().padding(CHESS_LAYOUT_PADDING_DP.dp)) {
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
                        pieceTextures = pieceTextures,
                        onTap = {},
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(CHESS_LAYOUT_GAP_DP.dp))
                    ChessMenuPanel(
                        versionName,
                        onSingle,
                        onTwo,
                        onExit,
                        Modifier
                            .width(CHESS_MENU_RAIL_WIDTH_DP.dp)
                            .fillMaxHeight(CHESS_MENU_RAIL_HEIGHT_FRACTION)
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
                        pieceTextures = pieceTextures,
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
    Column(
        modifier = modifier.padding(horizontal = 14.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "国际象棋",
                color = Ink,
                fontFamily = FontFamily.Serif,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "CLASSICAL BOARD",
                color = MutedInk,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
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
    pieceTextures: Map<ChessPiece, ImageBitmap>,
    onTap: (Int) -> Unit,
    onUndo: () -> Unit,
    onRestart: () -> Unit,
    onReturn: () -> Unit
) {
    ChessBackdrop {
        BoxWithConstraints(Modifier.fillMaxSize().padding(CHESS_LAYOUT_PADDING_DP.dp)) {
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
                        pieceTextures,
                        onTap,
                        Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(CHESS_LAYOUT_GAP_DP.dp))
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
                        Modifier
                            .width(CHESS_GAME_RAIL_WIDTH_DP.dp)
                            .fillMaxHeight(CHESS_GAME_RAIL_HEIGHT_FRACTION)
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
                        pieceTextures,
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
internal fun ChessPromotionDialog(
    side: ChessSide,
    choices: List<ChessPieceType>,
    pieceTextures: Map<ChessPiece, ImageBitmap>,
    onSelect: (ChessPieceType) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("选择升变棋子", color = Ink, fontWeight = FontWeight.Bold)
        },
        text = {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                choices.forEach { type ->
                    TextButton(onClick = { onSelect(type) }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            ChessPieceVisual(
                                piece = ChessPiece(side, type),
                                texture = pieceTextures[ChessPiece(side, type)],
                                size = 48.dp
                            )
                            Text(type.chineseName(), color = Ink, fontSize = 14.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = MutedInk)
            }
        },
        shape = RoundedCornerShape(8.dp),
        containerColor = Color(0xFFFCFDFC)
    )
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
    pieceTextures: Map<ChessPiece, ImageBitmap>,
    onTap: (Int) -> Unit,
    modifier: Modifier
) {
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val boardSize = minOf(maxWidth, maxHeight, 760.dp)
        val rimWidth = 24.dp
        val squareSize = (boardSize - rimWidth * 2) / 8
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
                .shadow(14.dp, RoundedCornerShape(6.dp), clip = false)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF425A53), BoardRim, Color(0xFF2A3C37))
                    )
                )
                .border(2.dp, BoardRimEdge, RoundedCornerShape(6.dp))
        ) {
            ChessBoardCoordinates(rotated)
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(rimWidth)
                    .border(2.dp, BoardRimEdge)
            ) {
                repeat(8) { displayRow ->
                    Row(Modifier.weight(1f)) {
                        repeat(8) { displayCol ->
                            val square = chessModelSquare(displayRow, displayCol, rotated)
                            var hasFocus by remember(square, interactive) {
                                mutableStateOf(false)
                            }
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
                                        if (checkedKing == square) {
                                            Modifier.background(CheckRed.copy(alpha = 0.18f))
                                        } else Modifier
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
                                    .onFocusChanged { hasFocus = it.isFocused }
                                    .focusable(enabled = interactive)
                                    .clickable(enabled = interactive) { onTap(square) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (legal.isNotEmpty()) {
                                    if (piece == null) {
                                        Box(
                                            Modifier
                                                .size(squareSize / 8)
                                                .background(Ink.copy(alpha = 0.46f), CircleShape)
                                        )
                                    } else {
                                        Box(
                                            Modifier
                                                .fillMaxSize(0.90f)
                                                .border(3.dp, Brass.copy(alpha = 0.88f), CircleShape)
                                        )
                                    }
                                }
                                piece?.let {
                                    ChessPieceVisual(
                                        piece = it,
                                        texture = pieceTextures[it],
                                        size = squareSize
                                    )
                                }
                                if (isSelected) {
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .background(Brass.copy(alpha = 0.12f))
                                            .border(3.dp, Brass)
                                    )
                                }
                                if (checkedKing == square) {
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .border(4.dp, CheckRed)
                                    )
                                }
                                if (hasFocus) {
                                    Box(Modifier.fillMaxSize().border(3.dp, FocusTeal))
                                }
                                if (isLast) {
                                    LastMoveMarker(squareSize * CHESS_LAST_MOVE_MARKER_SCALE)
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
private fun ChessPieceVisual(piece: ChessPiece, texture: ImageBitmap?, size: Dp) {
    if (texture != null) {
        Image(
            bitmap = texture,
            contentDescription = null,
            modifier = Modifier.size(size * CHESS_PIECE_TEXTURE_SCALE),
            contentScale = ContentScale.Fit
        )
        return
    }

    val glyph = chessPieceGlyph(piece)
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

private fun chessPieceGlyph(piece: ChessPiece): String = when (piece.side) {
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

@Composable
private fun BoxScope.ChessBoardCoordinates(rotated: Boolean) {
    val files = if (rotated) ('h' downTo 'a').toList() else ('a'..'h').toList()
    val ranks = if (rotated) (1..8).toList() else (8 downTo 1).toList()
    val coordinateColor = Ivory.copy(alpha = 0.78f)

    listOf(Alignment.TopCenter, Alignment.BottomCenter).forEach { alignment ->
        Row(
            Modifier
                .align(alignment)
                .fillMaxWidth()
                .height(24.dp)
                .padding(horizontal = 24.dp)
        ) {
            files.forEach { file ->
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Text(
                        file.toString(),
                        color = coordinateColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
    listOf(Alignment.CenterStart, Alignment.CenterEnd).forEach { alignment ->
        Column(
            Modifier
                .align(alignment)
                .fillMaxHeight()
                .width(24.dp)
                .padding(vertical = 24.dp)
        ) {
            ranks.forEach { rank ->
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        rank.toString(),
                        color = coordinateColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
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
            Text(score, color = Ink, fontSize = 44.sp, fontWeight = FontWeight.Normal)
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
        HorizontalDivider(color = RailOutline.copy(alpha = 0.62f))
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
                }
                Text(status, color = MutedInk, fontSize = 15.sp, textAlign = TextAlign.Center)
            }
        }
        HorizontalDivider(color = RailOutline.copy(alpha = 0.62f))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
            .shadow(5.dp, RoundedCornerShape(8.dp), clip = false)
            .clip(RoundedCornerShape(8.dp))
            .background(RailSurface)
            .border(1.dp, RailOutline, RoundedCornerShape(8.dp))
            .padding(horizontal = 24.dp, vertical = 26.dp),
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
    var hasFocus by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(7.dp)
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .onFocusChanged { hasFocus = it.isFocused }
            .then(
                if (hasFocus) Modifier.border(2.dp, FocusTeal, shape) else Modifier
            ),
        shape = shape,
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
