package com.buddygames.junqi

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

internal data class JunqiTextureSet(
    val board: ImageBitmap?,
    val icon: ImageBitmap?,
    val shelf: ImageBitmap?,
)

internal data class JunqiTextureMetadata(
    val width: Int,
    val height: Int,
    val mimeType: String?,
)

internal interface JunqiTextureDecoder<Texture> {
    fun decodeBounds(file: File): JunqiTextureMetadata?
    fun decode(file: File): Texture?
    fun dimensions(texture: Texture): Pair<Int, Int>
}

internal fun loadJunqiTextures(assetsDirectory: File): JunqiTextureSet = JunqiTextureSet(
    board = decodeJunqiTexture(
        assetsDirectory.resolve(JunqiVisuals.BOARD_TEXTURE_PATH),
        JunqiVisuals.BOARD_TEXTURE_CONTRACT,
    ),
    icon = decodeJunqiTexture(
        assetsDirectory.resolve(JunqiVisuals.ICON_TEXTURE_PATH),
        JunqiVisuals.ICON_TEXTURE_CONTRACT,
    ),
    shelf = decodeJunqiTexture(
        assetsDirectory.resolve(JunqiVisuals.SHELF_TEXTURE_PATH),
        JunqiVisuals.SHELF_TEXTURE_CONTRACT,
    ),
)

internal fun <Texture> loadJunqiTexture(
    file: File,
    contract: JunqiTextureContract,
    decoder: JunqiTextureDecoder<Texture>,
): Texture? = runCatching {
    if (!file.isFile) return@runCatching null
    val bounds = decoder.decodeBounds(file) ?: return@runCatching null
    if (
        bounds.mimeType != PNG_MIME_TYPE ||
        bounds.width != contract.width ||
        bounds.height != contract.height
    ) {
        return@runCatching null
    }
    decoder.decode(file)?.takeIf { texture ->
        decoder.dimensions(texture) == contract.width to contract.height
    }
}.getOrNull()

private fun decodeJunqiTexture(file: File, contract: JunqiTextureContract): ImageBitmap? =
    loadJunqiTexture(file, contract, BitmapFactoryJunqiTextureDecoder)?.asImageBitmap()

private object BitmapFactoryJunqiTextureDecoder : JunqiTextureDecoder<Bitmap> {
    override fun decodeBounds(file: File): JunqiTextureMetadata? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, options)
        return JunqiTextureMetadata(
            width = options.outWidth,
            height = options.outHeight,
            mimeType = options.outMimeType,
        )
    }

    override fun decode(file: File): Bitmap? = BitmapFactory.decodeFile(file.path)

    override fun dimensions(texture: Bitmap): Pair<Int, Int> = texture.width to texture.height
}

private const val PNG_MIME_TYPE = "image/png"
