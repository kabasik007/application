package com.kabasik007.wrongulator.arcade

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArcadeContractTest {
    @Test fun gameFactoriesCreateIndependentSessions() {
        val factories: List<() -> ArcadeGame> = listOf(
            { SnakeGame(Random(1)) }, { BlockDropGame(Random(1)) },
            { CountryballsGame(Random(1)) }, { MotoTrailGame() },
        )
        for (factory in factories) {
            val first = factory()
            val second = factory()
            assertFalse(first === second)
            assertTrue(first.width > 0f && first.height > 0f && first.intervalMs in 16..1000)
            repeat(5) { first.tick() }
            first.reset()
            assertEquals(0, first.score)
            assertFalse(first.finished)
        }
    }

    @Test fun snakeHitsTheWallAndResetRestarts() {
        val game = SnakeGame(Random(12))
        repeat(24) { game.tick() }
        assertTrue(game.finished)
        game.reset()
        assertFalse(game.finished)
    }

    @Test fun blockDropHardDropLocksAndProducesNextShape() {
        val game = BlockDropGame(Random(22))
        game.input(PadKey.ACTION)
        assertTrue(game.score > 0)
        assertFalse(game.finished)
    }

    @Test fun countryballsBoundedMovementAndReset() {
        val game = CountryballsGame(Random(24))
        repeat(99) { game.input(PadKey.LEFT) }
        game.input(PadKey.ACTION)
        repeat(4) { game.tick() }
        assertTrue(game.score > 0)
        game.reset()
        assertEquals(0, game.score)
    }

    @Test fun bikeMovesAndCanJump() {
        val game = MotoTrailGame()
        game.input(PadKey.ACTION)
        repeat(12) { game.tick() }
        assertTrue(game.score > 0)
        game.reset()
        assertEquals(0, game.score)
    }

    @Test fun allGamesDrawWithoutExternalAssetsOrAndroidContext() {
        val painter = object : ArcadePainter {
            var count = 0
            override fun box(x: Float, y: Float, width: Float, height: Float, argb: Int) { count++ }
            override fun disc(x: Float, y: Float, radius: Float, argb: Int) { count++ }
            override fun line(
                x1: Float, y1: Float, x2: Float, y2: Float, argb: Int, stroke: Float,
            ) { count++ }
        }
        listOf(SnakeGame(), BlockDropGame(), CountryballsGame(), MotoTrailGame())
            .forEach {
                it.paint(painter)
                assertTrue(painter.count > 0)
            }
    }
}
