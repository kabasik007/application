package com.kabasik007.wrongulator.arcade

import kotlin.random.Random

/** Falling-block puzzle: classic genre, original implementation and color palette. */
class BlockDropGame(private val random: Random = Random.Default) : ArcadeGame {
    override val width = 10f
    override val height = 20f
    override val intervalMs = 470L
    override var score = 0
        private set
    override var finished = false
        private set

    private val board = IntArray(200)
    private val shapes: List<List<GridCell>> = listOf(
        listOf(GridCell(0, 1), GridCell(1, 1), GridCell(2, 1), GridCell(3, 1)),
        listOf(GridCell(1, 0), GridCell(2, 0), GridCell(1, 1), GridCell(2, 1)),
        listOf(GridCell(1, 0), GridCell(0, 1), GridCell(1, 1), GridCell(2, 1)),
        listOf(GridCell(1, 0), GridCell(2, 0), GridCell(0, 1), GridCell(1, 1)),
        listOf(GridCell(0, 0), GridCell(1, 0), GridCell(1, 1), GridCell(2, 1)),
        listOf(GridCell(0, 0), GridCell(0, 1), GridCell(1, 1), GridCell(2, 1)),
        listOf(GridCell(2, 0), GridCell(0, 1), GridCell(1, 1), GridCell(2, 1)),
    )
    private val palette = intArrayOf(
        ArcadeColors.blue, ArcadeColors.orange, ArcadeColors.purple,
        ArcadeColors.mint, ArcadeColors.red, 0xFFF5D77B.toInt(), 0xFF67CBDA.toInt(),
    )
    private var shape = shapes[0]
    private var x = 3
    private var y = -1
    private var kind = 0

    init { reset() }

    override fun reset() {
        board.fill(-1)
        score = 0
        finished = false
        spawn()
    }

    private fun spawn() {
        kind = random.nextInt(shapes.size)
        shape = shapes[kind]
        x = 3
        y = -1
        if (!canMove(shape, x, y)) finished = true
    }

    private fun canMove(cells: List<GridCell>, atX: Int, atY: Int): Boolean =
        cells.all { cell ->
            val cx = atX + cell.x
            val cy = atY + cell.y
            cx in 0..9 && cy < 20 && (cy < 0 || board[cy * 10 + cx] == -1)
        }

    private fun tryMove(dx: Int, dy: Int): Boolean {
        if (!canMove(shape, x + dx, y + dy)) return false
        x += dx
        y += dy
        return true
    }

    override fun input(key: PadKey) {
        if (finished) return
        when (key) {
            PadKey.LEFT -> tryMove(-1, 0)
            PadKey.RIGHT -> tryMove(1, 0)
            PadKey.DOWN -> { if (tryMove(0, 1)) score++ else lock() }
            PadKey.UP -> {
                if (kind != 1) {
                    val rotated = shape.map { GridCell(3 - it.y, it.x) }
                    // Small wall-kicks keep rotations playable near edges.
                    for (dx in intArrayOf(0, -1, 1, -2, 2)) {
                        if (canMove(rotated, x + dx, y)) {
                            shape = rotated
                            x += dx
                            break
                        }
                    }
                }
            }
            PadKey.ACTION -> {
                var shifted = 0
                while (tryMove(0, 1)) shifted++
                score += shifted * 2
                lock()
            }
        }
    }

    override fun tick() {
        if (!finished && !tryMove(0, 1)) lock()
    }

    private fun lock() {
        for (cell in shape) {
            val bx = x + cell.x
            val by = y + cell.y
            if (by < 0) {
                finished = true
                return
            }
            board[by * 10 + bx] = kind
        }
        var cleared = 0
        var dst = 19
        for (row in 19 downTo 0) {
            val full = (0 until 10).all { board[row * 10 + it] >= 0 }
            if (full) {
                cleared++
            } else {
                for (col in 0 until 10) board[dst * 10 + col] = board[row * 10 + col]
                dst--
            }
        }
        while (dst >= 0) {
            for (col in 0 until 10) board[dst * 10 + col] = -1
            dst--
        }
        if (cleared > 0) score += intArrayOf(0, 100, 300, 500, 800)[cleared.coerceAtMost(4)]
        spawn()
    }

    override fun paint(painter: ArcadePainter) {
        painter.box(0f, 0f, width, height, ArcadeColors.backdrop)
        for (i in 0 until board.size) {
            val bx = i % 10
            val by = i / 10
            val k = board[i]
            painter.box(bx + .04f, by + .04f, .92f, .92f,
                if (k == -1) ArcadeColors.grid else palette[k])
        }
        if (!finished) for (cell in shape) {
            val bx = x + cell.x
            val by = y + cell.y
            if (by >= 0) painter.box(bx + .04f, by + .04f, .92f, .92f, palette[kind])
        }
    }
}
