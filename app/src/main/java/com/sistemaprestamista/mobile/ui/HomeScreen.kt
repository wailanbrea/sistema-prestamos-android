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
internal fun HomeScreen(
    state: AppUiState,
    onOpenReceipt: () -> Unit,
) {
    val dashboard = state.dashboard

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            WelcomeCard(state)
        }
        if (dashboard != null) {
            item { FinanceOverview(dashboard) }
        }
        item { CollectorOverview(state) }
        item { LastReceiptCard(state, onOpenReceipt) }
    }
}

@Composable
private fun WelcomeCard(state: AppUiState) {
    val user = state.user ?: return

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Hola, ${user.name}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = user.roles.joinToString().ifBlank { "Sin rol asignado" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun FinanceOverview(summary: DashboardSummary) {
    val currency = rememberCurrency()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("Capital", currency.format(summary.capitalPrestado), Modifier.weight(1f))
            MetricCard("Cobros hoy", currency.format(summary.cobrosHoy), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("Ganancia", currency.format(summary.gananciaNeta), Modifier.weight(1f))
            MetricCard("En mora", summary.prestamosMora.toString(), Modifier.weight(1f))
        }
    }
}

@Composable
private fun CollectorOverview(state: AppUiState) {
    val summary = state.collectorSummary ?: return
    val currency = rememberCurrency()

    Card(shape = RoundedCornerShape(18.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Ruta de cobro", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Clientes", summary.assignedClients.toString(), Modifier.weight(1f), compact = true)
                MetricCard("Cuotas", summary.pendingInstallments.toString(), Modifier.weight(1f), compact = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Atrasos", summary.lateLoans.toString(), Modifier.weight(1f), compact = true)
                MetricCard("Hoy", currency.format(summary.collectedToday), Modifier.weight(1f), compact = true)
            }
        }
    }
}


