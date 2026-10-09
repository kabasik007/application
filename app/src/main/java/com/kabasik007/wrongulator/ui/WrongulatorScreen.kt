package com.kabasik007.wrongulator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.stringResource
import com.kabasik007.wrongulator.R
import com.kabasik007.wrongulator.core.CalcKey
import com.kabasik007.wrongulator.core.ResultKind

private val background = Color(0xFF0A0F1E)
private val surface = Color(0xFF181F32)
private val darkKey = Color(0xFF202A40)
private val operationKey = Color(0xFF33294D)
private val accent = Color(0xFFFFC857)
private val purple = Color(0xFFC5B1FF)
private val muted = Color(0xFF9EAAC2)
private val white = Color(0xFFF3F5FC)

private val keypad = listOf(
    listOf(CalcKey.CLEAR, CalcKey.SIGN, CalcKey.PERCENT, CalcKey.DIVIDE),
    listOf(CalcKey.SEVEN, CalcKey.EIGHT, CalcKey.NINE, CalcKey.MULTIPLY),
    listOf(CalcKey.FOUR, CalcKey.FIVE, CalcKey.SIX, CalcKey.SUBTRACT),
    listOf(CalcKey.ONE, CalcKey.TWO, CalcKey.THREE, CalcKey.ADD),
    listOf(CalcKey.BACKSPACE, CalcKey.ZERO, CalcKey.DOT, CalcKey.EQUALS),
)

@Composable
fun WrongulatorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = accent,
            onPrimary = background,
            background = background,
            onBackground = white,
            surface = surface,
            onSurface = white,
        ),
        content = content,
    )
}

@Composable
fun WrongulatorScreen(
    viewModel: CalculatorViewModel,
    allowDebugPreview: Boolean,
) {
    var showPaywall by rememberSaveable { mutableStateOf(false) }
    val state = viewModel.state
    val accurate = viewModel.debugProPreview && allowDebugPreview

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(background),
    ) {
        val compact = maxHeight < 730.dp
        val buttonHeight = if (compact) 55.dp else 66.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "WRONGULATOR",
                        fontSize = 23.sp,
                        color = white,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.1.sp,
                    )
                    Text(
                        stringResource(R.string.app_tagline),
                        style = MaterialTheme.typography.labelSmall,
                        color = muted,
                    )
                }
                Surface(
                    color = if (accurate) Color(0xFF153C34) else operationKey,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.clickable {
                        if (accurate && allowDebugPreview) {
                            viewModel.setDebugProPreview(false)
                        } else {
                            showPaywall = true
                        }
                    },
                ) {
                    Text(
                        text = if (accurate) "PRO ✓" else "PRO ✦",
                        color = if (accurate) Color(0xFF98F0D0) else accent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }

            Spacer(Modifier.height(if (compact) 24.dp else 48.dp))
            Text(
                text = stringResource(R.string.hero_caption),
                color = muted,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (accurate) stringResource(R.string.pro_demo_label)
                else stringResource(R.string.free_label),
                color = if (accurate) Color(0xFF98F0D0) else accent,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(if (compact) 20.dp else 48.dp))
            Text(
                text = state.expression.ifBlank { " " },
                modifier = Modifier.fillMaxWidth(),
                color = muted,
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = state.display,
                modifier = Modifier.fillMaxWidth(),
                color = white,
                fontSize = when {
                    state.display.length > 18 -> 27.sp
                    state.display.length > 12 -> 36.sp
                    state.display.length > 8 -> 46.sp
                    else -> 64.sp
                },
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            val hint = when (state.resultKind) {
                ResultKind.READY -> R.string.idle_hint
                ResultKind.WRONG -> R.string.wrong_hint
                ResultKind.CORRECT -> R.string.correct_hint
                ResultKind.ERROR -> R.string.error_hint
            }
            Text(
                text = stringResource(hint),
                modifier = Modifier.fillMaxWidth(),
                color = if (state.resultKind == ResultKind.CORRECT) Color(0xFF98F0D0) else accent,
                textAlign = TextAlign.End,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )

            Spacer(Modifier.height(if (compact) 24.dp else 34.dp))
            Surface(
                modifier = Modifier.fillMaxWidth().clickable {
                    if (accurate && allowDebugPreview) {
                        viewModel.setDebugProPreview(false)
                    } else {
                        showPaywall = true
                    }
                },
                shape = RoundedCornerShape(18.dp),
                color = surface,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (accurate) stringResource(R.string.debug_leave_pro)
                        else stringResource(R.string.upgrade_button),
                        color = white,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    )
                    Text(if (accurate) "↩" else "↗", color = accent, fontSize = 20.sp)
                }
            }

            Spacer(Modifier.height(16.dp))
            keypad.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { key ->
                        val isEquals = key == CalcKey.EQUALS
                        val isOperator = key in listOf(
                            CalcKey.ADD, CalcKey.SUBTRACT, CalcKey.MULTIPLY, CalcKey.DIVIDE,
                        )
                        val isUtility = key in listOf(
                            CalcKey.CLEAR, CalcKey.SIGN, CalcKey.PERCENT, CalcKey.BACKSPACE,
                        )
                        Button(
                            onClick = { viewModel.press(key) },
                            modifier = Modifier.weight(1f).height(buttonHeight),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when {
                                    isEquals -> accent
                                    isOperator -> operationKey
                                    isUtility -> surface
                                    else -> darkKey
                                },
                                contentColor = when {
                                    isEquals -> background
                                    isOperator -> purple
                                    isUtility -> accent
                                    else -> white
                                },
                            ),
                        ) {
                            Text(
                                text = key.label,
                                fontSize = if (key == CalcKey.CLEAR) 18.sp else 25.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            Text(
                text = stringResource(R.string.app_footer),
                modifier = Modifier.fillMaxWidth().padding(top = 7.dp, bottom = 10.dp),
                color = muted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
            )
        }
    }

    if (showPaywall) {
        AlertDialog(
            onDismissRequest = { showPaywall = false },
            title = {
                Text(
                    text = stringResource(R.string.upgrade_title),
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.upgrade_body))
                    Text(
                        text = stringResource(R.string.paywall_notice),
                        color = accent,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    if (allowDebugPreview) {
                        TextButton(
                            onClick = {
                                viewModel.setDebugProPreview(true)
                                showPaywall = false
                            },
                        ) {
                            Text(stringResource(R.string.debug_try_pro))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPaywall = false }) {
                    Text(stringResource(R.string.upgrade_understood))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaywall = false }) {
                    Text(stringResource(R.string.upgrade_close))
                }
            },
        )
    }
}
