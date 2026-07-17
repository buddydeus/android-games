package com.buddygames.center.ui

import android.animation.ValueAnimator
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buddygames.api.GameContext
import com.buddygames.api.GameMode
import com.buddygames.api.GamePackage
import com.buddygames.api.GamePlugin
import com.buddygames.center.loader.DexGamePluginLoader
import com.buddygames.center.packages.GamePackageRepository
import java.io.File

private val MineralCanvas = Color(0xFFDDE5E3)
private val ButtonTop = Color(0xFFFCFDFB)
private val ButtonBottom = Color(0xFFF0F4F1)
private val PressedSurface = Color(0xFFD8E3E0)
private val Ink = Color(0xFF17282C)
private val MutedInk = Color(0xFF56686C)
private val PrimaryTeal = Color(0xFF185864)
private val Border = Color(0xFFAEBDB9)
private val FocusRing = Color(0xFF08758A)
private val Success = Color(0xFF2E715E)
private val SuccessSoft = Color(0xFFE0EEE7)
private val Warning = Color(0xFF98551F)
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
            surface = ButtonTop,
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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MineralCanvas)
    ) {
        val layout = remember(maxWidth, maxHeight) {
            homeGameLayout(
                widthDp = maxWidth.value,
                heightDp = maxHeight.value
            )
        }
        val sortedPackages = remember(packages) {
            packages.sortedWith(
                compareBy<GamePackage>(
                    { homeGamePresentation(it.manifest.gameId).order },
                    { it.manifest.displayName }
                )
            )
        }
        val compact = layout.mode == HomeGameLayoutMode.CompactColumn

        if (mineralTexture != null) {
            Image(
                bitmap = mineralTexture,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.06f
            )
        }

        Column(Modifier.fillMaxSize()) {
            HomeTopBar(onImport = onImport, compact = compact)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = layout.horizontalPaddingDp.dp,
                        end = layout.horizontalPaddingDp.dp,
                        bottom = 32.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(if (compact) 36.dp else 72.dp))
                Text(
                    text = "选择游戏",
                    modifier = if (compact) Modifier.fillMaxWidth() else Modifier,
                    color = Ink,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (message != null) {
                    Spacer(Modifier.height(20.dp))
                    ImportMessageBar(message)
                }
                Spacer(Modifier.height(if (compact) 32.dp else 48.dp))

                if (sortedPackages.isEmpty()) {
                    EmptyGameState(onImport)
                } else if (compact) {
                    CompactGameButtons(
                        packages = sortedPackages,
                        layout = layout,
                        onOpen = onOpen
                    )
                } else {
                    SquareGameButtons(
                        packages = sortedPackages,
                        layout = layout,
                        onOpen = onOpen
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeTopBar(
    onImport: () -> Unit,
    compact: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .shadow(2.dp)
            .background(ButtonTop)
            .border(1.dp, Border)
            .padding(horizontal = if (compact) 24.dp else 32.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "游戏中心",
            color = Ink,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )
        ImportButton(
            label = if (compact) "导入" else "导入游戏包",
            onClick = onImport
        )
    }
}

@Composable
private fun SquareGameButtons(
    packages: List<GamePackage>,
    layout: HomeGameLayout,
    onOpen: (GamePackage) -> Unit
) {
    val buttonSize = requireNotNull(layout.buttonSizeDp).dp
    Column(verticalArrangement = Arrangement.spacedBy(layout.gapDp.dp)) {
        packages.chunked(3).forEach { rowPackages ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(
                    layout.gapDp.dp,
                    Alignment.CenterHorizontally
                )
            ) {
                rowPackages.forEach { gamePackage ->
                    GameSelectionButton(
                        gamePackage = gamePackage,
                        logoSizeDp = layout.logoSizeDp,
                        horizontalContent = false,
                        modifier = Modifier.size(buttonSize),
                        onClick = { onOpen(gamePackage) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactGameButtons(
    packages: List<GamePackage>,
    layout: HomeGameLayout,
    onOpen: (GamePackage) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(layout.gapDp.dp)
    ) {
        packages.forEach { gamePackage ->
            GameSelectionButton(
                gamePackage = gamePackage,
                logoSizeDp = layout.logoSizeDp,
                horizontalContent = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(layout.buttonHeightDp.dp),
                onClick = { onOpen(gamePackage) }
            )
        }
    }
}

@Composable
private fun GameSelectionButton(
    gamePackage: GamePackage,
    logoSizeDp: Int,
    horizontalContent: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val presentation = homeGamePresentation(gamePackage.manifest.gameId)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    val animationsEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    val duration = if (pressed) 140 else 180
    val scale by animateFloatAsState(
        targetValue = if (pressed && animationsEnabled) 0.985f else 1f,
        animationSpec = if (animationsEnabled) tween(duration) else snap(),
        label = "game-button-scale"
    )
    val topColor by animateColorAsState(
        targetValue = if (pressed) PressedSurface else ButtonTop,
        animationSpec = if (animationsEnabled) tween(duration) else snap(),
        label = "game-button-top"
    )
    val bottomColor by animateColorAsState(
        targetValue = if (pressed) PressedSurface else ButtonBottom,
        animationSpec = if (animationsEnabled) tween(duration) else snap(),
        label = "game-button-bottom"
    )
    val ambientElevation by animateDpAsState(
        targetValue = if (pressed) 2.dp else 12.dp,
        animationSpec = if (animationsEnabled) tween(duration) else snap(),
        label = "game-button-ambient-elevation"
    )
    val contactElevation by animateDpAsState(
        targetValue = if (pressed) 1.dp else 3.dp,
        animationSpec = if (animationsEnabled) tween(duration) else snap(),
        label = "game-button-contact-elevation"
    )
    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(ambientElevation, shape, clip = false)
            .then(
                if (focused) {
                    Modifier.border(3.dp, FocusRing, shape)
                } else {
                    Modifier
                }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(contactElevation, shape, clip = false)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(topColor, bottomColor)))
                .border(1.dp, Border, shape)
                .semantics(mergeDescendants = true) {
                    contentDescription = "打开 ${gamePackage.manifest.displayName}"
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(1.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.66f), shape),
                contentAlignment = Alignment.Center
            ) {
                if (horizontalContent) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HomeGameLogoMark(
                            logo = presentation.logo,
                            fallbackSymbol = gamePackage.manifest.displayName,
                            logoSizeDp = logoSizeDp,
                            modifier = Modifier.size(logoSizeDp.dp)
                        )
                        Spacer(Modifier.width(32.dp))
                        Text(
                            text = gamePackage.manifest.displayName,
                            modifier = Modifier.weight(1f),
                            color = Ink,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        HomeGameLogoMark(
                            logo = presentation.logo,
                            fallbackSymbol = gamePackage.manifest.displayName,
                            logoSizeDp = logoSizeDp,
                            modifier = Modifier.size(logoSizeDp.dp)
                        )
                        Spacer(Modifier.height(22.dp))
                        Text(
                            text = gamePackage.manifest.displayName,
                            modifier = Modifier.fillMaxWidth(),
                            color = Ink,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyGameState(onImport: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "还没有游戏。导入本地游戏包开始。",
            color = MutedInk,
            fontSize = 16.sp
        )
        Spacer(Modifier.height(16.dp))
        ImportButton(label = "导入游戏包", onClick = onImport)
    }
}

@Composable
private fun ImportButton(
    label: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .height(48.dp)
            .semantics { contentDescription = "导入游戏包" },
        interactionSource = interactionSource,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (focused) 3.dp else 2.dp,
            color = if (focused) FocusRing else PrimaryTeal
        ),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryTeal)
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
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
            .semantics {
                contentDescription = message
                liveRegion = LiveRegionMode.Polite
            },
        color = foreground,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium
    )
}

private fun loadAppTexture(context: android.content.Context, path: String): ImageBitmap? = runCatching {
    context.assets.open(path).use { input ->
        requireNotNull(BitmapFactory.decodeStream(input)).asImageBitmap()
    }
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
