package com.sistemaprestamista.mobile.ui

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sistemaprestamista.mobile.data.model.InstallmentSummary
import com.sistemaprestamista.mobile.data.model.PaymentMethod
import com.sistemaprestamista.mobile.ui.components.EmptyCard
import com.sistemaprestamista.mobile.ui.components.StatusPill
import com.sistemaprestamista.mobile.ui.components.rememberCurrency
import java.util.Locale

@Composable
internal fun CollectionsScreen(
    state: AppUiState,
    onRegisterPayment: (Long, String, String) -> Unit,
    onOpenInstallment: (Long) -> Unit,
) {
    val installments = state.collectorInstallments

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (installments.isEmpty()) {
            item { EmptyCard("No hay cuotas pendientes para este cobrador.") }
        } else {
            items(installments, key = { it.id }) { installment ->
                InstallmentCard(
                    installment = installment,
                    isLoading = state.isLoading,
                    onRegisterPayment = onRegisterPayment,
                    onOpenInstallment = onOpenInstallment,
                )
            }
        }
    }
}

@Composable
private fun InstallmentCard(
    installment: InstallmentSummary,
    isLoading: Boolean,
    onRegisterPayment: (Long, String, String) -> Unit,
    onOpenInstallment: (Long) -> Unit,
) {
    var amount by remember(installment.id) {
        mutableStateOf("%.2f".format(Locale.US, installment.pendingAmount))
    }
    var paymentMethod by remember(installment.id) { mutableStateOf(PaymentMethod.Cash) }
    var showConfirmation by remember(installment.id) { mutableStateOf(false) }
    val currency = rememberCurrency()
    val isLate = installment.daysLate > 0
    val parsedAmount = amount.toDoubleOrNull()
    val amountError = when {
        amount.isBlank() -> "Indica el monto."
        parsedAmount == null -> "Monto inválido."
        parsedAmount <= 0 -> "Debe ser mayor que cero."
        parsedAmount > installment.pendingAmount -> "No puede exceder ${currency.format(installment.pendingAmount)}."
        else -> null
    }
    val canSubmit = amountError == null && !isLoading

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLate) Color(0xFFFFF1F0) else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = installment.client?.fullName ?: "Cliente sin nombre",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "${installment.loanNumber} · cuota ${installment.installmentNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusPill(if (isLate) "${installment.daysLate} días" else installment.status)
            }
            Text(
                text = "Vence ${installment.dueDate ?: "-"} · pendiente ${currency.format(installment.pendingAmount)}",
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Monto a cobrar") },
                singleLine = true,
                isError = amountError != null,
                supportingText = { amountError?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(14.dp),
            )
            PaymentMethodSelector(
                selected = paymentMethod,
                onSelected = { paymentMethod = it },
            )
            Button(
                onClick = { showConfirmation = true },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Registrar cobro")
            }
            OutlinedButton(
                onClick = { onOpenInstallment(installment.id) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Ver detalle")
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
                onRegisterPayment(installment.loanId, amount, paymentMethod.apiValue)
            },
        )
    }
}

@Composable
internal fun PaymentMethodSelector(
    selected: PaymentMethod,
    onSelected: (PaymentMethod) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Método de pago", style = MaterialTheme.typography.labelMedium)
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(selected.label)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            PaymentMethod.entries.forEach { method ->
                DropdownMenuItem(
                    text = { Text(method.label) },
                    onClick = {
                        onSelected(method)
                        expanded = false
                    },
                )
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
        title = { Text("Confirmar cobro") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(clientName, fontWeight = FontWeight.SemiBold)
                Text("Monto: $amount")
                Text("Método: $method")
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    )
}
