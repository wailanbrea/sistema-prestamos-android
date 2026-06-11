package com.sistemaprestamista.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.sistemaprestamista.mobile.data.model.CashMovementInput
import com.sistemaprestamista.mobile.data.model.CashMovementItem
import com.sistemaprestamista.mobile.data.model.CashSummary
import com.sistemaprestamista.mobile.ui.components.rememberCurrency
import java.time.LocalDate

private val ScreenBackground = Color(0xFFF4F7FB)
private val CardBackground = Color(0xFFFFFFFF)
private val Primary = Color(0xFF00386C)
private val Secondary = Color(0xFF505F76)
private val TextMain = Color(0xFF1A1C20)
private val TextMuted = Color(0xFF667085)
private val Green = Color(0xFF12B76A)
private val Red = Color(0xFFBA1A1A)

private val MovementTypes = listOf(
    "capital_injection" to "Inyección de capital",
    "capital_withdrawal" to "Retiro de capital",
    "adjustment" to "Ajuste",
)

private val DirectionOptions = listOf("in" to "Entrada (+)", "out" to "Salida (-)")

@Composable
internal fun CashScreen(
    summary: CashSummary?,
    movements: List<CashMovementItem>,
    canCreateMovement: Boolean = false,
    isSavingMovement: Boolean = false,
    onCreateMovement: ((CashMovementInput) -> Unit)? = null,
) {
    val currency = rememberCurrency()
    var showCreateDialog by remember { mutableStateOf(false) }

    if (showCreateDialog && onCreateMovement != null) {
        CreateMovementDialog(
            isSaving = isSavingMovement,
            onDismiss = { showCreateDialog = false },
            onConfirm = { input ->
                showCreateDialog = false
                onCreateMovement(input)
            },
        )
    }

    Scaffold(
        containerColor = ScreenBackground,
        floatingActionButton = {
            if (canCreateMovement && onCreateMovement != null) {
                ExtendedFloatingActionButton(
                    onClick = { showCreateDialog = true },
                    icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                    text = { Text("Nuevo movimiento") },
                    containerColor = Primary,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(4.dp),
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(ScreenBackground)
                .padding(innerPadding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Caja", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Primary)
                    Text("Entradas, salidas y balance", style = MaterialTheme.typography.bodyLarge, color = Secondary)
                }
            }

            if (summary != null) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CashCell("Entradas", currency.format(summary.totalIn), Green, Modifier.weight(1f))
                        CashCell("Salidas", currency.format(summary.totalOut), Red, Modifier.weight(1f))
                    }
                }
                item {
                    CashCell(
                        "Balance de caja",
                        currency.format(summary.balance),
                        if (summary.balance < 0) Red else Primary,
                        Modifier.fillMaxWidth(),
                    )
                }
            }

            item {
                Text("Movimientos recientes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextMain)
            }

            if (movements.isEmpty()) {
                item { Text("No hay movimientos de caja.", style = MaterialTheme.typography.bodyMedium, color = TextMuted) }
            } else {
                items(movements, key = { it.id }) { movement ->
                    MovementCard(movement = movement, amount = currency.format(movement.amount))
                }
            }
        }
    }
}

@Composable
private fun CreateMovementDialog(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (CashMovementInput) -> Unit,
) {
    var movementType by remember { mutableStateOf(MovementTypes.first()) }
    var direction by remember { mutableStateOf(DirectionOptions.first()) }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val movementDate = LocalDate.now().toString()

    val canSubmit = !isSaving && amount.toDoubleOrNull()?.let { it > 0 } == true && description.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo movimiento de caja") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OptionSelector(
                    label = "Tipo",
                    options = MovementTypes,
                    selected = movementType,
                    onSelected = { movementType = it },
                )
                if (movementType.first == "adjustment") {
                    OptionSelector(
                        label = "Dirección",
                        options = DirectionOptions,
                        selected = direction,
                        onSelected = { direction = it },
                    )
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Monto *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción *") },
                    singleLine = false,
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        CashMovementInput(
                            type = movementType.first,
                            direction = if (movementType.first == "adjustment") direction.first else null,
                            amount = amount.toDouble(),
                            movementDate = movementDate,
                            description = description.trim(),
                        ),
                    )
                },
                enabled = canSubmit,
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.padding(horizontal = 8.dp), color = Color.White)
                } else {
                    Text("Guardar")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun CashCell(title: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextMuted)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = valueColor)
        }
    }
}

@Composable
private fun MovementCard(movement: CashMovementItem, amount: String) {
    val isIn = movement.direction == "in"
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(movement.description ?: movementTypeLabel(movement.type), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextMain, maxLines = 1)
                Text(listOfNotNull(movement.date, movementTypeLabel(movement.type)).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = TextMuted, maxLines = 1)
            }
            Text((if (isIn) "+" else "-") + amount, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (isIn) Green else Red)
        }
    }
}

private fun movementTypeLabel(type: String): String = when (type) {
    "loan_disbursement" -> "Desembolso"
    "payment_received" -> "Cobro"
    "expense" -> "Gasto"
    "collector_commission" -> "Comisión"
    "capital_injection" -> "Inyección de capital"
    "capital_withdrawal" -> "Retiro de capital"
    "adjustment" -> "Ajuste"
    else -> type
}
