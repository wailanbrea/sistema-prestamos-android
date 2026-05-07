package com.sistemaprestamista.mobile.ui

import com.sistemaprestamista.mobile.data.model.DashboardSummary
import com.sistemaprestamista.mobile.data.model.ClientDetail
import com.sistemaprestamista.mobile.data.model.ClientSummary
import com.sistemaprestamista.mobile.data.model.CollectorSummary
import com.sistemaprestamista.mobile.data.model.CollectorRoute
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
    val errorMessage: String? = null,
    val successMessage: String? = null,
) {
    val isAuthenticated: Boolean = user != null
    val isCollector: Boolean = user?.permissions?.contains("payments.create") == true
}
