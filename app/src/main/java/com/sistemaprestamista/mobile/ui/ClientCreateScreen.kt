package com.sistemaprestamista.mobile.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.sistemaprestamista.mobile.data.model.NewClientInput

private val ScreenBackground = Color(0xFFF4F7FB)
private val CardBackground = Color(0xFFFFFFFF)
private val Primary = Color(0xFF00386C)
private val PrimaryContainer = Color(0xFF1A4F8B)
private val TextMain = Color(0xFF1A1C20)
private val TextVariant = Color(0xFF424750)
private val Success = Color(0xFF008A5C)

private val ClientStatuses = listOf(
    "active" to "Activo",
    "inactive" to "Inactivo",
    "moroso" to "Moroso",
    "blocked" to "Bloqueado",
)

private val RiskLevels = listOf(
    "low" to "Bajo",
    "medium" to "Medio",
    "high" to "Alto",
    "critical" to "Crítico",
)

/**
 * Alta de cliente desde el back-office: mismos campos que el formulario web
 * (StoreClientRequest), incluida la captura de coordenadas GPS del domicilio.
 */
@Composable
internal fun ClientCreateScreen(
    isSaving: Boolean,
    onSubmit: (NewClientInput) -> Unit,
) {
    val context = LocalContext.current

    var fullName by remember { mutableStateOf("") }
    var identification by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var secondaryPhone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var locationReference by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var locationStatus by remember { mutableStateOf<String?>(null) }
    var workplace by remember { mutableStateOf("") }
    var workplacePhone by remember { mutableStateOf("") }
    var workplaceAddress by remember { mutableStateOf("") }
    var monthlyIncome by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(ClientStatuses.first()) }
    var riskLevel by remember { mutableStateOf(RiskLevels.first()) }
    var notes by remember { mutableStateOf("") }

    @SuppressLint("MissingPermission")
    fun captureLocation() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            locationStatus = "Permiso de ubicación no concedido."
            return
        }

        locationStatus = "Obteniendo ubicación..."
        LocationServices.getFusedLocationProviderClient(context).lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    latitude = location.latitude
                    longitude = location.longitude
                    locationStatus = "Ubicación capturada: %.5f, %.5f".format(location.latitude, location.longitude)
                } else {
                    locationStatus = "No se pudo obtener la ubicación. Abre el mapa un momento e intenta de nuevo."
                }
            }
            .addOnFailureListener {
                locationStatus = "No se pudo obtener la ubicación."
            }
    }

    val canSubmit = !isSaving && fullName.isNotBlank() && address.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FormSectionCard(title = "Datos personales") {
            FormField(value = fullName, onValueChange = { fullName = it }, label = "Nombre completo *")
            FormField(value = identification, onValueChange = { identification = it }, label = "Cédula / identificación")
            FormField(value = phone, onValueChange = { phone = it }, label = "Teléfono", keyboardType = KeyboardType.Phone)
            PhoneCountryHint(phone)
            FormField(value = secondaryPhone, onValueChange = { secondaryPhone = it }, label = "Teléfono secundario", keyboardType = KeyboardType.Phone)
            PhoneCountryHint(secondaryPhone)
            FormField(value = email, onValueChange = { email = it }, label = "Correo electrónico", keyboardType = KeyboardType.Email)
        }

        FormSectionCard(title = "Ubicación") {
            FormField(value = address, onValueChange = { address = it }, label = "Dirección *", singleLine = false)
            FormField(value = locationReference, onValueChange = { locationReference = it }, label = "Referencia de ubicación")

            Text(
                text = "Toca el mapa para abrirlo, buscar y elegir la ubicación; la dirección se completa sola.",
                style = MaterialTheme.typography.labelSmall,
                color = TextVariant,
            )
            AddressMapPicker(
                latitude = latitude,
                longitude = longitude,
                onLocationPicked = { lat, lng ->
                    latitude = lat
                    longitude = lng
                    locationStatus = "Ubicación elegida en el mapa: %.5f, %.5f".format(lat, lng)
                },
                onAddressResolved = { address = it },
            )

            OutlinedButton(
                onClick = ::captureLocation,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.MyLocation,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = PrimaryContainer,
                )
                Text(
                    text = " Capturar ubicación GPS",
                    fontWeight = FontWeight.Bold,
                    color = PrimaryContainer,
                )
            }

            locationStatus?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (latitude != null) Success else TextVariant,
                )
            }
        }

        FormSectionCard(title = "Información laboral") {
            FormField(value = workplace, onValueChange = { workplace = it }, label = "Lugar de trabajo")
            FormField(value = workplacePhone, onValueChange = { workplacePhone = it }, label = "Teléfono del trabajo", keyboardType = KeyboardType.Phone)
            FormField(value = workplaceAddress, onValueChange = { workplaceAddress = it }, label = "Dirección del trabajo", singleLine = false)
            FormField(value = monthlyIncome, onValueChange = { monthlyIncome = it }, label = "Ingreso mensual", keyboardType = KeyboardType.Decimal)
        }

        FormSectionCard(title = "Clasificación") {
            OptionSelector(
                label = "Estado",
                options = ClientStatuses,
                selected = status,
                onSelected = { status = it },
            )

            OptionSelector(
                label = "Nivel de riesgo",
                options = RiskLevels,
                selected = riskLevel,
                onSelected = { riskLevel = it },
            )

            FormField(value = notes, onValueChange = { notes = it }, label = "Notas", singleLine = false)
        }

        Button(
            onClick = {
                onSubmit(
                    NewClientInput(
                        fullName = fullName.trim(),
                        identification = identification.trim(),
                        phone = phone.trim(),
                        secondaryPhone = secondaryPhone.trim(),
                        email = email.trim(),
                        address = address.trim(),
                        locationReference = locationReference.trim(),
                        latitude = latitude,
                        longitude = longitude,
                        workplace = workplace.trim(),
                        workplacePhone = workplacePhone.trim(),
                        workplaceAddress = workplaceAddress.trim(),
                        monthlyIncome = monthlyIncome.toDoubleOrNull(),
                        status = status.first,
                        riskLevel = riskLevel.first,
                        notes = notes.trim(),
                    ),
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
                Text(
                    text = "Guardar cliente",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
internal fun FormSectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Primary,
            )

            content()
        }
    }
}

@Composable
internal fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(14.dp),
    )
}

/** Dropdown simple de opciones (apiValue → etiqueta visible). */
@Composable
internal fun OptionSelector(
    label: String,
    options: List<Pair<String, String>>,
    selected: Pair<String, String>,
    onSelected: (Pair<String, String>) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = TextVariant,
        )

        Row {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = selected.second,
                        fontWeight = FontWeight.Bold,
                        color = TextMain,
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.second) },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}
