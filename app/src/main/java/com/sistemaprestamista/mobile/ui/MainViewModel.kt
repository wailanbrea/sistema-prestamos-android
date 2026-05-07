package com.sistemaprestamista.mobile.ui

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sistemaprestamista.mobile.data.PaymentRegistrationResult
import com.sistemaprestamista.mobile.data.PrestamistaRepository
import com.sistemaprestamista.mobile.data.model.PaymentHistoryFilters
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
                    val dashboard = repository.dashboard()
                    val collectorWorkload = loadCollectorWorkloadIfAllowed(user.permissions)
                    Triple(user, dashboard, collectorWorkload)
                }
            }.onSuccess { (user, dashboard, collectorWorkload) ->
                _uiState.value = authenticatedState(user, dashboard, collectorWorkload)
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
                    val dashboard = repository.dashboard()
                    val collectorWorkload = loadCollectorWorkloadIfAllowed(uiState.value.user?.permissions.orEmpty())
                    dashboard to collectorWorkload
                }
            }.onSuccess { (dashboard, collectorWorkload) ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        dashboard = dashboard,
                        collectorSummary = collectorWorkload?.summary ?: it.collectorSummary,
                        collectorClients = collectorWorkload?.clients ?: it.collectorClients,
                        collectorLoans = collectorWorkload?.loans ?: it.collectorLoans,
                        collectorInstallments = collectorWorkload?.installments ?: it.collectorInstallments,
                        paymentHistory = collectorWorkload?.payments ?: it.paymentHistory,
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
                            val dashboard = repository.dashboard()
                            val collectorWorkload = loadCollectorWorkloadIfAllowed(uiState.value.user?.permissions.orEmpty())
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
                                dashboard = outcome.dashboard,
                                collectorSummary = outcome.collectorWorkload?.summary ?: it.collectorSummary,
                                collectorClients = outcome.collectorWorkload?.clients ?: it.collectorClients,
                                collectorLoans = outcome.collectorWorkload?.loans ?: it.collectorLoans,
                                collectorInstallments = outcome.collectorWorkload?.installments ?: it.collectorInstallments,
                                paymentHistory = outcome.collectorWorkload?.payments ?: it.paymentHistory,
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
                    val dashboard = repository.dashboard()
                    val collectorWorkload = loadCollectorWorkloadIfAllowed(user.permissions)
                    Triple(user, dashboard, collectorWorkload)
                }
            }.onSuccess { (user, dashboard, collectorWorkload) ->
                _uiState.value = authenticatedState(user, dashboard, collectorWorkload)
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

    private fun authenticatedState(
        user: com.sistemaprestamista.mobile.data.model.UserProfile,
        dashboard: com.sistemaprestamista.mobile.data.model.DashboardSummary,
        collectorWorkload: CollectorWorkload?,
    ): AppUiState {
        return AppUiState(
            isLoading = false,
            hasSavedSession = true,
            pendingPaymentCount = repository.pendingPaymentCount(),
            pendingPayments = repository.pendingPayments(),
            user = user,
            dashboard = dashboard,
            collectorSummary = collectorWorkload?.summary,
            collectorClients = collectorWorkload?.clients.orEmpty(),
            collectorLoans = collectorWorkload?.loans.orEmpty(),
            collectorInstallments = collectorWorkload?.installments.orEmpty(),
            paymentHistory = collectorWorkload?.payments.orEmpty(),
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

    private fun loadCollectorWorkloadIfAllowed(permissions: List<String>): CollectorWorkload? {
        if (!permissions.contains("payments.create")) {
            return null
        }

        return CollectorWorkload(
            summary = repository.collectorSummary(),
            clients = repository.collectorClients(),
            loans = repository.collectorLoans(),
            installments = repository.collectorInstallments(),
            payments = repository.collectorPayments(),
        )
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
    )

    private sealed interface PaymentRegistrationOutcome {
        data class Sent(
            val receipt: com.sistemaprestamista.mobile.data.model.PaymentReceipt,
            val dashboard: com.sistemaprestamista.mobile.data.model.DashboardSummary,
            val collectorWorkload: CollectorWorkload?,
        ) : PaymentRegistrationOutcome

        data class Queued(val pendingCount: Int) : PaymentRegistrationOutcome
    }
}
