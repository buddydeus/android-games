package com.buddygames.doushouqi

import org.junit.Assert.assertEquals
import org.junit.Test

class DoushouqiPluginTest {
    @Test
    fun pluginUsesPackageManifest() {
        assertEquals(DoushouqiManifest.gameManifest, DoushouqiPlugin().getManifest())
    }

    @Test
    fun menuAndVersionCopyAreExact() {
        assertEquals(
            listOf("单人模式", "双人对战", "退出游戏"),
            doushouqiMenuLabels(),
        )
        assertEquals("版本 0.0.1", doushouqiVersionLabel("0.0.1"))
    }

    @Test
    fun currentTurnUsesSimpleRedAndGreenSideNames() {
        assertEquals("绿方", doushouqiSinglePlayerSideLabel(DoushouqiSide.PINE_GREEN))
        assertEquals("红方", doushouqiSinglePlayerSideLabel(DoushouqiSide.VERMILION))
        assertEquals("当前回合：绿方", doushouqiTurnLine(DoushouqiSide.PINE_GREEN))
        assertEquals("当前回合：红方", doushouqiTurnLine(DoushouqiSide.VERMILION))
    }

    @Test
    fun capturedPieceCopyUsesOnlyRedAndGreenFactionNames() {
        assertEquals(
            "红方鼠",
            doushouqiCapturedPieceLabel(red(DoushouqiAnimal.RAT)),
        )
        assertEquals(
            "绿方象",
            doushouqiCapturedPieceLabel(green(DoushouqiAnimal.ELEPHANT)),
        )
    }

    @Test
    fun resultCopyIsModeAware() {
        val result = DoushouqiResult.Win(
            DoushouqiSide.VERMILION,
            DoushouqiWinReason.DEN,
        )

        assertEquals(
            "红方进入兽穴",
            doushouqiResultText(result, DoushouqiMode.SINGLE_PLAYER),
        )
        assertEquals(
            "朱砂方进入兽穴",
            doushouqiResultText(result, DoushouqiMode.TWO_PLAYERS),
        )
    }
}
