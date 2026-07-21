package com.buddygames.junqi

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

internal data class JunqiTextureSet(
    val board: ImageBitmap?,
    val icon: ImageBitmap?,
    val shelf: ImageBitmap?,
)

internal fun loadJunqiTextures(assetsDirectory: File): JunqiTextureSet = JunqiTextureSet(
    board = decodeJunqiTexture(
        assetsDirectory.resolve(JunqiVisuals.BOARD_TEXTURE_PATH),
        maxDimension = 2_048,
    ),
    icon = decodeJunqiTexture(
        assetsDirectory.resolve(JunqiVisuals.ICON_TEXTURE_PATH),
        maxDimension = 512,
    ),
    shelf = decodeJunqiTexture(
        assetsDirectory.resolve(JunqiVisuals.SHELF_TEXTURE_PATH),
        maxDimension = 1_024,
    ),
)

private fun decodeJunqiTexture(file: File, maxDimension: Int): ImageBitmap? = runCatching {
    if (!file.isFile) return@runCatching null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

    var sampleSize = 1
    while (
        bounds.outWidth / sampleSize > maxDimension ||
        bounds.outHeight / sampleSize > maxDimension
    ) {
        sampleSize *= 2
    }
    BitmapFactory.decodeFile(
        file.path,
        BitmapFactory.Options().apply { inSampleSize = sampleSize },
    )?.asImageBitmap()
}.getOrNull()
