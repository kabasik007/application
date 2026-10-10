package com.kabasik007.wrongulator.arcade

import java.util.Base64

/**
 * Replay v1: immutable run settings, RNG seed and bounded events.
 * URL-safe text, safe to share manually, no permission/network/backend required.
 */
object SnakeReplayCodec {
    private const val VERSION = "SC1"
    const val MAX_ACTIONS = 12000
    const val MAX_ENCODED = 150000

    fun encode(run: SnakeReplay): String {
        require(run.actions.size <= MAX_ACTIONS)
        val rules = run.rules
        val header = listOf(
            VERSION, run.seed, rules.mode.name, rules.difficulty.name,
            rules.map.name, rules.skin.name, rules.rivals,
        ).joinToString("|")
        val commands = run.actions.joinToString(";") { action ->
            listOf(action.tick, action.direction?.name ?: "_", action.fire?.name ?: "_")
                .joinToString(",")
        }
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString("$header\n$commands".toByteArray(Charsets.UTF_8))
    }

    fun decode(encoded: String): SnakeReplay? = runCatching {
        if (encoded.length > MAX_ENCODED) return null
        val raw = String(Base64.getUrlDecoder().decode(encoded), Charsets.UTF_8)
        val segments = raw.split('\n', limit = 2)
        if (segments.size != 2) return null
        val header = segments[0].split('|')
        if (header.size != 7 || header[0] != VERSION) return null
        val rules = SnakeRules(
            SnakeMode.valueOf(header[2]),
            SnakeDifficulty.valueOf(header[3]),
            SnakeMap.valueOf(header[4]),
            SnakeSkin.valueOf(header[5]),
            header[6].toInt(),
        )
        val actions = if (segments[1].isEmpty()) emptyList() else {
            segments[1].split(';').map { record ->
                val parts = record.split(',')
                require(parts.size == 3)
                SnakeReplayAction(
                    parts[0].toInt(),
                    parts[1].takeUnless { it == "_" }?.let(PadKey::valueOf),
                    parts[2].takeUnless { it == "_" }?.let(SnakeWeapon::valueOf),
                )
            }
        }
        if (actions.size > MAX_ACTIONS || actions.any { it.tick < 0 || it.tick > 100_000 } ||
            actions.zipWithNext().any { (a, b) -> b.tick < a.tick }
        ) return null
        SnakeReplay(header[1].toInt(), rules, actions)
    }.getOrNull()
}
