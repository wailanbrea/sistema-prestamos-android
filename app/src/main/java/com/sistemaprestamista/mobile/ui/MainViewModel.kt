package com.sistemaprestamista.mobile.ui

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sistemaprestamista.mobile.data.PaymentRegistrationResult
import com.sistemaprestamista.mobile.data.PrestamistaRepository
import com.sistemaprestamista.mobile.data.model.AccountPayableInput
import com.sistemaprestamista.mobile.data.model.CreditorInput
import com.sistemaprestamista.mobile.data.model.PaymentHistoryFilters
import com.sistemaprestamista.mobile.data.model.RoutePoint
import com.sistemaprestamista.mobile.data.pending.PendingPayment
import com.sistemaprestamista.mobile.data.remote.ApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class MainViewModel(
    private val repository: PrestamistaRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        prepareSession()
    }

    fun login(email: String, password: String) {
        val normalizedEmail = email.trim().lowercase()
        val validationError = validateLogin(normalizedEmail, password)
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val user = withContext(Dispatchers.IO) { repository.login(normalizedEmail, password) }
                loadSessionProgressive(user)
            }.onFailure { throwable ->
                _uiState.value = AppUiState(
                    isLoading = false,
                    hasSavedSession = repository.hasSavedSession(),
                    errorMessage = throwable.userMessage(),
                )
            }
        }
    }

    fun unlockSavedSession() {
        restoreSession()
    }

    fun requestPasswordReset(email: String) {
        val normalizedEmail = email.trim().lowercase()
        if (!isValidEmail(normalizedEmail)) {
            _uiState.update { it.copy(errorMessage = "Escribe un correo valido para recuperar la contrasena.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            runCatching {
                withContext(Dispatchers.IO) {
                    repository.requestPasswordReset(normalizedEmail)
                }
            }.onSuccess { message ->
                _uiState.update { it.copy(isLoading = false, successMessage = message) }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isLoading = false, isPaymentSaving = false, errorMessage = throwable.userMessage()) }
            }
        }
    }

    fun resetPassword(email: String, token: String, password: String, passwordConfirmation: String) {
        val normalizedEmail = email.trim().lowercase()
        val validationError = validatePasswordReset(normalizedEmail, token, password, passwordConfirmation)
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            runCatching {
                withContext(Dispatchers.IO) {
                    repository.resetPassword(
                        email = normalizedEmail,
                        token = token,
                        password = password,
                        passwordConfirmation = passwordConfirmation,
                    )
                }
            }.onSuccess { message ->
                _uiState.update { it.copy(isLoading = false, successMessage = message) }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isLoading = false, isPaymentSaving = false, errorMessage = throwable.userMessage()) }
            }
        }
    }

    fun refreshDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                withContext(Dispatchers.IO) {
                    repository.syncPendingPayments()
                    val currentUser = uiState.value.user
                    coroutineScope {
                        val dashboard = async { loadDashboardIfAllowed(currentUser) }
                        val collectorWorkload = async { loadCollectorWorkloadIfAllowed(currentUser) }
                        val adminWorkload = async { loadAdminWorkloadIfAllowed(currentUser) }
                        val cashboxWorkload = async { loadCashboxWorkloadIfAllowed(currentUser) }
                        RefreshBundle(
                            dashboard.await(),
                            collectorWorkload.await(),
                            adminWorkload.await(),
                            cashboxWorkload.await(),
                        )
                    }
                }
            }.onSuccess { (dashboard, collectorWorkload, adminWorkload, cashboxWorkload) ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        dashboard = dashboard ?: it.dashboard,
                        collectorSummary = collectorWorkload?.summary ?: it.collectorSummary,
                        collectorClients = collectorWorkload?.clients ?: it.collectorClients,
                        collectorLoans = collectorWorkload?.loans ?: it.collectorLoans,
                        collectorInstallments = collectorWorkload?.installments ?: it.collectorInstallments,
                        paymentHistory = collectorWorkload?.payments ?: adminWorkload?.payments?.takeIf { p -> p.isNotEmpty() } ?: it.paymentHistory,
                        mapClients = collectorWorkload?.mapClients ?: it.mapClients,
                        collectorRoutes = collectorWorkload?.routes ?: it.collectorRoutes,
                        activeRouteSession = collectorWorkload?.activeRouteSession ?: it.activeRouteSession,
                        adminClients = adminWorkload?.clients ?: it.adminClients,
                        adminLoans = adminWorkload?.loans ?: it.adminLoans,
                        adminLoansHasMore = adminWorkload?.loansHasMore ?: it.adminLoansHasMore,
                        adminLoansLoadedPage = if (adminWorkload != null) 1 else it.adminLoansLoadedPage,
                        pendingApprovals = adminWorkload?.approvals ?: it.pendingApprovals,
                        reportSummary = adminWorkload?.reportSummary ?: it.reportSummary,
                        collectorPerformance = adminWorkload?.collectorPerformance ?: it.collectorPerformance,
                        expenses = cashboxWorkload?.expenses ?: it.expenses,
                        expenseCategories = cashboxWorkload?.categories ?: it.expenseCategories,
                        cashMovements = cashboxWorkload?.movements ?: it.cashMovements,
                        cashSummary = cashboxWorkload?.summary ?: it.cashSummary,
                        pendingPaymentCount = repository.pendingPaymentCount(),
                        pendingPayments = repository.pendingPayments(),
                    )
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isLoading = false, isPaymentSaving = false, errorMessage = throwable.userMessage()) }
            }
        }
    }

    /**
     * Mantiene el mismo mobile_uuid mientras un cobro idéntico (préstamo+monto+fecha) no
     * se confirme. Si el primer intento llegó al servidor pero la respuesta se perdió (o el
     * refresco posterior falló), el reintento del cobrador devuelve el pago ya registrado
     * (idempotencia del backend) en vez de duplicarlo o rechazarlo por monto excedente.
     */
    private val paymentUuidByAttempt = mutableMapOf<String, String>()

    fun registerPayment(loanId: Long, amountText: String, paymentMethod: String, allocationMode: String = "auto", targetInstallmentId: Long? = null, capitalPrepaymentAmount: Double? = null) {
        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _uiState.update { it.copy(errorMessage = "El monto debe ser mayor que cero.") }
            return
        }
        if (paymentMethod.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Selecciona un metodo de pago.") }
            return
        }
        if (allocationMode == "current_plus_capital" && (capitalPrepaymentAmount == null || capitalPrepaymentAmount <= 0)) {
            _uiState.update { it.copy(errorMessage = "Indica cuanto se abonara al capital.") }
            return
        }

        val paymentDate = LocalDate.now().toString()
        val attemptKey = "$loanId|$amount|$paymentDate|$allocationMode|${targetInstallmentId ?: 0}|${capitalPrepaymentAmount ?: 0}"
        val mobileUuid = paymentUuidByAttempt.getOrPut(attemptKey) { UUID.randomUUID().toString() }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isPaymentSaving = true, errorMessage = null, lastPaymentReceipt = null) }
            runCatching {
                withContext(Dispatchers.IO) {
                    repository.registerCollectorPayment(
                        loanId = loanId,
                        amount = amount,
                        paymentDate = paymentDate,
                        paymentMethod = paymentMethod,
                        allocationMode = allocationMode,
                        targetInstallmentId = targetInstallmentId,
                        capitalPrepaymentAmount = capitalPrepaymentAmount,
                        mobileUuid = mobileUuid,
                    )
                }
            }.onSuccess { result ->
                when (result) {
                    is PaymentRegistrationResult.Sent -> {
                        paymentUuidByAttempt.remove(attemptKey)
                        // El cobro ya está confirmado por el servidor: el recibo se muestra
                        // de inmediato. El refresco de la cartera va aparte para que un fallo
                        // de red posterior no haga creer al cobrador que el pago falló.
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isPaymentSaving = false,
                                pendingPaymentCount = repository.pendingPaymentCount(),
                                lastPaymentReceipt = result.receipt,
                                selectedPaymentDetail = result.receipt,
                            )
                        }
                        refreshAfterPayment()
                    }
                    is PaymentRegistrationResult.Queued -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isPaymentSaving = false,
                                pendingPaymentCount = result.pendingCount,
                                successMessage = "Cobro guardado sin conexion. Se sincronizara automaticamente.",
                            )
                        }
                    }
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isLoading = false, isPaymentSaving = false, errorMessage = throwable.userMessage()) }
            }
        }
    }

    /**
     * Cobro desde back-office (Administrador): mismo contrato idempotente que el del
     * cobrador, pero contra admin/payments y sin cola offline. Al confirmar, muestra
     * el recibo y refresca el detalle del préstamo en un paso aparte.
     */
    fun registerAdminPayment(loanId: Long, amountText: String, paymentMethod: String, allocationMode: String = "auto", targetInstallmentId: Long? = null, capitalPrepaymentAmount: Double? = null) {
        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _uiState.update { it.copy(errorMessage = "El monto debe ser mayor que cero.") }
            return
        }
        if (paymentMethod.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Selecciona un metodo de pago.") }
            return
        }
        if (allocationMode == "current_plus_capital" && (capitalPrepaymentAmount == null || capitalPrepaymentAmount <= 0)) {
            _uiState.update { it.copy(errorMessage = "Indica cuanto se abonara al capital.") }
            return
        }

        val paymentDate = LocalDate.now().toString()
        val attemptKey = "admin|$loanId|$amount|$paymentDate|$allocationMode|${targetInstallmentId ?: 0}|${capitalPrepaymentAmount ?: 0}"
        val mobileUuid = paymentUuidByAttempt.getOrPut(attemptKey) { UUID.randomUUID().toString() }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isPaymentSaving = true, errorMessage = null, lastPaymentReceipt = null) }
            runCatching {
                withContext(Dispatchers.IO) {
                    repository.registerAdminPayment(
                        loanId = loanId,
                        amount = amount,
                        paymentDate = paymentDate,
                        paymentMethod = paymentMethod,
                        allocationMode = allocationMode,
                        targetInstallmentId = targetInstallmentId,
                        capitalPrepaymentAmount = capitalPrepaymentAmount,
                        mobileUuid = mobileUuid,
                    )
                }
            }.onSuccess { receipt ->
                paymentUuidByAttempt.remove(attemptKey)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isPaymentSaving = false,
                        lastPaymentReceipt = receipt,
                        selectedPaymentDetail = receipt,
                    )
                }
                refreshAdminLoanAfterPayment(loanId)
            }.onFailure { throwable ->
                _uiState.update { it.copy(isLoading = false, isPaymentSaving = false, errorMessage = throwable.userMessage()) }
            }
        }
    }

    /**
     * Genera (o reusa) un documento legal del préstamo y actualiza el detalle en
     * pantalla. Usa la ruta admin si el usuario tiene cartera global; si no, la
     * del cobrador (su propia cartera).
     */
    fun generateLoanDocument(loanId: Long, documentType: String) {
        if (uiState.value.isDocumentGenerating) return

        val viaAdmin = uiState.value.canManagePortfolio

        viewModelScope.launch {
            _uiState.update { it.copy(isDocumentGenerating = true, errorMessage = null) }
            runCatching {
                withContext(Dispatchers.IO) {
                    repository.generateLoanDocument(loanId, documentType, viaAdmin)
                }
            }.onSuccess { document ->
                _uiState.update { current ->
                    val detail = current.selectedLoanDetail
                    val updatedDetail = if (detail?.summary?.id == loanId) {
                        val documents = detail.documents
                        detail.copy(
                            documents = if (documents.any { it.documentType == document.documentType }) {
                                documents.map { if (it.documentType == document.documentType) document else it }
                            } else {
                                documents + document
                            },
                        )
                    } else {
                        detail
                    }

                    current.copy(
                        isDocumentGenerating = false,
                        selectedLoanDetail = updatedDetail,
                        successMessage = "${document.label} generado.",
                    )
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isDocumentGenerating = false, errorMessage = throwable.userMessage()) }
            }
        }
    }

    /** Carga el contrato digital más reciente del préstamo (o null) para mostrar su estado. */
    fun loadLoanContract(loanId: Long) {
        if (!uiState.value.canManageContracts || uiState.value.isContractLoading) return

        viewModelScope.launch {
            // Limpia el contrato previo para no mostrar el de otro préstamo mientras carga.
            _uiState.update { it.copy(isContractLoading = true, selectedLoanContract = null) }
            runCatching {
                withContext(Dispatchers.IO) { repository.loanContract(loanId) }
            }.onSuccess { contract ->
                _uiState.update { it.copy(isContractLoading = false, selectedLoanContract = contract) }
            }.onFailure {
                _uiState.update { it.copy(isContractLoading = false) }
            }
        }
    }

    /** Genera el contrato digital del préstamo y actualiza el estado en pantalla. */
    fun generateContract(loanId: Long) {
        if (uiState.value.isContractLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isContractLoading = true, errorMessage = null) }
            runCatching {
                withContext(Dispatchers.IO) { repository.generateContract(loanId) }
            }.onSuccess { contract ->
                _uiState.update {
                    it.copy(
                        isContractLoading = false,
                        selectedLoanContract = contract,
                        successMessage = "Contrato ${contract.contractNumber} generado.",
                    )
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isContractLoading = false, errorMessage = throwable.userMessage()) }
            }
        }
    }

    /** Limpia el contrato cargado al salir del detalle del préstamo. */
    fun clearLoanContract() {
        _uiState.update { it.copy(selectedLoanContract = null) }
    }

    /** Carga el catálogo de reportes (con enlaces firmados a sus PDF). */
    fun loadReportCatalog() {
        if (uiState.value.isReportCatalogLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isReportCatalogLoading = true) }
            runCatching {
                withContext(Dispatchers.IO) { repository.adminReportCatalog() }
            }.onSuccess { catalog ->
                _uiState.update { it.copy(isReportCatalogLoading = false, reportCatalog = catalog) }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isReportCatalogLoading = false, errorMessage = throwable.userMessage()) }
            }
        }
    }

    /** Alta de cliente desde back-office: mismo contrato que la web (clients.create). */
    fun createAdminClient(input: com.sistemaprestamista.mobile.data.model.NewClientInput) {
        if (uiState.value.isClientSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(isClientSaving = true, errorMessage = null) }
            runCatching {
                withContext(Dispatchers.IO) { repository.adminCreateClient(input) }
            }.onSuccess { client ->
                _uiState.update {
                    it.copy(
                        isClientSaving = false,
                        adminClients = listOf(client) + it.adminClients,
                        lastCreatedClientId = client.id,
                        successMessage = "Cliente ${client.fullName} creado.",
                    )
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isClientSaving = false, errorMessage = throwable.userMessage()) }
            }
        }
    }

    fun loadAdminQuotes() {
        if (uiState.value.isQuotesLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isQuotesLoading = true, errorMessage = null) }
            runCatching {
                withContext(Dispatchers.IO) { repository.adminQuotes() }
            }.onSuccess { quotes ->
                _uiState.update { it.copy(isQuotesLoading = false, adminQuotes = quotes) }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isQuotesLoading = false, errorMessage = throwable.userMessage()) }
            }
        }
    }

    fun createAdminQuote(
        clientId: Long?,
        amount: Double,
        interestRate: Double,
        interestType: String,
        paymentFrequency: String,
        calculationMethod: String,
        termQuantity: Int,
    ) {
        if (uiState.value.isQuoteSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(isQuoteSaving = true, errorMessage = null) }
            runCatching {
                withContext(Dispatchers.IO) {
                    repository.adminCreateQuote(
                        clientId = clientId,
                        amount = amount,
                        interestRate = interestRate,
                        interestType = interestType,
                        paymentFrequency = paymentFrequency,
                        calculationMethod = calculationMethod,
                        termQuantity = termQuantity,
                    )
                }
            }.onSuccess { quote ->
                _uiState.update {
                    it.copy(
                        isQuoteSaving = false,
                        adminQuotes = listOf(quote) + it.adminQuotes,
                        selectedQuote = quote,
                        lastCreatedQuoteId = quote.id,
                        successMessage = "Cotización creada.",
                    )
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isQuoteSaving = false, errorMessage = throwable.userMessage()) }
            }
        }
    }

    /** Abre el detalle de una cotización (con cronograma completo desde el API). */
    fun loadAdminQuote(quoteId: Long) {
        val cached = uiState.value.adminQuotes.firstOrNull { it.id == quoteId }
        if (cached != null && cached.installments.isNotEmpty()) {
            _uiState.update { it.copy(selectedQuote = cached) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isDetailLoading = true, errorMessage = null, selectedQuote = cached) }
            runCatching {
                withContext(Dispatchers.IO) { repository.adminQuote(quoteId) }
            }.onSuccess { quote ->
                _uiState.update { it.copy(isDetailLoading = false, selectedQuote = quote) }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isDetailLoading = false, errorMessage = throwable.userMessage()) }
            }
        }
    }

    fun deleteAdminQuote(quoteId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isQuoteSaving = true, errorMessage = null) }
            runCatching {
                withContext(Dispatchers.IO) { repository.adminDeleteQuote(quoteId) }
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isQuoteSaving = false,
                        adminQuotes = it.adminQuotes.filterNot { quote -> quote.id == quoteId },
                        selectedQuote = it.selectedQuote?.takeIf { quote -> quote.id != quoteId },
                        successMessage = "Cotización eliminada.",
                    )
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isQuoteSaving = false, errorMessage = throwable.userMessage()) }
            }
        }
    }

    fun loadAdminCollectors() {
        if (uiState.value.adminCollectors.isNotEmpty()) return

        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { repository.adminCollectors() }
            }.onSuccess { collectors ->
                _uiState.update { it.copy(adminCollectors = collectors) }
            }
        }
    }

    fun createAdminLoan(input: com.sistemaprestamista.mobile.data.model.NewLoanInput) {
        if (uiState.value.isLoanSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoanSaving = true, errorMessage = null) }
            runCatching {
                withContext(Dispatchers.IO) { repository.adminCreateLoan(input) }
            }.onSuccess { loan ->
                _uiState.update {
                    it.copy(
                        isLoanSaving = false,
                        adminLoans = listOf(loan) + it.adminLoans,
                        lastCreatedLoanId = loan.id,
                        successMessage = "Préstamo ${loan.loanNumber} creado.",
                    )
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isLoanSaving = false, errorMessage = throwable.userMessage()) }
            }
        }
    }

    fun updateAdminLoan(loanId: Long, input: com.sistemaprestamista.mobile.data.model.UpdateLoanInput) {
        if (uiState.value.isLoanUpdating) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoanUpdating = true, errorMessage = null) }
            runCatching {
                withContext(Dispatchers.IO) { repository.adminUpdateLoan(loanId, input) }
            }.onSuccess { detail ->
                _uiState.update {
                    it.copy(
                        isLoanUpdating = false,
                        selectedLoanDetail = detail,
                        adminLoans = it.adminLoans.map { loan -> if (loan.id == loanId) detail.summary else loan },
                        successMessage = "Préstamo actualizado.",
                    )
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isLoanUpdating = false, errorMessage = throwable.userMessage()) }
            }
        }
    }

    fun createClientRegistrationLink(recipientName: String?, recipientPhone: String?) {
        if (uiState.value.isLinkGenerating) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLinkGenerating = true, errorMessage = null) }
            runCatching {
                withContext(Dispatchers.IO) { repository.adminCreateRegistrationLink(recipientName, recipientPhone) }
            }.onSuccess { link ->
                _uiState.update { it.copy(isLinkGenerating = false, lastGeneratedRegistrationLink = link) }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isLinkGenerating = false, errorMessage = throwable.userMessage()) }
            }
        }
    }

    fun clearRegistrationLink() {
        _uiState.update { it.copy(lastGeneratedRegistrationLink = null) }
    }

    /** Editar un cliente existente (back-office). */
    fun updateAdminClient(clientId: Long, input: com.sistemaprestamista.mobile.data.model.UpdateClientInput) {
        if (uiState.value.isClientSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(isClientSaving = true, errorMessage = null) }
            runCatching {
                withContext(Dispatchers.IO) { repository.adminUpdateClient(clientId, input) }
            }.onSuccess { detail ->
                _uiState.update {
                    val updatedSummary = detail.summary
                    it.copy(
                        isClientSaving = false,
                        selectedClientDetail = detail,
                        adminClients = it.adminClients.map { c -> if (c.id == clientId) updatedSummary else c },
                        successMessage = "Cliente actualizado.",
                    )
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isClientSaving = false, errorMessage = throwable.userMessage()) }
            }
        }
    }

    /** Eliminar un cliente (back-office). */
    fun deleteAdminClient(clientId: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isClientSaving = true, errorMessage = null) }
            runCatching {
                withContext(Dispatchers.IO) { repository.adminDeleteClient(clientId) }
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isClientSaving = false,
                        adminClients = it.adminClients.filterNot { c -> c.id == clientId },
                        selectedClientDetail = it.selectedClientDetail?.takeIf { d -> d.summary.id != clientId },
                        successMessage = "Cliente eliminado.",
                    )
                }
                onSuccess()
            }.onFailure { throwable ->
                _uiState.update { it.copy(isClientSaving = false, errorMessage = throwable.userMessage()) }
            }
        }
    }

    /** Eliminar un préstamo (back-office). */
    fun deleteAdminLoan(loanId: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoanSaving = true, errorMessage = null) }
            runCatching {
                withContext(Dispatchers.IO) { repository.adminDeleteLoan(loanId) }
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isLoanSaving = false,
                        adminLoans = it.adminLoans.filterNot { l -> l.id == loanId },
                        selectedLoanDetail = it.selectedLoanDetail?.takeIf { d -> d.summary.id != loanId },
                        successMessage = "Préstamo eliminado.",
                    )
                }
                onSuccess()
            }.onFailure { throwable ->
                _uiState.update { it.copy(isLoanSaving = false, errorMessage = throwable.userMessage()) }
            }
        }
    }

    /** Cancelar un pago (back-office). */
    fun cancelPayment(paymentId: Long, reason: String, onSuccess: () -> Unit) {
        if (reason.trim().length < 10) {
            _uiState.update { it.copy(errorMessage = "La razón debe tener al menos 10 caracteres.") }
            return
        }
        if (uiState.value.isPaymentCancelling) return

        viewModelScope.launch {
            _uiState.update { it.copy(isPaymentCancelling = true, errorMessage = null) }
            runCatching {
                withContext(Dispatchers.IO) { repository.adminCancelPayment(paymentId, reason.trim()) }
            }.onSuccess { receipt ->
                _uiState.update {
                    it.copy(
                        isPaymentCancelling = false,
                        selectedPaymentDetail = receipt,
                        successMessage = "Pago anulado.",
                    )
                }
                onSuccess()
            }.onFailure { throwable ->
                _uiState.update { it.copy(isPaymentCancelling = false, errorMessage = throwable.userMessage()) }
            }
        }
    }

    /** Cargar detalle de cobrador (back-office). */
    fun loadAdminCollectorDetail(collectorId: Long) {
        if (uiState.value.selectedCollectorDetail?.id == collectorId || uiState.value.isDetailLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isDetailLoading = true, errorMessage = null, selectedCollectorDetail = null) }
            runCatching {
                withContext(Dispatchers.IO) { repository.adminCollectorDetail(collectorId) }
            }.onSuccess { detail ->
                _uiState.update { it.copy(isDetailLoading = false, selectedCollectorDetail = detail) }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isDetailLoading = false, errorMessage = throwable.userMessage()) }
            }
        }
    }

    /** Crear cobrador (back-office). */
    fun createAdminCollector(input: com.sistemaprestamista.mobile.data.model.NewCollectorInput) {
        if (uiState.value.isCollectorSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCollectorSaving = true, errorMessage = null) }
            runCatching {
                withContext(Dispatchers.IO) { repository.adminCreateCollector(input) }
            }.onSuccess { detail ->
                _uiState.update {
                    it.copy(
                        isCollectorSaving = false,
                        selectedCollectorDetail = detail,
                        lastCreatedCollectorId = detail.id,
                        adminCollectors = it.adminCollectors + com.sistemaprestamista.mobile.data.model.CollectorOption(detail.id, detail.name),
                        successMessage = "Cobrador ${detail.name} creado.",
                    )
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isCollectorSaving = false, errorMessage = throwable.userMessage()) }
            }
        }
    }

    /** Editar cobrador (back-office). */
    fun updateAdminCollector(collectorId: Long, input: com.sistemaprestamista.mobile.data.model.UpdateCollectorInput) {
        if (uiState.value.isCollectorSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCollectorSaving = true, errorMessage = null) }
            runCatching {
                withContext(Dispatchers.IO) { repository.adminUpdateCollector(collectorId, input) }
            }.onSuccess { detail ->
                _uiState.update {
                    it.copy(
                        isCollectorSaving = false,
                        selectedCollectorDetail = detail,
                        adminCollectors = it.adminCollectors.map { o -> if (o.id == collectorId) com.sistemaprestamista.mobile.data.model.CollectorOption(detail.id, detail.name) else o },
                        successMessage = "Cobrador actualizado.",
                    )
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isCollectorSaving = false, errorMessage = throwable.userMessage()) }
            }
        }
    }

    /** Pagar comisión de cobrador (back-office). */
    fun payCollectorCommission(collectorId: Long, commissionId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCollectorSaving = true, errorMessage = null) }
            runCatching {
                withContext(Dispatchers.IO) {
                    repository.adminPayCommission(collectorId, commissionId)
                    repository.adminCollectorDetail(collectorId)
                }
            }.onSuccess { detail ->
                _uiState.update {
                    it.copy(
                        isCollectorSaving = false,
                        selectedCollectorDetail = detail,
                        successMessage = "Comisión pagada.",
                    )
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isCollectorSaving = false, errorMessage = throwable.userMessage()) }
            }
        }
    }

    /** Registrar movimiento manual de caja (back-office). */
    fun storeAdminCashMovement(input: com.sistemaprestamista.mobile.data.model.CashMovementInput) {
        if (uiState.value.isMovementSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(isMovementSaving = true, errorMessage = null) }
            runCatching {
                withContext(Dispatchers.IO) {
                    repository.adminStoreMovement(input)
                    Triple(repository.cashboxMovements(), repository.cashboxSummary(), repository.cashboxExpenses())
                }
            }.onSuccess { (movements, summary, expenses) ->
                _uiState.update {
                    it.copy(
                        isMovementSaving = false,
                        cashMovements = movements,
                        cashSummary = summary,
                        expenses = expenses,
                        successMessage = "Movimiento registrado.",
                    )
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isMovementSaving = false, errorMessage = throwable.userMessage()) }
            }
        }
    }

    /** Limpia los marcadores de "recién creado" usados para navegar tras guardar. */
    fun clearCreationMarkers() {
        _uiState.update {
            it.copy(
                lastCreatedClientId = null,
                lastCreatedQuoteId = null,
                lastCreatedLoanId = null,
                lastCreatedCollectorId = null,
                lastCreatedAccountPayableId = null,
            )
        }
    }

    /** Recarga el detalle del préstamo cobrado y su fila en la cartera; un fallo se ignora. */
    private fun refreshAdminLoanAfterPayment(loanId: Long) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { repository.adminLoan(loanId) }
            }.onSuccess { detail ->
                _uiState.update { current ->
                    current.copy(
                        selectedLoanDetail = detail,
                        adminLoans = current.adminLoans.map { if (it.id == loanId) detail.summary else it },
                    )
                }
            }
        }
    }

    /**
     * Refresco selectivo tras un pago: un pago solo afecta totales del cobrador, el
     * préstamo cobrado (saldo), sus cuotas y el historial. Recargamos únicamente eso en
     * paralelo y reutilizamos el resto de la cartera (clientes, mapa, rutas, sesión).
     * Si falla, se ignora: el pago ya está confirmado y la cartera se actualizará en el
     * próximo refresco manual o de arranque.
     */
    private fun refreshAfterPayment() {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    coroutineScope {
                        val summary = async { repository.collectorSummary() }
                        val loans = async { repository.collectorLoans() }
                        val installments = async { repository.collectorInstallments() }
                        val payments = async { repository.collectorPayments() }
                        PostPaymentRefresh(
                            summary = summary.await(),
                            loans = loans.await(),
                            installments = installments.await(),
                            payments = payments.await(),
                        )
                    }
                }
            }.onSuccess { refresh ->
                _uiState.update {
                    it.copy(
                        collectorSummary = refresh.summary,
                        collectorLoans = refresh.loans,
                        collectorInstallments = refresh.installments,
                        paymentHistory = refresh.payments,
                    )
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.logout()
            }
            _uiState.value = AppUiState(isLoading = false)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }

    /**
     * Genera (o reusa) el estado de cuenta del préstamo y deja su enlace de
     * WhatsApp en `pendingShareUrl` para que la UI abra el chat. Permite
     * compartir el documento sin tener que entrar al detalle del préstamo.
     */
    fun sendAccountStatement(loanId: Long) {
        if (uiState.value.isSharingDocument) return

        val viaAdmin = uiState.value.canManagePortfolio

        viewModelScope.launch {
            _uiState.update { it.copy(isSharingDocument = true, errorMessage = null) }
            runCatching {
                withContext(Dispatchers.IO) {
                    repository.generateLoanDocument(loanId, "account_statement", viaAdmin)
                }
            }.onSuccess { document ->
                val url = document.whatsappUrl
                if (url.isNullOrBlank()) {
                    _uiState.update {
                        it.copy(
                            isSharingDocument = false,
                            errorMessage = "No se pudo preparar el enlace de WhatsApp del estado de cuenta.",
                        )
                    }
                } else {
                    _uiState.update { it.copy(isSharingDocument = false, pendingShareUrl = url) }
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isSharingDocument = false, errorMessage = throwable.userMessage()) }
            }
        }
    }

    fun consumePendingShareUrl() {
        _uiState.update { it.copy(pendingShareUrl = null) }
    }

    fun loadClientDetail(clientId: Long) {
        val current = uiState.value.selectedClientDetail
        if (current?.summary?.id == clientId || uiState.value.isDetailLoading) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isDetailLoading = true,
                    errorMessage = null,
                    selectedClientDetail = null,
                    selectedLoanDetail = null,
                )
            }
            runCatching {
                withContext(Dispatchers.IO) {
                    repository.collectorClient(clientId)
                }
            }.onSuccess { detail ->
                _uiState.update {
                    it.copy(
                        isDetailLoading = false,
                        selectedClientDetail = detail,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isDetailLoading = false, errorMessage = throwable.userMessage()) }
            }
        }
    }

    fun loadLoanDetail(loanId: Long) {
        val current = uiState.value.selectedLoanDetail
        if (current?.summary?.id == loanId || uiState.value.isDetailLoading) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isDetailLoading = true,
                    errorMessage = null,
                    selectedLoanDetail = null,
                )
            }
            runCatching {
                withContext(Dispatchers.IO) {
                    repository.collectorLoan(loanId)
                }
            }.onSuccess { detail ->
                _uiState.update {
                    it.copy(
                        isDetailLoading = false,
                        selectedLoanDetail = detail,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isDetailLoading = false, errorMessage = throwable.userMessage()) }
            }
        }
    }

    fun loadPaymentDetail(paymentId: Long) {
        val current = uiState.value.selectedPaymentDetail
        if (current?.id == paymentId || uiState.value.isDetailLoading) {
            return
        }

        // El back-office (cartera global) consulta admin/payments; el cobrador, su
        // propia cartera. Usar el endpoint correcto evita el "no query results".
        val viaAdmin = uiState.value.canManagePortfolio

        viewModelScope.launch {
            _uiState.update { it.copy(isDetailLoading = true, errorMessage = null) }
            runCatching {
                withContext(Dispatchers.IO) {
                    if (viaAdmin) repository.adminPayment(paymentId) else repository.collectorPayment(paymentId)
                }
            }.onSuccess { payment ->
                _uiState.update {
                    it.copy(
                        isDetailLoading = false,
                        selectedPaymentDetail = payment,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isDetailLoading = false, errorMessage = throwable.userMessage()) }
            }
        }
    }

    fun loadInstallmentDetail(installmentId: Long) {
        val current = uiState.value.selectedInstallmentDetail
        if (current?.summary?.id == installmentId || uiState.value.isDetailLoading) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isDetailLoading = true,
                    errorMessage = null,
                    selectedInstallmentDetail = null,
                )
            }
            runCatching {
                withContext(Dispatchers.IO) {
                    repository.collectorInstallment(installmentId)
                }
            }.onSuccess { detail ->
                _uiState.update {
                    it.copy(
                        isDetailLoading = false,
                        selectedInstallmentDetail = detail,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isDetailLoading = false, errorMessage = throwable.userMessage()) }
            }
        }
    }

    fun loadPaymentHistory(filters: PaymentHistoryFilters = uiState.value.paymentHistoryFilters) {
        if (uiState.value.isPaymentHistoryLoading) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isPaymentHistoryLoading = true,
                    paymentHistoryFilters = filters,
                    errorMessage = null,
                )
            }
            runCatching {
                withContext(Dispatchers.IO) {
                    repository.collectorPayments(filters)
                }
            }.onSuccess { payments ->
                _uiState.update {
                    it.copy(
                        isPaymentHistoryLoading = false,
                        paymentHistory = payments,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isPaymentHistoryLoading = false, errorMessage = throwable.userMessage()) }
            }
        }
    }

    fun loadMapData() {
        if (uiState.value.isMapLoading) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isMapLoading = true, errorMessage = null, routeWarning = null) }
            runCatching {
                withContext(Dispatchers.IO) {
                    val clients = repository.collectorMapClients()
                    val routes = repository.collectorRoutes()
                    val activeSession = repository.activeRouteSession()
                    val selectedRouteId = resolveSelectedMapRouteId(
                        currentRouteId = uiState.value.selectedMapRouteId,
                        routes = routes,
                        activeSession = activeSession,
                    )
                    val routePoints = routePointsFor(clients, routes, selectedRouteId)
                    val cached = uiState.value
                    val realRoute = if (
                        cached.cachedPolylineRouteId == selectedRouteId &&
                        cached.realRoutePoints.isNotEmpty()
                    ) {
                        cached.realRoutePoints
                    } else {
                        runCatching { repository.drivingRoute(routePoints) }.getOrElse { emptyList() }
                    }
                    MapLoadResult(clients, routes, activeSession, selectedRouteId, realRoute)
                }
            }.onSuccess { result ->
                _uiState.update {
                    it.copy(
                        isMapLoading = false,
                        mapClients = result.clients,
                        collectorRoutes = result.routes,
                        selectedMapRouteId = result.selectedRouteId,
                        optimizedRouteClientIds = routeClientIdsFor(result.routes, result.selectedRouteId),
                        activeRouteSession = result.activeSession,
                        realRoutePoints = result.realRoute,
                        cachedPolylineRouteId = if (result.realRoute.isNotEmpty()) result.selectedRouteId else it.cachedPolylineRouteId,
                        routeWarning = if (routePointsFor(result.clients, result.routes, result.selectedRouteId).size > 1 && result.realRoute.isEmpty()) {
                            "Google no pudo calcular una ruta real. Revisa API Routes, coordenadas o cantidad de paradas."
                        } else {
                            null
                        },
                    )
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isMapLoading = false, errorMessage = throwable.userMessage()) }
            }
        }
    }

    fun startRouteTracking(routeId: Long, origin: RoutePoint? = null) {
        val resolvedRouteId = routeId.takeIf { it > 0L }
            ?: uiState.value.collectorRoutes.singleOrNull()?.id

        if (resolvedRouteId == null) {
            _uiState.update { it.copy(errorMessage = "Selecciona una ruta antes de iniciar seguimiento.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isRouteTrackingLoading = true, errorMessage = null, successMessage = null) }
            runCatching {
                withContext(Dispatchers.IO) {
                    val session = repository.startRouteSession(resolvedRouteId)
                    val current = uiState.value
                    val selectedRouteId = session.routeId ?: resolvedRouteId
                    val optimizedClientIds = optimizedClientIdsForRoute(
                        clients = current.mapClients,
                        routes = current.collectorRoutes,
                        routeId = selectedRouteId,
                        origin = origin,
                    )
                    val routePoints = routePointsFor(
                        clients = current.mapClients,
                        routes = current.collectorRoutes,
                        routeId = selectedRouteId,
                        orderedClientIds = optimizedClientIds,
                        origin = origin,
                    )
                    val realRoute = runCatching { repository.drivingRoute(routePoints) }.getOrElse { emptyList() }

                    RouteStartResult(session, selectedRouteId, optimizedClientIds, realRoute)
                }
            }.onSuccess { result ->
                _uiState.update {
                    it.copy(
                        isRouteTrackingLoading = false,
                        activeRouteSession = result.session,
                        selectedMapRouteId = result.selectedRouteId,
                        optimizedRouteClientIds = result.optimizedClientIds,
                        realRoutePoints = result.realRoute,
                        cachedPolylineRouteId = if (result.realRoute.isNotEmpty()) result.selectedRouteId else it.cachedPolylineRouteId,
                        successMessage = "Seguimiento de ruta iniciado.",
                    )
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isRouteTrackingLoading = false, errorMessage = throwable.userMessage()) }
            }
        }
    }

    fun finishRouteTracking() {
        val sessionId = uiState.value.activeRouteSession?.id
        if (sessionId == null) {
            _uiState.update { it.copy(errorMessage = "No hay una ruta activa para finalizar.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isRouteTrackingLoading = true, errorMessage = null, successMessage = null) }
            runCatching {
                withContext(Dispatchers.IO) {
                    repository.finishRouteSession(sessionId)
                }
            }.onSuccess { session ->
                _uiState.update {
                    it.copy(
                        isRouteTrackingLoading = false,
                        activeRouteSession = session.takeIf { completed -> completed.status == "active" },
                        successMessage = "Seguimiento de ruta finalizado.",
                    )
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isRouteTrackingLoading = false, errorMessage = throwable.userMessage()) }
            }
        }
    }

    fun selectMapRoute(routeId: Long) {
        if (uiState.value.selectedMapRouteId == routeId) {
            return
        }

        // If we already have a cached polyline for this route, switch without an API call.
        val currentState = uiState.value
        if (currentState.cachedPolylineRouteId == routeId && currentState.realRoutePoints.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    selectedMapRouteId = routeId,
                    optimizedRouteClientIds = routeClientIdsFor(it.collectorRoutes, routeId),
                    routeWarning = null,
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedMapRouteId = routeId,
                    optimizedRouteClientIds = routeClientIdsFor(it.collectorRoutes, routeId),
                    isMapLoading = true,
                    realRoutePoints = emptyList(),
                    routeWarning = null,
                )
            }
            runCatching {
                withContext(Dispatchers.IO) {
                    val current = uiState.value
                    val orderedClientIds = routeClientIdsFor(current.collectorRoutes, routeId)
                    val points = routePointsFor(
                        clients = current.mapClients,
                        routes = current.collectorRoutes,
                        routeId = routeId,
                        orderedClientIds = orderedClientIds,
                    )
                    points to repository.drivingRoute(points)
                }
            }.onSuccess { (points, realRoute) ->
                _uiState.update {
                    it.copy(
                        isMapLoading = false,
                        realRoutePoints = realRoute,
                        cachedPolylineRouteId = if (realRoute.isNotEmpty()) routeId else it.cachedPolylineRouteId,
                        routeWarning = if (points.size > 1 && realRoute.isEmpty()) {
                            "Google no pudo calcular una ruta real. Revisa API Routes, coordenadas o cantidad de paradas."
                        } else {
                            null
                        },
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isMapLoading = false,
                        realRoutePoints = emptyList(),
                        routeWarning = "Google no pudo calcular una ruta real. Revisa API Routes, coordenadas o cantidad de paradas.",
                    )
                }
            }
        }
    }

    fun loadPendingPayments() {
        _uiState.update {
            it.copy(
                pendingPayments = repository.pendingPayments(),
                pendingPaymentCount = repository.pendingPaymentCount(),
            )
        }
    }

    fun retryPendingPayment(mobileUuid: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPendingSyncLoading = true, errorMessage = null, successMessage = null) }
            runCatching {
                withContext(Dispatchers.IO) {
                    repository.retryPendingPayment(mobileUuid)
                }
            }.onSuccess { result ->
                _uiState.update {
                    it.copy(
                        isPendingSyncLoading = false,
                        pendingPayments = repository.pendingPayments(),
                        pendingPaymentCount = result.remaining,
                        successMessage = syncResultMessage(result),
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isPendingSyncLoading = false,
                        pendingPayments = repository.pendingPayments(),
                        pendingPaymentCount = repository.pendingPaymentCount(),
                        errorMessage = throwable.userMessage(),
                    )
                }
            }
        }
    }

    fun syncPendingPaymentsNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isPendingSyncLoading = true, errorMessage = null, successMessage = null) }
            runCatching {
                withContext(Dispatchers.IO) {
                    repository.syncPendingPayments()
                }
            }.onSuccess { result ->
                _uiState.update {
                    it.copy(
                        isPendingSyncLoading = false,
                        pendingPayments = repository.pendingPayments(),
                        pendingPaymentCount = result.remaining,
                        successMessage = syncResultMessage(result),
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isPendingSyncLoading = false,
                        pendingPayments = repository.pendingPayments(),
                        pendingPaymentCount = repository.pendingPaymentCount(),
                        errorMessage = throwable.userMessage(),
                    )
                }
            }
        }
    }

    fun discardPendingPayment(mobileUuid: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.discardPendingPayment(mobileUuid)
            }
            _uiState.update {
                it.copy(
                    pendingPayments = repository.pendingPayments(),
                    pendingPaymentCount = repository.pendingPaymentCount(),
                    successMessage = "Cobro pendiente descartado.",
                )
            }
        }
    }

    private fun prepareSession() {
        // El estado inicial (AppUiState) ya muestra el splash con isLoading=true.
        // Leer sesion guardada (dispara Keystore) y los cobros pendientes (lee disco)
        // se hace en IO para no bloquear el hilo principal en el arranque; al terminar
        // pasamos a isLoading=false con el estado real (sin parpadeo de pantalla).
        viewModelScope.launch {
            val snapshot = withContext(Dispatchers.IO) {
                SessionSnapshot(
                    hasSavedSession = repository.hasSavedSession(),
                    pendingPaymentCount = repository.pendingPaymentCount(),
                    pendingPayments = repository.pendingPayments(),
                )
            }
            _uiState.value = AppUiState(
                isLoading = false,
                hasSavedSession = snapshot.hasSavedSession,
                pendingPaymentCount = snapshot.pendingPaymentCount,
                pendingPayments = snapshot.pendingPayments,
            )
            withContext(Dispatchers.IO) { repository.enqueuePendingPaymentSync() }
        }
    }

    private data class SessionSnapshot(
        val hasSavedSession: Boolean,
        val pendingPaymentCount: Int,
        val pendingPayments: List<PendingPayment>,
    )

    private fun restoreSession() {
        viewModelScope.launch {
            if (repository.savedToken() == null) {
                _uiState.value = AppUiState(isLoading = false)
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // Apertura instantánea: pinta la cartera del cobrador desde la caché local (sin red)
            // mientras se refresca en segundo plano. No bloquea ni reemplaza el refresco real.
            withContext(Dispatchers.IO) { loadCachedAuthBundleOrNull() }?.let { cached ->
                _uiState.value = authenticatedState(cached)
            }

            runCatching {
                val user = withContext(Dispatchers.IO) { repository.me() }
                loadSessionProgressive(user)
            }.onFailure { throwable ->
                val isAuthError = throwable is ApiException && throwable.statusCode == 401
                if (isAuthError) {
                    withContext(Dispatchers.IO) { repository.logout() }
                    _uiState.value = AppUiState(
                        isLoading = false,
                        errorMessage = "La sesion guardada expiro. Entra con correo y contrasena.",
                    )
                } else {
                    // Error de red: conserva la sesión y lo ya pintado desde caché.
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = if (it.user == null) {
                                "Sin conexión. Revisa tu internet e intenta de nuevo."
                            } else {
                                null
                            },
                        )
                    }
                }
            }
        }
    }

    // Construye un AuthBundle solo desde la caché local (sin red) para el pintado instantáneo.
    // Devuelve null si no hay caché suficiente (p. ej. primer arranque). Solo cubre al cobrador,
    // que es quien más necesita ver su cartera al instante y sin conexión en campo.
    private fun loadCachedAuthBundleOrNull(): AuthBundle? {
        return runCatching {
            val user = repository.me(cacheOnly = true)
            if (user.isCollector != true) return null
            AuthBundle(
                user = user,
                dashboard = null,
                collectorWorkload = CollectorWorkload(
                    summary = repository.collectorSummary(cacheOnly = true),
                    clients = repository.collectorClients(cacheOnly = true),
                    loans = repository.collectorLoans(cacheOnly = true),
                    installments = repository.collectorInstallments(cacheOnly = true),
                    payments = repository.collectorPayments(cacheOnly = true),
                    mapClients = repository.collectorMapClients(cacheOnly = true),
                    routes = repository.collectorRoutes(cacheOnly = true),
                    activeRouteSession = repository.activeRouteSession(cacheOnly = true),
                ),
                adminWorkload = null,
                cashboxWorkload = null,
            )
        }.getOrNull()
    }

    private fun authenticatedState(bundle: AuthBundle): AppUiState {
        val collectorWorkload = bundle.collectorWorkload
        val adminWorkload = bundle.adminWorkload

        return AppUiState(
            isLoading = false,
            hasSavedSession = true,
            pendingPaymentCount = repository.pendingPaymentCount(),
            pendingPayments = repository.pendingPayments(),
            user = bundle.user,
            dashboard = bundle.dashboard,
            collectorSummary = collectorWorkload?.summary,
            collectorClients = collectorWorkload?.clients.orEmpty(),
            collectorLoans = collectorWorkload?.loans.orEmpty(),
            collectorInstallments = collectorWorkload?.installments.orEmpty(),
            paymentHistory = collectorWorkload?.payments.orEmpty().ifEmpty { adminWorkload?.payments.orEmpty() },
            mapClients = collectorWorkload?.mapClients.orEmpty(),
            collectorRoutes = collectorWorkload?.routes.orEmpty(),
            activeRouteSession = collectorWorkload?.activeRouteSession,
            adminClients = adminWorkload?.clients.orEmpty(),
            adminLoans = adminWorkload?.loans.orEmpty(),
            adminLoansHasMore = adminWorkload?.loansHasMore ?: false,
            adminLoansLoadedPage = 1,
            pendingApprovals = adminWorkload?.approvals.orEmpty(),
            reportSummary = adminWorkload?.reportSummary,
            collectorPerformance = adminWorkload?.collectorPerformance.orEmpty(),
            expenses = bundle.cashboxWorkload?.expenses.orEmpty(),
            expenseCategories = bundle.cashboxWorkload?.categories.orEmpty(),
            cashMovements = bundle.cashboxWorkload?.movements.orEmpty(),
            cashSummary = bundle.cashboxWorkload?.summary,
        )
    }

    fun createExpense(categoryId: Long?, description: String, amountText: String, paymentMethod: String) {
        val amount = amountText.toDoubleOrNull()
        if (description.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Escribe una descripción del gasto.") }
            return
        }
        if (amount == null || amount <= 0) {
            _uiState.update { it.copy(errorMessage = "El monto debe ser mayor que cero.") }
            return
        }
        if (uiState.value.isExpenseSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(isExpenseSaving = true, errorMessage = null, successMessage = null) }
            runCatching {
                withContext(Dispatchers.IO) {
                    repository.cashboxCreateExpense(categoryId, description.trim(), amount, LocalDate.now().toString(), paymentMethod)
                    Triple(repository.cashboxExpenses(), repository.cashboxMovements(), repository.cashboxSummary())
                }
            }.onSuccess { (expenses, movements, summary) ->
                _uiState.update {
                    it.copy(
                        isExpenseSaving = false,
                        expenses = expenses,
                        cashMovements = movements,
                        cashSummary = summary,
                        successMessage = "Gasto registrado.",
                    )
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isExpenseSaving = false, errorMessage = throwable.userMessage()) }
            }
        }
    }

    fun loadAccountsPayable() {
        val state = uiState.value
        if (!state.canManageAccountsPayable || state.isAccountsPayableLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isAccountsPayableLoading = true, errorMessage = null) }
            runCatching {
                withContext(Dispatchers.IO) {
                    coroutineScope {
                        val accounts = async { repository.accountsPayable() }
                        val creditors = async { repository.creditors() }
                        accounts.await() to creditors.await()
                    }
                }
            }.onSuccess { (accounts, creditors) ->
                _uiState.update {
                    it.copy(
                        isAccountsPayableLoading = false,
                        accountsPayable = accounts,
                        creditors = creditors,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isAccountsPayableLoading = false,
                        errorMessage = throwable.userMessage(),
                    )
                }
            }
        }
    }

    fun loadAccountPayableDetail(accountId: Long) {
        val state = uiState.value
        if (!state.canManageAccountsPayable || state.isAccountsPayableLoading) return
        if (state.selectedAccountPayable?.summary?.id == accountId) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isAccountsPayableLoading = true,
                    selectedAccountPayable = null,
                    errorMessage = null,
                )
            }
            runCatching {
                withContext(Dispatchers.IO) { repository.accountPayable(accountId) }
            }.onSuccess { detail ->
                _uiState.update {
                    it.copy(
                        isAccountsPayableLoading = false,
                        selectedAccountPayable = detail,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isAccountsPayableLoading = false,
                        errorMessage = throwable.userMessage(),
                    )
                }
            }
        }
    }

    fun createAccountPayable(input: AccountPayableInput) {
        if (!uiState.value.canManageAccountsPayable || uiState.value.isAccountPayableSaving) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isAccountPayableSaving = true,
                    errorMessage = null,
                    successMessage = null,
                    lastCreatedAccountPayableId = null,
                )
            }
            runCatching {
                withContext(Dispatchers.IO) {
                    val detail = repository.createAccountPayable(input)
                    detail to repository.accountsPayable()
                }
            }.onSuccess { (detail, accounts) ->
                _uiState.update {
                    it.copy(
                        isAccountPayableSaving = false,
                        accountsPayable = accounts,
                        selectedAccountPayable = detail,
                        lastCreatedAccountPayableId = detail.summary.id,
                        successMessage = "Cuenta por pagar creada.",
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isAccountPayableSaving = false,
                        errorMessage = throwable.userMessage(),
                    )
                }
            }
        }
    }

    fun updateAccountPayable(accountId: Long, input: AccountPayableInput) {
        if (!uiState.value.canManageAccountsPayable || uiState.value.isAccountPayableSaving) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isAccountPayableSaving = true,
                    errorMessage = null,
                    successMessage = null,
                )
            }
            runCatching {
                withContext(Dispatchers.IO) {
                    val detail = repository.updateAccountPayable(accountId, input)
                    detail to repository.accountsPayable()
                }
            }.onSuccess { (detail, accounts) ->
                _uiState.update {
                    it.copy(
                        isAccountPayableSaving = false,
                        accountsPayable = accounts,
                        selectedAccountPayable = detail,
                        successMessage = "Cuenta por pagar actualizada.",
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isAccountPayableSaving = false,
                        errorMessage = throwable.userMessage(),
                    )
                }
            }
        }
    }

    fun deleteAccountPayable(accountId: Long, onSuccess: () -> Unit) {
        if (!uiState.value.canManageAccountsPayable || uiState.value.isAccountPayableSaving) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isAccountPayableSaving = true,
                    errorMessage = null,
                    successMessage = null,
                )
            }
            runCatching {
                withContext(Dispatchers.IO) {
                    repository.deleteAccountPayable(accountId)
                    repository.accountsPayable()
                }
            }.onSuccess { accounts ->
                _uiState.update {
                    it.copy(
                        isAccountPayableSaving = false,
                        accountsPayable = accounts,
                        selectedAccountPayable = null,
                        successMessage = "Cuenta por pagar eliminada.",
                    )
                }
                onSuccess()
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isAccountPayableSaving = false,
                        errorMessage = throwable.userMessage(),
                    )
                }
            }
        }
    }

    fun registerAccountPayablePayment(
        accountId: Long,
        amountText: String,
        paymentMethod: String,
        notes: String?,
    ) {
        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _uiState.update { it.copy(errorMessage = "El monto debe ser mayor que cero.") }
            return
        }
        if (paymentMethod.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Selecciona un metodo de pago.") }
            return
        }
        if (!uiState.value.canManageAccountsPayable || uiState.value.isAccountPayableSaving) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isAccountPayableSaving = true,
                    errorMessage = null,
                    successMessage = null,
                )
            }
            runCatching {
                withContext(Dispatchers.IO) {
                    repository.registerAccountPayablePayment(
                        accountId = accountId,
                        amount = amount,
                        paymentDate = LocalDate.now().toString(),
                        paymentMethod = paymentMethod,
                        notes = notes,
                    )
                    coroutineScope {
                        val detail = async { repository.accountPayable(accountId) }
                        val accounts = async { repository.accountsPayable() }
                        detail.await() to accounts.await()
                    }
                }
            }.onSuccess { (detail, accounts) ->
                _uiState.update {
                    it.copy(
                        isAccountPayableSaving = false,
                        selectedAccountPayable = detail,
                        accountsPayable = accounts,
                        successMessage = "Pago registrado.",
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isAccountPayableSaving = false,
                        errorMessage = throwable.userMessage(),
                    )
                }
            }
        }
    }

    fun createCreditor(input: CreditorInput) {
        if (!uiState.value.canManageAccountsPayable || uiState.value.isAccountPayableSaving) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isAccountPayableSaving = true,
                    errorMessage = null,
                    successMessage = null,
                )
            }
            runCatching {
                withContext(Dispatchers.IO) { repository.createCreditor(input) }
            }.onSuccess { creditor ->
                _uiState.update {
                    it.copy(
                        isAccountPayableSaving = false,
                        creditors = (listOf(creditor) + it.creditors).distinctBy { item -> item.id },
                        successMessage = "Acreedor creado.",
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isAccountPayableSaving = false,
                        errorMessage = throwable.userMessage(),
                    )
                }
            }
        }
    }

    private suspend fun loadCashboxWorkloadIfAllowed(
        user: com.sistemaprestamista.mobile.data.model.UserProfile?,
    ): CashboxWorkload? = coroutineScope {
        val permissions = user?.permissions.orEmpty()
        val managePortfolio = permissions.contains("collectors.manage") && user?.isCollector != true
        val isCollector = user?.isCollector == true
        val canExpenses = permissions.contains("expenses.manage") && !managePortfolio && !isCollector
        val canCash = permissions.contains("cash.view") && !managePortfolio && !isCollector

        if (!canExpenses && !canCash) {
            return@coroutineScope null
        }

        // Peticiones independientes lanzadas en paralelo.
        val expenses = async(Dispatchers.IO) { if (canExpenses) repository.cashboxExpenses() else emptyList() }
        val categories = async(Dispatchers.IO) { if (canExpenses) runCatching { repository.cashboxCategories() }.getOrDefault(emptyList()) else emptyList() }
        val movements = async(Dispatchers.IO) { if (canCash) repository.cashboxMovements() else emptyList() }
        val summary = async(Dispatchers.IO) { if (canCash) runCatching { repository.cashboxSummary() }.getOrNull() else null }

        CashboxWorkload(
            expenses = expenses.await(),
            categories = categories.await(),
            movements = movements.await(),
            summary = summary.await(),
        )
    }

    // --- Back-office / administrador ---

    fun approveLoan(loanId: Long) {
        resolveApproval(loanId) { repository.adminApproveLoan(loanId); "Préstamo aprobado." }
    }

    fun rejectLoan(loanId: Long, reason: String?) {
        resolveApproval(loanId) { repository.adminRejectLoan(loanId, reason); "Préstamo rechazado." }
    }

    private fun resolveApproval(loanId: Long, action: () -> String) {
        if (uiState.value.isApprovalActionLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isApprovalActionLoading = true, errorMessage = null, successMessage = null) }
            runCatching {
                withContext(Dispatchers.IO) {
                    val message = action()
                    val approvals = repository.adminApprovals()
                    val dashboard = repository.dashboard()
                    Triple(message, approvals, dashboard)
                }
            }.onSuccess { (message, approvals, dashboard) ->
                _uiState.update {
                    it.copy(
                        isApprovalActionLoading = false,
                        pendingApprovals = approvals,
                        dashboard = dashboard,
                        successMessage = message,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isApprovalActionLoading = false, errorMessage = throwable.userMessage()) }
            }
        }
    }

    fun loadAdminClientDetail(clientId: Long) {
        if (uiState.value.selectedClientDetail?.summary?.id == clientId || uiState.value.isDetailLoading) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isDetailLoading = true, errorMessage = null, selectedClientDetail = null) }
            runCatching {
                withContext(Dispatchers.IO) { repository.adminClient(clientId) }
            }.onSuccess { detail ->
                _uiState.update { it.copy(isDetailLoading = false, selectedClientDetail = detail) }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isDetailLoading = false, errorMessage = throwable.userMessage()) }
            }
        }
    }

    fun loadAdminLoanDetail(loanId: Long) {
        if (uiState.value.selectedLoanDetail?.summary?.id == loanId || uiState.value.isDetailLoading) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isDetailLoading = true, errorMessage = null, selectedLoanDetail = null) }
            runCatching {
                withContext(Dispatchers.IO) { repository.adminLoan(loanId) }
            }.onSuccess { detail ->
                _uiState.update { it.copy(isDetailLoading = false, selectedLoanDetail = detail) }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isDetailLoading = false, errorMessage = throwable.userMessage()) }
            }
        }
    }

    /** Trae la siguiente página de la cartera de préstamos (admin) y la agrega a la lista. */
    fun loadMoreAdminLoans() {
        val state = uiState.value
        if (state.isLoadingMoreAdminLoans || !state.adminLoansHasMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoreAdminLoans = true, errorMessage = null) }
            runCatching {
                withContext(Dispatchers.IO) { repository.adminLoansPage(state.adminLoansLoadedPage + 1) }
            }.onSuccess { page ->
                _uiState.update { current ->
                    val existingIds = current.adminLoans.mapTo(HashSet()) { it.id }
                    val merged = current.adminLoans + page.items.filter { it.id !in existingIds }
                    current.copy(
                        isLoadingMoreAdminLoans = false,
                        adminLoans = merged,
                        adminLoansLoadedPage = page.currentPage,
                        adminLoansHasMore = page.hasMore,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isLoadingMoreAdminLoans = false, errorMessage = throwable.userMessage()) }
            }
        }
    }

    private suspend fun loadAdminWorkloadIfAllowed(
        user: com.sistemaprestamista.mobile.data.model.UserProfile?,
    ): AdminWorkload? = coroutineScope {
        val permissions = user?.permissions.orEmpty()
        val managePortfolio = permissions.contains("collectors.manage") && user?.isCollector != true
        val canApprove = permissions.contains("loans.approve")
        val canViewReports = permissions.contains("reports.view")

        if (!managePortfolio && !canApprove && !canViewReports) {
            return@coroutineScope null
        }

        // Peticiones independientes lanzadas en paralelo.
        val clients = async(Dispatchers.IO) { if (managePortfolio) repository.adminClients() else emptyList() }
        // Cartera de préstamos paginada: solo la primera página al inicio (arranque rápido);
        // el resto se trae bajo demanda con "Cargar más" (loadMoreAdminLoans).
        val loansPage = async(Dispatchers.IO) { if (managePortfolio) repository.adminLoansPage(1) else null }
        val approvals = async(Dispatchers.IO) { if (canApprove) repository.adminApprovals() else emptyList() }
        val reportSummary = async(Dispatchers.IO) { if (canViewReports) runCatching { repository.adminReportSummary() }.getOrNull() else null }
        val collectorPerformance = async(Dispatchers.IO) { if (canViewReports) runCatching { repository.adminReportCollectors() }.getOrNull().orEmpty() else emptyList() }
        val payments = async(Dispatchers.IO) { if (managePortfolio) runCatching { repository.adminPayments() }.getOrDefault(emptyList()) else emptyList() }

        val page = loansPage.await()
        AdminWorkload(
            clients = clients.await(),
            loans = page?.items.orEmpty(),
            loansHasMore = page?.hasMore ?: false,
            approvals = approvals.await(),
            reportSummary = reportSummary.await(),
            collectorPerformance = collectorPerformance.await(),
            payments = payments.await(),
        )
    }

    private fun validateLogin(email: String, password: String): String? {
        return when {
            !isValidEmail(email) -> "Escribe un correo valido."
            password.isBlank() -> "Escribe tu contrasena."
            password.length < 8 -> "La contrasena debe tener al menos 8 caracteres."
            else -> null
        }
    }

    private fun validatePasswordReset(
        email: String,
        token: String,
        password: String,
        passwordConfirmation: String,
    ): String? {
        return when {
            !isValidEmail(email) -> "Escribe un correo valido."
            token.isBlank() -> "Pega el token de recuperacion."
            password.length < 10 -> "La nueva contrasena debe tener al menos 10 caracteres."
            password != passwordConfirmation -> "Las contrasenas no coinciden."
            else -> null
        }
    }

    private fun syncResultMessage(result: com.sistemaprestamista.mobile.data.PendingPaymentSyncResult): String {
        return when {
            result.sent > 0 && result.failed == 0 && result.remaining == 0 -> "Cobros pendientes sincronizados."
            result.sent > 0 && result.failed > 0 -> "Se sincronizaron ${result.sent}; ${result.failed} requieren revision."
            result.failed > 0 -> "${result.failed} cobros requieren revision."
            result.requiresRetry -> "No hay conexion estable. Se reintentara automaticamente."
            result.remaining > 0 -> "Quedan ${result.remaining} cobros pendientes."
            else -> "No hay cobros pendientes."
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private suspend fun loadCollectorWorkloadIfAllowed(
        user: com.sistemaprestamista.mobile.data.model.UserProfile?,
    ): CollectorWorkload? = coroutineScope {
        // Solo carga la cartera de campo si el usuario es un cobrador real (flag del backend),
        // no por tener el permiso payments.create (que el Administrador también tiene).
        if (user?.isCollector != true) {
            return@coroutineScope null
        }

        // Las 8 peticiones son independientes: se lanzan en paralelo para acelerar el arranque.
        val summary = async(Dispatchers.IO) { repository.collectorSummary() }
        val clients = async(Dispatchers.IO) { repository.collectorClients() }
        val loans = async(Dispatchers.IO) { repository.collectorLoans() }
        val installments = async(Dispatchers.IO) { repository.collectorInstallments() }
        val payments = async(Dispatchers.IO) { repository.collectorPayments() }
        val mapClients = async(Dispatchers.IO) { repository.collectorMapClients() }
        val routes = async(Dispatchers.IO) { repository.collectorRoutes() }
        val activeRouteSession = async(Dispatchers.IO) { repository.activeRouteSession() }

        CollectorWorkload(
            summary = summary.await(),
            clients = clients.await(),
            loans = loans.await(),
            installments = installments.await(),
            payments = payments.await(),
            mapClients = mapClients.await(),
            routes = routes.await(),
            activeRouteSession = activeRouteSession.await(),
        )
    }

    // Carga progresiva: pinta el dashboard apenas responde su llamada (rápido) y deja
    // las cargas pesadas (clientes, préstamos, pagos, caja) corriendo en segundo plano,
    // marcando isWorkloadLoading para que las listas muestren un indicador mientras tanto.
    private suspend fun loadSessionProgressive(
        user: com.sistemaprestamista.mobile.data.model.UserProfile,
    ) {
        val dashboard = withContext(Dispatchers.IO) { loadDashboardIfAllowed(user) }
        _uiState.value = authenticatedState(
            AuthBundle(
                user = user,
                dashboard = dashboard,
                collectorWorkload = null,
                adminWorkload = null,
                cashboxWorkload = null,
            ),
        ).copy(isWorkloadLoading = true)

        // Fase intermedia: la lista de clientes es lo más consultado, así que la
        // emitimos en cuanto responde su llamada (rápida) sin esperar a préstamos,
        // reportes ni pagos del bundle completo.
        val permissions = user.permissions
        val managePortfolio = permissions.contains("collectors.manage") && !user.isCollector
        if (managePortfolio) {
            val clients = withContext(Dispatchers.IO) {
                runCatching { repository.adminClients() }.getOrDefault(emptyList())
            }
            if (clients.isNotEmpty()) _uiState.update { it.copy(adminClients = clients) }
        } else if (user.isCollector) {
            val clients = withContext(Dispatchers.IO) {
                runCatching { repository.collectorClients() }.getOrDefault(emptyList())
            }
            if (clients.isNotEmpty()) _uiState.update { it.copy(collectorClients = clients) }
        }

        val bundle = withContext(Dispatchers.IO) {
            coroutineScope {
                val collectorWorkload = async { loadCollectorWorkloadIfAllowed(user) }
                val adminWorkload = async { loadAdminWorkloadIfAllowed(user) }
                val cashboxWorkload = async { loadCashboxWorkloadIfAllowed(user) }
                AuthBundle(
                    user = user,
                    dashboard = dashboard,
                    collectorWorkload = collectorWorkload.await(),
                    adminWorkload = adminWorkload.await(),
                    cashboxWorkload = cashboxWorkload.await(),
                )
            }
        }
        _uiState.value = authenticatedState(bundle)
    }

    // Carga las cuatro áreas (dashboard, cobrador, admin, caja) en paralelo. Solo la del
    // rol activo hace trabajo real; las demás devuelven null al instante.
    private suspend fun loadAuthBundle(
        user: com.sistemaprestamista.mobile.data.model.UserProfile,
    ): AuthBundle = coroutineScope {
        val dashboard = async { loadDashboardIfAllowed(user) }
        val collectorWorkload = async { loadCollectorWorkloadIfAllowed(user) }
        val adminWorkload = async { loadAdminWorkloadIfAllowed(user) }
        val cashboxWorkload = async { loadCashboxWorkloadIfAllowed(user) }

        AuthBundle(
            user = user,
            dashboard = dashboard.await(),
            collectorWorkload = collectorWorkload.await(),
            adminWorkload = adminWorkload.await(),
            cashboxWorkload = cashboxWorkload.await(),
        )
    }

    private fun loadDashboardIfAllowed(
        user: com.sistemaprestamista.mobile.data.model.UserProfile?,
    ): com.sistemaprestamista.mobile.data.model.DashboardSummary? {
        val permissions = user?.permissions.orEmpty()

        if (user?.isCollector == true || !permissions.contains("dashboard.view")) {
            return null
        }

        return repository.dashboard()
    }

    private fun routePointsFor(
        clients: List<com.sistemaprestamista.mobile.data.model.MapClient>,
        routes: List<com.sistemaprestamista.mobile.data.model.CollectorRoute>,
        routeId: Long,
        orderedClientIds: List<Long> = routeClientIdsFor(routes, routeId),
        origin: RoutePoint? = null,
    ): List<RoutePoint> {
        val visibleClients = if (routeId == 0L) {
            clients
        } else {
            val clientsById = clients.associateBy { it.summary.id }
            orderedClientIds.mapNotNull { clientsById[it] }
        }

        val stopPoints = visibleClients.mapNotNull { client ->
            val latitude = client.summary.latitude ?: return@mapNotNull null
            val longitude = client.summary.longitude ?: return@mapNotNull null
            RoutePoint(latitude, longitude)
        }

        return if (origin != null && stopPoints.isNotEmpty()) {
            listOf(origin) + stopPoints
        } else {
            stopPoints
        }
    }

    private fun routeClientIdsFor(
        routes: List<com.sistemaprestamista.mobile.data.model.CollectorRoute>,
        routeId: Long,
    ): List<Long> {
        if (routeId == 0L) {
            return emptyList()
        }

        return routes
            .firstOrNull { it.id == routeId }
            ?.clients
            ?.sortedBy { it.orderNumber }
            ?.map { it.summary.id }
            .orEmpty()
    }

    private fun optimizedClientIdsForRoute(
        clients: List<com.sistemaprestamista.mobile.data.model.MapClient>,
        routes: List<com.sistemaprestamista.mobile.data.model.CollectorRoute>,
        routeId: Long,
        origin: RoutePoint?,
    ): List<Long> {
        val baseClientIds = routeClientIdsFor(routes, routeId)
        if (origin == null || baseClientIds.size < 2) {
            return baseClientIds
        }

        val clientsById = clients.associateBy { it.summary.id }
        val remaining = baseClientIds.mapNotNull { clientId ->
            val client = clientsById[clientId] ?: return@mapNotNull null
            val latitude = client.summary.latitude ?: return@mapNotNull null
            val longitude = client.summary.longitude ?: return@mapNotNull null
            client.summary.id to RoutePoint(latitude, longitude)
        }.toMutableList()

        val optimizedIds = mutableListOf<Long>()
        var cursor: RoutePoint = origin

        while (remaining.isNotEmpty()) {
            val nearest = remaining.minBy { (_, point) -> distanceMeters(cursor, point) }
            optimizedIds.add(nearest.first)
            cursor = nearest.second
            remaining.remove(nearest)
        }

        return optimizedIds + baseClientIds.filterNot { it in optimizedIds }
    }

    private fun distanceMeters(from: RoutePoint, to: RoutePoint): Double {
        val earthRadiusMeters = 6371000.0
        val latDelta = Math.toRadians(to.latitude - from.latitude)
        val lngDelta = Math.toRadians(to.longitude - from.longitude)
        val fromLat = Math.toRadians(from.latitude)
        val toLat = Math.toRadians(to.latitude)
        val a = sin(latDelta / 2).pow(2.0) +
            cos(fromLat) * cos(toLat) * sin(lngDelta / 2).pow(2.0)

        return earthRadiusMeters * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    private fun resolveSelectedMapRouteId(
        currentRouteId: Long,
        routes: List<com.sistemaprestamista.mobile.data.model.CollectorRoute>,
        activeSession: com.sistemaprestamista.mobile.data.model.CollectorRouteSession?,
    ): Long {
        val routeIds = routes.map { it.id }.toSet()
        val activeRouteId = activeSession?.routeId

        return when {
            activeRouteId != null && activeRouteId in routeIds -> activeRouteId
            currentRouteId in routeIds -> currentRouteId
            routes.size == 1 -> routes.single().id
            else -> 0L
        }
    }

    private fun Throwable.userMessage(): String {
        return when (this) {
            is ApiException -> message ?: "Error del servidor."
            is java.net.SocketTimeoutException -> "El servidor tardó demasiado en responder. Intenta de nuevo."
            is java.net.UnknownHostException -> "No se encontró el servidor. Revisa tu conexión a internet."
            is java.io.IOException -> "Sin conexión con el servidor. Revisa tu internet e intenta de nuevo."
            // Cualquier otro error inesperado: mostramos la causa real para no esconder el problema.
            else -> message?.let { "No se pudo completar la operación: $it" }
                ?: "No se pudo completar la operación."
        }
    }

    class Factory(
        private val repository: PrestamistaRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository) as T
        }
    }

    private data class CollectorWorkload(
        val summary: com.sistemaprestamista.mobile.data.model.CollectorSummary,
        val clients: List<com.sistemaprestamista.mobile.data.model.ClientSummary>,
        val loans: List<com.sistemaprestamista.mobile.data.model.LoanSummary>,
        val installments: List<com.sistemaprestamista.mobile.data.model.InstallmentSummary>,
        val payments: List<com.sistemaprestamista.mobile.data.model.PaymentReceipt>,
        val mapClients: List<com.sistemaprestamista.mobile.data.model.MapClient>,
        val routes: List<com.sistemaprestamista.mobile.data.model.CollectorRoute>,
        val activeRouteSession: com.sistemaprestamista.mobile.data.model.CollectorRouteSession?,
    )

    private data class AdminWorkload(
        val clients: List<com.sistemaprestamista.mobile.data.model.ClientSummary>,
        val loans: List<com.sistemaprestamista.mobile.data.model.LoanSummary>,
        val loansHasMore: Boolean,
        val approvals: List<com.sistemaprestamista.mobile.data.model.LoanSummary>,
        val reportSummary: com.sistemaprestamista.mobile.data.model.AdminReportSummary?,
        val collectorPerformance: List<com.sistemaprestamista.mobile.data.model.CollectorPerformanceRow>,
        val payments: List<com.sistemaprestamista.mobile.data.model.PaymentReceipt> = emptyList(),
    )

    private data class AuthBundle(
        val user: com.sistemaprestamista.mobile.data.model.UserProfile,
        val dashboard: com.sistemaprestamista.mobile.data.model.DashboardSummary?,
        val collectorWorkload: CollectorWorkload?,
        val adminWorkload: AdminWorkload?,
        val cashboxWorkload: CashboxWorkload?,
    )

    private data class CashboxWorkload(
        val expenses: List<com.sistemaprestamista.mobile.data.model.ExpenseItem>,
        val categories: List<com.sistemaprestamista.mobile.data.model.ExpenseCategoryOption>,
        val movements: List<com.sistemaprestamista.mobile.data.model.CashMovementItem>,
        val summary: com.sistemaprestamista.mobile.data.model.CashSummary?,
    )

    private data class RefreshBundle(
        val dashboard: com.sistemaprestamista.mobile.data.model.DashboardSummary?,
        val collectorWorkload: CollectorWorkload?,
        val adminWorkload: AdminWorkload?,
        val cashboxWorkload: CashboxWorkload?,
    )

    private data class MapLoadResult(
        val clients: List<com.sistemaprestamista.mobile.data.model.MapClient>,
        val routes: List<com.sistemaprestamista.mobile.data.model.CollectorRoute>,
        val activeSession: com.sistemaprestamista.mobile.data.model.CollectorRouteSession?,
        val selectedRouteId: Long,
        val realRoute: List<RoutePoint>,
    )

    private data class RouteStartResult(
        val session: com.sistemaprestamista.mobile.data.model.CollectorRouteSession,
        val selectedRouteId: Long,
        val optimizedClientIds: List<Long>,
        val realRoute: List<RoutePoint>,
    )

    private data class PostPaymentRefresh(
        val summary: com.sistemaprestamista.mobile.data.model.CollectorSummary,
        val loans: List<com.sistemaprestamista.mobile.data.model.LoanSummary>,
        val installments: List<com.sistemaprestamista.mobile.data.model.InstallmentSummary>,
        val payments: List<com.sistemaprestamista.mobile.data.model.PaymentReceipt>,
    )
}
