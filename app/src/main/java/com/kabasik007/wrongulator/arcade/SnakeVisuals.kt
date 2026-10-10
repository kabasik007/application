package com.kabasik007.wrongulator.arcade

import kotlin.math.cos
import kotlin.math.sin

/** Stateless, procedural rendering: no image allocations, Android APIs or asset loads. */
internal object SnakeVisuals {
    fun paint(game: SnakeGame, painter: ArcadePainter, progress: Float) {
        val t = progress.coerceIn(0f, 1f)
        painter.box(0f, 0f, game.width, game.height, 0xFF0B1626.toInt())
        val gridColor = if (game.rules.skin == SnakeSkin.HIGH_CONTRAST)
            0xFF385063.toInt() else 0xFF193044.toInt()
        for (x in 0..20) painter.line(x.toFloat(), 0f, x.toFloat(), 26f, gridColor, .03f)
        for (y in 0..26) painter.line(0f, y.toFloat(), 20f, y.toFloat(), gridColor, .03f)
        painter.line(.2f, .2f, 19.8f, .2f, 0xFF305367.toInt(), .09f)
        painter.line(.2f, 25.8f, 19.8f, 25.8f, 0xFF305367.toInt(), .09f)

        if (game.rules.map == SnakeMap.PORTALS) {
            for (portal in listOf(GridCell(1, 3), GridCell(18, 22))) {
                painter.disc(portal.x + .5f, portal.y + .5f, .47f, 0xFFBBA3FF.toInt())
                painter.disc(portal.x + .5f, portal.y + .5f, .27f, 0xFF0B1626.toInt())
                painter.disc(portal.x + .5f, portal.y + .5f, .12f, 0xFFBBA3FF.toInt())
            }
        }
        for (danger in game.world.hazards) {
            val x = danger.x.toFloat()
            val y = danger.y.toFloat()
            painter.box(x + .08f, y + .08f, .84f, .84f, ArcadeColors.red)
            painter.line(x + .24f, y + .24f, x + .76f, y + .76f, ArcadeColors.white, .12f)
        }
        for (crate in game.world.obstacles) {
            val x = crate.x.toFloat()
            val y = crate.y.toFloat()
            painter.box(x + .04f, y + .04f, .92f, .92f, 0xFF493B47.toInt())
            painter.box(x + .13f, y + .13f, .74f, .74f, 0xFFFF9664.toInt())
            painter.line(x + .22f, y + .22f, x + .78f, y + .78f, 0xFF503342.toInt(), .1f)
            painter.line(x + .78f, y + .22f, x + .22f, y + .78f, 0xFF503342.toInt(), .1f)
        }

        for (rival in game.world.rivals) {
            for (cell in rival.trail) {
                painter.box(cell.x + .11f, cell.y + .11f, .78f, .78f, rival.color)
            }
            painter.disc(rival.cell.x + .3f, rival.cell.y + .3f, .09f, ArcadeColors.white)
            painter.disc(rival.cell.x + .7f, rival.cell.y + .3f, .09f, ArcadeColors.white)
        }

        game.world.boss?.let { boss ->
            val x = boss.cell.x.toFloat()
            val y = boss.cell.y.toFloat()
            painter.disc(x + .5f, y + .5f, 1.6f, 0x99483678.toInt())
            painter.box(x - .8f, y - .6f, 2.6f, 2.15f, 0xFF9470D6.toInt())
            painter.box(x - .35f, y - .12f, 1.75f, .65f, ArcadeColors.backdrop)
            painter.disc(x + .1f, y + .2f, .19f, ArcadeColors.red)
            painter.disc(x + 1.04f, y + .2f, .19f, ArcadeColors.red)
            for (i in 0 until boss.health.coerceAtMost(4)) {
                painter.box(x - .45f + i * .47f, y - 1.1f, .35f, .2f, ArcadeColors.orange)
            }
        }

        val pulse = .06f * sin((game.steps + t) * .9f)
        painter.disc(game.food.x + .5f, game.food.y + .5f, .59f + pulse, 0x447FE9B4)
        painter.disc(game.food.x + .5f, game.food.y + .5f, .37f + pulse, ArcadeColors.orange)
        painter.disc(game.food.x + .38f, game.food.y + .38f, .12f, ArcadeColors.white)
        game.powerCell?.let { power ->
            val color = when (game.powerToCollect) {
                SnakePower.SHIELD -> ArcadeColors.blue
                SnakePower.MAGNET -> ArcadeColors.purple
                SnakePower.SLOW_MOTION -> ArcadeColors.mint
                SnakePower.DOUBLE_SCORE -> ArcadeColors.orange
            }
            painter.disc(power.x + .5f, power.y + .5f, .5f + pulse, color)
            painter.disc(power.x + .5f, power.y + .5f, .2f, ArcadeColors.white)
        }

        val oldTail = game.previousBody.lastOrNull() ?: game.body.last()
        for (index in game.body.indices.reversed()) {
            val cell = game.body.elementAt(index)
            val previous = game.previousBody.getOrNull(index) ?: oldTail

            // Crossing an arena edge is a one-cell movement, not a 19- or
            // 25-cell dash across the screen. Render both clipped halves of
            // the segment so it smoothly exits one edge and enters the other.
            val wrapRight = previous.x == 19 && cell.x == 0 && previous.y == cell.y
            val wrapLeft = previous.x == 0 && cell.x == 19 && previous.y == cell.y
            val wrapDown = previous.y == 25 && cell.y == 0 && previous.x == cell.x
            val wrapUp = previous.y == 0 && cell.y == 25 && previous.x == cell.x
            val discontinuity = (kotlin.math.abs(cell.x - previous.x) > 1 &&
                !wrapRight && !wrapLeft) ||
                (kotlin.math.abs(cell.y - previous.y) > 1 && !wrapUp && !wrapDown)

            // Separate, distant teleport portals intentionally snap instead
            // of interpolating a long, confusing line across the arena.
            val x = when {
                discontinuity -> if (t < .5f) previous.x.toFloat() else cell.x.toFloat()
                wrapRight -> previous.x.toFloat() + t
                wrapLeft -> previous.x.toFloat() - t
                else -> previous.x + (cell.x - previous.x) * t
            }
            val y = when {
                discontinuity -> if (t < .5f) previous.y.toFloat() else cell.y.toFloat()
                wrapDown -> previous.y.toFloat() + t
                wrapUp -> previous.y.toFloat() - t
                else -> previous.y + (cell.y - previous.y) * t
            }
            val isHead = index == 0
            val duplicates = if (wrapRight || wrapLeft || wrapDown || wrapUp) 2 else 1
            for (copy in 0 until duplicates) {
                val px = x + if (copy == 1) when {
                    wrapRight -> -20f
                    wrapLeft -> 20f
                    else -> 0f
                } else 0f
                val py = y + if (copy == 1) when {
                    wrapDown -> -26f
                    wrapUp -> 26f
                    else -> 0f
                } else 0f
                painter.box(px + .025f, py + .095f, .95f, .88f, 0xFF173E3B.toInt())
                painter.box(px + .085f, py + .085f, .83f, .83f,
                    if (isHead && game.finished) ArcadeColors.red
                    else if (isHead) SnakePalette.head(game.rules.skin)
                    else SnakePalette.body(game.rules.skin))
                if (isHead) {
                    if (game.activePower == SnakePower.SHIELD) {
                        painter.disc(px + .5f, py + .5f, .66f, 0x6681E6BD)
                    }
                    val eyeX = when (game.direction) {
                        PadKey.LEFT -> -.16f
                        PadKey.RIGHT -> .16f
                        else -> 0f
                    }
                    val eyeY = when (game.direction) {
                        PadKey.UP -> -.16f
                        PadKey.DOWN -> .16f
                        else -> 0f
                    }
                    painter.disc(px + .34f + eyeX, py + .36f + eyeY, .095f, ArcadeColors.backdrop)
                    painter.disc(px + .67f + eyeX, py + .36f + eyeY, .095f, ArcadeColors.backdrop)
                } else if (game.combo >= 3 && index % 3 == 0) {
                    painter.disc(px + .5f, py + .5f, .12f, ArcadeColors.orange)
                }
            }
        }

        if (game.sparkTicks > 0) {
            val brightness = game.sparkTicks / 4f
            for (i in 0 until 6) {
                val angle = i * 1.0472f
                val radius = (1.7f - brightness) + t * .7f
                painter.disc(
                    game.snack.x + .5f + cos(angle) * radius,
                    game.snack.y + .5f + sin(angle) * radius,
                    .12f * brightness, ArcadeColors.orange)
            }
        }

        val shot = game.lastBlast
        if (shot != null && game.blastTicks > 0) {
            val fade = (game.blastTicks - t).coerceIn(0f, 5f) / 5f
            val cx = shot.impact.x + .5f
            val cy = shot.impact.y + .5f
            painter.line(
                shot.origin.x + .5f, shot.origin.y + .5f, cx, cy,
                0x99FFCB73.toInt(), .06f + .14f * fade,
            )
            painter.disc(cx, cy, 1.9f * (1.15f - fade), 0x44FFB066)
            painter.disc(cx, cy, 1.0f * (1.15f - fade), 0xAAFFCD77.toInt())
            painter.disc(cx, cy, .29f, ArcadeColors.white)
            for (i in 0 until 8) {
                val a = i * .7854f
                val spread = (1f - fade) * 2.8f + .5f
                painter.disc(cx + cos(a) * spread, cy + sin(a) * spread,
                    .08f + .12f * fade, ArcadeColors.orange)
            }
        }
    }
}
