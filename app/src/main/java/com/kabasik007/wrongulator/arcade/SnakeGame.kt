package com.kabasik007.wrongulator.arcade

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class SnakeEvent { NONE, EAT, CRASH }

/** A strictly visual shot record: collisions and scoring happen when the button is pressed. */
data class SnakeBlast(val origin: GridCell, val impact: GridCell, val destroyed: Int)

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
    val ammo: Int get() = grenades
    val obstaclesRemaining: Int get() = obstacles.size
    var lastBlast: SnakeBlast? = null
        private set

    private val body = ArrayDeque<GridCell>()
    private var previousBody: List<GridCell> = emptyList()
    private var facing = PadKey.RIGHT
    private var nextDirection = PadKey.RIGHT
    private var food = GridCell(14, 13)
    private var lastSnack = food
    private var sparkTicks = 0
    private val obstacles = linkedSetOf<GridCell>()
    private var grenades = 3
    private var foodCollected = 0
    private var blastTicks = 0

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
        grenades = 3
        foodCollected = 0
        blastTicks = 0
        lastBlast = null
        obstacles.clear()
        // The first barrel is in the original travel lane: an immediately testable target.
        obstacles.addAll(listOf(GridCell(15, 13), GridCell(5, 6), GridCell(14, 19)))
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
        if (blastTicks > 0) blastTicks--
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
        if (next.x !in 0 until width.toInt() || next.y !in 0 until height.toInt() || occupied || next in obstacles) {
            lastEvent = SnakeEvent.CRASH
            finished = true
            return
        }
        previousBody = body.toList()
        body.addFirst(next)
        steps++
        if (eating) {
            score += 10
            foodCollected++
            if (foodCollected % 2 == 0) grenades = (grenades + 1).coerceAtMost(5)
            if (foodCollected % 3 == 0) spawnObstacle()
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
            if (candidate !in body && candidate !in obstacles) {
                food = candidate
                return
            }
        }
        finished = true
    }

    /**
     * A fictional arcade grenade. Fired along the current heading for at most 8
     * cells, stopping on the first destructible obstacle. A 3x3 blast clears
     * nearby obstacles. It never injures the snake or alters real-world objects.
     */
    fun launchGrenade(): Boolean {
        if (finished || grenades <= 0) return false
        grenades--
        val head = body.first()
        val (dx, dy) = when (facing) {
            PadKey.LEFT -> -1 to 0
            PadKey.RIGHT -> 1 to 0
            PadKey.UP -> 0 to -1
            else -> 0 to 1
        }
        var impact = head
        for (distance in 1..8) {
            val target = GridCell(head.x + dx * distance, head.y + dy * distance)
            if (target.x !in 0 until width.toInt() || target.y !in 0 until height.toInt()) break
            impact = target
            if (target in obstacles) break
        }
        val destroyed = obstacles.count {
            abs(it.x - impact.x) <= 1 && abs(it.y - impact.y) <= 1
        }
        obstacles.removeAll {
            abs(it.x - impact.x) <= 1 && abs(it.y - impact.y) <= 1
        }
        score += destroyed * 15
        lastBlast = SnakeBlast(head, impact, destroyed)
        blastTicks = 5
        return true
    }

    private fun spawnObstacle() {
        if (obstacles.size >= 12) return
        val cells = width.toInt() * height.toInt()
        val start = random.nextInt(cells)
        for (offset in 0 until cells) {
            val index = (start + offset) % cells
            val pos = GridCell(index % width.toInt(), index / width.toInt())
            val head = body.first()
            // Keep a safe launch area; never spawn under food, body or an old barrel.
            if (pos !in obstacles && pos !in body && pos != food &&
                abs(pos.x - head.x) + abs(pos.y - head.y) >= 5
            ) {
                obstacles.add(pos)
                return
            }
        }
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

        // Destructible neon crates. No sprites or texture allocation.
        for (obstacle in obstacles) {
            val bx = obstacle.x.toFloat()
            val by = obstacle.y.toFloat()
            painter.box(bx + .04f, by + .04f, .92f, .92f, 0xFF493B47.toInt())
            painter.box(bx + .13f, by + .13f, .74f, .74f, 0xFFFF9664.toInt())
            painter.line(bx + .22f, by + .22f, bx + .78f, by + .78f,
                0xFF503342.toInt(), .11f)
            painter.line(bx + .78f, by + .22f, bx + .22f, by + .78f,
                0xFF503342.toInt(), .11f)
        }


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
        val blast = lastBlast
        if (blast != null && blastTicks > 0) {
            val fade = (blastTicks - t).coerceIn(0f, 5f) / 5f
            val cx = blast.impact.x + .5f
            val cy = blast.impact.y + .5f
            painter.line(
                blast.origin.x + .5f, blast.origin.y + .5f, cx, cy,
                0x99FFCB73.toInt(), (.06f + .14f * fade),
            )
            painter.disc(cx, cy, 1.9f * (1.15f - fade), 0x44FFB066)
            painter.disc(cx, cy, 1.0f * (1.15f - fade), 0xAAFFCD77.toInt())
            painter.disc(cx, cy, .29f, ArcadeColors.white)
            for (index in 0 until 8) {
                val angle = index * .7854f
                val spread = (1f - fade) * 2.8f + .5f
                painter.disc(cx + cos(angle) * spread, cy + sin(angle) * spread,
                    .08f + .12f * fade, ArcadeColors.orange)
            }
        }
    }
}
