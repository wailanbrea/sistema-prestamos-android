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
import com.sistemaprestamista.mobile.data.model.ClientDetail
import com.sistemaprestamista.mobile.data.model.ClientSummary
import com.sistemaprestamista.mobile.data.model.InstallmentSummary
import com.sistemaprestamista.mobile.ui.components.EmptyCard
import com.sistemaprestamista.mobile.ui.components.MetricCard
import com.sistemaprestamista.mobile.ui.components.StatusPill
import com.sistemaprestamista.mobile.ui.components.rememberCurrency

@Composable
internal fun ClientDetailScreen(
    detail: ClientDetail?,
    isLoading: Boolean,
    fallbackClient: ClientSummary?,
    onOpenLoan: (Long) -> Unit,
    onOpenInstallment: (Long) -> Unit,
) {
    val client = detail?.summary ?: fallbackClient
    if (client == null) {
        EmptyCard("No se encontró el cliente seleccionado.")
        return
    }

    val currency = rememberCurrency()
    val loans = detail?.loans.orEmpty()
    val installments = detail?.pendingInstallments.orEmpty()
    val recentPayments = detail?.recentPayments.orEmpty()
    val activeBalance = detail?.financialSummary?.remainingBalance ?: loans.sumOf { it.remainingBalance }
    val pendingAmount = installments.sumOf { it.pendingAmount }

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
                    Text(client.fullName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(listOfNotNull(client.phone, client.identification).joinToString(" · "))
                    if (!client.address.isNullOrBlank()) {
                        Text(client.address)
                    }
                    StatusPill("${client.status} · riesgo ${client.riskLevel}")
                    detail?.let {
                        if (!it.workplace.isNullOrBlank()) {
                            Text("Trabajo: ${it.workplace}")
                        }
                        if (!it.email.isNullOrBlank()) {
                            Text(it.email)
                        }
                    }
                }
            }
        }
        if (isLoading && detail == null) {
            item { EmptyCard("Cargando detalle del cliente...") }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Balance", currency.format(activeBalance), Modifier.weight(1f))
                MetricCard("Pendiente", currency.format(pendingAmount), Modifier.weight(1f))
            }
        }
        detail?.financialSummary?.let { summary ->
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard("Pagado", currency.format(summary.totalPaid), Modifier.weight(1f), compact = true)
                    MetricCard("Moras", summary.lateInstallments.toString(), Modifier.weight(1f), compact = true)
                }
            }
        }
        item {
            Text("Préstamos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        if (loans.isEmpty()) {
            item { EmptyCard("Este cliente no tiene préstamos asignados al cobrador.") }
        } else {
            items(loans, key = { it.id }) { loan ->
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Text(loan.loanNumber, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Cuota ${currency.format(loan.installmentAmount)} · balance ${currency.format(loan.remainingBalance)}")
                        StatusPill("${loan.paymentFrequency} · ${loan.status}")
                        OutlinedButton(
                            onClick = { onOpenLoan(loan.id) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text("Ver préstamo")
                        }
                    }
                }
            }
        }
        item {
            Text("Cuotas pendientes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        if (installments.isEmpty()) {
            item { EmptyCard("No hay cuotas pendientes para este cliente.") }
        } else {
            items(installments, key = { it.id }) { installment ->
                CompactInstallmentCard(installment, onOpenInstallment)
            }
        }
        if (recentPayments.isNotEmpty()) {
            item {
                Text("Pagos recientes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            items(recentPayments, key = { it.id }) { payment ->
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
private fun CompactInstallmentCard(
    installment: InstallmentSummary,
    onOpenInstallment: (Long) -> Unit,
) {
    val currency = rememberCurrency()

    Card(shape = RoundedCornerShape(18.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text("${installment.loanNumber} · cuota ${installment.installmentNumber}", fontWeight = FontWeight.SemiBold)
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
