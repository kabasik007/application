package com.kabasik007.wrongulator.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kabasik007.wrongulator.R
import com.kabasik007.wrongulator.arcade.PadKey
import com.kabasik007.wrongulator.arcade.SnakeGame
import kotlin.math.abs

/**
 * Game field owns only rendering and swipe detection.
 * Never overlays a "Ready" countdown on entry: the field fades in over 900 ms,
 * while controller input is already accepted by the session.
 */
@Composable
internal fun SnakePlayfield(
    game: SnakeGame,
    modifier: Modifier,
    revision: Int,
    interpolation: Float,
    paused: Boolean,
    onMain: () -> Unit,
    onDirection: (PadKey) -> Unit,
) {
    val fadeIn = remember(game) { Animatable(.65f) }
    val latestDirection by rememberUpdatedState(onDirection)
    LaunchedEffect(game) { fadeIn.animateTo(1f, tween(900)) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF0B1626))
            .border(1.dp, Color(0xFF38576C), RoundedCornerShape(18.dp))
            .pointerInput(game) {
                var drag = Offset.Zero
                val threshold = 14.dp.toPx()
                detectDragGestures(
                    onDragStart = { drag = Offset.Zero },
                    onDragEnd = { drag = Offset.Zero },
                    onDragCancel = { drag = Offset.Zero },
                ) { change, delta ->
                    change.consume()
                    drag += delta
                    if (abs(drag.x) >= threshold || abs(drag.y) >= threshold) {
                        val direction = if (abs(drag.x) >= abs(drag.y)) {
                            if (drag.x > 0f) PadKey.RIGHT else PadKey.LEFT
                        } else {
                            if (drag.y > 0f) PadKey.DOWN else PadKey.UP
                        }
                        latestDirection(direction)
                        drag = Offset.Zero
                    }
                }
            }
            .padding(3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = fadeIn.value }) {
            val frame = revision
            val progress = interpolation
            if (frame >= 0) drawArcadeFrame(game, progress)
        }

        if (paused || game.finished) {
            Surface(
                shape = RoundedCornerShape(19.dp),
                color = Color(0xEB162638),
                border = BorderStroke(1.dp, Color(0xFF4C8D8A)),
                modifier = Modifier.padding(12.dp),
            ) {
                Column(
                    Modifier.padding(horizontal = 22.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Text(
                        stringResource(if (game.finished) R.string.arcade_game_over else R.string.arcade_paused),
                        color = Color(0xFFF7FAFF),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                    )
                    if (game.finished) {
                        Text("${stringResource(R.string.arcade_score)} ${game.score}",
                            color = Color(0xFF81E6BD), fontSize = 20.sp,
                            fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onMain,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF81E6BD),
                            contentColor = Color(0xFF0B1626),
                        ),
                    ) {
                        Text(stringResource(
                            if (game.finished) R.string.arcade_restart else R.string.arcade_resume,
                        ), fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}
