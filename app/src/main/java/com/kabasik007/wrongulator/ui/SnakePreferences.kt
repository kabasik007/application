package com.kabasik007.wrongulator.ui

import android.content.SharedPreferences
import com.kabasik007.wrongulator.arcade.SnakeDifficulty
import com.kabasik007.wrongulator.arcade.SnakeMap
import com.kabasik007.wrongulator.arcade.SnakeMode
import com.kabasik007.wrongulator.arcade.SnakeRules
import com.kabasik007.wrongulator.arcade.SnakeSkin
import com.kabasik007.wrongulator.arcade.SnakeAchievement

internal data class SnakePlayerSettings(
    val rules: SnakeRules = SnakeRules(),
    val fps: Int = 60,
    val leftHanded: Boolean = false,
    val haptics: Boolean = true,
    val sound: Boolean = true,
)

internal object SnakePreferences {
    private inline fun <reified E : Enum<E>> SharedPreferences.enum(
        key: String, fallback: E,
    ): E = runCatching {
        enumValueOf<E>(getString(key, null) ?: fallback.name)
    }.getOrDefault(fallback)

    fun load(prefs: SharedPreferences): SnakePlayerSettings = SnakePlayerSettings(
        SnakeRules(
            mode = prefs.enum("mode", SnakeMode.COMBAT),
            difficulty = prefs.enum("difficulty", SnakeDifficulty.NORMAL),
            map = prefs.enum("map", SnakeMap.ARENA),
            skin = prefs.enum("skin", SnakeSkin.MINT),
            rivals = prefs.getInt("rivals", 1).coerceIn(0, 3),
        ),
        fps = if (prefs.getInt("fps", 60) == 30) 30 else 60,
        leftHanded = prefs.getBoolean("leftHanded", false),
        haptics = prefs.getBoolean("haptics", true),
        sound = prefs.getBoolean("snake_sound", true),
    )

    fun save(prefs: SharedPreferences, settings: SnakePlayerSettings) {
        val r = settings.rules
        prefs.edit()
            .putString("mode", r.mode.name)
            .putString("difficulty", r.difficulty.name)
            .putString("map", r.map.name)
            .putString("skin", r.skin.name)
            .putInt("rivals", r.rivals)
            .putInt("fps", settings.fps)
            .putBoolean("leftHanded", settings.leftHanded)
            .putBoolean("haptics", settings.haptics)
            .putBoolean("snake_sound", settings.sound)
            .apply()
    }

    fun scoreKey(rules: SnakeRules): String =
        "snake_${rules.mode.name}_${rules.difficulty.name}_${rules.map.name}"

    fun persistAchievements(prefs: SharedPreferences, achievements: Set<SnakeAchievement>) {
        if (achievements.isEmpty()) return
        prefs.edit().apply {
            achievements.forEach { putBoolean("achievement_${it.name}", true) }
        }.apply()
    }

    fun unlocked(prefs: SharedPreferences): Set<SnakeAchievement> =
        SnakeAchievement.entries.filterTo(mutableSetOf()) {
            prefs.getBoolean("achievement_${it.name}", false)
        }
}
