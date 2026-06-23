package com.sistemaprestamista.mobile.ui

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import java.util.Locale

/**
 * País reconocido a partir de un número de teléfono: bandera (emoji), nombre del país
 * y el número en formato internacional. Se usa para mostrar bajo el campo de teléfono
 * qué país/código de área detectó la app según lo que escribe el usuario.
 */
data class PhoneCountryInfo(
    val regionCode: String,
    val flag: String,
    val displayName: String,
    val formattedInternational: String,
)

/**
 * Detector basado en libphonenumber. La región por defecto (RD) se usa para interpretar
 * números locales sin prefijo internacional; el código de área distingue el país real
 * dentro del plan +1 (p. ej. 809/829/849 = RD, 305 = EE.UU.).
 */
class PhoneCountryDetector(context: Context) {
    private val util: PhoneNumberUtil = PhoneNumberUtil.createInstance(context.applicationContext)

    fun detect(raw: String, defaultRegion: String = "DO"): PhoneCountryInfo? {
        val cleaned = raw.trim()
        if (cleaned.count { it.isDigit() } < 7) return null

        return runCatching {
            val number = util.parse(cleaned, defaultRegion)
            if (!util.isPossibleNumber(number)) return null

            val region = util.getRegionCodeForNumber(number)
                ?: util.getRegionCodeForCountryCode(number.countryCode)
                ?: defaultRegion

            PhoneCountryInfo(
                regionCode = region,
                flag = flagEmoji(region),
                displayName = Locale("", region).getDisplayCountry(Locale("es")).ifBlank { region },
                formattedInternational = util.format(number, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL),
            )
        }.getOrNull()
    }

    /** Convierte un código ISO de país (p. ej. "DO") en su emoji de bandera. */
    private fun flagEmoji(region: String): String {
        if (region.length != 2) return "🏳️"
        val base = 0x1F1E6
        val first = base + (region[0].uppercaseChar() - 'A')
        val second = base + (region[1].uppercaseChar() - 'A')
        return String(Character.toChars(first)) + String(Character.toChars(second))
    }
}

/**
 * Recuerda el detector y reconoce el país del teléfono dado. Se recalcula al cambiar
 * el número. Devuelve null si aún no hay dígitos suficientes para reconocerlo.
 */
@Composable
internal fun rememberDetectedPhoneCountry(phone: String): PhoneCountryInfo? {
    val context = LocalContext.current
    val detector = remember { PhoneCountryDetector(context) }
    return remember(phone) { detector.detect(phone) }
}

/** Etiqueta bajo el campo de teléfono con el país/código de área reconocido. */
@Composable
internal fun PhoneCountryHint(phone: String, modifier: Modifier = Modifier) {
    val info = rememberDetectedPhoneCountry(phone) ?: return
    Text(
        text = "${info.flag} ${info.displayName} · ${info.formattedInternational}",
        style = MaterialTheme.typography.labelSmall,
        color = Color(0xFF008A5C),
        modifier = modifier,
    )
}
