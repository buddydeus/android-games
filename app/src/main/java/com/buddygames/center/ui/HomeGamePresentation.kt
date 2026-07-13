package com.buddygames.center.ui

import androidx.compose.ui.graphics.Color

internal data class HomeGamePresentation(
    val boardSize: String,
    val accent: Color,
    val symbol: String,
    val packageLabel: String
)

internal fun homeGamePresentation(gameId: String): HomeGamePresentation = when (gameId) {
    "gomoku" -> HomeGamePresentation("15 x 15", Color(0xFF2D3438), "五", "gomoku")
    "othello" -> HomeGamePresentation("8 x 8", Color(0xFF2F6F56), "黑", "othello")
    "xiangqi" -> HomeGamePresentation("9 x 10", Color(0xFF9B2F2F), "象", "xiangqi")
    else -> HomeGamePresentation("本地游戏", Color(0xFF274C77), gameId.take(1), gameId)
}

