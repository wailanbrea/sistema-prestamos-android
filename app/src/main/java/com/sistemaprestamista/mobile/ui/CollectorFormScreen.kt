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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sistemaprestamista.mobile.data.model.CollectorDetail
import com.sistemaprestamista.mobile.data.model.NewCollectorInput
import com.sistemaprestamista.mobile.data.model.UpdateCollectorInput

private val BgCollector = Color(0xFFF4F7FB)
private val PrimaryCollector = Color(0xFF1A4F8B)

private val CommissionTypes = listOf(
    "percentage" to "Porcentaje (%)",
    "fixed" to "Monto fijo",
    "none" to "Sin comisión",
)

private val CommissionBases = listOf(
    "payment_total" to "Total del pago",
    "principal_paid" to "Capital cobrado",
    "interest_paid" to "Interés cobrado",
)

private val StatusCollectorOptions = listOf("active" to "Activo", "inactive" to "Inactivo")

@Composable
internal fun CollectorFormScreen(
    existing: CollectorDetail? = null,
    isSaving: Boolean,
    onCreateCollector: (NewCollectorInput) -> Unit = {},
    onUpdateCollector: (UpdateCollectorInput) -> Unit = {},
) {
    val isEdit = existing != null

    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var phone by remember { mutableStateOf(existing?.phone.orEmpty()) }
    var commissionType by remember {
        mutableStateOf(CommissionTypes.firstOrNull { it.first == existing?.commissionType } ?: CommissionTypes.first())
    }
    var commissionBase by remember {
        mutableStateOf(CommissionBases.firstOrNull { it.first == existing?.commissionBase } ?: CommissionBases.first())
    }
    var commissionValue by remember {
        mutableStateOf(if ((existing?.commissionValue ?: 0.0) > 0) existing!!.commissionValue.toString() else "")
    }
    var status by remember {
        mutableStateOf(StatusCollectorOptions.firstOrNull { it.first == existing?.status } ?: StatusCollectorOptions.first())
    }

    val needsValue = commissionType.first != "none"
    val canSubmit = !isSaving && name.isNotBlank() && (!needsValue || commissionValue.toDoubleOrNull()?.let { it >= 0 } == true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCollector)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FormSectionCard(title = "Datos del cobrador") {
            FormField(value = name, onValueChange = { name = it }, label = "Nombre completo *")
            FormField(value = phone, onValueChange = { phone = it }, label = "Teléfono", keyboardType = KeyboardType.Phone)
        }

        FormSectionCard(title = "Comisión") {
            OptionSelector(
                label = "Tipo de comisión",
                options = CommissionTypes,
                selected = commissionType,
                onSelected = { commissionType = it },
            )
            if (commissionType.first != "none") {
                OptionSelector(
                    label = "Base de comisión",
                    options = CommissionBases,
                    selected = commissionBase,
                    onSelected = { commissionBase = it },
                )
                FormField(
                    value = commissionValue,
                    onValueChange = { commissionValue = it },
                    label = if (commissionType.first == "percentage") "Porcentaje *" else "Monto fijo *",
                    keyboardType = KeyboardType.Decimal,
                )
            }
        }

        if (isEdit) {
            FormSectionCard(title = "Estado") {
                OptionSelector(
                    label = "Estado",
                    options = StatusCollectorOptions,
                    selected = status,
                    onSelected = { status = it },
                )
            }
        }

        Button(
            onClick = {
                val value = if (needsValue) commissionValue.toDoubleOrNull() else null
                if (isEdit) {
                    onUpdateCollector(
                        UpdateCollectorInput(
                            name = name.trim(),
                            phone = phone.trim().takeIf { it.isNotBlank() },
                            commissionType = commissionType.first,
                            commissionBase = commissionBase.first,
                            commissionValue = value,
                            status = status.first,
                        ),
                    )
                } else {
                    onCreateCollector(
                        NewCollectorInput(
                            name = name.trim(),
                            phone = phone.trim().takeIf { it.isNotBlank() },
                            commissionType = commissionType.first,
                            commissionBase = commissionBase.first,
                            commissionValue = value,
                            status = "active",
                        ),
                    )
                }
            },
            enabled = canSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryCollector,
                contentColor = Color.White,
            ),
        ) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
            } else {
                Text(
                    text = if (isEdit) "Guardar cambios" else "Crear cobrador",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
