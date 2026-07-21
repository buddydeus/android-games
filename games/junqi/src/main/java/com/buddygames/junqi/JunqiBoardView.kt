package com.buddygames.junqi

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal data class JunqiVisiblePiece(
    val id: String,
    val side: JunqiSide,
    val position: JunqiPosition,
    val label: String?,
    val isBack: Boolean,
)

internal data class JunqiTapResolution(
    val selection: JunqiPosition? = null,
    val move: JunqiMove? = null,
)

internal fun junqiModelPosition(
    displayRow: Int,
    displayColumn: Int,
    bottomSide: JunqiSide,
): JunqiPosition {
    require(displayRow in 0..11)
    require(displayColumn in 0..4)
    return if (bottomSide == JunqiSide.RED) {
        JunqiPosition(displayRow, displayColumn)
    } else {
        JunqiPosition(11 - displayRow, 4 - displayColumn)
    }
}

internal fun junqiDisplayPosition(
    position: JunqiPosition,
    bottomSide: JunqiSide,
): JunqiPosition = if (bottomSide == JunqiSide.RED) {
    position
} else {
    JunqiPosition(11 - position.row, 4 - position.column)
}

internal fun junqiPieceLabel(type: JunqiPieceType): String = when (type) {
    JunqiPieceType.COMMANDER -> "司令"
    JunqiPieceType.ARMY_COMMANDER -> "军长"
    JunqiPieceType.DIVISION_COMMANDER -> "师长"
    JunqiPieceType.BRIGADE_COMMANDER -> "旅长"
    JunqiPieceType.REGIMENT -> "团长"
    JunqiPieceType.BATTALION -> "营长"
    JunqiPieceType.COMPANY -> "连长"
    JunqiPieceType.PLATOON -> "排长"
    JunqiPieceType.ENGINEER -> "工兵"
    JunqiPieceType.BOMB -> "炸弹"
    JunqiPieceType.MINE -> "地雷"
    JunqiPieceType.FLAG -> "军旗"
}

internal fun junqiVisiblePieces(observation: JunqiObservation): List<JunqiVisiblePiece> =
    observation.ownPieces.map { piece ->
        JunqiVisiblePiece(
            id = piece.id,
            side = observation.viewer,
            position = piece.position,
            label = junqiPieceLabel(piece.type),
            isBack = false,
        )
    } + observation.opponentPieces.map { piece ->
        JunqiVisiblePiece(
            id = piece.id,
            side = other(observation.viewer),
            position = piece.position,
            label = if (piece.constraints.revealedFlag) junqiPieceLabel(JunqiPieceType.FLAG) else null,
            isBack = true,
        )
    }

internal fun junqiDeploymentPieces(deployment: List<JunqiPiece>): List<JunqiVisiblePiece> =
    deployment.map { piece ->
        JunqiVisiblePiece(
            id = piece.id,
            side = piece.side,
            position = piece.position,
            label = junqiPieceLabel(piece.type),
            isBack = false,
        )
    }

internal fun resolveJunqiBoardTap(
    observation: JunqiObservation,
    selected: JunqiPosition?,
    tapped: JunqiPosition,
): JunqiTapResolution {
    if (observation.result != null || observation.currentSide != observation.viewer) {
        return JunqiTapResolution()
    }
    val legalMoves = observation.visibleLegalMoves()
    legalMoves.firstOrNull { move -> move.from == selected && move.to == tapped }?.let { move ->
        return JunqiTapResolution(move = move)
    }
    val selectable = observation.ownPieces.any { piece -> piece.position == tapped } &&
        legalMoves.any { move -> move.from == tapped }
    return JunqiTapResolution(selection = tapped.takeIf { selectable })
}

@Composable
internal fun JunqiBoardView(
    boardTexture: ImageBitmap?,
    pieces: List<JunqiVisiblePiece>,
    bottomSide: JunqiSide,
    selected: JunqiPosition?,
    legalDestinations: Set<JunqiPosition>,
    lastMove: JunqiMove?,
    interactive: Boolean,
    onTap: (JunqiPosition) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(6.dp)
    BoxWithConstraints(
        modifier = modifier
            .clip(shape)
            .background(Color(JunqiVisuals.FALLBACK_BOARD_COLOR))
            .border(1.dp, Color(JunqiVisuals.FALLBACK_BOUNDARY_COLOR).copy(alpha = 0.5f), shape),
    ) {
        if (boardTexture != null) {
            Image(
                bitmap = boardTexture,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(if (bottomSide == JunqiSide.BLUE) 180f else 0f),
                contentScale = ContentScale.FillBounds,
            )
        } else {
            JunqiFallbackBoard(bottomSide)
        }

        val pieceByPosition = pieces.associateBy { it.position }
        val columnStep = maxWidth * (
            (JunqiVisuals.BOARD_GRID_RIGHT_FRACTION - JunqiVisuals.BOARD_GRID_LEFT_FRACTION) / 4f
        )
        val rowStep = maxHeight * (
            (JunqiVisuals.BOARD_GRID_BOTTOM_FRACTION - JunqiVisuals.BOARD_GRID_TOP_FRACTION) / 11f
        )
        val nodeWidth = columnStep * 0.92f
        val nodeHeight = rowStep * 0.92f
        val pieceWidth = columnStep * 0.72f
        val pieceHeight = rowStep * 0.68f

        repeat(12) { modelRow ->
            repeat(5) { modelColumn ->
                val modelPosition = JunqiPosition(modelRow, modelColumn)
                val displayPosition = junqiDisplayPosition(modelPosition, bottomSide)
                val xFraction = JunqiVisuals.BOARD_GRID_LEFT_FRACTION +
                    displayPosition.column *
                    (JunqiVisuals.BOARD_GRID_RIGHT_FRACTION - JunqiVisuals.BOARD_GRID_LEFT_FRACTION) / 4f
                val yFraction = JunqiVisuals.BOARD_GRID_TOP_FRACTION +
                    displayPosition.row *
                    (JunqiVisuals.BOARD_GRID_BOTTOM_FRACTION - JunqiVisuals.BOARD_GRID_TOP_FRACTION) / 11f
                val piece = pieceByPosition[modelPosition]
                val isLegal = modelPosition in legalDestinations
                val isLastMove = lastMove?.to == modelPosition
                val shouldDescribe = interactive || piece != null || isLegal || isLastMove
                Box(
                    modifier = Modifier
                        .offset(
                            x = maxWidth * xFraction - nodeWidth / 2,
                            y = maxHeight * yFraction - nodeHeight / 2,
                        )
                        .size(nodeWidth, nodeHeight)
                        .then(
                            if (shouldDescribe) {
                                Modifier.semantics {
                                    contentDescription = junqiNodeDescription(
                                        modelPosition,
                                        piece,
                                        isLegal,
                                        isLastMove,
                                    )
                                }
                            } else {
                                Modifier
                            },
                        )
                        .clickable(enabled = interactive) { onTap(modelPosition) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isLegal) {
                        if (piece == null) {
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .background(JunqiInk.copy(alpha = 0.52f), CircleShape),
                            )
                        } else {
                            Box(
                                Modifier
                                    .size(pieceWidth * 1.04f, pieceHeight * 1.12f)
                                    .border(2.dp, JunqiBrass, RoundedCornerShape(5.dp)),
                            )
                        }
                    }
                    piece?.let {
                        JunqiPieceView(
                            piece = it,
                            modifier = Modifier.size(pieceWidth, pieceHeight),
                        )
                    }
                    if (selected == modelPosition) {
                        Box(
                            Modifier
                                .size(pieceWidth * 1.08f, pieceHeight * 1.18f)
                                .border(2.dp, JunqiSelection, RoundedCornerShape(5.dp)),
                        )
                    }
                    if (isLastMove) {
                        JunqiLastMoveMarker(
                            modifier = Modifier.size(
                                columnStep * JunqiVisuals.LAST_MOVE_SCALE,
                                rowStep * JunqiVisuals.LAST_MOVE_SCALE,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun JunqiPieceView(piece: JunqiVisiblePiece, modifier: Modifier) {
    val sideColor = if (piece.side == JunqiSide.RED) JunqiRed else JunqiBlue
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (piece.isBack) sideColor else JunqiPieceFace)
            .border(2.dp, sideColor, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = piece.label ?: "★",
            color = if (piece.isBack) Color.White else sideColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun JunqiFallbackBoard(bottomSide: JunqiSide) {
    Canvas(Modifier.fillMaxSize()) {
        fun center(position: JunqiPosition): Offset {
            val display = junqiDisplayPosition(position, bottomSide)
            val x = JunqiVisuals.BOARD_GRID_LEFT_FRACTION +
                display.column *
                (JunqiVisuals.BOARD_GRID_RIGHT_FRACTION - JunqiVisuals.BOARD_GRID_LEFT_FRACTION) / 4f
            val y = JunqiVisuals.BOARD_GRID_TOP_FRACTION +
                display.row *
                (JunqiVisuals.BOARD_GRID_BOTTOM_FRACTION - JunqiVisuals.BOARD_GRID_TOP_FRACTION) / 11f
            return Offset(size.width * x, size.height * y)
        }

        drawRect(Color(JunqiVisuals.FALLBACK_BOARD_COLOR))
        JunqiBoard.roadNeighbors.forEach { (from, neighbors) ->
            neighbors.filter { to -> boardIndex(from) < boardIndex(to) }.forEach { to ->
                drawLine(
                    color = Color(JunqiVisuals.FALLBACK_ROAD_COLOR),
                    start = center(from),
                    end = center(to),
                    strokeWidth = size.minDimension * 0.008f,
                    cap = StrokeCap.Round,
                )
            }
        }
        JunqiBoard.railNeighbors.forEach { (from, neighbors) ->
            neighbors.filter { to -> boardIndex(from) < boardIndex(to) }.forEach { to ->
                drawLine(
                    color = Color(JunqiVisuals.FALLBACK_RAIL_COLOR),
                    start = center(from),
                    end = center(to),
                    strokeWidth = size.minDimension * 0.014f,
                    cap = StrokeCap.Round,
                )
            }
        }

        val leftBoundary = center(JunqiPosition(5, 0))
        val rightBoundary = center(JunqiPosition(5, 4))
        val nextLeftBoundary = center(JunqiPosition(6, 0))
        drawLine(
            color = Color(JunqiVisuals.FALLBACK_BOUNDARY_COLOR),
            start = Offset(leftBoundary.x, (leftBoundary.y + nextLeftBoundary.y) / 2f),
            end = Offset(rightBoundary.x, (rightBoundary.y + center(JunqiPosition(6, 4)).y) / 2f),
            strokeWidth = size.minDimension * 0.008f,
        )

        JunqiBoard.camps.forEach { position ->
            drawCircle(
                color = Color(JunqiVisuals.FALLBACK_CAMP_COLOR),
                radius = size.minDimension * 0.032f,
                center = center(position),
            )
            drawCircle(
                color = Color(JunqiVisuals.FALLBACK_RAIL_COLOR),
                radius = size.minDimension * 0.032f,
                center = center(position),
                style = Stroke(width = size.minDimension * 0.004f),
            )
        }
        JunqiBoard.headquarters.forEach { position ->
            val node = center(position)
            val width = size.width * 0.082f
            val height = size.height * 0.040f
            drawRoundRect(
                color = Color(JunqiVisuals.FALLBACK_HEADQUARTERS_COLOR),
                topLeft = Offset(node.x - width / 2f, node.y - height / 2f),
                size = Size(width, height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(height * 0.18f),
            )
            drawRoundRect(
                color = Color(JunqiVisuals.FALLBACK_BOUNDARY_COLOR),
                topLeft = Offset(node.x - width / 2f, node.y - height / 2f),
                size = Size(width, height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(height * 0.18f),
                style = Stroke(width = size.minDimension * 0.004f),
            )
        }
    }
}

@Composable
private fun JunqiLastMoveMarker(modifier: Modifier) {
    Canvas(modifier) {
        val insetX = size.width * JunqiVisuals.LAST_MOVE_INSET_FRACTION
        val insetY = size.height * JunqiVisuals.LAST_MOVE_INSET_FRACTION
        val edgeX = size.width - insetX
        val edgeY = size.height - insetY
        val lengthX = size.width * JunqiVisuals.LAST_MOVE_CORNER_LENGTH_FRACTION
        val lengthY = size.height * JunqiVisuals.LAST_MOVE_CORNER_LENGTH_FRACTION
        val segments = listOf(
            Offset(insetX, insetY + lengthY) to Offset(insetX, insetY),
            Offset(insetX, insetY) to Offset(insetX + lengthX, insetY),
            Offset(edgeX - lengthX, insetY) to Offset(edgeX, insetY),
            Offset(edgeX, insetY) to Offset(edgeX, insetY + lengthY),
            Offset(insetX, edgeY - lengthY) to Offset(insetX, edgeY),
            Offset(insetX, edgeY) to Offset(insetX + lengthX, edgeY),
            Offset(edgeX - lengthX, edgeY) to Offset(edgeX, edgeY),
            Offset(edgeX, edgeY) to Offset(edgeX, edgeY - lengthY),
        )
        val shadowWidth = size.minDimension * 0.068f
        val highlightWidth = size.minDimension * 0.045f
        segments.forEach { (start, end) ->
            drawLine(
                color = Color(JunqiVisuals.LAST_MOVE_SHADOW_COLOR),
                start = start,
                end = end,
                strokeWidth = shadowWidth,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Color(JunqiVisuals.LAST_MOVE_COLOR),
                start = start,
                end = end,
                strokeWidth = highlightWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

private fun junqiNodeDescription(
    position: JunqiPosition,
    piece: JunqiVisiblePiece?,
    legal: Boolean,
    lastMove: Boolean,
): String = buildString {
    append("第${position.row + 1}行第${position.column + 1}列")
    piece?.let {
        append("，")
        append(if (it.side == JunqiSide.RED) "红方" else "蓝方")
        if (it.isBack && it.label == null) {
            append("棋背")
        } else if (it.isBack) {
            append("已公开")
            append(it.label)
        } else {
            append(it.label)
        }
    }
    if (legal) append("，合法落点")
    if (lastMove) append("，最后一步")
}

private fun boardIndex(position: JunqiPosition): Int = position.row * 5 + position.column

private val JunqiInk = Color(0xFF20312E)
private val JunqiRed = Color(0xFFB83A32)
private val JunqiBlue = Color(0xFF315B83)
private val JunqiBrass = Color(0xFFC9B779)
private val JunqiPieceFace = Color(0xFFF7F6F0)
private val JunqiSelection = Color(0xFF08758A)
