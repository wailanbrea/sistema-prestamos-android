package com.sistemaprestamista.mobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sistemaprestamista.mobile.data.PrestamistaRepository
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
                    )
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isLoading = false, errorMessage = throwable.userMessage()) }
            }
        }
    }

    fun registerPayment(loanId: Long, amountText: String) {
        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _uiState.update { it.copy(errorMessage = "El monto debe ser mayor que cero.") }
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
                        paymentMethod = "cash",
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
                        lastPaymentReceipt = receipt,
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
    )
}
