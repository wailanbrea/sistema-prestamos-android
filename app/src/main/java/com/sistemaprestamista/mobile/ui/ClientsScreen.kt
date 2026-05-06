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
internal fun ClientsScreen(
    clients: List<ClientSummary>,
    onOpenClient: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (clients.isEmpty()) {
            item { EmptyCard("No hay clientes asignados para este cobrador.") }
        } else {
            items(clients, key = { it.id }) { client ->
                ClientCard(client, onOpenClient)
            }
        }
    }
}

@Composable
private fun ClientCard(
    client: ClientSummary,
    onOpenClient: (Long) -> Unit,
) {
    Card(shape = RoundedCornerShape(18.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(client.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = listOfNotNull(client.phone, client.identification).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!client.address.isNullOrBlank()) {
                Text(client.address, style = MaterialTheme.typography.bodyMedium)
            }
            StatusPill("${client.status} · riesgo ${client.riskLevel}")
            OutlinedButton(
                onClick = { onOpenClient(client.id) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Ver expediente")
            }
        }
    }
}


