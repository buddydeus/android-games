package com.buddygames.center.loader

import com.buddygames.api.GamePackage
import com.buddygames.api.GamePlugin
import dalvik.system.DexClassLoader
import java.io.File
import java.security.MessageDigest

/**
 * Loads package-owned plugin code and reuses it only while the plugin entry point and APK bytes
 * remain unchanged.
 */
class DexGamePluginLoader(private val optimizedRoot: File) {
    private data class CachedLoader(
        val key: PluginLoaderKey,
        val loader: DexClassLoader,
    )

    private val loaders = mutableMapOf<String, CachedLoader>()

    /**
     * Instantiates the package manifest's plugin entry point.
     *
     * @throws java.io.IOException when the plugin APK cannot be read for fingerprinting.
     * @throws ReflectiveOperationException when the entry class cannot be loaded or instantiated.
     * @throws IllegalStateException when the entry class does not implement [GamePlugin].
     */
    fun load(gamePackage: GamePackage): GamePlugin {
        val manifest = gamePackage.manifest
        val key = pluginLoaderKey(gamePackage)
        val cached = loaders[manifest.gameId]
        val loader = if (cached?.key == key) {
            cached.loader
        } else {
            val optimizedDir = optimizedRoot
                .resolve("game-dex/${manifest.gameId}/${key.apkSha256}")
                .also { it.mkdirs() }
            DexClassLoader(
                gamePackage.pluginApk.absolutePath,
                optimizedDir.absolutePath,
                null,
                GamePlugin::class.java.classLoader
            ).also { created ->
                loaders[manifest.gameId] = CachedLoader(key, created)
            }
        }
        val clazz = loader.loadClass(manifest.entryClass)
        val instance = clazz.getDeclaredConstructor().newInstance()
        return instance as? GamePlugin
            ?: error("${manifest.entryClass} does not implement GamePlugin")
    }
}

/** Stable identity for one loadable plugin artifact. */
internal data class PluginLoaderKey(
    val gameId: String,
    val entryClass: String,
    val apkSha256: String,
)

/**
 * Derives the loader cache identity from manifest routing and the plugin's actual bytes.
 *
 * @throws java.io.IOException when [GamePackage.pluginApk] cannot be read.
 */
internal fun pluginLoaderKey(gamePackage: GamePackage): PluginLoaderKey = PluginLoaderKey(
    gameId = gamePackage.manifest.gameId,
    entryClass = gamePackage.manifest.entryClass,
    apkSha256 = gamePackage.pluginApk.sha256(),
)

private fun File.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    inputStream().buffered().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
    }
    return digest.digest().joinToString(separator = "") { byte -> "%02x".format(byte) }
}
