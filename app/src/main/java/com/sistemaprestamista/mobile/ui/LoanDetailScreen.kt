package com.sistemaprestamista.mobile.ui

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sistemaprestamista.mobile.data.model.AllocationMode
import com.sistemaprestamista.mobile.data.model.InstallmentSummary
import com.sistemaprestamista.mobile.data.model.LoanDetail
import com.sistemaprestamista.mobile.data.model.LoanDocument
import com.sistemaprestamista.mobile.data.model.LoanSummary
import com.sistemaprestamista.mobile.data.model.PaymentMethod
import com.sistemaprestamista.mobile.data.model.PaymentReceipt
import com.sistemaprestamista.mobile.ui.components.formatPaymentFrequency
import com.sistemaprestamista.mobile.ui.components.rememberCurrency

private val ScreenBackground = Color(0xFFF4F7FB)
private val CardBackground = Color(0xFFFFFFFF)
private val Primary = Color(0xFF00386C)
private val PrimaryContainer = Color(0xFF1A4F8B)
private val Secondary = Color(0xFF505F76)
private val SecondaryContainer = Color(0xFFD0E1FB)
private val SurfaceContainerLow = Color(0xFFF3F3F9)
private val SurfaceContainerHigh = Color(0xFFE8E8ED)
private val TextMain = Color(0xFF1A1C20)
private val TextVariant = Color(0xFF424750)
private val Outline = Color(0xFF737781)
private val OutlineVariant = Color(0xFFC2C6D1)
private val Error = Color(0xFFBA1A1A)
private val ErrorContainer = Color(0xFFFFDAD6)
private val Success = Color(0xFF008A5C)
private val SuccessSoft = Color(0xFFE7F5ED)
private val Orange = Color(0xFFEA580C)

@Composable
internal fun LoanDetailScreen(
    detail: LoanDetail?,
    isLoading: Boolean,
    fallbackLoan: LoanSummary?,
    onOpenInstallment: (Long) -> Unit,
    onRegisterPayment: ((Long, String, String, String, Long?) -> Unit)? = null,
    isPaymentLoading: Boolean = false,
    onGenerateDocument: ((Long, String) -> Unit)? = null,
    isDocumentGenerating: Boolean = false,
    onEditLoan: (() -> Unit)? = null,
    onDeleteLoan: (() -> Unit)? = null,
    isDeletingLoan: Boolean = false,
) {
    val loan = detail?.summary ?: fallbackLoan

    if (isLoading && detail == null && loan == null) {
        LoanLoadingState()
        return
    }

    if (loan == null) {
        LoanNotFoundState()
        return
    }

    val currency = rememberCurrency()
    val installments = detail?.installments.orEmpty()
    val payments = detail?.payments.orEmpty()

    var showPaymentDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar préstamo") },
            text = { Text("¿Deseas eliminar el préstamo ${loan.loanNumber}? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = { showDeleteDialog = false; onDeleteLoan?.invoke() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                ) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") } },
        )
    }

    val canPay = onRegisterPayment != null &&
            loan.status.trim().lowercase() in setOf("active", "late")

    if (showPaymentDialog && onRegisterPayment != null) {
        RegisterPaymentDialog(
            loan = loan,
            isLoading = isPaymentLoading,
            onDismiss = { showPaymentDialog = false },
            onConfirm = { amountText, methodApiValue, allocationModeApiValue ->
                showPaymentDialog = false
                onRegisterPayment(loan.id, amountText, methodApiValue, allocationModeApiValue, null)
            },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground),
    ) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 20.dp,
            bottom = run {
                val fabCount = listOf(canPay, onEditLoan != null, onDeleteLoan != null).count { it }
                when (fabCount) {
                    0 -> 28.dp
                    1 -> 96.dp
                    2 -> 168.dp
                    else -> 240.dp
                }
            },
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            LoanHeaderCard(
                loan = loan,
                detail = detail,
            )
        }

        if (isLoading && detail == null) {
            item {
                LoadingDetailCard()
            }
        }

        item {
            LoanFinancialSummaryGrid(
                capital = currency.format(loan.principalAmount),
                balance = currency.format(loan.remainingBalance),
                installment = currency.format(loan.installmentAmount),
                paid = currency.format(detail?.financialSummary?.amountPaid ?: 0.0),
                total = currency.format(loan.totalAmount),
                late = (detail?.financialSummary?.installmentsLate ?: 0).toString(),
            )
        }

        item {
            SectionHeader(
                title = "Cuotas",
                action = if (installments.isNotEmpty()) "Ver calendario" else null,
            )
        }

        if (installments.isEmpty()) {
            item {
                EmptySectionCard("Aún no hay cuotas cargadas para este préstamo.")
            }
        } else {
            items(installments, key = { it.id }) { installment ->
                LoanInstallmentCard(
                    installment = installment,
                    pendingAmount = currency.format(installment.pendingAmount),
                    onOpenInstallment = onOpenInstallment,
                )
            }
        }

        if (detail != null && detail.documents.isNotEmpty()) {
            item {
                SectionHeader(title = "Documentos")
            }

            item {
                LoanDocumentsCard(
                    documents = detail.documents,
                    canGenerate = onGenerateDocument != null,
                    isGenerating = isDocumentGenerating,
                    onGenerate = { type -> onGenerateDocument?.invoke(loan.id, type) },
                )
            }
        }

        if (payments.isNotEmpty()) {
            item {
                SectionHeader(title = "Pagos relacionados")
            }

            item {
                LoanPaymentsCard(
                    payments = payments,
                    formatAmount = { currency.format(it) },
                )
            }
        }
    }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End,
        ) {
            if (onEditLoan != null) {
                ExtendedFloatingActionButton(
                    onClick = onEditLoan,
                    containerColor = Color(0xFF2E6DA4),
                    contentColor = Color.White,
                    icon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                    text = { Text("Editar préstamo", fontWeight = FontWeight.Bold) },
                )
            }

            if (onDeleteLoan != null) {
                ExtendedFloatingActionButton(
                    onClick = { showDeleteDialog = true },
                    containerColor = Color(0xFFDC2626),
                    contentColor = Color.White,
                    icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                    text = { Text("Eliminar préstamo", fontWeight = FontWeight.Bold) },
                )
            }

            if (canPay) {
                ExtendedFloatingActionButton(
                    onClick = { showPaymentDialog = true },
                    containerColor = PrimaryContainer,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                    text = { Text("Registrar pago", fontWeight = FontWeight.Bold) },
                )
            }
        }
    }
}

/**
 * Documentos legales del préstamo (contrato, pagaré, desembolso, etc.).
 * Espeja la sección de documentos del sistema web: cada tipo se puede
 * generar una vez y luego abrir/compartir su PDF.
 */
@Composable
private fun LoanDocumentsCard(
    documents: List<LoanDocument>,
    canGenerate: Boolean,
    isGenerating: Boolean,
    onGenerate: (String) -> Unit,
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            documents.forEachIndexed { index, document ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (document.generated) SuccessSoft else SurfaceContainerLow),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = null,
                            tint = if (document.generated) Success else Outline,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = document.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextMain,
                            maxLines = 1,
                        )

                        Text(
                            text = if (document.generated) {
                                "Generado · ${document.createdAt.orEmpty().take(10)}"
                            } else {
                                "No generado"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (document.generated) Success else TextVariant,
                        )
                    }

                    when {
                        document.downloadUrl != null -> {
                            TextButton(
                                onClick = {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(document.downloadUrl)),
                                    )
                                },
                            ) {
                                Text(
                                    text = "Abrir",
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryContainer,
                                )
                            }
                        }

                        canGenerate -> {
                            TextButton(
                                onClick = { onGenerate(document.documentType) },
                                enabled = !isGenerating,
                            ) {
                                Text(
                                    text = if (isGenerating) "Generando..." else "Generar",
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryContainer,
                                )
                            }
                        }
                    }
                }

                if (index < documents.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(OutlineVariant.copy(alpha = 0.45f)),
                    )
                }
            }
        }
    }
}

/**
 * Diálogo de cobro para el back-office: monto prellenado con la cuota del
 * préstamo y método de pago. La lógica de registro vive en el ViewModel.
 */
@Composable
private fun RegisterPaymentDialog(
    loan: LoanSummary,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit,
) {
    val currency = rememberCurrency()
    var amount by remember {
        mutableStateOf(
            when {
                loan.installmentAmount <= 0 -> ""
                loan.installmentAmount % 1.0 == 0.0 -> loan.installmentAmount.toLong().toString()
                else -> loan.installmentAmount.toString()
            },
        )
    }
    var method by remember { mutableStateOf(PaymentMethod.Cash) }
    var allocationMode by remember { mutableStateOf(AllocationMode.PrincipalAndInterest) }

    val parsedAmount = amount.toDoubleOrNull()
    val amountError = when {
        amount.isBlank() -> null
        parsedAmount == null || parsedAmount <= 0 -> "Indica un monto válido."
        else -> null
    }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Registrar pago") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = buildString {
                        append("Préstamo #${loan.loanNumber}")
                        loan.client?.fullName?.let { append(" · ").append(it) }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextMain,
                )

                Text(
                    text = "Balance pendiente: ${currency.format(loan.remainingBalance)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextVariant,
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Monto a cobrar") },
                    singleLine = true,
                    isError = amountError != null,
                    supportingText = {
                        amountError?.let { Text(it) }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.AttachMoney,
                            contentDescription = null,
                            tint = if (amountError != null) Error else TextVariant,
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp),
                )

                PaymentMethodSelector(
                    selected = method,
                    onSelected = { method = it },
                )

                AllocationModeSelector(
                    selected = allocationMode,
                    hasLateFee = false,
                    onSelected = { allocationMode = it },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(amount, method.apiValue, allocationMode.apiValue) },
                enabled = !isLoading && parsedAmount != null && parsedAmount > 0,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryContainer),
            ) {
                Text(if (isLoading) "Procesando..." else "Cobrar")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading,
            ) {
                Text("Cancelar")
            }
        },
    )
}

@Composable
private fun LoanHeaderCard(
    loan: LoanSummary,
    detail: LoanDetail?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryContainer),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(130.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f)),
            )

            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
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
                            text = "PRÉSTAMO NO.",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.72f),
                        )

                        Text(
                            text = "#${loan.loanNumber}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }

                    LoanStatusBadge(
                        text = loan.status,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    Column {
                        Text(
                            text = loan.client?.fullName ?: "Cliente no disponible",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                        )

                        Text(
                            text = "Cliente verificado",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.70f),
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.12f)),
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        LoanHeaderInfoItem(
                            icon = Icons.Outlined.Payments,
                            label = "Frecuencia",
                            value = formatPaymentFrequency(loan.paymentFrequency),
                            modifier = Modifier.weight(1f),
                        )

                        LoanHeaderInfoItem(
                            icon = Icons.Outlined.Percent,
                            label = "Tasa de interés",
                            value = detail?.let { "${it.interestRate}%" } ?: "-",
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        LoanHeaderInfoItem(
                            icon = Icons.Outlined.CalendarToday,
                            label = "Inicio",
                            value = detail?.startDate ?: "-",
                            modifier = Modifier.weight(1f),
                        )

                        LoanHeaderInfoItem(
                            icon = Icons.Outlined.EventRepeat,
                            label = "Primer pago",
                            value = detail?.firstPaymentDate ?: "-",
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoanHeaderInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.70f),
                modifier = Modifier.size(16.dp),
            )

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.70f),
            )
        }

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
        )
    }
}

@Composable
private fun LoanStatusBadge(
    text: String,
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.lowercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
        )
    }
}

@Composable
private fun LoanFinancialSummaryGrid(
    capital: String,
    balance: String,
    installment: String,
    paid: String,
    total: String,
    late: String,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            LoanMetricCard(
                title = "Capital",
                value = capital,
                valueColor = Primary,
                accentColor = Primary,
                modifier = Modifier.weight(1f),
            )

            LoanMetricCard(
                title = "Balance",
                value = balance,
                valueColor = Error,
                accentColor = Error,
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            LoanMetricCard(
                title = "Cuota",
                value = installment,
                valueColor = PrimaryContainer,
                accentColor = null,
                modifier = Modifier.weight(1f),
            )

            LoanMetricCard(
                title = "Pagado",
                value = paid,
                valueColor = Success,
                accentColor = null,
                modifier = Modifier.weight(1f),
            )
        }

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
                Column {
                    Text(
                        text = "Total del préstamo",
                        style = MaterialTheme.typography.labelSmall,
                        color = Secondary,
                    )

                    Text(
                        text = total,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Secondary,
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        text = "Atrasos",
                        style = MaterialTheme.typography.labelSmall,
                        color = Error,
                    )

                    Text(
                        text = late,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Error,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoanMetricCard(
    title: String,
    value: String,
    valueColor: Color,
    accentColor: Color?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.height(104.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
        ) {
            if (accentColor != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(0.05f)
                        .background(accentColor),
                )

                Column(
                    modifier = Modifier
                        .weight(0.95f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    MetricText(title = title, value = value, valueColor = valueColor)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    MetricText(title = title, value = value, valueColor = valueColor)
                }
            }
        }
    }
}

@Composable
private fun MetricText(
    title: String,
    value: String,
    valueColor: Color,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = Secondary,
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
            color = TextMain,
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
private fun LoanInstallmentCard(
    installment: InstallmentSummary,
    pendingAmount: String,
    onOpenInstallment: (Long) -> Unit,
) {
    val isLate = installment.daysLate > 0
    val installmentNumber = installment.installmentNumber ?: 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isLate) ErrorContainer else SurfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = installmentNumber.toString().padStart(2, '0'),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isLate) Error else Primary,
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = installment.dueDate ?: "-",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextMain,
                    )

                    Text(
                        text = if (isLate) {
                            "Vencida hace ${installment.daysLate} días"
                        } else {
                            "$pendingAmount pendiente"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isLate) Error else Secondary,
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                InstallmentStatusBadge(
                    text = if (isLate) "atrasado" else installment.status,
                    isLate = isLate,
                )

                OutlinedButton(
                    onClick = { onOpenInstallment(installment.id) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Primary,
                    ),
                    border = null,
                ) {
                    Text(
                        text = "Ver cuota",
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(Modifier.width(4.dp))

                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun InstallmentStatusBadge(
    text: String,
    isLate: Boolean,
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isLate) ErrorContainer else SecondaryContainer)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isLate) Error else Secondary,
            maxLines = 1,
        )
    }
}

@Composable
private fun LoanPaymentsCard(
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
                LoanPaymentRow(
                    payment = payment,
                    amount = formatAmount(payment.amount),
                    showDivider = index < payments.lastIndex,
                )
            }
        }
    }
}

@Composable
private fun LoanPaymentRow(
    payment: PaymentReceipt,
    amount: String,
    showDivider: Boolean,
) {
    val isCancelled = payment.status.equals("cancelled", ignoreCase = true) ||
            payment.status.equals("anulado", ignoreCase = true)

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                        .background(if (isCancelled) SurfaceContainerHigh else SuccessSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ReceiptLong,
                        contentDescription = null,
                        tint = if (isCancelled) Secondary else Success,
                        modifier = Modifier.size(22.dp),
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = "Recibo #${payment.receiptNumber}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextMain,
                    )

                    Text(
                        text = payment.paymentDate ?: "-",
                        style = MaterialTheme.typography.labelSmall,
                        color = Secondary,
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
                    color = if (isCancelled) Secondary else Primary,
                )

                Text(
                    text = payment.status.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isCancelled) Error else Success,
                    maxLines = 1,
                )
            }
        }

        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(SurfaceContainerLow),
            )
        }
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CircularProgressIndicator(
                color = Primary,
            )

            Text(
                text = "Cargando detalle del préstamo...",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Secondary,
            )
        }
    }
}

@Composable
private fun LoanLoadingState() {
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
                    text = "Cargando préstamo...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextMain,
                )

                Text(
                    text = "Estamos obteniendo los datos del préstamo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextVariant,
                )
            }
        }
    }
}

@Composable
private fun LoanNotFoundState() {
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
                        .background(SurfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SearchOff,
                        contentDescription = null,
                        tint = Outline,
                        modifier = Modifier.size(38.dp),
                    )
                }

                Text(
                    text = "Préstamo no encontrado",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextMain,
                )

                Text(
                    text = "No se encontró el préstamo seleccionado.",
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