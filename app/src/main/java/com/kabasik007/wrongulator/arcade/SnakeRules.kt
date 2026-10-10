package com.kabasik007.wrongulator.arcade

/** Immutable run settings. They must be part of a replay, not changed mid-run. */
enum class SnakeMode { CLASSIC, COMBAT, ZEN, TIME_ATTACK }
enum class SnakeDifficulty { CASUAL, NORMAL, EXPERT }
enum class SnakeMap { ARENA, WRAP, PORTALS, HAZARDS }
enum class SnakeSkin { MINT, GOLD, VIOLET, HIGH_CONTRAST }
enum class SnakeWeapon { GRENADE, BOUNCE, CHAIN }
enum class SnakePower { SHIELD, MAGNET, SLOW_MOTION, DOUBLE_SCORE }
enum class SnakeMission { SURVIVE_60, DESTROY_10, LENGTH_30 }
enum class SnakeAchievement { FIRST_FOOD, SCORE_100, SCORE_500, DESTROY_10, LENGTH_20, SURVIVE_60, NO_HIT_60, BOSS_WIN }

data class SnakeRules(
    val mode: SnakeMode = SnakeMode.COMBAT,
    val difficulty: SnakeDifficulty = SnakeDifficulty.NORMAL,
    val map: SnakeMap = SnakeMap.ARENA,
    val skin: SnakeSkin = SnakeSkin.MINT,
    /** 0–3 bounded rivals, only used in Combat. */
    val rivals: Int = 1,
) {
    init { require(rivals in 0..3) }
    val canShoot: Boolean get() = mode == SnakeMode.COMBAT
    val isInvincible: Boolean get() = mode == SnakeMode.ZEN
}

data class SnakeRunStats(
    val ticks: Int,
    val food: Int,
    val crates: Int,
    val length: Int,
    val combo: Int,
    val maxCombo: Int,
    val bossDefeated: Boolean,
)

data class SnakeReplayAction(
    val tick: Int,
    val direction: PadKey? = null,
    val fire: SnakeWeapon? = null,
)

/** A compact, data-only input trace. No executable scripts or downloaded code. */
data class SnakeReplay(
    val seed: Int,
    val rules: SnakeRules,
    val actions: List<SnakeReplayAction>,
)

object SnakePalette {
    fun body(skin: SnakeSkin): Int = when (skin) {
        SnakeSkin.MINT -> 0xFF64D8B4.toInt()
        SnakeSkin.GOLD -> 0xFFFFC765.toInt()
        SnakeSkin.VIOLET -> 0xFFBBA3FF.toInt()
        SnakeSkin.HIGH_CONTRAST -> 0xFFFFFFFF.toInt()
    }
    fun head(skin: SnakeSkin): Int = when (skin) {
        SnakeSkin.MINT -> 0xFFC0FFE8.toInt()
        SnakeSkin.GOLD -> 0xFFFFFFB0.toInt()
        SnakeSkin.VIOLET -> 0xFFE6DAFF.toInt()
        SnakeSkin.HIGH_CONTRAST -> 0xFFFFFF00.toInt()
    }
}
