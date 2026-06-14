package com.sistemaprestamista.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale

@Composable
fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 12.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun StatusPill(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun EmptyCard(message: String) {
    Card(shape = RoundedCornerShape(18.dp)) {
        Text(
            text = message,
            modifier = Modifier.padding(18.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun LoadingSplash() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

/**
 * Código de moneda activo (RD$/US$), provisto en la raíz de la app desde la
 * configuración de la empresa. Las pantallas leen este valor vía rememberCurrency().
 */
val LocalCurrencyCode = staticCompositionLocalOf { "RD\$" }

/**
 * Formatea montos anteponiendo el símbolo real de la moneda de la empresa
 * (RD$, US$, etc.) en vez de forzar el peso dominicano.
 */
class MoneyFormatter(private val symbol: String) {
    private val numberFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-DO")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    fun format(amount: Number): String = "$symbol ${numberFormat.format(amount)}"
}

@Composable
fun rememberCurrency(): MoneyFormatter {
    val code = LocalCurrencyCode.current
    return remember(code) { MoneyFormatter(code.ifBlank { "RD\$" }) }
}

/** Traduce la frecuencia de pago del API (daily/weekly/...) al español. */
fun formatPaymentFrequency(frequency: String): String {
    return when (frequency.trim().lowercase()) {
        "daily" -> "Diario"
        "weekly" -> "Semanal"
        "biweekly" -> "Quincenal"
        "monthly" -> "Mensual"
        else -> frequency.replaceFirstChar { it.uppercase() }
    }
}

/** Etiqueta tipo "Cuota semanal" para acompañar el monto de la cuota. */
fun installmentFrequencyLabel(frequency: String): String {
    return when (frequency.trim().lowercase()) {
        "daily" -> "Cuota diaria"
        "weekly" -> "Cuota semanal"
        "biweekly" -> "Cuota quincenal"
        "monthly" -> "Cuota mensual"
        else -> "Cuota"
    }
}
