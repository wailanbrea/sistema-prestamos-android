package com.sistemaprestamista.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.sistemaprestamista.mobile.data.model.AdminReportSummary
import com.sistemaprestamista.mobile.data.model.CollectorPerformanceRow
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
internal fun AdminReportsScreen(
    summary: AdminReportSummary?,
    collectors: List<CollectorPerformanceRow>,
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
                Text(
                    text = "Reportes",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                )
                Text(
                    text = "Resumen financiero del mes y rendimiento por cobrador",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Secondary,
                )
            }
        }

        if (summary != null) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCell("Capital en calle", currency.format(summary.capitalOnStreet), Primary, Modifier.weight(1f))
                    MetricCell("Capital invertido", currency.format(summary.capitalInvested), Primary, Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCell("Interés ganado", currency.format(summary.interestEarned), Green, Modifier.weight(1f))
                    MetricCell("Mora ganada", currency.format(summary.lateFeeEarned), Green, Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCell("Gastos", currency.format(summary.expenses), Red, Modifier.weight(1f))
                    MetricCell("Balance neto", currency.format(summary.netBalance), Primary, Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCell("ROI", "${summary.roi}%", Green, Modifier.weight(1f))
                    MetricCell("Rent. mensual", currency.format(summary.monthlyReturn), Primary, Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCell("Clientes activos", summary.activeClients.toString(), Primary, Modifier.weight(1f))
                    MetricCell("Atrasados", summary.overdueClients.toString(), Red, Modifier.weight(1f))
                }
            }
        }

        item {
            Text(
                text = "Rendimiento por cobrador",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextMain,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        if (collectors.isEmpty()) {
            item {
                Text(
                    text = "No hay actividad de cobradores en el período.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                )
            }
        } else {
            items(collectors, key = { it.collector }) { row ->
                CollectorPerformanceCard(
                    row = row,
                    collected = currency.format(row.collected),
                    disbursed = currency.format(row.disbursed),
                )
            }
        }
    }
}

@Composable
private fun MetricCell(
    title: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.height(96.dp),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                maxLines = 1,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CollectorPerformanceCard(
    row: CollectorPerformanceRow,
    collected: String,
    disbursed: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = row.collector,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextMain,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                LabeledValue("Cobrado", collected, Green)
                LabeledValue("Entregado", disbursed, Primary)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                LabeledValue("Cuentas activas", row.activeAccounts.toString(), TextMain)
                LabeledValue("Atrasadas", row.overdueAccounts.toString(), if (row.overdueAccounts > 0) Red else TextMain)
            }
        }
    }
}

@Composable
private fun LabeledValue(
    label: String,
    value: String,
    valueColor: Color,
) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = valueColor)
    }
}
