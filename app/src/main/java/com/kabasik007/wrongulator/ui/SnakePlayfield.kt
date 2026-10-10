package com.kabasik007.wrongulator.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kabasik007.wrongulator.R
import com.kabasik007.wrongulator.arcade.PadKey
import com.kabasik007.wrongulator.arcade.SnakeGame
import kotlin.math.abs

/** Only the Canvas reads interpolation per-vsync, keeping the HUD free of 60 FPS recompositions. */
@Composable
internal fun SnakePlayfield(
    game: SnakeGame,
    modifier: Modifier,
    revision: Int,
    interpolation: Float,
    running: Boolean,
    paused: Boolean,
    started: Boolean,
    countdown: Int,
    onMain: () -> Unit,
    onDirection: (PadKey) -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF0B1626))
            .border(1.dp, Color(0xFF31475B), RoundedCornerShape(22.dp))
            .pointerInput(game, running) {
                var drag = Offset.Zero
                val limit = 22.dp.toPx()
                detectDragGestures(
                    onDragStart = { drag = Offset.Zero },
                    onDragCancel = { drag = Offset.Zero },
                    onDragEnd = { drag = Offset.Zero },
                ) { change, delta ->
                    change.consume()
                    drag += delta
                    if (running && (abs(drag.x) > limit || abs(drag.y) > limit)) {
                        onDirection(if (abs(drag.x) > abs(drag.y)) {
                            if (drag.x > 0) PadKey.RIGHT else PadKey.LEFT
                        } else if (drag.y > 0) PadKey.DOWN else PadKey.UP)
                        drag = Offset.Zero
                    }
                }
            }
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val frame = revision
            val progress = interpolation
            if (frame >= 0) drawArcadeFrame(game, progress)
        }
        if (!running) {
            Surface(
                shape = RoundedCornerShape(21.dp),
                color = Color(0xF0162639),
                border = BorderStroke(1.dp, Color(0xFF416B73)),
                modifier = Modifier.padding(9.dp),
            ) {
                Column(
                    Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Text(
                        when {
                            game.finished -> stringResource(R.string.arcade_game_over)
                            countdown > 0 -> countdown.toString()
                            !started -> stringResource(R.string.snake_ready)
                            else -> stringResource(R.string.arcade_paused)
                        },
                        color = Color(0xFFF4FAFF), fontSize = 25.sp, fontWeight = FontWeight.Black,
                    )
                    if (game.finished) {
                        Text("${stringResource(R.string.arcade_score)}  ${game.score}",
                            color = Color(0xFF81E6BD), fontSize = 21.sp,
                            fontWeight = FontWeight.Bold)
                    }
                    if (countdown == 0) {
                        Button(
                            onClick = onMain,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF81E6BD),
                                contentColor = Color(0xFF081423),
                            ),
                        ) {
                            Text(stringResource(when {
                                game.finished -> R.string.arcade_restart
                                !started -> R.string.snake_start
                                else -> R.string.arcade_resume
                            }), fontWeight = FontWeight.ExtraBold)
                        }
                    } else {
                        Text(stringResource(R.string.snake_countdown),
                            color = Color(0xFFA5BDCD), fontSize = 12.sp,
                            textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
