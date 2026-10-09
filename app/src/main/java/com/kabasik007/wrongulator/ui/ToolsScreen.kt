package com.kabasik007.wrongulator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kabasik007.wrongulator.R
import com.kabasik007.wrongulator.core.PracticalMath
import java.math.BigDecimal

private val toolPanel = Color(0xFF181F31)
private val toolAccent = Color(0xFF81E6BD)
private val toolSubtle = Color(0xFFAAB6CB)

@Composable
fun ToolsScreen(modifier: Modifier = Modifier) {
    var price by rememberSaveable { mutableStateOf("100") }
    var discount by rememberSaveable { mutableStateOf("20") }
    var bill by rememberSaveable { mutableStateOf("120") }
    var tip by rememberSaveable { mutableStateOf("10") }
    var people by rememberSaveable { mutableStateOf("3") }

    val priceValue = PracticalMath.parseAmount(price)
    val discountValue = PracticalMath.parseAmount(discount)
    val discountResult = if (priceValue != null && discountValue != null &&
        discountValue <= BigDecimal("100")
    ) PracticalMath.discount(priceValue, discountValue) else null

    val billValue = PracticalMath.parseAmount(bill)
    val tipValue = PracticalMath.parseAmount(tip)
    val peopleValue = people.toIntOrNull()
    val splitResult = if (billValue != null && tipValue != null &&
        tipValue <= BigDecimal("100") && peopleValue != null &&
        peopleValue in 1..100
    ) PracticalMath.split(billValue, tipValue, peopleValue) else null

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090D17))
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            stringResource(R.string.tools_title),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF8FAFF),
        )
        Text(
            stringResource(R.string.tools_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = toolSubtle,
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = toolPanel),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.discount_title), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                NumberInput(price, { price = it }, R.string.original_price)
                NumberInput(discount, { discount = it }, R.string.discount_percent)
                HorizontalDivider(color = Color(0xFF3B4559))
                ResultLine(R.string.savings, discountResult?.savings?.let(PracticalMath::money))
                ResultLine(R.string.final_price, discountResult?.final?.let(PracticalMath::money), important = true)
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = toolPanel),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.split_title), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                NumberInput(bill, { bill = it }, R.string.bill_amount)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumberInput(
                        tip, { tip = it }, R.string.tip_percent,
                        modifier = Modifier.weight(1f),
                    )
                    NumberInput(
                        people, { people = it }, R.string.people_count,
                        modifier = Modifier.weight(1f), integer = true,
                    )
                }
                HorizontalDivider(color = Color(0xFF3B4559))
                ResultLine(R.string.total_with_tip, splitResult?.total?.let(PracticalMath::money))
                ResultLine(R.string.per_person, splitResult?.perPerson?.let(PracticalMath::money), important = true)
            }
        }

        Text(stringResource(R.string.tools_live_note), color = toolSubtle, fontSize = 12.sp)
    }
}

@Composable
private fun NumberInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: Int,
    modifier: Modifier = Modifier,
    integer: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { entered ->
            if (entered.length <= 14 && entered.all { ch ->
                ch.isDigit() || (!integer && (ch == '.' || ch == ','))
            }) onValueChange(entered)
        },
        label = { Text(stringResource(label), fontSize = 13.sp) },
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            keyboardType = if (integer) KeyboardType.Number else KeyboardType.Decimal,
        ),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
    )
}

@Composable
private fun ResultLine(label: Int, value: String?, important: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(label), color = toolSubtle)
        Text(
            value ?: "—",
            color = if (important) toolAccent else Color(0xFFF8FAFF),
            fontWeight = FontWeight.Bold,
            fontSize = if (important) 23.sp else 17.sp,
        )
    }
}
