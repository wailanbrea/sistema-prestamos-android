package com.sistemaprestamista.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sistemaprestamista.mobile.data.model.LoanSummary
import com.sistemaprestamista.mobile.ui.components.rememberCurrency

private val ScreenBackground = Color(0xFFF4F7FB)
private val CardBackground = Color(0xFFFFFFFF)
private val Primary = Color(0xFF00386C)
private val Secondary = Color(0xFF505F76)
private val TextMain = Color(0xFF1A1C20)
private val TextMuted = Color(0xFF667085)
private val Green = Color(0xFF12B76A)
private val Red = Color(0xFFBA1A1A)

@Composable
internal fun ApprovalsScreen(
    approvals: List<LoanSummary>,
    isActionLoading: Boolean,
    onApprove: (Long) -> Unit,
    onReject: (Long, String?) -> Unit,
    onOpenLoan: (Long) -> Unit,
) {
    val currency = rememberCurrency()
    var rejectLoanId by remember { mutableStateOf<Long?>(null) }

    rejectLoanId?.let { loanId ->
        RejectReasonDialog(
            onConfirm = { reason ->
                onReject(loanId, reason)
                rejectLoanId = null
            },
            onDismiss = { rejectLoanId = null },
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Aprobaciones",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                )
                Text(
                    text = "Préstamos pendientes de aprobar",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Secondary,
                )
            }
        }

        if (approvals.isEmpty()) {
            item { EmptyApprovals() }
        } else {
            items(approvals, key = { it.id }) { loan ->
                ApprovalCard(
                    loan = loan,
                    amount = currency.format(loan.principalAmount),
                    isActionLoading = isActionLoading,
                    onApprove = onApprove,
                    onReject = { rejectLoanId = it },
                    onOpenLoan = onOpenLoan,
                )
            }
        }
    }
}

@Composable
private fun ApprovalCard(
    loan: LoanSummary,
    amount: String,
    isActionLoading: Boolean,
    onApprove: (Long) -> Unit,
    onReject: (Long) -> Unit,
    onOpenLoan: (Long) -> Unit,
) {
    Card(
        onClick = { onOpenLoan(loan.id) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = loan.client?.fullName ?: "Cliente no disponible",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextMain,
                        maxLines = 1,
                    )
                    Text(
                        text = "${loan.loanNumber} · ${loan.paymentFrequency}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Monto", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(
                        text = amount,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Primary,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { onReject(loan.id) },
                    enabled = !isActionLoading,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Red),
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.height(18.dp))
                    Text("Rechazar", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onApprove(loan.id) },
                    enabled = !isActionLoading,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Green),
                ) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.height(18.dp))
                    Text("Aprobar", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun RejectReasonDialog(
    onConfirm: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rechazar préstamo") },
        text = {
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                placeholder = { Text("Motivo (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(reason.trim().ifBlank { null }) }) {
                Text("Rechazar", color = Red, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

@Composable
private fun EmptyApprovals() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Outlined.TaskAlt,
                    contentDescription = null,
                    tint = Green,
                    modifier = Modifier.height(54.dp),
                )
            }
            Text(
                text = "Sin préstamos pendientes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Secondary,
            )
            Text(
                text = "Cuando haya solicitudes por aprobar aparecerán aquí.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
            )
        }
    }
}
