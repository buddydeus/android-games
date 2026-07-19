package com.buddygames.xiangqi

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

internal const val XIANGQI_BOARD_TEXTURE_PATH = "board/xiangqi-board.png"
internal const val XIANGQI_BOARD_TEXTURE_WIDTH = 1600
internal const val XIANGQI_BOARD_TEXTURE_HEIGHT = 1500
internal const val XIANGQI_BOARD_ASPECT_RATIO =
    XIANGQI_BOARD_TEXTURE_WIDTH.toFloat() / XIANGQI_BOARD_TEXTURE_HEIGHT
internal const val XIANGQI_GRID_LEFT_FRACTION = 128f / XIANGQI_BOARD_TEXTURE_WIDTH
internal const val XIANGQI_GRID_TOP_FRACTION = 90f / XIANGQI_BOARD_TEXTURE_HEIGHT
internal const val XIANGQI_GRID_RIGHT_FRACTION = 1472f / XIANGQI_BOARD_TEXTURE_WIDTH
internal const val XIANGQI_GRID_BOTTOM_FRACTION = 1410f / XIANGQI_BOARD_TEXTURE_HEIGHT
internal const val XIANGQI_PIECE_TEXTURE_SCALE = 0.90f

private const val MAX_DECODED_PIECE_DIMENSION = 512
private const val MAX_DECODED_BOARD_DIMENSION = 2048

internal data class XiangqiTextureSet(
    val board: ImageBitmap?,
    val pieces: Map<XiangqiPiece, ImageBitmap>
)

internal data class XiangqiTextureBounds(
    val left: Int,
    val top: Int,
    val rightExclusive: Int,
    val bottomExclusive: Int
)

internal fun xiangqiPieceTexturePaths(): Map<XiangqiPiece, String> = linkedMapOf(
    XiangqiPiece(Side.RED, PieceType.GENERAL) to "pieces/red-general.png",
    XiangqiPiece(Side.RED, PieceType.ROOK) to "pieces/red-rook.png",
    XiangqiPiece(Side.RED, PieceType.HORSE) to "pieces/red-horse.png",
    XiangqiPiece(Side.RED, PieceType.CANNON) to "pieces/red-cannon.png",
    XiangqiPiece(Side.RED, PieceType.ELEPHANT) to "pieces/red-elephant.png",
    XiangqiPiece(Side.RED, PieceType.ADVISOR) to "pieces/red-advisor.png",
    XiangqiPiece(Side.RED, PieceType.SOLDIER) to "pieces/red-soldier.png",
    XiangqiPiece(Side.BLACK, PieceType.GENERAL) to "pieces/black-general.png",
    XiangqiPiece(Side.BLACK, PieceType.ROOK) to "pieces/black-rook.png",
    XiangqiPiece(Side.BLACK, PieceType.HORSE) to "pieces/black-horse.png",
    XiangqiPiece(Side.BLACK, PieceType.CANNON) to "pieces/black-cannon.png",
    XiangqiPiece(Side.BLACK, PieceType.ELEPHANT) to "pieces/black-elephant.png",
    XiangqiPiece(Side.BLACK, PieceType.ADVISOR) to "pieces/black-advisor.png",
    XiangqiPiece(Side.BLACK, PieceType.SOLDIER) to "pieces/black-soldier.png"
)

internal fun loadXiangqiTextures(assetsDirectory: File): XiangqiTextureSet {
    val board = decodeXiangqiTexture(
        assetsDirectory.resolve(XIANGQI_BOARD_TEXTURE_PATH),
        MAX_DECODED_BOARD_DIMENSION,
        trim = false
    )
    val pieces = xiangqiPieceTexturePaths().mapNotNull { (piece, relativePath) ->
        decodeXiangqiTexture(
            assetsDirectory.resolve(relativePath),
            MAX_DECODED_PIECE_DIMENSION,
            trim = true
        )?.let { piece to it }
    }.toMap()
    return XiangqiTextureSet(board = board, pieces = pieces)
}

private fun decodeXiangqiTexture(
    file: File,
    maxDimension: Int,
    trim: Boolean
): ImageBitmap? = runCatching {
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
    val decoded = BitmapFactory.decodeFile(
        file.path,
        BitmapFactory.Options().apply { inSampleSize = sampleSize }
    ) ?: return@runCatching null
    (if (trim) trimTransparentMargins(decoded) else decoded).asImageBitmap()
}.getOrNull()

internal fun findXiangqiTextureBounds(
    width: Int,
    height: Int,
    alphaAt: (x: Int, y: Int) -> Int
): XiangqiTextureBounds? {
    var left = width
    var top = height
    var right = -1
    var bottom = -1
    for (y in 0 until height) {
        for (x in 0 until width) {
            if (alphaAt(x, y) <= 0) continue
            left = minOf(left, x)
            top = minOf(top, y)
            right = maxOf(right, x)
            bottom = maxOf(bottom, y)
        }
    }
    return if (right < left || bottom < top) {
        null
    } else {
        XiangqiTextureBounds(left, top, right + 1, bottom + 1)
    }
}

private fun trimTransparentMargins(bitmap: Bitmap): Bitmap {
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    val bounds = findXiangqiTextureBounds(bitmap.width, bitmap.height) { x, y ->
        pixels[y * bitmap.width + x] ushr 24
    } ?: return bitmap
    if (
        bounds.left == 0 &&
        bounds.top == 0 &&
        bounds.rightExclusive == bitmap.width &&
        bounds.bottomExclusive == bitmap.height
    ) {
        return bitmap
    }
    val cropped = Bitmap.createBitmap(
        bitmap,
        bounds.left,
        bounds.top,
        bounds.rightExclusive - bounds.left,
        bounds.bottomExclusive - bounds.top
    )
    bitmap.recycle()
    return cropped
}
