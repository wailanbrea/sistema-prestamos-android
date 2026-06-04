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
    /** Cobrador de campo real (vinculado a un Collector activo). Lo envía el backend. */
    val isCollector: Boolean = false,
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
    val latitude: Double?,
    val longitude: Double?,
    val locationReference: String?,
    val status: String,
    val riskLevel: String,
)

data class ClientReference(
    val id: Long,
    val name: String,
    val phone: String,
    val relationship: String?,
    val address: String?,
)

data class ClientRouteSummary(
    val id: Long,
    val name: String,
)

data class ClientFinancialSummary(
    val activeLoans: Int,
    val lateLoans: Int,
    val totalPrincipal: Double,
    val remainingBalance: Double,
    val pendingInstallments: Int,
    val lateInstallments: Int,
    val totalPaid: Double,
    val lastPaymentDate: String?,
)

data class MapClient(
    val summary: ClientSummary,
    val financialSummary: ClientFinancialSummary,
    val routes: List<ClientRouteMapSummary>,
) {
    val hasCoordinates: Boolean
        get() = summary.latitude != null && summary.longitude != null
}

data class ClientRouteMapSummary(
    val id: Long,
    val name: String,
    val orderNumber: Int,
)

data class CollectorRoute(
    val id: Long,
    val name: String,
    val description: String?,
    val zoneName: String?,
    val clientsCount: Int,
    val clients: List<RouteClientStop>,
)

data class CollectorRouteSession(
    val id: Long,
    val status: String,
    val startedAt: String?,
    val endedAt: String?,
    val lastLocationAt: String?,
    val lastLatitude: Double?,
    val lastLongitude: Double?,
    val collectorName: String?,
    val routeId: Long?,
    val routeName: String?,
    val stops: List<RouteTrackingStop>,
) {
    val visitedStops: Int
        get() = stops.count { it.visited }

    val totalStops: Int
        get() = stops.size
}

data class RouteTrackingStop(
    val clientId: Long,
    val clientName: String,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
    val expectedOrder: Int,
    val visited: Boolean,
    val visitedOrder: Int?,
    val visitedAt: String?,
    val visitStatus: String?,
    val distanceMeters: Int?,
)

data class RouteClientStop(
    val summary: ClientSummary,
    val orderNumber: Int,
    val financialSummary: ClientFinancialSummary,
)

data class RoutePoint(
    val latitude: Double,
    val longitude: Double,
)

data class ClientDetail(
    val summary: ClientSummary,
    val secondaryPhone: String?,
    val email: String?,
    val workplace: String?,
    val workplacePhone: String?,
    val monthlyIncome: Double,
    val notes: String?,
    val references: List<ClientReference>,
    val routes: List<ClientRouteSummary>,
    val financialSummary: ClientFinancialSummary,
    val loans: List<LoanSummary>,
    val pendingInstallments: List<InstallmentSummary>,
    val recentPayments: List<PaymentReceipt>,
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

data class LoanFinancialSummary(
    val installmentsTotal: Int,
    val installmentsPending: Int,
    val installmentsLate: Int,
    val paymentsTotal: Int,
    val amountPaid: Double,
)

data class LoanDetail(
    val summary: LoanSummary,
    val interestRate: Double,
    val interestType: String,
    val calculationMethod: String,
    val termQuantity: Int,
    val totalInterest: Double,
    val paidPrincipal: Double,
    val paidInterest: Double,
    val paidLateFee: Double,
    val startDate: String?,
    val firstPaymentDate: String?,
    val endDate: String?,
    val lateFeeType: String,
    val lateFeeValue: Double,
    val guaranteeDescription: String?,
    val notes: String?,
    val financialSummary: LoanFinancialSummary,
    val installments: List<InstallmentSummary>,
    val payments: List<PaymentReceipt>,
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

data class InstallmentPaymentLine(
    val id: Long,
    val paymentId: Long,
    val receiptNumber: String?,
    val paymentDate: String?,
    val paymentMethod: String?,
    val paymentStatus: String?,
    val principalPaid: Double,
    val interestPaid: Double,
    val lateFeePaid: Double,
    val amountPaid: Double,
)

data class InstallmentDetail(
    val summary: InstallmentSummary,
    val payments: List<InstallmentPaymentLine>,
)

data class PaymentDetailLine(
    val id: Long,
    val installmentId: Long,
    val installmentNumber: Int?,
    val principalPaid: Double,
    val interestPaid: Double,
    val lateFeePaid: Double,
    val amountPaid: Double,
)

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
    val details: List<PaymentDetailLine> = emptyList(),
)

data class PaymentHistoryFilters(
    val clientId: Long? = null,
    val loanId: Long? = null,
    val status: String? = null,
    val dateFrom: String? = null,
    val dateTo: String? = null,
)

// --- Back-office / administrador ---

/** Resumen financiero/inversión (endpoint admin/reports/summary). */
data class AdminReportSummary(
    val capitalInvested: Double,
    val capitalOnStreet: Double,
    val capitalRecovered: Double,
    val interestEarned: Double,
    val lateFeeEarned: Double,
    val expenses: Double,
    val newDisbursed: Double,
    val netBalance: Double,
    val roi: Double,
    val monthlyReturn: Double,
    val activeClients: Int,
    val inactiveClients: Int,
    val overdueClients: Int,
)

/** Una fila del rendimiento por cobrador (endpoint admin/reports/collectors). */
data class CollectorPerformanceRow(
    val collector: String,
    val capital: Double,
    val interest: Double,
    val lateFee: Double,
    val collected: Double,
    val disbursed: Double,
    val activeAccounts: Int,
    val overdueAccounts: Int,
)

enum class PaymentMethod(
    val apiValue: String,
    val label: String,
) {
    Cash("cash", "Efectivo"),
    Transfer("transfer", "Transferencia"),
    Card("card", "Tarjeta"),
    Check("check", "Cheque"),
    Other("other", "Otro"),
}
