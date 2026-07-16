package com.buddygames.center.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeGamePresentationTest {
    @Test
    fun presentationUsesKnownBoardMetadataAndFallsBackForImportedGames() {
        assertEquals("15 x 15", homeGamePresentation("gomoku").boardSize)
        assertEquals("8 x 8", homeGamePresentation("othello").boardSize)
        assertEquals("9 x 10", homeGamePresentation("xiangqi").boardSize)
        assertEquals("本地游戏", homeGamePresentation("sudoku").boardSize)
    }

    @Test
    fun builtInGamesUseDistinctShelfBoardTextures() {
        assertEquals("textures/gomoku-shelf.png", homeGamePresentation("gomoku").shelfTexture)
        assertEquals("textures/othello-shelf.png", homeGamePresentation("othello").shelfTexture)
        assertEquals("textures/xiangqi-shelf.png", homeGamePresentation("xiangqi").shelfTexture)
    }
}
