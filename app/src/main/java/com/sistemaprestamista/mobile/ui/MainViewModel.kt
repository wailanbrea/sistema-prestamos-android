package com.sistemaprestamista.mobile.ui

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sistemaprestamista.mobile.data.PaymentRegistrationResult
import com.sistemaprestamista.mobile.data.PrestamistaRepository
import com.sistemaprestamista.mobile.data.model.PaymentHistoryFilters
import com.sistemaprestamista.mobile.data.model.RoutePoint
import com.sistemaprestamista.mobile.data.remote.ApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID

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
                withContext(Dispatchers.IO) {
                    val user = repository.login(normalizedEmail, password)
                    AuthBundle(
                        user = user,
                        dashboard = loadDashboardIfAllowed(user),
                        collectorWorkload = loadCollectorWorkloadIfAllowed(user),
                        adminWorkload = loadAdminWorkloadIfAllowed(user),
                        cashboxWorkload = loadCashboxWorkloadIfAllowed(user),
                    )
                }
            }.onSuccess { bundle ->
                _uiState.value = authenticatedState(bundle)
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
                _uiState.update { it.copy(isLoading = false, errorMessage = throwable.userMessage()) }
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
                _uiState.update { it.copy(isLoading = false, errorMessage = throwable.userMessage()) }
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
                    val dashboard = loadDashboardIfAllowed(currentUser)
                    val collectorWorkload = loadCollectorWorkloadIfAllowed(currentUser)
                    val adminWorkload = loadAdminWorkloadIfAllowed(currentUser)
                    val cashboxWorkload = loadCashboxWorkloadIfAllowed(currentUser)
                    RefreshBundle(dashboard, collectorWorkload, adminWorkload, cashboxWorkload)
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
                        paymentHistory = collectorWorkload?.payments ?: it.paymentHistory,
                        mapClients = collectorWorkload?.mapClients ?: it.mapClients,
                        collectorRoutes = collectorWorkload?.routes ?: it.collectorRoutes,
                        activeRouteSession = collectorWorkload?.activeRouteSession ?: it.activeRouteSession,
                        adminClients = adminWorkload?.clients ?: it.adminClients,
                        adminLoans = adminWorkload?.loans ?: it.adminLoans,
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
                _uiState.update { it.copy(isLoading = false, errorMessage = throwable.userMessage()) }
            }
        }
    }

    fun registerPayment(loanId: Long, amountText: String, paymentMethod: String) {
        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _uiState.update { it.copy(errorMessage = "El monto debe ser mayor que cero.") }
            return
        }
        if (paymentMethod.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Selecciona un metodo de pago.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, lastPaymentReceipt = null) }
            runCatching {
                withContext(Dispatchers.IO) {
                    val result = repository.registerCollectorPayment(
                        loanId = loanId,
                        amount = amount,
                        paymentDate = LocalDate.now().toString(),
                        paymentMethod = paymentMethod,
                        mobileUuid = UUID.randomUUID().toString(),
                    )
                    when (result) {
                        is PaymentRegistrationResult.Sent -> {
                            val dashboard = loadDashboardIfAllowed(uiState.value.user)
                            val collectorWorkload = loadCollectorWorkloadIfAllowed(uiState.value.user)
                            PaymentRegistrationOutcome.Sent(result.receipt, dashboard, collectorWorkload)
                        }
                        is PaymentRegistrationResult.Queued -> PaymentRegistrationOutcome.Queued(result.pendingCount)
                    }
                }
            }.onSuccess { outcome ->
                when (outcome) {
                    is PaymentRegistrationOutcome.Sent -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                dashboard = outcome.dashboard ?: it.dashboard,
                                collectorSummary = outcome.collectorWorkload?.summary ?: it.collectorSummary,
                                collectorClients = outcome.collectorWorkload?.clients ?: it.collectorClients,
                                collectorLoans = outcome.collectorWorkload?.loans ?: it.collectorLoans,
                                collectorInstallments = outcome.collectorWorkload?.installments ?: it.collectorInstallments,
                                paymentHistory = outcome.collectorWorkload?.payments ?: it.paymentHistory,
                                mapClients = outcome.collectorWorkload?.mapClients ?: it.mapClients,
                                collectorRoutes = outcome.collectorWorkload?.routes ?: it.collectorRoutes,
                                activeRouteSession = outcome.collectorWorkload?.activeRouteSession ?: it.activeRouteSession,
                                pendingPaymentCount = repository.pendingPaymentCount(),
                                lastPaymentReceipt = outcome.receipt,
                                selectedPaymentDetail = outcome.receipt,
                            )
                        }
                    }
                    is PaymentRegistrationOutcome.Queued -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                pendingPaymentCount = outcome.pendingCount,
                                successMessage = "Cobro guardado sin conexion. Se sincronizara automaticamente.",
                            )
                        }
                    }
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isLoading = false, errorMessage = throwable.userMessage()) }
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

        viewModelScope.launch {
            _uiState.update { it.copy(isDetailLoading = true, errorMessage = null) }
            runCatching {
                withContext(Dispatchers.IO) {
                    repository.collectorPayment(paymentId)
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
                    val realRoute = runCatching { repository.drivingRoute(routePoints) }.getOrElse { emptyList() }
                    MapLoadResult(clients, routes, activeSession, selectedRouteId, realRoute)
                }
            }.onSuccess { result ->
                _uiState.update {
                    it.copy(
                        isMapLoading = false,
                        mapClients = result.clients,
                        collectorRoutes = result.routes,
                        selectedMapRouteId = result.selectedRouteId,
                        activeRouteSession = result.activeSession,
                        realRoutePoints = result.realRoute,
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

    fun startRouteTracking(routeId: Long) {
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
                    repository.startRouteSession(resolvedRouteId)
                }
            }.onSuccess { session ->
                _uiState.update {
                    it.copy(
                        isRouteTrackingLoading = false,
                        activeRouteSession = session,
                        selectedMapRouteId = session.routeId ?: resolvedRouteId,
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

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedMapRouteId = routeId,
                    isMapLoading = true,
                    realRoutePoints = emptyList(),
                    routeWarning = null,
                )
            }
            runCatching {
                withContext(Dispatchers.IO) {
                    val current = uiState.value
                    val points = routePointsFor(current.mapClients, current.collectorRoutes, routeId)
                    points to repository.drivingRoute(points)
                }
            }.onSuccess { (points, realRoute) ->
                _uiState.update {
                    it.copy(
                        isMapLoading = false,
                        realRoutePoints = realRoute,
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
        _uiState.value = AppUiState(
            isLoading = false,
            hasSavedSession = repository.hasSavedSession(),
            pendingPaymentCount = repository.pendingPaymentCount(),
            pendingPayments = repository.pendingPayments(),
        )
        repository.enqueuePendingPaymentSync()
    }

    private fun restoreSession() {
        viewModelScope.launch {
            if (repository.savedToken() == null) {
                _uiState.value = AppUiState(isLoading = false)
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                withContext(Dispatchers.IO) {
                    val user = repository.me()
                    AuthBundle(
                        user = user,
                        dashboard = loadDashboardIfAllowed(user),
                        collectorWorkload = loadCollectorWorkloadIfAllowed(user),
                        adminWorkload = loadAdminWorkloadIfAllowed(user),
                        cashboxWorkload = loadCashboxWorkloadIfAllowed(user),
                    )
                }
            }.onSuccess { bundle ->
                _uiState.value = authenticatedState(bundle)
            }.onFailure {
                withContext(Dispatchers.IO) {
                    repository.logout()
                }
                _uiState.value = AppUiState(
                    isLoading = false,
                    errorMessage = "La sesion guardada expiro. Entra con correo y contrasena.",
                )
            }
        }
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
            paymentHistory = collectorWorkload?.payments.orEmpty(),
            mapClients = collectorWorkload?.mapClients.orEmpty(),
            collectorRoutes = collectorWorkload?.routes.orEmpty(),
            activeRouteSession = collectorWorkload?.activeRouteSession,
            adminClients = adminWorkload?.clients.orEmpty(),
            adminLoans = adminWorkload?.loans.orEmpty(),
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

    private fun loadCashboxWorkloadIfAllowed(
        user: com.sistemaprestamista.mobile.data.model.UserProfile?,
    ): CashboxWorkload? {
        val permissions = user?.permissions.orEmpty()
        val managePortfolio = permissions.contains("collectors.manage") && user?.isCollector != true
        val isCollector = user?.isCollector == true
        val canExpenses = permissions.contains("expenses.manage") && !managePortfolio && !isCollector
        val canCash = permissions.contains("cash.view") && !managePortfolio && !isCollector

        if (!canExpenses && !canCash) {
            return null
        }

        return CashboxWorkload(
            expenses = if (canExpenses) repository.cashboxExpenses() else emptyList(),
            categories = if (canExpenses) runCatching { repository.cashboxCategories() }.getOrDefault(emptyList()) else emptyList(),
            movements = if (canCash) repository.cashboxMovements() else emptyList(),
            summary = if (canCash) runCatching { repository.cashboxSummary() }.getOrNull() else null,
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

    private fun loadAdminWorkloadIfAllowed(
        user: com.sistemaprestamista.mobile.data.model.UserProfile?,
    ): AdminWorkload? {
        val permissions = user?.permissions.orEmpty()
        val managePortfolio = permissions.contains("collectors.manage") && user?.isCollector != true
        val canApprove = permissions.contains("loans.approve")
        val canViewReports = permissions.contains("reports.view")

        if (!managePortfolio && !canApprove && !canViewReports) {
            return null
        }

        return AdminWorkload(
            clients = if (managePortfolio) repository.adminClients() else emptyList(),
            loans = if (managePortfolio) repository.adminLoans() else emptyList(),
            approvals = if (canApprove) repository.adminApprovals() else emptyList(),
            reportSummary = if (canViewReports) runCatching { repository.adminReportSummary() }.getOrNull() else null,
            collectorPerformance = if (canViewReports) runCatching { repository.adminReportCollectors() }.getOrNull().orEmpty() else emptyList(),
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

    private fun loadCollectorWorkloadIfAllowed(
        user: com.sistemaprestamista.mobile.data.model.UserProfile?,
    ): CollectorWorkload? {
        // Solo carga la cartera de campo si el usuario es un cobrador real (flag del backend),
        // no por tener el permiso payments.create (que el Administrador también tiene).
        if (user?.isCollector != true) {
            return null
        }

        return CollectorWorkload(
            summary = repository.collectorSummary(),
            clients = repository.collectorClients(),
            loans = repository.collectorLoans(),
            installments = repository.collectorInstallments(),
            payments = repository.collectorPayments(),
            mapClients = repository.collectorMapClients(),
            routes = repository.collectorRoutes(),
            activeRouteSession = repository.activeRouteSession(),
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
    ): List<RoutePoint> {
        val visibleClients = if (routeId == 0L) {
            clients
        } else {
            val clientIds = routes.firstOrNull { it.id == routeId }?.clients?.map { it.summary.id }?.toSet().orEmpty()
            clients.filter { it.summary.id in clientIds }
        }

        return visibleClients.mapNotNull { client ->
            val latitude = client.summary.latitude ?: return@mapNotNull null
            val longitude = client.summary.longitude ?: return@mapNotNull null
            RoutePoint(latitude, longitude)
        }
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
            else -> "No se pudo conectar con el servidor."
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
        val approvals: List<com.sistemaprestamista.mobile.data.model.LoanSummary>,
        val reportSummary: com.sistemaprestamista.mobile.data.model.AdminReportSummary?,
        val collectorPerformance: List<com.sistemaprestamista.mobile.data.model.CollectorPerformanceRow>,
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

    private sealed interface PaymentRegistrationOutcome {
        data class Sent(
            val receipt: com.sistemaprestamista.mobile.data.model.PaymentReceipt,
            val dashboard: com.sistemaprestamista.mobile.data.model.DashboardSummary?,
            val collectorWorkload: CollectorWorkload?,
        ) : PaymentRegistrationOutcome

        data class Queued(val pendingCount: Int) : PaymentRegistrationOutcome
    }
}
