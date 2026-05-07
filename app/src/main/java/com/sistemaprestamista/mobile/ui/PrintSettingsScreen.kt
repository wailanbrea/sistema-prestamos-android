package com.sistemaprestamista.mobile.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import com.sistemaprestamista.mobile.data.model.ClientSummary
import com.sistemaprestamista.mobile.data.model.PaymentReceipt
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.BluetoothDisabled
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.PrintDisabled
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sistemaprestamista.mobile.printing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Bg = Color(0xFFF4F7FB)
private val Primary = Color(0xFF00386C)
private val PrimaryContainer = Color(0xFF1A4F8B)
private val Success = Color(0xFF16A34A)
private val WarningBg = Color(0xFFFFFBEB)
private val WarningText = Color(0xFF92400E)

@Composable
internal fun PrintSettingsScreen(
    printSettingsStore: PrintSettingsStore
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    val bluetoothPrinter = remember {
        BluetoothReceiptPrinter(context.applicationContext)
    }

    var selectedPaper by remember {
        mutableStateOf(printSettingsStore.thermalPaper())
    }

    var printers by remember { mutableStateOf<List<BluetoothPrinter>>(emptyList()) }
    var selectedPrinter by remember { mutableStateOf(printSettingsStore.selectedPrinter()) }
    var message by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
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
            .background(Bg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // 🔹 HEADER
        Card(shape = RoundedCornerShape(24.dp)) {
            Row(Modifier.padding(16.dp)) {
                Icon(Icons.Outlined.Print, null, tint = Primary)
                Spacer(Modifier.width(12.dp))

                Column {
                    Text("Configuración de impresión", fontWeight = FontWeight.Bold)
                    Text("Selecciona la impresora y tamaño térmico")
                }
            }
        }

        // 🔹 ALERTA PERMISO
        if (message?.contains("denegado", true) == true) {
            Card(
                colors = CardDefaults.cardColors(containerColor = WarningBg)
            ) {
                Row(Modifier.padding(16.dp)) {
                    Icon(Icons.Outlined.BluetoothDisabled, null, tint = WarningText)
                    Spacer(Modifier.width(8.dp))
                    Text(message ?: "", color = WarningText)
                }
            }
        }

        // 🔹 PAPEL
        Card(shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Papel térmico", fontWeight = FontWeight.Bold)

                Spacer(Modifier.height(12.dp))

                Row {
                    ThermalPaper.entries.forEach {
                        Button(
                            onClick = {
                                selectedPaper = it
                                printSettingsStore.saveThermalPaper(it)
                                selectedPrinter?.let { printer ->
                                    printSettingsStore.savePrinter(printer, it)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (it == selectedPaper) PrimaryContainer else Color.LightGray
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(it.label)
                        }
                    }
                }
            }
        }

        // 🔹 IMPRESORAS
        Card(shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(16.dp)) {

                Text("Impresora Bluetooth", fontWeight = FontWeight.Bold)

                Text(
                    selectedPrinter?.let { "Actual: ${it.name}" }
                        ?: "No hay impresora seleccionada"
                )

                Spacer(Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                            !bluetoothPrinter.hasConnectPermission()
                        ) {
                            permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                        } else {
                            printers = bluetoothPrinter.pairedPrinters()
                            if (printers.isEmpty()) {
                                message = "No hay impresoras vinculadas."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Buscar impresoras")
                }

                Spacer(Modifier.height(10.dp))

                printers.forEach { printer ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row {
                                Icon(Icons.Outlined.Bluetooth, null)
                                Spacer(Modifier.width(8.dp))
                                Text(printer.name)
                            }

                            if (selectedPrinter?.address == printer.address) {
                                Icon(Icons.Outlined.Check, null, tint = Success)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        selectedPrinter = null
                        printSettingsStore.clearPrinter()
                        message = "Impresora eliminada"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Quitar predeterminada")
                }
            }
        }
         fun sampleReceipt(): PaymentReceipt {
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
                    latitude = null,
                    longitude = null,
                    locationReference = null,
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

        // 🔹 PRUEBA
        Card(shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(16.dp)) {

                Button(
                    onClick = {
                        val printer = selectedPrinter
                        if (printer == null) {
                            message = "Selecciona una impresora"
                            return@Button
                        }

                        scope.launch {
                            message = "Enviando prueba..."

                            runCatching {
                                withContext(Dispatchers.IO) {
                                    bluetoothPrinter.printReceipt(
                                        printer.address,
                                        sampleReceipt(),
                                        selectedPaper
                                    )
                                }
                            }.onSuccess {
                                message = "Prueba enviada a ${printer.name}"
                            }.onFailure {
                                message = "Error al imprimir"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Imprimir prueba ${selectedPaper.label}")
                }

                message?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it)
                }
            }
        }

        // 🔹 EMPTY STATE
        if (printers.isEmpty()) {
            Card {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Outlined.PrintDisabled, null)
                    Text("No hay impresoras Bluetooth")
                }
            }
        }
    }
}
