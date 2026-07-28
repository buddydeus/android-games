package com.buddygames.doushouqi

import com.buddygames.api.GameManifest

internal const val DOUSHOUQI_VERSION_CODE = 7
internal const val DOUSHOUQI_VERSION_NAME = "0.0.7"

internal object DoushouqiManifest {
    val gameManifest = GameManifest(
        gameId = "doushouqi",
        displayName = "斗兽棋",
        versionCode = DOUSHOUQI_VERSION_CODE,
        versionName = DOUSHOUQI_VERSION_NAME,
        entryClass = "com.buddygames.doushouqi.DoushouqiPlugin",
        minShellApi = 1,
        orientation = "landscape",
        icon = "assets/icon.png",
    )
}
