package com.sistemaprestamista.mobile.printing

enum class ThermalPaper(
    val label: String,
    val columns: Int,
) {
    Mm58("58 mm", 32),
    Mm88("88 mm", 48),
}

data class BluetoothPrinter(
    val name: String,
    val address: String,
)
