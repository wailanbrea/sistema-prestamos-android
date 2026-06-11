package com.sistemaprestamista.mobile.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import com.sistemaprestamista.mobile.data.model.PaymentReceipt
import com.sistemaprestamista.mobile.printing.A4ReceiptPrinter
import com.sistemaprestamista.mobile.printing.BluetoothPrinter
import com.sistemaprestamista.mobile.printing.BluetoothReceiptPrinter
import com.sistemaprestamista.mobile.printing.PrintSettingsStore
import com.sistemaprestamista.mobile.printing.ThermalPaper
import com.sistemaprestamista.mobile.ui.components.rememberCurrency
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val ScreenBackground = Color(0xFFF4F7FB)
private val CardBackground = Color(0xFFFFFFFF)
private val Primary = Color(0xFF00386C)
private val PrimaryContainer = Color(0xFF1A4F8B)
private val SecondaryContainer = Color(0xFFD0E1FB)
private val SurfaceContainerLow = Color(0xFFF3F3F9)
private val SurfaceContainer = Color(0xFFEDEDF3)
private val TextMain = Color(0xFF1A1C20)
private val TextVariant = Color(0xFF424750)
private val OutlineVariant = Color(0xFFC2C6D1)
private val Success = Color(0xFF005236)
private val SuccessSoft = Color(0xFF6FFBBE)
private val Error = Color(0xFFBA1A1A)

@Composable
internal fun ReceiptDetailScreen(
    receipt: PaymentReceipt?,
    printSettingsStore: PrintSettingsStore,
    canCancelPayment: Boolean = false,
    isCancelling: Boolean = false,
    onCancelPayment: ((paymentId: Long, reason: String) -> Unit)? = null,
) {
    if (receipt == null) {
        LoadingReceiptState()
        return
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val bluetoothPrinter = remember(context) { BluetoothReceiptPrinter(context.applicationContext) }

    var selectedPaper by remember { mutableStateOf(printSettingsStore.thermalPaper()) }
    var printers by remember { mutableStateOf<List<BluetoothPrinter>>(emptyList()) }
    var selectedPrinter by remember { mutableStateOf(printSettingsStore.selectedPrinter()) }
    var printMessage by remember { mutableStateOf<String?>(null) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelReason by remember { mutableStateOf("") }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Anular pago #${receipt.receiptNumber}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Escribe la razón de la anulación (mínimo 10 caracteres).")
                    OutlinedTextField(
                        value = cancelReason,
                        onValueChange = { cancelReason = it },
                        label = { Text("Razón") },
                        singleLine = false,
                        minLines = 2,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelDialog = false
                        onCancelPayment?.invoke(receipt.id, cancelReason)
                    },
                    enabled = cancelReason.trim().length >= 10,
                    colors = ButtonDefaults.buttonColors(containerColor = Error),
                ) { Text("Anular") }
            },
            dismissButton = { TextButton(onClick = { showCancelDialog = false; cancelReason = "" }) { Text("Cancelar") } },
        )
    }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            printers = bluetoothPrinter.pairedPrinters()
            selectedPrinter = selectedPrinter ?: printers.firstOrNull()
        } else {
            printMessage = "Permiso Bluetooth denegado."
        }
    }

    // Auto-cargar impresoras vinculadas al abrir el recibo (si ya hay permiso)
    // sin sobreescribir la impresora guardada por el usuario.
    LaunchedEffect(Unit) {
        if (bluetoothPrinter.hasConnectPermission()) {
            printers = bluetoothPrinter.pairedPrinters()
            selectedPrinter = selectedPrinter ?: printers.firstOrNull()
        }
    }

    val currency = rememberCurrency()
    val isCancelled = receipt.status.equals("cancelled", ignoreCase = true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        ReceiptHeaderCard(receipt = receipt)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AmountSummaryCard(
                title = "Monto pagado",
                value = currency.format(receipt.amount),
                valueColor = Success,
                borderColor = SuccessSoft,
                modifier = Modifier.weight(1f),
            )

            AmountSummaryCard(
                title = "Balance restante",
                value = currency.format(receipt.newBalance),
                valueColor = if (receipt.newBalance > 0.0) Error else TextMain,
                borderColor = OutlineVariant,
                modifier = Modifier.weight(1f),
            )
        }

        PaymentApplicationCard(
            principalPaid = currency.format(receipt.principalPaid),
            interestPaid = currency.format(receipt.interestPaid),
            lateFeePaid = currency.format(receipt.lateFeePaid),
            previousBalance = currency.format(receipt.previousBalance),
            paymentMethod = receipt.paymentMethod,
            status = receipt.status,
            isCancelled = isCancelled,
        )

        receipt.commission?.let { commission ->
            CommissionDetailCard(
                commissionType = commission.commissionType,
                commissionValue = commission.commissionValue,
                baseAmount = currency.format(commission.baseAmount),
                commissionAmount = currency.format(commission.commissionAmount),
                status = commission.status,
                paidAt = commission.paidAt,
            )
        }

        if (receipt.details.isNotEmpty()) {
            InstallmentDetailsCard(
                receipt = receipt,
                formatAmount = { currency.format(it) },
            )
        }

        if (canCancelPayment && !isCancelled && onCancelPayment != null) {
            OutlinedButton(
                onClick = { showCancelDialog = true },
                enabled = !isCancelling,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Error),
            ) {
                if (isCancelling) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Error)
                } else {
                    Icon(Icons.Outlined.Block, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Anular pago", fontWeight = FontWeight.Bold)
                }
            }
        }

        receipt.whatsappUrl?.let { url ->
            Button(
                onClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF25D366),
                    contentColor = Color.White,
                ),
            ) {
                Icon(Icons.Outlined.Description, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(
                    text = "Enviar por WhatsApp",
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        FilledTonalButton(
            onClick = { A4ReceiptPrinter(context).printReceipt(receipt) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(22.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = SecondaryContainer,
                contentColor = Primary,
            ),
        ) {
            Icon(Icons.Outlined.PictureAsPdf, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text(
                text = "Imprimir A4 (PDF)",
                fontWeight = FontWeight.Bold,
            )
        }

        ThermalPrinterCard(
            selectedPaper = selectedPaper,
            printers = printers,
            selectedPrinter = selectedPrinter,
            printMessage = printMessage,
            onPaperSelected = { paper ->
                selectedPaper = paper
                printSettingsStore.saveThermalPaper(paper)
            },
            onSearchPrinters = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !bluetoothPrinter.hasConnectPermission()) {
                    bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                } else {
                    printers = bluetoothPrinter.pairedPrinters()
                    selectedPrinter = selectedPrinter ?: printers.firstOrNull()

                    if (printers.isEmpty()) {
                        printMessage = "No hay impresoras Bluetooth vinculadas."
                    }
                }
            },
            onPrinterSelected = { printer ->
                selectedPrinter = printer
                printSettingsStore.savePrinter(printer, selectedPaper)
                printMessage = "Impresora predeterminada: ${printer.name}."
            },
            onPrintThermal = {
                val printer = selectedPrinter

                if (printer == null) {
                    printMessage = "Selecciona una impresora vinculada."
                    return@ThermalPrinterCard
                }

                scope.launch {
                    printMessage = "Imprimiendo..."

                    runCatching {
                        withContext(Dispatchers.IO) {
                            bluetoothPrinter.printReceipt(
                                printer.address,
                                receipt,
                                selectedPaper,
                            )
                        }
                    }.onSuccess {
                        printMessage = "Recibo enviado a ${printer.name}."
                    }.onFailure {
                        printMessage = it.message ?: "No se pudo imprimir el recibo."
                    }
                }
            },
        )
    }
}

@Composable
private fun ReceiptHeaderCard(
    receipt: PaymentReceipt,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryContainer),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(150.dp)
                    .offset(x = 42.dp, y = 42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f)),
            )
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "RECIBO DE PAGO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.75f),
                        )

                        Text(
                            text = receipt.receiptNumber,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(34.dp),
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    Column {
                        Text(
                            text = "Cliente",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.75f),
                        )

                        Text(
                            text = receipt.client?.fullName ?: "Cliente no disponible",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AmountSummaryCard(
    title: String,
    value: String,
    valueColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.height(105.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(0.05f)
                    .background(borderColor),
            )

            Column(
                modifier = Modifier
                    .weight(0.95f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextVariant,
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = valueColor,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun PaymentApplicationCard(
    principalPaid: String,
    interestPaid: String,
    lateFeePaid: String,
    previousBalance: String,
    paymentMethod: String,
    status: String,
    isCancelled: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Aplicación del pago",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextMain,
                )

                StatusBadge(
                    text = if (isCancelled) "Anulado" else "Válido",
                    isCancelled = isCancelled,
                )
            }

            ReceiptInfoRow("Capital", principalPaid)
            ReceiptInfoRow("Interés", interestPaid)
            ReceiptInfoRow("Mora", lateFeePaid)
            ReceiptInfoRow("Balance anterior", previousBalance)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Método",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextVariant,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Payments,
                        contentDescription = null,
                        tint = TextMain,
                        modifier = Modifier.size(20.dp),
                    )

                    Text(
                        text = paymentMethod,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextMain,
                    )
                }
            }

            ReceiptInfoRow("Estado", status, showDivider = false)
        }
    }
}

@Composable
private fun ReceiptInfoRow(
    label: String,
    value: String,
    showDivider: Boolean = true,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextVariant,
            )

            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = TextMain,
                maxLines = 1,
            )
        }

        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(OutlineVariant.copy(alpha = 0.35f)),
            )
        }
    }
}

@Composable
private fun StatusBadge(
    text: String,
    isCancelled: Boolean,
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isCancelled) Color(0xFFFFDAD6) else SuccessSoft)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isCancelled) Error else Success,
        )
    }
}

@Composable
private fun CommissionDetailCard(
    commissionType: String,
    commissionValue: Double,
    baseAmount: String,
    commissionAmount: String,
    status: String,
    paidAt: String?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Comision del cobrador",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextMain,
            )

            ReceiptInfoRow("Base", baseAmount)
            ReceiptInfoRow("Comision", commissionAmount)
            ReceiptInfoRow("Tipo", commissionType.ifBlank { "-" })
            ReceiptInfoRow("Valor", commissionValue.toString())
            ReceiptInfoRow("Estado", status.ifBlank { "pendiente" }, showDivider = paidAt != null)

            paidAt?.let {
                ReceiptInfoRow("Pagada en", it, showDivider = false)
            }
        }
    }
}

@Composable
private fun InstallmentDetailsCard(
    receipt: PaymentReceipt,
    formatAmount: (Double) -> String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Detalle por cuota",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextMain,
            )

            receipt.details.forEach { detail ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceContainerLow)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFD5E3FF)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "#${detail.installmentNumber ?: detail.installmentId}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Primary,
                                maxLines = 1,
                            )
                        }

                        Column {
                            Text(
                                text = "Cuota ${detail.installmentNumber ?: detail.installmentId}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextMain,
                            )

                            Text(
                                text = "Aplicación de pago",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextVariant,
                            )
                        }
                    }

                    Text(
                        text = formatAmount(detail.amountPaid),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Primary,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun ThermalPrinterCard(
    selectedPaper: ThermalPaper,
    printers: List<BluetoothPrinter>,
    selectedPrinter: BluetoothPrinter?,
    printMessage: String?,
    onPaperSelected: (ThermalPaper) -> Unit,
    onSearchPrinters: () -> Unit,
    onPrinterSelected: (BluetoothPrinter) -> Unit,
    onPrintThermal: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
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
                Column {
                    Text(
                        text = "Impresora térmica",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextMain,
                    )

                    Text(
                        text = "Selecciona papel e impresora Bluetooth",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextVariant,
                    )
                }

                Icon(
                    imageVector = Icons.Outlined.Print,
                    contentDescription = null,
                    tint = TextVariant,
                )
            }

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth(),
            ) {
                ThermalPaper.entries.forEachIndexed { index, paper ->
                    SegmentedButton(
                        selected = selectedPaper == paper,
                        onClick = { onPaperSelected(paper) },
                        shape = SegmentedButtonDefaults.itemShape(index, ThermalPaper.entries.size),
                    ) {
                        Text(
                            text = paper.label,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = onSearchPrinters,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Primary,
                ),
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(
                    text = "Buscar vinculadas",
                    fontWeight = FontWeight.Bold,
                )
            }

            if (printers.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    printers.forEach { printer ->
                        PrinterItem(
                            printer = printer,
                            selected = selectedPrinter?.address == printer.address,
                            onClick = { onPrinterSelected(printer) },
                        )
                    }
                }
            }

            Button(
                onClick = onPrintThermal,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = Color.White,
                ),
            ) {
                Icon(Icons.Outlined.Print, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(
                    text = "Imprimir ${selectedPaper.label}",
                    fontWeight = FontWeight.Bold,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (selectedPrinter != null) SuccessSoft else OutlineVariant),
                )

                Spacer(Modifier.size(8.dp))

                Text(
                    text = selectedPrinter?.let { "Predeterminada: ${it.name} · ${selectedPaper.label}" }
                        ?: "Sin impresora predeterminada.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextVariant,
                )
            }

            if (printMessage != null) {
                Text(
                    text = printMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = TextVariant,
                )
            }
        }
    }
}

@Composable
private fun PrinterItem(
    printer: BluetoothPrinter,
    selected: Boolean,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) Primary.copy(alpha = 0.06f) else Color.White,
            contentColor = if (selected) Primary else TextMain,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Print,
                    contentDescription = null,
                    tint = if (selected) Primary else TextVariant,
                )

                Text(
                    text = printer.name,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                )
            }

            if (selected) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = Primary,
                )
            }
        }
    }
}

@Composable
private fun EmptyReceiptState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.72f)),
            border = CardDefaults.outlinedCardBorder(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(82.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                        contentDescription = null,
                        tint = TextVariant,
                        modifier = Modifier.size(42.dp),
                    )
                }

                Text(
                    text = "No hay recibos recientes",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextMain,
                )

                Text(
                    text = "Todavía no hay un recibo generado en esta sesión.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextVariant,
                )
            }
        }
    }
}
@Composable
private fun LoadingReceiptState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CircularProgressIndicator(color = Primary)

                Text(
                    text = "Cargando recibo...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextMain,
                )

                Text(
                    text = "Estamos obteniendo los datos del pago.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextVariant,
                )
            }
        }
    }
}
