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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sistemaprestamista.mobile.data.model.ClientSummary
import com.sistemaprestamista.mobile.data.model.LoanSummary
import com.sistemaprestamista.mobile.data.model.PaymentHistoryFilters
import com.sistemaprestamista.mobile.data.model.PaymentReceipt
import com.sistemaprestamista.mobile.ui.components.rememberCurrency

private val ScreenBackground = Color(0xFFF4F7FB)
private val CardBackground = Color(0xFFFFFFFF)
private val Primary = Color(0xFF00386C)
private val PrimaryContainer = Color(0xFF1A4F8B)
private val Secondary = Color(0xFF505F76)
private val SecondaryContainer = Color(0xFFD0E1FB)
private val SurfaceContainer = Color(0xFFEDEDF3)
private val SurfaceContainerLow = Color(0xFFF3F3F9)
private val SurfaceContainerHigh = Color(0xFFE8E8ED)
private val TextMain = Color(0xFF1A1C20)
private val TextVariant = Color(0xFF424750)
private val OutlineVariant = Color(0xFFC2C6D1)
private val SuccessContainer = Color(0xFF6FFBBE)
private val SuccessText = Color(0xFF005236)
private val Error = Color(0xFFBA1A1A)
private val ErrorContainer = Color(0xFFFFDAD6)
private val ErrorText = Color(0xFF93000A)

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
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            PaymentHistoryHeader()
        }

        item {
            PaymentFilterCard(
                clients = clients,
                loans = loans,
                selectedClientId = selectedClientId,
                selectedLoanId = selectedLoanId,
                selectedStatus = selectedStatus,
                dateFrom = dateFrom,
                dateTo = dateTo,
                isLoading = isLoading,
                onDateFromChange = { dateFrom = it },
                onDateToChange = { dateTo = it },
                onStatusSelected = { selectedStatus = it },
                onClientSelected = { id ->
                    selectedClientId = id
                    selectedLoanId = null
                },
                onLoanSelected = { selectedLoanId = it },
                onClear = {
                    selectedClientId = null
                    selectedLoanId = null
                    selectedStatus = null
                    dateFrom = ""
                    dateTo = ""
                    onApplyFilters(PaymentHistoryFilters())
                },
                onApply = {
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
            )
        }

        if (payments.isEmpty()) {
            item {
                PaymentEmptyState(
                    message = if (isLoading) {
                        "Cargando pagos..."
                    } else {
                        "No hay pagos para los filtros seleccionados. Intenta ajustar el rango de fechas."
                    },
                    isLoading = isLoading,
                )
            }
        } else {
            items(payments, key = { it.id }) { payment ->
                PaymentCard(
                    payment = payment,
                    onOpenReceipt = { onOpenReceipt(payment.id) },
                )
            }
        }
    }
}

@Composable
private fun PaymentHistoryHeader() {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "Historial de pagos",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Primary,
        )

        Text(
            text = "Consulta y filtra los pagos registrados",
            style = MaterialTheme.typography.bodyMedium,
            color = Secondary,
        )
    }
}

@Composable
private fun PaymentFilterCard(
    clients: List<ClientSummary>,
    loans: List<LoanSummary>,
    selectedClientId: Long?,
    selectedLoanId: Long?,
    selectedStatus: String?,
    dateFrom: String,
    dateTo: String,
    isLoading: Boolean,
    onDateFromChange: (String) -> Unit,
    onDateToChange: (String) -> Unit,
    onStatusSelected: (String?) -> Unit,
    onClientSelected: (Long?) -> Unit,
    onLoanSelected: (Long?) -> Unit,
    onClear: () -> Unit,
    onApply: () -> Unit,
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
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DateField(
                    label = "Desde",
                    value = dateFrom,
                    onValueChange = onDateFromChange,
                    modifier = Modifier.weight(1f),
                )

                DateField(
                    label = "Hasta",
                    value = dateTo,
                    onValueChange = onDateToChange,
                    modifier = Modifier.weight(1f),
                )
            }

            PaymentStatusSelector(
                selectedStatus = selectedStatus,
                onStatusSelected = onStatusSelected,
            )

            EntitySelector(
                label = "Cliente",
                value = clients.firstOrNull { it.id == selectedClientId }?.fullName ?: "Todos los clientes",
                options = listOf(null to "Todos los clientes") + clients.map { it.id to it.fullName },
                onSelected = onClientSelected,
            )

            EntitySelector(
                label = "Préstamo",
                value = loans.firstOrNull { it.id == selectedLoanId }?.loanNumber ?: "Todos los préstamos",
                options = listOf(null to "Todos los préstamos") + loans
                    .filter { selectedClientId == null || it.client?.id == selectedClientId }
                    .map { it.id to "${it.loanNumber} · ${it.client?.fullName.orEmpty()}" },
                onSelected = onLoanSelected,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onClear,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Primary,
                    ),
                ) {
                    Text(
                        text = "Limpiar",
                        fontWeight = FontWeight.Bold,
                    )
                }

                Button(
                    onClick = onApply,
                    modifier = Modifier
                        .weight(2f)
                        .height(50.dp),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryContainer,
                        contentColor = Color.White,
                    ),
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White,
                        )
                    } else {
                        Text(
                            text = "Aplicar filtros",
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DateField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Secondary,
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("YYYY-MM-DD") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(14.dp),
        )
    }
}

@Composable
private fun PaymentStatusSelector(
    selectedStatus: String?,
    onStatusSelected: (String?) -> Unit,
) {
    val statuses = listOf(null to "Todos", "valid" to "Válidos", "cancelled" to "Anulados")

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Estado",
            style = MaterialTheme.typography.labelSmall,
            color = Secondary,
        )

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth(),
        ) {
            statuses.forEachIndexed { index, (value, label) ->
                SegmentedButton(
                    selected = selectedStatus == value,
                    onClick = { onStatusSelected(value) },
                    shape = SegmentedButtonDefaults.itemShape(index, statuses.size),
                ) {
                    Text(
                        text = label,
                        fontWeight = FontWeight.Bold,
                    )
                }
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

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Secondary,
        )

        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
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
                        text = value,
                        maxLines = 1,
                        color = TextMain,
                    )

                    Icon(
                        imageVector = Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        tint = Secondary,
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { (id, text) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = text,
                                maxLines = 1,
                            )
                        },
                        onClick = {
                            onSelected(id)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentCard(
    payment: PaymentReceipt,
    onOpenReceipt: () -> Unit,
) {
    val currency = rememberCurrency()
    val isCancelled = payment.status.equals("cancelled", ignoreCase = true)

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
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = "RECIBO ${payment.receiptNumber}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Primary,
                    )

                    Text(
                        text = payment.client?.fullName ?: "Cliente no disponible",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextMain,
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            tint = Secondary,
                            modifier = Modifier.size(18.dp),
                        )

                        Text(
                            text = payment.paymentDate ?: "-",
                            style = MaterialTheme.typography.bodySmall,
                            color = Secondary,
                        )

                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = Secondary,
                        )

                        Text(
                            text = payment.loanNumber ?: "-",
                            style = MaterialTheme.typography.bodySmall,
                            color = Secondary,
                            maxLines = 1,
                        )
                    }
                }

                PaymentStatusBadge(status = payment.status)
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "MONTO PAGADO",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = Secondary,
                )

                Text(
                    text = currency.format(payment.amount),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isCancelled) Error else Primary,
                )
            }

            payment.commission?.let { commission ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceContainerLow)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Comision generada",
                            style = MaterialTheme.typography.labelSmall,
                            color = Secondary,
                        )

                        Text(
                            text = commission.status.ifBlank { "pendiente" },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = TextVariant,
                        )
                    }

                    Text(
                        text = currency.format(commission.commissionAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SuccessText,
                    )
                }
            }

            Button(
                onClick = onOpenReceipt,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SurfaceContainerLow,
                    contentColor = if (isCancelled) Secondary else Primary,
                ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = "Ver recibo",
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun PaymentStatusBadge(status: String?) {
    val isCancelled = status.equals("cancelled", ignoreCase = true)

    val text = if (isCancelled) {
        "Anulado"
    } else {
        "Válido"
    }

    val background = if (isCancelled) {
        ErrorContainer
    } else {
        SuccessContainer
    }

    val content = if (isCancelled) {
        ErrorText
    } else {
        SuccessText
    }

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
private fun PaymentEmptyState(
    message: String,
    isLoading: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceContainerLow,
        ),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(CardBackground),
                contentAlignment = Alignment.Center,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Primary,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.SearchOff,
                        contentDescription = null,
                        tint = Secondary,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }

            Text(
                text = if (isLoading) "Cargando pagos" else "Sin resultados",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextMain,
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Secondary,
            )
        }
    }
}
