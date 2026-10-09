package com.kabasik007.wrongulator.arcade

import kotlin.random.Random

/** Classic grid snake. Pure Kotlin; no sprites, bitmaps, threads or network. */
class SnakeGame(private val random: Random = Random.Default) : ArcadeGame {
    override val width = 20f
    override val height = 26f
    override val intervalMs = 125L
    override var score = 0
        private set
    override var finished = false
        private set

    private val body = ArrayDeque<GridCell>()
    private var facing = PadKey.RIGHT
    private var pending = PadKey.RIGHT
    private var food = GridCell(14, 13)

    init { reset() }

    override fun reset() {
        body.clear()
        body.addAll(listOf(GridCell(8, 13), GridCell(7, 13), GridCell(6, 13)))
        facing = PadKey.RIGHT
        pending = facing
        score = 0
        finished = false
        nextFood()
    }

    override fun input(key: PadKey) {
        if (finished || key == PadKey.ACTION) return
        if (key == facing) return
        if ((facing == PadKey.LEFT && key == PadKey.RIGHT) ||
            (facing == PadKey.RIGHT && key == PadKey.LEFT) ||
            (facing == PadKey.UP && key == PadKey.DOWN) ||
            (facing == PadKey.DOWN && key == PadKey.UP)
        ) return
        pending = key
    }

    override fun tick() {
        if (finished) return
        facing = pending
        val head = body.first()
        val next = when (facing) {
            PadKey.LEFT -> GridCell(head.x - 1, head.y)
            PadKey.RIGHT -> GridCell(head.x + 1, head.y)
            PadKey.UP -> GridCell(head.x, head.y - 1)
            else -> GridCell(head.x, head.y + 1)
        }
        val eating = next == food
        if (next.x !in 0 until width.toInt() || next.y !in 0 until height.toInt() ||
            body.anyIndexedExceptTailIf(!eating) { it == next }
        ) {
            finished = true
            return
        }
        body.addFirst(next)
        if (eating) {
            score += 10
            if (body.size >= width.toInt() * height.toInt()) {
                finished = true
            } else {
                nextFood()
            }
        } else {
            body.removeLast()
        }
    }

    private inline fun <T> ArrayDeque<T>.anyIndexedExceptTailIf(
        skipTail: Boolean, predicate: (T) -> Boolean,
    ): Boolean {
        val lastIndex = size - (if (skipTail) 1 else 0)
        return this.withIndex().any { it.index < lastIndex && predicate(it.value) }
    }

    private fun nextFood() {
        val limit = width.toInt() * height.toInt()
        val start = random.nextInt(limit)
        for (offset in 0 until limit) {
            val pos = (start + offset) % limit
            val candidate = GridCell(pos % width.toInt(), pos / width.toInt())
            if (candidate !in body) {
                food = candidate
                return
            }
        }
        finished = true
    }

    override fun paint(painter: ArcadePainter) {
        painter.box(0f, 0f, width, height, ArcadeColors.backdrop)
        for (x in 0..width.toInt()) painter.line(x.toFloat(), 0f, x.toFloat(), height, ArcadeColors.grid, .035f)
        for (y in 0..height.toInt()) painter.line(0f, y.toFloat(), width, y.toFloat(), ArcadeColors.grid, .035f)
        painter.disc(food.x + .5f, food.y + .5f, .43f, ArcadeColors.orange)
        for ((index, cell) in body.withIndex()) {
            painter.box(cell.x + .09f, cell.y + .09f, .82f, .82f,
                if (index == 0) ArcadeColors.white else ArcadeColors.mint)
        }
    }
}
