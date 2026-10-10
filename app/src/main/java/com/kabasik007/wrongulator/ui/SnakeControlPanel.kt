package com.kabasik007.wrongulator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kabasik007.wrongulator.R
import com.kabasik007.wrongulator.arcade.PadKey
import com.kabasik007.wrongulator.arcade.SnakeWeapon

/**
 * Independent controller component. The handedness preference swaps left/right
 * controls; gameplay, scoring and UI shell do not know about layout details.
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
    BoxWithConstraints {
        val widthDp = maxWidth.value
        val key = (widthDp * .163f).coerceIn(42f, 58f).dp
        val action = (widthDp * .212f).coerceIn(62f, 78f).dp
        val controls: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Spacer(Modifier.size(key))
                    DirectionButton("▲", key, active) { onDirection(PadKey.UP) }
                    Spacer(Modifier.size(key))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DirectionButton("◀", key, active) { onDirection(PadKey.LEFT) }
                    Box(
                        Modifier.size(key).background(Color(0xFF203649), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) { Text("✚", fontSize = 20.sp, color = Color(0xFF9EBDCB)) }
                    DirectionButton("▶", key, active) { onDirection(PadKey.RIGHT) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Spacer(Modifier.size(key))
                    DirectionButton("▼", key, active) { onDirection(PadKey.DOWN) }
                    Spacer(Modifier.size(key))
                }
            }
        }
        val actions: @Composable () -> Unit = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                TextButton(onClick = onWeapon, enabled = canFire) {
                    Text(
                        when (weapon) {
                            SnakeWeapon.GRENADE -> "💥 1"
                            SnakeWeapon.BOUNCE -> "↪ 2"
                            SnakeWeapon.CHAIN -> "⚡ 3"
                        },
                        color = Color(0xFFFFC08B), fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Button(
                    onClick = onFire,
                    enabled = active && canFire && ammo > 0,
                    modifier = Modifier.width(action + 15.dp).height(key * .85f),
                    shape = RoundedCornerShape(17.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFB879),
                        contentColor = Color(0xFF07111B),
                    ),
                ) { Text("💥 $ammo", fontWeight = FontWeight.Black, fontSize = 18.sp) }
                Text(stringResource(mainLabel), color = Color(0xFF81E6BD),
                    fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Button(
                    onClick = onMain,
                    modifier = Modifier.size(action),
                    shape = CircleShape,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF81E6BD),
                        contentColor = Color(0xFF061422),
                    ),
                ) { Text(mainSymbol, fontSize = 27.sp, fontWeight = FontWeight.Black) }
            }
        }
        Row(
            modifier = Modifier.padding(bottom = 4.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (leftHanded) {
                actions()
                Spacer(Modifier.weight(1f))
                controls()
            } else {
                controls()
                Spacer(Modifier.weight(1f))
                actions()
            }
        }
    }
}

@Composable
private fun DirectionButton(label: String, size: Dp, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(size),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF17263A),
            contentColor = Color(0xFFF4FAFF),
        ),
    ) { Text(label, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
}
