package com.kabasik007.wrongulator.arcade

import kotlin.math.abs
import kotlin.random.Random

/**
 * Original flag-colored ball dodging arcade, with no copied artwork or licensed assets.
 * Avoid incoming balls, collect coins and use the action shield when available.
 */
class CountryballsGame(private val random: Random = Random.Default) : ArcadeGame {
    override val width = 100f
    override val height = 150f
    override val intervalMs = 60L
    override var score = 0
        private set
    override var finished = false
        private set

    private data class Falling(var x: Float, var y: Float, val bad: Boolean, val color: Int)
    private val objects = ArrayList<Falling>(24)
    private val colors = intArrayOf(ArcadeColors.red, ArcadeColors.blue, ArcadeColors.purple)
    private var playerX = 50f
    private var elapsed = 0
    private var shieldTicks = 0
    private var cooldown = 0

    init { reset() }

    override fun reset() {
        playerX = 50f
        score = 0
        elapsed = 0
        shieldTicks = 0
        cooldown = 0
        objects.clear()
        finished = false
    }

    override fun input(key: PadKey) {
        if (finished) return
        when (key) {
            PadKey.LEFT -> playerX = (playerX - 7f).coerceAtLeast(7f)
            PadKey.RIGHT -> playerX = (playerX + 7f).coerceAtMost(93f)
            PadKey.UP, PadKey.ACTION -> {
                if (cooldown == 0) {
                    shieldTicks = 22
                    cooldown = 130
                }
            }
            PadKey.DOWN -> Unit
        }
    }

    override fun tick() {
        if (finished) return
        elapsed++
        score += 1
        if (shieldTicks > 0) shieldTicks--
        if (cooldown > 0) cooldown--

        val frequency = (12 - elapsed / 300).coerceAtLeast(5)
        if (elapsed % frequency == 0 && objects.size < 35) {
            objects.add(
                Falling(
                    x = random.nextInt(10, 91).toFloat(),
                    y = -8f,
                    bad = random.nextInt(6) != 0,
                    color = colors[random.nextInt(colors.size)],
                )
            )
        }
        val iter = objects.iterator()
        while (iter.hasNext()) {
            val ball = iter.next()
            ball.y += (2.3f + elapsed / 1000f).coerceAtMost(5f)
            if (abs(ball.x - playerX) < 10f && abs(ball.y - 132f) < 9f) {
                iter.remove()
                if (ball.bad && shieldTicks <= 0) {
                    finished = true
                    break
                }
                if (!ball.bad) score += 30
            } else if (ball.y > 165f) {
                iter.remove()
            }
        }
    }

    override fun paint(painter: ArcadePainter) {
        painter.box(0f, 0f, width, height, ArcadeColors.backdrop)
        for (i in 0..8) {
            val x = i * 13f
            painter.line(x, 0f, x, height, ArcadeColors.grid, .4f)
        }
        for (item in objects) {
            painter.disc(item.x, item.y, if (item.bad) 6f else 4f, item.color.takeIf { item.bad } ?: ArcadeColors.orange)
            if (item.bad) {
                painter.box(item.x - 5f, item.y - 1f, 10f, 2f, ArcadeColors.white)
            } else {
                painter.disc(item.x, item.y, 1.5f, ArcadeColors.white)
            }
        }
        painter.box(0f, 141f, 100f, 9f, ArcadeColors.grid)
        if (shieldTicks > 0) painter.disc(playerX, 132f, 9.3f, ArcadeColors.mint)
        painter.disc(playerX, 132f, 7f, ArcadeColors.white)
        // Generic countryball motifs: two blue and yellow horizontal stripes, eyes.
        painter.box(playerX - 6.6f, 132f, 13.2f, 5.4f, ArcadeColors.blue)
        painter.disc(playerX - 2.4f, 130f, 1.9f, ArcadeColors.white)
        painter.disc(playerX + 2.4f, 130f, 1.9f, ArcadeColors.white)
        painter.disc(playerX - 2.2f, 130f, .65f, ArcadeColors.backdrop)
        painter.disc(playerX + 2.6f, 130f, .65f, ArcadeColors.backdrop)
    }
}
