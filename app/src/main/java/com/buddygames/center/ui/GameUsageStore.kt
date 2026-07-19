package com.buddygames.center.ui

import android.content.Context

private const val PREFERENCES_NAME = "game_usage"
private const val LAUNCH_COUNT_PREFIX = "successful_launches:"

internal class GameUsageStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun readCounts(): Map<String, Int> = preferences.all.mapNotNull { (key, value) ->
        if (!key.startsWith(LAUNCH_COUNT_PREFIX) || value !is Int) {
            null
        } else {
            key.removePrefix(LAUNCH_COUNT_PREFIX) to value.coerceAtLeast(0)
        }
    }.toMap()

    fun recordSuccessfulLaunch(gameId: String): Int {
        val key = "$LAUNCH_COUNT_PREFIX$gameId"
        val current = preferences.getInt(key, 0).coerceAtLeast(0)
        val next = if (current == Int.MAX_VALUE) current else current + 1
        preferences.edit().putInt(key, next).apply()
        return next
    }
}
