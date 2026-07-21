package com.buddygames.junqi

import org.junit.Assert.assertEquals
import org.junit.Test

class JunqiManifestTest {
    @Test
    fun manifestUsesTheCurrentIndependentVersion() {
        assertEquals(0x4A_55_4E_51_49_00_00_06L, AI_PACKAGE_SALT)
        assertEquals(6, JUNQI_VERSION_CODE)
        assertEquals("0.0.6", JUNQI_VERSION_NAME)
    }
}
