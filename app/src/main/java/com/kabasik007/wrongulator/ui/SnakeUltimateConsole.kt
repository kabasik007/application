package com.kabasik007.wrongulator.ui

import android.content.Intent
import android.view.KeyEvent as AndroidKeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kabasik007.wrongulator.R
import com.kabasik007.wrongulator.arcade.PadKey
import com.kabasik007.wrongulator.arcade.SnakeAchievement
import com.kabasik007.wrongulator.arcade.SnakeEvent
import com.kabasik007.wrongulator.arcade.SnakeGame
import com.kabasik007.wrongulator.arcade.SnakeMode
import com.kabasik007.wrongulator.arcade.SnakeReplay
import com.kabasik007.wrongulator.arcade.SnakeReplayCodec
import com.kabasik007.wrongulator.arcade.SnakeWeapon
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.random.Random

private val shellInk = Color(0xFF07101E)
private val shellMint = Color(0xFF81E6BD)
private val shellMuted = Color(0xFFA3C0CD)
private val shellWhite = Color(0xFFF7FAFF)

private fun freshGame(settings: SnakePlayerSettings): SnakeGame {
    val seed = Random.Default.nextInt()
    return SnakeGame(Random(seed), settings.rules, seed)
}

/**
 * Lifecycle-aware game orchestrator. Game state lives only in the pure Kotlin
 * engine; the Compose UI owns visibility, user preferences and frame scheduling.
 */
@Composable
internal fun SnakeUltimateConsole(onExit: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val options = remember(context) { context.getSharedPreferences("arcade_options", 0) }
    val scores = remember(context) { context.getSharedPreferences("arcade_scores", 0) }
    var settings by remember { mutableStateOf(SnakePreferences.load(options)) }
    var game by remember { mutableStateOf(freshGame(settings)) }
    var best by remember { mutableIntStateOf(scores.getInt(SnakePreferences.scoreKey(settings.rules), 0)) }
    var unlocked by remember { mutableStateOf(SnakePreferences.unlocked(options)) }
    var showSettings by remember { mutableStateOf(false) }
    var showProgress by remember { mutableStateOf(false) }
    var started by remember { mutableStateOf(false) }
    var paused by remember { mutableStateOf(false) }
    var foreground by remember { mutableStateOf(true) }
    var countdown by remember { mutableIntStateOf(0) }
    var revision by remember { mutableIntStateOf(0) }
    var interpolation by remember { mutableFloatStateOf(1f) }
    var generation by remember { mutableIntStateOf(0) }
    var replay by remember { mutableStateOf<SnakeReplay?>(null) }
    var replayCursor by remember { mutableIntStateOf(0) }
    val sounds = remember { SnakeSounds() }
    val lifecycle = LocalLifecycleOwner.current
    val focus = remember { FocusRequester() }
    val haptics = LocalHapticFeedback.current
    val scoreNow = revision.let { game.score }
    val gameOver = revision.let { game.finished }
    val live = started && countdown == 0 && !paused && foreground &&
        !gameOver && !showSettings && !showProgress

    BackHandler { onExit() }
    DisposableEffect(sounds) { onDispose { sounds.close() } }
    DisposableEffect(lifecycle) {
        foreground = lifecycle.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        val listener = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> foreground = true
                Lifecycle.Event.ON_STOP -> {
                    foreground = false
                    if (started) paused = true
                }
                else -> Unit
            }
        }
        lifecycle.lifecycle.addObserver(listener)
        onDispose { lifecycle.lifecycle.removeObserver(listener) }
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
    LaunchedEffect(countdown, paused, foreground) {
        if (countdown > 0 && !paused && foreground) {
            delay(1_000)
            countdown--
        }
    }

    fun feedback() {
        if (settings.haptics) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    fun effect(cue: SnakeSounds.Cue) = sounds.play(cue, settings.sound)

    fun startOver() {
        replay = null
        replayCursor = 0
        game = freshGame(settings)
        started = true
        paused = false
        countdown = 5
        interpolation = 1f
        generation++
        revision++
        effect(SnakeSounds.Cue.START)
    }

    fun mainAction() {
        feedback()
        when {
            gameOver -> startOver()
            !started -> {
                started = true
                countdown = 5
                paused = false
                generation++
                effect(SnakeSounds.Cue.START)
            }
            countdown > 0 -> Unit
            else -> {
                paused = !paused
                effect(SnakeSounds.Cue.PAUSE)
            }
        }
    }

    fun steer(direction: PadKey) {
        if (!live || replay != null) return
        game.input(direction)
        revision++
        feedback()
    }

    fun fire() {
        if (!live || replay != null) return
        if (game.launchGrenade()) {
            revision++
            effect(SnakeSounds.Cue.BLAST)
            if (settings.haptics) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            if (game.score > best) {
                best = game.score
                scores.edit().putInt(SnakePreferences.scoreKey(settings.rules), best).apply()
            }
        } else {
            effect(SnakeSounds.Cue.EMPTY)
        }
    }

    fun nextWeapon() {
        val types = SnakeWeapon.entries
        game.chooseWeapon(types[(types.indexOf(game.selectedWeapon) + 1) % types.size])
        revision++
    }

    fun playReplay() {
        val token = game.exportReplay() ?: return
        val trace = SnakeReplayCodec.decode(token) ?: return
        replay = trace
        replayCursor = 0
        game = SnakeGame.fromReplay(trace)
        started = true
        paused = false
        countdown = 3
        interpolation = 1f
        generation++
        revision++
    }

    fun shareRun() {
        val code = game.exportReplay()
        // Share only an explicit user-selected score card. Trace is optional,
        // bounded and contains game inputs only, never account/private data.
        val text = buildString {
            append("Wrongulator Snake — ${game.score} pts · ${settings.rules.mode}\n")
            append("Food ${game.foodCollected}, crates ${game.destroyedTotal}, combo x${game.maxCombo}")
            if (code != null && code.length <= 8_000) append("\nReplay code: $code")
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, null))
    }

    // The game runs while visible, never in the app background or in a paused sheet.
    LaunchedEffect(game, live, generation, settings.fps) {
        if (!live) return@LaunchedEffect
        var previous = 0L
        var elapsed = 0f
        var lastVisualFrame = 0L
        while (isActive && foreground && !paused && !game.finished) {
            val frame = withFrameNanos { it }
            if (previous == 0L) {
                previous = frame
                continue
            }
            elapsed += ((frame - previous) / 1_000_000f).coerceIn(0f, 90f)
            previous = frame
            val duration = game.intervalMs.toFloat()
            if (elapsed >= duration) {
                elapsed -= duration
                val trace = replay
                if (trace != null) {
                    while (replayCursor < trace.actions.size &&
                        trace.actions[replayCursor].tick <= game.steps
                    ) {
                        val action = trace.actions[replayCursor++]
                        action.direction?.let(game::input)
                        action.fire?.let {
                            game.chooseWeapon(it)
                            game.launchGrenade()
                        }
                    }
                }
                game.tick()
                revision++
                if (trace == null) {
                    if (game.score > best) {
                        best = game.score
                        scores.edit().putInt(SnakePreferences.scoreKey(settings.rules), best).apply()
                    }
                    val earned = game.earnedAchievements
                    if (!unlocked.containsAll(earned)) {
                        SnakePreferences.persistAchievements(options, earned)
                        unlocked = unlocked + earned
                    }
                }
                when (game.lastEvent) {
                    SnakeEvent.EAT -> effect(SnakeSounds.Cue.EAT)
                    SnakeEvent.CRASH, SnakeEvent.TIME_UP -> {
                        effect(SnakeSounds.Cue.LOSE)
                        if (settings.haptics) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }
                    SnakeEvent.POWERUP, SnakeEvent.BOSS_DEFEATED -> effect(SnakeSounds.Cue.START)
                    else -> Unit
                }
            }
            val visualIntervalNs = if (settings.fps == 30) 33_333_333L else 16_666_667L
            if (frame - lastVisualFrame >= visualIntervalNs || game.finished) {
                interpolation = if (game.finished) 1f
                    else (elapsed / game.intervalMs).coerceIn(0f, 1f)
                lastVisualFrame = frame
            }
        }
    }

    val actionRes = when {
        gameOver -> R.string.arcade_restart
        !started -> R.string.snake_start
        paused -> R.string.arcade_resume
        countdown > 0 -> R.string.snake_start
        else -> R.string.arcade_pause
    }
    val actionSymbol = when {
        gameOver -> "↻"
        !started || paused -> "▶"
        countdown > 0 -> "5"
        else -> "Ⅱ"
    }

    Column(
        modifier = modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
            .background(Brush.verticalGradient(listOf(Color(0xFF0D1A2B), shellInk)))
            .focusRequester(focus)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.nativeKeyEvent.keyCode) {
                    AndroidKeyEvent.KEYCODE_DPAD_LEFT -> steer(PadKey.LEFT)
                    AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> steer(PadKey.RIGHT)
                    AndroidKeyEvent.KEYCODE_DPAD_UP -> steer(PadKey.UP)
                    AndroidKeyEvent.KEYCODE_DPAD_DOWN -> steer(PadKey.DOWN)
                    AndroidKeyEvent.KEYCODE_BUTTON_B, AndroidKeyEvent.KEYCODE_BUTTON_R1 -> fire()
                    AndroidKeyEvent.KEYCODE_BUTTON_X -> nextWeapon()
                    AndroidKeyEvent.KEYCODE_BUTTON_A, AndroidKeyEvent.KEYCODE_ENTER,
                    AndroidKeyEvent.KEYCODE_SPACE,
                    AndroidKeyEvent.KEYCODE_DPAD_CENTER -> mainAction()
                    else -> return@onPreviewKeyEvent false
                }
                true
            }.focusable()
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onExit) {
                Text("‹ ${stringResource(R.string.arcade_library)}", color = shellMuted)
            }
            Text("SNAKE ULTIMATE", color = shellWhite,
                fontWeight = FontWeight.Black, fontSize = 17.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                TextButton(onClick = {
                    paused = true
                    showProgress = true
                }) { Text("🏆", fontSize = 19.sp) }
                TextButton(onClick = {
                    paused = true
                    showSettings = true
                }) { Text("⚙", color = shellMint, fontSize = 20.sp) }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            SnakeHudStat(Modifier.weight(1f), stringResource(R.string.arcade_score),
                scoreNow.toString(), primary = true)
            SnakeHudStat(Modifier.weight(1f), stringResource(R.string.arcade_best), best.toString())
            SnakeHudStat(Modifier.weight(1f), "COMBO", "x${game.combo}")
            SnakeHudStat(Modifier.weight(1f), stringResource(R.string.snake_speed),
                (1 + scoreNow / 40).coerceAtMost(12).toString())
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("${settings.rules.mode.name} · ${settings.rules.difficulty.name}",
                color = shellMuted, fontSize = 11.sp)
            Text(
                when {
                    game.bossHealth > 0 -> "BOSS ♥ ${game.bossHealth}"
                    game.activePower != null -> "✦ ${game.activePower?.name} ${game.powerTicks}"
                    settings.rules.mode == SnakeMode.TIME_ATTACK ->
                        "${game.timeRemainingMs / 1000}s"
                    replay != null -> "● REPLAY"
                    else -> "${game.destroyedTotal} 💥 · ${game.rivalCount} rivals"
                },
                color = shellMint, fontSize = 11.sp,
            )
        }
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val landscape = maxWidth > maxHeight
            if (landscape) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SnakePlayfield(
                        game, Modifier.weight(1f).fillMaxSize(), revision, interpolation,
                        live, paused, started, countdown, ::mainAction, ::steer,
                    )
                    Column(Modifier.width(240.dp), verticalArrangement = Arrangement.SpaceBetween) {
                        SnakeStatusLine(game, replay != null)
                        SnakeControlPanel(
                            settings.leftHanded, live && replay == null,
                            settings.rules.canShoot, game.ammo, game.selectedWeapon,
                            actionRes, actionSymbol, ::steer, ::fire, ::mainAction, ::nextWeapon,
                        )
                        if (gameOver && replay == null) {
                            TextButton(onClick = ::playReplay) {
                                Text(stringResource(R.string.snake_replay), color = shellMint)
                            }
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SnakePlayfield(
                        game, Modifier.weight(1f).fillMaxWidth(), revision, interpolation,
                        live, paused, started, countdown, ::mainAction, ::steer,
                    )
                    SnakeStatusLine(game, replay != null)
                    SnakeControlPanel(
                        settings.leftHanded, live && replay == null,
                        settings.rules.canShoot, game.ammo, game.selectedWeapon,
                        actionRes, actionSymbol, ::steer, ::fire, ::mainAction, ::nextWeapon,
                    )
                    if (gameOver && replay == null) {
                        TextButton(onClick = ::playReplay) {
                            Text(stringResource(R.string.snake_replay), color = shellMint)
                        }
                    }
                }
            }
        }
    }

    if (showSettings) SnakeSettingsSheet(
        settings,
        onApply = { next ->
            settings = next
            SnakePreferences.save(options, next)
            showSettings = false
            best = scores.getInt(SnakePreferences.scoreKey(next.rules), 0)
            startOver()
            started = false
            countdown = 0
        },
        onDismiss = { showSettings = false },
    )

    if (showProgress) SnakeProgressSheet(
        game, best, unlocked, ::shareRun,
        onDismiss = { showProgress = false },
    )
}

@Composable
private fun SnakeHudStat(
    modifier: Modifier,
    title: String,
    value: String,
    primary: Boolean = false,
) {
    Surface(modifier = modifier,
        color = Color(0xFF16253A), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 7.dp)) {
            Text(title.uppercase(), color = shellMuted, fontSize = 9.sp, maxLines = 1)
            Text(value, color = if (primary) shellMint else shellWhite,
                fontWeight = FontWeight.Black, fontSize = 21.sp, maxLines = 1)
        }
    }
}

@Composable
private fun SnakeStatusLine(game: SnakeGame, replay: Boolean) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            when {
                replay -> stringResource(R.string.snake_replay_active)
                game.finished -> stringResource(R.string.arcade_game_over)
                else -> stringResource(R.string.snake_combat_hint)
            },
            color = shellMuted, fontSize = 11.sp, maxLines = 1,
        )
        Text("●", color = if (game.finished) Color(0xFFFF8293) else shellMint,
            fontSize = 13.sp)
    }
}
