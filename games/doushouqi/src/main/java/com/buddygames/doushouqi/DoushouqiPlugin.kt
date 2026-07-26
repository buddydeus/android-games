package com.buddygames.doushouqi

import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import com.buddygames.api.GameContext
import com.buddygames.api.GameManifest
import com.buddygames.api.GameMode
import com.buddygames.api.GamePlugin
import java.util.concurrent.Executors
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

class DoushouqiPlugin : GamePlugin {
    override fun getManifest(): GameManifest = DoushouqiManifest.gameManifest

    @Composable
    override fun MainScreen(context: GameContext) {
        val icon = remember(
            context.gamePackage.rootDir,
            context.gamePackage.manifest.versionCode,
        ) {
            runCatching {
                requireNotNull(
                    BitmapFactory.decodeFile(
                        context.gamePackage.assetsDir.resolve("icon.png").absolutePath,
                    ),
                ).asImageBitmap()
            }.getOrNull()
        }
        DoushouqiMenu(
            versionName = context.gamePackage.manifest.versionName,
            icon = icon,
            onSingle = { context.startGame(GameMode.SINGLE_PLAYER) },
            onTwo = { context.startGame(GameMode.TWO_PLAYERS) },
            onExit = context::exitGame,
        )
    }

    @Composable
    override fun GameScreen(context: GameContext, mode: GameMode) {
        val sessionMode = if (mode == GameMode.SINGLE_PLAYER) {
            DoushouqiMode.SINGLE_PLAYER
        } else {
            DoushouqiMode.TWO_PLAYERS
        }
        val session = remember(mode) { DoushouqiSession(sessionMode) }
        var projection by remember(mode) { mutableStateOf(session.state()) }
        var selected by remember(mode) { mutableStateOf<DoushouqiPosition?>(null) }
        val executor = remember {
            Executors.newSingleThreadExecutor { runnable ->
                Thread(runnable, "doushouqi-ai").apply { isDaemon = true }
            }
        }
        val dispatcher = remember(executor) { executor.asCoroutineDispatcher() }
        DisposableEffect(dispatcher) {
            onDispose {
                session.invalidate()
                dispatcher.close()
                executor.shutdownNow()
            }
        }

        val request = projection.robotRequest
        LaunchedEffect(request?.generation, request?.sourcePositionKey) {
            if (request == null) return@LaunchedEffect
            val move = withContext(dispatcher) {
                DoushouqiAi.chooseMove(
                    request.state,
                    request.level,
                    shouldStop = { !isActive },
                )
            } ?: return@LaunchedEffect
            projection = session.applyRobotMove(request, move)
            selected = null
        }

        fun tap(position: DoushouqiPosition) {
            if (projection.robotRequest != null || projection.position.result != null) return
            val current = selected
            if (current != null) {
                val move = doushouqiTapMove(projection.position, current, position)
                if (move != null) {
                    projection = session.play(move)
                    selected = null
                    return
                }
            }
            selected = position.takeIf {
                projection.position.pieceAt(it)?.side == projection.position.sideToMove
            }
        }

        val legalMoves = selected?.let { from ->
            DoushouqiRules.legalMoves(projection.position).filter { it.from == from }
        } ?: emptyList()
        Surface(Modifier.fillMaxSize(), color = DoushouqiCanvas) {
            DoushouqiGameLayout(
                session = projection,
                mode = sessionMode,
                selected = selected,
                legalMoves = legalMoves,
                robotThinking = request != null,
                onTap = ::tap,
                onUndo = {
                    projection = session.undo()
                    selected = null
                },
                onRestart = {
                    projection = session.restart()
                    selected = null
                },
                onReturn = {
                    session.invalidate()
                    context.returnToGameMain()
                },
            )
        }
    }
}
