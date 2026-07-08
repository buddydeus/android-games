package com.buddygames.center.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.buddygames.api.GameContext
import com.buddygames.api.GameMode
import com.buddygames.api.GamePackage
import com.buddygames.api.GamePlugin
import com.buddygames.center.loader.DexGamePluginLoader
import com.buddygames.center.packages.GamePackageRepository
import com.buddygames.center.registry.BuiltInGameRegistry
import java.io.File

@Composable
fun GameCenterApp() {
    val androidContext = LocalContext.current
    val repository = remember { GamePackageRepository(androidContext.filesDir) }
    val pluginLoader = remember { DexGamePluginLoader(androidContext.codeCacheDir) }
    var packages by remember {
        mutableStateOf(
            run {
                repository.ensureBuiltInPackagesInstalled(BuiltInGameRegistry.packageDefinitions())
                repository.discoverInstalledGames()
            }
        )
    }
    var activePackage by remember { mutableStateOf<GamePackage?>(null) }
    var activePlugin by remember { mutableStateOf<GamePlugin?>(null) }
    var activeMode by remember { mutableStateOf<GameMode?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val tempZip = File(androidContext.cacheDir, "imported-game.zip")
            androidContext.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "无法读取游戏包" }
                tempZip.outputStream().use { output -> input.copyTo(output) }
            }
            repository.installFromZip(tempZip)
            packages = repository.discoverInstalledGames()
            message = "游戏包导入完成"
        }.onFailure {
            message = "导入失败：${it.message}"
        }
    }

    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = Color(0xFFF6F7F1)) {
            val currentPackage = activePackage
            val currentPlugin = activePlugin
            if (currentPackage == null || currentPlugin == null) {
                GameCenterHome(
                    packages = packages,
                    message = message,
                    onImport = { launcher.launch(arrayOf("application/zip", "application/octet-stream")) },
                    onOpen = { gamePackage ->
                        runCatching {
                            BuiltInGameRegistry.pluginFor(gamePackage.manifest.gameId)
                                ?: pluginLoader.load(gamePackage)
                        }.onSuccess { plugin ->
                            activePackage = gamePackage
                            activePlugin = plugin
                            activeMode = null
                            message = null
                        }.onFailure {
                            message = "加载失败：${it.message}"
                        }
                    }
                )
            } else {
                val shellContext = remember(currentPackage, currentPlugin) {
                    ShellGameContext(
                        gamePackage = currentPackage,
                        onStartGame = { activeMode = it },
                        onExitGame = {
                            activePackage = null
                            activePlugin = null
                            activeMode = null
                        },
                        onReturnToGameMain = { activeMode = null }
                    )
                }
                val mode = activeMode
                if (mode == null) {
                    currentPlugin.MainScreen(shellContext)
                } else {
                    currentPlugin.GameScreen(shellContext, mode)
                }
            }
        }
    }
}

@Composable
private fun GameCenterHome(
    packages: List<GamePackage>,
    message: String?,
    onImport: () -> Unit,
    onOpen: (GamePackage) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(28.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("单机游戏中心", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text("Android Pad / 本地游戏包", style = MaterialTheme.typography.titleMedium, color = Color(0xFF5B6472))
            }
            Button(onClick = onImport) { Text("导入游戏包") }
        }
        if (message != null) {
            Spacer(Modifier.height(12.dp))
            Text(message, color = Color(0xFF245C39), style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(Modifier.height(24.dp))
        if (packages.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Button(onClick = onImport) { Text("导入第一个游戏包") }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(220.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(packages, key = { it.manifest.gameId }) { gamePackage ->
                    GameTile(gamePackage, onClick = { onOpen(gamePackage) })
                }
            }
        }
    }
}

@Composable
private fun GameTile(gamePackage: GamePackage, onClick: () -> Unit) {
    Card(
        modifier = Modifier.size(width = 220.dp, height = 150.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(
                modifier = Modifier.size(48.dp).background(Color(0xFF274C77)),
                contentAlignment = Alignment.Center
            ) {
                Text(gamePackage.manifest.displayName.take(1), color = Color.White, fontWeight = FontWeight.Bold)
            }
            Column {
                Text(gamePackage.manifest.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("v${gamePackage.manifest.versionName}", color = Color(0xFF5B6472))
            }
        }
    }
}

private class ShellGameContext(
    override val gamePackage: GamePackage,
    private val onStartGame: (GameMode) -> Unit,
    private val onExitGame: () -> Unit,
    private val onReturnToGameMain: () -> Unit
) : GameContext {
    override fun startGame(mode: GameMode) = onStartGame(mode)
    override fun exitGame() = onExitGame()
    override fun returnToGameMain() = onReturnToGameMain()
    override fun log(message: String) {
        android.util.Log.d("GameCenter", message)
    }
}
