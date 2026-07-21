package com.buddygames.junqi

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal object JunqiUiText {
    const val TITLE = "军棋"
    val MENU_LABELS: List<String> = listOf("单人模式", "双人对战", "退出游戏")
    val versionLabel: String
        get() = versionLabel(JUNQI_VERSION_NAME)

    fun versionLabel(versionName: String): String = "版本 $versionName"

    fun sideLabel(side: JunqiSide): String = if (side == JunqiSide.RED) "红方" else "蓝方"

    fun battleOutcomeLabel(outcome: JunqiBattleOutcome): String = when (outcome) {
        JunqiBattleOutcome.ATTACKER_WINS -> "进攻方胜"
        JunqiBattleOutcome.DEFENDER_WINS -> "防守方胜"
        JunqiBattleOutcome.BOTH_REMOVED -> "同归于尽"
    }

    fun resultLabel(result: JunqiResult, mode: JunqiMode, playerSide: JunqiSide): String = when {
        result == JunqiResult.DRAW -> "和棋"
        mode == JunqiMode.SINGLE_PLAYER && result.winner == playerSide -> "玩家胜"
        mode == JunqiMode.SINGLE_PLAYER -> "智能胜"
        else -> "${sideLabel(requireNotNull(result.winner))}胜"
    }
}

internal data class JunqiLandscapeLayout(
    val horizontalPaddingDp: Float,
    val verticalPaddingDp: Float,
    val gapDp: Float,
    val boardWidthDp: Float,
    val boardHeightDp: Float,
    val railWidthDp: Float,
    val contentHeightDp: Float,
) {
    val contentWidthDp: Float
        get() = boardWidthDp + gapDp + railWidthDp
}

internal fun junqiLandscapeLayout(
    availableWidthDp: Float,
    availableHeightDp: Float,
): JunqiLandscapeLayout {
    require(availableWidthDp > 0f && availableHeightDp > 0f)
    val horizontalPadding = 20f
    val verticalPadding = 20f
    val gap = 20f
    val innerWidth = (availableWidthDp - horizontalPadding * 2f).coerceAtLeast(1f)
    val contentHeight = (availableHeightDp - verticalPadding * 2f).coerceAtLeast(1f)
    val railWidth = (availableWidthDp * 0.30f)
        .coerceIn(220f, 280f)
        .coerceAtMost((innerWidth - gap - 1f).coerceAtLeast(1f))
    val availableBoardWidth = (innerWidth - gap - railWidth).coerceAtLeast(1f)
    val boardHeight = minOf(
        contentHeight,
        availableBoardWidth / JUNQI_BOARD_ASPECT_RATIO,
        760f,
    )
    return JunqiLandscapeLayout(
        horizontalPaddingDp = horizontalPadding,
        verticalPaddingDp = verticalPadding,
        gapDp = gap,
        boardWidthDp = boardHeight * JUNQI_BOARD_ASPECT_RATIO,
        boardHeightDp = boardHeight,
        railWidthDp = railWidth,
        contentHeightDp = contentHeight,
    )
}

internal fun junqiShowsUndo(result: JunqiResult?): Boolean = result?.winner == null

internal fun junqiShowsRestart(result: JunqiResult?): Boolean = result != null

@Composable
internal fun JunqiMenu(
    textures: JunqiTextureSet,
    versionName: String,
    onSingle: () -> Unit,
    onTwoPlayers: () -> Unit,
    onExit: () -> Unit,
) {
    JunqiBackdrop {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val layout = junqiLandscapeLayout(maxWidth.value, maxHeight.value)
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(layout.contentWidthDp.dp)
                    .height(layout.contentHeightDp.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                JunqiBoardView(
                    boardTexture = textures.board,
                    pieces = emptyList(),
                    bottomSide = JunqiSide.RED,
                    selected = null,
                    legalDestinations = emptySet(),
                    lastMove = null,
                    interactive = false,
                    onTap = {},
                    modifier = Modifier.size(layout.boardWidthDp.dp, layout.boardHeightDp.dp),
                )
                Spacer(Modifier.width(layout.gapDp.dp))
                JunqiMenuPanel(
                    icon = textures.icon,
                    shelf = textures.shelf,
                    versionName = versionName,
                    onSingle = onSingle,
                    onTwoPlayers = onTwoPlayers,
                    onExit = onExit,
                    modifier = Modifier
                        .width(layout.railWidthDp.dp)
                        .height(layout.contentHeightDp.dp),
                )
            }
        }
    }
}

@Composable
internal fun JunqiGameLayout(
    state: JunqiSessionState,
    textures: JunqiTextureSet,
    selected: JunqiPosition?,
    legalDestinations: Set<JunqiPosition>,
    robotThinking: Boolean,
    onBoardTap: (JunqiPosition) -> Unit,
    onRandomize: () -> Unit,
    onResetDeployment: () -> Unit,
    onReady: () -> Unit,
    onAcceptHandoff: () -> Unit,
    onAcknowledgeBattle: () -> Unit,
    onUndo: () -> Unit,
    onRestart: () -> Unit,
    onReturn: () -> Unit,
) {
    when (state.phase) {
        JunqiPhase.HANDOFF -> JunqiHandoffScreen(state.currentSide, onAcceptHandoff)
        JunqiPhase.BATTLE_RESULT -> JunqiBattleResultScreen(
            outcome = requireNotNull(state.battleOutcome),
            onConfirm = onAcknowledgeBattle,
        )
        JunqiPhase.DEPLOYMENT,
        JunqiPhase.PLAYING,
        JunqiPhase.FINISHED,
        -> JunqiBoardAndRail(
            state = state,
            textures = textures,
            selected = selected,
            legalDestinations = legalDestinations,
            robotThinking = robotThinking,
            onBoardTap = onBoardTap,
            onRandomize = onRandomize,
            onResetDeployment = onResetDeployment,
            onReady = onReady,
            onUndo = onUndo,
            onRestart = onRestart,
            onReturn = onReturn,
        )
    }
}

@Composable
private fun JunqiBoardAndRail(
    state: JunqiSessionState,
    textures: JunqiTextureSet,
    selected: JunqiPosition?,
    legalDestinations: Set<JunqiPosition>,
    robotThinking: Boolean,
    onBoardTap: (JunqiPosition) -> Unit,
    onRandomize: () -> Unit,
    onResetDeployment: () -> Unit,
    onReady: () -> Unit,
    onUndo: () -> Unit,
    onRestart: () -> Unit,
    onReturn: () -> Unit,
) {
    val observation = state.observation
    val pieces = if (state.phase == JunqiPhase.DEPLOYMENT) {
        junqiDeploymentPieces(state.deployment)
    } else {
        observation?.let(::junqiVisiblePieces).orEmpty()
    }
    val bottomSide = observation?.viewer ?: state.currentSide
    val interactive = when (state.phase) {
        JunqiPhase.DEPLOYMENT -> true
        JunqiPhase.PLAYING -> observation?.currentSide == observation?.viewer && !robotThinking
        else -> false
    }

    JunqiBackdrop {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val layout = junqiLandscapeLayout(maxWidth.value, maxHeight.value)
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(layout.contentWidthDp.dp)
                    .height(layout.contentHeightDp.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                JunqiBoardView(
                    boardTexture = textures.board,
                    pieces = pieces,
                    bottomSide = bottomSide,
                    selected = selected,
                    legalDestinations = legalDestinations,
                    lastMove = state.lastMove,
                    interactive = interactive,
                    onTap = onBoardTap,
                    modifier = Modifier.size(layout.boardWidthDp.dp, layout.boardHeightDp.dp),
                )
                Spacer(Modifier.width(layout.gapDp.dp))
                JunqiInfoRail(
                    state = state,
                    robotThinking = robotThinking,
                    onRandomize = onRandomize,
                    onResetDeployment = onResetDeployment,
                    onReady = onReady,
                    onUndo = onUndo,
                    onRestart = onRestart,
                    onReturn = onReturn,
                    modifier = Modifier
                        .width(layout.railWidthDp.dp)
                        .height(layout.contentHeightDp.dp),
                )
            }
        }
    }
}

@Composable
private fun JunqiMenuPanel(
    icon: ImageBitmap?,
    shelf: ImageBitmap?,
    versionName: String,
    onSingle: () -> Unit,
    onTwoPlayers: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(126.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (shelf != null) {
                    Image(
                        bitmap = shelf,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(0.70f),
                        contentScale = ContentScale.FillBounds,
                    )
                } else {
                    JunqiShelfFallback()
                }
                if (icon != null) {
                    Image(
                        bitmap = icon,
                        contentDescription = "军棋 Logo",
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    JunqiIconFallback()
                }
            }
            Text(
                text = JunqiUiText.TITLE,
                color = JunqiInk,
                fontFamily = FontFamily.Serif,
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = JunqiUiText.versionLabel(versionName),
                color = JunqiMutedInk,
                fontSize = 12.sp,
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            JunqiButton(JunqiUiText.MENU_LABELS[0], onSingle, primary = true)
            JunqiButton(JunqiUiText.MENU_LABELS[1], onTwoPlayers)
            JunqiButton(JunqiUiText.MENU_LABELS[2], onExit, danger = true)
        }
    }
}

@Composable
private fun JunqiInfoRail(
    state: JunqiSessionState,
    robotThinking: Boolean,
    onRandomize: () -> Unit,
    onResetDeployment: () -> Unit,
    onReady: () -> Unit,
    onUndo: () -> Unit,
    onRestart: () -> Unit,
    onReturn: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(JunqiRailSurface)
            .border(1.dp, JunqiRailOutline, RoundedCornerShape(6.dp))
            .padding(horizontal = 18.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        JunqiScoreBlock(state)
        HorizontalDivider(color = JunqiRailOutline.copy(alpha = 0.65f))
        JunqiStatusBlock(state, robotThinking)
        HorizontalDivider(color = JunqiRailOutline.copy(alpha = 0.65f))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (state.phase) {
                JunqiPhase.DEPLOYMENT -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        JunqiButton("随机", onRandomize, modifier = Modifier.weight(1f))
                        JunqiButton("重置", onResetDeployment, modifier = Modifier.weight(1f))
                    }
                    JunqiButton("出战", onReady, primary = true)
                }
                JunqiPhase.PLAYING,
                JunqiPhase.FINISHED,
                -> {
                    if (junqiShowsRestart(state.result)) {
                        JunqiButton("重新开始", onRestart, primary = true)
                    }
                    if (junqiShowsUndo(state.result)) {
                        JunqiButton("悔棋", onUndo, enabled = state.canUndo)
                    }
                }
                else -> Unit
            }
            JunqiButton("返回菜单", onReturn)
        }
    }
}

@Composable
private fun JunqiScoreBlock(state: JunqiSessionState) {
    val singlePlayer = state.mode == JunqiMode.SINGLE_PLAYER
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (singlePlayer) "玩家 : 智能" else "红方 : 蓝方",
            color = JunqiInk,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (singlePlayer) {
                "${state.score.player} : ${state.score.robot}"
            } else {
                "${state.score.red} : ${state.score.blue}"
            },
            color = JunqiInk,
            fontSize = 38.sp,
        )
        if (singlePlayer) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "智能等级 ${state.score.intelligenceLevel}",
                color = JunqiPine,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun JunqiStatusBlock(state: JunqiSessionState, robotThinking: Boolean) {
    val title = when (state.phase) {
        JunqiPhase.DEPLOYMENT -> "${JunqiUiText.sideLabel(state.currentSide)}布阵"
        JunqiPhase.FINISHED -> JunqiUiText.resultLabel(
            requireNotNull(state.result),
            state.mode,
            state.playerSide,
        )
        JunqiPhase.PLAYING -> if (robotThinking) {
            "智能思考中"
        } else {
            "${JunqiUiText.sideLabel(state.observation?.currentSide ?: state.currentSide)}回合"
        }
        else -> ""
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            color = if (state.phase == JunqiPhase.FINISHED) JunqiRed else JunqiInk,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        if (state.phase == JunqiPhase.DEPLOYMENT && state.selectedDeploymentPiece != null) {
            Spacer(Modifier.height(8.dp))
            Text("已选择棋子", color = JunqiMutedInk, fontSize = 14.sp)
        }
        if (state.phase == JunqiPhase.PLAYING && state.mode == JunqiMode.SINGLE_PLAYER) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "玩家执${if (state.playerSide == JunqiSide.RED) "红" else "蓝"}",
                color = JunqiMutedInk,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun JunqiHandoffScreen(side: JunqiSide, onAccept: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(JunqiPrivacy),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text = "请${JunqiUiText.sideLabel(side)}接管",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
            )
            JunqiButton(
                label = "接管棋盘",
                onClick = onAccept,
                primary = true,
                modifier = Modifier.width(220.dp),
            )
        }
    }
}

@Composable
private fun JunqiBattleResultScreen(
    outcome: JunqiBattleOutcome,
    onConfirm: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(JunqiPrivacy),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text("碰子结果", color = JunqiPrivacyMuted, fontSize = 16.sp)
            Text(
                text = JunqiUiText.battleOutcomeLabel(outcome),
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
            )
            JunqiButton(
                label = "确认",
                onClick = onConfirm,
                primary = true,
                modifier = Modifier.width(220.dp),
            )
        }
    }
}

@Composable
private fun JunqiButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    danger: Boolean = false,
    enabled: Boolean = true,
) {
    val contentColor = when {
        primary -> Color.White
        danger -> JunqiRed
        else -> JunqiInk
    }
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp),
        shape = RoundedCornerShape(6.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (primary) JunqiPine else Color.Transparent,
            contentColor = contentColor,
        ),
        border = if (primary) null else BorderStroke(1.dp, contentColor.copy(alpha = 0.40f)),
    ) {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun JunqiIconFallback() {
    Canvas(
        Modifier
            .size(92.dp)
            .semantics { contentDescription = "军棋 Logo" },
    ) {
        drawCircle(Color(0xFFF7F6F0))
        drawLine(
            color = JunqiPine,
            start = Offset(size.width * 0.20f, size.height * 0.50f),
            end = Offset(size.width * 0.80f, size.height * 0.50f),
            strokeWidth = size.minDimension * 0.08f,
            cap = StrokeCap.Round,
        )
        drawCircle(JunqiRed, radius = size.minDimension * 0.17f, center = Offset(size.width * 0.34f, size.height * 0.38f))
        drawCircle(JunqiBlue, radius = size.minDimension * 0.17f, center = Offset(size.width * 0.66f, size.height * 0.62f))
    }
}

@Composable
private fun JunqiShelfFallback() {
    Canvas(Modifier.fillMaxSize()) {
        drawLine(
            color = JunqiPine.copy(alpha = 0.22f),
            start = Offset(size.width * 0.08f, size.height * 0.72f),
            end = Offset(size.width * 0.92f, size.height * 0.72f),
            strokeWidth = 2.dp.toPx(),
        )
        drawLine(
            color = JunqiBrass.copy(alpha = 0.28f),
            start = Offset(size.width * 0.18f, size.height * 0.80f),
            end = Offset(size.width * 0.82f, size.height * 0.80f),
            strokeWidth = 2.dp.toPx(),
        )
    }
}

@Composable
private fun JunqiBackdrop(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(JunqiCanvas),
    ) {
        content()
    }
}

private const val JUNQI_BOARD_ASPECT_RATIO =
    JunqiVisuals.BOARD_TEXTURE_WIDTH.toFloat() / JunqiVisuals.BOARD_TEXTURE_HEIGHT

private val JunqiCanvas = Color(0xFFE9EFEC)
private val JunqiInk = Color(0xFF20312E)
private val JunqiMutedInk = Color(0xFF60706C)
private val JunqiPine = Color(0xFF214F45)
private val JunqiRed = Color(0xFFB83A32)
private val JunqiBlue = Color(0xFF315B83)
private val JunqiBrass = Color(0xFFC9B779)
private val JunqiRailSurface = Color(0xFFF7F8F5)
private val JunqiRailOutline = Color(0xFFAAB8B4)
private val JunqiPrivacy = Color(0xFF182523)
private val JunqiPrivacyMuted = Color(0xFFB8C7C2)
