package com.buddygames.api

import androidx.compose.runtime.Composable
import java.io.File

const val CURRENT_SHELL_API: Int = 1

enum class GameMode {
    SINGLE_PLAYER,
    TWO_PLAYERS
}

data class GameManifest(
    val schemaVersion: Int = 1,
    val gameId: String,
    val displayName: String,
    val versionCode: Int,
    val versionName: String,
    val entryClass: String,
    val minShellApi: Int,
    val orientation: String = "landscape",
    val icon: String? = null
) {
    fun isValidForShell(shellApi: Int): Boolean {
        return schemaVersion == 1 &&
            gameId.matches(Regex("[a-z][a-z0-9_-]{1,63}")) &&
            displayName.isNotBlank() &&
            versionCode > 0 &&
            versionName.isNotBlank() &&
            entryClass.isNotBlank() &&
            minShellApi <= shellApi &&
            orientation == "landscape" &&
            icon?.startsWith("/") != true &&
            icon?.contains("..") != true
    }
}

data class GamePackage(
    val manifest: GameManifest,
    val rootDir: File,
    val pluginApk: File,
    val assetsDir: File
)

interface GameContext {
    val gamePackage: GamePackage
    fun startGame(mode: GameMode)
    fun exitGame()
    fun returnToGameMain()
    fun log(message: String)
}

interface GamePlugin {
    fun getManifest(): GameManifest

    @Composable
    fun MainScreen(context: GameContext)

    @Composable
    fun GameScreen(context: GameContext, mode: GameMode)
}

