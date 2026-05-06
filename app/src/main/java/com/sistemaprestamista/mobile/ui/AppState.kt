package com.sistemaprestamista.mobile.ui

import com.sistemaprestamista.mobile.data.model.DashboardSummary
import com.sistemaprestamista.mobile.data.model.ClientSummary
import com.sistemaprestamista.mobile.data.model.CollectorSummary
import com.sistemaprestamista.mobile.data.model.InstallmentSummary
import com.sistemaprestamista.mobile.data.model.LoanSummary
import com.sistemaprestamista.mobile.data.model.PaymentReceipt
import com.sistemaprestamista.mobile.data.model.UserProfile

data class AppUiState(
    val isLoading: Boolean = true,
    val user: UserProfile? = null,
    val dashboard: DashboardSummary? = null,
    val collectorSummary: CollectorSummary? = null,
    val collectorClients: List<ClientSummary> = emptyList(),
    val collectorLoans: List<LoanSummary> = emptyList(),
    val collectorInstallments: List<InstallmentSummary> = emptyList(),
    val lastPaymentReceipt: PaymentReceipt? = null,
    val errorMessage: String? = null,
) {
    val isAuthenticated: Boolean = user != null
    val isCollector: Boolean = user?.permissions?.contains("payments.create") == true
}
