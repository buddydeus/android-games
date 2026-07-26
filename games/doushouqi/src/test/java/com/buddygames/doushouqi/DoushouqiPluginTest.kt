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
}
