package com.sistemaprestamista.mobile.data

import android.content.Context
import com.sistemaprestamista.mobile.data.model.CashMovementInput
import com.sistemaprestamista.mobile.data.model.ClientDetail
import com.sistemaprestamista.mobile.data.model.ClientSummary
import com.sistemaprestamista.mobile.data.model.CollectorDetail
import com.sistemaprestamista.mobile.data.model.CollectorCommissionItem
import com.sistemaprestamista.mobile.data.model.CollectorOption
import com.sistemaprestamista.mobile.data.model.CollectorSummary
import com.sistemaprestamista.mobile.data.model.CollectorRoute
import com.sistemaprestamista.mobile.data.model.CollectorRouteSession
import com.sistemaprestamista.mobile.data.model.DashboardSummary
import com.sistemaprestamista.mobile.data.model.InstallmentDetail
import com.sistemaprestamista.mobile.data.model.InstallmentSummary
import com.sistemaprestamista.mobile.data.model.LoanDetail
import com.sistemaprestamista.mobile.data.model.LoanSummary
import com.sistemaprestamista.mobile.data.model.ClientRegistrationLink
import com.sistemaprestamista.mobile.data.model.NewCollectorInput
import com.sistemaprestamista.mobile.data.model.NewLoanInput
import com.sistemaprestamista.mobile.data.model.UpdateClientInput
import com.sistemaprestamista.mobile.data.model.UpdateCollectorInput
import com.sistemaprestamista.mobile.data.model.UpdateLoanInput
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
        // Limpia la caché de cualquier sesión previa para no mezclar datos entre cuentas.
        apiClient.clearCache()
        val result = apiClient.login(
            email = email.trim(),
            password = password,
            deviceName = "Android",
        )
        sessionStore.saveToken(result.accessToken)
        enqueuePendingPaymentSync()

        return result.user
    }

    fun me(cacheOnly: Boolean = false): UserProfile = apiClient.me(requiredToken(), cacheOnly)

    fun dashboard(): DashboardSummary = apiClient.dashboard(requiredToken())

    // --- Back-office / administrador ---

    fun adminCollectors(): List<CollectorOption> = apiClient.adminCollectors(requiredToken())

    fun adminCreateLoan(input: NewLoanInput): LoanSummary = apiClient.adminCreateLoan(requiredToken(), input)

    fun adminUpdateLoan(loanId: Long, input: UpdateLoanInput): LoanDetail = apiClient.adminUpdateLoan(requiredToken(), loanId, input)

    fun adminCreateRegistrationLink(recipientName: String?, recipientPhone: String?): ClientRegistrationLink =
        apiClient.adminCreateRegistrationLink(requiredToken(), recipientName, recipientPhone)

    fun adminClients(search: String? = null): List<ClientSummary> = apiClient.adminClients(requiredToken(), search)

    fun adminClient(clientId: Long): ClientDetail = apiClient.adminClient(requiredToken(), clientId)

    fun adminLoans(status: String? = null, search: String? = null): List<LoanSummary> = apiClient.adminLoans(requiredToken(), status, search)

    fun adminLoansPage(page: Int, status: String? = null, search: String? = null): com.sistemaprestamista.mobile.data.model.Page<LoanSummary> =
        apiClient.adminLoansPage(requiredToken(), page, status, search)

    fun adminLoan(loanId: Long): LoanDetail = apiClient.adminLoan(requiredToken(), loanId)

    fun adminApprovals(): List<LoanSummary> = apiClient.adminApprovals(requiredToken())

    fun adminApproveLoan(loanId: Long): LoanSummary = apiClient.adminApproveLoan(requiredToken(), loanId)

    fun adminRejectLoan(loanId: Long, reason: String? = null): LoanSummary = apiClient.adminRejectLoan(requiredToken(), loanId, reason)

    fun adminReportSummary(dateFrom: String? = null, dateTo: String? = null): com.sistemaprestamista.mobile.data.model.AdminReportSummary =
        apiClient.adminReportSummary(requiredToken(), dateFrom, dateTo)

    fun adminReportCollectors(dateFrom: String? = null, dateTo: String? = null): List<com.sistemaprestamista.mobile.data.model.CollectorPerformanceRow> =
        apiClient.adminReportCollectors(requiredToken(), dateFrom, dateTo)

    fun adminReportCatalog(): List<com.sistemaprestamista.mobile.data.model.ReportCatalogItem> =
        apiClient.adminReportCatalog(requiredToken())

    // --- Caja / Contabilidad ---

    fun cashboxExpenses(): List<com.sistemaprestamista.mobile.data.model.ExpenseItem> = apiClient.cashboxExpenses(requiredToken())

    fun cashboxCategories(): List<com.sistemaprestamista.mobile.data.model.ExpenseCategoryOption> = apiClient.cashboxCategories(requiredToken())

    fun cashboxCreateExpense(categoryId: Long?, description: String, amount: Double, expenseDate: String, paymentMethod: String): com.sistemaprestamista.mobile.data.model.ExpenseItem =
        apiClient.cashboxCreateExpense(requiredToken(), categoryId, description, amount, expenseDate, paymentMethod)

    fun cashboxMovements(): List<com.sistemaprestamista.mobile.data.model.CashMovementItem> = apiClient.cashboxMovements(requiredToken())

    fun cashboxSummary(): com.sistemaprestamista.mobile.data.model.CashSummary = apiClient.cashboxSummary(requiredToken())

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

    fun collectorSummary(cacheOnly: Boolean = false): CollectorSummary = apiClient.collectorSummary(requiredToken(), cacheOnly)

    fun collectorClients(cacheOnly: Boolean = false): List<ClientSummary> = apiClient.collectorClients(requiredToken(), cacheOnly)

    fun collectorClient(clientId: Long): ClientDetail = apiClient.collectorClient(requiredToken(), clientId)

    fun collectorMapClients(cacheOnly: Boolean = false): List<MapClient> = apiClient.collectorMapClients(requiredToken(), cacheOnly)

    fun collectorRoutes(cacheOnly: Boolean = false): List<CollectorRoute> = apiClient.collectorRoutes(requiredToken(), cacheOnly)

    fun activeRouteSession(cacheOnly: Boolean = false): CollectorRouteSession? = apiClient.activeRouteSession(requiredToken(), cacheOnly)

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

    fun collectorLoans(cacheOnly: Boolean = false): List<LoanSummary> = apiClient.collectorLoans(requiredToken(), cacheOnly)

    fun collectorLoan(loanId: Long): LoanDetail = apiClient.collectorLoan(requiredToken(), loanId)

    fun collectorInstallments(cacheOnly: Boolean = false): List<InstallmentSummary> = apiClient.collectorInstallments(requiredToken(), cacheOnly)

    fun collectorInstallment(installmentId: Long): InstallmentDetail {
        return apiClient.collectorInstallment(requiredToken(), installmentId)
    }

    fun collectorPayments(filters: PaymentHistoryFilters = PaymentHistoryFilters(), cacheOnly: Boolean = false): List<PaymentReceipt> {
        return apiClient.collectorPayments(requiredToken(), filters, cacheOnly)
    }

    fun collectorPayment(paymentId: Long): PaymentReceipt = apiClient.collectorPayment(requiredToken(), paymentId)

    fun registerCollectorPayment(
        loanId: Long,
        amount: Double,
        paymentDate: String,
        paymentMethod: String,
        allocationMode: String,
        targetInstallmentId: Long?,
        capitalPrepaymentAmount: Double?,
        mobileUuid: String,
    ): PaymentRegistrationResult {
        val pendingPayment = pendingPaymentStore.create(
            loanId = loanId,
            amount = amount,
            paymentDate = paymentDate,
            paymentMethod = paymentMethod,
            allocationMode = allocationMode,
            targetInstallmentId = targetInstallmentId,
            capitalPrepaymentAmount = capitalPrepaymentAmount,
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

    /**
     * Cobro desde back-office (Administrador): va directo al endpoint admin sin
     * cola offline (el admin trabaja con conexión; un fallo se muestra y reintenta).
     * Idempotente por mobileUuid igual que el flujo del cobrador.
     */
    fun registerAdminPayment(
        loanId: Long,
        amount: Double,
        paymentDate: String,
        paymentMethod: String,
        allocationMode: String,
        targetInstallmentId: Long?,
        mobileUuid: String,
    ): PaymentReceipt {
        return apiClient.adminRegisterPayment(
            token = requiredToken(),
            loanId = loanId,
            amount = amount,
            paymentDate = paymentDate,
            paymentMethod = paymentMethod,
            mobileUuid = mobileUuid,
            allocationMode = allocationMode,
            targetInstallmentId = targetInstallmentId,
        )
    }

    fun generateLoanDocument(loanId: Long, documentType: String, viaAdmin: Boolean): com.sistemaprestamista.mobile.data.model.LoanDocument =
        apiClient.generateLoanDocument(requiredToken(), loanId, documentType, viaAdmin)

    fun loanContract(loanId: Long): com.sistemaprestamista.mobile.data.model.ContractSummary? =
        apiClient.adminLoanContract(requiredToken(), loanId)

    fun generateContract(loanId: Long, contractType: String = "loan_contract"): com.sistemaprestamista.mobile.data.model.ContractSummary =
        apiClient.adminGenerateContract(requiredToken(), loanId, contractType)

    fun adminCreateClient(input: com.sistemaprestamista.mobile.data.model.NewClientInput): ClientSummary =
        apiClient.adminCreateClient(requiredToken(), input)

    fun adminQuotes(): List<com.sistemaprestamista.mobile.data.model.LoanQuote> = apiClient.adminQuotes(requiredToken())

    fun adminCreateQuote(
        clientId: Long?,
        amount: Double,
        interestRate: Double,
        interestType: String,
        paymentFrequency: String,
        calculationMethod: String,
        termQuantity: Int,
    ): com.sistemaprestamista.mobile.data.model.LoanQuote = apiClient.adminCreateQuote(
        token = requiredToken(),
        clientId = clientId,
        amount = amount,
        interestRate = interestRate,
        interestType = interestType,
        paymentFrequency = paymentFrequency,
        calculationMethod = calculationMethod,
        termQuantity = termQuantity,
    )

    fun adminQuote(quoteId: Long): com.sistemaprestamista.mobile.data.model.LoanQuote = apiClient.adminQuote(requiredToken(), quoteId)

    fun adminDeleteQuote(quoteId: Long) = apiClient.adminDeleteQuote(requiredToken(), quoteId)

    fun adminUpdateClient(clientId: Long, input: UpdateClientInput): ClientDetail =
        apiClient.adminUpdateClient(requiredToken(), clientId, input)

    fun adminDeleteClient(clientId: Long) = apiClient.adminDeleteClient(requiredToken(), clientId)

    fun adminDeleteLoan(loanId: Long) = apiClient.adminDeleteLoan(requiredToken(), loanId)

    fun adminPayments(): List<PaymentReceipt> =
        apiClient.adminPayments(requiredToken())

    fun adminPayment(paymentId: Long): PaymentReceipt =
        apiClient.adminPayment(requiredToken(), paymentId)

    fun adminCancelPayment(paymentId: Long, reason: String): PaymentReceipt =
        apiClient.adminCancelPayment(requiredToken(), paymentId, reason)

    fun adminCreateCollector(input: NewCollectorInput): CollectorDetail =
        apiClient.adminCreateCollector(requiredToken(), input)

    fun adminCollectorDetail(collectorId: Long): CollectorDetail =
        apiClient.adminCollectorDetail(requiredToken(), collectorId)

    fun adminUpdateCollector(collectorId: Long, input: UpdateCollectorInput): CollectorDetail =
        apiClient.adminUpdateCollector(requiredToken(), collectorId, input)

    fun adminPayCommission(collectorId: Long, commissionId: Long): CollectorCommissionItem =
        apiClient.adminPayCommission(requiredToken(), collectorId, commissionId)

    fun adminStoreMovement(input: CashMovementInput) =
        apiClient.adminStoreMovement(requiredToken(), input)

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
        apiClient.clearCache()
    }

    private fun sendPendingPayment(pendingPayment: PendingPayment): PaymentReceipt {
        return apiClient.registerCollectorPayment(
            token = requiredToken(),
            loanId = pendingPayment.loanId,
            amount = pendingPayment.amount,
            paymentDate = pendingPayment.paymentDate,
            paymentMethod = pendingPayment.paymentMethod,
            mobileUuid = pendingPayment.mobileUuid,
            allocationMode = pendingPayment.allocationMode,
            targetInstallmentId = pendingPayment.targetInstallmentId,
            capitalPrepaymentAmount = pendingPayment.capitalPrepaymentAmount,
        )
    }

    private fun requiredToken(): String {
        return sessionStore.token() ?: error("No hay sesion activa.")
    }
}
