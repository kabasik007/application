package com.kabasik007.wrongulator.arcade

import kotlin.math.abs
import kotlin.math.sin

/**
 * Original side-scrolling bike challenge (not a clone of any commercial game).
 * Right boosts, Left brakes, Up/Action jumps; survive the ramps and cones.
 */
class MotoTrailGame : ArcadeGame {
    override val width = 100f
    override val height = 65f
    override val intervalMs = 45L
    override var score = 0
        private set
    override var finished = false
        private set

    private var x = 12f
    private var y = 0f
    private var verticalSpeed = 0f
    private var speed = 15f
    private var airborne = false
    private var jumps = 0

    init { reset() }

    override fun reset() {
        x = 12f
        y = terrain(x) - 3.1f
        verticalSpeed = 0f
        speed = 15f
        airborne = false
        jumps = 0
        score = 0
        finished = false
    }

    private fun terrain(at: Float): Float =
        46f + (sin(at.toDouble() * .07) * 5.0 + sin(at.toDouble() * .23) * 2.5).toFloat()

    override fun input(key: PadKey) {
        if (finished) return
        when (key) {
            PadKey.LEFT -> speed = (speed - 4f).coerceAtLeast(5f)
            PadKey.RIGHT -> speed = (speed + 2.2f).coerceAtMost(27f)
            PadKey.UP, PadKey.ACTION -> {
                if (!airborne) {
                    verticalSpeed = -26f
                    airborne = true
                    jumps++
                }
            }
            PadKey.DOWN -> speed = (speed - 2f).coerceAtLeast(5f)
        }
    }

    override fun tick() {
        if (finished) return
        val dt = intervalMs / 1000f
        speed = (speed * .997f).coerceAtLeast(7f)
        x += speed * dt
        val roadY = terrain(x) - 3.1f

        if (airborne) {
            verticalSpeed += 55f * dt
            y += verticalSpeed * dt
            if (y >= roadY) {
                if (verticalSpeed > 34f) {
                    finished = true
                }
                y = roadY
                verticalSpeed = 0f
                airborne = false
            }
        } else {
            y = roadY
        }
        // Cone clusters placed on the course. Jump to clear them.
        val segment = (x / 33f).toInt()
        for (index in (segment - 1).coerceAtLeast(0)..segment + 2) {
            val coneX = 36f + index * 33f
            if (abs(x - coneX) < 2.8f && y > terrain(coneX) - 9f) {
                finished = true
            }
        }
        if (y > height + 12f) finished = true
        score = (x - 12f).toInt().coerceAtLeast(0) + jumps * 5
    }

    override fun paint(painter: ArcadePainter) {
        painter.box(0f, 0f, width, height, ArcadeColors.backdrop)
        painter.disc(82f, 12f, 7f, ArcadeColors.purple)
        for (i in 0 until 8) {
            val cloudX = (i * 21f - (x * .08f) % 120f + 120f) % 120f
            painter.box(cloudX - 8f, 17f + (i % 3) * 5f, 18f, 2.0f, ArcadeColors.grid)
        }
        val camera = x - 24f
        for (sx in 0..100 step 2) {
            val wx = camera + sx
            val nextWx = wx + 2f
            painter.line(sx.toFloat(), terrain(wx), (sx + 2).toFloat(), terrain(nextWx),
                ArcadeColors.mint, 1.35f)
            painter.box(sx.toFloat(), terrain(wx) + 1f, 2f, 65f - terrain(wx), ArcadeColors.grid)
        }
        val firstIndex = ((camera - 36f) / 33f).toInt() - 1
        for (i in firstIndex.coerceAtLeast(0)..firstIndex + 6) {
            val coneX = 36f + i * 33f
            val sx = coneX - camera
            if (sx > -5f && sx < 105f) {
                val ty = terrain(coneX)
                painter.box(sx - 1.7f, ty - 5.7f, 3.4f, 5.7f, ArcadeColors.orange)
                painter.box(sx - 2.7f, ty - .6f, 5.4f, 1f, ArcadeColors.white)
            }
        }
        val bikeX = x - camera
        val wheelY = y + 2.0f
        painter.disc(bikeX - 3f, wheelY, 2.4f, ArcadeColors.white)
        painter.disc(bikeX + 3f, wheelY, 2.4f, ArcadeColors.white)
        painter.line(bikeX - 3f, wheelY, bikeX, y - 1f, ArcadeColors.blue, 1.2f)
        painter.line(bikeX + 3f, wheelY, bikeX, y - 1f, ArcadeColors.blue, 1.2f)
        painter.line(bikeX - 3f, wheelY, bikeX + 3f, wheelY, ArcadeColors.blue, .7f)
        painter.disc(bikeX, y - 5.4f, 2f, ArcadeColors.orange)
        painter.line(bikeX, y - 3.4f, bikeX, y, ArcadeColors.white, 1.2f)
    }
}
