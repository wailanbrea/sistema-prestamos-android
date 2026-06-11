package com.sistemaprestamista.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sistemaprestamista.mobile.data.model.ClientDetail
import com.sistemaprestamista.mobile.data.model.ClientSummary
import com.sistemaprestamista.mobile.data.model.InstallmentSummary
import com.sistemaprestamista.mobile.data.model.LoanSummary
import com.sistemaprestamista.mobile.data.model.PaymentReceipt
import com.sistemaprestamista.mobile.ui.components.installmentFrequencyLabel
import com.sistemaprestamista.mobile.ui.components.rememberCurrency
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

private val ScreenBackground = Color(0xFFF8FAFC)
private val CardBackground = Color(0xFFFFFFFF)
private val Primary = Color(0xFF0F4C81)
private val PrimarySoft = Color(0xFFE3EDF7)
private val Success = Color(0xFF16A34A)
private val SuccessSoft = Color(0xFFDCFCE7)
private val Danger = Color(0xFFDC2626)
private val DangerSoft = Color(0xFFFEE2E2)
private val Warning = Color(0xFFD97706)
private val WarningSoft = Color(0xFFFEF3C7)
private val TextMain = Color(0xFF0F172A)
private val TextVariant = Color(0xFF64748B)
private val DividerColor = Color(0xFFE2E8F0)

@Composable
internal fun ClientDetailScreen(
    detail: ClientDetail?,
    isLoading: Boolean,
    fallbackClient: ClientSummary?,
    onOpenLoan: (Long) -> Unit,
    onOpenInstallment: (Long) -> Unit,
) {
    val client = detail?.summary ?: fallbackClient

    if (isLoading && detail == null && client == null) {
        ClientLoadingState()
        return
    }

    if (client == null) {
        ClientNotFoundState()
        return
    }

    val currency = rememberCurrency()
    val loans = detail?.loans.orEmpty()
    val installments = detail?.pendingInstallments.orEmpty()
    val recentPayments = detail?.recentPayments.orEmpty()
    val financial = detail?.financialSummary

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 14.dp,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ClientHeaderCard(
                client = client,
                detail = detail,
            )
        }

        if (isLoading && detail == null) {
            item {
                LoadingDetailCard()
            }
        }

        if (loans.isEmpty()) {
            item {
                SectionHeader(title = "Préstamo activo")
            }

            item {
                EmptySectionCard("Este cliente no tiene préstamos asignados al cobrador.")
            }
        } else {
            item {
                SectionHeader(title = if (loans.size > 1) "Préstamos activos" else "Préstamo activo")
            }

            items(loans, key = { it.id }) { loan ->
                ActiveLoanCard(
                    loan = loan,
                    currencyFormat = { currency.format(it) },
                    nextDueDate = installments
                        .filter { it.loanId == loan.id }
                        .mapNotNull { it.dueDate }
                        .minOrNull(),
                    onOpenLoan = onOpenLoan,
                )
            }
        }

        if (financial != null) {
            item {
                SectionHeader(title = "Estado financiero del cliente")
            }

            item {
                FinancialStatusCard(
                    balance = currency.format(financial.remainingBalance),
                    pendingPrincipal = currency.format(financial.pendingPrincipal),
                    pendingInterest = currency.format(financial.pendingInterest),
                    pendingInstallments = financial.pendingInstallments,
                    daysLate = financial.maxDaysLate,
                )
            }
        }

        item {
            SectionHeader(title = "Próximos vencimientos")
        }

        if (installments.isEmpty()) {
            item {
                EmptySectionCard("No hay cuotas pendientes para este cliente.")
            }
        } else {
            items(installments, key = { it.id }) { installment ->
                InstallmentCard(
                    installment = installment,
                    pendingAmount = currency.format(installment.pendingAmount),
                    onOpenInstallment = onOpenInstallment,
                )
            }
        }

        if (financial != null) {
            item {
                SectionHeader(title = "Resumen del cliente")
            }

            item {
                ClientSummaryCard(
                    activeLoans = financial.activeLoans,
                    lateLoans = financial.lateLoans,
                    totalPrincipal = currency.format(financial.totalPrincipal),
                    totalPaid = currency.format(financial.totalPaid),
                    lastPaymentDate = formatShortDate(financial.lastPaymentDate),
                )
            }
        }

        if (recentPayments.isNotEmpty()) {
            item {
                SectionHeader(title = "Últimos pagos")
            }

            item {
                RecentPaymentsCard(
                    payments = recentPayments,
                    formatAmount = { currency.format(it) },
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Encabezado del cliente
// ---------------------------------------------------------------------------

@Composable
private fun ClientHeaderCard(
    client: ClientSummary,
    detail: ClientDetail?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Nombre a ancho completo: nunca compite con badges ni acciones.
            Text(
                text = client.fullName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextMain,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!client.identification.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Fingerprint,
                            contentDescription = null,
                            tint = TextVariant,
                            modifier = Modifier.size(16.dp),
                        )

                        Text(
                            text = "Cliente ${client.identification}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                ClientRiskBadge(
                    status = client.status,
                    riskLevel = client.riskLevel,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(DividerColor),
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ClientInfoLine(icon = Icons.Outlined.Call, value = client.phone)
                ClientInfoLine(icon = Icons.Outlined.LocationOn, value = client.address)
                ClientInfoLine(icon = Icons.Outlined.Work, value = detail?.workplace)
                ClientInfoLine(icon = Icons.Outlined.Mail, value = detail?.email)
            }
        }
    }
}

@Composable
private fun ClientInfoLine(
    icon: ImageVector,
    value: String?,
) {
    if (value.isNullOrBlank()) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(PrimarySoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(17.dp),
            )
        }

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ClientRiskBadge(
    status: String,
    riskLevel: String,
) {
    val normalizedStatus = status.lowercase()
    val normalizedRisk = riskLevel.lowercase()

    val isDanger = normalizedStatus.contains("mora") ||
            normalizedStatus.contains("atras") ||
            normalizedStatus.contains("moroso") ||
            normalizedRisk.contains("alto") ||
            normalizedRisk.contains("high") ||
            normalizedRisk.contains("critical")

    val isPending = normalizedStatus.contains("pend") ||
            normalizedStatus.contains("inactivo") ||
            normalizedStatus.contains("inactive")

    val background = when {
        isDanger -> DangerSoft
        isPending -> PrimarySoft
        else -> SuccessSoft
    }

    val textColor = when {
        isDanger -> Danger
        isPending -> Primary
        else -> Success
    }

    StatusBadge(
        text = translateClientStatus(status),
        background = background,
        textColor = textColor,
        dotColor = textColor,
    )
}

// ---------------------------------------------------------------------------
// Préstamo activo (tarjeta principal)
// ---------------------------------------------------------------------------

@Composable
private fun ActiveLoanCard(
    loan: LoanSummary,
    currencyFormat: (Double) -> String,
    nextDueDate: String?,
    onOpenLoan: (Long) -> Unit,
) {
    val isDanger = loan.status.lowercase().let {
        it.contains("mora") || it.contains("late") || it.contains("atras")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenLoan(loan.id) },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Préstamo #${loan.loanNumber}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )

                StatusBadge(
                    text = translateLoanStatus(loan.status),
                    background = if (isDanger) DangerSoft else SuccessSoft,
                    textColor = if (isDanger) Danger else Success,
                    dotColor = if (isDanger) Danger else Success,
                )
            }

            // El balance pendiente es el dato protagonista de la tarjeta.
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Balance pendiente",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextVariant,
                )

                Text(
                    text = currencyFormat(loan.remainingBalance),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Primary,
                    maxLines = 1,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                DataBlock(
                    label = installmentFrequencyLabel(loan.paymentFrequency),
                    value = currencyFormat(loan.installmentAmount),
                    modifier = Modifier.weight(1f),
                )

                DataBlock(
                    label = "Próximo pago",
                    value = formatShortDate(nextDueDate) ?: "—",
                    modifier = Modifier.weight(1f),
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(DividerColor),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = "Ver préstamo",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                )

                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Estado financiero del cliente
// ---------------------------------------------------------------------------

@Composable
private fun FinancialStatusCard(
    balance: String,
    pendingPrincipal: String,
    pendingInterest: String,
    pendingInstallments: Int,
    daysLate: Int,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SummaryRow(label = "Balance pendiente", value = balance, valueColor = Primary, emphasized = true)
            SummaryRow(label = "Capital pendiente", value = pendingPrincipal)
            SummaryRow(label = "Interés pendiente", value = pendingInterest)
            SummaryRow(label = "Cuotas pendientes", value = pendingInstallments.toString())
            SummaryRow(
                label = "Días de atraso",
                value = if (daysLate > 0) "$daysLate días" else "Al día",
                valueColor = if (daysLate > 0) Danger else Success,
                emphasized = daysLate > 0,
            )
        }
    }
}

@Composable
private fun ClientSummaryCard(
    activeLoans: Int,
    lateLoans: Int,
    totalPrincipal: String,
    totalPaid: String,
    lastPaymentDate: String?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SummaryRow(label = "Préstamos activos", value = activeLoans.toString())
            SummaryRow(
                label = "Préstamos en mora",
                value = lateLoans.toString(),
                valueColor = if (lateLoans > 0) Danger else TextMain,
            )
            SummaryRow(label = "Total prestado", value = totalPrincipal)
            SummaryRow(label = "Total cobrado", value = totalPaid, valueColor = Success)
            SummaryRow(label = "Último pago", value = lastPaymentDate ?: "Sin pagos")
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    valueColor: Color = TextMain,
    emphasized: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextVariant,
        )

        Text(
            text = value,
            style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1,
        )
    }
}

// ---------------------------------------------------------------------------
// Próximos vencimientos
// ---------------------------------------------------------------------------

@Composable
private fun InstallmentCard(
    installment: InstallmentSummary,
    pendingAmount: String,
    onOpenInstallment: (Long) -> Unit,
) {
    val daysUntilDue = daysUntilDue(installment.dueDate)
    val isOverdue = installment.daysLate > 0 || (daysUntilDue != null && daysUntilDue < 0)
    val isUrgent = !isOverdue && daysUntilDue != null && daysUntilDue <= 3

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenInstallment(installment.id) },
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DateBadge(
                dueDate = installment.dueDate,
                isOverdue = isOverdue,
                isUrgent = isUrgent,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "Cuota #${installment.installmentNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextMain,
                )

                Text(
                    text = pendingAmount,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isOverdue) Danger else Primary,
                    maxLines = 1,
                )

                InstallmentUrgencyLabel(
                    installment = installment,
                    daysUntilDue = daysUntilDue,
                    isOverdue = isOverdue,
                    isUrgent = isUrgent,
                )
            }

            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = "Ver detalle",
                tint = TextVariant,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun InstallmentUrgencyLabel(
    installment: InstallmentSummary,
    daysUntilDue: Long?,
    isOverdue: Boolean,
    isUrgent: Boolean,
) {
    val (text, color) = when {
        isOverdue -> {
            val days = if (installment.daysLate > 0) installment.daysLate.toLong() else -(daysUntilDue ?: 0)
            "Vencida · $days ${if (days == 1L) "día" else "días"} de atraso" to Danger
        }

        isUrgent && daysUntilDue == 0L -> "Vence hoy" to Warning
        isUrgent -> "Vence en $daysUntilDue ${if (daysUntilDue == 1L) "día" else "días"}" to Warning
        else -> translateInstallmentStatus(installment.status) to TextVariant
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (isOverdue || isUrgent) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(13.dp),
            )
        }

        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isOverdue || isUrgent) FontWeight.Bold else FontWeight.Medium,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DateBadge(
    dueDate: String?,
    isOverdue: Boolean = false,
    isUrgent: Boolean = false,
) {
    val cleanDate = dueDate.orEmpty()
    val day = cleanDate.takeLast(2).ifBlank { "--" }
    val month = when {
        cleanDate.length >= 7 -> cleanDate.substring(5, 7).toMonthLabel()
        else -> "---"
    }

    val background = when {
        isOverdue -> DangerSoft
        isUrgent -> WarningSoft
        else -> PrimarySoft
    }

    val contentColor = when {
        isOverdue -> Danger
        isUrgent -> Warning
        else -> Primary
    }

    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = day,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = contentColor,
            )

            Text(
                text = month,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Últimos pagos
// ---------------------------------------------------------------------------

@Composable
private fun RecentPaymentsCard(
    payments: List<PaymentReceipt>,
    formatAmount: (Double) -> String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            payments.forEachIndexed { index, payment ->
                RecentPaymentRow(
                    payment = payment,
                    amount = formatAmount(payment.amount),
                    showDivider = index < payments.lastIndex,
                )
            }
        }
    }
}

@Composable
private fun RecentPaymentRow(
    payment: PaymentReceipt,
    amount: String,
    showDivider: Boolean,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(SuccessSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = Success,
                        modifier = Modifier.size(22.dp),
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "#${payment.receiptNumber}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextMain,
                    )

                    Text(
                        text = formatShortDate(payment.paymentDate) ?: "-",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextVariant,
                    )
                }
            }

            Text(
                text = amount,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Success,
            )
        }

        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(DividerColor),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Componentes compartidos de la pantalla
// ---------------------------------------------------------------------------

@Composable
private fun SectionHeader(
    title: String,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = TextMain,
    )
}

@Composable
private fun DataBlock(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextVariant,
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = TextMain,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StatusBadge(
    text: String,
    background: Color,
    textColor: Color,
    dotColor: Color,
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(background)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )

            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun LoadingDetailCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CircularProgressIndicator(
                color = Primary,
                modifier = Modifier.size(28.dp),
            )

            Column {
                Text(
                    text = "Cargando detalle del cliente...",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextMain,
                )

                Text(
                    text = "Estamos obteniendo el expediente actualizado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextVariant,
                )
            }
        }
    }
}

@Composable
private fun ClientLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CircularProgressIndicator(color = Primary)

                Text(
                    text = "Cargando expediente...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextMain,
                )

                Text(
                    text = "Estamos obteniendo los datos del cliente.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextVariant,
                )
            }
        }
    }
}

@Composable
private fun ClientNotFoundState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(DangerSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SearchOff,
                        contentDescription = null,
                        tint = Danger,
                        modifier = Modifier.size(36.dp),
                    )
                }

                Text(
                    text = "Cliente no encontrado",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextMain,
                )

                Text(
                    text = "No hemos podido localizar el expediente solicitado.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextVariant,
                )
            }
        }
    }
}

@Composable
private fun EmptySectionCard(
    message: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(18.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = TextVariant,
        )
    }
}

// ---------------------------------------------------------------------------
// Helpers de formato y traducción
// ---------------------------------------------------------------------------

private fun translateLoanStatus(status: String): String {
    return when (status.trim().lowercase()) {
        "active" -> "Activo"
        "late" -> "En mora"
        "completed", "paid" -> "Completado"
        "pending", "pending_approval" -> "Pendiente"
        "rejected" -> "Rechazado"
        "cancelled", "canceled" -> "Cancelado"
        else -> status.replaceFirstChar { it.uppercase() }
    }
}

private fun translateClientStatus(status: String): String {
    return when (status.trim().lowercase()) {
        "active" -> "Activo"
        "inactive" -> "Inactivo"
        "blocked" -> "Bloqueado"
        "late" -> "En mora"
        else -> status.replaceFirstChar { it.uppercase() }
    }
}

private fun translateInstallmentStatus(status: String): String {
    return when (status.trim().lowercase()) {
        "pending" -> "Pendiente"
        "partial" -> "Pago parcial"
        "late" -> "Atrasada"
        "paid" -> "Pagada"
        else -> status.replaceFirstChar { it.uppercase() }
    }
}

/** Días que faltan para la fecha (negativo si ya pasó); null si no se puede interpretar. */
private fun daysUntilDue(dueDate: String?): Long? {
    if (dueDate.isNullOrBlank()) return null

    return try {
        ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(dueDate.take(10)))
    } catch (_: DateTimeParseException) {
        null
    }
}

/** "2026-06-09" → "09 Jun 2026". Devuelve null si la fecha viene vacía. */
private fun formatShortDate(date: String?): String? {
    val clean = date.orEmpty().take(10)
    if (clean.length < 10) return date?.takeIf { it.isNotBlank() }

    val month = clean.substring(5, 7).toMonthLabel()
    return "${clean.substring(8, 10)} $month ${clean.substring(0, 4)}"
}

private fun String.toMonthLabel(): String {
    return when (this) {
        "01" -> "Ene"
        "02" -> "Feb"
        "03" -> "Mar"
        "04" -> "Abr"
        "05" -> "May"
        "06" -> "Jun"
        "07" -> "Jul"
        "08" -> "Ago"
        "09" -> "Sep"
        "10" -> "Oct"
        "11" -> "Nov"
        "12" -> "Dic"
        else -> "---"
    }
}
