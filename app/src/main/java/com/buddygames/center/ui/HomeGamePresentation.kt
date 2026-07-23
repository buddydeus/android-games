package com.buddygames.center.ui

import com.buddygames.api.GamePackage

internal enum class HomeGameLayoutMode {
    SquareRow,
    CompactColumn
}

internal data class HomeGameLayout(
    val mode: HomeGameLayoutMode,
    val horizontalPaddingDp: Int,
    val gapDp: Int,
    val maxColumns: Int,
    val buttonSizeDp: Int?,
    val buttonHeightDp: Int,
    val logoSizeDp: Int
)

internal fun rankGamePackages(
    packages: List<GamePackage>,
    usageCounts: Map<String, Int>
): List<GamePackage> = packages.sortedWith(
    compareByDescending<GamePackage> { usageCounts[it.manifest.gameId] ?: 0 }
        .thenBy { it.manifest.displayName }
        .thenBy { it.manifest.gameId }
)

internal fun gameCenterVersionLabel(versionName: String): String = "版本 $versionName"

internal fun homeGameGridRows(
    packages: List<GamePackage>,
    columns: Int
): List<List<GamePackage?>> {
    require(columns > 0)
    return packages.chunked(columns).map { row ->
        row.map<GamePackage, GamePackage?> { it } + List(columns - row.size) { null }
    }
}

internal fun homeGameLayout(widthDp: Float, heightDp: Float): HomeGameLayout {
    if (widthDp < 600 || heightDp > widthDp) {
        return HomeGameLayout(
            mode = HomeGameLayoutMode.CompactColumn,
            horizontalPaddingDp = 24,
            gapDp = 16,
            maxColumns = 1,
            buttonSizeDp = null,
            buttonHeightDp = 112,
            logoSizeDp = 64
        )
    }

    val wide = widthDp >= 1108
    val horizontalPadding = if (wide) 32 else 24
    val gap = if (wide) 28 else 20
    val maxColumns = 4
    val buttonSize = ((widthDp - horizontalPadding * 2 - gap * (maxColumns - 1)) / maxColumns)
        .toInt()
        .coerceAtMost(240)

    return HomeGameLayout(
        mode = HomeGameLayoutMode.SquareRow,
        horizontalPaddingDp = horizontalPadding,
        gapDp = gap,
        maxColumns = maxColumns,
        buttonSizeDp = buttonSize,
        buttonHeightDp = buttonSize,
        logoSizeDp = if (wide) 112 else 72
    )
}
