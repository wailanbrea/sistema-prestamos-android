package com.sistemaprestamista.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sistemaprestamista.mobile.data.model.CollectorCommissionItem
import com.sistemaprestamista.mobile.data.model.CollectorDetail

private val BgCollectorDetail = Color(0xFFF4F7FB)
private val PrimaryCD = Color(0xFF0F4C81)
private val PrimarySoftCD = Color(0xFFE3EDF7)
private val SuccessCD = Color(0xFF16A34A)
private val SuccessSoftCD = Color(0xFFDCFCE7)
private val TextMainCD = Color(0xFF0F172A)
private val TextVariantCD = Color(0xFF64748B)

@Composable
internal fun CollectorDetailScreen(
    detail: CollectorDetail?,
    isLoading: Boolean,
    isPayingCommission: Boolean,
    onEdit: () -> Unit,
    onPayCommission: (commissionId: Long) -> Unit,
) {
    if (isLoading && detail == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (detail == null) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("No se encontró el cobrador.", color = TextVariantCD)
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCollectorDetail),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(detail.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextMainCD)
                            val statusLabel = if (detail.status == "active") "Activo" else "Inactivo"
                            val statusColor = if (detail.status == "active") SuccessCD else TextVariantCD
                            Text(statusLabel, style = MaterialTheme.typography.bodySmall, color = statusColor)
                        }
                        FilledTonalButton(
                            onClick = onEdit,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Editar", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    if (!detail.phone.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Outlined.Phone, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextVariantCD)
                            Text(detail.phone, style = MaterialTheme.typography.bodySmall, color = TextVariantCD)
                        }
                    }

                    val commTypeLabel = when (detail.commissionType) {
                        "percentage" -> "Comisión: ${detail.commissionValue}% sobre ${commissionBaseLabel(detail.commissionBase)}"
                        "fixed" -> "Comisión fija: ${detail.commissionValue}"
                        else -> "Sin comisión"
                    }
                    Text(commTypeLabel, style = MaterialTheme.typography.bodySmall, color = TextVariantCD)
                }
            }
        }

        item {
            val summary = detail.commissionSummary
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Resumen de comisiones", style = MaterialTheme.typography.labelLarge, color = PrimaryCD)
                    Spacer(Modifier.height(2.dp))
                    CommissionSummaryRow("Total generado", summary.totalGenerated)
                    CommissionSummaryRow("Pendiente", summary.totalPending, highlight = summary.totalPending > 0)
                    CommissionSummaryRow("Pagado", summary.totalPaid)
                }
            }
        }

        if (detail.pendingCommissions.isNotEmpty()) {
            item {
                Text(
                    text = "Comisiones pendientes (${detail.pendingCommissions.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextMainCD,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            items(detail.pendingCommissions, key = { it.id }) { commission ->
                CommissionItemCard(
                    commission = commission,
                    isPayingCommission = isPayingCommission,
                    onPay = { onPayCommission(commission.id) },
                )
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                ) {
                    Text(
                        "No hay comisiones pendientes.",
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = SuccessCD,
                    )
                }
            }
        }
    }
}

@Composable
private fun CommissionSummaryRow(label: String, amount: Double, highlight: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextVariantCD)
        Text(
            "RD$ ${"%.2f".format(amount)}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
            color = if (highlight) Color(0xFFD97706) else TextMainCD,
        )
    }
}

@Composable
private fun CommissionItemCard(
    commission: CollectorCommissionItem,
    isPayingCommission: Boolean,
    onPay: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    val typeLabel = when (commission.commissionType) {
                        "percentage" -> "${commission.commissionValue}% s/ ${commission.baseAmount.formatted()}"
                        "fixed" -> "Fija"
                        else -> commission.commissionType
                    }
                    Text(typeLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextMainCD)
                    Text(
                        text = commission.receiptNumber?.let { "Recibo #$it" } ?: "Sin recibo",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextVariantCD,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "RD$ ${commission.commissionAmount.formatted()}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD97706),
                    )
                }
            }

            Button(
                onClick = onPay,
                enabled = !isPayingCommission,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SuccessCD, contentColor = Color.White),
            ) {
                if (isPayingCommission) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text("Pagar comisión", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

private fun commissionBaseLabel(base: String) = when (base) {
    "principal_paid" -> "capital"
    "interest_paid" -> "interés"
    else -> "total cobrado"
}

private fun Double.formatted() = "%.2f".format(this)
