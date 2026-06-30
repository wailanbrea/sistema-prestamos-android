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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import com.sistemaprestamista.mobile.data.model.AllocationMode
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
internal fun CollectionsScreen(
    state: AppUiState,
    onRegisterPayment: (Long, String, String, String, Long?, Double?) -> Unit,
    onOpenInstallment: (Long) -> Unit,
) {
    val installments = state.collectorInstallments

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 22.dp,
            bottom = 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            CollectionsHeader(
                pendingCount = installments.size,
            )
        }

        if (installments.isEmpty()) {
            item {
                EmptyCollectionsState()
            }
        } else {
            items(installments, key = { it.id }) { installment ->
                val loanRemainingBalance = state.collectorLoans
                    .firstOrNull { it.id == installment.loanId }
                    ?.remainingBalance
                CollectionInstallmentCard(
                    installment = installment,
                    isLoading = state.isLoading,
                    loanRemainingBalance = loanRemainingBalance,
                    onRegisterPayment = onRegisterPayment,
                    onOpenInstallment = onOpenInstallment,
                )
            }
        }
    }
}

@Composable
private fun CollectionsHeader(
    pendingCount: Int,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Cobros",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Primary,
        )

        Text(
            text = "Registra pagos de clientes",
            style = MaterialTheme.typography.bodyLarge,
            color = TextVariant,
        )

        if (pendingCount > 0) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(PrimaryFixed)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(16.dp),
                )

                Text(
                    text = "$pendingCount cuotas pendientes hoy",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                )
            }
        }
    }
}

@Composable
private fun CollectionInstallmentCard(
    installment: InstallmentSummary,
    isLoading: Boolean,
    loanRemainingBalance: Double?,
    onRegisterPayment: (Long, String, String, String, Long?, Double?) -> Unit,
    onOpenInstallment: (Long) -> Unit,
) {
    var amount by remember(installment.id) {
        mutableStateOf("%.2f".format(Locale.US, installment.pendingAmount))
    }

    var capitalText by remember(installment.id) {
        mutableStateOf("")
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
    val isLate = installment.daysLate > 0 && installment.status.trim().lowercase() !in setOf("paid", "cancelled")
    val isCapitalMode = allocationMode == AllocationMode.CurrentPlusCapital

    // En "Cuota + capital" la base es la cuota completa; el cobrador solo indica el abono.
    val cuotaBase = installment.pendingAmount
    val parsedCapital = capitalText.toDoubleOrNull()
    // Tope local del abono: lo que queda del préstamo después de cubrir esta cuota.
    // El backend valida el capital exacto re-amortizable; esto evita 422 obvios.
    val capitalCap = loanRemainingBalance?.let { (it - cuotaBase).coerceAtLeast(0.0) }

    val parsedAmount = if (isCapitalMode) {
        parsedCapital?.let { cuotaBase + it }
    } else {
        amount.toDoubleOrNull()
    }

    val capitalError = when {
        !isCapitalMode -> null
        capitalText.isBlank() || parsedCapital == null -> "Indica el abono a capital."
        parsedCapital <= 0 -> "El abono debe ser mayor que cero."
        capitalCap != null && parsedCapital > capitalCap -> "No puede exceder ${currency.format(capitalCap)}."
        else -> null
    }

    val amountError = when {
        isCapitalMode -> capitalError
        amount.isBlank() -> "Indica el monto."
        parsedAmount == null -> "Monto inválido."
        parsedAmount <= 0 -> "Debe ser mayor que cero."
        parsedAmount > installment.pendingAmount -> "No puede exceder ${currency.format(installment.pendingAmount)}."
        else -> null
    }

    val canSubmit = amountError == null && !isLoading

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
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextMain,
                        maxLines = 2,
                    )

                    Text(
                        text = "${installment.loanNumber} · Cuota #${installment.installmentNumber}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextVariant,
                    )
                }

                CollectionStatusBadge(
                    isLate = isLate,
                    daysLate = installment.daysLate,
                    status = installment.status,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CollectionInfoBlock(
                    title = "Vence",
                    value = installment.dueDate ?: "-",
                    valueColor = TextMain,
                    modifier = Modifier.weight(1f),
                )

                CollectionInfoBlock(
                    title = "Pendiente",
                    value = currency.format(installment.pendingAmount),
                    valueColor = if (isLate) Error else Success,
                    modifier = Modifier.weight(1f),
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(OutlineVariant.copy(alpha = 0.25f)),
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                OutlinedTextField(
                    value = if (isCapitalMode) "%.2f".format(Locale.US, cuotaBase) else amount,
                    onValueChange = { if (!isCapitalMode) amount = it },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCapitalMode,
                    label = { Text(if (isCapitalMode) "Cuota a cobrar" else "Monto a cobrar") },
                    singleLine = true,
                    isError = amountError != null && !isCapitalMode,
                    supportingText = {
                        if (!isCapitalMode) amountError?.let { Text(it) }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.AttachMoney,
                            contentDescription = null,
                            tint = if (amountError != null && !isCapitalMode) Error else TextVariant,
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp),
                )

                PaymentMethodSelector(
                    selected = paymentMethod,
                    onSelected = { paymentMethod = it },
                )

                AllocationModeSelector(
                    selected = allocationMode,
                    hasLateFee = installment.pendingLateFee > 0,
                    includeCapitalPrepayment = true,
                    onSelected = { allocationMode = it },
                )

                if (isCapitalMode) {
                    OutlinedTextField(
                        value = capitalText,
                        onValueChange = { capitalText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Monto a abonar a capital") },
                        singleLine = true,
                        isError = capitalError != null,
                        supportingText = {
                            Text(capitalError ?: capitalCap?.let { "Disponible: ${currency.format(it)}" } ?: "Se aplica directo a capital.")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.AttachMoney,
                                contentDescription = null,
                                tint = if (capitalError != null) Error else TextVariant,
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(14.dp),
                    )

                    CapitalPrepaymentSummary(
                        cuota = cuotaBase,
                        capital = parsedCapital?.takeIf { it > 0 } ?: 0.0,
                    )
                } else {
                    parsedAmount?.let { amt ->
                        PaymentBreakdownCard(
                            amount = amt,
                            mode = allocationMode,
                            pendingPrincipal = installment.pendingPrincipal,
                            pendingInterest = installment.pendingInterest,
                            pendingLateFee = installment.pendingLateFee,
                        )
                    }
                }

                Button(
                    onClick = { showConfirmation = true },
                    enabled = canSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryContainer,
                        contentColor = Color.White,
                        disabledContainerColor = SurfaceContainerHigh,
                        disabledContentColor = Outline,
                    ),
                ) {
                    Text(
                        text = if (isLoading) "Procesando..." else "Registrar cobro",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                OutlinedButton(
                    onClick = { onOpenInstallment(installment.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextVariant,
                    ),
                ) {
                    Text(
                        text = "Ver detalle",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }

    if (showConfirmation && parsedAmount != null) {
        ConfirmPaymentDialog(
            clientName = installment.client?.fullName ?: "Cliente sin nombre",
            amount = currency.format(parsedAmount),
            method = paymentMethod.label,
            onDismiss = { showConfirmation = false },
            onConfirm = {
                showConfirmation = false
                onRegisterPayment(
                    installment.loanId,
                    "%.2f".format(Locale.US, parsedAmount),
                    paymentMethod.apiValue,
                    allocationMode.apiValue,
                    installment.id,
                    if (isCapitalMode) parsedCapital else null,
                )
            },
        )
    }
}

@Composable
private fun CollectionInfoBlock(
    title: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = TextVariant,
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1,
        )
    }
}

@Composable
private fun CollectionStatusBadge(
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
            else -> "Al día"
        }
    }

    val background = if (isLate) ErrorContainer else SuccessSoft
    val content = if (isLate) ErrorText else Success

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(background)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = content,
            maxLines = 1,
        )
    }
}

@Composable
internal fun PaymentMethodSelector(
    selected: PaymentMethod,
    onSelected: (PaymentMethod) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Método de pago",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = TextVariant,
        )

        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextMain,
                    containerColor = Color.White,
                ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = selected.label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )

                    Icon(
                        imageVector = Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        tint = TextVariant,
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                PaymentMethod.entries.forEach { method ->
                    DropdownMenuItem(
                        text = {
                            Text(method.label)
                        },
                        onClick = {
                            onSelected(method)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
internal fun ConfirmPaymentDialog(
    clientName: String,
    amount: String,
    method: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        shape = RoundedCornerShape(30.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(66.dp)
                    .clip(CircleShape)
                    .background(PrimaryFixed),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Payments,
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
                    text = "¿Estás seguro de registrar el pago del cliente?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextVariant,
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ConfirmInfoRow(
                            label = "Cliente",
                            value = clientName,
                        )

                        ConfirmInfoRow(
                            label = "Monto",
                            value = amount,
                        )

                        ConfirmInfoRow(
                            label = "Método",
                            value = method,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryContainer,
                    contentColor = Color.White,
                ),
            ) {
                Text(
                    text = "Confirmar",
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    text = "Cancelar",
                    fontWeight = FontWeight.Bold,
                    color = TextVariant,
                )
            }
        },
    )
}

@Composable
private fun ConfirmInfoRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
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

// ---------------------------------------------------------------------------
// Selector de modo de asignación del pago
// ---------------------------------------------------------------------------

@Composable
internal fun AllocationModeSelector(
    selected: AllocationMode,
    hasLateFee: Boolean,
    onSelected: (AllocationMode) -> Unit,
    includeCapitalPrepayment: Boolean = false,
) {
    val modes = AllocationMode.entries.filter { mode ->
        when (mode) {
            AllocationMode.Auto -> hasLateFee
            AllocationMode.CurrentPlusCapital -> includeCapitalPrepayment
            else -> true
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "¿Qué se cubre con este pago?",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = TextVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            modes.forEach { mode ->
                FilterChip(
                    selected = selected == mode,
                    onClick = { onSelected(mode) },
                    label = { Text(mode.shortLabel, style = MaterialTheme.typography.labelMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryContainer,
                        selectedLabelColor = Color.White,
                    ),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Desglose estimado del pago
// ---------------------------------------------------------------------------

private data class PaymentBreakdown(val principal: Double, val interest: Double, val late: Double)

private fun computeBreakdown(
    mode: AllocationMode,
    amount: Double,
    pendingPrincipal: Double,
    pendingInterest: Double,
    pendingLateFee: Double,
): PaymentBreakdown {
    var rem = amount
    var late = 0.0
    var interest = 0.0
    var principal = 0.0
    when (mode) {
        AllocationMode.Auto -> {
            late = min(rem, pendingLateFee); rem -= late
            interest = min(rem, pendingInterest); rem -= interest
            principal = min(rem, pendingPrincipal)
        }
        AllocationMode.PrincipalAndInterest -> {
            interest = min(rem, pendingInterest); rem -= interest
            principal = min(rem, pendingPrincipal)
        }
        AllocationMode.PrincipalOnly -> {
            principal = min(rem, pendingPrincipal)
        }
        AllocationMode.InterestOnly -> {
            interest = min(rem, pendingInterest)
        }
        AllocationMode.CurrentPlusCapital -> {
            // La cuota se cubre como Auto (mora→interés→capital); el sobrante
            // declarado va a capital y se muestra aparte (CapitalPrepaymentSummary).
            late = min(rem, pendingLateFee); rem -= late
            interest = min(rem, pendingInterest); rem -= interest
            principal = min(rem, pendingPrincipal)
        }
    }
    return PaymentBreakdown(principal, interest, late)
}

@Composable
internal fun CapitalPrepaymentSummary(
    cuota: Double,
    capital: Double,
) {
    val currency = rememberCurrency()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryFixed.copy(alpha = 0.45f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CapitalSummaryRow("Cuota", currency.format(cuota), bold = false)
            CapitalSummaryRow("Abono a capital", currency.format(capital), bold = false)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(OutlineVariant.copy(alpha = 0.25f)),
            )
            CapitalSummaryRow("Total a cobrar", currency.format(cuota + capital), bold = true)
        }
    }
}

@Composable
private fun CapitalSummaryRow(label: String, value: String, bold: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = if (bold) TextMain else TextVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = TextMain,
        )
    }
}

@Composable
internal fun PaymentBreakdownCard(
    amount: Double,
    mode: AllocationMode,
    pendingPrincipal: Double,
    pendingInterest: Double,
    pendingLateFee: Double,
) {
    val currency = rememberCurrency()
    val breakdown = computeBreakdown(mode, amount, pendingPrincipal, pendingInterest, pendingLateFee)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryFixed.copy(alpha = 0.45f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Desglose estimado",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Primary,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BreakdownItem("Capital", currency.format(breakdown.principal), Modifier.weight(1f))
                BreakdownItem("Interés", currency.format(breakdown.interest), Modifier.weight(1f))
                if (breakdown.late > 0 || mode == AllocationMode.Auto) {
                    BreakdownItem("Mora", currency.format(breakdown.late), Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun BreakdownItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TextMain)
    }
}

@Composable
private fun EmptyCollectionsState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 34.dp, vertical = 42.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(116.dp)
                    .clip(CircleShape)
                    .background(SecondaryContainer.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AssignmentTurnedIn,
                        contentDescription = null,
                        tint = Outline,
                        modifier = Modifier.size(52.dp),
                    )
                }
            }

            Text(
                text = "Ruta completada",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextMain,
            )

            Text(
                text = "No hay cuotas pendientes para este cobrador en la zona seleccionada.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextVariant,
            )
        }
    }
}
