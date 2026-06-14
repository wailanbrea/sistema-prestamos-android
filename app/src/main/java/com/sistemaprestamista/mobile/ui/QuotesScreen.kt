package com.sistemaprestamista.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sistemaprestamista.mobile.data.model.ClientSummary
import com.sistemaprestamista.mobile.data.model.LoanQuote
import com.sistemaprestamista.mobile.ui.components.formatPaymentFrequency
import com.sistemaprestamista.mobile.ui.components.rememberCurrency

private val ScreenBackground = Color(0xFFF4F7FB)
private val CardBackground = Color(0xFFFFFFFF)
private val Primary = Color(0xFF00386C)
private val PrimaryContainer = Color(0xFF1A4F8B)
private val PrimarySoft = Color(0xFFD0E1FB)
private val Secondary = Color(0xFF505F76)
private val TextMain = Color(0xFF1A1C20)
private val TextVariant = Color(0xFF424750)
private val DividerColor = Color(0xFFE2E8F0)
private val Success = Color(0xFF008A5C)
private val SuccessSoft = Color(0xFFE7F5ED)
private val Error = Color(0xFFBA1A1A)
private val ErrorSoft = Color(0xFFFFDAD6)

private val InterestTypes = listOf(
    "fixed" to "Fijo",
    "compound" to "Compuesto",
    "amortized" to "Amortizado",
)

private val Frequencies = listOf(
    "daily" to "Diario",
    "weekly" to "Semanal",
    "biweekly" to "Quincenal",
    "monthly" to "Mensual",
)

private val CalculationMethods = listOf(
    "flat_interest" to "Interés simple",
    "fixed_installment" to "Cuota fija",
    "capital_plus_interest" to "Capital + interés",
    "interest_only" to "Solo interés",
    "french_amortization" to "Amortización francesa",
)

// ---------------------------------------------------------------------------
// Listado de cotizaciones
// ---------------------------------------------------------------------------

@Composable
internal fun QuotesScreen(
    quotes: List<LoanQuote>,
    isLoading: Boolean,
    onOpenQuote: (Long) -> Unit,
    onCreateQuote: () -> Unit,
) {
    val currency = rememberCurrency()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Cotizaciones",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Primary,
                    )
                    Text(
                        text = "Simula préstamos antes de crearlos",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Secondary,
                    )
                }
            }

            if (isLoading && quotes.isEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
            } else if (quotes.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                    ) {
                        Text(
                            text = "Aún no hay cotizaciones. Crea la primera con el botón de abajo.",
                            modifier = Modifier.padding(18.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextVariant,
                        )
                    }
                }
            } else {
                items(quotes, key = { it.id }) { quote ->
                    QuoteCard(
                        quote = quote,
                        amount = currency.format(quote.amount),
                        installment = currency.format(quote.installmentAmount),
                        onOpenQuote = onOpenQuote,
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onCreateQuote,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = PrimaryContainer,
            contentColor = Color.White,
            icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
            text = { Text("Nueva cotización", fontWeight = FontWeight.Bold) },
        )
    }
}

@Composable
private fun QuoteCard(
    quote: LoanQuote,
    amount: String,
    installment: String,
    onOpenQuote: (Long) -> Unit,
) {
    Card(
        onClick = { onOpenQuote(quote.id) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = quote.clientName ?: "Sin cliente",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextMain,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false),
                )

                QuoteStatusBadge(status = quote.status)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                QuoteDataBlock(label = "Monto", value = amount, modifier = Modifier.weight(1f))
                QuoteDataBlock(
                    label = "Cuota ${formatPaymentFrequency(quote.paymentFrequency).lowercase()}",
                    value = installment,
                    modifier = Modifier.weight(1f),
                )
                QuoteDataBlock(label = "Cuotas", value = quote.termQuantity.toString(), modifier = Modifier.weight(0.5f))
            }
        }
    }
}

@Composable
private fun QuoteDataBlock(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextVariant,
            maxLines = 1,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = TextMain,
            maxLines = 1,
        )
    }
}

@Composable
private fun QuoteStatusBadge(status: String) {
    val (label, background, color) = when (status.trim().lowercase()) {
        "converted" -> Triple("Convertida", SuccessSoft, Success)
        "expired" -> Triple("Vencida", ErrorSoft, Error)
        else -> Triple("Pendiente", PrimarySoft, PrimaryContainer)
    }

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(background)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
        )
    }
}

// ---------------------------------------------------------------------------
// Formulario de nueva cotización
// ---------------------------------------------------------------------------

@Composable
internal fun QuoteFormScreen(
    clients: List<ClientSummary>,
    isSaving: Boolean,
    onSubmit: (Long?, Double, Double, String, String, String, Int) -> Unit,
) {
    var clientOption by remember {
        mutableStateOf<Pair<String, String>>("" to "Sin cliente (cotización libre)")
    }
    var amount by remember { mutableStateOf("") }
    var interestRate by remember { mutableStateOf("") }
    var interestType by remember { mutableStateOf(InterestTypes.first()) }
    var frequency by remember { mutableStateOf(Frequencies[1]) }
    var calculationMethod by remember { mutableStateOf(CalculationMethods.first()) }
    var termQuantity by remember { mutableStateOf("") }

    val clientOptions = remember(clients) {
        listOf("" to "Sin cliente (cotización libre)") +
                clients.map { it.id.toString() to it.fullName }
    }

    val parsedAmount = amount.toDoubleOrNull()
    val parsedRate = interestRate.toDoubleOrNull()
    val parsedTerm = termQuantity.toIntOrNull()
    val canSubmit = !isSaving &&
            parsedAmount != null && parsedAmount > 0 &&
            parsedRate != null && parsedRate >= 0 &&
            parsedTerm != null && parsedTerm > 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FormSectionCard(title = "Datos de la cotización") {
            OptionSelector(
                label = "Cliente (opcional)",
                options = clientOptions,
                selected = clientOption,
                onSelected = { clientOption = it },
            )

            FormField(value = amount, onValueChange = { amount = it }, label = "Monto a prestar *", keyboardType = KeyboardType.Decimal)
            FormField(value = interestRate, onValueChange = { interestRate = it }, label = "Tasa de interés (%) *", keyboardType = KeyboardType.Decimal)
            FormField(value = termQuantity, onValueChange = { termQuantity = it }, label = "Cantidad de cuotas *", keyboardType = KeyboardType.Number)
        }

        FormSectionCard(title = "Condiciones") {
            OptionSelector(
                label = "Frecuencia de pago",
                options = Frequencies,
                selected = frequency,
                onSelected = { frequency = it },
            )

            OptionSelector(
                label = "Tipo de interés",
                options = InterestTypes,
                selected = interestType,
                onSelected = { interestType = it },
            )

            OptionSelector(
                label = "Método de cálculo",
                options = CalculationMethods,
                selected = calculationMethod,
                onSelected = { calculationMethod = it },
            )
        }

        Button(
            onClick = {
                onSubmit(
                    clientOption.first.toLongOrNull(),
                    parsedAmount ?: return@Button,
                    parsedRate ?: return@Button,
                    interestType.first,
                    frequency.first,
                    calculationMethod.first,
                    parsedTerm ?: return@Button,
                )
            },
            enabled = canSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryContainer,
                contentColor = Color.White,
            ),
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color.White,
                )
            } else {
                Icon(Icons.Outlined.Calculate, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(
                    text = " Calcular y guardar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Detalle de cotización (resumen + cronograma)
// ---------------------------------------------------------------------------

@Composable
internal fun QuoteDetailScreen(
    quote: LoanQuote?,
    isLoading: Boolean,
    isDeleting: Boolean,
    onDelete: (Long) -> Unit,
) {
    val currency = rememberCurrency()
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (quote == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ScreenBackground),
            contentAlignment = Alignment.Center,
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Primary)
            } else {
                Text(
                    text = "Cotización no encontrada.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextVariant,
                )
            }
        }
        return
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar cotización") },
            text = { Text("¿Seguro que deseas eliminar esta cotización? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDelete(quote.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Error),
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            },
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryContainer),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = quote.clientName ?: "Cotización libre",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false),
                        )

                        QuoteStatusBadge(status = quote.status)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Cuota ${formatPaymentFrequency(quote.paymentFrequency).lowercase()}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.75f),
                        )
                        Text(
                            text = currency.format(quote.installmentAmount),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        QuoteHeroBlock("Monto", currency.format(quote.amount), Modifier.weight(1f))
                        QuoteHeroBlock("Interés total", currency.format(quote.totalInterest), Modifier.weight(1f))
                        QuoteHeroBlock("Total a pagar", currency.format(quote.totalAmount), Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            FormSectionCard(title = "Condiciones") {
                QuoteConditionRow("Tasa de interés", "${quote.interestRate}%")
                QuoteConditionRow("Tipo de interés", InterestTypes.firstOrNull { it.first == quote.interestType }?.second ?: quote.interestType)
                QuoteConditionRow("Frecuencia", formatPaymentFrequency(quote.paymentFrequency))
                QuoteConditionRow("Método de cálculo", CalculationMethods.firstOrNull { it.first == quote.calculationMethod }?.second ?: quote.calculationMethod)
                QuoteConditionRow("Cantidad de cuotas", quote.termQuantity.toString())
            }
        }

        if (quote.installments.isNotEmpty()) {
            item {
                Text(
                    text = "Cronograma de pagos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextMain,
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        quote.installments.forEachIndexed { index, installment ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "Cuota #${installment.number}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMain,
                                )

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = currency.format(installment.amount),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Primary,
                                    )
                                    Text(
                                        text = "Capital ${currency.format(installment.principal)} · Interés ${currency.format(installment.interest)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextVariant,
                                    )
                                }
                            }

                            if (index < quote.installments.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(DividerColor),
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    val interestTypeLabel = InterestTypes.firstOrNull { it.first == quote.interestType }?.second ?: quote.interestType
                    val frequencyLabel = formatPaymentFrequency(quote.paymentFrequency)
                    val methodLabel = CalculationMethods.firstOrNull { it.first == quote.calculationMethod }?.second ?: quote.calculationMethod
                    val message = buildString {
                        appendLine("*Cotización de Préstamo*")
                        appendLine()
                        quote.clientName?.let { appendLine("Cliente: $it") }
                        appendLine("Monto: ${currency.format(quote.amount)}")
                        appendLine("Cuota $frequencyLabel: ${currency.format(quote.installmentAmount)}")
                        appendLine("Cantidad de cuotas: ${quote.termQuantity}")
                        appendLine("Tasa de interés: ${quote.interestRate}%")
                        appendLine("Tipo: $interestTypeLabel · $methodLabel")
                        appendLine("Interés total: ${currency.format(quote.totalInterest)}")
                        append("Total a pagar: ${currency.format(quote.totalAmount)}")
                    }
                    val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        setPackage("com.whatsapp")
                        putExtra(Intent.EXTRA_TEXT, message)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    runCatching { context.startActivity(whatsappIntent) }.onFailure {
                        val fallbackUri = Uri.parse("https://wa.me/?text=${Uri.encode(message)}")
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, fallbackUri).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                },
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF25D366),
                    contentColor = Color.White,
                ),
            ) {
                Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Enviar por WhatsApp", fontWeight = FontWeight.Bold)
            }
        }

        if (quote.status.trim().lowercase() != "converted") {
            item {
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    enabled = !isDeleting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Error),
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        text = if (isDeleting) " Eliminando..." else " Eliminar cotización",
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuoteHeroBlock(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.75f),
            maxLines = 1,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
        )
    }
}

@Composable
private fun QuoteConditionRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
        )
    }
}
