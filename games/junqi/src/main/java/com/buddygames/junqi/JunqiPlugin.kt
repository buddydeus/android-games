package com.buddygames.junqi

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
import com.buddygames.api.GameContext
import com.buddygames.api.GameManifest
import com.buddygames.api.GameMode
import com.buddygames.api.GamePlugin
import java.util.concurrent.Executors
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

class JunqiPlugin : GamePlugin {
    override fun getManifest(): GameManifest = manifest

    @Composable
    override fun MainScreen(context: GameContext) {
        val textures = rememberJunqiTextures(context)
        JunqiMenu(
            textures = textures,
            versionName = context.gamePackage.manifest.versionName,
            onSingle = { context.startGame(GameMode.SINGLE_PLAYER) },
            onTwoPlayers = { context.startGame(GameMode.TWO_PLAYERS) },
            onExit = context::exitGame,
        )
    }

    @Composable
    override fun GameScreen(context: GameContext, mode: GameMode) {
        val textures = rememberJunqiTextures(context)
        val session = remember(mode) { JunqiSession(mode.toJunqiMode()) }
        var state by remember(session) { mutableStateOf(session.state) }
        var selected by remember(session) { mutableStateOf<JunqiPosition?>(null) }

        val robotExecutor = remember(session) {
            Executors.newSingleThreadExecutor { runnable ->
                Thread(runnable, "junqi-ai").apply { isDaemon = true }
            }
        }
        val robotDispatcher = remember(robotExecutor) { robotExecutor.asCoroutineDispatcher() }
        DisposableEffect(robotDispatcher) {
            onDispose {
                robotDispatcher.close()
                robotExecutor.shutdownNow()
            }
        }

        val robotRequest = session.robotRequest
        LaunchedEffect(robotRequest) {
            val request = robotRequest ?: return@LaunchedEffect
            val move = withContext(robotDispatcher) {
                JunqiAi.chooseMove(
                    observation = request.observation,
                    knowledge = request.knowledge,
                    level = request.level,
                )
            }
            if (
                !isActive ||
                session.robotRequest != request ||
                state.robotRequestGeneration != request.generation
            ) {
                return@LaunchedEffect
            }
            state = session.applyRobotMove(request, move)
            selected = null
        }

        val observation = state.observation
        val legalDestinations = remember(observation, selected) {
            if (
                observation == null ||
                selected == null ||
                observation.currentSide != observation.viewer ||
                observation.result != null
            ) {
                emptySet()
            } else {
                observation.visibleLegalMoves()
                    .asSequence()
                    .filter { move -> move.from == selected }
                    .mapTo(linkedSetOf()) { move -> move.to }
            }
        }

        fun update(nextState: JunqiSessionState) {
            state = nextState
            selected = null
        }

        fun tapBoard(position: JunqiPosition) {
            when (state.phase) {
                JunqiPhase.DEPLOYMENT -> {
                    if (state.deployment.any { piece -> piece.position == position }) {
                        state = session.selectDeploymentPiece(position)
                    }
                }
                JunqiPhase.PLAYING -> {
                    val visible = state.observation ?: return
                    val resolution = resolveJunqiBoardTap(visible, selected, position)
                    selected = resolution.selection
                    resolution.move?.let { move -> update(session.play(move)) }
                }
                else -> Unit
            }
        }

        Surface(Modifier.fillMaxSize()) {
            JunqiGameLayout(
                state = state,
                textures = textures,
                selected = if (state.phase == JunqiPhase.DEPLOYMENT) {
                    state.selectedDeploymentPiece
                } else {
                    selected
                },
                legalDestinations = legalDestinations,
                robotThinking = state.robotRequestGeneration != null,
                onBoardTap = ::tapBoard,
                onRandomize = { update(session.randomizeDeployment()) },
                onResetDeployment = { update(session.resetDeployment()) },
                onReady = { update(session.ready()) },
                onAcceptHandoff = { update(session.acceptHandoff()) },
                onAcknowledgeBattle = { update(session.acknowledgeBattle()) },
                onUndo = { update(session.undo()) },
                onRestart = { update(session.restart()) },
                onReturn = context::returnToGameMain,
            )
        }
    }

    companion object {
        val manifest = GameManifest(
            gameId = "junqi",
            displayName = "军棋",
            versionCode = JUNQI_VERSION_CODE,
            versionName = JUNQI_VERSION_NAME,
            entryClass = "com.buddygames.junqi.JunqiPlugin",
            minShellApi = 1,
            orientation = "landscape",
            icon = "assets/icon.png",
        )
    }
}

@Composable
private fun rememberJunqiTextures(context: GameContext): JunqiTextureSet = remember(
    context.gamePackage.rootDir,
    context.gamePackage.manifest.versionCode,
) {
    loadJunqiTextures(context.gamePackage.assetsDir)
}

private fun GameMode.toJunqiMode(): JunqiMode = when (this) {
    GameMode.SINGLE_PLAYER -> JunqiMode.SINGLE_PLAYER
    GameMode.TWO_PLAYERS -> JunqiMode.TWO_PLAYERS
}
