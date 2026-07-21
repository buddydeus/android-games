package com.buddygames.junqi

import org.junit.Assert.assertEquals
import org.junit.Test

class JunqiManifestTest {
    @Test
    fun manifestStartsAtIndependentVersion() {
        assertEquals(1, JUNQI_VERSION_CODE)
        assertEquals("0.0.1", JUNQI_VERSION_NAME)
    }
}
