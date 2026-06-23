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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sistemaprestamista.mobile.data.model.AllocationMode
import com.sistemaprestamista.mobile.data.model.InstallmentDetail
import com.sistemaprestamista.mobile.data.model.InstallmentSummary
import com.sistemaprestamista.mobile.data.model.PaymentMethod
import com.sistemaprestamista.mobile.ui.components.rememberCurrency
import java.util.Locale
import kotlin.math.min

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
private val Error = Color(0xFFBA1A1A)
private val ErrorContainer = Color(0xFFFFDAD6)
private val ErrorText = Color(0xFF93000A)
private val Success = Color(0xFF005236)
private val SuccessSoft = Color(0xFF6FFBBE)

@Composable
internal fun InstallmentDetailScreen(
    detail: InstallmentDetail?,
    fallbackInstallment: InstallmentSummary?,
    isLoading: Boolean,
    onRegisterPayment: (Long, String, String, String, Long?, Double?) -> Unit,
) {
    val installment = detail?.summary ?: fallbackInstallment

    if (isLoading && installment == null) {
        InstallmentLoadingState()
        return
    }

    if (installment == null) {
        InstallmentNotFoundState()
        return
    }

    var amount by remember(installment.id) {
        mutableStateOf("%.2f".format(Locale.US, installment.pendingAmount))
    }

    var paymentMethod by remember(installment.id) {
        mutableStateOf(PaymentMethod.Cash)
    }

    var allocationMode by remember(installment.id) {
        mutableStateOf(if (installment.pendingLateFee > 0) AllocationMode.Auto else AllocationMode.PrincipalAndInterest)
    }

    var showConfirmation by remember(installment.id) {
        mutableStateOf(false)
    }

    val currency = rememberCurrency()
    val parsedAmount = amount.toDoubleOrNull()
    val isLate = installment.daysLate > 0 && installment.status.trim().lowercase() !in setOf("paid", "cancelled")

    val amountError = when {
        amount.isBlank() -> "Indica el monto."
        parsedAmount == null -> "Monto inválido."
        parsedAmount <= 0 -> "Debe ser mayor que cero."
        parsedAmount > installment.pendingAmount -> "No puede exceder ${currency.format(installment.pendingAmount)}."
        else -> null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        InstallmentHeaderCard(
            installment = installment,
            isLate = isLate,
        )

        if (isLoading && detail == null) {
            LoadingDetailCard()
        }

        InstallmentMetricsGrid(
            capital = currency.format(installment.principalAmount),
            interest = currency.format(installment.interestAmount),
            lateFee = currency.format(installment.lateFee),
            pending = currency.format(installment.pendingAmount),
        )

        PaymentRegisterCard(
            amount = amount,
            amountError = amountError,
            paymentMethod = paymentMethod,
            allocationMode = allocationMode,
            pendingPrincipal = installment.pendingPrincipal,
            pendingInterest = installment.pendingInterest,
            pendingLateFee = installment.pendingLateFee,
            isLoading = isLoading,
            onAmountChange = { amount = it },
            onPaymentMethodChange = { paymentMethod = it },
            onAllocationModeChange = { allocationMode = it },
            onRegisterClick = { showConfirmation = true },
        )

        if (detail?.payments?.isNotEmpty() == true) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = CardDefaults.outlinedCardBorder(),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Pagos aplicados",
                        modifier = Modifier.padding(
                            start = 20.dp,
                            top = 18.dp,
                            end = 20.dp,
                            bottom = 4.dp,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextVariant,
                    )

                    detail.payments.forEachIndexed { index, payment ->
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
                                            .background(SuccessSoft.copy(alpha = 0.35f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.CheckCircle,
                                            contentDescription = null,
                                            tint = Success,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    }

                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(3.dp),
                                    ) {
                                        Text(
                                            text = "Recibo #${payment.receiptNumber ?: "-"}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextMain,
                                            maxLines = 1,
                                        )

                                        Text(
                                            text = payment.paymentDate ?: "-",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextVariant,
                                            maxLines = 1,
                                        )
                                    }
                                }

                                Text(
                                    text = currency.format(payment.amountPaid),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Primary,
                                    maxLines = 1,
                                )
                            }

                            if (index < detail.payments.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(OutlineVariant.copy(alpha = 0.35f)),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showConfirmation && parsedAmount != null) {
        InstallmentConfirmPaymentDialog(
            clientName = installment.client?.fullName ?: "Cliente sin nombre",
            amount = currency.format(parsedAmount),
            method = paymentMethod.label,
            onDismiss = { showConfirmation = false },
            onConfirm = {
                showConfirmation = false
                onRegisterPayment(
                    installment.loanId,
                    amount,
                    paymentMethod.apiValue,
                    allocationMode.apiValue,
                    installment.id,
                    null,
                )
            },
        )
    }
}

@Composable
private fun InstallmentHeaderCard(
    installment: InstallmentSummary,
    isLate: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = CardDefaults.outlinedCardBorder(),
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
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = installment.client?.fullName ?: "Cliente sin nombre",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Primary,
                        maxLines = 2,
                    )

                    Text(
                        text = "Préstamo #${installment.loanNumber}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextVariant,
                        maxLines = 1,
                    )
                }

                InstallmentStatusBadge(
                    isLate = isLate,
                    daysLate = installment.daysLate,
                    status = installment.status,
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(SecondaryContainer.copy(alpha = 0.45f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(18.dp),
                )

                Text(
                    text = "Cuota ${installment.installmentNumber}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                )
            }
        }
    }
}

@Composable
private fun InstallmentStatusBadge(
    isLate: Boolean,
    daysLate: Int,
    status: String,
) {
    val text = if (isLate) {
        "$daysLate días atraso"
    } else {
        when {
            status.isBlank() -> "Al día"
            status.lowercase().contains("pending") -> "Pendiente"
            status.lowercase().contains("pendiente") -> "Pendiente"
            status.lowercase().contains("paid") -> "Pagada"
            status.lowercase().contains("pagada") -> "Pagada"
            else -> status
        }
    }

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isLate) ErrorContainer else SuccessSoft)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isLate) ErrorText else Success,
            maxLines = 1,
        )
    }
}

@Composable
private fun InstallmentMetricsGrid(
    capital: String,
    interest: String,
    lateFee: String,
    pending: String,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            InstallmentMetricCard(
                title = "Capital",
                value = capital,
                valueColor = TextMain,
                modifier = Modifier.weight(1f),
            )

            InstallmentMetricCard(
                title = "Interés",
                value = interest,
                valueColor = TextMain,
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            InstallmentMetricCard(
                title = "Mora",
                value = lateFee,
                valueColor = Error,
                modifier = Modifier.weight(1f),
            )

            InstallmentMetricCard(
                title = "Pendiente",
                value = pending,
                valueColor = PrimaryFixed,
                containerColor = PrimaryContainer,
                labelColor = Color.White.copy(alpha = 0.72f),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun InstallmentMetricCard(
    title: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
    containerColor: Color = CardBackground,
    labelColor: Color = TextVariant,
) {
    Card(
        modifier = modifier.height(98.dp),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = if (containerColor == CardBackground) {
            CardDefaults.outlinedCardBorder()
        } else {
            null
        },
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
                color = labelColor,
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
private fun PaymentRegisterCard(
    amount: String,
    amountError: String?,
    paymentMethod: PaymentMethod,
    allocationMode: AllocationMode,
    pendingPrincipal: Double,
    pendingInterest: Double,
    pendingLateFee: Double,
    isLoading: Boolean,
    onAmountChange: (String) -> Unit,
    onPaymentMethodChange: (PaymentMethod) -> Unit,
    onAllocationModeChange: (AllocationMode) -> Unit,
    onRegisterClick: () -> Unit,
) {
    val parsedAmount = amount.toDoubleOrNull()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Monto a cobrar",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextVariant,
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = onAmountChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = amountError != null,
                    supportingText = {
                        amountError?.let {
                            Text(it)
                        }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.AttachMoney,
                            contentDescription = null,
                            tint = if (amountError != null) Error else Primary,
                        )
                    },
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Primary,
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(16.dp),
                )
            }

            InstallmentPaymentMethodSelector(
                selected = paymentMethod,
                onSelected = onPaymentMethodChange,
            )

            AllocationModeSelector(
                selected = allocationMode,
                hasLateFee = pendingLateFee > 0,
                onSelected = onAllocationModeChange,
            )

            parsedAmount?.let { amt ->
                PaymentBreakdownCard(
                    amount = amt,
                    mode = allocationMode,
                    pendingPrincipal = pendingPrincipal,
                    pendingInterest = pendingInterest,
                    pendingLateFee = pendingLateFee,
                )
            }

            Button(
                onClick = onRegisterClick,
                enabled = amountError == null && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryContainer,
                    contentColor = Color.White,
                    disabledContainerColor = SurfaceContainerHigh,
                    disabledContentColor = Outline,
                ),
            ) {
                Icon(
                    imageVector = Icons.Outlined.ReceiptLong,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )

                Spacer(Modifier.size(8.dp))

                Text(
                    text = if (isLoading) "Procesando..." else "Registrar cobro",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun InstallmentPaymentMethodSelector(
    selected: PaymentMethod,
    onSelected: (PaymentMethod) -> Unit,
) {
    val methods = PaymentMethod.entries

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Método de pago",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = TextVariant,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            methods.take(3).forEach { method ->
                PaymentMethodChip(
                    method = method,
                    selected = selected == method,
                    onClick = { onSelected(method) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (methods.size > 3) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                methods.drop(3).forEach { method ->
                    PaymentMethodWideButton(
                        method = method,
                        selected = selected == method,
                        onClick = { onSelected(method) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodChip(
    method: PaymentMethod,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(76.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) PrimaryFixed.copy(alpha = 0.45f) else CardBackground,
            contentColor = if (selected) Primary else TextVariant,
        ),
        border = ButtonDefaults.outlinedButtonBorder,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = method.toIcon(),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = method.shortLabel(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun PaymentMethodWideButton(
    method: PaymentMethod,
    selected: Boolean,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) PrimaryFixed.copy(alpha = 0.45f) else CardBackground,
            contentColor = if (selected) Primary else TextVariant,
        ),
    ) {
        Text(
            text = method.label,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun PaymentMethod.toIcon(): ImageVector {
    return when {
        label.lowercase().contains("efectivo") -> Icons.Outlined.Payments
        label.lowercase().contains("transfer") -> Icons.Outlined.AccountBalance
        else -> Icons.Outlined.MoreHoriz
    }
}

private fun PaymentMethod.shortLabel(): String {
    return when {
        label.lowercase().contains("efectivo") -> "Efectivo"
        label.lowercase().contains("transfer") -> "Transf."
        else -> label.take(8)
    }
}

@Composable
private fun InstallmentConfirmPaymentDialog(
    clientName: String,
    amount: String,
    method: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        shape = RoundedCornerShape(32.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(66.dp)
                    .clip(CircleShape)
                    .background(PrimaryFixed),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.TaskAlt,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(34.dp),
                )
            }
        },
        title = {
            Text(
                text = "Confirmar cobro",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextMain,
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text(
                    text = "Verifique los datos antes de procesar el pago.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextVariant,
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ConfirmInfoRow("Cliente", clientName)
                        ConfirmInfoRow("Monto", amount, valueColor = Primary)
                        ConfirmInfoRow("Método", method)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = Color.White,
                ),
            ) {
                Text(
                    text = "Confirmar pago",
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50.dp),
            ) {
                Text(
                    text = "Cancelar",
                    fontWeight = FontWeight.Bold,
                    color = Error,
                )
            }
        },
    )
}

@Composable
private fun ConfirmInfoRow(
    label: String,
    value: String,
    valueColor: Color = TextMain,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextVariant,
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor,
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
                    text = "Cargando detalle de la cuota...",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextMain,
                )

                Text(
                    text = "Estamos obteniendo la información actualizada.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextVariant,
                )
            }
        }
    }
}

@Composable
private fun InstallmentLoadingState() {
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
                    text = "Cargando cuota...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextMain,
                )

                Text(
                    text = "Estamos obteniendo los datos de la cuota.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextVariant,
                )
            }
        }
    }
}

@Composable
private fun InstallmentNotFoundState() {
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
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SearchOff,
                        contentDescription = null,
                        tint = Outline,
                        modifier = Modifier.size(40.dp),
                    )
                }

                Text(
                    text = "No se encontró la cuota",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextMain,
                )

                Text(
                    text = "La cuota seleccionada no está disponible o ha sido eliminada del sistema.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextVariant,
                )
            }
        }
    }
}