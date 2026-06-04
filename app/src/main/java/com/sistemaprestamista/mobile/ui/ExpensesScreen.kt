package com.sistemaprestamista.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.sistemaprestamista.mobile.data.model.ExpenseCategoryOption
import com.sistemaprestamista.mobile.data.model.ExpenseItem
import com.sistemaprestamista.mobile.data.model.PaymentMethod
import com.sistemaprestamista.mobile.ui.components.rememberCurrency

private val ScreenBackground = Color(0xFFF4F7FB)
private val CardBackground = Color(0xFFFFFFFF)
private val Primary = Color(0xFF00386C)
private val PrimaryContainer = Color(0xFF1A4F8B)
private val Secondary = Color(0xFF505F76)
private val TextMain = Color(0xFF1A1C20)
private val TextMuted = Color(0xFF667085)
private val Red = Color(0xFFBA1A1A)

@Composable
internal fun ExpensesScreen(
    expenses: List<ExpenseItem>,
    categories: List<ExpenseCategoryOption>,
    isSaving: Boolean,
    onCreateExpense: (Long?, String, String, String) -> Unit,
) {
    val currency = rememberCurrency()
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var method by remember { mutableStateOf(PaymentMethod.Cash) }
    var categoryId by remember { mutableStateOf<Long?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Gastos", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Primary)
                Text("Registra y consulta los gastos de la empresa", style = MaterialTheme.typography.bodyLarge, color = Secondary)
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = CardDefaults.outlinedCardBorder(),
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Nuevo gasto", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextMain)

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Descripción") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors(),
                    )
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Monto") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors(),
                    )

                    Text("Método", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PaymentMethod.entries.forEach { pm ->
                            FilterChip(
                                selected = method == pm,
                                onClick = { method = pm },
                                label = { Text(pm.label) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryContainer.copy(alpha = 0.15f)),
                            )
                        }
                    }

                    if (categories.isNotEmpty()) {
                        Text("Categoría", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = categoryId == null,
                                onClick = { categoryId = null },
                                label = { Text("Sin categoría") },
                            )
                            categories.forEach { cat ->
                                FilterChip(
                                    selected = categoryId == cat.id,
                                    onClick = { categoryId = cat.id },
                                    label = { Text(cat.name) },
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            onCreateExpense(categoryId, description, amount, method.apiValue)
                            description = ""
                            amount = ""
                        },
                        enabled = !isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    ) {
                        Text(if (isSaving) "Guardando..." else "Registrar gasto", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        item {
            Text("Historial de gastos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextMain)
        }

        if (expenses.isEmpty()) {
            item { Text("No hay gastos registrados.", style = MaterialTheme.typography.bodyMedium, color = TextMuted) }
        } else {
            items(expenses, key = { it.id }) { expense ->
                ExpenseCard(expense = expense, amount = currency.format(expense.amount))
            }
        }
    }
}

@Composable
private fun ExpenseCard(expense: ExpenseItem, amount: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(expense.description, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextMain, maxLines = 1)
                Text(
                    listOfNotNull(expense.date, expense.category ?: "Sin categoría").joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                )
            }
            Text("-$amount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Red)
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = CardBackground,
    unfocusedContainerColor = CardBackground,
    focusedBorderColor = PrimaryContainer,
    unfocusedBorderColor = Color(0xFFC2C6D1),
)
