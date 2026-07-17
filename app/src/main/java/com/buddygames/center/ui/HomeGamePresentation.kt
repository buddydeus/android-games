package com.buddygames.center.ui

import kotlin.math.roundToInt

internal enum class HomeGameLogo {
    Gomoku,
    Xiangqi,
    Othello,
    Generic
}

internal data class HomeGamePresentation(
    val logo: HomeGameLogo,
    val order: Int
)

internal enum class HomeGameLayoutMode {
    SquareRow,
    CompactColumn
}

internal data class HomeGameLayout(
    val mode: HomeGameLayoutMode,
    val horizontalPaddingDp: Int,
    val gapDp: Int,
    val buttonSizeDp: Int?,
    val buttonHeightDp: Int,
    val logoSizeDp: Int
)

internal fun homeGamePresentation(gameId: String): HomeGamePresentation = when (gameId) {
    "gomoku" -> HomeGamePresentation(HomeGameLogo.Gomoku, 0)
    "xiangqi" -> HomeGamePresentation(HomeGameLogo.Xiangqi, 1)
    "othello" -> HomeGamePresentation(HomeGameLogo.Othello, 2)
    else -> HomeGamePresentation(HomeGameLogo.Generic, Int.MAX_VALUE)
}

internal fun homeGameLayout(widthDp: Float, heightDp: Float): HomeGameLayout {
    if (widthDp < 600 || heightDp > widthDp) {
        return HomeGameLayout(
            mode = HomeGameLayoutMode.CompactColumn,
            horizontalPaddingDp = 24,
            gapDp = 16,
            buttonSizeDp = null,
            buttonHeightDp = 112,
            logoSizeDp = 64
        )
    }

    val wide = widthDp >= 960
    val horizontalPadding = if (wide) 32 else 24
    val gap = if (wide) 28 else 20
    val buttonSize = if (wide) {
        264
    } else {
        ((widthDp - horizontalPadding * 2 - gap * 2) / 3f)
            .toInt()
            .coerceAtMost(240)
    }

    return HomeGameLayout(
        mode = HomeGameLayoutMode.SquareRow,
        horizontalPaddingDp = horizontalPadding,
        gapDp = gap,
        buttonSizeDp = buttonSize,
        buttonHeightDp = buttonSize,
        logoSizeDp = if (wide) 112 else 96
    )
}

internal fun xiangqiGlyphSizeSp(logoSizeDp: Int): Int =
    ((logoSizeDp - 8) * 0.48f)
        .roundToInt()
        .coerceIn(24, 42)
