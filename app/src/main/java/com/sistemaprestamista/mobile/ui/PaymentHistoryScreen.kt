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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.sistemaprestamista.mobile.data.model.ClientSummary
import com.sistemaprestamista.mobile.data.model.LoanSummary
import com.sistemaprestamista.mobile.data.model.PaymentHistoryFilters
import com.sistemaprestamista.mobile.data.model.PaymentReceipt
import com.sistemaprestamista.mobile.ui.components.EmptyCard
import com.sistemaprestamista.mobile.ui.components.StatusPill
import com.sistemaprestamista.mobile.ui.components.rememberCurrency

@Composable
internal fun PaymentHistoryScreen(
    payments: List<PaymentReceipt>,
    clients: List<ClientSummary>,
    loans: List<LoanSummary>,
    filters: PaymentHistoryFilters,
    isLoading: Boolean,
    onApplyFilters: (PaymentHistoryFilters) -> Unit,
    onOpenReceipt: (Long) -> Unit,
) {
    var selectedClientId by remember(filters) { mutableStateOf(filters.clientId) }
    var selectedLoanId by remember(filters) { mutableStateOf(filters.loanId) }
    var selectedStatus by remember(filters) { mutableStateOf(filters.status) }
    var dateFrom by remember(filters) { mutableStateOf(filters.dateFrom.orEmpty()) }
    var dateTo by remember(filters) { mutableStateOf(filters.dateTo.orEmpty()) }
    val currency = rememberCurrency()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Historial de pagos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = dateFrom,
                            onValueChange = { dateFrom = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Desde") },
                            placeholder = { Text("YYYY-MM-DD") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(14.dp),
                        )
                        OutlinedTextField(
                            value = dateTo,
                            onValueChange = { dateTo = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Hasta") },
                            placeholder = { Text("YYYY-MM-DD") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(14.dp),
                        )
                    }
                    PaymentStatusSelector(
                        selectedStatus = selectedStatus,
                        onStatusSelected = { selectedStatus = it },
                    )
                    EntitySelector(
                        label = "Cliente",
                        value = clients.firstOrNull { it.id == selectedClientId }?.fullName ?: "Todos",
                        options = listOf(null to "Todos") + clients.map { it.id to it.fullName },
                        onSelected = { id ->
                            selectedClientId = id
                            selectedLoanId = null
                        },
                    )
                    EntitySelector(
                        label = "Préstamo",
                        value = loans.firstOrNull { it.id == selectedLoanId }?.loanNumber ?: "Todos",
                        options = listOf(null to "Todos") + loans
                            .filter { selectedClientId == null || it.client?.id == selectedClientId }
                            .map { it.id to "${it.loanNumber} · ${it.client?.fullName.orEmpty()}" },
                        onSelected = { selectedLoanId = it },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                selectedClientId = null
                                selectedLoanId = null
                                selectedStatus = null
                                dateFrom = ""
                                dateTo = ""
                                onApplyFilters(PaymentHistoryFilters())
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text("Limpiar")
                        }
                        Button(
                            onClick = {
                                onApplyFilters(
                                    PaymentHistoryFilters(
                                        clientId = selectedClientId,
                                        loanId = selectedLoanId,
                                        status = selectedStatus,
                                        dateFrom = dateFrom.ifBlank { null },
                                        dateTo = dateTo.ifBlank { null },
                                    ),
                                )
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading,
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator()
                            } else {
                                Text("Aplicar")
                            }
                        }
                    }
                }
            }
        }
        if (payments.isEmpty()) {
            item { EmptyCard(if (isLoading) "Cargando pagos..." else "No hay pagos para los filtros seleccionados.") }
        } else {
            items(payments, key = { it.id }) { payment ->
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(payment.receiptNumber, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(payment.client?.fullName ?: "Cliente no disponible")
                                Text("${payment.paymentDate ?: "-"} · ${payment.loanNumber ?: "-"}")
                            }
                            StatusPill(payment.status)
                        }
                        Text(currency.format(payment.amount), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        OutlinedButton(
                            onClick = { onOpenReceipt(payment.id) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text("Ver recibo")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentStatusSelector(
    selectedStatus: String?,
    onStatusSelected: (String?) -> Unit,
) {
    val statuses = listOf(null to "Todos", "valid" to "Válidos", "cancelled" to "Anulados")

    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        statuses.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = selectedStatus == value,
                onClick = { onStatusSelected(value) },
                shape = SegmentedButtonDefaults.itemShape(index, statuses.size),
            ) {
                Text(label)
            }
        }
    }
}

@Composable
private fun EntitySelector(
    label: String,
    value: String,
    options: List<Pair<Long?, String>>,
    onSelected: (Long?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(value, maxLines = 1)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { (id, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onSelected(id)
                        expanded = false
                    },
                )
            }
        }
    }
}
