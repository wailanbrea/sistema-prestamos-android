package com.sistemaprestamista.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sistemaprestamista.mobile.data.model.CashMovementItem
import com.sistemaprestamista.mobile.data.model.CashSummary
import com.sistemaprestamista.mobile.ui.components.rememberCurrency

private val ScreenBackground = Color(0xFFF4F7FB)
private val CardBackground = Color(0xFFFFFFFF)
private val Primary = Color(0xFF00386C)
private val Secondary = Color(0xFF505F76)
private val TextMain = Color(0xFF1A1C20)
private val TextMuted = Color(0xFF667085)
private val Green = Color(0xFF12B76A)
private val Red = Color(0xFFBA1A1A)

@Composable
internal fun CashScreen(
    summary: CashSummary?,
    movements: List<CashMovementItem>,
) {
    val currency = rememberCurrency()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Caja", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Primary)
                Text("Entradas, salidas y balance", style = MaterialTheme.typography.bodyLarge, color = Secondary)
            }
        }

        if (summary != null) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CashCell("Entradas", currency.format(summary.totalIn), Green, Modifier.weight(1f))
                    CashCell("Salidas", currency.format(summary.totalOut), Red, Modifier.weight(1f))
                }
            }
            item {
                CashCell(
                    "Balance de caja",
                    currency.format(summary.balance),
                    if (summary.balance < 0) Red else Primary,
                    Modifier.fillMaxWidth(),
                )
            }
        }

        item {
            Text("Movimientos recientes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextMain)
        }

        if (movements.isEmpty()) {
            item { Text("No hay movimientos de caja.", style = MaterialTheme.typography.bodyMedium, color = TextMuted) }
        } else {
            items(movements, key = { it.id }) { movement ->
                MovementCard(movement = movement, amount = currency.format(movement.amount))
            }
        }
    }
}

@Composable
private fun CashCell(title: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextMuted)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = valueColor)
        }
    }
}

@Composable
private fun MovementCard(movement: CashMovementItem, amount: String) {
    val isIn = movement.direction == "in"
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(movement.description ?: movementTypeLabel(movement.type), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextMain, maxLines = 1)
                Text(listOfNotNull(movement.date, movementTypeLabel(movement.type)).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = TextMuted, maxLines = 1)
            }
            Text((if (isIn) "+" else "-") + amount, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (isIn) Green else Red)
        }
    }
}

private fun movementTypeLabel(type: String): String = when (type) {
    "loan_disbursement" -> "Desembolso"
    "payment_received" -> "Cobro"
    "expense" -> "Gasto"
    "collector_commission" -> "Comisión"
    "capital_injection" -> "Inyección de capital"
    "capital_withdrawal" -> "Retiro de capital"
    "adjustment" -> "Ajuste"
    else -> type
}
