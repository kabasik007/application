package com.kabasik007.wrongulator.arcade

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Protect ammo, collision and reset behavior independently of the Compose UI. */
class SnakeGrenadeTest {
    @Test fun grenadeClearsFirstBarrelAndAwardsBonus() {
        val game = SnakeGame(Random(1))
        assertEquals(3, game.ammo)
        assertEquals(3, game.obstaclesRemaining)
        assertTrue(game.launchGrenade())
        assertEquals(2, game.ammo)
        assertEquals(2, game.obstaclesRemaining)
        assertEquals(15, game.score)
        assertEquals(GridCell(8, 13), game.lastBlast?.origin)
        assertEquals(GridCell(15, 13), game.lastBlast?.impact)
        assertEquals(1, game.lastBlast?.destroyed)
    }

    @Test fun repeatedShotsUseAmmoButNeverMakeItNegative() {
        val game = SnakeGame(Random(2))
        repeat(3) { assertTrue(game.launchGrenade()) }
        assertEquals(0, game.ammo)
        assertFalse(game.launchGrenade())
        assertEquals(0, game.ammo)
    }

    @Test fun launchWithoutTargetDoesNotAwardFreeScore() {
        val game = SnakeGame(Random(3))
        assertTrue(game.launchGrenade()) // destroys first barrel
        assertTrue(game.launchGrenade()) // misses
        assertEquals(15, game.score)
        assertEquals(0, game.lastBlast?.destroyed)
    }

    @Test fun obstacleCollisionFinishesSnakeAndPreventsShooting() {
        val game = SnakeGame(Random(4))
        repeat(7) { game.tick() } // right from x8 to obstacle x15
        assertTrue(game.finished)
        assertEquals(SnakeEvent.CRASH, game.lastEvent)
        assertFalse(game.launchGrenade())
        assertEquals(3, game.ammo)
    }

    @Test fun resetReloadsMagazineAndRestoresObstacles() {
        val game = SnakeGame(Random(5))
        game.launchGrenade()
        game.reset()
        assertEquals(3, game.ammo)
        assertEquals(3, game.obstaclesRemaining)
        assertEquals(0, game.score)
        assertEquals(null, game.lastBlast)
        assertFalse(game.finished)
    }

    @Test fun blastFramesAreReadOnly() {
        val game = SnakeGame(Random(6))
        assertTrue(game.launchGrenade())
        val score = game.score
        var draws = 0
        val painter = object : ArcadePainter {
            override fun box(x: Float, y: Float, width: Float, height: Float, argb: Int) { draws++ }
            override fun disc(x: Float, y: Float, radius: Float, argb: Int) {
                assertTrue(radius >= 0f)
                draws++
            }
            override fun line(x1: Float, y1: Float, x2: Float, y2: Float, argb: Int, stroke: Float) {
                draws++
            }
        }
        game.paintSmooth(painter, .4f)
        game.paintSmooth(painter, 1f)
        assertTrue(draws > 0)
        assertNotNull(game.lastBlast)
        assertEquals(score, game.score)
    }
}
