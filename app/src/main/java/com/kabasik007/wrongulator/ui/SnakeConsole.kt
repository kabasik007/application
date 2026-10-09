package com.kabasik007.wrongulator.ui

import android.view.KeyEvent as AndroidKeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kabasik007.wrongulator.R
import com.kabasik007.wrongulator.arcade.PadKey
import com.kabasik007.wrongulator.arcade.SnakeEvent
import com.kabasik007.wrongulator.arcade.SnakeGame
import kotlinx.coroutines.isActive
import androidx.compose.runtime.withFrameNanos
import kotlin.math.abs

private val snakeInk = Color(0xFF080F1D)
private val snakePanel = Color(0xFF17263A)
private val snakeMint = Color(0xFF81E6BD)
private val snakeWhite = Color(0xFFF4FAFF)
private val snakeMuted = Color(0xFF9EBDCB)
private val snakeBorder = Color(0xFF31475B)

/** Dedicated snake console: the other games keep their current controller and loop. */
@Composable
internal fun SnakeConsole(onExit: () -> Unit, modifier: Modifier = Modifier) {
    val game = remember { SnakeGame() }
    val context = LocalContext.current
    val scores = remember(context) { context.getSharedPreferences("arcade_scores", 0) }
    val options = remember(context) { context.getSharedPreferences("arcade_options", 0) }
    val soundFx = remember { SnakeSounds() }
    val haptics = LocalHapticFeedback.current
    val lifecycle = LocalLifecycleOwner.current
    val focusRequester = remember { FocusRequester() }

    var soundEnabled by rememberSaveable { mutableStateOf(options.getBoolean("snake_sound", true)) }
    var best by remember { mutableIntStateOf(scores.getInt("snake", 0)) }
    var started by remember { mutableStateOf(false) }
    var paused by remember { mutableStateOf(false) }
    var foreground by remember { mutableStateOf(lifecycle.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    var generation by remember { mutableIntStateOf(0) }
    var revision by remember { mutableIntStateOf(0) }
    var interpolation by remember { mutableFloatStateOf(1f) }
    val scoreScale = remember { Animatable(1f) }
    val scoreNow = revision.let { game.score }
    val over = revision.let { game.finished }
    val running = started && !paused && foreground && !over

    BackHandler { onExit() }
    DisposableEffect(soundFx) { onDispose { soundFx.close() } }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> foreground = true
                Lifecycle.Event.ON_STOP -> {
                    foreground = false
                    if (started) paused = true // never resume by surprise after phone backgrounding
                }
                else -> Unit
            }
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    fun play(cue: SnakeSounds.Cue) = soundFx.play(cue, soundEnabled)

    fun mainAction() {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        when {
            over -> {
                game.reset()
                generation++
                revision++
                interpolation = 1f
                paused = false
                started = true
                play(SnakeSounds.Cue.START)
            }
            !started -> {
                started = true
                paused = false
                generation++
                play(SnakeSounds.Cue.START)
            }
            else -> {
                paused = !paused
                interpolation = 1f
                play(SnakeSounds.Cue.PAUSE)
            }
        }
    }

    fun move(key: PadKey) {
        if (!running) return
        game.input(key)
        revision++
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun fireGrenade() {
        if (!running) return
        if (game.launchGrenade()) {
            revision++
            play(SnakeSounds.Cue.BLAST)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            if (game.score > best) {
                best = game.score
                scores.edit().putInt("snake", best).apply()
            }
        } else {
            play(SnakeSounds.Cue.EMPTY)
        }
    }

    // Vsync pacing for visuals; fixed-step logical movement. No per-frame allocations in SnakeGame.
    // The loop is automatically cancelled when the screen leaves composition.
    LaunchedEffect(running, generation) {
        if (!running) return@LaunchedEffect
        var previousFrame = 0L
        var elapsed = 0f
        while (isActive && foreground && !paused && !game.finished) {
            val timestamp = withFrameNanos { it }
            if (previousFrame == 0L) {
                previousFrame = timestamp
                continue
            }
            elapsed += ((timestamp - previousFrame) / 1_000_000f).coerceIn(0f, 85f)
            previousFrame = timestamp
            val step = game.intervalMs.toFloat()
            if (elapsed >= step) {
                elapsed -= step
                game.tick()
                revision++
                when (game.lastEvent) {
                    SnakeEvent.EAT -> {
                        play(SnakeSounds.Cue.EAT)
                        if (game.score > best) {
                            best = game.score
                            scores.edit().putInt("snake", best).apply()
                        }
                    }
                    SnakeEvent.CRASH -> {
                        play(SnakeSounds.Cue.LOSE)
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    SnakeEvent.NONE -> Unit
                }
            }
            interpolation = if (game.finished) 1f
                else (elapsed / game.intervalMs).coerceIn(0f, 1f)
        }
    }

    LaunchedEffect(scoreNow) {
        if (scoreNow > 0 && started) {
            scoreScale.snapTo(1.16f)
            scoreScale.animateTo(1f, tween(270))
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF0A1524), snakeInk, Color(0xFF080C16))),
            )
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.nativeKeyEvent.keyCode) {
                    AndroidKeyEvent.KEYCODE_DPAD_LEFT -> move(PadKey.LEFT)
                    AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> move(PadKey.RIGHT)
                    AndroidKeyEvent.KEYCODE_DPAD_UP -> move(PadKey.UP)
                    AndroidKeyEvent.KEYCODE_DPAD_DOWN -> move(PadKey.DOWN)
                    AndroidKeyEvent.KEYCODE_BUTTON_B, AndroidKeyEvent.KEYCODE_BUTTON_R1 -> fireGrenade()
                    AndroidKeyEvent.KEYCODE_BUTTON_A, AndroidKeyEvent.KEYCODE_ENTER,
                    AndroidKeyEvent.KEYCODE_SPACE, AndroidKeyEvent.KEYCODE_DPAD_CENTER -> mainAction()
                    else -> return@onPreviewKeyEvent false
                }
                true
            }
            .focusable()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(42.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onExit) {
                Text("‹  " + stringResource(R.string.arcade_library), color = snakeMuted)
            }
            Text("SNAKE", fontWeight = FontWeight.Black, fontSize = 21.sp,
                letterSpacing = 2.sp, color = snakeWhite)
            TextButton(
                onClick = {
                    soundEnabled = !soundEnabled
                    options.edit().putBoolean("snake_sound", soundEnabled).apply()
                    if (soundEnabled) play(SnakeSounds.Cue.START)
                },
            ) {
                Text(
                    text = if (soundEnabled) "♪ ON" else "♪ OFF",
                    color = if (soundEnabled) snakeMint else snakeMuted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            SnakeStat(
                modifier = Modifier.weight(1f).graphicsLayer {
                    scaleX = scoreScale.value
                    scaleY = scoreScale.value
                },
                label = stringResource(R.string.arcade_score),
                value = scoreNow.toString(),
                emphasis = true,
            )
            SnakeStat(Modifier.weight(1f), stringResource(R.string.arcade_best), best.toString())
            SnakeStat(
                Modifier.weight(1f),
                stringResource(R.string.snake_speed),
                (1 + scoreNow / 40).coerceAtMost(12).toString().padStart(2, '0'),
            )
        }
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f)
                .clip(RoundedCornerShape(23.dp))
                .background(Color(0xFF0B1626))
                .border(1.dp, snakeBorder, RoundedCornerShape(23.dp))
                .pointerInput(game) {
                    var drag = Offset.Zero
                    val threshold = 25.dp.toPx()
                    detectDragGestures(
                        onDragStart = { drag = Offset.Zero },
                        onDragCancel = { drag = Offset.Zero },
                        onDragEnd = { drag = Offset.Zero },
                    ) { change, delta ->
                        change.consume()
                        drag += delta
                        if (abs(drag.x) > threshold || abs(drag.y) > threshold) {
                            val direction = if (abs(drag.x) > abs(drag.y)) {
                                if (drag.x > 0) PadKey.RIGHT else PadKey.LEFT
                            } else {
                                if (drag.y > 0) PadKey.DOWN else PadKey.UP
                            }
                            move(direction)
                            drag = Offset.Zero
                        }
                    }
                }
                .padding(9.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                // Compose observes these frame values and schedules the next Canvas render.
                val frame = revision
                val progress = interpolation
                if (frame >= 0) drawArcadeFrame(game, progress)
            }
            if (!running) {
                val firstScreen = !started
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xF0152538),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF416B73)),
                    modifier = Modifier.padding(16.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 25.dp, vertical = 23.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            when {
                                over -> stringResource(R.string.arcade_game_over)
                                firstScreen -> stringResource(R.string.snake_ready)
                                else -> stringResource(R.string.arcade_paused)
                            },
                            color = snakeWhite, fontSize = 26.sp, fontWeight = FontWeight.Black,
                        )
                        if (over) {
                            Text("${stringResource(R.string.arcade_score)}  $scoreNow",
                                color = snakeMint, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Text(
                                stringResource(R.string.snake_intro),
                                color = snakeMuted, textAlign = TextAlign.Center,
                                fontSize = 13.sp, lineHeight = 18.sp,
                            )
                        }
                        Button(
                            onClick = { mainAction() },
                            shape = RoundedCornerShape(15.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = snakeMint, contentColor = snakeInk,
                            ),
                        ) {
                            Text(
                                stringResource(
                                    when {
                                        over -> R.string.arcade_restart
                                        firstScreen -> R.string.snake_start
                                        else -> R.string.arcade_resume
                                    },
                                ),
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.snake_hint),
                color = snakeMuted, fontSize = 12.sp,
            )
            Text(if (running) "● LIVE" else "● IDLE", color = if (running) snakeMint else snakeMuted,
                fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
        BoxWithConstraints(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            val keySize = (maxWidth.value * .175f).coerceIn(46f, 63f).dp
            val actionSize = (maxWidth.value * .23f).coerceIn(66f, 87f).dp
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Spacer(Modifier.size(keySize))
                        SnakeDirection("▲", keySize) { move(PadKey.UP) }
                        Spacer(Modifier.size(keySize))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        SnakeDirection("◀", keySize) { move(PadKey.LEFT) }
                        Box(
                            Modifier.size(keySize).background(
                                Color(0xFF203649), RoundedCornerShape(13.dp),
                            ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("✚", color = snakeMuted, fontSize = 22.sp)
                        }
                        SnakeDirection("▶", keySize) { move(PadKey.RIGHT) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Spacer(Modifier.size(keySize))
                        SnakeDirection("▼", keySize) { move(PadKey.DOWN) }
                        Spacer(Modifier.size(keySize))
                    }
                }
                Column(
                    modifier = Modifier.padding(end = 2.dp, bottom = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Button(
                        onClick = { fireGrenade() },
                        enabled = running && game.ammo > 0,
                        modifier = Modifier
                            .width((actionSize.value + 20f).dp)
                            .height((keySize.value * .82f).dp),
                        shape = RoundedCornerShape(17.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFAE72),
                            contentColor = snakeInk,
                            disabledContainerColor = snakePanel,
                            disabledContentColor = snakeMuted,
                        ),
                    ) {
                        Text("💥 ${game.ammo}", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Text(
                        stringResource(R.string.snake_grenade),
                        color = if (game.ammo > 0) Color(0xFFFFB781) else snakeMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                    )
                    Text(
                        stringResource(
                            when {
                                over -> R.string.arcade_restart
                                !started -> R.string.snake_start
                                paused -> R.string.arcade_resume
                                else -> R.string.arcade_pause
                            },
                        ),
                        color = snakeMint, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = { mainAction() },
                        modifier = Modifier.size(actionSize),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = snakeMint, contentColor = snakeInk,
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    ) {
                        Text(
                            when {
                                over -> "↻"
                                !started || paused -> "▶"
                                else -> "Ⅱ"
                            },
                            fontSize = 31.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SnakeStat(
    modifier: Modifier,
    label: String,
    value: String,
    emphasis: Boolean = false,
) {
    Surface(
        modifier = modifier,
        color = snakePanel,
        shape = RoundedCornerShape(15.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, snakeBorder),
    ) {
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(label.uppercase(), color = snakeMuted, fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold)
            Text(value, color = if (emphasis) snakeMint else snakeWhite,
                fontSize = 23.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun SnakeDirection(label: String, size: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(size),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        shape = RoundedCornerShape(15.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = snakePanel,
            contentColor = snakeWhite,
        ),
    ) {
        Text(label, fontSize = 23.sp, fontWeight = FontWeight.Black)
    }
}
