package com.sistemaprestamista.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.sistemaprestamista.mobile.data.model.UserProfile
import com.sistemaprestamista.mobile.ui.components.rememberCurrency
import com.sistemaprestamista.mobile.update.AppUpdater
import com.sistemaprestamista.mobile.update.UpdateCheck
import com.sistemaprestamista.mobile.update.UpdateInfo
import kotlinx.coroutines.launch

private val ScreenBackground = Color(0xFFF9F9FF)
private val CardBackground = Color(0xFFFFFFFF)
private val Primary = Color(0xFF00386C)
private val PrimaryContainer = Color(0xFF1A4F8B)
private val OnPrimaryContainer = Color(0xFF9BC2FF)
private val SecondaryContainer = Color(0xFFD0E1FB)
private val OnSecondaryContainer = Color(0xFF54647A)
private val SurfaceContainerHigh = Color(0xFFE8E8ED)
private val TextMain = Color(0xFF1A1C20)
private val TextVariant = Color(0xFF424750)
private val OutlineVariant = Color(0xFFC2C6D1)
private val Success = Color(0xFF005236)
private val SuccessContainer = Color(0xFF6FFBBE)
private val Error = Color(0xFFBA1A1A)

@Composable
internal fun ProfileScreen(
    user: UserProfile?,
    pendingPaymentCount: Int,
    onOpenPendingPayments: () -> Unit,
    onOpenPrintSettings: () -> Unit,
    onLogout: () -> Unit,
) {
    if (user == null) return

    val roleText = user.roles.joinToString().ifBlank { "Sin rol asignado" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ProfileHeader(
            name = user.name,
            email = user.email,
            role = roleText,
        )

        ProfileInfoCard(
            name = user.name,
            email = user.email,
            company = user.company.name,
            role = roleText,
        )

        ProfileActions(
            pendingPaymentCount = pendingPaymentCount,
            onOpenPendingPayments = onOpenPendingPayments,
            onOpenPrintSettings = onOpenPrintSettings,
            onLogout = onLogout,
        )

        AppUpdateCard()

        Text(
            text = "Sistema Prestamista v${AppUpdater.currentVersionName}",
            style = MaterialTheme.typography.labelSmall,
            color = TextVariant.copy(alpha = 0.45f),
        )
    }
}

private sealed interface UpdateDialogState {
    data class Available(val info: UpdateInfo) : UpdateDialogState
    data object UpToDate : UpdateDialogState
    data class Failed(val message: String) : UpdateDialogState
}

@Composable
private fun AppUpdateCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf<Int?>(null) }
    var dialog by remember { mutableStateOf<UpdateDialogState?>(null) }

    FilledTonalButton(
        onClick = {
            if (checking || downloadProgress != null) return@FilledTonalButton
            checking = true
            scope.launch {
                dialog = when (val result = AppUpdater.check()) {
                    is UpdateCheck.Available -> UpdateDialogState.Available(result.info)
                    is UpdateCheck.UpToDate -> UpdateDialogState.UpToDate
                    is UpdateCheck.Failed -> UpdateDialogState.Failed(result.message)
                }
                checking = false
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = SecondaryContainer,
            contentColor = OnSecondaryContainer,
        ),
    ) {
        if (checking) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = Primary,
            )
        } else {
            Icon(Icons.Outlined.SystemUpdate, contentDescription = null)
        }
        Spacer(Modifier.size(10.dp))
        Text(
            text = if (checking) "Buscando…" else "Buscar actualización",
            fontWeight = FontWeight.Bold,
        )
    }

    when (val current = dialog) {
        is UpdateDialogState.UpToDate -> AlertDialog(
            onDismissRequest = { dialog = null },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text("Aceptar") } },
            title = { Text("Todo al día") },
            text = { Text("Ya tienes la última versión (v${AppUpdater.currentVersionName}).") },
        )

        is UpdateDialogState.Failed -> AlertDialog(
            onDismissRequest = { dialog = null },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text("Aceptar") } },
            title = { Text("No se pudo verificar") },
            text = { Text(current.message) },
        )

        is UpdateDialogState.Available -> {
            val progress = downloadProgress
            AlertDialog(
                onDismissRequest = { if (progress == null) dialog = null },
                confirmButton = {
                    if (progress == null) {
                        TextButton(onClick = {
                            scope.launch {
                                try {
                                    downloadProgress = 0
                                    val file = AppUpdater.downloadApk(context, current.info.apkUrl) { p ->
                                        downloadProgress = p
                                    }
                                    downloadProgress = null
                                    dialog = null
                                    if (AppUpdater.canInstall(context)) {
                                        AppUpdater.installApk(context, file)
                                    } else {
                                        AppUpdater.openInstallPermissionSettings(context)
                                    }
                                } catch (e: Exception) {
                                    downloadProgress = null
                                    dialog = UpdateDialogState.Failed(e.message ?: "Error al descargar")
                                }
                            }
                        }) { Text("Descargar e instalar") }
                    }
                },
                dismissButton = {
                    if (progress == null) {
                        TextButton(onClick = { dialog = null }) { Text("Ahora no") }
                    }
                },
                title = { Text("Nueva versión ${current.info.versionName}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (current.info.notes.isNotBlank()) {
                            Text(current.info.notes)
                        }
                        if (progress != null) {
                            Text("Descargando… $progress%")
                            LinearProgressIndicator(
                                progress = { progress / 100f },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                },
            )
        }

        null -> Unit
    }
}

@Composable
private fun ProfileHeader(
    name: String,
    email: String,
    role: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(CircleShape)
                    .background(PrimaryContainer)
                    .border(4.dp, CardBackground, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = name.firstOrNull()?.uppercase() ?: "U",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(SuccessContainer)
                    .border(2.dp, CardBackground, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(Success),
                )
            }
        }

        Text(
            text = name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextMain,
        )

        Text(
            text = email,
            style = MaterialTheme.typography.bodyMedium,
            color = TextVariant,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfileBadge(
                text = role,
                background = PrimaryContainer,
                content = OnPrimaryContainer,
            )

            ProfileBadge(
                text = "Activo",
                background = Success,
                content = SuccessContainer,
            )
        }
    }
}

@Composable
private fun ProfileBadge(
    text: String,
    background: Color,
    content: Color,
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(background)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = content,
            maxLines = 1,
        )
    }
}

@Composable
private fun ProfileInfoCard(
    name: String,
    email: String,
    company: String,
    role: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            InfoRow("Nombre completo", name, showDivider = true)
            InfoRow("Correo electrónico", email, showDivider = true)
            InfoRow("Empresa", company, showDivider = true)
            InfoRow("Rol del sistema", role, showDivider = false)
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    showDivider: Boolean,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = TextVariant,
            )

            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextMain,
                maxLines = 1,
            )
        }

        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(OutlineVariant.copy(alpha = 0.25f)),
            )
        }
    }
}

@Composable
private fun ProfileActions(
    pendingPaymentCount: Int,
    onOpenPendingPayments: () -> Unit,
    onOpenPrintSettings: () -> Unit,
    onLogout: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FilledTonalButton(
            onClick = onOpenPendingPayments,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = if (pendingPaymentCount > 0) SecondaryContainer else SurfaceContainerHigh,
                contentColor = if (pendingPaymentCount > 0) Primary else TextVariant,
            ),
        ) {
            Icon(Icons.Outlined.PendingActions, contentDescription = null)
            Spacer(Modifier.size(10.dp))
            Text(
                text = if (pendingPaymentCount > 0) {
                    "Cobros pendientes ($pendingPaymentCount)"
                } else {
                    "Cobros pendientes"
                },
                fontWeight = FontWeight.Bold,
            )
        }

        FilledTonalButton(
            onClick = onOpenPrintSettings,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = SecondaryContainer,
                contentColor = OnSecondaryContainer,
            ),
        ) {
            Icon(Icons.Outlined.Print, contentDescription = null)
            Spacer(Modifier.size(10.dp))
            Text(
                text = "Configurar impresora",
                fontWeight = FontWeight.Bold,
            )
        }

        FilledTonalButton(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = SurfaceContainerHigh,
                contentColor = TextVariant,
            ),
        ) {
            Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
            Spacer(Modifier.size(10.dp))
            Text(
                text = "Cerrar sesión",
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
internal fun LastReceiptCard(
    state: AppUiState,
    onOpenReceipt: () -> Unit,
) {
    val receipt = state.lastPaymentReceipt ?: return
    val currency = rememberCurrency()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                contentDescription = null,
                tint = Primary.copy(alpha = 0.08f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(72.dp),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                        contentDescription = null,
                        tint = Primary,
                    )

                    Text(
                        text = "Último recibo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Primary,
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ReceiptInfoRow("Recibo #", receipt.receiptNumber)
                    ReceiptInfoRow("Cliente", receipt.client?.fullName ?: "Cliente no disponible")
                    ReceiptInfoRow(
                        label = "Monto pagado",
                        value = currency.format(receipt.amount),
                        valueColor = Primary,
                        valueWeight = FontWeight.Bold,
                    )
                    ReceiptInfoRow(
                        label = "Saldo restante",
                        value = currency.format(receipt.newBalance),
                        valueColor = Error,
                        valueWeight = FontWeight.Bold,
                    )
                }

                Button(
                    onClick = onOpenReceipt,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary,
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = "Ver recibo",
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReceiptInfoRow(
    label: String,
    value: String,
    valueColor: Color = TextMain,
    valueWeight: FontWeight = FontWeight.SemiBold,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextVariant,
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = valueWeight,
            color = valueColor,
            maxLines = 1,
        )
    }
}
