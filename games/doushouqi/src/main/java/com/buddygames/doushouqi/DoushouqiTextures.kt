package com.buddygames.doushouqi

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

internal data class DoushouqiTextureSet(
    val board: ImageBitmap?,
    val pieces: Map<Pair<DoushouqiSide, DoushouqiAnimal>, ImageBitmap>,
) {
    fun piece(piece: DoushouqiPiece): ImageBitmap? = pieces[piece.side to piece.animal]
}

internal data class DoushouqiTextureMetadata(
    val width: Int,
    val height: Int,
    val mimeType: String?,
)

internal interface DoushouqiTextureDecoder<Texture> {
    fun decodeBounds(file: File): DoushouqiTextureMetadata?
    fun decode(file: File): Texture?
    fun dimensions(texture: Texture): Pair<Int, Int>
}

internal fun loadDoushouqiTextures(packageRoot: File): DoushouqiTextureSet {
    val board = decodeDoushouqiTexture(
        packageRoot.resolve(DoushouqiVisuals.BOARD_TEXTURE),
        DoushouqiVisuals.BOARD_TEXTURE_SIZE,
        DoushouqiVisuals.BOARD_TEXTURE_SIZE,
    )
    val pieces = DoushouqiVisuals.PIECE_TEXTURES.mapNotNull { (piece, path) ->
        decodeDoushouqiTexture(
            packageRoot.resolve(path),
            DoushouqiVisuals.PIECE_TEXTURE_SIZE,
            DoushouqiVisuals.PIECE_TEXTURE_SIZE,
        )?.let { piece to it }
    }.toMap()
    return DoushouqiTextureSet(board, pieces)
}

internal fun <Texture> loadDoushouqiTexture(
    file: File,
    expectedWidth: Int,
    expectedHeight: Int,
    decoder: DoushouqiTextureDecoder<Texture>,
): Texture? = runCatching {
    if (!file.isFile) return@runCatching null
    val bounds = decoder.decodeBounds(file) ?: return@runCatching null
    if (
        bounds.mimeType != PNG_MIME_TYPE ||
        bounds.width != expectedWidth ||
        bounds.height != expectedHeight
    ) {
        return@runCatching null
    }
    decoder.decode(file)?.takeIf { texture ->
        decoder.dimensions(texture) == expectedWidth to expectedHeight
    }
}.getOrNull()

private fun decodeDoushouqiTexture(
    file: File,
    expectedWidth: Int,
    expectedHeight: Int,
): ImageBitmap? = loadDoushouqiTexture(
    file,
    expectedWidth,
    expectedHeight,
    BitmapFactoryDoushouqiTextureDecoder,
)?.asImageBitmap()

private object BitmapFactoryDoushouqiTextureDecoder : DoushouqiTextureDecoder<Bitmap> {
    override fun decodeBounds(file: File): DoushouqiTextureMetadata {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, options)
        return DoushouqiTextureMetadata(
            width = options.outWidth,
            height = options.outHeight,
            mimeType = options.outMimeType,
        )
    }

    override fun decode(file: File): Bitmap? = BitmapFactory.decodeFile(file.path)

    override fun dimensions(texture: Bitmap): Pair<Int, Int> = texture.width to texture.height
}

private const val PNG_MIME_TYPE = "image/png"
