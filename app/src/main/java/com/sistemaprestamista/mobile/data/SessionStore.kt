package com.sistemaprestamista.mobile.data

import android.content.Context
import com.sistemaprestamista.mobile.printing.ThermalPaper

class SessionStore(context: Context) {
    private val preferences = context.getSharedPreferences("session", Context.MODE_PRIVATE)

    fun token(): String? = preferences.getString(KEY_TOKEN, null)

    fun saveToken(token: String) {
        preferences.edit().putString(KEY_TOKEN, token).apply()
    }

    fun printerAddress(): String? = preferences.getString(KEY_PRINTER_ADDRESS, null)

    fun printerName(): String? = preferences.getString(KEY_PRINTER_NAME, null)

    fun thermalPaper(): ThermalPaper {
        val saved = preferences.getString(KEY_THERMAL_PAPER, ThermalPaper.Mm58.name)

        return ThermalPaper.entries.firstOrNull { it.name == saved } ?: ThermalPaper.Mm58
    }

    fun savePrinter(
        address: String,
        name: String,
        paper: ThermalPaper,
    ) {
        preferences.edit()
            .putString(KEY_PRINTER_ADDRESS, address)
            .putString(KEY_PRINTER_NAME, name)
            .putString(KEY_THERMAL_PAPER, paper.name)
            .apply()
    }

    fun saveThermalPaper(paper: ThermalPaper) {
        preferences.edit().putString(KEY_THERMAL_PAPER, paper.name).apply()
    }

    fun clearPrinter() {
        preferences.edit()
            .remove(KEY_PRINTER_ADDRESS)
            .remove(KEY_PRINTER_NAME)
            .apply()
    }

    fun clear() {
        preferences.edit().remove(KEY_TOKEN).apply()
    }

    private companion object {
        const val KEY_TOKEN = "access_token"
        const val KEY_PRINTER_ADDRESS = "printer_address"
        const val KEY_PRINTER_NAME = "printer_name"
        const val KEY_THERMAL_PAPER = "thermal_paper"
    }
}
