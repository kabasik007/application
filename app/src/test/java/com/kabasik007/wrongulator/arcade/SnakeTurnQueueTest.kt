package com.kabasik007.wrongulator.arcade

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SnakeTurnQueueTest {
    @Test fun rapidValidTurnsAreConsumedOnTwoSeparateLogicTicks() {
        val buffer = SnakeTurnQueue()
        assertTrue(buffer.offer(PadKey.UP))
        assertTrue(buffer.offer(PadKey.LEFT))
        assertEquals(PadKey.UP, buffer.advance())
        assertEquals(PadKey.LEFT, buffer.advance())
    }

    @Test fun cannotReverseOrFillQueueBeyondTwoTurns() {
        val buffer = SnakeTurnQueue()
        assertFalse(buffer.offer(PadKey.LEFT)) // moving RIGHT
        assertTrue(buffer.offer(PadKey.UP))
        assertFalse(buffer.offer(PadKey.DOWN)) // reverse of queued UP
        assertTrue(buffer.offer(PadKey.LEFT))
        assertFalse(buffer.offer(PadKey.DOWN)) // bounded queue
        assertEquals(PadKey.UP, buffer.advance())
        assertEquals(PadKey.LEFT, buffer.advance())
        buffer.reset()
        assertEquals(PadKey.RIGHT, buffer.advance())
    }

    @Test fun aTapDuringStartupCanBeBufferedBeforeFirstTick() {
        val game = SnakeGame(Random(27), SnakeRules(mode = SnakeMode.CLASSIC))
        game.input(PadKey.UP) // while visual transition is in progress
        game.tick() // first playable logic step
        assertEquals(PadKey.UP, game.direction)
        assertEquals(1, game.steps)
        assertFalse(game.finished)
    }

    @Test fun rapidSwipeSequenceDoesNotKillTheSnakeOnItsFirstTick() {
        val game = SnakeGame(Random(21), SnakeRules(mode = SnakeMode.CLASSIC))
        game.input(PadKey.UP)
        game.input(PadKey.LEFT)
        game.tick()
        assertEquals(PadKey.UP, game.direction)
        assertFalse(game.finished)
        game.tick()
        assertEquals(PadKey.LEFT, game.direction)
        assertFalse(game.finished)
    }
}
