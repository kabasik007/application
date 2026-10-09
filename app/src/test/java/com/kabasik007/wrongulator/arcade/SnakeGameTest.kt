package com.kabasik007.wrongulator.arcade

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SnakeGameTest {
    @Test fun resetRestoresAllRuntimeProperties() {
        val game = SnakeGame(Random(21))
        repeat(4) { game.tick() }
        game.reset()
        assertEquals(0, game.score)
        assertEquals(0, game.steps)
        assertEquals(3, game.length)
        assertEquals(SnakeEvent.NONE, game.lastEvent)
        assertFalse(game.finished)
        assertEquals(155L, game.intervalMs)
    }

    @Test fun oppositeDirectionIsRejected() {
        val game = SnakeGame(Random(8))
        game.input(PadKey.LEFT) // initial heading RIGHT
        game.tick()
        assertEquals(1, game.steps)
        val points = ArrayList<Pair<Float, Float>>()
        val painter = object : ArcadePainter {
            override fun box(x: Float, y: Float, width: Float, height: Float, argb: Int) {
                if (argb == 0xFFB6FFE0.toInt()) points.add(x to y)
            }
            override fun disc(x: Float, y: Float, radius: Float, argb: Int) = Unit
            override fun line(
                x1: Float, y1: Float, x2: Float, y2: Float, argb: Int, stroke: Float,
            ) = Unit
        }
        game.paint(painter)
        assertTrue(points.isNotEmpty())
        assertTrue(points.first().first > 8f)
    }

    @Test fun crashAndResetAreStable() {
        val game = SnakeGame(Random(1))
        repeat(20) { game.tick() }
        assertTrue(game.finished)
        assertEquals(SnakeEvent.CRASH, game.lastEvent)
        val afterCrash = game.steps
        game.tick()
        assertEquals(afterCrash, game.steps)
        game.reset()
        assertFalse(game.finished)
    }

    @Test fun smoothFrameRenderingIsReadOnlyAndClampsProgress() {
        val game = SnakeGame(Random(2))
        game.tick()
        val before = game.steps
        var rendered = 0
        val painter = object : ArcadePainter {
            override fun box(x: Float, y: Float, width: Float, height: Float, argb: Int) {
                rendered++
                assertTrue(width >= 0f && height >= 0f)
            }
            override fun disc(x: Float, y: Float, radius: Float, argb: Int) {
                rendered++
                assertTrue(radius >= 0f)
            }
            override fun line(
                x1: Float, y1: Float, x2: Float, y2: Float, argb: Int, stroke: Float,
            ) { rendered++ }
        }
        listOf(-1f, 0f, .25f, .8f, 1f, 3f).forEach { game.paintSmooth(painter, it) }
        assertTrue(rendered > 20)
        assertEquals(before, game.steps)
    }
}
