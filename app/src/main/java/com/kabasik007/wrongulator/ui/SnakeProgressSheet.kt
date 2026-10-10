package com.kabasik007.wrongulator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kabasik007.wrongulator.R
import com.kabasik007.wrongulator.arcade.SnakeAchievement
import com.kabasik007.wrongulator.arcade.SnakeGame
import com.kabasik007.wrongulator.arcade.SnakeMission

/**
 * Offline achievements, local personal best and three inspectable challenge missions.
 * No fake multiplayer or server-backed ranking.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SnakeProgressSheet(
    game: SnakeGame,
    best: Int,
    unlocked: Set<SnakeAchievement>,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF152237),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 670.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp).padding(bottom = 25.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.snake_progress),
                color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Text("${stringResource(R.string.arcade_best)}: $best    ${stringResource(R.string.arcade_score)}: ${game.score}",
                color = Color(0xFF81E6BD), fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.snake_missions),
                color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            SnakeMission.entries.forEach { mission ->
                val (progress, goal) = when (mission) {
                    SnakeMission.SURVIVE_60 -> (game.timeMs / 1_000L).toInt() to 60
                    SnakeMission.DESTROY_10 -> game.destroyedTotal to 10
                    SnakeMission.LENGTH_30 -> game.length to 30
                }
                ProgressItem(
                    name = when (mission) {
                        SnakeMission.SURVIVE_60 -> stringResource(R.string.snake_mission_survive)
                        SnakeMission.DESTROY_10 -> stringResource(R.string.snake_mission_destroy)
                        SnakeMission.LENGTH_30 -> stringResource(R.string.snake_mission_length)
                    },
                    value = "${progress.coerceAtMost(goal)} / $goal",
                    completed = progress >= goal,
                )
            }
            Text(stringResource(R.string.snake_achievements),
                color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            SnakeAchievement.entries.forEach { achievement ->
                ProgressItem(
                    name = when (achievement) {
                        SnakeAchievement.FIRST_FOOD -> stringResource(R.string.snake_achievement_food)
                        SnakeAchievement.SCORE_100 -> "100 ${stringResource(R.string.arcade_score)}"
                        SnakeAchievement.SCORE_500 -> "500 ${stringResource(R.string.arcade_score)}"
                        SnakeAchievement.DESTROY_10 -> stringResource(R.string.snake_mission_destroy)
                        SnakeAchievement.LENGTH_20 -> stringResource(R.string.snake_achievement_length)
                        SnakeAchievement.SURVIVE_60 -> stringResource(R.string.snake_mission_survive)
                        SnakeAchievement.NO_HIT_60 -> stringResource(R.string.snake_achievement_no_hit)
                        SnakeAchievement.BOSS_WIN -> stringResource(R.string.snake_achievement_boss)
                    },
                    value = if (achievement in unlocked) "✓" else "○",
                    completed = achievement in unlocked,
                )
            }
            Button(onClick = onShare, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.snake_share))
            }
            Text(stringResource(R.string.snake_offline_leaderboard),
                color = Color(0xFFAABFD0), fontSize = 12.sp)
        }
    }
}

@Composable
private fun ProgressItem(name: String, value: String, completed: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(name, color = Color(0xFFF2F5FF), fontSize = 13.sp)
        Text(value, color = if (completed) Color(0xFF81E6BD) else Color(0xFFABB7C8),
            fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
