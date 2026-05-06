package com.sistemaprestamista.mobile.data.model

data class Company(
    val id: Long,
    val name: String,
    val status: String,
)

data class UserProfile(
    val id: Long,
    val name: String,
    val email: String,
    val company: Company,
    val roles: List<String>,
    val permissions: List<String>,
)

data class DashboardSummary(
    val capitalPrestado: Double,
    val cobrosHoy: Double,
    val interesesGenerados: Double,
    val gananciaNeta: Double,
    val gastosMes: Double,
    val clientesAtrasados: Int,
    val prestamosActivos: Int,
    val prestamosMora: Int,
    val cobradoresActivos: Int,
)

data class LoginResult(
    val accessToken: String,
    val user: UserProfile,
)

data class CollectorSummary(
    val collectorId: Long,
    val collectorName: String,
    val assignedClients: Int,
    val activeLoans: Int,
    val lateLoans: Int,
    val pendingInstallments: Int,
    val collectedToday: Double,
)

data class ClientSummary(
    val id: Long,
    val fullName: String,
    val identification: String?,
    val phone: String?,
    val address: String?,
    val status: String,
    val riskLevel: String,
)

data class LoanSummary(
    val id: Long,
    val loanNumber: String,
    val client: ClientSummary?,
    val principalAmount: Double,
    val installmentAmount: Double,
    val totalAmount: Double,
    val remainingBalance: Double,
    val paymentFrequency: String,
    val status: String,
)

data class InstallmentSummary(
    val id: Long,
    val loanId: Long,
    val loanNumber: String,
    val client: ClientSummary?,
    val installmentNumber: Int,
    val dueDate: String?,
    val principalAmount: Double,
    val interestAmount: Double,
    val lateFee: Double,
    val installmentAmount: Double,
    val totalPaid: Double,
    val daysLate: Int,
    val status: String,
) {
    val pendingAmount: Double
        get() = (principalAmount + interestAmount + lateFee - totalPaid).coerceAtLeast(0.0)
}

data class PaymentReceipt(
    val id: Long,
    val receiptNumber: String,
    val loanId: Long,
    val loanNumber: String?,
    val client: ClientSummary?,
    val paymentDate: String?,
    val amount: Double,
    val principalPaid: Double,
    val interestPaid: Double,
    val lateFeePaid: Double,
    val previousBalance: Double,
    val newBalance: Double,
    val paymentMethod: String,
    val status: String,
)
