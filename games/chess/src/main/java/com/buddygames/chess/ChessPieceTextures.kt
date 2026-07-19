package com.buddygames.chess

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

internal const val CHESS_PIECE_TEXTURE_SCALE = 0.82f

private const val MAX_DECODED_PIECE_DIMENSION = 512

internal data class ChessTextureBounds(
    val left: Int,
    val top: Int,
    val rightExclusive: Int,
    val bottomExclusive: Int
)

internal fun chessPieceTexturePaths(): Map<ChessPiece, String> = linkedMapOf(
    ChessPiece(ChessSide.WHITE, ChessPieceType.KING) to "pieces/white-king.png",
    ChessPiece(ChessSide.WHITE, ChessPieceType.QUEEN) to "pieces/white-queen.png",
    ChessPiece(ChessSide.WHITE, ChessPieceType.ROOK) to "pieces/white-rook.png",
    ChessPiece(ChessSide.WHITE, ChessPieceType.BISHOP) to "pieces/white-bishop.png",
    ChessPiece(ChessSide.WHITE, ChessPieceType.KNIGHT) to "pieces/white-knight.png",
    ChessPiece(ChessSide.WHITE, ChessPieceType.PAWN) to "pieces/white-pawn.png",
    ChessPiece(ChessSide.BLACK, ChessPieceType.KING) to "pieces/black-king.png",
    ChessPiece(ChessSide.BLACK, ChessPieceType.QUEEN) to "pieces/black-queen.png",
    ChessPiece(ChessSide.BLACK, ChessPieceType.ROOK) to "pieces/black-rook.png",
    ChessPiece(ChessSide.BLACK, ChessPieceType.BISHOP) to "pieces/black-bishop.png",
    ChessPiece(ChessSide.BLACK, ChessPieceType.KNIGHT) to "pieces/black-knight.png",
    ChessPiece(ChessSide.BLACK, ChessPieceType.PAWN) to "pieces/black-pawn.png"
)

internal fun loadChessPieceTextures(assetsDirectory: File): Map<ChessPiece, ImageBitmap> {
    return chessPieceTexturePaths().mapNotNull { (piece, relativePath) ->
        decodeChessPieceTexture(assetsDirectory.resolve(relativePath))?.let { piece to it }
    }.toMap()
}

private fun decodeChessPieceTexture(file: File): ImageBitmap? = runCatching {
    if (!file.isFile) return@runCatching null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

    var sampleSize = 1
    while (
        bounds.outWidth / sampleSize > MAX_DECODED_PIECE_DIMENSION ||
        bounds.outHeight / sampleSize > MAX_DECODED_PIECE_DIMENSION
    ) {
        sampleSize *= 2
    }
    val decoded = BitmapFactory.decodeFile(
        file.path,
        BitmapFactory.Options().apply { inSampleSize = sampleSize }
    ) ?: return@runCatching null
    trimTransparentMargins(decoded).asImageBitmap()
}.getOrNull()

internal fun findChessTextureBounds(
    width: Int,
    height: Int,
    alphaAt: (x: Int, y: Int) -> Int
): ChessTextureBounds? {
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
        ChessTextureBounds(left, top, right + 1, bottom + 1)
    }
}

private fun trimTransparentMargins(bitmap: Bitmap): Bitmap {
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    val bounds = findChessTextureBounds(bitmap.width, bitmap.height) { x, y ->
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
