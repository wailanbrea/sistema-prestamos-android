package com.sistemaprestamista.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sistemaprestamista.mobile.data.model.ClientSummary
import com.sistemaprestamista.mobile.data.model.CollectorOption
import com.sistemaprestamista.mobile.data.model.NewLoanInput
import java.time.LocalDate

private val ScreenBackground = Color(0xFFF4F7FB)
private val PrimaryLoan = Color(0xFF00386C)
private val PrimaryContainerLoan = Color(0xFF1A4F8B)

private val Currencies = listOf("RD$" to "Peso dominicano (RD\$)", "US$" to "Dólar (US\$)")

private val InterestTypes = listOf(
    "fixed" to "Fijo",
    "compound" to "Compuesto",
    "amortized" to "Amortizado",
)

private val PaymentFrequencies = listOf(
    "daily" to "Diario",
    "weekly" to "Semanal",
    "biweekly" to "Quincenal",
    "monthly" to "Mensual",
)

private val CalculationMethods = listOf(
    "flat_interest" to "Interés plano",
    "fixed_installment" to "Cuota fija",
    "capital_plus_interest" to "Capital + interés",
    "interest_only" to "Solo interés",
    "french_amortization" to "Amortización francesa",
)

private val LateFeeTypes = listOf(
    "none" to "Sin mora",
    "fixed" to "Fijo",
    "daily_percentage" to "Porcentaje diario",
    "daily_fixed" to "Monto fijo diario",
)

@Composable
internal fun LoanCreateScreen(
    clients: List<ClientSummary>,
    collectors: List<CollectorOption>,
    isSaving: Boolean,
    preselectedClientId: Long? = null,
    onLoadCollectors: () -> Unit = {},
    onSubmit: (NewLoanInput) -> Unit,
) {
    LaunchedEffect(Unit) { onLoadCollectors() }

    val today = LocalDate.now()

    var selectedClient by remember { mutableStateOf(clients.firstOrNull { it.id == preselectedClientId }) }
    var selectedCollector by remember { mutableStateOf<CollectorOption?>(null) }
    var currency by remember { mutableStateOf(Currencies.first()) }
    var principalAmount by remember { mutableStateOf("") }
    var interestRate by remember { mutableStateOf("") }
    var interestType by remember { mutableStateOf(InterestTypes.first()) }
    var paymentFrequency by remember { mutableStateOf(PaymentFrequencies.last()) }
    var calculationMethod by remember { mutableStateOf(CalculationMethods[1]) }
    var termQuantity by remember { mutableStateOf("") }
    var lateFeeType by remember { mutableStateOf(LateFeeTypes.first()) }
    var lateFeeValue by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(today.toString()) }
    var firstPaymentDate by remember { mutableStateOf(today.plusMonths(1).toString()) }
    var notes by remember { mutableStateOf("") }

    val canSubmit = !isSaving &&
        selectedClient != null &&
        principalAmount.toDoubleOrNull()?.let { it > 0 } == true &&
        interestRate.toDoubleOrNull()?.let { it >= 0 } == true &&
        termQuantity.toIntOrNull()?.let { it > 0 } == true &&
        startDate.isNotBlank() &&
        firstPaymentDate.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FormSectionCard(title = "Cliente y cobrador") {
            OptionSelector(
                label = "Cliente *",
                options = clients.map { it.id.toString() to it.fullName },
                selected = selectedClient?.let { it.id.toString() to it.fullName } ?: ("" to "Seleccionar cliente"),
                onSelected = { (id, _) ->
                    selectedClient = clients.firstOrNull { it.id == id.toLongOrNull() }
                },
            )

            val collectorOptions = listOf("" to "Sin cobrador asignado") +
                collectors.map { it.id.toString() to it.name }
            OptionSelector(
                label = "Cobrador",
                options = collectorOptions,
                selected = selectedCollector?.let { it.id.toString() to it.name } ?: ("" to "Sin cobrador asignado"),
                onSelected = { (id, _) ->
                    selectedCollector = collectors.firstOrNull { it.id == id.toLongOrNull() }
                },
            )

            OptionSelector(
                label = "Moneda *",
                options = Currencies,
                selected = currency,
                onSelected = { currency = it },
            )
        }

        FormSectionCard(title = "Condiciones del préstamo") {
            FormField(value = principalAmount, onValueChange = { principalAmount = it }, label = "Monto principal *", keyboardType = KeyboardType.Decimal)
            FormField(value = interestRate, onValueChange = { interestRate = it }, label = "Tasa de interés (%) *", keyboardType = KeyboardType.Decimal)
            FormField(value = termQuantity, onValueChange = { termQuantity = it }, label = "Número de cuotas *", keyboardType = KeyboardType.Number)

            OptionSelector(
                label = "Tipo de interés",
                options = InterestTypes,
                selected = interestType,
                onSelected = { interestType = it },
            )

            OptionSelector(
                label = "Frecuencia de pago",
                options = PaymentFrequencies,
                selected = paymentFrequency,
                onSelected = { paymentFrequency = it },
            )

            OptionSelector(
                label = "Método de cálculo",
                options = CalculationMethods,
                selected = calculationMethod,
                onSelected = { calculationMethod = it },
            )
        }

        FormSectionCard(title = "Mora") {
            OptionSelector(
                label = "Tipo de mora",
                options = LateFeeTypes,
                selected = lateFeeType,
                onSelected = { lateFeeType = it },
            )

            if (lateFeeType.first != "none") {
                FormField(
                    value = lateFeeValue,
                    onValueChange = { lateFeeValue = it },
                    label = "Valor de mora",
                    keyboardType = KeyboardType.Decimal,
                )
            }
        }

        FormSectionCard(title = "Fechas") {
            FormField(value = startDate, onValueChange = { startDate = it }, label = "Fecha de inicio * (YYYY-MM-DD)")
            FormField(value = firstPaymentDate, onValueChange = { firstPaymentDate = it }, label = "Fecha primer pago * (YYYY-MM-DD)")
        }

        FormSectionCard(title = "Notas (opcional)") {
            FormField(value = notes, onValueChange = { notes = it }, label = "Observaciones", singleLine = false)
        }

        Button(
            onClick = {
                val client = selectedClient ?: return@Button
                onSubmit(
                    NewLoanInput(
                        clientId = client.id,
                        collectorId = selectedCollector?.id,
                        currency = currency.first,
                        principalAmount = principalAmount.toDoubleOrNull() ?: 0.0,
                        interestRate = interestRate.toDoubleOrNull() ?: 0.0,
                        interestType = interestType.first,
                        paymentFrequency = paymentFrequency.first,
                        calculationMethod = calculationMethod.first,
                        termQuantity = termQuantity.toIntOrNull() ?: 0,
                        lateFeeType = lateFeeType.first,
                        lateFeeValue = lateFeeValue.toDoubleOrNull()?.takeIf { lateFeeType.first != "none" },
                        startDate = startDate,
                        firstPaymentDate = firstPaymentDate,
                        notes = notes.trim().takeIf { it.isNotBlank() },
                    ),
                )
            },
            enabled = canSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryContainerLoan,
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
                Text(
                    text = "Crear préstamo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
