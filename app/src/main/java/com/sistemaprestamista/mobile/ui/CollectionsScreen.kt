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
internal fun CollectionsScreen(
    state: AppUiState,
    onRegisterPayment: (Long, String) -> Unit,
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
    onRegisterPayment: (Long, String) -> Unit,
    onOpenInstallment: (Long) -> Unit,
) {
    var amount by remember(installment.id) {
        mutableStateOf("%.2f".format(Locale.US, installment.pendingAmount))
    }
    val currency = rememberCurrency()
    val isLate = installment.daysLate > 0

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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(14.dp),
            )
            Button(
                onClick = { onRegisterPayment(installment.loanId, amount) },
                enabled = !isLoading,
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
}


