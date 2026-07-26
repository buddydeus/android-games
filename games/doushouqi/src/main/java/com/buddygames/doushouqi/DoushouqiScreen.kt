package com.buddygames.doushouqi

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun DoushouqiMenu(
    versionName: String,
    icon: ImageBitmap?,
    onSingle: () -> Unit,
    onTwo: () -> Unit,
    onExit: () -> Unit,
) {
    DoushouqiResponsiveLayout(
        railWidth = DOUSHOUQI_MENU_RAIL_WIDTH_DP,
        railHeightFraction = DOUSHOUQI_MENU_RAIL_HEIGHT_FRACTION,
        board = {
            DoushouqiBoard(
                state = DoushouqiState.initial().copyWith(
                    board = List(DoushouqiState.SQUARES) { null },
                ),
                selected = null,
                legalMoves = emptyList(),
                rotated = false,
                enabled = false,
                onTap = {},
            )
        },
        rail = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (icon != null) {
                    Image(
                        bitmap = icon,
                        contentDescription = "斗兽棋图标",
                        modifier = Modifier.size(112.dp),
                    )
                }
                Text(
                    "斗兽棋",
                    color = DoushouqiInk,
                    fontFamily = FontFamily.Serif,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    doushouqiVersionLabel(versionName),
                    color = DoushouqiMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                )
                DoushouqiActionButton("单人模式", onSingle, primary = true)
                DoushouqiActionButton("双人对战", onTwo)
                DoushouqiActionButton("退出游戏", onExit, danger = true)
            }
        },
    )
}

@Composable
internal fun DoushouqiGameLayout(
    session: DoushouqiSessionState,
    mode: DoushouqiMode,
    selected: DoushouqiPosition?,
    legalMoves: List<DoushouqiMove>,
    robotThinking: Boolean,
    onTap: (DoushouqiPosition) -> Unit,
    onUndo: () -> Unit,
    onRestart: () -> Unit,
    onReturn: () -> Unit,
) {
    val rotated = shouldRotateDoushouqiBoard(mode, session.playerSide)
    DoushouqiResponsiveLayout(
        railWidth = DOUSHOUQI_GAME_RAIL_WIDTH_DP,
        railHeightFraction = DOUSHOUQI_GAME_RAIL_HEIGHT_FRACTION,
        board = {
            DoushouqiBoard(
                state = session.position,
                selected = selected,
                legalMoves = legalMoves,
                rotated = rotated,
                enabled = !robotThinking && session.position.result == null,
                onTap = onTap,
            )
        },
        rail = {
            DoushouqiStatusRail(
                session,
                mode,
                robotThinking,
                onUndo,
                onRestart,
                onReturn,
            )
        },
    )
}

@Composable
private fun DoushouqiResponsiveLayout(
    railWidth: Float,
    railHeightFraction: Float,
    board: @Composable () -> Unit,
    rail: @Composable () -> Unit,
) {
    Surface(Modifier.fillMaxSize(), color = DoushouqiCanvas) {
        BoxWithConstraints(Modifier.padding(DOUSHOUQI_OUTER_PADDING_DP.dp)) {
            if (maxWidth.value >= DOUSHOUQI_WIDE_BREAKPOINT_DP) {
                Row(
                    Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(
                        DOUSHOUQI_BOARD_RAIL_GAP_DP.dp,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.weight(1f).fillMaxHeight(),
                        contentAlignment = Alignment.Center,
                    ) {
                        board()
                    }
                    Surface(
                        Modifier
                            .width(railWidth.dp)
                            .fillMaxHeight(railHeightFraction),
                        color = DoushouqiSurface,
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Box(Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                            rail()
                        }
                    }
                }
            } else {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        board()
                    }
                    Surface(
                        Modifier.fillMaxWidth().heightIn(min = 152.dp),
                        color = DoushouqiSurface,
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Box(Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                            rail()
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun DoushouqiBoard(
    state: DoushouqiState,
    selected: DoushouqiPosition?,
    legalMoves: List<DoushouqiMove>,
    rotated: Boolean,
    enabled: Boolean,
    onTap: (DoushouqiPosition) -> Unit,
) {
    Box(
        Modifier
            .fillMaxHeight()
            .aspectRatio(7f / 9f)
            .background(DoushouqiBoardColor)
            .border(2.dp, DoushouqiGrid),
    ) {
        Column(Modifier.fillMaxSize()) {
            repeat(DoushouqiState.ROWS) { displayRow ->
                Row(Modifier.weight(1f)) {
                    repeat(DoushouqiState.COLUMNS) { displayColumn ->
                        val model = modelPosition(displayRow, displayColumn, rotated)
                        val piece = state.pieceAt(model)
                        val destination = legalMoves.firstOrNull { it.to == model }
                        DoushouqiCell(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            model = model,
                            piece = piece,
                            selected = selected == model,
                            legal = destination != null,
                            capture = destination != null && piece != null,
                            latest = state.lastMove?.to == model,
                            enabled = enabled,
                            onTap = { onTap(model) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DoushouqiCell(
    modifier: Modifier,
    model: DoushouqiPosition,
    piece: DoushouqiPiece?,
    selected: Boolean,
    legal: Boolean,
    capture: Boolean,
    latest: Boolean,
    enabled: Boolean,
    onTap: () -> Unit,
) {
    val terrain = terrainAt(model)
    val terrainColor = when (terrain) {
        DoushouqiTerrain.RIVER -> DoushouqiRiver
        DoushouqiTerrain.DEN -> DoushouqiDen.copy(alpha = 0.82f)
        DoushouqiTerrain.TRAP -> DoushouqiBoardLight
        DoushouqiTerrain.LAND -> DoushouqiBoardLight
    }
    val description = buildString {
        piece?.let {
            append(if (it.side == DoushouqiSide.PINE_GREEN) "松绿方" else "朱砂方")
            append(it.animal.label)
        } ?: append(
            when (terrain) {
                DoushouqiTerrain.RIVER -> "河道"
                DoushouqiTerrain.TRAP -> "陷阱"
                DoushouqiTerrain.DEN -> "兽穴"
                DoushouqiTerrain.LAND -> "空格"
            },
        )
        append("，第${model.row + 1}行第${model.column + 1}列")
        if (selected) append("，已选择")
        if (legal) append("，合法落点")
    }
    Box(
        modifier
            .background(terrainColor)
            .border(0.5.dp, DoushouqiGrid.copy(alpha = 0.72f))
            .semantics { contentDescription = description }
            .clickable(enabled = enabled, onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        if (terrain == DoushouqiTerrain.RIVER) {
            Canvas(Modifier.fillMaxSize()) {
                repeat(3) { line ->
                    val y = size.height * (0.25f + line * 0.25f)
                    drawLine(
                        DoushouqiRiverLine,
                        Offset(size.width * 0.2f, y),
                        Offset(size.width * 0.8f, y),
                        strokeWidth = 1.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
        if (piece == null && terrain in setOf(DoushouqiTerrain.DEN, DoushouqiTerrain.TRAP)) {
            Text(
                if (terrain == DoushouqiTerrain.DEN) "兽穴" else "陷阱",
                color = if (terrain == DoushouqiTerrain.DEN) DoushouqiPieceText else DoushouqiGrid,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        if (legal && !capture) {
            Box(Modifier.size(10.dp).background(DoushouqiInk, RoundedCornerShape(50)))
        }
        if (piece != null) {
            Box(
                Modifier
                    .fillMaxSize(DOUSHOUQI_PIECE_SCALE)
                    .background(
                        if (piece.side == DoushouqiSide.PINE_GREEN) {
                            DoushouqiGreenPiece
                        } else {
                            DoushouqiRedPiece
                        },
                        RoundedCornerShape(7.dp),
                    )
                    .then(
                        if (selected) Modifier.border(
                            3.dp,
                            DoushouqiPieceText,
                            RoundedCornerShape(7.dp),
                        ) else Modifier,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    piece.animal.label,
                    color = DoushouqiPieceText,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
        if (capture) {
            Canvas(Modifier.fillMaxSize(0.68f)) {
                drawCircle(
                    DoushouqiPieceText,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)),
                    ),
                )
            }
        }
        if (latest) {
            LastMoveCorners()
        }
    }
}

@Composable
private fun LastMoveCorners() {
    Canvas(Modifier.fillMaxSize(DOUSHOUQI_LAST_MOVE_MARKER_SCALE)) {
        val inset = size.minDimension * DOUSHOUQI_LAST_MOVE_MARKER_INSET_FRACTION
        val length = size.minDimension * DOUSHOUQI_LAST_MOVE_MARKER_CORNER_LENGTH_FRACTION
        val points = listOf(
            Offset(inset, inset) to listOf(Offset(inset + length, inset), Offset(inset, inset + length)),
            Offset(size.width - inset, inset) to listOf(
                Offset(size.width - inset - length, inset),
                Offset(size.width - inset, inset + length),
            ),
            Offset(inset, size.height - inset) to listOf(
                Offset(inset + length, size.height - inset),
                Offset(inset, size.height - inset - length),
            ),
            Offset(size.width - inset, size.height - inset) to listOf(
                Offset(size.width - inset - length, size.height - inset),
                Offset(size.width - inset, size.height - inset - length),
            ),
        )
        points.forEach { (corner, ends) ->
            ends.forEach { end ->
                drawLine(DoushouqiLastMove, corner, end, 3.dp.toPx(), StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun DoushouqiStatusRail(
    session: DoushouqiSessionState,
    mode: DoushouqiMode,
    robotThinking: Boolean,
    onUndo: () -> Unit,
    onRestart: () -> Unit,
    onReturn: () -> Unit,
) {
    val result = session.position.result
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            if (mode == DoushouqiMode.SINGLE_PLAYER) "玩家      智能" else "松绿      朱砂",
            color = DoushouqiMuted,
            fontSize = 14.sp,
        )
        Text(
            if (mode == DoushouqiMode.SINGLE_PLAYER) {
                "${session.score.player}  :  ${session.score.robot}"
            } else {
                "${session.score.green}  :  ${session.score.vermilion}"
            },
            color = DoushouqiInk,
            fontFamily = FontFamily.Monospace,
            fontSize = 38.sp,
        )
        Text(
            resultText(result) ?: if (robotThinking) {
                "智能思考中"
            } else {
                "${sideLabel(session.position.sideToMove)}回合"
            },
            color = DoushouqiInk,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
        )
        if (mode == DoushouqiMode.SINGLE_PLAYER) {
            Text(
                "玩家执${sideLabel(session.playerSide)} · 等级 ${session.intelligenceLevel.level}",
                color = DoushouqiMuted,
                fontSize = 14.sp,
            )
        }
        if (shouldShowDoushouqiUndo(result)) {
            DoushouqiActionButton(
                "悔棋",
                onUndo,
                enabled = session.historySize > 0 && !robotThinking,
            )
        }
        if (shouldShowDoushouqiRestart(result)) {
            DoushouqiActionButton("重新开始", onRestart, primary = true)
        }
        DoushouqiActionButton("返回菜单", onReturn)
    }
}

@Composable
private fun DoushouqiActionButton(
    label: String,
    onClick: () -> Unit,
    primary: Boolean = false,
    danger: Boolean = false,
    enabled: Boolean = true,
) {
    if (primary) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DoushouqiGreenPiece),
            shape = RoundedCornerShape(8.dp),
        ) {
            Text(label)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            shape = RoundedCornerShape(8.dp),
        ) {
            Text(label, color = if (danger) DoushouqiRedPiece else DoushouqiInk)
        }
    }
}

private fun sideLabel(side: DoushouqiSide): String =
    if (side == DoushouqiSide.PINE_GREEN) "松绿方" else "朱砂方"

private fun resultText(result: DoushouqiResult?): String? = when (result) {
    is DoushouqiResult.Win -> "${sideLabel(result.winner)}${
        when (result.reason) {
            DoushouqiWinReason.DEN -> "进入兽穴"
            DoushouqiWinReason.FINAL_CAPTURE -> "吃光对方"
            DoushouqiWinReason.NO_LEGAL_MOVE -> "胜 · 对方无棋可走"
        }
    }"
    is DoushouqiResult.Draw -> when (result.reason) {
        DoushouqiDrawReason.REPETITION -> "三次重复和棋"
        DoushouqiDrawReason.QUIET_100 -> "连续百步未吃子和棋"
    }
    null -> null
}
