package com.buddygames.junqi

import org.junit.Assert.assertEquals
import org.junit.Test

class JunqiManifestTest {
    @Test
    fun manifestUsesTheCurrentIndependentVersion() {
        assertEquals(2, JUNQI_VERSION_CODE)
        assertEquals("0.0.2", JUNQI_VERSION_NAME)
    }
}
