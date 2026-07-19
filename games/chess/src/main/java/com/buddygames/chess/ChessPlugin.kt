package com.buddygames.chess

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import com.buddygames.api.GameManifest
import com.buddygames.api.GameContext
import com.buddygames.api.GameMode
import com.buddygames.api.GamePlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

class ChessPlugin : GamePlugin {
    override fun getManifest(): GameManifest = manifest

    @Composable
    override fun MainScreen(context: GameContext) {
        val pieceTextures = rememberChessPieceTextures(context)
        ChessMenu(
            versionName = context.gamePackage.manifest.versionName,
            pieceTextures = pieceTextures,
            onSingle = { context.startGame(GameMode.SINGLE_PLAYER) },
            onTwo = { context.startGame(GameMode.TWO_PLAYERS) },
            onExit = context::exitGame
        )
    }

    @Composable
    override fun GameScreen(context: GameContext, mode: GameMode) {
        val pieceTextures = rememberChessPieceTextures(context)
        val initialRound = remember { newChessRound() }
        var state by remember { mutableStateOf(initialRound.state) }
        var selected by remember { mutableStateOf(initialRound.selected) }
        var result by remember { mutableStateOf(initialRound.result) }
        var playerSide by remember { mutableStateOf(initialRound.playerSide) }
        var lastMove by remember { mutableStateOf(initialRound.lastMove) }
        var repetitions by remember { mutableStateOf(initialRound.repetitionCounts) }
        var score by remember { mutableStateOf(ChessScore()) }
        var history by remember { mutableStateOf(emptyList<ChessSnapshot>()) }
        var pendingPromotionMoves by remember { mutableStateOf(emptyList<ChessMove>()) }
        var searchGeneration by remember { mutableStateOf(0) }

        fun applyMove(move: ChessMove) {
            val nextState = state.apply(move)
            val nextRepetitions = recordChessPosition(repetitions, nextState)
            val nextResult = ChessRules.result(
                nextState,
                nextRepetitions[chessRepetitionKey(nextState)] ?: 1
            )
            state = nextState
            repetitions = nextRepetitions
            result = nextResult
            lastMove = move
            selected = null
            pendingPromotionMoves = emptyList()
            if (nextResult != null) {
                score = score.record(nextResult, mode, playerSide)
            }
        }

        val legalMoves = remember(state, result) {
            if (result == null) ChessRules.legalMoves(state) else emptyList()
        }

        fun commitPlayerMove(move: ChessMove) {
            history = history + ChessSnapshot(
                state,
                result,
                score,
                lastMove,
                repetitions
            )
            applyMove(move)
        }

        fun tap(square: Int) {
            if (
                result != null ||
                pendingPromotionMoves.isNotEmpty() ||
                needsChessRobotTurn(mode, state, playerSide, result)
            ) {
                return
            }
            val piece = state.board[square]
            val currentSelection = selected
            if (currentSelection == null) {
                if (piece?.side == state.sideToMove) selected = square
                return
            }
            val candidates = chessTapCandidates(state, currentSelection, square)
            if (chessPromotionChoices(candidates).size > 1) {
                pendingPromotionMoves = candidates
            } else if (candidates.isNotEmpty()) {
                commitPlayerMove(candidates.first())
            } else {
                selected = if (piece?.side == state.sideToMove) square else null
            }
        }

        fun undo() {
            val undo = undoChess(history) ?: return
            searchGeneration++
            pendingPromotionMoves = emptyList()
            state = undo.snapshot.state
            selected = null
            result = undo.snapshot.result
            score = undo.snapshot.score
            lastMove = undo.snapshot.lastMove
            repetitions = undo.snapshot.repetitionCounts
            history = undo.remainingHistory
        }

        fun restart() {
            searchGeneration++
            pendingPromotionMoves = emptyList()
            val nextPlayerSide = if (mode == GameMode.SINGLE_PLAYER) {
                nextChessPlayerSide(playerSide, result)
            } else {
                ChessSide.WHITE
            }
            val round = newChessRound(nextPlayerSide)
            state = round.state
            selected = round.selected
            result = round.result
            playerSide = round.playerSide
            lastMove = round.lastMove
            repetitions = round.repetitionCounts
            history = emptyList()
        }

        val robotThinking = needsChessRobotTurn(mode, state, playerSide, result)
        val intelligenceLevel = score.intelligenceLevel
        LaunchedEffect(
            state,
            playerSide,
            result,
            intelligenceLevel,
            mode,
            searchGeneration
        ) {
            if (!needsChessRobotTurn(mode, state, playerSide, result)) return@LaunchedEffect
            val requestedState = state
            val requestedRepetitions = repetitions
            val requestedGeneration = searchGeneration
            val robotMove = withContext(Dispatchers.Default) {
                ChessAi.chooseMove(
                    requestedState,
                    intelligenceLevel,
                    repetitionCounts = requestedRepetitions,
                    shouldStop = { !isActive }
                )
            }
            if (
                robotMove == null ||
                !shouldApplyChessRobotResult(
                    requestedState,
                    requestedGeneration,
                    state,
                    searchGeneration,
                    result
                )
            ) {
                return@LaunchedEffect
            }
            applyMove(robotMove)
        }

        val inCheck = result == null && ChessRules.isInCheck(state, state.sideToMove)
        val selectedMoves = if (selected == null) {
            emptyList()
        } else {
            legalMoves.filter { it.from == selected }
        }
        Surface(Modifier.fillMaxSize()) {
            ChessGameLayout(
                state = state,
                selected = selected,
                lastMove = lastMove,
                legalDestinations = selectedMoves,
                status = if (mode == GameMode.SINGLE_PLAYER) {
                    chessStatusText(result, robotThinking, playerSide, intelligenceLevel)
                } else {
                    chessStatusText(result, false, state.sideToMove, intelligenceLevel)
                        .takeIf { result != null } ?: "本地双人对战"
                },
                score = score.displayText(mode),
                intelligenceLevel = if (mode == GameMode.SINGLE_PLAYER) intelligenceLevel else null,
                result = result,
                inCheck = inCheck,
                canUndo = history.isNotEmpty(),
                rotateBoard = shouldRotateChessBoard(mode, playerSide),
                pieceTextures = pieceTextures,
                onTap = ::tap,
                onUndo = ::undo,
                onRestart = ::restart,
                onReturn = context::returnToGameMain
            )
        }
        if (pendingPromotionMoves.isNotEmpty()) {
            ChessPromotionDialog(
                side = state.sideToMove,
                choices = chessPromotionChoices(pendingPromotionMoves),
                pieceTextures = pieceTextures,
                onSelect = { type ->
                    chooseChessPromotion(pendingPromotionMoves, type)?.let(::commitPlayerMove)
                },
                onDismiss = { pendingPromotionMoves = emptyList() }
            )
        }
    }

    companion object {
        val manifest = GameManifest(
            gameId = "chess",
            displayName = "国际象棋",
            versionCode = 8,
            versionName = "0.0.8",
            entryClass = "com.buddygames.chess.ChessPlugin",
            minShellApi = 1,
            icon = "assets/icon.png"
        )
    }
}

@Composable
private fun rememberChessPieceTextures(context: GameContext) = remember(
    context.gamePackage.rootDir,
    context.gamePackage.manifest.versionCode
) {
    loadChessPieceTextures(context.gamePackage.assetsDir)
}

internal fun chessStatusText(
    result: ChessResult?,
    robotThinking: Boolean,
    playerSide: ChessSide,
    intelligenceLevel: Int
): String {
    return when (result) {
        ChessResult.WHITE_WIN -> "白方胜"
        ChessResult.BLACK_WIN -> "黑方胜"
        ChessResult.DRAW_STALEMATE -> "逼和"
        ChessResult.DRAW_FIFTY_MOVE -> "五十回合和棋"
        ChessResult.DRAW_REPETITION -> "三次重复和棋"
        ChessResult.DRAW_INSUFFICIENT_MATERIAL -> "子力不足和棋"
        null -> if (robotThinking) {
            "智能思考中 · 等级 $intelligenceLevel"
        } else {
            "你的回合 · 执${if (playerSide == ChessSide.WHITE) "白" else "黑"}"
        }
    }
}
