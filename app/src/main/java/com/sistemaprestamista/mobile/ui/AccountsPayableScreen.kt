package com.sistemaprestamista.mobile.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sistemaprestamista.mobile.data.model.AccountPayableDetail
import com.sistemaprestamista.mobile.data.model.AccountPayableInput
import com.sistemaprestamista.mobile.data.model.AccountPayableSummary
import com.sistemaprestamista.mobile.data.model.CreditorInput
import com.sistemaprestamista.mobile.data.model.CreditorSummary
import com.sistemaprestamista.mobile.data.model.PaymentMethod
import com.sistemaprestamista.mobile.ui.components.EmptyCard
import com.sistemaprestamista.mobile.ui.components.LoadingSplash
import com.sistemaprestamista.mobile.ui.components.MetricCard
import com.sistemaprestamista.mobile.ui.components.MoneyFormatter
import com.sistemaprestamista.mobile.ui.components.StatusPill
import com.sistemaprestamista.mobile.ui.components.formatPaymentFrequency
import com.sistemaprestamista.mobile.ui.components.rememberCurrency
import java.time.LocalDate

@Composable
internal fun AccountsPayableScreen(
    accounts: List<AccountPayableSummary>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onOpenAccount: (Long) -> Unit,
    onCreateAccount: () -> Unit,
) {
    val defaultCurrency = rememberCurrency()
    val active = accounts.filter { it.status in setOf("active", "late") }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateAccount,
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("Nueva cuenta") },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Cuentas por pagar", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text("Obligaciones, cuotas y pagos a acreedores", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard("Activas", active.size.toString(), Modifier.weight(1f), compact = true)
                    MetricCard(
                        "Capital pendiente",
                        defaultCurrency.format(active.sumOf { it.remainingBalance }),
                        Modifier.weight(1f),
                        compact = true,
                    )
                }
            }
            if (isLoading && accounts.isEmpty()) {
                item { LoadingSplash() }
            } else if (accounts.isEmpty()) {
                item { EmptyCard("No hay cuentas por pagar registradas.") }
            } else {
                items(accounts, key = { it.id }) { account ->
                    AccountPayableCard(account, onOpenAccount)
                }
            }
            item {
                TextButton(onClick = onRefresh, enabled = !isLoading) {
                    Text(if (isLoading) "Actualizando..." else "Actualizar cuentas")
                }
            }
        }
    }
}

@Composable
private fun AccountPayableCard(
    account: AccountPayableSummary,
    onOpenAccount: (Long) -> Unit,
) {
    val money = remember(account.currency) { MoneyFormatter(account.currency) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenAccount(account.id) },
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(account.reference, fontWeight = FontWeight.Bold)
                    Text(account.creditor?.name ?: "Sin acreedor", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StatusPill(accountPayableStatusLabel(account.status))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Balance", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(money.format(account.remainingBalance), fontWeight = FontWeight.Bold)
            }
            Text(
                "${formatPaymentFrequency(account.paymentFrequency)} · Cuota ${money.format(account.installmentAmount)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun AccountPayableDetailScreen(
    detail: AccountPayableDetail?,
    isLoading: Boolean,
    isSaving: Boolean,
    onRegisterPayment: (Long, String, String, String?) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    if (detail == null) {
        if (isLoading) LoadingSplash() else EmptyCard("No se pudo cargar la cuenta.")
        return
    }

    val account = detail.summary
    val money = remember(account.currency) { MoneyFormatter(account.currency) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showPaymentDialog) {
        AccountPaymentDialog(
            installmentAmount = account.installmentAmount,
            isSaving = isSaving,
            onDismiss = { if (!isSaving) showPaymentDialog = false },
            onConfirm = { amount, method, notes ->
                onRegisterPayment(account.id, amount, method, notes)
                showPaymentDialog = false
            },
        )
    }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar cuenta") },
            text = { Text("Solo se puede eliminar si no tiene pagos. Esta acción revierte su entrada de caja.") },
            confirmButton = {
                Button(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") } },
        )
    }

    Scaffold(
        floatingActionButton = {
            if (account.status in setOf("active", "late")) {
                ExtendedFloatingActionButton(
                    onClick = { showPaymentDialog = true },
                    icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                    text = { Text("Registrar pago") },
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(account.reference, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(account.creditor?.name ?: "Sin acreedor", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    StatusPill(accountPayableStatusLabel(account.status))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard("Principal", money.format(account.principalAmount), Modifier.weight(1f), compact = true)
                    MetricCard("Balance", money.format(account.remainingBalance), Modifier.weight(1f), compact = true)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard("Cuota", money.format(account.installmentAmount), Modifier.weight(1f), compact = true)
                    MetricCard("Interés", money.format(account.totalInterest), Modifier.weight(1f), compact = true)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onEdit, enabled = detail.payments.isEmpty()) {
                        Icon(Icons.Outlined.Edit, contentDescription = null)
                        Text(" Editar")
                    }
                    OutlinedButton(onClick = { showDeleteDialog = true }, enabled = detail.payments.isEmpty()) {
                        Icon(Icons.Outlined.Delete, contentDescription = null)
                        Text(" Eliminar")
                    }
                }
            }
            item { Text("Plan de cuotas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(detail.installments, key = { it.id }) { installment ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Cuota #${installment.installmentNumber}", fontWeight = FontWeight.Bold)
                            StatusPill(installmentStatusLabel(installment.status))
                        }
                        Text("Vence: ${installment.dueDate ?: "Sin fecha"}")
                        Text(
                            "Pendiente: ${money.format(installment.pendingAmount)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item { Text("Pagos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            if (detail.payments.isEmpty()) {
                item { EmptyCard("No hay pagos registrados.") }
            } else {
                items(detail.payments, key = { it.id }) { payment ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(payment.paymentNumber, fontWeight = FontWeight.Bold)
                                Text(payment.paymentDate ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(money.format(payment.amount), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun AccountPayableFormScreen(
    existing: AccountPayableDetail?,
    creditors: List<CreditorSummary>,
    defaultCurrency: String,
    isSaving: Boolean,
    onCreateCreditor: (CreditorInput) -> Unit,
    onSubmit: (AccountPayableInput) -> Unit,
) {
    var creditorId by remember(existing?.summary?.id, creditors) {
        mutableStateOf(existing?.summary?.creditor?.id ?: creditors.firstOrNull()?.id)
    }
    var currency by remember(existing?.summary?.id) { mutableStateOf(existing?.summary?.currency ?: defaultCurrency) }
    var principal by remember(existing?.summary?.id) { mutableStateOf(existing?.summary?.principalAmount?.toPlainText().orEmpty()) }
    var interest by remember(existing?.summary?.id) { mutableStateOf(existing?.summary?.interestRate?.toPlainText() ?: "10") }
    var terms by remember(existing?.summary?.id) { mutableStateOf(existing?.summary?.termQuantity?.toString() ?: "12") }
    var frequency by remember(existing?.summary?.id) { mutableStateOf(existing?.summary?.paymentFrequency ?: "monthly") }
    var method by remember(existing?.summary?.id) { mutableStateOf(existing?.summary?.calculationMethod ?: "french_amortization") }
    var lateFeeType by remember(existing?.summary?.id) { mutableStateOf(existing?.lateFeeType ?: "none") }
    var lateFeeValue by remember(existing?.summary?.id) { mutableStateOf(existing?.lateFeeValue?.toPlainText() ?: "0") }
    var disbursementDate by remember(existing?.summary?.id) {
        mutableStateOf(existing?.summary?.disbursementDate ?: LocalDate.now().toString())
    }
    var firstPaymentDate by remember(existing?.summary?.id) {
        mutableStateOf(existing?.summary?.firstPaymentDate ?: LocalDate.now().plusMonths(1).toString())
    }
    var notes by remember(existing?.summary?.id) { mutableStateOf(existing?.notes.orEmpty()) }
    var showCreditorDialog by remember { mutableStateOf(false) }

    if (showCreditorDialog) {
        CreditorDialog(
            isSaving = isSaving,
            onDismiss = { showCreditorDialog = false },
            onConfirm = {
                onCreateCreditor(it)
                showCreditorDialog = false
            },
        )
    }

    val canSubmit = creditorId != null &&
        principal.toDoubleOrNull()?.let { it > 0 } == true &&
        interest.toDoubleOrNull() != null &&
        terms.toIntOrNull()?.let { it > 0 } == true &&
        lateFeeValue.toDoubleOrNull() != null &&
        disbursementDate.isNotBlank() &&
        firstPaymentDate.isNotBlank() &&
        !isSaving

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SelectorField(
                label = "Acreedor",
                selectedLabel = creditors.firstOrNull { it.id == creditorId }?.name ?: "Selecciona un acreedor",
                options = creditors.map { it.id to it.name },
                onSelected = { creditorId = it },
            )
            TextButton(onClick = { showCreditorDialog = true }) { Text("Crear acreedor") }
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("RD$", "US$").forEach { value ->
                    FilterChip(selected = currency == value, onClick = { currency = value }, label = { Text(value) })
                }
            }
        }
        item { DecimalField("Monto principal", principal) { principal = it } }
        item { DecimalField("Tasa (%)", interest) { interest = it } }
        item { IntegerField("Cantidad de cuotas", terms) { terms = it } }
        item {
            SelectorField(
                "Frecuencia",
                formatPaymentFrequency(frequency),
                listOf("daily" to "Diario", "weekly" to "Semanal", "biweekly" to "Quincenal", "monthly" to "Mensual"),
            ) { frequency = it }
        }
        item {
            SelectorField(
                "Método",
                calculationMethodLabel(method),
                listOf(
                    "french_amortization" to "Amortización francesa",
                    "flat_interest" to "Interés fijo",
                    "fixed_installment" to "Cuota fija",
                    "capital_plus_interest" to "Capital + interés",
                    "interest_only" to "Solo interés",
                ),
            ) { method = it }
        }
        item {
            SelectorField(
                "Mora",
                lateFeeLabel(lateFeeType),
                listOf("none" to "Sin mora", "fixed" to "Monto fijo", "daily_fixed" to "Monto diario"),
            ) { lateFeeType = it }
        }
        if (lateFeeType != "none") {
            item { DecimalField("Valor de mora", lateFeeValue) { lateFeeValue = it } }
        }
        item {
            OutlinedTextField(
                value = disbursementDate,
                onValueChange = { disbursementDate = it },
                label = { Text("Fecha de desembolso (AAAA-MM-DD)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = firstPaymentDate,
                onValueChange = { firstPaymentDate = it },
                label = { Text("Primer pago (AAAA-MM-DD)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notas") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Button(
                onClick = {
                    onSubmit(
                        AccountPayableInput(
                            creditorId = requireNotNull(creditorId),
                            currency = currency,
                            principalAmount = principal.toDouble(),
                            interestRate = interest.toDouble(),
                            interestType = if (method == "french_amortization") "amortized" else "fixed",
                            paymentFrequency = frequency,
                            calculationMethod = method,
                            termQuantity = terms.toInt(),
                            lateFeeType = lateFeeType,
                            lateFeeValue = if (lateFeeType == "none") 0.0 else lateFeeValue.toDouble(),
                            disbursementDate = disbursementDate,
                            firstPaymentDate = firstPaymentDate,
                            notes = notes.trim().ifBlank { null },
                        ),
                    )
                },
                enabled = canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (isSaving) CircularProgressIndicator() else Text("Guardar cuenta", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AccountPaymentDialog(
    installmentAmount: Double,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String?) -> Unit,
) {
    var amount by remember { mutableStateOf(installmentAmount.toPlainText()) }
    var method by remember { mutableStateOf(PaymentMethod.Cash) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar pago") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DecimalField("Monto", amount) { amount = it }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaymentMethod.entries.forEach { option ->
                        FilterChip(
                            selected = method == option,
                            onClick = { method = option },
                            label = { Text(option.label) },
                        )
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(amount, method.apiValue, notes.trim().ifBlank { null }) },
                enabled = !isSaving && amount.toDoubleOrNull()?.let { it > 0 } == true,
            ) { Text(if (isSaving) "Guardando..." else "Pagar") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Cancelar") } },
    )
}

@Composable
private fun CreditorDialog(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (CreditorInput) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo acreedor") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Teléfono") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(CreditorInput(name = name.trim(), phone = phone.trim().ifBlank { null })) },
                enabled = name.isNotBlank() && !isSaving,
            ) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun <T> SelectorField(
    label: String,
    selectedLabel: String,
    options: List<Pair<T, String>>,
    onSelected: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selectedLabel, modifier = Modifier.weight(1f))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        expanded = false
                        onSelected(value)
                    },
                )
            }
        }
    }
}

@Composable
private fun DecimalField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> onValueChange(input.filter { it.isDigit() || it == '.' }) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun IntegerField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> onValueChange(input.filter(Char::isDigit)) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun Double.toPlainText(): String =
    if (this % 1.0 == 0.0) toLong().toString() else toString()

private fun calculationMethodLabel(value: String): String = when (value) {
    "french_amortization" -> "Amortización francesa"
    "flat_interest" -> "Interés fijo"
    "fixed_installment" -> "Cuota fija"
    "capital_plus_interest" -> "Capital + interés"
    "interest_only" -> "Solo interés"
    else -> value
}

private fun lateFeeLabel(value: String): String = when (value) {
    "none" -> "Sin mora"
    "fixed" -> "Monto fijo"
    "daily_fixed" -> "Monto diario"
    else -> value
}

private fun accountPayableStatusLabel(value: String): String = when (value.trim().lowercase()) {
    "active" -> "Activa"
    "late" -> "Atrasada"
    "paid" -> "Pagada"
    "cancelled", "canceled" -> "Cancelada"
    else -> value.replaceFirstChar { it.uppercase() }
}
