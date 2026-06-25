package com.sistemaprestamista.mobile.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.RequestQuote
import androidx.compose.ui.graphics.vector.ImageVector
import com.sistemaprestamista.mobile.ui.AppUiState

/**
 * Destinos de la barra inferior. Cada destino declara `isVisible`, un predicado
 * sobre el estado (permisos/rol del usuario). La barra se construye filtrando por
 * ese predicado, de modo que las vistas quedan controladas por el rol:
 *  - Cobrador (payments.create): cartera propia (Cobros, Clientes, Mapa, Historial).
 *  - Admin/Supervisor (collectors.manage sin payments.create): cartera global.
 *  - loans.approve: bandeja de Aprobaciones. reports.view: Reportes.
 */
enum class AppDestination(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val isVisible: (AppUiState) -> Boolean,
    val navigationLabel: String = title,
) {
    Home("home", "Inicio", Icons.Outlined.Home, { true }),

    // Cobrador (cartera propia, datos de /collector).
    Collections("collections", "Cobros", Icons.AutoMirrored.Outlined.ReceiptLong, { it.isCollector }),
    Clients("clients", "Clientes", Icons.Outlined.Groups, { it.isCollector }),
    Map("map", "Mapa", Icons.Outlined.Map, { it.isCollector }),
    Payments("payments", "Historial", Icons.Outlined.History, { it.isCollector }),

    // Back-office (cartera global y supervisión).
    ClientsAdmin("adminClients", "Clientes", Icons.Outlined.Groups, { it.canManagePortfolio }),
    LoansAdmin("adminLoans", "Préstamos", Icons.Outlined.AccountBalance, { it.canManagePortfolio }),
    CollectorsAdmin("adminCollectors", "Cobradores", Icons.Outlined.People, { it.canManagePortfolio }),
    Approvals("approvals", "Aprobar", Icons.Outlined.PendingActions, { it.canApprove }),
    Reports("reports", "Reportes", Icons.Outlined.Assessment, { it.canViewReports }),
    AccountsPayable(
        "accountsPayable",
        "Por pagar",
        Icons.Outlined.RequestQuote,
        { it.canManageAccountsPayable },
        navigationLabel = "Cuentas",
    ),

    // Caja / Contabilidad.
    Expenses("cashboxExpenses", "Gastos", Icons.Outlined.Receipt, { it.canManageExpenses }),
    Cash("cashboxMovements", "Caja", Icons.Outlined.AccountBalanceWallet, { it.canViewCash }),

    Profile("profile", "Perfil", Icons.Outlined.Person, { true }),
}

object AppRoutes {
    const val ClientDetail = "clients/{clientId}"
    const val LoanDetail = "loans/{loanId}"
    const val InstallmentDetail = "installments/{installmentId}"
    const val AdminClientDetail = "admin/clients/{clientId}"
    const val AdminLoanDetail = "admin/loans/{loanId}"
    const val AdminClientCreate = "admin/client-create"
    const val AdminLoanCreate = "admin/loan-create"
    const val AdminLoanEdit = "admin/loan-edit/{loanId}"
    const val AdminClientEdit = "admin/client-edit/{clientId}"
    const val AdminCollectors = "admin/collectors"
    const val AdminCollectorCreate = "admin/collector-create"
    const val AdminCollectorDetail = "admin/collectors/{collectorId}"
    const val AdminCollectorEdit = "admin/collector-edit/{collectorId}"
    const val AdminQuotes = "admin/quotes"
    const val AdminQuoteCreate = "admin/quote-create"
    const val AdminQuoteDetail = "admin/quote/{quoteId}"
    const val AccountPayableDetail = "accounts-payable/{accountId}"
    const val AccountPayableCreate = "accounts-payable/create"
    const val AccountPayableEdit = "accounts-payable/{accountId}/edit"
    const val ReceiptDetail = "receipt"
    const val PrintSettings = "settings/printer"
    const val PendingPayments = "payments/pending"

    fun clientDetail(clientId: Long): String = "clients/$clientId"

    fun loanDetail(loanId: Long): String = "loans/$loanId"

    fun installmentDetail(installmentId: Long): String = "installments/$installmentId"

    fun adminClientDetail(clientId: Long): String = "admin/clients/$clientId"

    fun adminLoanDetail(loanId: Long): String = "admin/loans/$loanId"

    fun adminLoanEdit(loanId: Long): String = "admin/loan-edit/$loanId"

    fun adminClientEdit(clientId: Long): String = "admin/client-edit/$clientId"

    fun adminCollectorDetail(collectorId: Long): String = "admin/collectors/$collectorId"

    fun adminCollectorEdit(collectorId: Long): String = "admin/collector-edit/$collectorId"

    fun adminQuoteDetail(quoteId: Long): String = "admin/quote/$quoteId"

    fun accountPayableDetail(accountId: Long): String = "accounts-payable/$accountId"

    fun accountPayableEdit(accountId: Long): String = "accounts-payable/$accountId/edit"
}
