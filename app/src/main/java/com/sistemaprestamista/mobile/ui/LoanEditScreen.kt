package com.sistemaprestamista.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sistemaprestamista.mobile.data.model.CollectorOption
import com.sistemaprestamista.mobile.data.model.LoanDetail
import com.sistemaprestamista.mobile.data.model.UpdateLoanInput

private val ScreenBackground = Color(0xFFF4F7FB)
private val PrimaryEdit = Color(0xFF00386C)
private val PrimaryContainerEdit = Color(0xFF1A4F8B)

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
internal fun LoanEditScreen(
    detail: LoanDetail,
    collectors: List<CollectorOption>,
    isSaving: Boolean,
    onLoadCollectors: () -> Unit = {},
    onSubmit: (UpdateLoanInput) -> Unit,
) {
    androidx.compose.runtime.LaunchedEffect(Unit) { onLoadCollectors() }

    val canEditFinancials = detail.financialSummary.paymentsTotal == 0

    // Always-editable fields
    var selectedCollector by remember {
        mutableStateOf(collectors.firstOrNull { it.id == detail.collectorId })
    }
    var currencyState by remember {
        mutableStateOf(Currencies.firstOrNull { it.first == detail.currency } ?: Currencies.first())
    }
    var guaranteeDescription by remember { mutableStateOf(detail.guaranteeDescription.orEmpty()) }
    var notes by remember { mutableStateOf(detail.notes.orEmpty()) }
    var allowsCapitalPrepayment by remember { mutableStateOf(detail.allowsCapitalPrepayment) }

    // Financial fields (only editable if no valid payments)
    var principalAmount by remember { mutableStateOf(detail.summary.principalAmount.toString()) }
    var interestRate by remember { mutableStateOf(detail.interestRate.toString()) }
    var interestType by remember {
        mutableStateOf(InterestTypes.firstOrNull { it.first == detail.interestType } ?: InterestTypes.first())
    }
    var paymentFrequency by remember {
        mutableStateOf(PaymentFrequencies.firstOrNull { it.first == detail.summary.paymentFrequency } ?: PaymentFrequencies.last())
    }
    var calculationMethod by remember {
        mutableStateOf(CalculationMethods.firstOrNull { it.first == detail.calculationMethod } ?: CalculationMethods[1])
    }
    var termQuantity by remember { mutableStateOf(detail.termQuantity.toString()) }
    var lateFeeType by remember {
        mutableStateOf(LateFeeTypes.firstOrNull { it.first == detail.lateFeeType } ?: LateFeeTypes.first())
    }
    var lateFeeValue by remember {
        mutableStateOf(if (detail.lateFeeValue > 0) detail.lateFeeValue.toString() else "")
    }
    var startDate by remember { mutableStateOf(detail.startDate.orEmpty()) }
    var firstPaymentDate by remember { mutableStateOf(detail.firstPaymentDate.orEmpty()) }

    // Sync collector selector once collectors list is loaded
    if (selectedCollector == null && detail.collectorId != null) {
        val found = collectors.firstOrNull { it.id == detail.collectorId }
        if (found != null) selectedCollector = found
    }

    val canSubmit = !isSaving && (!canEditFinancials || (
        principalAmount.toDoubleOrNull()?.let { it > 0 } == true &&
        interestRate.toDoubleOrNull()?.let { it >= 0 } == true &&
        termQuantity.toIntOrNull()?.let { it > 0 } == true &&
        startDate.isNotBlank() &&
        firstPaymentDate.isNotBlank()
    ))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (canEditFinancials) {
            androidx.compose.material3.Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = Color(0xFFE8F0FE),
                ),
            ) {
                Text(
                    text = "Este préstamo no tiene pagos registrados — puedes modificar todos los campos.",
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF1A4F8B),
                )
            }
        } else {
            androidx.compose.material3.Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF3E0),
                ),
            ) {
                Text(
                    text = "El préstamo ya tiene pagos — solo puedes editar cobrador, moneda, garantía, notas y prepago.",
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFE65100),
                )
            }
        }

        FormSectionCard(title = "Cobrador y moneda") {
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
                selected = currencyState,
                onSelected = { currencyState = it },
            )
        }

        FormSectionCard(title = "Garantía y notas") {
            FormField(
                value = guaranteeDescription,
                onValueChange = { guaranteeDescription = it },
                label = "Descripción de garantía",
                singleLine = false,
            )
            FormField(
                value = notes,
                onValueChange = { notes = it },
                label = "Notas / observaciones",
                singleLine = false,
            )
        }

        FormSectionCard(title = "Configuración") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Permite prepago de capital",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF1A1C20),
                )
                Switch(
                    checked = allowsCapitalPrepayment,
                    onCheckedChange = { allowsCapitalPrepayment = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = PrimaryContainerEdit),
                )
            }
        }

        if (canEditFinancials) {
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

            FormSectionCard(title = "Fechas") {
                FormField(value = startDate, onValueChange = { startDate = it }, label = "Fecha de inicio * (YYYY-MM-DD)")
                FormField(value = firstPaymentDate, onValueChange = { firstPaymentDate = it }, label = "Fecha primer pago * (YYYY-MM-DD)")
            }
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

        Button(
            onClick = {
                onSubmit(
                    UpdateLoanInput(
                        collectorId = selectedCollector?.id,
                        currency = currencyState.first,
                        guaranteeDescription = guaranteeDescription.trim().takeIf { it.isNotBlank() },
                        notes = notes.trim().takeIf { it.isNotBlank() },
                        allowsCapitalPrepayment = allowsCapitalPrepayment,
                        principalAmount = if (canEditFinancials) principalAmount.toDoubleOrNull() else null,
                        interestRate = if (canEditFinancials) interestRate.toDoubleOrNull() else null,
                        interestType = if (canEditFinancials) interestType.first else null,
                        paymentFrequency = if (canEditFinancials) paymentFrequency.first else null,
                        calculationMethod = if (canEditFinancials) calculationMethod.first else null,
                        termQuantity = if (canEditFinancials) termQuantity.toIntOrNull() else null,
                        lateFeeType = lateFeeType.first,
                        lateFeeValue = if (lateFeeType.first != "none") lateFeeValue.toDoubleOrNull() else 0.0,
                        startDate = if (canEditFinancials) startDate.trim().takeIf { it.isNotBlank() } else null,
                        firstPaymentDate = if (canEditFinancials) firstPaymentDate.trim().takeIf { it.isNotBlank() } else null,
                    ),
                )
            },
            enabled = canSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryContainerEdit,
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
                    text = "Guardar cambios",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
