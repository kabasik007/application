package com.kabasik007.wrongulator.arcade

/**
 * Small bounded input buffer. Touches arriving between two logic ticks are
 * not lost, and a fast UP→LEFT gesture can never reverse direction in one tick.
 */
internal class SnakeTurnQueue {
    private val turns = ArrayDeque<PadKey>()
    private var heading = PadKey.RIGHT

    fun reset(direction: PadKey = PadKey.RIGHT) {
        heading = direction
        turns.clear()
    }

    fun offer(next: PadKey): Boolean {
        if (next == PadKey.ACTION || turns.size >= 2) return false
        val from = turns.lastOrNull() ?: heading
        if (next == from || isOpposite(from, next)) return false
        turns.addLast(next)
        return true
    }

    fun advance(): PadKey {
        if (turns.isNotEmpty()) heading = turns.removeFirst()
        return heading
    }

    private fun isOpposite(a: PadKey, b: PadKey): Boolean =
        (a == PadKey.LEFT && b == PadKey.RIGHT) ||
            (a == PadKey.RIGHT && b == PadKey.LEFT) ||
            (a == PadKey.UP && b == PadKey.DOWN) ||
            (a == PadKey.DOWN && b == PadKey.UP)
}
