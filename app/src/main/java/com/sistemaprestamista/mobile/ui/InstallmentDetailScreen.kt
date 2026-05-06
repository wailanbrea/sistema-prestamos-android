package com.sistemaprestamista.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sistemaprestamista.mobile.data.model.InstallmentSummary
import com.sistemaprestamista.mobile.data.model.PaymentMethod
import com.sistemaprestamista.mobile.ui.components.EmptyCard
import com.sistemaprestamista.mobile.ui.components.MetricCard
import com.sistemaprestamista.mobile.ui.components.StatusPill
import com.sistemaprestamista.mobile.ui.components.rememberCurrency
import java.util.Locale

@Composable
internal fun InstallmentDetailScreen(
    installment: InstallmentSummary?,
    isLoading: Boolean,
    onRegisterPayment: (Long, String, String) -> Unit,
) {
    if (installment == null) {
        EmptyCard("No se encontró la cuota seleccionada.")
        return
    }

    var amount by remember(installment.id) {
        mutableStateOf("%.2f".format(Locale.US, installment.pendingAmount))
    }
    var paymentMethod by remember(installment.id) { mutableStateOf(PaymentMethod.Cash) }
    var showConfirmation by remember(installment.id) { mutableStateOf(false) }
    val currency = rememberCurrency()
    val parsedAmount = amount.toDoubleOrNull()
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
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Card(shape = RoundedCornerShape(18.dp)) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Text(installment.client?.fullName ?: "Cliente sin nombre", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${installment.loanNumber} · cuota ${installment.installmentNumber}")
                StatusPill(if (installment.daysLate > 0) "${installment.daysLate} días atraso" else installment.status)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("Capital", currency.format(installment.principalAmount), Modifier.weight(1f))
            MetricCard("Interés", currency.format(installment.interestAmount), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("Mora", currency.format(installment.lateFee), Modifier.weight(1f))
            MetricCard("Pendiente", currency.format(installment.pendingAmount), Modifier.weight(1f))
        }
        Card(shape = RoundedCornerShape(18.dp)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
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
                    enabled = !isLoading && amountError == null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Registrar cobro")
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
                onRegisterPayment(installment.loanId, amount, paymentMethod.apiValue)
            },
        )
    }
}
