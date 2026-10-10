package com.kabasik007.wrongulator.arcade

import kotlin.math.abs
import kotlin.random.Random

internal data class SnakeRival(var cell: GridCell, val color: Int, val trail: ArrayDeque<GridCell>)
internal data class SnakeBoss(var cell: GridCell, var health: Int)
internal data class BlastOutcome(
    val impact: GridCell,
    val destroyed: Int,
    val bossDamage: Int,
    val bossDefeated: Boolean,
)

/** Pure-world collisions, hazards, AI and bounded destructible structures. */
internal class SnakeWorld(private val random: Random, private val rules: SnakeRules) {
    val obstacles = linkedSetOf<GridCell>()
    val hazards = linkedSetOf<GridCell>()
    val rivals = mutableListOf<SnakeRival>()
    var boss: SnakeBoss? = null
        private set
    var bossDefeated = false
        private set
    var destroyedTotal = 0
        private set

    fun reset() {
        obstacles.clear()
        hazards.clear()
        rivals.clear()
        boss = null
        bossDefeated = false
        destroyedTotal = 0
        if (rules.canShoot) {
            obstacles.addAll(listOf(GridCell(15, 13), GridCell(5, 6), GridCell(14, 19)))
        }
        if (rules.map == SnakeMap.HAZARDS) {
            for (cell in listOf(GridCell(3, 5), GridCell(16, 5),
                    GridCell(3, 20), GridCell(16, 20), GridCell(10, 4))) {
                hazards.add(cell)
            }
        }
    }

    fun isBlocked(cell: GridCell): Boolean =
        cell in obstacles || cell in hazards ||
            rivals.any { cell in it.trail } ||
            (boss?.let { abs(cell.x - it.cell.x) <= 1 && abs(cell.y - it.cell.y) <= 1 } == true)

    fun spawnObstacle(body: Collection<GridCell>, food: GridCell) {
        if (!rules.canShoot || obstacles.size >= 12) return
        val start = random.nextInt(520)
        for (offset in 0 until 520) {
            val n = (start + offset) % 520
            val candidate = GridCell(n % 20, n / 20)
            val head = body.first()
            if (candidate !in obstacles && candidate !in hazards && candidate !in body &&
                candidate != food && !isBlocked(candidate) &&
                abs(candidate.x - head.x) + abs(candidate.y - head.y) >= 5
            ) {
                obstacles.add(candidate)
                return
            }
        }
    }

    fun maybeSpawnRivals(foodCount: Int) {
        if (!rules.canShoot || foodCount < 2 || rivals.size >= rules.rivals) return
        val locations = listOf(GridCell(2, 3), GridCell(17, 3), GridCell(2, 23))
        val spot = locations[rivals.size]
        if (!isBlocked(spot)) {
            rivals.add(SnakeRival(spot,
                listOf(ArcadeColors.red, ArcadeColors.purple, ArcadeColors.orange)[rivals.size],
                ArrayDeque<GridCell>().apply { addFirst(spot) }))
        }
    }

    fun maybeSpawnBoss(points: Int) {
        if (!rules.canShoot || points < 120 || boss != null || bossDefeated) return
        val spot = GridCell(10, 3)
        if (!isBlocked(spot)) boss = SnakeBoss(spot, health = 4)
    }

    /** Tiny local deterministic AI, no threads or pathfinders. */
    fun moveRivals(step: Int, snakeBody: Collection<GridCell>) {
        if (step % 4 != 0) return
        for ((index, rival) in rivals.withIndex()) {
            val target = snakeBody.first()
            val variants = listOf(
                GridCell(rival.cell.x + if (target.x >= rival.cell.x) 1 else -1, rival.cell.y),
                GridCell(rival.cell.x, rival.cell.y + if (target.y >= rival.cell.y) 1 else -1),
                GridCell(rival.cell.x + if (index % 2 == 0) 1 else -1, rival.cell.y),
            )
            val next = variants.firstOrNull {
                it.x in 1..18 && it.y in 1..24 &&
                    it !in obstacles && it !in hazards && it !in rival.trail
            }
            if (next != null) {
                rival.cell = next
                rival.trail.addFirst(next)
                while (rival.trail.size > 3) rival.trail.removeLast()
            }
        }
        val current = boss ?: return
        if (step % 8 == 0) {
            val horizontal = if ((step / 8) % 2 == 0) 1 else -1
            val nextX = (current.cell.x + horizontal).coerceIn(4, 16)
            current.cell = current.cell.copy(x = nextX)
        }
    }

    fun fire(origin: GridCell, dx: Int, dy: Int, weapon: SnakeWeapon): BlastOutcome {
        var last = origin
        var stepX = dx
        var stepY = dy
        var bounced = false
        val range = if (weapon == SnakeWeapon.BOUNCE) 13 else 8
        for (distance in 1..range) {
            var target = GridCell(last.x + stepX, last.y + stepY)
            if (target.x !in 0..19 || target.y !in 0..25) {
                if (weapon != SnakeWeapon.BOUNCE || bounced) break
                bounced = true
                stepX = -stepX
                stepY = -stepY
                target = GridCell(last.x + stepX, last.y + stepY)
            }
            last = target
            val currentBoss = boss
            if (target in obstacles || (currentBoss != null &&
                abs(target.x - currentBoss.cell.x) <= 1 &&
                abs(target.y - currentBoss.cell.y) <= 1)) break
        }
        val radius = if (weapon == SnakeWeapon.CHAIN) 2 else 1
        val touched = obstacles.filter {
            abs(it.x - last.x) <= radius && abs(it.y - last.y) <= radius
        }.toMutableSet()
        if (weapon == SnakeWeapon.CHAIN) {
            // At most 12 crates. Chain reactions are intentionally bounded.
            var changed: Boolean
            do {
                val old = touched.size
                touched.addAll(obstacles.filter { candidate ->
                    touched.any { destroyed ->
                        abs(candidate.x - destroyed.x) <= 2 &&
                            abs(candidate.y - destroyed.y) <= 2
                    }
                })
                changed = touched.size != old
            } while (changed && touched.size < 12)
        }
        obstacles.removeAll(touched)
        destroyedTotal += touched.size
        val targetBoss = boss
        val bossDamage = if (targetBoss != null &&
            abs(last.x - targetBoss.cell.x) <= radius + 1 &&
            abs(last.y - targetBoss.cell.y) <= radius + 1
        ) if (weapon == SnakeWeapon.CHAIN) 2 else 1 else 0
        if (bossDamage > 0 && targetBoss != null) {
            targetBoss.health -= bossDamage
            if (targetBoss.health <= 0) {
                boss = null
                bossDefeated = true
            }
        }
        return BlastOutcome(last, touched.size, bossDamage, bossDefeated)
    }
}
