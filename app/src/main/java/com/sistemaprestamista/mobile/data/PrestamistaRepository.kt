package com.sistemaprestamista.mobile.data

import android.content.Context
import com.sistemaprestamista.mobile.data.model.ClientDetail
import com.sistemaprestamista.mobile.data.model.ClientSummary
import com.sistemaprestamista.mobile.data.model.CollectorSummary
import com.sistemaprestamista.mobile.data.model.CollectorRoute
import com.sistemaprestamista.mobile.data.model.CollectorRouteSession
import com.sistemaprestamista.mobile.data.model.DashboardSummary
import com.sistemaprestamista.mobile.data.model.InstallmentDetail
import com.sistemaprestamista.mobile.data.model.InstallmentSummary
import com.sistemaprestamista.mobile.data.model.LoanDetail
import com.sistemaprestamista.mobile.data.model.LoanSummary
import com.sistemaprestamista.mobile.data.model.MapClient
import com.sistemaprestamista.mobile.data.model.PaymentHistoryFilters
import com.sistemaprestamista.mobile.data.model.PaymentReceipt
import com.sistemaprestamista.mobile.data.model.RoutePoint
import com.sistemaprestamista.mobile.data.model.UserProfile
import com.sistemaprestamista.mobile.data.pending.PendingPayment
import com.sistemaprestamista.mobile.data.pending.PendingPaymentStore
import com.sistemaprestamista.mobile.data.remote.ApiException
import com.sistemaprestamista.mobile.data.remote.GoogleRoutesClient
import com.sistemaprestamista.mobile.data.remote.PrestamistaApiClient
import com.sistemaprestamista.mobile.sync.PendingPaymentSyncScheduler
import java.io.IOException

class PrestamistaRepository(
    private val context: Context,
    private val apiClient: PrestamistaApiClient,
    private val googleRoutesClient: GoogleRoutesClient,
    private val sessionStore: SessionStore,
    private val pendingPaymentStore: PendingPaymentStore,
) {
    fun savedToken(): String? = sessionStore.token()

    fun hasSavedSession(): Boolean = sessionStore.hasToken()

    fun pendingPaymentCount(): Int = pendingPaymentStore.pendingCount()

    fun pendingPayments(): List<PendingPayment> = pendingPaymentStore.allPending()

    fun login(email: String, password: String): UserProfile {
        val result = apiClient.login(
            email = email.trim(),
            password = password,
            deviceName = "Android",
        )
        sessionStore.saveToken(result.accessToken)
        enqueuePendingPaymentSync()

        return result.user
    }

    fun me(): UserProfile = apiClient.me(requiredToken())

    fun dashboard(): DashboardSummary = apiClient.dashboard(requiredToken())

    // --- Back-office / administrador ---

    fun adminClients(search: String? = null): List<ClientSummary> = apiClient.adminClients(requiredToken(), search)

    fun adminClient(clientId: Long): ClientDetail = apiClient.adminClient(requiredToken(), clientId)

    fun adminLoans(status: String? = null, search: String? = null): List<LoanSummary> = apiClient.adminLoans(requiredToken(), status, search)

    fun adminLoan(loanId: Long): LoanDetail = apiClient.adminLoan(requiredToken(), loanId)

    fun adminApprovals(): List<LoanSummary> = apiClient.adminApprovals(requiredToken())

    fun adminApproveLoan(loanId: Long): LoanSummary = apiClient.adminApproveLoan(requiredToken(), loanId)

    fun adminRejectLoan(loanId: Long, reason: String? = null): LoanSummary = apiClient.adminRejectLoan(requiredToken(), loanId, reason)

    fun adminReportSummary(dateFrom: String? = null, dateTo: String? = null): com.sistemaprestamista.mobile.data.model.AdminReportSummary =
        apiClient.adminReportSummary(requiredToken(), dateFrom, dateTo)

    fun adminReportCollectors(dateFrom: String? = null, dateTo: String? = null): List<com.sistemaprestamista.mobile.data.model.CollectorPerformanceRow> =
        apiClient.adminReportCollectors(requiredToken(), dateFrom, dateTo)

    fun requestPasswordReset(email: String): String = apiClient.requestPasswordReset(email.trim())

    fun resetPassword(
        email: String,
        token: String,
        password: String,
        passwordConfirmation: String,
    ): String = apiClient.resetPassword(
        email = email.trim(),
        token = token.trim(),
        password = password,
        passwordConfirmation = passwordConfirmation,
    )

    fun collectorSummary(): CollectorSummary = apiClient.collectorSummary(requiredToken())

    fun collectorClients(): List<ClientSummary> = apiClient.collectorClients(requiredToken())

    fun collectorClient(clientId: Long): ClientDetail = apiClient.collectorClient(requiredToken(), clientId)

    fun collectorMapClients(): List<MapClient> = apiClient.collectorMapClients(requiredToken())

    fun collectorRoutes(): List<CollectorRoute> = apiClient.collectorRoutes(requiredToken())

    fun activeRouteSession(): CollectorRouteSession? = apiClient.activeRouteSession(requiredToken())

    fun startRouteSession(routeId: Long): CollectorRouteSession = apiClient.startRouteSession(requiredToken(), routeId)

    fun sendRouteLocation(
        sessionId: Long,
        latitude: Double,
        longitude: Double,
        accuracyMeters: Int?,
        batteryLevel: Int?,
        recordedAt: String,
    ): CollectorRouteSession = apiClient.sendRouteLocation(
        token = requiredToken(),
        sessionId = sessionId,
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = accuracyMeters,
        batteryLevel = batteryLevel,
        recordedAt = recordedAt,
    )

    fun finishRouteSession(sessionId: Long): CollectorRouteSession = apiClient.finishRouteSession(requiredToken(), sessionId)

    fun drivingRoute(points: List<RoutePoint>): List<RoutePoint> = googleRoutesClient.drivingRoute(points)

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
    ): PaymentRegistrationResult {
        val pendingPayment = pendingPaymentStore.create(
            loanId = loanId,
            amount = amount,
            paymentDate = paymentDate,
            paymentMethod = paymentMethod,
            mobileUuid = mobileUuid,
        )

        return runCatching {
            sendPendingPayment(pendingPayment)
        }.fold(
            onSuccess = { receipt ->
                pendingPaymentStore.delete(mobileUuid)
                PaymentRegistrationResult.Sent(receipt)
            },
            onFailure = { throwable ->
                when (throwable) {
                    is ApiException -> {
                        pendingPaymentStore.delete(mobileUuid)
                        throw throwable
                    }
                    else -> {
                        pendingPaymentStore.incrementAttempts(mobileUuid, throwable.message)
                        enqueuePendingPaymentSync()
                        PaymentRegistrationResult.Queued(pendingPaymentStore.pendingCount())
                    }
                }
            },
        )
    }

    fun syncPendingPayments(): PendingPaymentSyncResult {
        if (!hasSavedSession()) {
            return PendingPaymentSyncResult(
                sent = 0,
                failed = 0,
                remaining = pendingPaymentStore.pendingCount(),
                requiresRetry = false,
            )
        }

        var sent = 0
        var failed = 0
        var requiresRetry = false

        for (pendingPayment in pendingPaymentStore.pendingForSync()) {
            try {
                sendPendingPayment(pendingPayment)
                pendingPaymentStore.delete(pendingPayment.mobileUuid)
                sent++
            } catch (exception: ApiException) {
                pendingPaymentStore.markFailed(
                    mobileUuid = pendingPayment.mobileUuid,
                    message = exception.message ?: "Error del servidor.",
                )
                failed++
            } catch (exception: IOException) {
                pendingPaymentStore.incrementAttempts(pendingPayment.mobileUuid, exception.message)
                requiresRetry = true
                break
            } catch (exception: RuntimeException) {
                pendingPaymentStore.incrementAttempts(pendingPayment.mobileUuid, exception.message)
                requiresRetry = true
                break
            }
        }

        return PendingPaymentSyncResult(
            sent = sent,
            failed = failed,
            remaining = pendingPaymentStore.pendingCount(),
            requiresRetry = requiresRetry,
        )
    }

    fun retryPendingPayment(mobileUuid: String): PendingPaymentSyncResult {
        pendingPaymentStore.markPending(mobileUuid)
        return syncPendingPayments()
    }

    fun discardPendingPayment(mobileUuid: String) {
        pendingPaymentStore.delete(mobileUuid)
    }

    fun enqueuePendingPaymentSync() {
        if (pendingPaymentStore.pendingCount() > 0 && hasSavedSession()) {
            PendingPaymentSyncScheduler.enqueue(context)
        }
    }

    fun logout() {
        val token = sessionStore.token()
        if (token != null) {
            runCatching { apiClient.logout(token) }
        }
        sessionStore.clear()
    }

    private fun sendPendingPayment(pendingPayment: PendingPayment): PaymentReceipt {
        return apiClient.registerCollectorPayment(
            token = requiredToken(),
            loanId = pendingPayment.loanId,
            amount = pendingPayment.amount,
            paymentDate = pendingPayment.paymentDate,
            paymentMethod = pendingPayment.paymentMethod,
            mobileUuid = pendingPayment.mobileUuid,
        )
    }

    private fun requiredToken(): String {
        return sessionStore.token() ?: error("No hay sesion activa.")
    }
}
