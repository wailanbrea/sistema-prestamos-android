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
import com.sistemaprestamista.mobile.data.model.ClientDetail
import com.sistemaprestamista.mobile.data.model.UpdateClientInput

private val BgEdit = Color(0xFFF4F7FB)
private val PrimaryClientEdit = Color(0xFF1A4F8B)

private val StatusOptions = listOf("active" to "Activo", "inactive" to "Inactivo")
private val RiskOptions = listOf("low" to "Bajo", "medium" to "Medio", "high" to "Alto")

@Composable
internal fun ClientEditScreen(
    detail: ClientDetail,
    isSaving: Boolean,
    onSubmit: (UpdateClientInput) -> Unit,
) {
    val s = detail.summary

    var fullName by remember { mutableStateOf(s.fullName) }
    var identification by remember { mutableStateOf(s.identification.orEmpty()) }
    var phone by remember { mutableStateOf(s.phone.orEmpty()) }
    var secondaryPhone by remember { mutableStateOf(detail.secondaryPhone.orEmpty()) }
    var email by remember { mutableStateOf(detail.email.orEmpty()) }
    var address by remember { mutableStateOf(s.address.orEmpty()) }
    var locationReference by remember { mutableStateOf(s.locationReference.orEmpty()) }
    var workplace by remember { mutableStateOf(detail.workplace.orEmpty()) }
    var workplacePhone by remember { mutableStateOf(detail.workplacePhone.orEmpty()) }
    var monthlyIncome by remember {
        mutableStateOf(if (detail.monthlyIncome > 0) detail.monthlyIncome.toString() else "")
    }
    var notes by remember { mutableStateOf(detail.notes.orEmpty()) }
    var status by remember { mutableStateOf(StatusOptions.firstOrNull { it.first == s.status } ?: StatusOptions.first()) }
    var riskLevel by remember { mutableStateOf(RiskOptions.firstOrNull { it.first == s.riskLevel } ?: RiskOptions.first()) }

    val canSubmit = !isSaving && fullName.isNotBlank() && address.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgEdit)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FormSectionCard(title = "Datos personales") {
            FormField(value = fullName, onValueChange = { fullName = it }, label = "Nombre completo *")
            FormField(value = identification, onValueChange = { identification = it }, label = "Cédula / identificación")
            FormField(value = phone, onValueChange = { phone = it }, label = "Teléfono principal", keyboardType = KeyboardType.Phone)
            FormField(value = secondaryPhone, onValueChange = { secondaryPhone = it }, label = "Teléfono secundario", keyboardType = KeyboardType.Phone)
            FormField(value = email, onValueChange = { email = it }, label = "Correo electrónico", keyboardType = KeyboardType.Email)
        }

        FormSectionCard(title = "Dirección") {
            FormField(value = address, onValueChange = { address = it }, label = "Dirección *", singleLine = false)
            FormField(value = locationReference, onValueChange = { locationReference = it }, label = "Referencia de ubicación", singleLine = false)
        }

        FormSectionCard(title = "Trabajo e ingresos") {
            FormField(value = workplace, onValueChange = { workplace = it }, label = "Lugar de trabajo")
            FormField(value = workplacePhone, onValueChange = { workplacePhone = it }, label = "Teléfono del trabajo", keyboardType = KeyboardType.Phone)
            FormField(value = monthlyIncome, onValueChange = { monthlyIncome = it }, label = "Ingreso mensual", keyboardType = KeyboardType.Decimal)
        }

        FormSectionCard(title = "Estado y riesgo") {
            OptionSelector(label = "Estado", options = StatusOptions, selected = status, onSelected = { status = it })
            OptionSelector(label = "Nivel de riesgo", options = RiskOptions, selected = riskLevel, onSelected = { riskLevel = it })
        }

        FormSectionCard(title = "Notas") {
            FormField(value = notes, onValueChange = { notes = it }, label = "Observaciones", singleLine = false)
        }

        Button(
            onClick = {
                onSubmit(
                    UpdateClientInput(
                        fullName = fullName.trim(),
                        identification = identification.trim().takeIf { it.isNotBlank() },
                        phone = phone.trim().takeIf { it.isNotBlank() },
                        secondaryPhone = secondaryPhone.trim().takeIf { it.isNotBlank() },
                        email = email.trim().takeIf { it.isNotBlank() },
                        address = address.trim(),
                        locationReference = locationReference.trim().takeIf { it.isNotBlank() },
                        workplace = workplace.trim().takeIf { it.isNotBlank() },
                        workplacePhone = workplacePhone.trim().takeIf { it.isNotBlank() },
                        monthlyIncome = monthlyIncome.toDoubleOrNull(),
                        status = status.first,
                        riskLevel = riskLevel.first,
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
                containerColor = PrimaryClientEdit,
                contentColor = Color.White,
            ),
        ) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
            } else {
                Text(text = "Guardar cambios", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
