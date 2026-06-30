package com.sistemaprestamista.mobile.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sistemaprestamista.mobile.data.model.AllocationMode
import com.sistemaprestamista.mobile.data.model.InstallmentSummary
import com.sistemaprestamista.mobile.data.model.LoanDetail
import com.sistemaprestamista.mobile.data.model.LoanDocument
import com.sistemaprestamista.mobile.data.model.LoanSummary
import com.sistemaprestamista.mobile.data.model.PaymentMethod
import com.sistemaprestamista.mobile.data.model.PaymentReceipt
import com.sistemaprestamista.mobile.ui.components.formatPaymentFrequency
import com.sistemaprestamista.mobile.ui.components.rememberCurrency

private val ScreenBackground = Color(0xFFF4F7FB)
private val CardBackground = Color(0xFFFFFFFF)
private val Primary = Color(0xFF00386C)
private val PrimaryContainer = Color(0xFF1A4F8B)
private val Secondary = Color(0xFF505F76)
private val SecondaryContainer = Color(0xFFD0E1FB)
private val SurfaceContainerLow = Color(0xFFF3F3F9)
private val SurfaceContainerHigh = Color(0xFFE8E8ED)
private val TextMain = Color(0xFF1A1C20)
private val TextVariant = Color(0xFF424750)
private val Outline = Color(0xFF737781)
private val OutlineVariant = Color(0xFFC2C6D1)
private val Error = Color(0xFFBA1A1A)
private val ErrorContainer = Color(0xFFFFDAD6)
private val Success = Color(0xFF008A5C)
private val SuccessSoft = Color(0xFFE7F5ED)
private val Orange = Color(0xFFEA580C)

private fun paymentInput(value: Double): String {
    return when {
        value <= 0 -> ""
        value % 1.0 == 0.0 -> value.toLong().toString()
        else -> "%.2f".format(java.util.Locale.US, value)
    }
}

@Composable
internal fun LoanDetailScreen(
    detail: LoanDetail?,
    isLoading: Boolean,
    fallbackLoan: LoanSummary?,
    onOpenInstallment: (Long) -> Unit,
    onRegisterPayment: ((Long, String, String, String, Long?, Double?) -> Unit)? = null,
    onWaiveInstallmentLateFee: ((Long) -> Unit)? = null,
    isPaymentLoading: Boolean = false,
    onGenerateDocument: ((Long, String) -> Unit)? = null,
    isDocumentGenerating: Boolean = false,
    contract: com.sistemaprestamista.mobile.data.model.ContractSummary? = null,
    canManageContracts: Boolean = false,
    isContractLoading: Boolean = false,
    onGenerateContract: ((Long) -> Unit)? = null,
    onEditLoan: (() -> Unit)? = null,
    onDeleteLoan: (() -> Unit)? = null,
    isDeletingLoan: Boolean = false,
    onSendAccountStatement: (() -> Unit)? = null,
    isSendingStatement: Boolean = false,
) {
    val loan = detail?.summary ?: fallbackLoan

    if (isLoading && detail == null && loan == null) {
        LoanLoadingState()
        return
    }

    if (loan == null) {
        LoanNotFoundState()
        return
    }

    val currency = rememberCurrency()
    val installments = detail?.installments.orEmpty()
    val payments = detail?.payments.orEmpty()

    var showPaymentDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var actionsExpanded by remember { mutableStateOf(false) }
    var awaitingPayment by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar préstamo") },
            text = { Text("¿Deseas eliminar el préstamo ${loan.loanNumber}? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = { showDeleteDialog = false; onDeleteLoan?.invoke() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                ) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") } },
        )
    }

    val hasCollectibleInstallments = installments.any {
        it.status.trim().lowercase() != "cancelled" && it.hasPendingCharge
    }
    val canPay = onRegisterPayment != null &&
            (loan.status.trim().lowercase() in setOf("active", "late") || hasCollectibleInstallments)

    if (showPaymentDialog && onRegisterPayment != null) {
        // Mantener el diálogo abierto mostrando "Procesando..." mientras el cobro corre
        // (puede tardar varios segundos) y cerrarlo al terminar: en éxito la app navega
        // al recibo; en error el diálogo se cierra y el mensaje queda visible. Antes se
        // cerraba al instante y parecía que "no pasaba nada".
        LaunchedEffect(isPaymentLoading) {
            if (isPaymentLoading) {
                awaitingPayment = true
            } else if (awaitingPayment) {
                awaitingPayment = false
                showPaymentDialog = false
            }
        }
        RegisterPaymentDialog(
            loan = loan,
            detail = detail,
            isLoading = isPaymentLoading,
            onDismiss = { if (!isPaymentLoading) showPaymentDialog = false },
            onConfirm = { amountText, methodApiValue, allocationModeApiValue, targetInstallmentId, capitalPrepaymentAmount ->
                onRegisterPayment(loan.id, amountText, methodApiValue, allocationModeApiValue, targetInstallmentId, capitalPrepaymentAmount)
            },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground),
    ) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 20.dp,
            bottom = run {
                val fabCount = listOf(canPay, onEditLoan != null, onDeleteLoan != null).count { it }
                when (fabCount) {
                    0 -> 28.dp
                    1 -> 96.dp
                    2 -> 168.dp
                    else -> 240.dp
                }
            },
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            LoanHeaderCard(
                loan = loan,
                detail = detail,
            )
        }

        if (isLoading && detail == null) {
            item {
                LoadingDetailCard()
            }
        }

        item {
            LoanFinancialSummaryGrid(
                capital = currency.format(loan.principalAmount),
                balance = currency.format(loan.remainingBalance),
                installment = currency.format(loan.installmentAmount),
                paid = currency.format(detail?.financialSummary?.amountPaid ?: 0.0),
                total = currency.format(loan.totalAmount),
                late = (detail?.financialSummary?.installmentsLate ?: 0).toString(),
            )
        }

        val overdueCount = detail?.financialSummary?.overdueInstallmentsCount ?: 0
        if (overdueCount > 0) {
            item {
                OverdueInstallmentsCard(
                    count = overdueCount,
                    overdueTotal = currency.format(detail?.financialSummary?.overdueInstallmentsTotal ?: 0.0),
                    lateFeeTotal = currency.format(detail?.financialSummary?.overdueLateFeeTotal ?: 0.0),
                    dueTodayTotal = currency.format(detail?.financialSummary?.totalDueToday ?: 0.0),
                )
            }
        }

        item {
            SectionHeader(
                title = "Cuotas",
                action = if (installments.isNotEmpty()) "Ver calendario" else null,
            )
        }

        if (installments.isEmpty()) {
            item {
                EmptySectionCard("Aún no hay cuotas cargadas para este préstamo.")
            }
        } else {
            items(installments, key = { it.id }) { installment ->
                LoanInstallmentCard(
                    installment = installment,
                    formatAmount = { currency.format(it) },
                    onOpenInstallment = onOpenInstallment,
                    onWaiveLateFee = onWaiveInstallmentLateFee,
                    isWaivingLateFee = isLoading,
                )
            }
        }

        if (detail != null && detail.documents.isNotEmpty()) {
            item {
                SectionHeader(title = "Documentos")
            }

            item {
                LoanDocumentsCard(
                    documents = detail.documents,
                    canGenerate = onGenerateDocument != null,
                    isGenerating = isDocumentGenerating,
                    onGenerate = { type -> onGenerateDocument?.invoke(loan.id, type) },
                )
            }
        }

        if (canManageContracts) {
            item {
                SectionHeader(title = "Contrato digital")
            }

            item {
                LoanContractCard(
                    contract = contract,
                    isLoading = isContractLoading,
                    onGenerate = { onGenerateContract?.invoke(loan.id) },
                )
            }
        }

        if (payments.isNotEmpty()) {
            item {
                SectionHeader(title = "Pagos relacionados")
            }

            item {
                LoanPaymentsCard(
                    payments = payments,
                    formatAmount = { currency.format(it) },
                )
            }
        }
    }

        // Un solo botón que despliega las acciones disponibles (ocupa menos espacio).
        val hasActions = onEditLoan != null || onDeleteLoan != null || canPay || onSendAccountStatement != null
        if (hasActions) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End,
            ) {
                AnimatedVisibility(visible = actionsExpanded) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.End,
                    ) {
                        if (canPay) {
                            ExtendedFloatingActionButton(
                                onClick = { actionsExpanded = false; showPaymentDialog = true },
                                containerColor = PrimaryContainer,
                                contentColor = Color.White,
                                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                                text = { Text("Registrar pago", fontWeight = FontWeight.Bold) },
                            )
                        }

                        if (onEditLoan != null) {
                            ExtendedFloatingActionButton(
                                onClick = { actionsExpanded = false; onEditLoan() },
                                containerColor = Color(0xFF2E6DA4),
                                contentColor = Color.White,
                                icon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                                text = { Text("Editar préstamo", fontWeight = FontWeight.Bold) },
                            )
                        }

                        if (onSendAccountStatement != null) {
                            ExtendedFloatingActionButton(
                                onClick = {
                                    if (!isSendingStatement) {
                                        actionsExpanded = false
                                        onSendAccountStatement()
                                    }
                                },
                                containerColor = Color(0xFF128C7E),
                                contentColor = Color.White,
                                icon = {
                                    if (isSendingStatement) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = Color.White,
                                        )
                                    } else {
                                        Icon(Icons.Outlined.Send, contentDescription = null)
                                    }
                                },
                                text = {
                                    Text(
                                        if (isSendingStatement) "Preparando..." else "Enviar estado de cuenta",
                                        fontWeight = FontWeight.Bold,
                                    )
                                },
                            )
                        }

                        if (onDeleteLoan != null) {
                            ExtendedFloatingActionButton(
                                onClick = { actionsExpanded = false; showDeleteDialog = true },
                                containerColor = Color(0xFFDC2626),
                                contentColor = Color.White,
                                icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                                text = { Text("Eliminar préstamo", fontWeight = FontWeight.Bold) },
                            )
                        }
                    }
                }

                FloatingActionButton(
                    onClick = { actionsExpanded = !actionsExpanded },
                    containerColor = PrimaryContainer,
                    contentColor = Color.White,
                ) {
                    Icon(
                        imageVector = if (actionsExpanded) Icons.Outlined.Close else Icons.Outlined.MoreVert,
                        contentDescription = if (actionsExpanded) "Cerrar acciones" else "Acciones",
                    )
                }
            }
        }
    }
}

/**
 * Cuadros de deuda vencida: cuotas vencidas (sin saldar), mora acumulada y el
 * total a pagar hoy (cuotas + mora). Todo viene ya calculado desde el backend.
 */
@Composable
private fun OverdueInstallmentsCard(
    count: Int,
    overdueTotal: String,
    lateFeeTotal: String,
    dueTodayTotal: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DebtBox(
                title = "Cuotas vencidas",
                subtitle = if (count == 1) "1 cuota sin saldar" else "$count cuotas sin saldar",
                amount = overdueTotal,
                background = ErrorContainer,
                accent = Error,
            )
            DebtBox(
                title = "Mora",
                subtitle = "Mora pendiente acumulada",
                amount = lateFeeTotal,
                background = Color(0xFFFFF1E6),
                accent = Orange,
            )
            DebtBox(
                title = "Total a pagar hoy",
                subtitle = "Cuotas vencidas + mora",
                amount = dueTodayTotal,
                background = Error,
                accent = Color.White,
                emphasized = true,
            )
        }
    }
}

@Composable
private fun DebtBox(
    title: String,
    subtitle: String,
    amount: String,
    background: Color,
    accent: Color,
    emphasized: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = accent.copy(alpha = if (emphasized) 0.85f else 0.75f),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = amount,
            style = if (emphasized) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = accent,
            maxLines = 1,
        )
    }
}

/**
 * Contrato digital del préstamo. Permite generarlo y compartir el enlace de firma
 * por WhatsApp; el cliente firma en una página web desde su celular. Espeja el
 * módulo de contratos de la web.
 */
@Composable
private fun LoanContractCard(
    contract: com.sistemaprestamista.mobile.data.model.ContractSummary?,
    isLoading: Boolean,
    onGenerate: () -> Unit,
) {
    val context = LocalContext.current

    fun openUrl(url: String?) {
        if (!url.isNullOrBlank()) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    val statusLabel = when (contract?.status) {
        "generated" -> "Generado"
        "sent" -> "Enviado"
        "viewed" -> "Visto"
        "signed" -> "Firmado"
        "cancelled" -> "Anulado"
        "expired" -> "Vencido"
        else -> contract?.status.orEmpty()
    }
    val isSigned = contract?.status == "signed"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (contract == null) {
                Text(
                    text = "Genera el contrato y envíalo al cliente para que lo firme desde su celular.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextVariant,
                )
                Button(
                    onClick = onGenerate,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Generar contrato digital", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(contract.contractNumber, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextMain)
                        Text("v${contract.version}", style = MaterialTheme.typography.labelSmall, color = TextVariant)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSigned) SuccessSoft else SecondaryContainer)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSigned) Success else PrimaryContainer,
                        )
                    }
                }

                if (isSigned) {
                    Text("El cliente firmó el contrato.", style = MaterialTheme.typography.bodyMedium, color = Success)
                    OutlinedButton(onClick = { openUrl(contract.verifyUrl) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Ver verificación")
                    }
                } else {
                    if (contract.whatsappUrl != null) {
                        Button(
                            onClick = { openUrl(contract.whatsappUrl) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Success),
                        ) {
                            Text("Enviar por WhatsApp", fontWeight = FontWeight.Bold)
                        }
                    }
                    if (contract.signingUrl != null) {
                        OutlinedButton(onClick = { openUrl(contract.signingUrl) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Abrir enlace de firma")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Espeja la sección de documentos del sistema web: cada tipo se puede
 * generar una vez y luego abrir/compartir su PDF.
 */
@Composable
private fun LoanDocumentsCard(
    documents: List<LoanDocument>,
    canGenerate: Boolean,
    isGenerating: Boolean,
    onGenerate: (String) -> Unit,
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            documents.forEachIndexed { index, document ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (document.generated) SuccessSoft else SurfaceContainerLow),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = null,
                            tint = if (document.generated) Success else Outline,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = document.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextMain,
                            maxLines = 1,
                        )

                        Text(
                            text = if (document.generated) {
                                "Generado · ${document.createdAt.orEmpty().take(10)}"
                            } else {
                                "No generado"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (document.generated) Success else TextVariant,
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (document.whatsappUrl != null) {
                            TextButton(
                                onClick = {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(document.whatsappUrl)),
                                    )
                                },
                            ) {
                                Text(
                                    text = "Enviar",
                                    fontWeight = FontWeight.Bold,
                                    color = Success,
                                )
                            }
                        }

                        when {
                            document.downloadUrl != null -> {
                                TextButton(
                                    onClick = {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse(document.downloadUrl)),
                                        )
                                    },
                                ) {
                                    Text(
                                        text = "Abrir",
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryContainer,
                                    )
                                }
                            }

                            canGenerate -> {
                                TextButton(
                                    onClick = { onGenerate(document.documentType) },
                                    enabled = !isGenerating,
                                ) {
                                    Text(
                                        text = if (isGenerating) "Generando..." else "Generar",
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryContainer,
                                    )
                                }
                            }
                        }
                    }
                }

                if (index < documents.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(OutlineVariant.copy(alpha = 0.45f)),
                    )
                }
            }
        }
    }
}

/**
 * Diálogo de cobro para el back-office: monto prellenado con la cuota del
 * préstamo y método de pago. La lógica de registro vive en el ViewModel.
 */
@Composable
private fun RegisterPaymentDialog(
    loan: LoanSummary,
    detail: LoanDetail?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Long?, Double?) -> Unit,
) {
    val currency = rememberCurrency()
    val nextCollectibleInstallment = remember(detail) {
        detail?.installments
            ?.filter { it.status.trim().lowercase() != "cancelled" && it.hasPendingCharge }
            ?.minByOrNull { it.installmentNumber }
    }
    val allowsCapitalPrepayment = detail?.allowsCapitalPrepayment == true && nextCollectibleInstallment != null
    val suggestedAmount = nextCollectibleInstallment?.pendingAmount?.takeIf { it > 0.0 }
        ?: loan.installmentAmount.takeIf { it > 0.0 }
        ?: loan.remainingBalance.takeIf { it > 0.0 }
        ?: 0.0

    var amount by remember(suggestedAmount) { mutableStateOf(paymentInput(suggestedAmount)) }
    var capitalText by remember(loan.id, nextCollectibleInstallment?.id) { mutableStateOf("") }
    var method by remember { mutableStateOf(PaymentMethod.Cash) }
    var allocationMode by remember { mutableStateOf(AllocationMode.PrincipalAndInterest) }

    val isCapitalPrepaymentMode = allocationMode == AllocationMode.CurrentPlusCapital
    val currentChargeAmount = nextCollectibleInstallment?.let { installment ->
        if (installment.pendingPrincipal <= 0.01 && installment.pendingInterest > 0) {
            installment.pendingLateFee + installment.pendingInterest
        } else {
            installment.pendingAmount
        }
    } ?: 0.0
    val suggestedCapital = nextCollectibleInstallment?.let { installment ->
        val principalCoveredByCurrentCharge = if (installment.pendingPrincipal <= 0.01 && installment.pendingInterest > 0) {
            0.0
        } else {
            installment.pendingPrincipal
        }
        (loan.remainingBalance - principalCoveredByCurrentCharge).coerceAtLeast(0.0)
    } ?: 0.0
    val parsedCapital = capitalText.toDoubleOrNull()
    val parsedAmount = if (isCapitalPrepaymentMode) {
        parsedCapital?.let { currentChargeAmount + it }
    } else {
        amount.toDoubleOrNull()
    }

    val capitalError = when {
        !isCapitalPrepaymentMode -> null
        !allowsCapitalPrepayment -> "Este préstamo no permite abono a capital."
        capitalText.isBlank() || parsedCapital == null -> "Indica el abono o saldo a capital."
        parsedCapital <= 0 -> "El abono debe ser mayor que cero."
        parsedCapital - suggestedCapital > 0.01 -> "No puede exceder ${currency.format(suggestedCapital)}."
        else -> null
    }
    val amountError = when {
        isCapitalPrepaymentMode -> capitalError
        amount.isBlank() -> null
        parsedAmount == null || parsedAmount <= 0 -> "Indica un monto válido."
        else -> null
    }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Registrar pago") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = buildString {
                        append("Préstamo #${loan.loanNumber}")
                        loan.client?.fullName?.let { append(" · ").append(it) }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextMain,
                )

                Text(
                    text = buildString {
                        append("Balance capital: ${currency.format(loan.remainingBalance)}")
                        nextCollectibleInstallment?.let {
                            append(" · Cuota objetivo #${it.installmentNumber}: ${currency.format(it.pendingAmount)}")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextVariant,
                )

                OutlinedTextField(
                    value = if (isCapitalPrepaymentMode) paymentInput(currentChargeAmount) else amount,
                    onValueChange = { if (!isCapitalPrepaymentMode) amount = it },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCapitalPrepaymentMode,
                    label = { Text(if (isCapitalPrepaymentMode) "Interés actual de la cuota" else "Monto a cobrar") },
                    singleLine = true,
                    isError = amountError != null && !isCapitalPrepaymentMode,
                    supportingText = {
                        if (isCapitalPrepaymentMode) {
                            nextCollectibleInstallment?.let {
                                Text("Cuota #${it.installmentNumber}: se cobra la mora/interés actual antes del abono.")
                            }
                        } else {
                            amountError?.let { Text(it) }
                        }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.AttachMoney,
                            contentDescription = null,
                            tint = if (amountError != null && !isCapitalPrepaymentMode) Error else TextVariant,
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp),
                )

                PaymentMethodSelector(
                    selected = method,
                    onSelected = { method = it },
                )

                AllocationModeSelector(
                    selected = allocationMode,
                    hasLateFee = false,
                    includeCapitalPrepayment = allowsCapitalPrepayment,
                    onSelected = { selectedMode ->
                        allocationMode = selectedMode
                        if (selectedMode == AllocationMode.CurrentPlusCapital && capitalText.isBlank()) {
                            capitalText = paymentInput(suggestedCapital)
                        }
                    },
                )

                if (isCapitalPrepaymentMode) {
                    OutlinedTextField(
                        value = capitalText,
                        onValueChange = { capitalText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Abono o saldo a capital") },
                        singleLine = true,
                        isError = capitalError != null,
                        supportingText = {
                            Text(capitalError ?: "Para saldar, deja el capital pendiente completo.")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.AttachMoney,
                                contentDescription = null,
                                tint = if (capitalError != null) Error else TextVariant,
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(14.dp),
                    )

                    CapitalPrepaymentSummary(
                        cuota = currentChargeAmount,
                        capital = parsedCapital?.takeIf { it > 0 } ?: 0.0,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        paymentInput(parsedAmount ?: 0.0),
                        method.apiValue,
                        allocationMode.apiValue,
                        nextCollectibleInstallment?.id?.takeIf { isCapitalPrepaymentMode },
                        parsedCapital?.takeIf { isCapitalPrepaymentMode },
                    )
                },
                enabled = !isLoading && parsedAmount != null && parsedAmount > 0 && amountError == null,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryContainer),
            ) {
                Text(if (isLoading) "Procesando..." else "Cobrar")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading,
            ) {
                Text("Cancelar")
            }
        },
    )
}
@Composable
private fun LoanHeaderCard(
    loan: LoanSummary,
    detail: LoanDetail?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryContainer),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(130.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f)),
            )

            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "PRÉSTAMO NO.",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.72f),
                        )

                        Text(
                            text = "#${loan.loanNumber}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    LoanStatusBadge(
                        text = loanStatusLabel(loan.status),
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    Column {
                        Text(
                            text = loan.client?.fullName ?: "Cliente no disponible",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                        )

                        Text(
                            text = "Cliente verificado",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.70f),
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.12f)),
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        LoanHeaderInfoItem(
                            icon = Icons.Outlined.Payments,
                            label = "Frecuencia",
                            value = formatPaymentFrequency(loan.paymentFrequency),
                            modifier = Modifier.weight(1f),
                        )

                        LoanHeaderInfoItem(
                            icon = Icons.Outlined.Percent,
                            label = "Tasa de interés",
                            value = detail?.let { "${it.interestRate}%" } ?: "-",
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        LoanHeaderInfoItem(
                            icon = Icons.Outlined.CalendarToday,
                            label = "Inicio",
                            value = detail?.startDate ?: "-",
                            modifier = Modifier.weight(1f),
                        )

                        LoanHeaderInfoItem(
                            icon = Icons.Outlined.EventRepeat,
                            label = "Primer pago",
                            value = detail?.firstPaymentDate ?: "-",
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoanHeaderInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.70f),
                modifier = Modifier.size(16.dp),
            )

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.70f),
            )
        }

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
        )
    }
}

@Composable
private fun LoanStatusBadge(
    text: String,
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
        )
    }
}

@Composable
private fun LoanFinancialSummaryGrid(
    capital: String,
    balance: String,
    installment: String,
    paid: String,
    total: String,
    late: String,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            LoanMetricCard(
                title = "Capital",
                value = capital,
                valueColor = Primary,
                accentColor = Primary,
                modifier = Modifier.weight(1f),
            )

            LoanMetricCard(
                title = "Balance",
                value = balance,
                valueColor = Error,
                accentColor = Error,
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            LoanMetricCard(
                title = "Cuota",
                value = installment,
                valueColor = PrimaryContainer,
                accentColor = null,
                modifier = Modifier.weight(1f),
            )

            LoanMetricCard(
                title = "Pagado",
                value = paid,
                valueColor = Success,
                accentColor = null,
                modifier = Modifier.weight(1f),
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "Total del préstamo",
                        style = MaterialTheme.typography.labelSmall,
                        color = Secondary,
                    )

                    Text(
                        text = total,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Secondary,
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        text = "Atrasos",
                        style = MaterialTheme.typography.labelSmall,
                        color = Error,
                    )

                    Text(
                        text = late,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Error,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoanMetricCard(
    title: String,
    value: String,
    valueColor: Color,
    accentColor: Color?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.height(104.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
        ) {
            if (accentColor != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(0.05f)
                        .background(accentColor),
                )

                Column(
                    modifier = Modifier
                        .weight(0.95f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    MetricText(title = title, value = value, valueColor = valueColor)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    MetricText(title = title, value = value, valueColor = valueColor)
                }
            }
        }
    }
}

@Composable
private fun MetricText(
    title: String,
    value: String,
    valueColor: Color,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = Secondary,
    )

    Spacer(Modifier.height(4.dp))

    Text(
        text = value,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = valueColor,
        maxLines = 1,
    )
}

@Composable
private fun SectionHeader(
    title: String,
    action: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextMain,
        )

        if (action != null) {
            Text(
                text = action,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Primary,
            )
        }
    }
}

@Composable
private fun LoanInstallmentCard(
    installment: InstallmentSummary,
    formatAmount: (Double) -> String,
    onOpenInstallment: (Long) -> Unit,
    onWaiveLateFee: ((Long) -> Unit)?,
    isWaivingLateFee: Boolean,
) {
    val isLate = installment.daysLate > 0
    val installmentNumber = installment.installmentNumber ?: 0
    val pendingAmount = formatAmount(installment.pendingAmount)
    val canWaiveLateFee = onWaiveLateFee != null && installment.pendingLateFee > 0 && installment.status !in setOf("paid", "cancelled")
    var showWaiveLateFeeConfirmation by remember(installment.id) { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isLate) ErrorContainer else SurfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = installmentNumber.toString().padStart(2, '0'),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isLate) Error else Primary,
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = installment.dueDate ?: "-",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextMain,
                    )

                    Text(
                        text = if (isLate) {
                            "Vencida hace ${installment.daysLate} días"
                        } else {
                            "$pendingAmount pendiente"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isLate) Error else Secondary,
                    )

                    Text(
                        text = "Capital ${formatAmount(installment.pendingPrincipal)} · Interés ${formatAmount(installment.pendingInterest)} · Mora ${formatAmount(installment.pendingLateFee)}",
                        modifier = if (canWaiveLateFee) {
                            Modifier.pointerInput(installment.id) {
                                detectTapGestures(onLongPress = { showWaiveLateFeeConfirmation = true })
                            }
                        } else {
                            Modifier
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = TextVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                InstallmentStatusBadge(
                    text = if (isLate) "Vencida" else installmentStatusLabel(installment.status),
                    isLate = isLate,
                )

                OutlinedButton(
                    onClick = { onOpenInstallment(installment.id) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Primary,
                    ),
                    border = null,
                ) {
                    Text(
                        text = "Ver cuota",
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(Modifier.width(4.dp))

                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }

    if (showWaiveLateFeeConfirmation) {
        AlertDialog(
            onDismissRequest = { if (!isWaivingLateFee) showWaiveLateFeeConfirmation = false },
            title = { Text("Eliminar mora") },
            text = { Text("Se pondrá en cero la mora pendiente de la cuota #$installmentNumber.") },
            confirmButton = {
                Button(
                    onClick = {
                        showWaiveLateFeeConfirmation = false
                        onWaiveLateFee?.invoke(installment.id)
                    },
                    enabled = !isWaivingLateFee,
                    colors = ButtonDefaults.buttonColors(containerColor = Error),
                ) {
                    Text("Eliminar mora")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showWaiveLateFeeConfirmation = false },
                    enabled = !isWaivingLateFee,
                ) {
                    Text("Cancelar")
                }
            },
        )
    }
}

@Composable
private fun InstallmentStatusBadge(
    text: String,
    isLate: Boolean,
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isLate) ErrorContainer else SecondaryContainer)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isLate) Error else Secondary,
            maxLines = 1,
        )
    }
}

@Composable
private fun LoanPaymentsCard(
    payments: List<PaymentReceipt>,
    formatAmount: (Double) -> String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            payments.forEachIndexed { index, payment ->
                LoanPaymentRow(
                    payment = payment,
                    amount = formatAmount(payment.amount),
                    showDivider = index < payments.lastIndex,
                )
            }
        }
    }
}

@Composable
private fun LoanPaymentRow(
    payment: PaymentReceipt,
    amount: String,
    showDivider: Boolean,
) {
    val isCancelled = payment.status.equals("cancelled", ignoreCase = true) ||
            payment.status.equals("anulado", ignoreCase = true)

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (isCancelled) SurfaceContainerHigh else SuccessSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ReceiptLong,
                        contentDescription = null,
                        tint = if (isCancelled) Secondary else Success,
                        modifier = Modifier.size(22.dp),
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = "Recibo #${payment.receiptNumber}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextMain,
                    )

                    Text(
                        text = payment.paymentDate ?: "-",
                        style = MaterialTheme.typography.labelSmall,
                        color = Secondary,
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = amount,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isCancelled) Secondary else Primary,
                )

                Text(
                    text = payment.status.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isCancelled) Error else Success,
                    maxLines = 1,
                )
            }
        }

        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(SurfaceContainerLow),
            )
        }
    }
}

@Composable
private fun LoadingDetailCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CircularProgressIndicator(
                color = Primary,
            )

            Text(
                text = "Cargando detalle del préstamo...",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Secondary,
            )
        }
    }
}

@Composable
private fun LoanLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CircularProgressIndicator(color = Primary)

                Text(
                    text = "Cargando préstamo...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextMain,
                )

                Text(
                    text = "Estamos obteniendo los datos del préstamo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextVariant,
                )
            }
        }
    }
}

@Composable
private fun LoanNotFoundState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SearchOff,
                        contentDescription = null,
                        tint = Outline,
                        modifier = Modifier.size(38.dp),
                    )
                }

                Text(
                    text = "Préstamo no encontrado",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextMain,
                )

                Text(
                    text = "No se encontró el préstamo seleccionado.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextVariant,
                )
            }
        }
    }
}

@Composable
private fun EmptySectionCard(
    message: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(20.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = TextVariant,
        )
    }
}
