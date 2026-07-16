package com.buddygames.center.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.buddygames.api.GameContext
import com.buddygames.api.GameMode
import com.buddygames.api.GamePackage
import com.buddygames.api.GamePlugin
import com.buddygames.center.loader.DexGamePluginLoader
import com.buddygames.center.packages.GamePackageRepository
import java.io.File

private val MineralCanvas = Color(0xFFDCE2E2)
private val RaisedSurface = Color(0xFFF8FAF8)
private val InsetSurface = Color(0xFFB8C3C4)
private val Ink = Color(0xFF1D2B2F)
private val MutedInk = Color(0xFF5D6E72)
private val PrimaryTeal = Color(0xFF164C5B)
private val Border = Color(0xFFB4C0C1)
private val Success = Color(0xFF2E715E)
private val SuccessSoft = Color(0xFFE0EEE7)
private val Warning = Color(0xFF9C5B22)
private val WarningSoft = Color(0xFFF4EADB)
private val Error = Color(0xFFA43B32)
private val ErrorSoft = Color(0xFFF1DDDD)

@Composable
fun GameCenterApp() {
    val androidContext = LocalContext.current
    val repository = remember { GamePackageRepository(androidContext.filesDir) }
    val pluginLoader = remember { DexGamePluginLoader(androidContext.codeCacheDir) }
    var packages by remember {
        mutableStateOf(
            run {
                installBundledGames(androidContext, repository)
                repository.discoverInstalledGames()
            }
        )
    }
    var activePackage by remember { mutableStateOf<GamePackage?>(null) }
    var activePlugin by remember { mutableStateOf<GamePlugin?>(null) }
    var activeMode by remember { mutableStateOf<GameMode?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            message = "未选择文件。请选择本地 .zip 游戏包。"
            return@rememberLauncherForActivityResult
        }
        runCatching {
            val tempZip = File(androidContext.cacheDir, "imported-game.zip")
            androidContext.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "无法读取游戏包" }
                tempZip.outputStream().use { output -> input.copyTo(output) }
            }
            repository.installFromZip(tempZip)
            packages = repository.discoverInstalledGames()
            message = "游戏包导入完成，可在本地游戏中启动。"
        }.onFailure {
            message = "导入失败：${it.message}"
        }
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = PrimaryTeal,
            surface = RaisedSurface,
            background = MineralCanvas,
            onSurface = Ink
        )
    ) {
        Surface(Modifier.fillMaxSize(), color = MineralCanvas) {
            val currentPackage = activePackage
            val currentPlugin = activePlugin
            if (currentPackage == null || currentPlugin == null) {
                GameCenterHome(
                    packages = packages,
                    message = message,
                    onImport = { launcher.launch(arrayOf("application/zip", "application/octet-stream")) },
                    onOpen = { gamePackage ->
                        runCatching {
                            pluginLoader.load(gamePackage)
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
private fun installBundledGames(androidContext: android.content.Context, repository: GamePackageRepository) {
    val assetManager = androidContext.assets
    val packageNames = assetManager.list("builtin-games").orEmpty().filter { it.endsWith(".zip") }
    packageNames.forEach { name ->
        val tempZip = File(androidContext.cacheDir, "builtin-$name")
        assetManager.open("builtin-games/$name").use { input ->
            tempZip.outputStream().use { output -> input.copyTo(output) }
        }
        runCatching { repository.installFromZip(tempZip) }
            .onFailure { android.util.Log.w("GameCenter", "Skipping bundled game $name: ${it.message}") }
    }
}

@Composable
private fun GameCenterHome(
    packages: List<GamePackage>,
    message: String?,
    onImport: () -> Unit,
    onOpen: (GamePackage) -> Unit
) {
    val context = LocalContext.current
    val mineralTexture = remember { loadAppTexture(context, "textures/mineral-slate.png") }
    Box(modifier = Modifier.fillMaxSize().background(MineralCanvas)) {
        if (mineralTexture != null) {
            Image(
                bitmap = mineralTexture,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.32f
            )
        }
        Column(Modifier.fillMaxSize()) {
            HomeTopBar(onImport)
            Column(Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 28.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        "本地收藏",
                        color = MutedInk,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "本地棋局",
                        color = Ink,
                        style = MaterialTheme.typography.headlineLarge,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    "${packages.size} 个本地游戏",
                    color = MutedInk,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            if (message != null) {
                Spacer(Modifier.height(18.dp))
                ImportMessageBar(message)
            }
            Spacer(Modifier.height(20.dp))
            if (packages.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Button(
                        onClick = onImport,
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("导入第一个游戏包")
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 260.dp),
                    contentPadding = PaddingValues(bottom = 28.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    items(packages, key = { it.manifest.gameId }) { gamePackage ->
                        GameTile(gamePackage, onClick = { onOpen(gamePackage) })
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun HomeTopBar(onImport: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .background(RaisedSurface)
            .border(1.dp, Border),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(RaisedSurface)
                    .border(2.dp, PrimaryTeal, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("棋局", color = PrimaryTeal, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(14.dp))
            Text(
                "游戏中心",
                color = Ink,
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(18.dp))
            InstalledInlay()
        }
        Button(
            onClick = onImport,
            modifier = Modifier
                .padding(end = 28.dp)
                .height(48.dp)
                .semantics { contentDescription = "导入游戏包" },
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
        ) {
            Text("导入游戏包")
        }
    }
}

@Composable
private fun InstalledInlay() {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(PrimaryTeal.copy(alpha = 0.7f), CircleShape)
            )
        }
    }
}

@Composable
private fun ImportMessageBar(message: String) {
    val isError = message.startsWith("导入失败") || message.startsWith("加载失败")
    val isWarning = message.startsWith("未选择")
    val container = when {
        isError -> ErrorSoft
        isWarning -> WarningSoft
        else -> SuccessSoft
    }
    val foreground = when {
        isError -> Error
        isWarning -> Warning
        else -> Success
    }
    Text(
        text = message,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(container)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics { contentDescription = message },
        color = foreground,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun GameTile(gamePackage: GamePackage, onClick: () -> Unit) {
    val presentation = homeGamePresentation(gamePackage.manifest.gameId)
    val texture = remember(gamePackage.assetsDir, presentation.shelfTexture) {
        presentation.shelfTexture.takeIf { it.isNotEmpty() }
            ?.let { loadPackageTexture(gamePackage.assetsDir.resolve(it)) }
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(274.dp)
            .semantics { contentDescription = "启动 ${gamePackage.manifest.displayName}" }
            .clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = RaisedSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ShelfBoardSurface(texture, presentation)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    gamePackage.manifest.displayName,
                    color = Ink,
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "可启动",
                    color = Success,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SuccessSoft)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    "v${gamePackage.manifest.versionName} · ${presentation.boardSize}",
                    color = MutedInk,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    presentation.packageLabel,
                    color = MutedInk,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun ShelfBoardSurface(texture: ImageBitmap?, presentation: HomeGamePresentation) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(152.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(InsetSurface)
            .border(1.dp, Border, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (texture != null) {
            Image(
                bitmap = texture,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.92f
            )
        }
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(presentation.accent.copy(alpha = 0.9f))
                .border(2.dp, RaisedSurface.copy(alpha = 0.8f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                presentation.symbol,
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun loadAppTexture(context: android.content.Context, path: String): ImageBitmap? = runCatching {
    context.assets.open(path).use { input ->
        requireNotNull(BitmapFactory.decodeStream(input)).asImageBitmap()
    }
}.getOrNull()

private fun loadPackageTexture(file: File): ImageBitmap? = runCatching {
    requireNotNull(BitmapFactory.decodeFile(file.absolutePath)).asImageBitmap()
}.getOrNull()

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
