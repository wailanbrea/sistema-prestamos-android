package com.sistemaprestamista.mobile.printing

import com.sistemaprestamista.mobile.data.SessionStore

class PrintSettingsStore(
    private val sessionStore: SessionStore,
) {
    fun selectedPrinter(): BluetoothPrinter? {
        val address = sessionStore.printerAddress() ?: return null
        val name = sessionStore.printerName() ?: address

        return BluetoothPrinter(name = name, address = address)
    }

    fun thermalPaper(): ThermalPaper = sessionStore.thermalPaper()

    fun savePrinter(printer: BluetoothPrinter, paper: ThermalPaper) {
        sessionStore.savePrinter(
            address = printer.address,
            name = printer.name,
            paper = paper,
        )
    }

    fun saveThermalPaper(paper: ThermalPaper) {
        sessionStore.saveThermalPaper(paper)
    }

    fun clearPrinter() {
        sessionStore.clearPrinter()
    }
}
