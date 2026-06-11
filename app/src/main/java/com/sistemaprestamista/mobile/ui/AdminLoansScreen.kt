package com.sistemaprestamista.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.unit.dp
import com.sistemaprestamista.mobile.data.model.LoanSummary
import com.sistemaprestamista.mobile.ui.components.formatPaymentFrequency
import com.sistemaprestamista.mobile.ui.components.rememberCurrency

private val ScreenBackground = Color(0xFFF4F7FB)
private val CardBackground = Color(0xFFFFFFFF)
private val Primary = Color(0xFF00386C)
private val PrimaryContainer = Color(0xFF1A4F8B)
private val Secondary = Color(0xFF505F76)
private val TextMain = Color(0xFF1A1C20)
private val TextMuted = Color(0xFF667085)
private val Outline = Color(0xFF737781)

@Composable
internal fun AdminLoansScreen(
    loans: List<LoanSummary>,
    onOpenLoan: (Long) -> Unit,
    hasMore: Boolean = false,
    isLoadingMore: Boolean = false,
    onLoadMore: () -> Unit = {},
    onOpenQuotes: (() -> Unit)? = null,
    onCreateLoan: (() -> Unit)? = null,
) {
    var query by remember { mutableStateOf("") }
    val currency = rememberCurrency()
    val listState = rememberLazyListState()

    val filtered = remember(loans, query) {
        val q = query.trim().lowercase()
        if (q.isBlank()) loans else loans.filter {
            it.loanNumber.lowercase().contains(q) ||
                it.client?.fullName.orEmpty().lowercase().contains(q)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Préstamos",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Primary,
                    )
                    Text(
                        text = "Cartera de toda la empresa",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Secondary,
                    )
                }

                if (onOpenQuotes != null) {
                    OutlinedButton(
                        onClick = onOpenQuotes,
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Calculate,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Primary,
                        )
                        Text(
                            text = " Cotizar",
                            fontWeight = FontWeight.Bold,
                            color = Primary,
                        )
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar por número o cliente...", color = Outline) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = Outline) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardBackground,
                    unfocusedContainerColor = CardBackground,
                    focusedBorderColor = PrimaryContainer,
                    unfocusedBorderColor = Color.Transparent,
                ),
            )
        }

        if (filtered.isEmpty()) {
            item {
                Text(
                    text = "No hay préstamos para mostrar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    modifier = Modifier.padding(8.dp),
                )
            }
        } else {
            items(filtered, key = { it.id }) { loan ->
                LoanRowCard(loan = loan, amount = currency.format(loan.remainingBalance), onOpenLoan = onOpenLoan)
            }
        }

        if (hasMore) {
            item {
                OutlinedButton(
                    onClick = onLoadMore,
                    enabled = !isLoadingMore,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    if (isLoadingMore) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Primary)
                    } else {
                        Text("Cargar más", fontWeight = FontWeight.Bold, color = Primary)
                    }
                }
            }
        }
    }

    if (onCreateLoan != null) {
        ExtendedFloatingActionButton(
            onClick = onCreateLoan,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = PrimaryContainer,
            contentColor = Color.White,
            icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
            text = { Text("Nuevo préstamo", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
        )
    }
    }
}

@Composable
private fun LoanRowCard(
    loan: LoanSummary,
    amount: String,
    onOpenLoan: (Long) -> Unit,
) {
    Card(
        onClick = { onOpenLoan(loan.id) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = loan.client?.fullName ?: "Cliente no disponible",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextMain,
                    maxLines = 1,
                )
                Text(
                    text = "${loan.loanNumber} · ${formatPaymentFrequency(loan.paymentFrequency)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 1,
                )
                LoanStatusChip(loan.status)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Balance", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text(
                    text = amount,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                )
            }
        }
    }
}

@Composable
private fun LoanStatusChip(status: String) {
    val label = when (status) {
        "active" -> "Activo"
        "late" -> "Atrasado"
        "paid" -> "Pagado"
        "pending" -> "Pendiente"
        "cancelled" -> "Cancelado"
        "refinanced" -> "Refinanciado"
        "legal" -> "Legal"
        "written_off" -> "Castigado"
        else -> status
    }
    val color = when (status) {
        "late", "legal", "written_off" -> Color(0xFFBA1A1A)
        "paid" -> Color(0xFF005236)
        "pending" -> Color(0xFFB25E00)
        else -> PrimaryContainer
    }
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color)
    }
}
