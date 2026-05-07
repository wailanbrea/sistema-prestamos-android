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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sistemaprestamista.mobile.data.pending.PendingPayment
import com.sistemaprestamista.mobile.data.pending.PendingPaymentStatus
import com.sistemaprestamista.mobile.ui.components.rememberCurrency
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ScreenBackground = Color(0xFFF9F9FF)
private val CardBackground = Color(0xFFFFFFFF)
private val Primary = Color(0xFF00386C)
private val PrimaryContainer = Color(0xFF1A4F8B)
private val TextMain = Color(0xFF1A1C20)
private val TextVariant = Color(0xFF424750)
private val Success = Color(0xFF005236)
private val SuccessContainer = Color(0xFF6FFBBE)
private val Error = Color(0xFFBA1A1A)
private val ErrorContainer = Color(0xFFFFDAD6)
private val Warning = Color(0xFFF79009)
private val WarningContainer = Color(0xFFFFF4E5)
private val SurfaceContainerHigh = Color(0xFFE8E8ED)

@Composable
internal fun PendingPaymentsScreen(
    payments: List<PendingPayment>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onRetry: (String) -> Unit,
    onDiscard: (String) -> Unit,
) {
    val failedCount = payments.count { it.status == PendingPaymentStatus.Failed }
    val pendingCount = payments.count { it.status == PendingPaymentStatus.Pending }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            PendingHeader(
                pendingCount = pendingCount,
                failedCount = failedCount,
                isLoading = isLoading,
                onRefresh = onRefresh,
            )
        }

        if (payments.isEmpty()) {
            item {
                EmptyPendingCard()
            }
        } else {
            items(payments, key = { it.mobileUuid }) { payment ->
                PendingPaymentCard(
                    payment = payment,
                    isLoading = isLoading,
                    onRetry = { onRetry(payment.mobileUuid) },
                    onDiscard = { onDiscard(payment.mobileUuid) },
                )
            }
        }
    }
}

@Composable
private fun PendingHeader(
    pendingCount: Int,
    failedCount: Int,
    isLoading: Boolean,
    onRefresh: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Primary),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Cobros pendientes",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        text = "Revisa errores antes de descartar operaciones.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.82f),
                    )
                }

                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(26.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Sync,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HeaderMetric("En cola", pendingCount.toString(), Modifier.weight(1f))
                HeaderMetric("Con error", failedCount.toString(), Modifier.weight(1f))
            }

            Button(
                onClick = onRefresh,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Primary,
                ),
            ) {
                Icon(Icons.Outlined.Refresh, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Sincronizar ahora", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun HeaderMetric(
    title: String,
    value: String,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.76f))
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun PendingPaymentCard(
    payment: PendingPayment,
    isLoading: Boolean,
    onRetry: () -> Unit,
    onDiscard: () -> Unit,
) {
    val currency = rememberCurrency()
    val isFailed = payment.status == PendingPaymentStatus.Failed

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isFailed) ErrorContainer else WarningContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isFailed) Icons.Outlined.ErrorOutline else Icons.Outlined.Payments,
                            contentDescription = null,
                            tint = if (isFailed) Error else Warning,
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = currency.format(payment.amount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextMain,
                        )
                        Text(
                            text = "Prestamo #${payment.loanId} · ${payment.paymentDate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                StatusBadge(isFailed)
            }

            PaymentInfoRow("Metodo", payment.paymentMethod)
            PaymentInfoRow("Intentos", payment.attempts.toString())
            PaymentInfoRow("Actualizado", formatTimestamp(payment.updatedAt))

            if (isFailed) {
                Text(
                    text = payment.lastError ?: "El servidor rechazo este cobro.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Error,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Reintentar")
                }

                OutlinedButton(
                    onClick = onDiscard,
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading && isFailed,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Descartar")
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(isFailed: Boolean) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isFailed) ErrorContainer else WarningContainer)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (isFailed) "Revision" else "Pendiente",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isFailed) Error else Warning,
        )
    }
}

@Composable
private fun PaymentInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextMain)
    }
}

@Composable
private fun EmptyPendingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Outlined.Sync, contentDescription = null, tint = Success, modifier = Modifier.size(42.dp))
            Text("Sin cobros pendientes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = "Todas las operaciones locales estan sincronizadas.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextVariant,
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
}
