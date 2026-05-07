package com.sistemaprestamista.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sistemaprestamista.mobile.data.model.ClientSummary

private val ScreenBackground = Color(0xFFF4F7FB)
private val CardBackground = Color(0xFFFFFFFF)
private val Primary = Color(0xFF00386C)
private val PrimaryContainer = Color(0xFF1A4F8B)
private val Secondary = Color(0xFF505F76)
private val SecondaryContainer = Color(0xFFD0E1FB)
private val SurfaceContainer = Color(0xFFEDEDF3)
private val SurfaceContainerLow = Color(0xFFF3F3F9)
private val TextMain = Color(0xFF1A1C20)
private val TextVariant = Color(0xFF424750)
private val Outline = Color(0xFF737781)
private val OutlineVariant = Color(0xFFC2C6D1)
private val Success = Color(0xFF005236)
private val SuccessSoft = Color(0xFF6FFBBE)
private val Error = Color(0xFFBA1A1A)
private val ErrorContainer = Color(0xFFFFDAD6)
private val ErrorText = Color(0xFF93000A)

@Composable
internal fun ClientsScreen(
    clients: List<ClientSummary>,
    onOpenClient: (Long) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredClients = remember(clients, searchQuery) {
        val query = searchQuery.trim().lowercase()

        if (query.isBlank()) {
            clients
        } else {
            clients.filter { client ->
                client.fullName.lowercase().contains(query) ||
                        client.phone.orEmpty().lowercase().contains(query) ||
                        client.identification.orEmpty().lowercase().contains(query) ||
                        client.address.orEmpty().lowercase().contains(query)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 22.dp,
            bottom = 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ClientsHeader()
        }

        item {
            ClientSearchBar(
                value = searchQuery,
                onValueChange = { searchQuery = it },
            )
        }

        if (filteredClients.isEmpty()) {
            item {
                EmptyClientsState(
                    message = if (searchQuery.isBlank()) {
                        "No hay clientes asignados para este cobrador en la ruta actual."
                    } else {
                        "No se encontraron clientes con ese criterio de búsqueda."
                    },
                )
            }
        } else {
            items(filteredClients, key = { it.id }) { client ->
                ClientCard(
                    client = client,
                    onOpenClient = onOpenClient,
                )
            }
        }
    }
}

@Composable
private fun ClientsHeader() {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "Clientes",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Primary,
        )

        Text(
            text = "Gestiona y consulta tus clientes",
            style = MaterialTheme.typography.bodyLarge,
            color = Secondary,
        )
    }
}

@Composable
private fun ClientSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = "Buscar cliente...",
                color = Outline,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = Outline,
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = CardBackground,
            unfocusedContainerColor = CardBackground,
            focusedBorderColor = PrimaryContainer,
            unfocusedBorderColor = Color.Transparent,
            focusedTextColor = TextMain,
            unfocusedTextColor = TextMain,
        ),
    )
}

@Composable
private fun ClientCard(
    client: ClientSummary,
    onOpenClient: (Long) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = client.fullName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextMain,
                    )

                    val phoneAndId = listOfNotNull(
                        client.phone?.takeIf { it.isNotBlank() },
                        client.identification?.takeIf { it.isNotBlank() }?.let { "ID $it" },
                    ).joinToString(" · ")

                    if (phoneAndId.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Call,
                                contentDescription = null,
                                tint = TextVariant,
                                modifier = Modifier.size(17.dp),
                            )

                            Text(
                                text = phoneAndId,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextVariant,
                                maxLines = 1,
                            )
                        }
                    }

                    if (!client.address.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = null,
                                tint = Outline,
                                modifier = Modifier.size(16.dp),
                            )

                            Text(
                                text = client.address,
                                style = MaterialTheme.typography.bodySmall,
                                color = Outline,
                                maxLines = 2,
                            )
                        }
                    }
                }

                ClientStatusBadge(
                    status = client.status,
                    riskLevel = client.riskLevel,
                )
            }

            OutlinedButton(
                onClick = { onOpenClient(client.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Primary,
                    containerColor = Color.Transparent,
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    width = 1.5.dp,
                ),
            ) {
                Text(
                    text = "Ver expediente",
                    fontWeight = FontWeight.Bold,
                )

                Spacer(Modifier.size(8.dp))

                Icon(
                    imageVector = Icons.Outlined.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun ClientStatusBadge(
    status: String,
    riskLevel: String,
) {
    val normalizedStatus = status.lowercase()
    val normalizedRisk = riskLevel.lowercase()

    val isDanger = normalizedStatus.contains("atras") ||
            normalizedStatus.contains("mora") ||
            normalizedStatus.contains("moroso") ||
            normalizedRisk.contains("alto") ||
            normalizedRisk.contains("high") ||
            normalizedRisk.contains("critical")

    val isPending = normalizedStatus.contains("pend") ||
            normalizedStatus.contains("inactive") ||
            normalizedStatus.contains("inactivo")

    val background = when {
        isDanger -> ErrorContainer
        isPending -> SecondaryContainer
        else -> SuccessSoft.copy(alpha = 0.28f)
    }

    val dotColor = when {
        isDanger -> Error
        isPending -> Primary
        else -> Success
    }

    val textColor = when {
        isDanger -> ErrorText
        isPending -> Secondary
        else -> Success
    }

    val label = buildString {
        append(status.replaceFirstChar { it.uppercase() })

        if (riskLevel.isNotBlank()) {
            append(" · riesgo ")
            append(riskLevel.lowercase())
        }
    }

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(background)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun EmptyClientsState(
    message: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(82.dp)
                    .clip(CircleShape)
                    .background(CardBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.PersonOff,
                    contentDescription = null,
                    tint = OutlineVariant,
                    modifier = Modifier.size(42.dp),
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "Sin resultados",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Secondary,
                )

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Outline,
                )
            }
        }
    }
}