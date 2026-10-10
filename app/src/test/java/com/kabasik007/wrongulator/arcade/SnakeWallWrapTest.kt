package com.kabasik007.wrongulator.arcade

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** All four edges wrap, independently of selected mode or arena theme. */
class SnakeWallWrapTest {
    private fun classic(map: SnakeMap = SnakeMap.ARENA) = SnakeGame(
        Random(913), SnakeRules(mode = SnakeMode.CLASSIC, map = map),
    )

    @Test fun rightEdgeContinuesOnTheLeft() {
        val game = classic()
        repeat(12) { game.tick() } // initial x=8 -> 0 after 12 rightward steps
        assertFalse(game.finished)
        assertEquals(GridCell(0, 13), game.body.first())
    }

    @Test fun leftEdgeContinuesOnTheRight() {
        val game = classic()
        game.input(PadKey.UP)
        game.tick()
        game.input(PadKey.LEFT)
        repeat(9) { game.tick() }
        assertFalse(game.finished)
        assertEquals(GridCell(19, 12), game.body.first())
    }

    @Test fun upperEdgeContinuesAtBottom() {
        val game = classic()
        game.input(PadKey.UP)
        repeat(14) { game.tick() } // y=13 -> 25 after 14 upward steps
        assertFalse(game.finished)
        assertEquals(GridCell(8, 25), game.body.first())
    }

    @Test fun lowerEdgeContinuesAtTop() {
        val game = classic()
        game.tick() // x=9: get off the original body column
        game.input(PadKey.DOWN)
        repeat(13) { game.tick() } // y=13 -> 0 after 13 downward steps
        assertFalse(game.finished)
        assertEquals(GridCell(9, 0), game.body.first())
    }

    @Test fun passingTheWallAlsoWorksInCombatAndAllMapVariants() {
        for (map in SnakeMap.entries) {
            val game = SnakeGame(Random(227), SnakeRules(
                mode = SnakeMode.COMBAT, map = map, rivals = 0,
            ))
            game.input(PadKey.UP)
            game.tick()
            game.input(PadKey.RIGHT)
            repeat(12) { game.tick() } // safe y=12 lane (initial combat crate y=13)
            assertFalse("Map $map", game.finished)
            assertEquals("Map $map", GridCell(0, 12), game.body.first())
        }
    }

    @Test fun wrappingIntoTheOwnBodyStillLoses() {
        val game = classic()
        game.body.clear()
        game.body.addAll(listOf(GridCell(19, 12), GridCell(0, 12), GridCell(1, 12)))
        game.tick()
        assertTrue(game.finished)
        assertEquals(SnakeEvent.CRASH, game.lastEvent)
    }

    @Test fun wrappingIntoAnObstacleStillLoses() {
        val game = SnakeGame(Random(8))
        game.body.clear()
        game.body.addAll(listOf(GridCell(19, 13), GridCell(18, 13), GridCell(17, 13)))
        game.world.obstacles.add(GridCell(0, 13))
        game.tick()
        assertTrue(game.finished)
        assertEquals(SnakeEvent.CRASH, game.lastEvent)
    }

    @Test fun crossingDrawsAtBothEdgesInsteadOfSlidingAcrossTheScreen() {
        val game = classic()
        repeat(12) { game.tick() }
        val heads = mutableListOf<Float>()
        val headColor = SnakePalette.head(SnakeSkin.MINT)
        val recorder = object : ArcadePainter {
            override fun box(x: Float, y: Float, width: Float, height: Float, argb: Int) {
                if (argb == headColor) heads.add(x)
            }
            override fun disc(x: Float, y: Float, radius: Float, argb: Int) = Unit
            override fun line(x1: Float, y1: Float, x2: Float, y2: Float, argb: Int, stroke: Float) = Unit
        }
        game.paintSmooth(recorder, .5f)
        assertEquals(2, heads.size)
        assertTrue(heads.any { it > 19f })
        assertTrue(heads.any { it < 0f })
        assertFalse(heads.any { it in 3f..16f })
    }
}
