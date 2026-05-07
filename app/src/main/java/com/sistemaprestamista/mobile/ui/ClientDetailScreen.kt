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
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sistemaprestamista.mobile.data.model.ClientDetail
import com.sistemaprestamista.mobile.data.model.ClientSummary
import com.sistemaprestamista.mobile.data.model.InstallmentSummary
import com.sistemaprestamista.mobile.data.model.LoanSummary
import com.sistemaprestamista.mobile.data.model.PaymentReceipt
import com.sistemaprestamista.mobile.ui.components.rememberCurrency

private val ScreenBackground = Color(0xFFF4F7FB)
private val CardBackground = Color(0xFFFFFFFF)
private val Primary = Color(0xFF00386C)
private val PrimaryContainer = Color(0xFF1A4F8B)
private val PrimaryFixed = Color(0xFFD5E3FF)
private val Secondary = Color(0xFF505F76)
private val SecondaryContainer = Color(0xFFD0E1FB)
private val SurfaceContainer = Color(0xFFEDEDF3)
private val SurfaceContainerLow = Color(0xFFF3F3F9)
private val SurfaceContainerHigh = Color(0xFFE8E8ED)
private val TextMain = Color(0xFF1A1C20)
private val TextVariant = Color(0xFF424750)
private val Outline = Color(0xFF737781)
private val OutlineVariant = Color(0xFFC2C6D1)
private val Success = Color(0xFF005236)
private val SuccessSoft = Color(0xFF6FFBBE)
private val Error = Color(0xFFBA1A1A)
private val ErrorContainer = Color(0xFFFFDAD6)
private val Orange = Color(0xFFEA580C)

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

    val activeBalance = detail?.financialSummary?.remainingBalance
        ?: loans.sumOf { it.remainingBalance }

    val pendingAmount = installments.sumOf { it.pendingAmount }

    val totalPaid = detail?.financialSummary?.totalPaid ?: 0.0
    val lateInstallments = detail?.financialSummary?.lateInstallments ?: 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 20.dp,
            bottom = 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
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

        item {
            FinancialSummaryGrid(
                balance = currency.format(activeBalance),
                pending = currency.format(pendingAmount),
                paid = currency.format(totalPaid),
                moras = lateInstallments.toString(),
            )
        }

        item {
            SectionHeader(
                title = "Préstamos activos",
                action = if (loans.isNotEmpty()) "Ver todos" else null,
            )
        }

        if (loans.isEmpty()) {
            item {
                EmptySectionCard("Este cliente no tiene préstamos asignados al cobrador.")
            }
        } else {
            items(loans, key = { it.id }) { loan ->
                LoanCard(
                    loan = loan,
                    installmentAmount = currency.format(loan.installmentAmount),
                    remainingBalance = currency.format(loan.remainingBalance),
                    onOpenLoan = onOpenLoan,
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

@Composable
private fun ClientHeaderCard(
    client: ClientSummary,
    detail: ClientDetail?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = client.fullName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Primary,
                        maxLines = 2,
                    )

                    if (!client.identification.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Fingerprint,
                                contentDescription = null,
                                tint = TextVariant,
                                modifier = Modifier.size(18.dp),
                            )

                            Text(
                                text = "ID: ${client.identification}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextVariant,
                            )
                        }
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
                    .background(OutlineVariant.copy(alpha = 0.45f)),
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ClientInfoLine(
                    icon = Icons.Outlined.Call,
                    value = client.phone,
                )

                ClientInfoLine(
                    icon = Icons.Outlined.LocationOn,
                    value = client.address,
                )

                ClientInfoLine(
                    icon = Icons.Outlined.Work,
                    value = detail?.workplace,
                )

                ClientInfoLine(
                    icon = Icons.Outlined.Mail,
                    value = detail?.email,
                )
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
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(SurfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Secondary,
                modifier = Modifier.size(20.dp),
            )
        }

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextVariant,
            maxLines = 2,
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
        isDanger -> ErrorContainer
        isPending -> SecondaryContainer
        else -> SuccessSoft
    }

    val textColor = when {
        isDanger -> Color(0xFF93000A)
        isPending -> Secondary
        else -> Success
    }

    val dotColor = when {
        isDanger -> Error
        isPending -> Primary
        else -> Success
    }

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(background)
            .padding(horizontal = 10.dp, vertical = 6.dp),
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
                text = buildString {
                    append(status.replaceFirstChar { it.uppercase() })
                    if (riskLevel.isNotBlank()) {
                        append(" · riesgo ")
                        append(riskLevel.lowercase())
                    }
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun FinancialSummaryGrid(
    balance: String,
    pending: String,
    paid: String,
    moras: String,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            FinancialMetricCard(
                title = "Balance",
                value = balance,
                valueColor = Primary,
                modifier = Modifier.weight(1f),
            )

            FinancialMetricCard(
                title = "Pendiente",
                value = pending,
                valueColor = Orange,
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            FinancialMetricCard(
                title = "Pagado",
                value = paid,
                valueColor = Success,
                modifier = Modifier.weight(1f),
            )

            FinancialMetricCard(
                title = "Moras",
                value = moras,
                valueColor = Error,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun FinancialMetricCard(
    title: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.height(96.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = TextVariant,
            )

            Spacer(Modifier.height(4.dp))

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
private fun SectionHeader(
    title: String,
    action: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Primary,
        )

        if (action != null) {
            Text(
                text = action,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Primary,
            )
        }
    }
}

@Composable
private fun LoanCard(
    loan: LoanSummary,
    installmentAmount: String,
    remainingBalance: String,
    onOpenLoan: (Long) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "#${loan.loanNumber}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Secondary,
                    )

                    Text(
                        text = "Préstamo activo",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextMain,
                    )
                }

                GenericStatusBadge(
                    text = loan.status,
                    isDanger = loan.status.lowercase().contains("mora") ||
                            loan.status.lowercase().contains("late") ||
                            loan.status.lowercase().contains("atras"),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                DataBlock(
                    label = "Cuota",
                    value = installmentAmount,
                    modifier = Modifier.weight(1f),
                )

                DataBlock(
                    label = "Balance",
                    value = remainingBalance,
                    modifier = Modifier.weight(1f),
                )
            }

            DataBlock(
                label = "Frecuencia",
                value = loan.paymentFrequency,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = { onOpenLoan(loan.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = Color.White,
                ),
            ) {
                Text(
                    text = "Ver préstamo",
                    fontWeight = FontWeight.Bold,
                )

                Spacer(Modifier.size(8.dp))

                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun DataBlock(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextVariant,
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = TextMain,
            maxLines = 1,
        )
    }
}

@Composable
private fun InstallmentCard(
    installment: InstallmentSummary,
    pendingAmount: String,
    onOpenInstallment: (Long) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                DateBadge(
                    dueDate = installment.dueDate,
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = "Cuota #${installment.installmentNumber}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextMain,
                    )

                    Text(
                        text = "$pendingAmount · ${if (installment.daysLate > 0) "${installment.daysLate} días atraso" else installment.status}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextVariant,
                        maxLines = 1,
                    )
                }
            }

            OutlinedButton(
                onClick = { onOpenInstallment(installment.id) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Primary,
                    containerColor = SurfaceContainerHigh,
                ),
                border = null,
            ) {
                Text(
                    text = "Ver cuota",
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun DateBadge(
    dueDate: String?,
) {
    val cleanDate = dueDate.orEmpty()
    val day = cleanDate.takeLast(2).ifBlank { "--" }
    val month = when {
        cleanDate.length >= 7 -> cleanDate.substring(5, 7).toMonthLabel()
        else -> "---"
    }

    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SecondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = day,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Primary,
            )

            Text(
                text = month,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Primary,
            )
        }
    }
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

@Composable
private fun RecentPaymentsCard(
    payments: List<PaymentReceipt>,
    formatAmount: (Double) -> String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
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
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Success),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = SuccessSoft,
                        modifier = Modifier.size(24.dp),
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = "#${payment.receiptNumber}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextMain,
                    )

                    Text(
                        text = payment.paymentDate ?: "-",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextVariant,
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = amount,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Success,
                )

                SmallStatusBadge(payment.status)
            }
        }

        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(OutlineVariant.copy(alpha = 0.45f)),
            )
        }
    }
}

@Composable
private fun GenericStatusBadge(
    text: String,
    isDanger: Boolean,
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isDanger) ErrorContainer else Success)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isDanger) Error else SuccessSoft,
            maxLines = 1,
        )
    }
}

@Composable
private fun SmallStatusBadge(
    text: String,
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(SecondaryContainer)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Secondary,
            maxLines = 1,
        )
    }
}

@Composable
private fun LoadingDetailCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
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
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
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
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
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
                        .background(ErrorContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SearchOff,
                        contentDescription = null,
                        tint = Error,
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
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(20.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = TextVariant,
        )
    }
}