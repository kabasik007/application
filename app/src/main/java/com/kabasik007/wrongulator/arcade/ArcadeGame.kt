package com.kabasik007.wrongulator.arcade

/**
 * Plug-in contract for bundled mini-games.
 *
 * Each game lives in one implementation file. The orchestrator creates only the
 * selected game, advances one fixed step at a time and supplies a painter.
 * No Activity, ViewModel or Compose dependencies in game logic.
 */
enum class PadKey { LEFT, RIGHT, UP, DOWN, ACTION }

interface ArcadePainter {
    fun box(x: Float, y: Float, width: Float, height: Float, argb: Int)
    fun disc(x: Float, y: Float, radius: Float, argb: Int)
    fun line(x1: Float, y1: Float, x2: Float, y2: Float, argb: Int, stroke: Float = 1f)
}

interface ArcadeGame {
    val width: Float
    val height: Float
    val intervalMs: Long
    val score: Int
    val finished: Boolean
    fun reset()
    fun input(key: PadKey)
    fun tick()
    fun paint(painter: ArcadePainter)
}

object ArcadeColors {
    val backdrop = 0xFF0C1325.toInt()
    val grid = 0xFF1A2841.toInt()
    val mint = 0xFF81E6BD.toInt()
    val blue = 0xFF95B9FF.toInt()
    val orange = 0xFFFFC779.toInt()
    val purple = 0xFFC5B1FF.toInt()
    val red = 0xFFFF6D82.toInt()
    val white = 0xFFF6F8FF.toInt()
}

data class GridCell(val x: Int, val y: Int)
