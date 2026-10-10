package com.kabasik007.wrongulator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kabasik007.wrongulator.R
import com.kabasik007.wrongulator.arcade.PadKey
import com.kabasik007.wrongulator.arcade.SnakeWeapon

private val keyBase = Color(0xFF29445D)
private val keyBorder = Color(0xFF4F7896)
private val keyText = Color(0xFFF6FDFF)
private val actionMint = Color(0xFF81E6BD)

/**
 * Every directional tile is a clickable 54–68 dp surface, including the center.
 * The D-pad works during the 1-second startup transition, queuing a turn in
 * the independent simulation rather than silently discarding input.
 */
@Composable
internal fun SnakeControlPanel(
    leftHanded: Boolean,
    active: Boolean,
    canFire: Boolean,
    ammo: Int,
    weapon: SnakeWeapon,
    mainLabel: Int,
    mainSymbol: String,
    onDirection: (PadKey) -> Unit,
    onFire: () -> Unit,
    onMain: () -> Unit,
    onWeapon: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        // Tiles scale with width but don't shrink below Android's comfortable touch target.
        val key = (maxWidth.value * .175f).coerceIn(54f, 68f).dp
        val action = (maxWidth.value * .205f).coerceIn(66f, 80f).dp
        val gap = 3.dp
        val pad: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    Spacer(Modifier.size(key))
                    SnakeDpadKey("▲", key, active, "Up") { onDirection(PadKey.UP) }
                    Spacer(Modifier.size(key))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    SnakeDpadKey("◀", key, active, "Left") { onDirection(PadKey.LEFT) }
                    SnakeDpadKey("Ⅱ", key, true, "Pause or resume", center = true, onClick = onMain)
                    SnakeDpadKey("▶", key, active, "Right") { onDirection(PadKey.RIGHT) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    Spacer(Modifier.size(key))
                    SnakeDpadKey("▼", key, active, "Down") { onDirection(PadKey.DOWN) }
                    Spacer(Modifier.size(key))
                }
            }
        }
        val actions: @Composable () -> Unit = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                TextButton(
                    onClick = onWeapon,
                    enabled = canFire,
                    modifier = Modifier.height(36.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 8.dp, vertical = 0.dp,
                    ),
                ) {
                    Text(
                        when (weapon) {
                            SnakeWeapon.GRENADE -> "💥 GRENADE"
                            SnakeWeapon.BOUNCE -> "↪ BOUNCE"
                            SnakeWeapon.CHAIN -> "⚡ CHAIN"
                        },
                        color = Color(0xFFFFCB96),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
                Button(
                    onClick = onFire,
                    enabled = active && canFire && ammo > 0,
                    modifier = Modifier.width(action + 20.dp).height(49.dp),
                    shape = RoundedCornerShape(17.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFB67C),
                        contentColor = Color(0xFF092030),
                        disabledContainerColor = Color(0xFF293442),
                        disabledContentColor = Color(0xFFA6AFC0),
                    ),
                ) { Text("💥 $ammo", fontSize = 20.sp, fontWeight = FontWeight.Black) }
                Text(
                    stringResource(mainLabel),
                    color = actionMint,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
                Button(
                    onClick = onMain,
                    modifier = Modifier.size(action),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = actionMint,
                        contentColor = Color(0xFF061625),
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                ) { Text(mainSymbol, fontSize = 30.sp, fontWeight = FontWeight.Black) }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            if (leftHanded) {
                actions()
                Spacer(Modifier.weight(1f))
                pad()
            } else {
                pad()
                Spacer(Modifier.weight(1f))
                actions()
            }
        }
    }
}

@Composable
private fun SnakeDpadKey(
    symbol: String,
    size: Dp,
    enabled: Boolean,
    label: String,
    center: Boolean = false,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(if (center) 18.dp else 15.dp)
    val background = if (center) Color(0xFF1F6E78) else keyBase
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(background.copy(alpha = if (enabled) 1f else .45f),
                        background.copy(alpha = if (enabled) .84f else .34f)),
                ),
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            symbol,
            color = if (enabled) keyText else Color(0xFF9CA9B8),
            fontWeight = FontWeight.Black,
            fontSize = if (center) 24.sp else 30.sp,
        )
    }
}
