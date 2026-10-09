package com.kabasik007.wrongulator.arcade

import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class SnakeEvent { NONE, EAT, CRASH }

/**
 * Android-independent Snake game. The game clock is owned by the console.
 * Motion frames interpolate between immutable pre-tick and post-tick body coordinates.
 */
class SnakeGame(private val random: Random = Random.Default) : ArcadeGame {
    override val width = 20f
    override val height = 26f
    override val intervalMs: Long get() = (155L - (score / 20) * 6L).coerceAtLeast(78L)
    override var score: Int = 0
        private set
    override var finished: Boolean = false
        private set
    var lastEvent: SnakeEvent = SnakeEvent.NONE
        private set
    var steps: Int = 0
        private set
    val length: Int get() = body.size

    private val body = ArrayDeque<GridCell>()
    private var previousBody: List<GridCell> = emptyList()
    private var facing = PadKey.RIGHT
    private var nextDirection = PadKey.RIGHT
    private var food = GridCell(14, 13)
    private var lastSnack = food
    private var sparkTicks = 0

    init { reset() }

    override fun reset() {
        body.clear()
        body.addAll(listOf(GridCell(8, 13), GridCell(7, 13), GridCell(6, 13)))
        previousBody = body.toList()
        facing = PadKey.RIGHT
        nextDirection = facing
        score = 0
        steps = 0
        finished = false
        lastEvent = SnakeEvent.NONE
        sparkTicks = 0
        nextFood()
    }

    override fun input(key: PadKey) {
        if (finished || key == PadKey.ACTION) return
        // Compare against the last *executed* movement, not the last queued turn.
        // This prevents a backwards turn on the same update even if inputs are rapid.
        if ((facing == PadKey.LEFT && key == PadKey.RIGHT) ||
            (facing == PadKey.RIGHT && key == PadKey.LEFT) ||
            (facing == PadKey.UP && key == PadKey.DOWN) ||
            (facing == PadKey.DOWN && key == PadKey.UP)
        ) return
        nextDirection = key
    }

    override fun tick() {
        if (finished) return
        lastEvent = SnakeEvent.NONE
        if (sparkTicks > 0) sparkTicks--
        facing = nextDirection
        val head = body.first()
        val next = when (facing) {
            PadKey.LEFT -> GridCell(head.x - 1, head.y)
            PadKey.RIGHT -> GridCell(head.x + 1, head.y)
            PadKey.UP -> GridCell(head.x, head.y - 1)
            else -> GridCell(head.x, head.y + 1)
        }
        val eating = next == food
        // Moving into the previous tail location is legal if we are not growing.
        val occupied = body.withIndex().any {
            (eating || it.index != body.size - 1) && it.value == next
        }
        if (next.x !in 0 until width.toInt() || next.y !in 0 until height.toInt() || occupied) {
            lastEvent = SnakeEvent.CRASH
            finished = true
            return
        }
        previousBody = body.toList()
        body.addFirst(next)
        steps++
        if (eating) {
            score += 10
            lastSnack = next
            sparkTicks = 4
            lastEvent = SnakeEvent.EAT
            if (body.size == width.toInt() * height.toInt()) {
                finished = true
            } else {
                nextFood()
            }
        } else {
            body.removeLast()
        }
    }

    private fun nextFood() {
        val cells = width.toInt() * height.toInt()
        val start = random.nextInt(cells)
        for (offset in 0 until cells) {
            val i = (start + offset) % cells
            val candidate = GridCell(i % width.toInt(), i / width.toInt())
            if (candidate !in body) {
                food = candidate
                return
            }
        }
        finished = true
    }

    override fun paint(painter: ArcadePainter) = paintSmooth(painter, 1f)

    /** Visual interpolation only; never mutates logical coordinates. */
    fun paintSmooth(painter: ArcadePainter, progress: Float) {
        val t = progress.coerceIn(0f, 1f)
        painter.box(0f, 0f, width, height, 0xFF0B1626.toInt())
        // Muted grid and insets give the arena a premium console appearance.
        for (x in 0..20) {
            painter.line(x.toFloat(), 0f, x.toFloat(), height, 0xFF193044.toInt(), .027f)
        }
        for (y in 0..26) {
            painter.line(0f, y.toFloat(), width, y.toFloat(), 0xFF193044.toInt(), .027f)
        }
        painter.line(.2f, .2f, 19.8f, .2f, 0xFF305367.toInt(), .09f)
        painter.line(.2f, 25.8f, 19.8f, 25.8f, 0xFF305367.toInt(), .09f)

        val pulse = .06f * sin((steps + t) * .9f)
        painter.disc(food.x + .5f, food.y + .5f, .59f + pulse, 0x447FE9B4)
        painter.disc(food.x + .5f, food.y + .5f, .37f + pulse, ArcadeColors.orange)
        painter.disc(food.x + .39f, food.y + .38f, .12f, ArcadeColors.white)

        // Paint from tail to head to prevent dark seams between consecutive segments.
        val oldTail = previousBody.lastOrNull() ?: body.last()
        for (i in body.indices.reversed()) {
            val target = body.elementAt(i)
            val origin = previousBody.getOrNull(i) ?: oldTail
            val sx = origin.x + (target.x - origin.x) * t
            val sy = origin.y + (target.y - origin.y) * t
            val isHead = i == 0
            val baseColor = when {
                isHead && finished -> ArcadeColors.red
                isHead -> 0xFFB6FFE0.toInt()
                i % 2 == 0 -> 0xFF65D8B3.toInt()
                else -> 0xFF55C3A3.toInt()
            }
            painter.box(sx + .025f, sy + .095f, .95f, .88f, 0xFF173E3B.toInt())
            painter.box(sx + .085f, sy + .085f, .83f, .83f, baseColor)
            if (isHead) {
                val dx = when (facing) { PadKey.LEFT -> -.16f; PadKey.RIGHT -> .16f; else -> 0f }
                val dy = when (facing) { PadKey.UP -> -.16f; PadKey.DOWN -> .16f; else -> 0f }
                painter.disc(sx + .34f + dx, sy + .36f + dy, .095f, ArcadeColors.backdrop)
                painter.disc(sx + .67f + dx, sy + .36f + dy, .095f, ArcadeColors.backdrop)
            }
        }

        if (sparkTicks > 0) {
            val intensity = sparkTicks.toFloat() / 4f
            for (i in 0..5) {
                val angle = i * 1.0472f
                val radius = (1.7f - intensity) + t * .7f
                val px = lastSnack.x + .5f + cos(angle) * radius
                val py = lastSnack.y + .5f + sin(angle) * radius
                painter.disc(px, py, .12f * intensity, ArcadeColors.orange)
            }
        }
    }
}
