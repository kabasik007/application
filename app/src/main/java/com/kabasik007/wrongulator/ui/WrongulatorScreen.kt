package com.kabasik007.wrongulator.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kabasik007.wrongulator.R
import com.kabasik007.wrongulator.core.CalcKey
import com.kabasik007.wrongulator.core.ResultKind
import kotlinx.coroutines.launch

private val ink = Color(0xFF090D17)
private val panel = Color(0xFF171D2B)
private val keyColor = Color(0xFF212A3B)
private val operatorColor = Color(0xFF2C324D)
private val accent = Color(0xFFA8B9FF)
private val mint = Color(0xFF81E6BD)
private val pale = Color(0xFFF8FAFF)
private val subtle = Color(0xFF9FAAC0)
private val divider = Color(0xFF384156)

private val keyboard = listOf(
    listOf(CalcKey.CLEAR, CalcKey.SIGN, CalcKey.PERCENT, CalcKey.DIVIDE),
    listOf(CalcKey.SEVEN, CalcKey.EIGHT, CalcKey.NINE, CalcKey.MULTIPLY),
    listOf(CalcKey.FOUR, CalcKey.FIVE, CalcKey.SIX, CalcKey.SUBTRACT),
    listOf(CalcKey.ONE, CalcKey.TWO, CalcKey.THREE, CalcKey.ADD),
    listOf(CalcKey.BACKSPACE, CalcKey.ZERO, CalcKey.DOT, CalcKey.EQUALS),
)

private enum class Plan(val nameRes: Int, val detailRes: Int, val price: String) {
    MONTH(R.string.plan_month, R.string.plan_month_detail, "$1"),
    GIFT(R.string.plan_gift, R.string.plan_gift_detail, "$2"),
    FOREVER(R.string.plan_forever, R.string.plan_forever_detail, "$19.99"),
}

@Composable
fun WrongulatorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = accent,
            onPrimary = ink,
            background = ink,
            onBackground = pale,
            surface = panel,
            onSurface = pale,
        ),
        content = content,
    )
}

/** Everything important stays inside the screen area; no caption/footer occupying story space. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WrongulatorScreen(
    viewModel: CalculatorViewModel,
    allowDebugPreview: Boolean,
) {
    val state = viewModel.state
    val accurate = allowDebugPreview && viewModel.debugProPreview
    var showPaywall by rememberSaveable { mutableStateOf(false) }
    var showBillingNotice by rememberSaveable { mutableStateOf(false) }
    var selectedName by rememberSaveable { mutableStateOf(Plan.GIFT.name) }
    var handledTrigger by rememberSaveable { mutableIntStateOf(viewModel.paywallTrigger) }
    val selected = Plan.valueOf(selectedName)
    val wobble = remember { Animatable(0f) }
    var arcadePlaying by remember { mutableStateOf(false) }
    var destinationName by rememberSaveable { mutableStateOf(AppDestination.CALCULATOR.name) }
    val destination = AppDestination.valueOf(destinationName)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()
    val onMenu: () -> Unit = { drawerScope.launch { drawerState.open() } }

    LaunchedEffect(viewModel.paywallTrigger) {
        val trigger = viewModel.paywallTrigger
        if (trigger > handledTrigger) {
            handledTrigger = trigger
            wobble.snapTo(0f)
            wobble.animateTo(0f, keyframes {
                durationMillis = 460
                12f at 70
                -12f at 140
                9f at 230
                -7f at 325
                0f at 460
            })
            showPaywall = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppNavigationDrawer(destination) { page ->
                destinationName = page.name
                drawerScope.launch { drawerState.close() }
            }
        },
    ) {
        if (destination == AppDestination.CALCULATOR) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ink)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        // Fit 5 keypad rows onto short phones and standard 9:16 story recordings.
        val keyHeight = ((maxHeight.value - 245f) / 5f).coerceIn(48f, 74f).dp
        val tight = maxHeight < 640.dp

        Column(
            modifier = Modifier.fillMaxSize().padding(
                horizontal = if (tight) 14.dp else 18.dp,
                vertical = if (tight) 9.dp else 14.dp,
            ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(44.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onMenu) {
                        Text("☰", color = pale, fontSize = 24.sp)
                    }
                    Text(
                        text = "WRONGULATOR",
                        fontSize = if (tight) 18.sp else 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                        color = pale,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = operatorColor,
                    modifier = Modifier.clickable {
                        if (accurate) viewModel.setAccuracyPreviewEnabled(false)
                        else showPaywall = true
                    },
                ) {
                    Text(
                        text = if (accurate) "PRO ✓" else "PRO ↗",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                        color = if (accurate) mint else accent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().weight(1f).graphicsLayer {
                    translationX = wobble.value
                    rotationZ = wobble.value * 0.12f
                },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = state.expression.ifEmpty { " " },
                    color = subtle,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = state.display,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                    color = pale,
                    fontSize = when {
                        state.display.length > 18 -> 27.sp
                        state.display.length > 12 -> 35.sp
                        state.display.length > 8 -> 44.sp
                        else -> 63.sp
                    },
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (state.resultKind == ResultKind.WRONG) {
                    Text(
                        stringResource(R.string.parody_result),
                        color = accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                } else if (state.resultKind == ResultKind.ERROR) {
                    Text(stringResource(R.string.error_short), color = subtle, fontSize = 11.sp)
                } else if (state.resultKind == ResultKind.CORRECT) {
                    Text(stringResource(R.string.pro_preview), color = mint, fontSize = 11.sp)
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = panel,
                modifier = Modifier.fillMaxWidth().clickable {
                    if (accurate) viewModel.setAccuracyPreviewEnabled(false)
                    else showPaywall = true
                },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = if (tight) 11.dp else 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(
                            if (accurate) R.string.pro_preview else R.string.unlock_accuracy
                        ),
                        color = pale,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    )
                    Text(if (accurate) "✓" else "↗", color = accent, fontSize = 19.sp)
                }
            }
            Spacer(Modifier.height(if (tight) 10.dp else 16.dp))
            keyboard.forEachIndexed { index, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { key ->
                        val equals = key == CalcKey.EQUALS
                        val operation = key == CalcKey.ADD || key == CalcKey.SUBTRACT ||
                            key == CalcKey.MULTIPLY || key == CalcKey.DIVIDE
                        val utility = key == CalcKey.CLEAR || key == CalcKey.SIGN ||
                            key == CalcKey.PERCENT || key == CalcKey.BACKSPACE
                        Button(
                            onClick = { viewModel.press(key) },
                            modifier = Modifier.weight(1f).height(keyHeight),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when {
                                    equals -> accent
                                    operation -> operatorColor
                                    utility -> panel
                                    else -> keyColor
                                },
                                contentColor = when {
                                    equals -> ink
                                    operation -> accent
                                    utility -> accent
                                    else -> pale
                                },
                            ),
                        ) {
                            Text(
                                text = key.label,
                                fontSize = if (key == CalcKey.CLEAR) 17.sp else 24.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
                if (index != keyboard.lastIndex) Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(if (tight) 2.dp else 8.dp))
        }
    }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().background(ink)
                    .windowInsetsPadding(WindowInsets.safeDrawing),
            ) {
                if (destination != AppDestination.ARCADE || !arcadePlaying) {
                    SubpageHeader(stringResource(destination.title), onMenu)
                }
                when (destination) {
                    AppDestination.TOOLS -> ToolsScreen(Modifier.weight(1f))
                    AppDestination.ARCADE -> ArcadeScreen(
                        Modifier.weight(1f),
                        onPlayingChange = { arcadePlaying = it },
                    )
                    else -> AboutScreen(Modifier.weight(1f))
                }
            }
        }
    }

    if (showPaywall && destination == AppDestination.CALCULATOR) {
        ModalBottomSheet(
            onDismissRequest = { showPaywall = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = panel,
            contentColor = pale,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .heightIn(max = 670.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.paywall_eyebrow),
                    color = mint,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp,
                )
                Text(
                    text = stringResource(R.string.paywall_heading),
                    color = pale,
                    fontSize = 29.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.paywall_subtitle),
                    color = subtle,
                    fontSize = 14.sp,
                )

                Plan.entries.forEach { plan ->
                    val picked = plan == selected
                    Surface(
                        modifier = Modifier.fillMaxWidth().selectable(
                            selected = picked,
                            role = Role.RadioButton,
                            onClick = { selectedName = plan.name },
                        ),
                        shape = RoundedCornerShape(20.dp),
                        color = if (picked) Color(0xFF272F4E) else Color(0xFF202636),
                        border = BorderStroke(if (picked) 2.dp else 1.dp, if (picked) accent else divider),
                    ) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            if (plan == Plan.GIFT) {
                                Surface(color = mint, shape = RoundedCornerShape(6.dp)) {
                                    Text(
                                        stringResource(R.string.free_third_month),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                        color = ink,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        stringResource(plan.nameRes),
                                        color = pale,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        stringResource(plan.detailRes),
                                        color = subtle,
                                        fontSize = 12.sp,
                                    )
                                }
                                Text(
                                    plan.price,
                                    color = if (picked) mint else pale,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = { showBillingNotice = true },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = ink,
                    ),
                ) {
                    Text(
                        text = "${stringResource(R.string.paywall_continue)} · ${selected.price}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = stringResource(R.string.paywall_disclosure),
                    color = subtle,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    textAlign = TextAlign.Center,
                )
                if (allowDebugPreview) {
                    TextButton(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        onClick = {
                            viewModel.setAccuracyPreviewEnabled(true)
                            showPaywall = false
                        },
                    ) {
                        Text(
                            stringResource(R.string.debug_try_pro),
                            color = mint,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
    }

    if (showBillingNotice) {
        AlertDialog(
            onDismissRequest = { showBillingNotice = false },
            title = { Text(stringResource(R.string.billing_demo_title)) },
            text = { Text(stringResource(R.string.billing_demo_message)) },
            confirmButton = {
                TextButton(onClick = { showBillingNotice = false }) {
                    Text(stringResource(R.string.billing_demo_ok))
                }
            },
        )
    }
}
