package com.sistemaprestamista.mobile.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.sistemaprestamista.mobile.data.model.ClientSummary
import com.sistemaprestamista.mobile.data.model.DashboardSummary
import com.sistemaprestamista.mobile.data.model.InstallmentSummary
import com.sistemaprestamista.mobile.data.model.LoanSummary
import com.sistemaprestamista.mobile.data.model.PaymentReceipt
import com.sistemaprestamista.mobile.data.model.UserProfile
import com.sistemaprestamista.mobile.printing.A4ReceiptPrinter
import com.sistemaprestamista.mobile.printing.BluetoothPrinter
import com.sistemaprestamista.mobile.printing.BluetoothReceiptPrinter
import com.sistemaprestamista.mobile.printing.PrintSettingsStore
import com.sistemaprestamista.mobile.printing.ThermalPaper
import com.sistemaprestamista.mobile.ui.components.EmptyCard
import com.sistemaprestamista.mobile.ui.components.MetricCard
import com.sistemaprestamista.mobile.ui.components.StatusPill
import com.sistemaprestamista.mobile.ui.components.rememberCurrency
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
@Composable
internal fun ClientDetailScreen(
    client: ClientSummary?,
    loans: List<LoanSummary>,
    installments: List<InstallmentSummary>,
    onOpenInstallment: (Long) -> Unit,
) {
    if (client == null) {
        EmptyCard("No se encontró el cliente seleccionado.")
        return
    }

    val currency = rememberCurrency()
    val activeBalance = loans.sumOf { it.remainingBalance }
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
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Balance", currency.format(activeBalance), Modifier.weight(1f))
                MetricCard("Pendiente", currency.format(pendingAmount), Modifier.weight(1f))
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


