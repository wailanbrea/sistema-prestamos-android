package com.sistemaprestamista.mobile.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.sistemaprestamista.mobile.printing.ThermalPaper

class SessionStore(context: Context) {
    private val appContext = context.applicationContext

    // EncryptedSharedPreferences/Keystore es costoso de inicializar (~cientos de ms
    // en frío). Diferido con `by lazy` para que esa operacion NO ocurra al construir
    // el contenedor en el hilo principal (causaba jank de arranque); el primer acceso
    // real ocurre en una corrutina de IO (prepareSession / requiredToken).
    private val preferences by lazy { securePreferences(appContext) }

    fun token(): String? = preferences.getString(KEY_TOKEN, null)

    fun hasToken(): Boolean = !token().isNullOrBlank()

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

    private fun securePreferences(context: Context): SharedPreferences {
        return runCatching {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }.getOrElse {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    private companion object {
        const val PREFS_NAME = "secure_session"
        const val KEY_TOKEN = "access_token"
        const val KEY_PRINTER_ADDRESS = "printer_address"
        const val KEY_PRINTER_NAME = "printer_name"
        const val KEY_THERMAL_PAPER = "thermal_paper"
    }
}
