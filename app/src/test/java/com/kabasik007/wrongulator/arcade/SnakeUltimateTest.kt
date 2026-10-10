package com.kabasik007.wrongulator.arcade

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SnakeUltimateTest {
    @Test fun modeRulesAreIndependent() {
        val classic = SnakeGame(Random(5), SnakeRules(mode = SnakeMode.CLASSIC))
        val zen = SnakeGame(Random(5), SnakeRules(mode = SnakeMode.ZEN))
        val combat = SnakeGame(Random(5))
        assertEquals(0, classic.obstaclesRemaining)
        assertEquals(0, zen.obstaclesRemaining)
        assertEquals(3, combat.obstaclesRemaining)
        assertFalse(classic.launchGrenade())
        assertEquals(0, classic.ammo)
    }

    @Test fun difficultyAffectsOnlyGameClock() {
        val casual = SnakeGame(Random(2), SnakeRules(difficulty = SnakeDifficulty.CASUAL))
        val expert = SnakeGame(Random(2), SnakeRules(difficulty = SnakeDifficulty.EXPERT))
        assertTrue(casual.intervalMs > expert.intervalMs)
        assertEquals(0, casual.score)
        assertEquals(0, expert.score)
    }

    @Test fun arenaWrapAllowsCrossingWallsWithoutDeath() {
        val game = SnakeGame(Random(31), SnakeRules(
            mode = SnakeMode.CLASSIC, map = SnakeMap.WRAP,
        ))
        repeat(13) { game.tick() }
        assertFalse(game.finished)
        assertEquals(13, game.steps)
    }

    @Test fun expertHazardMapHasAdditionalHazards() {
        val world = SnakeWorld(Random(3), SnakeRules(map = SnakeMap.HAZARDS))
        world.reset()
        assertEquals(5, world.hazards.size)
        assertTrue(world.isBlocked(GridCell(3, 5)))
    }

    @Test fun boundedEnemyCountAndBossLifecycle() {
        val world = SnakeWorld(Random(7), SnakeRules(rivals = 3))
        world.reset()
        repeat(7) { world.maybeSpawnRivals(3) }
        assertTrue(world.rivals.size <= 3)
        world.maybeSpawnBoss(120)
        assertNotNull(world.boss)
        repeat(4) {
            world.fire(GridCell(10, 10), 0, -1, SnakeWeapon.GRENADE)
        }
        assertTrue(world.bossDefeated)
        assertNull(world.boss)
        world.reset()
        assertFalse(world.bossDefeated)
    }

    @Test fun weaponModesConsumeAmmoAndKeepShotsBounded() {
        val game = SnakeGame(Random(6))
        game.chooseWeapon(SnakeWeapon.CHAIN)
        assertTrue(game.launchGrenade())
        assertEquals(2, game.ammo)
        game.chooseWeapon(SnakeWeapon.BOUNCE)
        assertTrue(game.launchGrenade())
        assertEquals(1, game.ammo)
        assertNotNull(game.lastBlast)
    }

    @Test fun replayCodecPreservesSettingsAndInputs() {
        val rules = SnakeRules(
            SnakeMode.TIME_ATTACK, SnakeDifficulty.EXPERT, SnakeMap.PORTALS,
            SnakeSkin.HIGH_CONTRAST, 2,
        )
        val trace = SnakeReplay(4421, rules, listOf(
            SnakeReplayAction(0, direction = PadKey.UP),
            SnakeReplayAction(2, fire = SnakeWeapon.CHAIN),
        ))
        val code = SnakeReplayCodec.encode(trace)
        assertEquals(trace, SnakeReplayCodec.decode(code))
        assertNull(SnakeReplayCodec.decode("garbage!!!"))
        assertNull(SnakeReplayCodec.decode("x".repeat(SnakeReplayCodec.MAX_ENCODED + 1)))
    }

    @Test fun replayReadyGameRecreatesRngSeed() {
        val original = SnakeGame(Random(144), SnakeRules(mode = SnakeMode.CLASSIC), 144)
        original.input(PadKey.UP)
        repeat(2) { original.tick() }
        val code = original.exportReplay()
        assertNotNull(code)
        val playback = SnakeGame.fromReplay(SnakeReplayCodec.decode(code!!)!!)
        playback.input(PadKey.UP)
        repeat(2) { playback.tick() }
        assertEquals(original.steps, playback.steps)
        assertEquals(original.score, playback.score)
    }

    @Test fun boundedArenaPaletteAndAchievements() {
        assertNotEquals(SnakePalette.head(SnakeSkin.MINT), SnakePalette.head(SnakeSkin.GOLD))
        val game = SnakeGame(Random(10))
        assertTrue(game.earnedAchievements.isEmpty())
        assertFalse(game.finished)
    }
}
