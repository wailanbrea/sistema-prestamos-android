package com.sistemaprestamista.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sistemaprestamista.mobile.data.model.DashboardSummary
import com.sistemaprestamista.mobile.data.model.PaymentReceipt
import com.sistemaprestamista.mobile.ui.components.rememberCurrency

private val AppBackground = Color(0xFFF4F7FB)
private val CardWhite = Color(0xFFFFFFFF)
private val PrimaryBlue = Color(0xFF155EEF)
private val DeepBlue = Color(0xFF0A2540)
private val SoftBlue = Color(0xFFEAF1FF)
private val Green = Color(0xFF12B76A)
private val SoftGreen = Color(0xFFE8F8F0)
private val Red = Color(0xFFF04438)
private val SoftRed = Color(0xFFFFECEA)
private val Amber = Color(0xFFF79009)
private val TextDark = Color(0xFF101828)
private val TextMuted = Color(0xFF667085)
private val BorderSoft = Color(0xFFE4E7EC)

@Composable
internal fun HomeScreen(
    state: AppUiState,
    onOpenReceipt: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground),
        contentPadding = PaddingValues(
            start = 18.dp,
            end = 18.dp,
            top = 18.dp,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            WelcomeCard(state)
        }

        state.dashboard?.let { dashboard ->
            item {
                FinanceOverview(dashboard)
            }
        }

        item {
            CollectorOverview(state)
        }

        item {
            LastReceiptModernCard(
                payments = state.paymentHistory,
                onOpenReceipt = onOpenReceipt,
            )
        }

        item {
            PaymentHistoryPreview(state.paymentHistory)
        }
    }
}

@Composable
private fun WelcomeCard(state: AppUiState) {
    val user = state.user ?: return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = DeepBlue),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            DeepBlue,
                            PrimaryBlue,
                        )
                    )
                )
                .padding(20.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "Hola, ${user.name}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )

                        Text(
                            text = user.roles.joinToString().ifBlank { "Sin rol asignado" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.82f),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Security,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Green),
                    )

                    Text(
                        text = "Activo hoy",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.88f),
                    )
                }
            }
        }
    }
}

@Composable
private fun FinanceOverview(summary: DashboardSummary) {
    val currency = rememberCurrency()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(
            title = "Resumen financiero",
            subtitle = "Estado general de la operación",
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCardModern(
                title = "Capital prestado",
                value = currency.format(summary.capitalPrestado),
                icon = Icons.Outlined.AccountBalance,
                iconColor = PrimaryBlue,
                iconBackground = SoftBlue,
                modifier = Modifier.weight(1f),
            )

            MetricCardModern(
                title = "Cobros hoy",
                value = currency.format(summary.cobrosHoy),
                icon = Icons.Outlined.Payments,
                iconColor = Green,
                iconBackground = SoftGreen,
                valueColor = Green,
                modifier = Modifier.weight(1f),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCardModern(
                title = "Ganancia neta",
                value = currency.format(summary.gananciaNeta),
                icon = Icons.Outlined.TrendingUp,
                iconColor = PrimaryBlue,
                iconBackground = SoftBlue,
                modifier = Modifier.weight(1f),
            )

            MetricCardModern(
                title = "Préstamos mora",
                value = "${summary.prestamosMora}",
                icon = Icons.Outlined.Warning,
                iconColor = Red,
                iconBackground = SoftRed,
                valueColor = if (summary.prestamosMora > 0) Red else TextDark,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CollectorOverview(state: AppUiState) {
    val summary = state.collectorSummary ?: return
    val currency = rememberCurrency()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionHeader(
                title = "Ruta de cobro",
                subtitle = "Actividad asignada al cobrador",
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RouteMetricCard(
                    title = "Clientes",
                    value = summary.assignedClients.toString(),
                    icon = Icons.Outlined.Groups,
                    modifier = Modifier.weight(1f),
                )

                RouteMetricCard(
                    title = "Cuotas",
                    value = summary.pendingInstallments.toString(),
                    icon = Icons.Outlined.PendingActions,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RouteMetricCard(
                    title = "Atrasos",
                    value = summary.lateLoans.toString(),
                    icon = Icons.Outlined.Warning,
                    valueColor = if (summary.lateLoans > 0) Red else TextDark,
                    iconColor = if (summary.lateLoans > 0) Red else Amber,
                    iconBackground = if (summary.lateLoans > 0) SoftRed else Color(0xFFFFF4E5),
                    modifier = Modifier.weight(1f),
                )

                RouteMetricCard(
                    title = "Hoy",
                    value = currency.format(summary.collectedToday),
                    icon = Icons.Outlined.Payments,
                    valueColor = Green,
                    iconColor = Green,
                    iconBackground = SoftGreen,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RouteMetricCard(
                    title = "Comision",
                    value = currency.format(summary.commissionGeneratedTotal),
                    icon = Icons.Outlined.TrendingUp,
                    valueColor = PrimaryBlue,
                    iconColor = PrimaryBlue,
                    iconBackground = SoftBlue,
                    modifier = Modifier.weight(1f),
                )

                RouteMetricCard(
                    title = "Pendiente",
                    value = currency.format(summary.commissionPendingTotal),
                    icon = Icons.Outlined.PendingActions,
                    valueColor = Amber,
                    iconColor = Amber,
                    iconBackground = Color(0xFFFFF4E5),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun LastReceiptModernCard(
    payments: List<PaymentReceipt>,
    onOpenReceipt: () -> Unit,
) {
    val lastPayment = payments.firstOrNull()
    val currency = rememberCurrency()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(
            title = "Último recibo",
            subtitle = "Pago más reciente registrado",
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            border = CardDefaults.outlinedCardBorder(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SoftBlue),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(26.dp),
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = lastPayment?.client?.fullName ?: "Sin recibos registrados",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            maxLines = 1,
                        )

                        Text(
                            text = if (lastPayment != null) {
                                val commission = currency.format(lastPayment.commission?.commissionAmount ?: 0.0)
                                "${lastPayment.paymentDate ?: "-"} · ${currency.format(lastPayment.amount)} · Comision $commission"
                            } else {
                                "Cuando registres un pago aparecerá aquí"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            maxLines = 1,
                        )
                    }
                }

                TextButton(
                    onClick = onOpenReceipt,
                    enabled = lastPayment != null,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        text = "Ver",
                        color = if (lastPayment != null) PrimaryBlue else TextMuted,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentHistoryPreview(payments: List<PaymentReceipt>) {
    val currency = rememberCurrency()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(
            title = "Historial reciente",
            subtitle = "Últimos pagos recibidos",
        )

        if (payments.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                border = CardDefaults.outlinedCardBorder(),
            ) {
                Text(
                    text = "No hay pagos recientes.",
                    modifier = Modifier.padding(18.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                )
            }
            return
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            border = CardDefaults.outlinedCardBorder(),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
                payments.take(5).forEachIndexed { index, payment ->
                    PaymentHistoryRow(
                        payment = payment,
                        amount = currency.format(payment.amount),
                        commissionAmount = currency.format(payment.commission?.commissionAmount ?: 0.0),
                        showDivider = index < payments.take(5).lastIndex,
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentHistoryRow(
    payment: PaymentReceipt,
    amount: String,
    commissionAmount: String,
    showDivider: Boolean,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(SoftGreen),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = Green,
                        modifier = Modifier.size(24.dp),
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = payment.client?.fullName ?: "Cliente no disponible",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark,
                        maxLines = 1,
                    )

                    Text(
                        text = "${payment.receiptNumber} · ${payment.paymentDate ?: "-"} · Comision $commissionAmount",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        maxLines = 1,
                    )
                }
            }

            Text(
                text = "+$amount",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Green,
            )
        }

        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(BorderSoft),
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextDark,
        )

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
        )
    }
}

@Composable
private fun MetricCardModern(
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    iconBackground: Color,
    modifier: Modifier = Modifier,
    valueColor: Color = TextDark,
) {
    Card(
        modifier = modifier.height(132.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(15.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
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
}

@Composable
private fun RouteMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    valueColor: Color = TextDark,
    iconColor: Color = PrimaryBlue,
    iconBackground: Color = SoftBlue,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFBFC)),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
}
