package com.sistemaprestamista.mobile.data

import com.sistemaprestamista.mobile.data.model.DashboardSummary
import com.sistemaprestamista.mobile.data.model.ClientDetail
import com.sistemaprestamista.mobile.data.model.ClientSummary
import com.sistemaprestamista.mobile.data.model.CollectorSummary
import com.sistemaprestamista.mobile.data.model.InstallmentSummary
import com.sistemaprestamista.mobile.data.model.InstallmentDetail
import com.sistemaprestamista.mobile.data.model.LoanDetail
import com.sistemaprestamista.mobile.data.model.LoanSummary
import com.sistemaprestamista.mobile.data.model.PaymentHistoryFilters
import com.sistemaprestamista.mobile.data.model.PaymentReceipt
import com.sistemaprestamista.mobile.data.model.UserProfile
import com.sistemaprestamista.mobile.data.remote.PrestamistaApiClient

class PrestamistaRepository(
    private val apiClient: PrestamistaApiClient,
    private val sessionStore: SessionStore,
) {
    fun savedToken(): String? = sessionStore.token()

    fun login(email: String, password: String): UserProfile {
        val result = apiClient.login(
            email = email.trim(),
            password = password,
            deviceName = "Android",
        )
        sessionStore.saveToken(result.accessToken)

        return result.user
    }

    fun me(): UserProfile = apiClient.me(requiredToken())

    fun dashboard(): DashboardSummary = apiClient.dashboard(requiredToken())

    fun collectorSummary(): CollectorSummary = apiClient.collectorSummary(requiredToken())

    fun collectorClients(): List<ClientSummary> = apiClient.collectorClients(requiredToken())

    fun collectorClient(clientId: Long): ClientDetail = apiClient.collectorClient(requiredToken(), clientId)

    fun collectorLoans(): List<LoanSummary> = apiClient.collectorLoans(requiredToken())

    fun collectorLoan(loanId: Long): LoanDetail = apiClient.collectorLoan(requiredToken(), loanId)

    fun collectorInstallments(): List<InstallmentSummary> = apiClient.collectorInstallments(requiredToken())

    fun collectorInstallment(installmentId: Long): InstallmentDetail {
        return apiClient.collectorInstallment(requiredToken(), installmentId)
    }

    fun collectorPayments(filters: PaymentHistoryFilters = PaymentHistoryFilters()): List<PaymentReceipt> {
        return apiClient.collectorPayments(requiredToken(), filters)
    }

    fun collectorPayment(paymentId: Long): PaymentReceipt = apiClient.collectorPayment(requiredToken(), paymentId)

    fun registerCollectorPayment(
        loanId: Long,
        amount: Double,
        paymentDate: String,
        paymentMethod: String,
        mobileUuid: String,
    ): PaymentReceipt = apiClient.registerCollectorPayment(
        token = requiredToken(),
        loanId = loanId,
        amount = amount,
        paymentDate = paymentDate,
        paymentMethod = paymentMethod,
        mobileUuid = mobileUuid,
    )

    fun logout() {
        val token = sessionStore.token()
        if (token != null) {
            runCatching { apiClient.logout(token) }
        }
        sessionStore.clear()
    }

    private fun requiredToken(): String {
        return sessionStore.token() ?: error("No hay sesión activa.")
    }
}
