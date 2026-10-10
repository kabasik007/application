package com.kabasik007.wrongulator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kabasik007.wrongulator.R
import com.kabasik007.wrongulator.arcade.SnakeDifficulty
import com.kabasik007.wrongulator.arcade.SnakeMap
import com.kabasik007.wrongulator.arcade.SnakeMode
import com.kabasik007.wrongulator.arcade.SnakeSkin

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun SnakeSettingsSheet(
    current: SnakePlayerSettings,
    onApply: (SnakePlayerSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember(current) { mutableStateOf(current) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF152237),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 720.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.snake_settings),
                color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.snake_settings_hint),
                color = Color(0xFFABC1D1), fontSize = 12.sp)

            SectionLabel(stringResource(R.string.snake_mode))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SnakeMode.entries.forEach { mode ->
                    SelectChip(modeName(mode), draft.rules.mode == mode) {
                        draft = draft.copy(rules = draft.rules.copy(mode = mode))
                    }
                }
            }

            SectionLabel(stringResource(R.string.snake_difficulty))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SnakeDifficulty.entries.forEach { difficulty ->
                    SelectChip(difficultyName(difficulty),
                        draft.rules.difficulty == difficulty) {
                        draft = draft.copy(rules = draft.rules.copy(difficulty = difficulty))
                    }
                }
            }

            SectionLabel(stringResource(R.string.snake_map))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SnakeMap.entries.forEach { map ->
                    SelectChip(mapName(map), draft.rules.map == map) {
                        draft = draft.copy(rules = draft.rules.copy(map = map))
                    }
                }
            }

            SectionLabel(stringResource(R.string.snake_skin))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SnakeSkin.entries.forEach { skin ->
                    SelectChip(skinName(skin), draft.rules.skin == skin) {
                        draft = draft.copy(rules = draft.rules.copy(skin = skin))
                    }
                }
            }

            if (draft.rules.mode == SnakeMode.COMBAT) {
                SectionLabel("${stringResource(R.string.snake_rivals)}: ${draft.rules.rivals}")
                Slider(
                    value = draft.rules.rivals.toFloat(),
                    onValueChange = {
                        draft = draft.copy(rules = draft.rules.copy(rivals = it.toInt().coerceIn(0, 3)))
                    },
                    valueRange = 0f..3f,
                    steps = 2,
                )
            }

            SectionLabel(stringResource(R.string.snake_frame_rate))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectChip("30 FPS", draft.fps == 30) { draft = draft.copy(fps = 30) }
                SelectChip("60 FPS", draft.fps == 60) { draft = draft.copy(fps = 60) }
            }
            SettingToggle(stringResource(R.string.snake_left_handed), draft.leftHanded) {
                draft = draft.copy(leftHanded = it)
            }
            SettingToggle(stringResource(R.string.snake_haptics), draft.haptics) {
                draft = draft.copy(haptics = it)
            }
            SettingToggle(stringResource(R.string.snake_sound), draft.sound) {
                draft = draft.copy(sound = it)
            }
            Button(
                onClick = { onApply(draft) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF81E6BD)),
            ) {
                Text(stringResource(R.string.snake_apply_restart),
                    color = Color(0xFF0A1422), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = Color(0xFFC1D4E8), fontWeight = FontWeight.Bold, fontSize = 14.sp)
}

@Composable
private fun SelectChip(text: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(text) })
}

@Composable
private fun SettingToggle(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, color = Color.White)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun modeName(value: SnakeMode) = stringResource(when (value) {
    SnakeMode.CLASSIC -> R.string.snake_classic
    SnakeMode.COMBAT -> R.string.snake_combat
    SnakeMode.ZEN -> R.string.snake_zen
    SnakeMode.TIME_ATTACK -> R.string.snake_time_attack
})

@Composable
private fun difficultyName(value: SnakeDifficulty) = stringResource(when (value) {
    SnakeDifficulty.CASUAL -> R.string.snake_casual
    SnakeDifficulty.NORMAL -> R.string.snake_normal
    SnakeDifficulty.EXPERT -> R.string.snake_expert
})

@Composable
private fun mapName(value: SnakeMap) = stringResource(when (value) {
    SnakeMap.ARENA -> R.string.snake_arena
    SnakeMap.WRAP -> R.string.snake_wrap
    SnakeMap.PORTALS -> R.string.snake_portals
    SnakeMap.HAZARDS -> R.string.snake_hazards
})

@Composable
private fun skinName(value: SnakeSkin) = stringResource(when (value) {
    SnakeSkin.MINT -> R.string.snake_mint
    SnakeSkin.GOLD -> R.string.snake_gold
    SnakeSkin.VIOLET -> R.string.snake_violet
    SnakeSkin.HIGH_CONTRAST -> R.string.snake_high_contrast
})
