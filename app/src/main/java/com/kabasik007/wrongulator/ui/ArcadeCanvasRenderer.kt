package com.kabasik007.wrongulator.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.kabasik007.wrongulator.arcade.ArcadeGame
import com.kabasik007.wrongulator.arcade.ArcadePainter
import com.kabasik007.wrongulator.arcade.SnakeGame
import kotlin.math.min

/** Shared viewport fitter and primitive renderer for every built-in arcade game. */
internal fun DrawScope.drawArcadeFrame(game: ArcadeGame, progress: Float = 1f) {
    val scale = min(size.width / game.width, size.height / game.height)
    if (scale <= 0f) return
    val offsetX = (size.width - game.width * scale) * .5f
    val offsetY = (size.height - game.height * scale) * .5f
    val painter = object : ArcadePainter {
        override fun box(x: Float, y: Float, width: Float, height: Float, argb: Int) {
            drawRect(
                color = Color(argb),
                topLeft = Offset(offsetX + x * scale, offsetY + y * scale),
                size = Size(width.coerceAtLeast(0f) * scale, height.coerceAtLeast(0f) * scale),
            )
        }

        override fun disc(x: Float, y: Float, radius: Float, argb: Int) {
            drawCircle(Color(argb), radius.coerceAtLeast(0f) * scale,
                Offset(offsetX + x * scale, offsetY + y * scale))
        }

        override fun line(
            x1: Float, y1: Float, x2: Float, y2: Float, argb: Int, stroke: Float,
        ) {
            drawLine(
                Color(argb),
                Offset(offsetX + x1 * scale, offsetY + y1 * scale),
                Offset(offsetX + x2 * scale, offsetY + y2 * scale),
                strokeWidth = (stroke * scale).coerceAtLeast(1f),
            )
        }
    }
    if (game is SnakeGame) game.paintSmooth(painter, progress) else game.paint(painter)
}
