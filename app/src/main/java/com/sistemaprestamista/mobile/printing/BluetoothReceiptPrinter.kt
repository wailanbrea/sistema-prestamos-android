package com.sistemaprestamista.mobile.printing

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.sistemaprestamista.mobile.data.model.PaymentReceipt
import java.nio.charset.Charset
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

class BluetoothReceiptPrinter(
    private val context: Context,
) {
    private val adapter: BluetoothAdapter?
        get() = context.getSystemService(BluetoothManager::class.java)?.adapter

    fun hasConnectPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun pairedPrinters(): List<BluetoothPrinter> {
        if (!hasConnectPermission()) {
            return emptyList()
        }

        return adapter
            ?.bondedDevices
            .orEmpty()
            .sortedBy { it.name ?: it.address }
            .map {
                BluetoothPrinter(
                    name = it.name ?: "Impresora ${it.address}",
                    address = it.address,
                )
            }
    }

    @SuppressLint("MissingPermission")
    fun printReceipt(
        printerAddress: String,
        receipt: PaymentReceipt,
        paper: ThermalPaper,
    ) {
        if (!hasConnectPermission()) {
            throw IllegalStateException("Permiso Bluetooth requerido.")
        }

        val device = adapter?.getRemoteDevice(printerAddress)
            ?: throw IllegalStateException("Bluetooth no disponible.")

        device.createRfcommSocketToServiceRecord(SPP_UUID).use { socket ->
            adapter?.cancelDiscovery()
            socket.connect()
            socket.outputStream.use { output ->
                output.write(receiptBytes(receipt, paper))
                output.flush()
            }
        }
    }

    private fun receiptBytes(receipt: PaymentReceipt, paper: ThermalPaper): ByteArray {
        val builder = StringBuilder()
        val width = paper.columns
        // Usa la moneda real del recibo (RD$/US$), no un símbolo fijo.
        val currency = com.sistemaprestamista.mobile.ui.components.MoneyFormatter(receipt.currency.ifBlank { "RD$" })

        builder.append(center("SISTEMA PRESTAMISTA", width)).append('\n')
        builder.append(center("RECIBO DE PAGO", width)).append('\n')
        builder.append(line(width)).append('\n')
        builder.append(row("Recibo", receipt.receiptNumber, width)).append('\n')
        builder.append(row("Fecha", receipt.paymentDate ?: "-", width)).append('\n')
        builder.append(row("Cliente", receipt.client?.fullName ?: "-", width)).append('\n')
        builder.append(row("Prestamo", receipt.loanNumber ?: receipt.loanId.toString(), width)).append('\n')
        builder.append(line(width)).append('\n')
        builder.append(row("Monto", currency.format(receipt.amount), width)).append('\n')
        builder.append(row("Capital", currency.format(receipt.principalPaid), width)).append('\n')
        builder.append(row("Interes", currency.format(receipt.interestPaid), width)).append('\n')
        builder.append(row("Mora", currency.format(receipt.lateFeePaid), width)).append('\n')
        builder.append(row("Balance ant.", currency.format(receipt.previousBalance), width)).append('\n')
        builder.append(row("Balance nuevo", currency.format(receipt.newBalance), width)).append('\n')
        builder.append(row("Metodo", receipt.paymentMethod, width)).append('\n')
        builder.append(line(width)).append('\n')
        builder.append(center("Conserve este recibo", width)).append('\n')
        builder.append("\n\n\n")

        return buildList {
            add(byteArrayOf(0x1B, 0x40))
            add(builder.toString().toByteArray(PRINTER_CHARSET))
            add(byteArrayOf(0x1D, 0x56, 0x42, 0x00))
        }.flatMap { it.asIterable() }.toByteArray()
    }

    private fun center(text: String, width: Int): String {
        val clean = text.take(width)
        val left = ((width - clean.length) / 2).coerceAtLeast(0)

        return " ".repeat(left) + clean
    }

    private fun line(width: Int): String = "-".repeat(width)

    private fun row(label: String, value: String, width: Int): String {
        val cleanLabel = label.take(14)
        val cleanValue = value.replace('\n', ' ')
        val available = (width - cleanLabel.length - 1).coerceAtLeast(1)

        return cleanLabel + " " + cleanValue.take(available).padStart(available)
    }

    private companion object {
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        val PRINTER_CHARSET: Charset = Charset.forName("CP437")
    }
}
