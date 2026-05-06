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
internal fun PrintSettingsScreen(printSettingsStore: PrintSettingsStore) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val bluetoothPrinter = remember(context) { BluetoothReceiptPrinter(context.applicationContext) }
    var selectedPaper by remember { mutableStateOf(printSettingsStore.thermalPaper()) }
    var printers by remember { mutableStateOf<List<BluetoothPrinter>>(emptyList()) }
    var selectedPrinter by remember { mutableStateOf(printSettingsStore.selectedPrinter()) }
    var message by remember { mutableStateOf<String?>(null) }
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            printers = bluetoothPrinter.pairedPrinters()
            selectedPrinter = selectedPrinter ?: printers.firstOrNull()
        } else {
            message = "Permiso Bluetooth denegado."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Card(shape = RoundedCornerShape(18.dp)) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Configuración de impresión", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    text = "Selecciona la impresora predeterminada y el tamaño térmico para recibos. Los documentos importantes se imprimen en A4 desde la pantalla del recibo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Card(shape = RoundedCornerShape(18.dp)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Papel térmico por defecto", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ThermalPaper.entries.forEachIndexed { index, paper ->
                        SegmentedButton(
                            selected = selectedPaper == paper,
                            onClick = {
                                selectedPaper = paper
                                printSettingsStore.saveThermalPaper(paper)
                                selectedPrinter?.let { printSettingsStore.savePrinter(it, paper) }
                            },
                            shape = SegmentedButtonDefaults.itemShape(index, ThermalPaper.entries.size),
                        ) {
                            Text(paper.label)
                        }
                    }
                }
            }
        }
        Card(shape = RoundedCornerShape(18.dp)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Impresora Bluetooth", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    text = selectedPrinter?.let { "Actual: ${it.name}" } ?: "No hay impresora predeterminada.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !bluetoothPrinter.hasConnectPermission()) {
                            bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                        } else {
                            printers = bluetoothPrinter.pairedPrinters()
                            selectedPrinter = selectedPrinter ?: printers.firstOrNull()
                            if (printers.isEmpty()) {
                                message = "No hay impresoras Bluetooth vinculadas."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Buscar impresoras vinculadas")
                }
                printers.forEach { printer ->
                    OutlinedButton(
                        onClick = {
                            selectedPrinter = printer
                            printSettingsStore.savePrinter(printer, selectedPaper)
                            message = "Guardada: ${printer.name} · ${selectedPaper.label}."
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(if (selectedPrinter?.address == printer.address) "✓ ${printer.name}" else printer.name)
                    }
                }
                OutlinedButton(
                    onClick = {
                        selectedPrinter = null
                        printSettingsStore.clearPrinter()
                        message = "Impresora predeterminada eliminada."
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Quitar predeterminada")
                }
            }
        }
        Card(shape = RoundedCornerShape(18.dp)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Prueba", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Button(
                    onClick = {
                        val printer = selectedPrinter
                        if (printer == null) {
                            message = "Selecciona una impresora antes de probar."
                            return@Button
                        }

                        scope.launch {
                            message = "Enviando prueba..."
                            runCatching {
                                withContext(Dispatchers.IO) {
                                    bluetoothPrinter.printReceipt(
                                        printerAddress = printer.address,
                                        receipt = sampleReceipt(),
                                        paper = selectedPaper,
                                    )
                                }
                            }.onSuccess {
                                message = "Prueba enviada a ${printer.name}."
                            }.onFailure {
                                message = it.message ?: "No se pudo imprimir la prueba."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Imprimir prueba ${selectedPaper.label}")
                }
                if (message != null) {
                    Text(
                        text = message.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun sampleReceipt(): PaymentReceipt {
    return PaymentReceipt(
        id = 0,
        receiptNumber = "PRUEBA",
        loanId = 0,
        loanNumber = "PRE-PRUEBA",
        client = ClientSummary(
            id = 0,
            fullName = "Cliente de prueba",
            identification = null,
            phone = null,
            address = null,
            status = "active",
            riskLevel = "low",
        ),
        paymentDate = "2026-05-06",
        amount = 100.0,
        principalPaid = 80.0,
        interestPaid = 20.0,
        lateFeePaid = 0.0,
        previousBalance = 500.0,
        newBalance = 420.0,
        paymentMethod = "cash",
        status = "valid",
    )
}



