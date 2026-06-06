package com.sistemaprestamista.mobile.ui

import com.sistemaprestamista.mobile.data.model.AdminReportSummary
import com.sistemaprestamista.mobile.data.model.CashMovementItem
import com.sistemaprestamista.mobile.data.model.CashSummary
import com.sistemaprestamista.mobile.data.model.CollectorPerformanceRow
import com.sistemaprestamista.mobile.data.model.ExpenseCategoryOption
import com.sistemaprestamista.mobile.data.model.ExpenseItem
import com.sistemaprestamista.mobile.data.model.DashboardSummary
import com.sistemaprestamista.mobile.data.model.ClientDetail
import com.sistemaprestamista.mobile.data.model.ClientSummary
import com.sistemaprestamista.mobile.data.model.CollectorSummary
import com.sistemaprestamista.mobile.data.model.CollectorRoute
import com.sistemaprestamista.mobile.data.model.CollectorRouteSession
import com.sistemaprestamista.mobile.data.model.InstallmentSummary
import com.sistemaprestamista.mobile.data.model.InstallmentDetail
import com.sistemaprestamista.mobile.data.model.LoanDetail
import com.sistemaprestamista.mobile.data.model.LoanSummary
import com.sistemaprestamista.mobile.data.model.MapClient
import com.sistemaprestamista.mobile.data.model.PaymentHistoryFilters
import com.sistemaprestamista.mobile.data.model.PaymentReceipt
import com.sistemaprestamista.mobile.data.model.RoutePoint
import com.sistemaprestamista.mobile.data.model.UserProfile
import com.sistemaprestamista.mobile.data.pending.PendingPayment

data class AppUiState(
    val isLoading: Boolean = true,
    val hasSavedSession: Boolean = false,
    val pendingPaymentCount: Int = 0,
    val pendingPayments: List<PendingPayment> = emptyList(),
    val isPendingSyncLoading: Boolean = false,
    val user: UserProfile? = null,
    val dashboard: DashboardSummary? = null,
    val collectorSummary: CollectorSummary? = null,
    val collectorClients: List<ClientSummary> = emptyList(),
    val collectorLoans: List<LoanSummary> = emptyList(),
    val collectorInstallments: List<InstallmentSummary> = emptyList(),
    val paymentHistory: List<PaymentReceipt> = emptyList(),
    val mapClients: List<MapClient> = emptyList(),
    val collectorRoutes: List<CollectorRoute> = emptyList(),
    val selectedMapRouteId: Long = 0L,
    val optimizedRouteClientIds: List<Long> = emptyList(),
    val activeRouteSession: CollectorRouteSession? = null,
    val isRouteTrackingLoading: Boolean = false,
    val realRoutePoints: List<RoutePoint> = emptyList(),
    val routeWarning: String? = null,
    val isMapLoading: Boolean = false,
    val paymentHistoryFilters: PaymentHistoryFilters = PaymentHistoryFilters(),
    val isPaymentHistoryLoading: Boolean = false,
    val selectedClientDetail: ClientDetail? = null,
    val selectedLoanDetail: LoanDetail? = null,
    val selectedInstallmentDetail: InstallmentDetail? = null,
    val selectedPaymentDetail: PaymentReceipt? = null,
    val isDetailLoading: Boolean = false,
    val lastPaymentReceipt: PaymentReceipt? = null,
    // Back-office / administrador
    val adminClients: List<ClientSummary> = emptyList(),
    val adminLoans: List<LoanSummary> = emptyList(),
    val adminLoansHasMore: Boolean = false,
    val adminLoansLoadedPage: Int = 1,
    val isLoadingMoreAdminLoans: Boolean = false,
    val pendingApprovals: List<LoanSummary> = emptyList(),
    val reportSummary: AdminReportSummary? = null,
    val collectorPerformance: List<CollectorPerformanceRow> = emptyList(),
    val isApprovalActionLoading: Boolean = false,
    // Caja / Contabilidad
    val expenses: List<ExpenseItem> = emptyList(),
    val expenseCategories: List<ExpenseCategoryOption> = emptyList(),
    val cashMovements: List<CashMovementItem> = emptyList(),
    val cashSummary: CashSummary? = null,
    val isExpenseSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
) {
    private val permissions: List<String> = user?.permissions.orEmpty()

    val isAuthenticated: Boolean = user != null

    /**
     * Cobrador de campo: vinculado a un Collector activo (flag del backend), no el mero
     * permiso `payments.create` (que el Administrador también tiene). Usa la app /collector.
     */
    val isCollector: Boolean = user?.isCollector == true

    /** Vista global de cartera (clientes/préstamos de toda la empresa). */
    val canManagePortfolio: Boolean = permissions.contains("collectors.manage") && !isCollector

    /** Puede aprobar/rechazar préstamos pendientes. */
    val canApprove: Boolean = permissions.contains("loans.approve")

    /** Puede ver reportes/informes. */
    val canViewReports: Boolean = permissions.contains("reports.view")

    /**
     * Caja: registrar gastos / ver caja. Se limita a roles SIN cartera global
     * (p. ej. Caja/Contabilidad), para no saturar la barra del administrador
     * (que gestiona gastos/caja desde la web).
     */
    val canManageExpenses: Boolean = permissions.contains("expenses.manage") && !canManagePortfolio && !isCollector
    val canViewCash: Boolean = permissions.contains("cash.view") && !canManagePortfolio && !isCollector
}
