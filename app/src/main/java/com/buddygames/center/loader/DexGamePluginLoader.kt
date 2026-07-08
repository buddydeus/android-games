package com.buddygames.center.loader

import com.buddygames.api.GamePackage
import com.buddygames.api.GamePlugin
import dalvik.system.DexClassLoader
import java.io.File

class DexGamePluginLoader(private val optimizedRoot: File) {
    private val loaders = mutableMapOf<String, DexClassLoader>()

    fun load(gamePackage: GamePackage): GamePlugin {
        val manifest = gamePackage.manifest
        val loader = loaders.getOrPut(manifest.gameId) {
            val optimizedDir = optimizedRoot.resolve("game-dex/${manifest.gameId}").also { it.mkdirs() }
            DexClassLoader(
                gamePackage.pluginApk.absolutePath,
                optimizedDir.absolutePath,
                null,
                GamePlugin::class.java.classLoader
            )
        }
        val clazz = loader.loadClass(manifest.entryClass)
        val instance = clazz.getDeclaredConstructor().newInstance()
        return instance as? GamePlugin
            ?: error("${manifest.entryClass} does not implement GamePlugin")
    }
}

