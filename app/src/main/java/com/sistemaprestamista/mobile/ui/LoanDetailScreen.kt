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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sistemaprestamista.mobile.data.model.InstallmentSummary
import com.sistemaprestamista.mobile.data.model.LoanDetail
import com.sistemaprestamista.mobile.data.model.LoanSummary
import com.sistemaprestamista.mobile.ui.components.EmptyCard
import com.sistemaprestamista.mobile.ui.components.MetricCard
import com.sistemaprestamista.mobile.ui.components.StatusPill
import com.sistemaprestamista.mobile.ui.components.rememberCurrency

@Composable
internal fun LoanDetailScreen(
    detail: LoanDetail?,
    isLoading: Boolean,
    fallbackLoan: LoanSummary?,
    onOpenInstallment: (Long) -> Unit,
) {
    val loan = detail?.summary ?: fallbackLoan
    if (loan == null) {
        EmptyCard("No se encontró el préstamo seleccionado.")
        return
    }

    val currency = rememberCurrency()
    val installments = detail?.installments.orEmpty()
    val payments = detail?.payments.orEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(loan.loanNumber, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(loan.client?.fullName ?: "Cliente no disponible")
                    StatusPill("${loan.paymentFrequency} · ${loan.status}")
                    detail?.let {
                        Text("${it.calculationMethod} · ${it.interestType} · ${it.interestRate}%")
                        Text("Inicio ${it.startDate ?: "-"} · primera cuota ${it.firstPaymentDate ?: "-"}")
                    }
                }
            }
        }
        if (isLoading && detail == null) {
            item { EmptyCard("Cargando detalle del préstamo...") }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Capital", currency.format(loan.principalAmount), Modifier.weight(1f))
                MetricCard("Balance", currency.format(loan.remainingBalance), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Cuota", currency.format(loan.installmentAmount), Modifier.weight(1f), compact = true)
                MetricCard("Total", currency.format(loan.totalAmount), Modifier.weight(1f), compact = true)
            }
        }
        detail?.let {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard("Pagado", currency.format(it.financialSummary.amountPaid), Modifier.weight(1f), compact = true)
                    MetricCard("Atrasos", it.financialSummary.installmentsLate.toString(), Modifier.weight(1f), compact = true)
                }
            }
        }
        item {
            Text("Cuotas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        if (installments.isEmpty()) {
            item { EmptyCard("Aún no hay cuotas cargadas para este préstamo.") }
        } else {
            items(installments, key = { it.id }) { installment ->
                LoanInstallmentRow(installment, onOpenInstallment)
            }
        }
        if (payments.isNotEmpty()) {
            item {
                Text("Pagos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            items(payments, key = { it.id }) { payment ->
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Text(payment.receiptNumber, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("${payment.paymentDate ?: "-"} · ${currency.format(payment.amount)}")
                        StatusPill(payment.status)
                    }
                }
            }
        }
    }
}

@Composable
private fun LoanInstallmentRow(
    installment: InstallmentSummary,
    onOpenInstallment: (Long) -> Unit,
) {
    val currency = rememberCurrency()

    Card(shape = RoundedCornerShape(18.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text("Cuota ${installment.installmentNumber}", fontWeight = FontWeight.SemiBold)
            Text("Vence ${installment.dueDate ?: "-"} · pendiente ${currency.format(installment.pendingAmount)}")
            StatusPill(if (installment.daysLate > 0) "${installment.daysLate} días atraso" else installment.status)
            OutlinedButton(
                onClick = { onOpenInstallment(installment.id) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Ver cuota")
            }
        }
    }
}
