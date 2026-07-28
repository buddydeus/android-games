package com.buddygames.doushouqi

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.layout.ContentScale
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
    textures: DoushouqiTextureSet,
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
                textures = textures,
                onTap = {},
            )
        },
        rail = {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
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
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    DoushouqiActionButton("单人模式", onSingle, primary = true)
                    DoushouqiActionButton("双人对战", onTwo)
                    DoushouqiActionButton("退出游戏", onExit, danger = true)
                }
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
    textures: DoushouqiTextureSet,
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
                textures = textures,
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
                        shape = RoundedCornerShape(DOUSHOUQI_RAIL_CORNER_DP.dp),
                        border = BorderStroke(1.dp, DoushouqiMuted.copy(alpha = 0.28f)),
                        shadowElevation = 5.dp,
                    ) {
                        Box(
                            Modifier.padding(
                                horizontal = DOUSHOUQI_RAIL_HORIZONTAL_PADDING_DP.dp,
                                vertical = DOUSHOUQI_RAIL_VERTICAL_PADDING_DP.dp,
                            ),
                            contentAlignment = Alignment.Center,
                        ) {
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
                        shape = RoundedCornerShape(DOUSHOUQI_RAIL_CORNER_DP.dp),
                        border = BorderStroke(1.dp, DoushouqiMuted.copy(alpha = 0.28f)),
                        shadowElevation = 5.dp,
                    ) {
                        Box(
                            Modifier.padding(
                                horizontal = DOUSHOUQI_RAIL_HORIZONTAL_PADDING_DP.dp,
                                vertical = DOUSHOUQI_RAIL_VERTICAL_PADDING_DP.dp,
                            ),
                            contentAlignment = Alignment.Center,
                        ) {
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
    textures: DoushouqiTextureSet,
    onTap: (DoushouqiPosition) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val boardSize = minOf(maxWidth, maxHeight)
        Box(
            Modifier
                .size(boardSize)
                .aspectRatio(DOUSHOUQI_BOARD_ASPECT_RATIO)
                .background(DoushouqiBoardColor),
        ) {
            textures.board?.let { board ->
                Image(
                    bitmap = board,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds,
                )
            }
            val gridInset = boardSize * (64f / DoushouqiVisuals.BOARD_TEXTURE_SIZE)
            Column(Modifier.fillMaxSize().padding(gridInset)) {
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
                                textures = textures,
                                texturedBoard = textures.board != null,
                                onTap = { onTap(model) },
                            )
                        }
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
    textures: DoushouqiTextureSet,
    texturedBoard: Boolean,
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
            .background(if (texturedBoard) Color.Transparent else terrainColor)
            .then(
                if (texturedBoard) {
                    Modifier
                } else {
                    Modifier.border(0.5.dp, DoushouqiGrid.copy(alpha = 0.72f))
                },
            )
            .semantics { contentDescription = description }
            .clickable(enabled = enabled, onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        if (!texturedBoard && terrain == DoushouqiTerrain.RIVER) {
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
        if (
            !texturedBoard &&
            piece == null &&
            terrain in setOf(DoushouqiTerrain.DEN, DoushouqiTerrain.TRAP)
        ) {
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
            val texture = textures.piece(piece)
            Box(
                Modifier
                    .fillMaxHeight(DOUSHOUQI_PIECE_SCALE)
                    .aspectRatio(1f)
                    .then(
                        if (texture == null) {
                            Modifier.background(
                                if (piece.side == DoushouqiSide.PINE_GREEN) {
                                    DoushouqiGreenPiece
                                } else {
                                    DoushouqiRedPiece
                                },
                                RoundedCornerShape(7.dp),
                            )
                        } else {
                            Modifier
                        },
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
                if (texture != null) {
                    Image(
                        bitmap = texture,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Text(
                        piece.animal.label,
                        color = DoushouqiPieceText,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
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
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (mode == DoushouqiMode.SINGLE_PLAYER) {
                    "玩家 : 智能"
                } else {
                    "松绿方 : 朱砂方"
                },
                color = DoushouqiInk,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val firstScore = if (mode == DoushouqiMode.SINGLE_PLAYER) {
                    session.score.player
                } else {
                    session.score.green
                }
                val secondScore = if (mode == DoushouqiMode.SINGLE_PLAYER) {
                    session.score.robot
                } else {
                    session.score.vermilion
                }
                Text(
                    firstScore.toString(),
                    color = DoushouqiGreenPiece,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 44.sp,
                )
                Text(
                    " : ",
                    color = DoushouqiInk,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 44.sp,
                )
                Text(
                    secondScore.toString(),
                    color = DoushouqiRedPiece,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 44.sp,
                )
            }
            if (mode == DoushouqiMode.SINGLE_PLAYER) {
                Spacer(Modifier.height(7.dp))
                Text(
                    "智能等级 ${session.intelligenceLevel.level}",
                    color = DoushouqiGreenPiece,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        HorizontalDivider(color = DoushouqiMuted.copy(alpha = 0.32f))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (result != null) {
                Text(
                    "对局结果",
                    color = DoushouqiMuted,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    requireNotNull(doushouqiResultText(result, mode)),
                    color = DoushouqiRedPiece,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "当前回合：",
                        color = DoushouqiInk,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    val side = session.position.sideToMove
                    val sideColor = if (side == DoushouqiSide.PINE_GREEN) {
                        DoushouqiGreenPiece
                    } else {
                        DoushouqiRedPiece
                    }
                    Text(
                        doushouqiSinglePlayerSideLabel(side),
                        modifier = Modifier
                            .background(
                                sideColor.copy(alpha = 0.10f),
                                RoundedCornerShape(5.dp),
                            )
                            .border(
                                1.dp,
                                sideColor.copy(alpha = 0.38f),
                                RoundedCornerShape(5.dp),
                            )
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                        color = sideColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (robotThinking) {
                    Text(
                        "智能思考中",
                        color = DoushouqiMuted,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val captureLabels =
                    doushouqiRoundCaptureLabels(session.lastCompletedRoundCaptures)
                if (captureLabels.isEmpty()) {
                    Text(
                        "无吃子",
                        color = DoushouqiMuted,
                        fontSize = 16.sp,
                    )
                } else {
                    captureLabels.forEach { (side, label) ->
                        val sideColor =
                            if (side == DoushouqiSide.PINE_GREEN) {
                                DoushouqiGreenPiece
                            } else {
                                DoushouqiRedPiece
                            }
                        Text(
                            label,
                            modifier = Modifier
                                .background(
                                    sideColor.copy(alpha = 0.10f),
                                    RoundedCornerShape(5.dp),
                                )
                                .border(
                                    1.dp,
                                    sideColor.copy(alpha = 0.38f),
                                    RoundedCornerShape(5.dp),
                                )
                                .padding(horizontal = 9.dp, vertical = 4.dp),
                            color = sideColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = DoushouqiMuted.copy(alpha = 0.32f))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (shouldShowDoushouqiRestart(result)) {
                DoushouqiActionButton("重新开始", onRestart, primary = true)
            }
            if (shouldShowDoushouqiUndo(result)) {
                DoushouqiActionButton(
                    "悔棋",
                    onUndo,
                    enabled = session.historySize > 0 && !robotThinking,
                )
            }
            DoushouqiActionButton("返回菜单", onReturn, danger = true)
        }
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
            modifier = Modifier.fillMaxWidth().height(DOUSHOUQI_ACTION_HEIGHT_DP.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DoushouqiGreenPiece),
            shape = RoundedCornerShape(DOUSHOUQI_RAIL_CORNER_DP.dp),
        ) {
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().height(DOUSHOUQI_ACTION_HEIGHT_DP.dp),
            shape = RoundedCornerShape(DOUSHOUQI_RAIL_CORNER_DP.dp),
            border = BorderStroke(
                1.dp,
                if (danger) {
                    DoushouqiRedPiece.copy(alpha = if (enabled) 0.76f else 0.24f)
                } else {
                    DoushouqiInk.copy(alpha = if (enabled) 0.42f else 0.16f)
                },
            ),
        ) {
            Text(
                label,
                color = if (danger) DoushouqiRedPiece else DoushouqiInk,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

internal fun doushouqiSinglePlayerSideLabel(side: DoushouqiSide): String =
    if (side == DoushouqiSide.PINE_GREEN) "绿方" else "红方"

internal fun doushouqiFullSideLabel(side: DoushouqiSide): String =
    if (side == DoushouqiSide.PINE_GREEN) "松绿方" else "朱砂方"

internal fun doushouqiTurnLine(side: DoushouqiSide): String =
    "当前回合：${doushouqiSinglePlayerSideLabel(side)}"

internal fun doushouqiRoundCaptureLabels(
    captures: DoushouqiRoundCaptures,
): List<Pair<DoushouqiSide, String>> = buildList {
    captures.capturedByGreen?.let {
        add(DoushouqiSide.PINE_GREEN to "绿方吃：${it.animal.label}")
    }
    captures.capturedByRed?.let {
        add(DoushouqiSide.VERMILION to "红方吃：${it.animal.label}")
    }
}

internal fun doushouqiResultText(
    result: DoushouqiResult?,
    mode: DoushouqiMode,
): String? = when (result) {
    is DoushouqiResult.Win -> "${
        if (mode == DoushouqiMode.SINGLE_PLAYER) {
            doushouqiSinglePlayerSideLabel(result.winner)
        } else {
            doushouqiFullSideLabel(result.winner)
        }
    }${
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
