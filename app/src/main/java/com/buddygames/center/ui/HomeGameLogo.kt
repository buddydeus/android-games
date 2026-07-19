package com.buddygames.center.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buddygames.api.GamePackage
import java.io.File
import java.util.Locale

private val LogoWell = Color(0xFFE5ECE9)
private val LogoBorder = Color(0xFFAEBDB9)
private val LogoRim = Color.White.copy(alpha = 0.72f)
private val LogoInk = Color(0xFF242A2C)
private const val MAX_TEXT_LOGO_BYTES = 16 * 1024
private const val MAX_TEXT_LOGO_CHARACTERS = 6
private const val MAX_DECODED_LOGO_DIMENSION = 512

internal sealed interface HomeGameLogoContent {
    data class Text(val value: String) : HomeGameLogoContent
    data class BitmapFile(val file: File) : HomeGameLogoContent
}

internal fun loadHomeGameLogo(gamePackage: GamePackage): HomeGameLogoContent {
    val fallback = HomeGameLogoContent.Text(
        gamePackage.manifest.displayName.trim().take(1).ifEmpty { "游" }
    )
    val iconPath = gamePackage.manifest.icon?.trim()?.takeIf { it.isNotEmpty() }
        ?: return fallback

    return runCatching {
        val packageRoot = gamePackage.rootDir.canonicalFile
        val iconFile = packageRoot.resolve(iconPath).canonicalFile
        if (!iconFile.toPath().startsWith(packageRoot.toPath()) || !iconFile.isFile) {
            return fallback
        }

        when (iconFile.extension.lowercase(Locale.ROOT)) {
            "txt" -> {
                if (iconFile.length() > MAX_TEXT_LOGO_BYTES) {
                    return fallback
                }
                val text = iconFile.readText().trim().take(MAX_TEXT_LOGO_CHARACTERS)
                if (text.isEmpty()) fallback else HomeGameLogoContent.Text(text)
            }
            "png", "webp", "jpg", "jpeg" -> HomeGameLogoContent.BitmapFile(iconFile)
            else -> fallback
        }
    }.getOrDefault(fallback)
}

@Composable
internal fun HomeGameLogoMark(
    gamePackage: GamePackage,
    logoSizeDp: Int,
    modifier: Modifier = Modifier
) {
    val logoContent = remember(
        gamePackage.rootDir,
        gamePackage.manifest.icon,
        gamePackage.manifest.versionCode
    ) {
        loadHomeGameLogo(gamePackage)
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(LogoWell)
            .border(1.dp, LogoBorder, CircleShape)
            .padding(2.dp)
            .border(1.dp, LogoRim, CircleShape)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        when (logoContent) {
            is HomeGameLogoContent.Text -> TextLogo(
                text = logoContent.value,
                logoSizeDp = logoSizeDp
            )
            is HomeGameLogoContent.BitmapFile -> {
                val bitmap = remember(
                    logoContent.file.path,
                    logoContent.file.lastModified(),
                    logoContent.file.length()
                ) {
                    decodePackageLogo(logoContent.file)
                }
                if (bitmap == null) {
                    TextLogo(
                        text = gamePackage.manifest.displayName.trim().take(1).ifEmpty { "游" },
                        logoSizeDp = logoSizeDp
                    )
                } else {
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

@Composable
private fun TextLogo(text: String, logoSizeDp: Int) {
    val scale = when (text.length) {
        1 -> 0.48f
        2, 3 -> 0.34f
        else -> 0.16f
    }
    Text(
        text = text,
        color = LogoInk,
        fontSize = (logoSizeDp * scale).toInt().coerceIn(10, 54).sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1
    )
}

private fun decodePackageLogo(file: File) = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        return@runCatching null
    }

    var sampleSize = 1
    while (
        bounds.outWidth / sampleSize > MAX_DECODED_LOGO_DIMENSION ||
        bounds.outHeight / sampleSize > MAX_DECODED_LOGO_DIMENSION
    ) {
        sampleSize *= 2
    }
    BitmapFactory.decodeFile(
        file.path,
        BitmapFactory.Options().apply { inSampleSize = sampleSize }
    )?.asImageBitmap()
}.getOrNull()
