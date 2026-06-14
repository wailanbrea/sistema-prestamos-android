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
    val commissionGeneratedTotal: Double = 0.0,
    val commissionPendingTotal: Double = 0.0,
    val commissionPaidTotal: Double = 0.0,
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
    val pendingPrincipal: Double = 0.0,
    val pendingInterest: Double = 0.0,
    val pendingInstallments: Int,
    val lateInstallments: Int,
    val maxDaysLate: Int = 0,
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
    val overdueInstallmentsCount: Int = 0,
    val overdueInstallmentsTotal: Double = 0.0,
    val overdueLateFeeTotal: Double = 0.0,
    val totalDueToday: Double = 0.0,
)

data class LoanDetail(
    val summary: LoanSummary,
    val collectorId: Long? = null,
    val collectorName: String? = null,
    val currency: String = "RD$",
    val allowsCapitalPrepayment: Boolean = false,
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
    val documents: List<LoanDocument> = emptyList(),
)

/**
 * Documento legal del préstamo (contrato, pagaré, etc.). El backend lista todos
 * los tipos soportados con su estado; `downloadUrl` solo existe si ya se generó.
 */
data class LoanDocument(
    val documentType: String,
    val label: String,
    val generated: Boolean,
    val documentId: Long?,
    val title: String?,
    val downloadUrl: String?,
    val createdAt: String?,
)

/**
 * Contrato digital del préstamo. `signingUrl`/`whatsappUrl` solo existen mientras el
 * contrato no esté finalizado (firmado/anulado/vencido). La firma del cliente ocurre
 * en una página web responsive abierta desde el `signingUrl`.
 */
data class ContractSummary(
    val uuid: String,
    val contractNumber: String,
    val status: String,
    val version: Int,
    val signedAt: String?,
    val signingUrl: String?,
    val whatsappUrl: String?,
    val verifyUrl: String?,
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
    val paidPrincipal: Double = 0.0,
    val paidInterest: Double = 0.0,
    val paidLateFee: Double = 0.0,
    val daysLate: Int,
    val status: String,
) {
    val pendingAmount: Double
        get() = (principalAmount + interestAmount + lateFee - totalPaid).coerceAtLeast(0.0)
    val pendingPrincipal: Double
        get() = (principalAmount - paidPrincipal).coerceAtLeast(0.0)
    val pendingInterest: Double
        get() = (interestAmount - paidInterest).coerceAtLeast(0.0)
    val pendingLateFee: Double
        get() = (lateFee - paidLateFee).coerceAtLeast(0.0)
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

data class PaymentCommission(
    val id: Long,
    val commissionType: String,
    val commissionValue: Double,
    val baseAmount: Double,
    val commissionAmount: Double,
    val status: String,
    val paidAt: String?,
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
    val commission: PaymentCommission? = null,
    val whatsappUrl: String? = null,
    val receiptUrl: String? = null,
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

/** Un gasto (endpoint cashbox/expenses). */
data class ExpenseItem(
    val id: Long,
    val date: String?,
    val category: String?,
    val categoryId: Long?,
    val description: String,
    val amount: Double,
    val paymentMethod: String,
)

/** Una opción de categoría de gasto (cashbox/expense-categories). */
data class ExpenseCategoryOption(
    val id: Long,
    val name: String,
)

/** Un movimiento de caja (cashbox/movements). */
data class CashMovementItem(
    val id: Long,
    val type: String,
    val amount: Double,
    val direction: String,
    val description: String?,
    val date: String?,
)

/** Totales de caja (cashbox/summary). */
data class CashSummary(
    val totalIn: Double,
    val totalOut: Double,
    val balance: Double,
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

data class CollectorOption(val id: Long, val name: String)

/** Datos del formulario de alta de préstamo (mismo contrato que la web). */
data class NewLoanInput(
    val clientId: Long,
    val collectorId: Long?,
    val currency: String,
    val principalAmount: Double,
    val interestRate: Double,
    val interestType: String,
    val paymentFrequency: String,
    val calculationMethod: String,
    val termQuantity: Int,
    val lateFeeType: String,
    val lateFeeValue: Double?,
    val startDate: String,
    val firstPaymentDate: String,
    val notes: String? = null,
    val quoteId: Long? = null,
)

/** Datos del formulario de alta de cliente (mismo contrato que la web). */
data class NewClientInput(
    val fullName: String,
    val identification: String? = null,
    val phone: String? = null,
    val secondaryPhone: String? = null,
    val email: String? = null,
    val address: String,
    val locationReference: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val workplace: String? = null,
    val workplacePhone: String? = null,
    val workplaceAddress: String? = null,
    val monthlyIncome: Double? = null,
    val status: String = "active",
    val riskLevel: String = "low",
    val notes: String? = null,
)

data class LoanQuote(
    val id: Long,
    val clientId: Long?,
    val clientName: String?,
    val amount: Double,
    val interestRate: Double,
    val interestType: String,
    val paymentFrequency: String,
    val calculationMethod: String,
    val termQuantity: Int,
    val status: String,
    val startDate: String?,
    val firstPaymentDate: String?,
    val createdAt: String?,
    val installmentAmount: Double,
    val totalInterest: Double,
    val totalAmount: Double,
    val installments: List<QuoteInstallment> = emptyList(),
)

data class QuoteInstallment(
    val number: Int,
    val principal: Double,
    val interest: Double,
    val amount: Double,
)

/** Datos del formulario de edición de cliente. */
data class UpdateClientInput(
    val fullName: String,
    val identification: String? = null,
    val phone: String? = null,
    val secondaryPhone: String? = null,
    val email: String? = null,
    val address: String,
    val locationReference: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val workplace: String? = null,
    val workplacePhone: String? = null,
    val monthlyIncome: Double? = null,
    val status: String = "active",
    val riskLevel: String = "low",
    val notes: String? = null,
)

/** Una comisión pendiente o pagada del cobrador. */
data class CollectorCommissionItem(
    val id: Long,
    val commissionType: String,
    val commissionValue: Double,
    val baseAmount: Double,
    val commissionAmount: Double,
    val status: String,
    val paidAt: String?,
    val receiptNumber: String?,
)

data class CollectorCommissionSummary(
    val totalGenerated: Double,
    val totalPending: Double,
    val totalPaid: Double,
)

data class CollectorStats(
    val activeLoans: Int,
    val lateLoans: Int,
)

/** Detalle completo de cobrador con comisiones. */
data class CollectorDetail(
    val id: Long,
    val name: String,
    val phone: String?,
    val commissionType: String,
    val commissionBase: String,
    val commissionValue: Double,
    val status: String,
    val commissionSummary: CollectorCommissionSummary,
    val pendingCommissions: List<CollectorCommissionItem>,
    val stats: CollectorStats = CollectorStats(0, 0),
)

data class NewCollectorInput(
    val name: String,
    val phone: String? = null,
    val commissionType: String,
    val commissionBase: String,
    val commissionValue: Double?,
    val status: String = "active",
)

data class UpdateCollectorInput(
    val name: String,
    val phone: String? = null,
    val commissionType: String,
    val commissionBase: String,
    val commissionValue: Double?,
    val status: String,
)

data class CashMovementInput(
    val type: String,
    val direction: String?,
    val amount: Double,
    val movementDate: String,
    val description: String,
)

/** Datos del formulario de edición de préstamo. */
data class UpdateLoanInput(
    val collectorId: Long?,
    val currency: String,
    val guaranteeDescription: String?,
    val notes: String?,
    val allowsCapitalPrepayment: Boolean,
    // Financial fields — only sent when the loan has no valid payments yet
    val principalAmount: Double? = null,
    val interestRate: Double? = null,
    val interestType: String? = null,
    val paymentFrequency: String? = null,
    val calculationMethod: String? = null,
    val termQuantity: Int? = null,
    val lateFeeType: String? = null,
    val lateFeeValue: Double? = null,
    val startDate: String? = null,
    val firstPaymentDate: String? = null,
)

/** Resultado de generar un link de auto-registro para un cliente. */
data class ClientRegistrationLink(
    val formUrl: String,
    val whatsappUrl: String?,
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

enum class AllocationMode(
    val apiValue: String,
    val label: String,
    val shortLabel: String,
) {
    Auto("auto", "Cap. + Int. + Mora", "Todo"),
    PrincipalAndInterest("principal_and_interest", "Capital + Interés", "Cap. + Int."),
    PrincipalOnly("principal_only", "Solo capital", "Capital"),
    InterestOnly("interest_only", "Solo interés", "Interés"),
}
