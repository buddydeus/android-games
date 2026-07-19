package com.buddygames.center.ui

import com.buddygames.api.GameManifest
import com.buddygames.api.GamePackage
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class HomeGamePresentationTest {
    @Test
    fun gameCenterVersionUsesReadableHomeLabel() {
        assertEquals("版本 0.0.3", gameCenterVersionLabel("0.0.3"))
    }

    @Test
    fun gamesAreRankedBySuccessfulLaunchCount() {
        val games = listOf(
            gamePackage("gomoku", "五子棋"),
            gamePackage("xiangqi", "象棋"),
            gamePackage("othello", "黑白棋")
        )

        assertEquals(
            listOf("xiangqi", "othello", "gomoku"),
            rankGamePackages(
                packages = games,
                usageCounts = mapOf("xiangqi" to 7, "othello" to 3, "gomoku" to 1)
            ).map { it.manifest.gameId }
        )
    }

    @Test
    fun gamesWithEqualUsageUsePackageNameAndIdForStableOrder() {
        val games = listOf(
            gamePackage("beta", "A Game"),
            gamePackage("alpha", "A Game"),
            gamePackage("chess", "B Game")
        )

        assertEquals(
            listOf("alpha", "beta", "chess"),
            rankGamePackages(
                packages = games,
                usageCounts = mapOf("alpha" to 2, "beta" to 2, "chess" to 2)
            ).map { it.manifest.gameId }
        )
    }

    @Test
    fun wideTabletUsesFixedEqualSquareButtons() {
        assertEquals(
            HomeGameLayout(
                mode = HomeGameLayoutMode.SquareRow,
                horizontalPaddingDp = 32,
                gapDp = 28,
                maxColumns = 4,
                buttonSizeDp = 240,
                buttonHeightDp = 240,
                logoSizeDp = 112
            ),
            homeGameLayout(widthDp = 1200f, heightDp = 800f)
        )
    }

    @Test
    fun mediumTabletUsesFourEqualAdaptiveSquares() {
        assertEquals(
            HomeGameLayout(
                mode = HomeGameLayoutMode.SquareRow,
                horizontalPaddingDp = 24,
                gapDp = 20,
                maxColumns = 4,
                buttonSizeDp = 173,
                buttonHeightDp = 173,
                logoSizeDp = 72
            ),
            homeGameLayout(widthDp = 800f, heightDp = 600f)
        )
    }

    @Test
    fun compactWidthUsesEqualHorizontalButtons() {
        val expected = HomeGameLayout(
            mode = HomeGameLayoutMode.CompactColumn,
            horizontalPaddingDp = 24,
            gapDp = 16,
            maxColumns = 1,
            buttonSizeDp = null,
            buttonHeightDp = 112,
            logoSizeDp = 64
        )

        assertEquals(expected, homeGameLayout(widthDp = 599f, heightDp = 800f))
        assertEquals(expected, homeGameLayout(widthDp = 375f, heightDp = 667f))
        assertEquals(expected, homeGameLayout(widthDp = 599.9f, heightDp = 800f))
    }

    @Test
    fun portraitTabletUsesCompactHorizontalButtons() {
        assertEquals(
            HomeGameLayout(
                mode = HomeGameLayoutMode.CompactColumn,
                horizontalPaddingDp = 24,
                gapDp = 16,
                maxColumns = 1,
                buttonSizeDp = null,
                buttonHeightDp = 112,
                logoSizeDp = 64
            ),
            homeGameLayout(widthDp = 800f, heightDp = 1100f)
        )
    }

    @Test
    fun exactResponsiveBoundariesUseSpecifiedSquareModes() {
        assertEquals(
            HomeGameLayoutMode.SquareRow,
            homeGameLayout(widthDp = 600f, heightDp = 600f).mode
        )
        assertEquals(
            212,
            homeGameLayout(widthDp = 959.9f, heightDp = 600f).buttonSizeDp
        )
        assertEquals(
            213,
            homeGameLayout(widthDp = 960f, heightDp = 600f).buttonSizeDp
        )
        val belowWideBreakpoint = homeGameLayout(widthDp = 1107.9f, heightDp = 700f)
        assertEquals(240, belowWideBreakpoint.buttonSizeDp)
        assertEquals(24, belowWideBreakpoint.horizontalPaddingDp)
        assertEquals(20, belowWideBreakpoint.gapDp)
        assertEquals(72, belowWideBreakpoint.logoSizeDp)

        val atWideBreakpoint = homeGameLayout(widthDp = 1108f, heightDp = 700f)
        assertEquals(240, atWideBreakpoint.buttonSizeDp)
        assertEquals(32, atWideBreakpoint.horizontalPaddingDp)
        assertEquals(28, atWideBreakpoint.gapDp)
        assertEquals(112, atWideBreakpoint.logoSizeDp)
    }

    private fun gamePackage(gameId: String, displayName: String): GamePackage {
        val root = File("build/test-games/$gameId")
        return GamePackage(
            manifest = GameManifest(
                gameId = gameId,
                displayName = displayName,
                versionCode = 1,
                versionName = "0.0.1",
                entryClass = "example.Plugin",
                minShellApi = 1
            ),
            rootDir = root,
            pluginApk = root.resolve("plugin.apk"),
            assetsDir = root.resolve("assets")
        )
    }
}
