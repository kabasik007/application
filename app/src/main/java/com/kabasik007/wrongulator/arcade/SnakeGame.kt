package com.kabasik007.wrongulator.arcade

import kotlin.math.abs
import kotlin.random.Random

enum class SnakeEvent {
    NONE, EAT, CRASH, POWERUP, SHIELD_HIT, PORTAL, TIME_UP, BOSS_DEFEATED,
}

/** Visual shot, no real-world ballistics or weapon behavior. */
data class SnakeBlast(val origin: GridCell, val impact: GridCell, val destroyed: Int)

/** Pure Kotlin Snake simulation. All timers advance only when the orchestrator calls tick(). */
class SnakeGame(
    private val random: Random = Random.Default,
    val rules: SnakeRules = SnakeRules(),
    val replaySeed: Int? = null,
    private val recording: Boolean = true,
) : ArcadeGame {
    override val width = 20f
    override val height = 26f
    override val intervalMs: Long
        get() {
            val base = when (rules.difficulty) {
                SnakeDifficulty.CASUAL -> 190L
                SnakeDifficulty.NORMAL -> 155L
                SnakeDifficulty.EXPERT -> 112L
            }
            val acceleration = (score / 20 * 6L).coerceAtMost(75L)
            return (base - acceleration + if (activePower == SnakePower.SLOW_MOTION) 65L else 0L)
                .coerceIn(65L, 270L)
        }

    override var score = 0
        private set
    override var finished = false
        private set
    var lastEvent = SnakeEvent.NONE
        private set
    var steps = 0
        private set

    internal val body = ArrayDeque<GridCell>()
    internal var previousBody: List<GridCell> = emptyList()
        private set
    internal var direction = PadKey.RIGHT
        private set
    private val turnQueue = SnakeTurnQueue()
    internal var food = GridCell(14, 13)
        private set
    internal var snack = food
        private set
    internal var sparkTicks = 0
        private set
    internal var blastTicks = 0
        private set
    internal val world = SnakeWorld(random, rules)

    var combo = 0
        private set
    var maxCombo = 0
        private set
    var activePower: SnakePower? = null
        private set
    var powerTicks = 0
        private set
    var powerCell: GridCell? = null
        private set
    var powerToCollect = SnakePower.SHIELD
        private set
    var selectedWeapon = SnakeWeapon.GRENADE
        private set
    var lastBlast: SnakeBlast? = null
        private set
    var timeMs = 0L
        private set
    var hits = 0
        private set
    var foodCollected = 0
        private set
    private var lastFoodStep = -1000
    private var grenades = 3
    private val commands = ArrayList<SnakeReplayAction>(256)

    val ammo get() = if (rules.canShoot) grenades else 0
    val length get() = body.size
    val obstaclesRemaining get() = world.obstacles.size
    val destroyedTotal get() = world.destroyedTotal
    val bossHealth get() = world.boss?.health ?: 0
    val bossDefeated get() = world.bossDefeated
    val rivalCount get() = world.rivals.size
    val timeRemainingMs get() = if (rules.mode == SnakeMode.TIME_ATTACK)
        (60_000L - timeMs).coerceAtLeast(0L) else 0L

    val stats get() = SnakeRunStats(steps, foodCollected, destroyedTotal, length,
        combo, maxCombo, bossDefeated)

    val earnedAchievements: Set<SnakeAchievement>
        get() = buildSet {
            if (foodCollected > 0) add(SnakeAchievement.FIRST_FOOD)
            if (score >= 100) add(SnakeAchievement.SCORE_100)
            if (score >= 500) add(SnakeAchievement.SCORE_500)
            if (destroyedTotal >= 10) add(SnakeAchievement.DESTROY_10)
            if (length >= 20) add(SnakeAchievement.LENGTH_20)
            if (timeMs >= 60_000L) add(SnakeAchievement.SURVIVE_60)
            if (timeMs >= 60_000L && hits == 0) add(SnakeAchievement.NO_HIT_60)
            if (bossDefeated) add(SnakeAchievement.BOSS_WIN)
        }

    init { reset() }

    override fun reset() {
        body.clear()
        body.addAll(listOf(GridCell(8, 13), GridCell(7, 13), GridCell(6, 13)))
        previousBody = body.toList()
        direction = PadKey.RIGHT
        turnQueue.reset(direction)
        score = 0
        finished = false
        lastEvent = SnakeEvent.NONE
        steps = 0
        grenades = 3
        combo = 0
        maxCombo = 0
        lastFoodStep = -1000
        timeMs = 0L
        hits = 0
        foodCollected = 0
        sparkTicks = 0
        blastTicks = 0
        activePower = null
        powerTicks = 0
        powerCell = null
        lastBlast = null
        selectedWeapon = SnakeWeapon.GRENADE
        commands.clear()
        world.reset()
        nextFood()
    }

    fun chooseWeapon(weapon: SnakeWeapon) {
        selectedWeapon = weapon
    }

    override fun input(key: PadKey) {
        if (finished || !turnQueue.offer(key)) return
        if (recording && commands.size < SnakeReplayCodec.MAX_ACTIONS) {
            commands.add(SnakeReplayAction(steps, direction = key))
        }
    }

    fun exportReplay(): String? {
        val seed = replaySeed ?: return null
        return SnakeReplayCodec.encode(SnakeReplay(seed, rules, commands.toList()))
    }

    companion object {
        fun fromReplay(trace: SnakeReplay): SnakeGame =
            SnakeGame(Random(trace.seed), trace.rules, trace.seed, recording = false)
    }

    override fun tick() {
        if (finished) return
        lastEvent = SnakeEvent.NONE
        if (sparkTicks > 0) sparkTicks--
        if (blastTicks > 0) blastTicks--
        if (powerTicks > 0) {
            powerTicks--
            if (powerTicks == 0) activePower = null
        }

        val elapsedThisTick = intervalMs
        timeMs += elapsedThisTick
        world.moveRivals(steps, body)
        direction = turnQueue.advance()
        val head = body.first()
        var next = when (direction) {
            PadKey.LEFT -> GridCell(head.x - 1, head.y)
            PadKey.RIGHT -> GridCell(head.x + 1, head.y)
            PadKey.UP -> GridCell(head.x, head.y - 1)
            else -> GridCell(head.x, head.y + 1)
        }

        if (rules.map == SnakeMap.WRAP || rules.isInvincible) {
            next = GridCell((next.x + 20) % 20, (next.y + 26) % 26)
        }
        if (rules.map == SnakeMap.PORTALS) {
            next = when (next) {
                GridCell(1, 3) -> GridCell(18, 22)
                GridCell(18, 22) -> GridCell(1, 3)
                else -> next
            }
            if (next != GridCell(head.x + directionDx(), head.y + directionDy())) {
                lastEvent = SnakeEvent.PORTAL
            }
        }

        val eating = next == food
        val collides = next.x !in 0..19 || next.y !in 0..25 ||
            body.withIndex().any { (eating || it.index != body.size - 1) && it.value == next } ||
            world.isBlocked(next)

        if (collides) {
            hits++
            if (rules.isInvincible || activePower == SnakePower.SHIELD) {
                if (activePower == SnakePower.SHIELD) {
                    activePower = null
                    powerTicks = 0
                }
                lastEvent = SnakeEvent.SHIELD_HIT
                steps++
                checkTimeLimit()
                return
            }
            lastEvent = SnakeEvent.CRASH
            finished = true
            return
        }

        previousBody = body.toList()
        body.addFirst(next)
        steps++

        if (eating || (activePower == SnakePower.MAGNET &&
            abs(next.x - food.x) + abs(next.y - food.y) <= 2)) {
            val shortGap = steps - lastFoodStep <= 35
            combo = if (shortGap) (combo + 1).coerceAtMost(5) else 1
            maxCombo = maxOf(maxCombo, combo)
            lastFoodStep = steps
            foodCollected++
            val multiplier = if (activePower == SnakePower.DOUBLE_SCORE) 2 else 1
            score += 10 * combo * multiplier
            if (rules.canShoot && foodCollected % 2 == 0) grenades = (grenades + 1).coerceAtMost(5)
            if (rules.canShoot && foodCollected % 3 == 0) world.spawnObstacle(body, food)
            world.maybeSpawnRivals(foodCollected)
            world.maybeSpawnBoss(score)
            snack = food
            sparkTicks = 4
            lastEvent = SnakeEvent.EAT
            nextFood()
            if (foodCollected % 2 == 0 && powerCell == null && rules.mode != SnakeMode.CLASSIC) {
                spawnPower()
            }
        } else {
            body.removeLast()
        }

        if (next == powerCell) {
            activePower = powerToCollect
            powerTicks = 50
            powerCell = null
            lastEvent = SnakeEvent.POWERUP
        }
        if (body.size == 520) finished = true
        checkTimeLimit()
    }

    private fun checkTimeLimit() {
        if (rules.mode == SnakeMode.TIME_ATTACK && timeMs >= 60_000L) {
            finished = true
            lastEvent = SnakeEvent.TIME_UP
        }
    }

    private fun directionDx() = when (direction) {
        PadKey.LEFT -> -1
        PadKey.RIGHT -> 1
        else -> 0
    }

    private fun directionDy() = when (direction) {
        PadKey.UP -> -1
        PadKey.DOWN -> 1
        else -> 0
    }

    private fun findFreeCell(): GridCell? {
        val initial = random.nextInt(520)
        repeat(520) { offset ->
            val id = (initial + offset) % 520
            val candidate = GridCell(id % 20, id / 20)
            if (candidate !in body && candidate != food && !world.isBlocked(candidate) &&
                candidate != powerCell
            ) return candidate
        }
        return null
    }

    private fun nextFood() {
        val initial = random.nextInt(520)
        repeat(520) { offset ->
            val id = (initial + offset) % 520
            val candidate = GridCell(id % 20, id / 20)
            if (candidate !in body && !world.isBlocked(candidate) &&
                candidate != powerCell
            ) {
                food = candidate
                return
            }
        }
        finished = true
    }

    private fun spawnPower() {
        val cell = findFreeCell() ?: return
        powerCell = cell
        powerToCollect = SnakePower.entries[random.nextInt(SnakePower.entries.size)]
    }

    /** Cartoon-only grenade. Misses consume ammo and never affect the snake's body. */
    fun launchGrenade(): Boolean {
        if (finished || !rules.canShoot || grenades <= 0) return false
        grenades--
        if (recording && commands.size < SnakeReplayCodec.MAX_ACTIONS) {
            commands.add(SnakeReplayAction(steps, fire = selectedWeapon))
        }
        val origin = body.first()
        val shot = world.fire(origin, directionDx(), directionDy(), selectedWeapon)
        score += shot.destroyed * 15 + shot.bossDamage * 20
        if (shot.bossDefeated) {
            score += 100
            lastEvent = SnakeEvent.BOSS_DEFEATED
        }
        lastBlast = SnakeBlast(origin, shot.impact, shot.destroyed)
        blastTicks = 5
        return true
    }

    override fun paint(painter: ArcadePainter) = paintSmooth(painter, 1f)

    fun paintSmooth(painter: ArcadePainter, progress: Float) =
        SnakeVisuals.paint(this, painter, progress)
}
