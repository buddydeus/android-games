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
        assertEquals("绿方", doushouqiTurnSideLabel(DoushouqiSide.PINE_GREEN))
        assertEquals("红方", doushouqiTurnSideLabel(DoushouqiSide.VERMILION))
        assertEquals("当前回合：绿方", doushouqiTurnLine(DoushouqiSide.PINE_GREEN))
        assertEquals("当前回合：红方", doushouqiTurnLine(DoushouqiSide.VERMILION))
    }

    @Test
    fun identityAndResultCopyKeepFullFactionMeaning() {
        assertEquals("玩家执松绿", doushouqiPlayerSideLine(DoushouqiSide.PINE_GREEN))
        assertEquals("玩家执朱砂", doushouqiPlayerSideLine(DoushouqiSide.VERMILION))
        assertEquals("松绿方", doushouqiFullSideLabel(DoushouqiSide.PINE_GREEN))
        assertEquals("朱砂方", doushouqiFullSideLabel(DoushouqiSide.VERMILION))
    }
}
