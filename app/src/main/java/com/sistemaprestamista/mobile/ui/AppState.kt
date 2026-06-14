package com.sistemaprestamista.mobile.ui

import com.sistemaprestamista.mobile.data.model.AdminReportSummary
import com.sistemaprestamista.mobile.data.model.CashMovementItem
import com.sistemaprestamista.mobile.data.model.CashSummary
import com.sistemaprestamista.mobile.data.model.ClientRegistrationLink
import com.sistemaprestamista.mobile.data.model.CollectorDetail
import com.sistemaprestamista.mobile.data.model.CollectorOption
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
import com.sistemaprestamista.mobile.data.model.LoanQuote
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
    val cachedPolylineRouteId: Long? = null,
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
    val selectedLoanContract: com.sistemaprestamista.mobile.data.model.ContractSummary? = null,
    val isContractLoading: Boolean = false,
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
    val isDocumentGenerating: Boolean = false,
    // Alta de clientes, cotizaciones y préstamos (back-office)
    val isClientSaving: Boolean = false,
    val lastCreatedClientId: Long? = null,
    val adminCollectors: List<CollectorOption> = emptyList(),
    val isLoanSaving: Boolean = false,
    val lastCreatedLoanId: Long? = null,
    val isLoanUpdating: Boolean = false,
    val isLinkGenerating: Boolean = false,
    val lastGeneratedRegistrationLink: ClientRegistrationLink? = null,
    val adminQuotes: List<LoanQuote> = emptyList(),
    val selectedQuote: LoanQuote? = null,
    val isQuoteSaving: Boolean = false,
    val isQuotesLoading: Boolean = false,
    val lastCreatedQuoteId: Long? = null,
    // Caja / Contabilidad
    val expenses: List<ExpenseItem> = emptyList(),
    val expenseCategories: List<ExpenseCategoryOption> = emptyList(),
    val cashMovements: List<CashMovementItem> = emptyList(),
    val cashSummary: CashSummary? = null,
    val isExpenseSaving: Boolean = false,
    // Cobradores (back-office)
    val selectedCollectorDetail: CollectorDetail? = null,
    val isCollectorSaving: Boolean = false,
    val lastCreatedCollectorId: Long? = null,
    val isPaymentCancelling: Boolean = false,
    val isMovementSaving: Boolean = false,
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

    /**
     * Cobro desde back-office (admin/payments): espeja el gate del servidor,
     * que exige cartera global ADEMÁS de payments.create.
     */
    val canRegisterAdminPayment: Boolean = canManagePortfolio && permissions.contains("payments.create")

    /**
     * Generación de documentos del préstamo. La ruta admin exige documents.generate;
     * la del cobrador solo collector.access (su propia cartera).
     */
    val canGenerateDocuments: Boolean = isCollector || permissions.contains("documents.generate")

    /** Alta de clientes desde back-office (espeja clients.create de la web). */
    val canCreateClients: Boolean = canManagePortfolio && permissions.contains("clients.create")

    /** Creación de préstamos desde la app (mismo gate que la web: loans.create). */
    val canCreateLoans: Boolean = canManagePortfolio && permissions.contains("loans.create")

    /** Edición de préstamos desde la app (mismo gate que la web: loans.update). */
    val canEditLoan: Boolean = canManagePortfolio && permissions.contains("loans.update")

    /** Editar clientes. */
    val canUpdateClients: Boolean = canManagePortfolio && permissions.contains("clients.update")

    /** Eliminar clientes. */
    val canDeleteClients: Boolean = canManagePortfolio

    /** Eliminar préstamos. */
    val canDeleteLoan: Boolean = canManagePortfolio

    /** Cancelar/anular pagos. */
    val canCancelPayments: Boolean = canManagePortfolio && permissions.contains("payments.cancel")

    /** Cotizaciones (mismo gate que la web: quotes.manage). */
    val canManageQuotes: Boolean = canManagePortfolio && permissions.contains("quotes.manage")

    /** Puede ver reportes/informes. */
    val canViewReports: Boolean = permissions.contains("reports.view")

    /**
     * Contratos digitales (mismo gate que la web: legal.manage). Requiere además
     * cartera global, ya que el endpoint vive bajo admin/.
     */
    val canManageContracts: Boolean = canManagePortfolio && permissions.contains("legal.manage")

    /**
     * Caja: registrar gastos / ver caja. Se limita a roles SIN cartera global
     * (p. ej. Caja/Contabilidad), para no saturar la barra del administrador
     * (que gestiona gastos/caja desde la web).
     */
    val canManageExpenses: Boolean = permissions.contains("expenses.manage") && !canManagePortfolio && !isCollector
    val canViewCash: Boolean = permissions.contains("cash.view") && !canManagePortfolio && !isCollector
}
