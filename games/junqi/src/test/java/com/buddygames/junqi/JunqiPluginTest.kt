package com.buddygames.junqi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JunqiPluginTest {
    @Test
    fun pluginMetadataUsesThePackageContractAndIndependentVersion() {
        val manifest = JunqiPlugin.manifest

        assertEquals("junqi", manifest.gameId)
        assertEquals("军棋", manifest.displayName)
        assertEquals(6, manifest.versionCode)
        assertEquals("0.0.6", manifest.versionName)
        assertEquals("com.buddygames.junqi.JunqiPlugin", manifest.entryClass)
        assertEquals(1, manifest.minShellApi)
        assertEquals("landscape", manifest.orientation)
        assertEquals("assets/icon.png", manifest.icon)
    }

    @Test
    fun menuUsesSharedLabelsAndManifestVersion() {
        assertEquals("军棋", JunqiUiText.TITLE)
        assertEquals(listOf("单人模式", "双人对战", "退出游戏"), JunqiUiText.MENU_LABELS)
        assertEquals("版本 0.0.6", JunqiUiText.versionLabel)
        assertEquals("版本 7.8.9", JunqiUiText.versionLabel("7.8.9"))
    }

    @Test
    fun landscapeGeometryKeepsBoardAndFixedRailSideBySideAtEightHundredBySixHundred() {
        val layout = junqiLandscapeLayout(availableWidthDp = 800f, availableHeightDp = 600f)

        assertEquals(240f, layout.railWidthDp, 0.001f)
        assertEquals(20f, layout.gapDp, 0.001f)
        assertEquals(560f, layout.boardHeightDp, 0.001f)
        assertEquals(466.66666f, layout.boardWidthDp, 0.001f)
        assertTrue(layout.boardWidthDp + layout.gapDp + layout.railWidthDp <= 760f)
        assertEquals(
            JunqiVisuals.BOARD_TEXTURE_WIDTH.toFloat() / JunqiVisuals.BOARD_TEXTURE_HEIGHT,
            layout.boardWidthDp / layout.boardHeightDp,
            0.0001f,
        )
    }

    @Test
    fun blueBottomViewMapsCoordinatesByOneHundredEightyDegrees() {
        assertEquals(
            JunqiPosition(0, 0),
            junqiModelPosition(displayRow = 0, displayColumn = 0, bottomSide = JunqiSide.RED),
        )
        assertEquals(
            JunqiPosition(11, 4),
            junqiModelPosition(displayRow = 0, displayColumn = 0, bottomSide = JunqiSide.BLUE),
        )
        assertEquals(
            JunqiPosition(1, 3),
            junqiDisplayPosition(JunqiPosition(10, 1), bottomSide = JunqiSide.BLUE),
        )
    }

    @Test
    fun observedPiecesExposeOwnRanksAndOnlyOpponentBacksOrPublicFlag() {
        val observation = JunqiObservation.from(
            state = JunqiState(
                pieces = listOf(
                    piece("red-engineer", JunqiSide.RED, JunqiPieceType.ENGINEER, 8, 2),
                    piece("red-flag", JunqiSide.RED, JunqiPieceType.FLAG, 11, 1),
                    piece("blue-secret", JunqiSide.BLUE, JunqiPieceType.COMMANDER, 3, 2),
                    piece("blue-flag", JunqiSide.BLUE, JunqiPieceType.FLAG, 0, 1),
                ).associateBy { it.position },
                revealedFlags = setOf(JunqiSide.BLUE),
            ),
            viewer = JunqiSide.RED,
        )

        val pieces = junqiVisiblePieces(observation)
        val own = pieces.single { it.id == "red-engineer" }
        val secret = pieces.single { it.id == "blue-secret" }
        val publicFlag = pieces.single { it.id == "blue-flag" }

        assertEquals("工兵", own.label)
        assertFalse(own.isBack)
        assertNull(secret.label)
        assertTrue(secret.isBack)
        assertEquals("军旗", publicFlag.label)
        assertTrue(publicFlag.isBack)
    }

    @Test
    fun boardTapResolutionSelectsOwnPieceAndReturnsOnlyVisibleLegalMove() {
        val state = JunqiState(
            pieces = listOf(
                piece("red-engineer", JunqiSide.RED, JunqiPieceType.ENGINEER, 3, 1),
                piece("red-flag", JunqiSide.RED, JunqiPieceType.FLAG, 11, 1),
                piece("blue-engineer", JunqiSide.BLUE, JunqiPieceType.ENGINEER, 8, 1),
                piece("blue-flag", JunqiSide.BLUE, JunqiPieceType.FLAG, 0, 1),
            ).associateBy { it.position },
            currentSide = JunqiSide.RED,
        )
        val observation = JunqiObservation.from(state, JunqiSide.RED)
        val from = JunqiPosition(3, 1)
        val to = JunqiPosition(3, 2)

        val selected = resolveJunqiBoardTap(observation, selected = null, tapped = from)
        val moved = resolveJunqiBoardTap(observation, selected = selected.selection, tapped = to)

        assertEquals(from, selected.selection)
        assertNull(selected.move)
        assertNull(moved.selection)
        assertEquals(JunqiMove(from, to), moved.move)

        val robotTurnObservation = JunqiObservation.from(
            JunqiState(state.pieces, currentSide = JunqiSide.BLUE),
            JunqiSide.RED,
        )
        assertEquals(
            JunqiTapResolution(),
            resolveJunqiBoardTap(robotTurnObservation, selected = null, tapped = from),
        )
    }

    @Test
    fun battleAndTerminalLabelsStayGenericAndUndoFollowsWinnerRule() {
        val battleLabels = JunqiBattleOutcome.entries.map(JunqiUiText::battleOutcomeLabel)

        assertEquals(listOf("进攻方胜", "防守方胜", "同归于尽"), battleLabels)
        JunqiPieceType.entries.forEach { type ->
            assertTrue(battleLabels.none { label -> label.contains(junqiPieceLabel(type)) })
        }
        assertFalse(junqiShowsUndo(JunqiResult.RED_WIN))
        assertFalse(junqiShowsUndo(JunqiResult.BLUE_WIN))
        assertTrue(junqiShowsUndo(JunqiResult.DRAW))
        assertTrue(junqiShowsUndo(null))
        assertTrue(junqiShowsRestart(JunqiResult.DRAW))
        assertFalse(junqiShowsRestart(null))
    }

    private fun piece(
        id: String,
        side: JunqiSide,
        type: JunqiPieceType,
        row: Int,
        column: Int,
    ): JunqiPiece = JunqiPiece(id, side, type, JunqiPosition(row, column))
}
