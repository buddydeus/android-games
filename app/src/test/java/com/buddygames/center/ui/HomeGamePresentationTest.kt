package com.buddygames.center.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeGamePresentationTest {
    @Test
    fun gameCenterVersionUsesReadableHomeLabel() {
        assertEquals("版本 0.0.2", gameCenterVersionLabel("0.0.2"))
    }

    @Test
    fun builtInGamesUseDesignOrderAndVectorLogos() {
        assertEquals(HomeGameLogo.Gomoku, homeGamePresentation("gomoku").logo)
        assertEquals(0, homeGamePresentation("gomoku").order)
        assertEquals(HomeGameLogo.Xiangqi, homeGamePresentation("xiangqi").logo)
        assertEquals(1, homeGamePresentation("xiangqi").order)
        assertEquals(HomeGameLogo.Chess, homeGamePresentation("chess").logo)
        assertEquals(2, homeGamePresentation("chess").order)
        assertEquals(HomeGameLogo.Othello, homeGamePresentation("othello").logo)
        assertEquals(3, homeGamePresentation("othello").order)
        assertEquals(HomeGameLogo.Generic, homeGamePresentation("sudoku").logo)
        assertEquals(Int.MAX_VALUE, homeGamePresentation("sudoku").order)
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
        assertEquals(
            240,
            homeGameLayout(widthDp = 1107.9f, heightDp = 700f).buttonSizeDp
        )
        assertEquals(
            240,
            homeGameLayout(widthDp = 1108f, heightDp = 700f).buttonSizeDp
        )
    }

    @Test
    fun xiangqiGlyphScalesDownInsideCompactLogo() {
        assertEquals(27, xiangqiGlyphSizeSp(64))
        assertEquals(42, xiangqiGlyphSizeSp(96))
        assertEquals(42, xiangqiGlyphSizeSp(112))
    }
}
