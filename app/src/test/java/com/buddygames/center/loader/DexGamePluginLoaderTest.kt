package com.buddygames.center.loader

import com.buddygames.api.GameManifest
import com.buddygames.api.GamePackage
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DexGamePluginLoaderTest {
    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun pluginIdentityChangesWhenSameVersionApkIsReplaced() {
        val root = temp.newFolder("gomoku")
        val pluginApk = root.resolve("plugin.apk")
        val gamePackage = GamePackage(
            manifest = GameManifest(
                gameId = "gomoku",
                displayName = "五子棋",
                versionCode = 1,
                versionName = "0.0.1",
                entryClass = "com.buddygames.gomoku.GomokuPlugin",
                minShellApi = 1,
            ),
            rootDir = root,
            pluginApk = pluginApk,
            assetsDir = root.resolve("assets"),
        )
        pluginApk.writeText("first plugin body")
        val firstIdentity = pluginLoaderKey(gamePackage)

        pluginApk.writeText("replacement body")
        val replacementIdentity = pluginLoaderKey(gamePackage)

        assertNotEquals(firstIdentity, replacementIdentity)
    }
}
