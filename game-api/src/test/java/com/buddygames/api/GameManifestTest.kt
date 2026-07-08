package com.buddygames.api

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameManifestTest {
    @Test
    fun validManifestPassesValidation() {
        val manifest = GameManifest(
            gameId = "gomoku",
            displayName = "五子棋",
            versionCode = 1,
            versionName = "1.0.0",
            entryClass = "com.buddygames.gomoku.GomokuPlugin",
            minShellApi = 1,
            icon = "assets/icon.png"
        )

        assertTrue(manifest.isValidForShell(shellApi = 1))
    }

    @Test
    fun invalidGameIdFailsValidation() {
        val manifest = GameManifest(
            gameId = "../bad",
            displayName = "Bad",
            versionCode = 1,
            versionName = "1.0.0",
            entryClass = "BadPlugin",
            minShellApi = 1,
            icon = null
        )

        assertFalse(manifest.isValidForShell(shellApi = 1))
    }

    @Test
    fun futureShellApiFailsValidation() {
        val manifest = GameManifest(
            gameId = "future",
            displayName = "Future",
            versionCode = 1,
            versionName = "1.0.0",
            entryClass = "FuturePlugin",
            minShellApi = 2,
            icon = null
        )

        assertFalse(manifest.isValidForShell(shellApi = 1))
    }
}

