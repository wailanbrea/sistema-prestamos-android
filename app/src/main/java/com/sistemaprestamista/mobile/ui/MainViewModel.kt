package com.sistemaprestamista.mobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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

class MainViewModel(
    private val repository: PrestamistaRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        restoreSession()
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Completa correo y contraseña.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                withContext(Dispatchers.IO) {
                    val user = repository.login(email, password)
                    val dashboard = repository.dashboard()
                    val collectorWorkload = loadCollectorWorkloadIfAllowed(user.permissions)
                    Triple(user, dashboard, collectorWorkload)
                }
            }.onSuccess { (user, dashboard, collectorWorkload) ->
                _uiState.value = AppUiState(
                    isLoading = false,
                    user = user,
                    dashboard = dashboard,
                    collectorSummary = collectorWorkload?.summary,
                    collectorClients = collectorWorkload?.clients.orEmpty(),
                    collectorLoans = collectorWorkload?.loans.orEmpty(),
                    collectorInstallments = collectorWorkload?.installments.orEmpty(),
                    paymentHistory = collectorWorkload?.payments.orEmpty(),
                )
            }.onFailure { throwable ->
                _uiState.value = AppUiState(
                    isLoading = false,
                    errorMessage = throwable.userMessage(),
                )
            }
        }
    }

    fun refreshDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                withContext(Dispatchers.IO) {
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
            _uiState.update { it.copy(errorMessage = "Selecciona un método de pago.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, lastPaymentReceipt = null) }
            runCatching {
                withContext(Dispatchers.IO) {
                    val receipt = repository.registerCollectorPayment(
                        loanId = loanId,
                        amount = amount,
                        paymentDate = LocalDate.now().toString(),
                        paymentMethod = paymentMethod,
                    )
                    val dashboard = repository.dashboard()
                    val collectorWorkload = loadCollectorWorkloadIfAllowed(uiState.value.user?.permissions.orEmpty())
                    Triple(receipt, dashboard, collectorWorkload)
                }
            }.onSuccess { (receipt, dashboard, collectorWorkload) ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        dashboard = dashboard,
                        collectorSummary = collectorWorkload?.summary ?: it.collectorSummary,
                        collectorClients = collectorWorkload?.clients ?: it.collectorClients,
                        collectorLoans = collectorWorkload?.loans ?: it.collectorLoans,
                        collectorInstallments = collectorWorkload?.installments ?: it.collectorInstallments,
                        paymentHistory = collectorWorkload?.payments ?: it.paymentHistory,
                        lastPaymentReceipt = receipt,
                        selectedPaymentDetail = receipt,
                    )
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

    private fun restoreSession() {
        viewModelScope.launch {
            if (repository.savedToken() == null) {
                _uiState.value = AppUiState(isLoading = false)
                return@launch
            }

            runCatching {
                withContext(Dispatchers.IO) {
                    val user = repository.me()
                    val dashboard = repository.dashboard()
                    val collectorWorkload = loadCollectorWorkloadIfAllowed(user.permissions)
                    Triple(user, dashboard, collectorWorkload)
                }
            }.onSuccess { (user, dashboard, collectorWorkload) ->
                _uiState.value = AppUiState(
                    isLoading = false,
                    user = user,
                    dashboard = dashboard,
                    collectorSummary = collectorWorkload?.summary,
                    collectorClients = collectorWorkload?.clients.orEmpty(),
                    collectorLoans = collectorWorkload?.loans.orEmpty(),
                    collectorInstallments = collectorWorkload?.installments.orEmpty(),
                    paymentHistory = collectorWorkload?.payments.orEmpty(),
                )
            }.onFailure {
                withContext(Dispatchers.IO) {
                    repository.logout()
                }
                _uiState.value = AppUiState(isLoading = false)
            }
        }
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
}

