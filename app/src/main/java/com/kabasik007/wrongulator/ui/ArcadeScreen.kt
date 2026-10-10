package com.kabasik007.wrongulator.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kabasik007.wrongulator.R
import com.kabasik007.wrongulator.arcade.ArcadeGame
import com.kabasik007.wrongulator.arcade.ArcadePainter
import com.kabasik007.wrongulator.arcade.BlockDropGame
import com.kabasik007.wrongulator.arcade.CountryballsGame
import com.kabasik007.wrongulator.arcade.MotoTrailGame
import com.kabasik007.wrongulator.arcade.PadKey
import com.kabasik007.wrongulator.arcade.SnakeGame
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.min

private val consoleBg = Color(0xFF090D17)
private val consoleCard = Color(0xFF1A2335)
private val consoleAccent = Color(0xFF81E6BD)
private val consoleBlue = Color(0xFFA8B9FF)
private val consoleWhite = Color(0xFFF8FAFF)
private val consoleSecondary = Color(0xFFAAB6CB)

/**
 * Only the selected implementation is instantiated. Adding a game means adding
 * one game file and one catalog entry; no classpath scan or untrusted APK loading.
 */
private data class ArcadeEntry(
    val id: String,
    val name: Int,
    val description: Int,
    val hint: Int,
    val icon: String,
    val color: Color,
    val factory: () -> ArcadeGame,
)

private val catalog = listOf(
    ArcadeEntry("snake", R.string.game_snake, R.string.snake_about, R.string.snake_controls,
        "🐍", consoleAccent, { SnakeGame() }),
    ArcadeEntry("blocks", R.string.game_blocks, R.string.blocks_about, R.string.blocks_controls,
        "▦", consoleBlue, { BlockDropGame() }),
    ArcadeEntry("balls", R.string.game_balls, R.string.balls_about, R.string.balls_controls,
        "🌐", Color(0xFFFFC779), { CountryballsGame() }),
    ArcadeEntry("moto", R.string.game_moto, R.string.moto_about, R.string.moto_controls,
        "🏍", Color(0xFFC5B1FF), { MotoTrailGame() }),
)

@Composable
fun ArcadeScreen(modifier: Modifier = Modifier) {
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    // Back while playing returns to the catalog, not the external calculator.
    val selected = catalog.firstOrNull { it.id == selectedId }
    if (selected == null) {
        ArcadeLibrary(modifier, onPlay = { selectedId = it })
    } else if (selected.id == "snake") {
        SnakeUltimateConsole(onExit = { selectedId = null }, modifier = modifier)
    } else {
        ArcadeSession(entry = selected, onExit = { selectedId = null }, modifier = modifier)
    }
}

@Composable
private fun ArcadeLibrary(modifier: Modifier, onPlay: (String) -> Unit) {
    Column(modifier.fillMaxSize().background(consoleBg).padding(horizontal = 18.dp)) {
        Text("ARCADE / 01", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = consoleAccent)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.arcade_heading), fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold, color = consoleWhite)
        Text(stringResource(R.string.arcade_subtitle), color = consoleSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(20.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(catalog, key = { it.id }) { entry ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onPlay(entry.id) },
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = consoleCard),
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        Text(entry.icon, fontSize = 44.sp)
                        Text(stringResource(entry.name), color = consoleWhite,
                            fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(stringResource(entry.description), color = consoleSecondary,
                            fontSize = 12.sp, lineHeight = 17.sp, minLines = 2)
                        Text("▶  " + stringResource(R.string.arcade_play),
                            color = entry.color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ArcadeSession(entry: ArcadeEntry, onExit: () -> Unit, modifier: Modifier) {
    val game = remember(entry.id) { entry.factory() }
    val context = LocalContext.current
    val scores = remember { context.getSharedPreferences("arcade_scores", 0) }
    var best by remember(entry.id) { mutableIntStateOf(scores.getInt(entry.id, 0)) }
    var revision by remember(entry.id) { mutableIntStateOf(0) }
    var restartGeneration by remember(entry.id) { mutableIntStateOf(0) }
    val scoreNow = remember(revision) { game.score }
    val finishedNow = remember(revision) { game.finished }
    BackHandler { onExit() }
    var paused by rememberSaveable(entry.id) { mutableStateOf(false) }
    var active by remember { mutableStateOf(true) }
    val owner = LocalLifecycleOwner.current

    DisposableEffect(owner) {
        active = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) active = true
            if (event == Lifecycle.Event.ON_STOP) active = false
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    // Exactly one bounded game loop. Leaving the page cancels the coroutine.
    LaunchedEffect(game, paused, active, restartGeneration) {
        while (isActive && active && !paused && !game.finished) {
            delay(game.intervalMs)
            game.tick()
            revision++
            if (game.finished && game.score > best) {
                best = game.score
                scores.edit().putInt(entry.id, best).apply()
            }
        }
    }

    fun dispatch(key: PadKey) {
        if (!paused && active && !game.finished) {
            game.input(key)
            revision++
        }
    }

    Column(modifier.fillMaxSize().background(consoleBg).padding(horizontal = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onExit) { Text("‹  " + stringResource(R.string.arcade_library)) }
            Text(stringResource(entry.name), fontWeight = FontWeight.ExtraBold,
                color = consoleWhite, fontSize = 17.sp)
            TextButton(onClick = { paused = !paused }, enabled = !finishedNow) {
                Text(if (paused) stringResource(R.string.arcade_resume)
                     else stringResource(R.string.arcade_pause))
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("${stringResource(R.string.arcade_score)}  ${scoreNow}",
                color = consoleAccent, fontWeight = FontWeight.Bold)
            Text("${stringResource(R.string.arcade_best)}  $best",
                color = consoleSecondary, fontWeight = FontWeight.Bold)
        }
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f)
                .background(Color(0xFF111A29), RoundedCornerShape(22.dp))
                .border(1.dp, Color(0xFF344057), RoundedCornerShape(22.dp))
                .padding(9.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                // Reading Compose state invalidates the Canvas every fixed tick.
                val currentFrame = revision
                if (currentFrame < 0) return@Canvas
                drawArcadeFrame(game)
            }
            if (paused || finishedNow) {
                Column(
                    modifier = Modifier.background(Color(0xE51A2335), RoundedCornerShape(18.dp))
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        stringResource(if (finishedNow) R.string.arcade_game_over
                            else R.string.arcade_paused),
                        fontWeight = FontWeight.Black,
                        fontSize = 26.sp,
                        color = consoleWhite,
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = {
                        game.reset()
                        restartGeneration++
                        revision++
                        paused = false
                    }) { Text(stringResource(R.string.arcade_restart)) }
                }
            }
        }
        Text(
            stringResource(entry.hint),
            modifier = Modifier.fillMaxWidth().padding(top = 9.dp),
            textAlign = TextAlign.Center,
            color = consoleSecondary,
            fontSize = 12.sp,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 11.dp, bottom = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArcadePadButton("◀", Modifier.weight(1f)) { dispatch(PadKey.LEFT) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                ArcadePadButton("▲", Modifier.fillMaxWidth()) { dispatch(PadKey.UP) }
                ArcadePadButton("▼", Modifier.fillMaxWidth()) { dispatch(PadKey.DOWN) }
            }
            ArcadePadButton("▶", Modifier.weight(1f)) { dispatch(PadKey.RIGHT) }
            ArcadePadButton("●", Modifier.weight(1f), action = true) { dispatch(PadKey.ACTION) }
        }
    }
}

@Composable
private fun ArcadePadButton(
    label: String,
    modifier: Modifier,
    action: Boolean = false,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(53.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        shape = RoundedCornerShape(17.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (action) consoleAccent else consoleCard,
            contentColor = if (action) consoleBg else consoleWhite,
        ),
    ) { Text(label, fontWeight = FontWeight.Black, fontSize = 24.sp) }
}
