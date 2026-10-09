package com.kabasik007.wrongulator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kabasik007.wrongulator.R

enum class AppDestination(val title: Int, val symbol: String) {
    CALCULATOR(R.string.menu_calculator, "≠"),
    TOOLS(R.string.menu_tools, "✓"),
    ABOUT(R.string.menu_about, "ⓘ")
}

@Composable
fun AppNavigationDrawer(selected: AppDestination, onNavigate: (AppDestination) -> Unit) {
    ModalDrawerSheet(
        modifier = Modifier.widthIn(max = 304.dp),
        drawerContainerColor = Color(0xFF151D2D),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("WRONGULATOR", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
            Text(stringResource(R.string.drawer_subtitle), color = Color(0xFFAAB6CB), fontSize = 12.sp)
        }
        AppDestination.entries.forEach { page ->
            NavigationDrawerItem(
                label = { Text(stringResource(page.title)) },
                icon = { Text(page.symbol, fontSize = 22.sp) },
                selected = page == selected,
                onClick = { onNavigate(page) },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp),
                shape = RoundedCornerShape(16.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            stringResource(R.string.drawer_footer),
            modifier = Modifier.padding(22.dp),
            color = Color(0xFFAAB6CB),
            fontSize = 12.sp,
        )
    }
}

@Composable
fun SubpageHeader(title: String, onMenu: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onMenu,
            modifier = Modifier.semantics { contentDescription = "Open menu" },
        ) {
            Text("☰", fontSize = 24.sp, color = Color(0xFFF8FAFF))
        }
        Spacer(Modifier.width(6.dp))
        Text(title, fontSize = 19.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().background(Color(0xFF090D17))
            .verticalScroll(rememberScrollState()).padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("WRONGULATOR ≠", fontWeight = FontWeight.ExtraBold, fontSize = 30.sp)
        Text(
            stringResource(R.string.about_explanation),
            color = Color(0xFFE6EAF4),
            fontSize = 16.sp,
            lineHeight = 24.sp,
        )
        Text(
            stringResource(R.string.about_tools),
            color = Color(0xFF81E6BD),
            fontSize = 15.sp,
            lineHeight = 23.sp,
        )
        Text(
            stringResource(R.string.about_billing),
            color = Color(0xFFAAB6CB),
            fontSize = 13.sp,
            lineHeight = 20.sp,
        )
    }
}
