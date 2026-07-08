package com.buddygames.center.registry

import com.buddygames.api.GamePlugin
import com.buddygames.center.packages.BuiltInPackageDefinition
import com.buddygames.center.packages.GameManifestJson
import com.buddygames.gomoku.GomokuPlugin
import com.buddygames.othello.OthelloPlugin
import com.buddygames.xiangqi.XiangqiPlugin

object BuiltInGameRegistry {
    private val plugins = listOf(
        GomokuPlugin(),
        OthelloPlugin(),
        XiangqiPlugin()
    )

    fun packageDefinitions(): List<BuiltInPackageDefinition> {
        return plugins.map { plugin ->
            val manifest = plugin.getManifest()
            BuiltInPackageDefinition(
                manifest = manifest,
                manifestJson = GameManifestJson.format(manifest)
            )
        }
    }

    fun pluginFor(gameId: String): GamePlugin? = plugins.firstOrNull { it.getManifest().gameId == gameId }
}

