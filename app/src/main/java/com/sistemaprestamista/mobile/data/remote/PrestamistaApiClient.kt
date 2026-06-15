package com.sistemaprestamista.mobile.data.remote

import com.sistemaprestamista.mobile.BuildConfig
import com.sistemaprestamista.mobile.data.ResponseCache
import com.sistemaprestamista.mobile.data.model.AdminReportSummary
import com.sistemaprestamista.mobile.data.model.CollectorPerformanceRow
import com.sistemaprestamista.mobile.data.model.Company
import com.sistemaprestamista.mobile.data.model.ContractSummary
import com.sistemaprestamista.mobile.data.model.ClientDetail
import com.sistemaprestamista.mobile.data.model.ClientFinancialSummary
import com.sistemaprestamista.mobile.data.model.ClientReference
import com.sistemaprestamista.mobile.data.model.ClientRouteMapSummary
import com.sistemaprestamista.mobile.data.model.ClientRouteSummary
import com.sistemaprestamista.mobile.data.model.ClientSummary
import com.sistemaprestamista.mobile.data.model.CollectorSummary
import com.sistemaprestamista.mobile.data.model.CollectorRoute
import com.sistemaprestamista.mobile.data.model.CollectorRouteSession
import com.sistemaprestamista.mobile.data.model.DashboardSummary
import com.sistemaprestamista.mobile.data.model.InstallmentSummary
import com.sistemaprestamista.mobile.data.model.InstallmentDetail
import com.sistemaprestamista.mobile.data.model.InstallmentPaymentLine
import com.sistemaprestamista.mobile.data.model.LoanDetail
import com.sistemaprestamista.mobile.data.model.LoanDocument
import com.sistemaprestamista.mobile.data.model.LoanFinancialSummary
import com.sistemaprestamista.mobile.data.model.LoanSummary
import com.sistemaprestamista.mobile.data.model.LoanQuote
import com.sistemaprestamista.mobile.data.model.LoginResult
import com.sistemaprestamista.mobile.data.model.MapClient
import com.sistemaprestamista.mobile.data.model.CashMovementInput
import com.sistemaprestamista.mobile.data.model.ClientRegistrationLink
import com.sistemaprestamista.mobile.data.model.CollectorCommissionItem
import com.sistemaprestamista.mobile.data.model.CollectorCommissionSummary
import com.sistemaprestamista.mobile.data.model.CollectorDetail
import com.sistemaprestamista.mobile.data.model.CollectorOption
import com.sistemaprestamista.mobile.data.model.CollectorStats
import com.sistemaprestamista.mobile.data.model.NewClientInput
import com.sistemaprestamista.mobile.data.model.NewCollectorInput
import com.sistemaprestamista.mobile.data.model.NewLoanInput
import com.sistemaprestamista.mobile.data.model.UpdateClientInput
import com.sistemaprestamista.mobile.data.model.UpdateCollectorInput
import com.sistemaprestamista.mobile.data.model.UpdateLoanInput
import com.sistemaprestamista.mobile.data.model.Page
import com.sistemaprestamista.mobile.data.model.PaymentDetailLine
import com.sistemaprestamista.mobile.data.model.PaymentHistoryFilters
import com.sistemaprestamista.mobile.data.model.PaymentCommission
import com.sistemaprestamista.mobile.data.model.PaymentReceipt
import com.sistemaprestamista.mobile.data.model.QuoteInstallment
import com.sistemaprestamista.mobile.data.model.RouteClientStop
import com.sistemaprestamista.mobile.data.model.RouteTrackingStop
import com.sistemaprestamista.mobile.data.model.UserProfile
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

class PrestamistaApiClient(
    private val responseCache: ResponseCache? = null,
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BASIC
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            },
        )
        .build()

    fun login(email: String, password: String, deviceName: String): LoginResult {
        val payload = JSONObject()
            .put("email", email)
            .put("password", password)
            .put("device_name", deviceName)

        val json = request(
            path = "auth/login",
            method = "POST",
            token = null,
            body = payload,
        )

        val data = json.getJSONObject("data")
        return LoginResult(
            accessToken = data.getString("access_token"),
            user = parseUser(data.getJSONObject("user")),
        )
    }

    fun me(token: String, cacheOnly: Boolean = false): UserProfile {
        val json = request(path = "me", method = "GET", token = token, cacheOnly = cacheOnly)
        return parseUser(json.getJSONObject("data"))
    }

    fun dashboard(token: String): DashboardSummary {
        val json = request(path = "dashboard", method = "GET", token = token)
        val data = json.getJSONObject("data")

        return DashboardSummary(
            capitalPrestado = data.optDouble("capital_prestado", 0.0),
            cobrosHoy = data.optDouble("cobros_hoy", 0.0),
            interesesGenerados = data.optDouble("intereses_generados", 0.0),
            gananciaNeta = data.optDouble("ganancia_neta", 0.0),
            gastosMes = data.optDouble("gastos_mes", 0.0),
            clientesAtrasados = data.optInt("clientes_atrasados", 0),
            prestamosActivos = data.optInt("prestamos_activos", 0),
            prestamosMora = data.optInt("prestamos_mora", 0),
            cobradoresActivos = data.optInt("cobradores_activos", 0),
        )
    }

    fun logout(token: String) {
        request(path = "auth/logout", method = "POST", token = token)
    }

    fun requestPasswordReset(email: String): String {
        val payload = JSONObject()
            .put("email", email)

        val json = request(
            path = "auth/forgot-password",
            method = "POST",
            token = null,
            body = payload,
        )

        return json.optString("message", "Si el correo existe, enviaremos las instrucciones.")
    }

    fun resetPassword(
        email: String,
        token: String,
        password: String,
        passwordConfirmation: String,
    ): String {
        val payload = JSONObject()
            .put("email", email)
            .put("token", token)
            .put("password", password)
            .put("password_confirmation", passwordConfirmation)

        val json = request(
            path = "auth/reset-password",
            method = "POST",
            token = null,
            body = payload,
        )

        return json.optString("message", "Contrasena restablecida correctamente.")
    }

    fun collectorSummary(token: String, cacheOnly: Boolean = false): CollectorSummary {
        val json = request(path = "collector/summary", method = "GET", token = token, cacheOnly = cacheOnly)
        val data = json.getJSONObject("data")
        val collector = data.getJSONObject("collector")
        val commissions = data.optJSONObject("commissions") ?: JSONObject()

        return CollectorSummary(
            collectorId = collector.getLong("id"),
            collectorName = collector.getString("name"),
            assignedClients = data.optInt("assigned_clients", 0),
            activeLoans = data.optInt("active_loans", 0),
            lateLoans = data.optInt("late_loans", 0),
            pendingInstallments = data.optInt("pending_installments", 0),
            collectedToday = data.optDouble("collected_today", 0.0),
            commissionGeneratedTotal = commissions.optDouble("generated_total", 0.0),
            commissionPendingTotal = commissions.optDouble("pending_total", 0.0),
            commissionPaidTotal = commissions.optDouble("paid_total", 0.0),
        )
    }

    fun collectorClients(token: String, cacheOnly: Boolean = false): List<ClientSummary> {
        val json = request(path = "collector/clients?per_page=100", method = "GET", token = token, cacheOnly = cacheOnly)
        return json.optJSONArray("data").mapObjects(::parseClient)
    }

    fun collectorMapClients(token: String, cacheOnly: Boolean = false): List<MapClient> {
        val json = request(path = "collector/map-clients", method = "GET", token = token, cacheOnly = cacheOnly)
        return json.optJSONArray("data").mapObjects(::parseMapClient)
    }

    fun collectorRoutes(token: String, cacheOnly: Boolean = false): List<CollectorRoute> {
        val json = request(path = "collector/routes", method = "GET", token = token, cacheOnly = cacheOnly)
        return json.optJSONArray("data").mapObjects(::parseRoute)
    }

    fun activeRouteSession(token: String, cacheOnly: Boolean = false): CollectorRouteSession? {
        val json = request(path = "collector/route-sessions/active", method = "GET", token = token, cacheOnly = cacheOnly)
        val data = json.optJSONObject("data")
        return data?.let(::parseRouteSession)
    }

    fun startRouteSession(token: String, routeId: Long): CollectorRouteSession {
        val payload = JSONObject().put("route_id", routeId)
        val json = request(
            path = "collector/route-sessions",
            method = "POST",
            token = token,
            body = payload,
        )
        return parseRouteSession(json.getJSONObject("data"))
    }

    fun sendRouteLocation(
        token: String,
        sessionId: Long,
        latitude: Double,
        longitude: Double,
        accuracyMeters: Int?,
        batteryLevel: Int?,
        recordedAt: String,
    ): CollectorRouteSession {
        val payload = JSONObject()
            .put("latitude", latitude)
            .put("longitude", longitude)
            .put("recorded_at", recordedAt)
        if (accuracyMeters != null) {
            payload.put("accuracy_meters", accuracyMeters)
        }
        if (batteryLevel != null) {
            payload.put("battery_level", batteryLevel)
        }

        val json = request(
            path = "collector/route-sessions/$sessionId/locations",
            method = "POST",
            token = token,
            body = payload,
        )
        return parseRouteSession(json.getJSONObject("data"))
    }

    fun finishRouteSession(token: String, sessionId: Long): CollectorRouteSession {
        val json = request(
            path = "collector/route-sessions/$sessionId/finish",
            method = "POST",
            token = token,
        )
        return parseRouteSession(json.getJSONObject("data"))
    }

    fun collectorClient(token: String, clientId: Long): ClientDetail {
        val json = request(path = "collector/clients/$clientId", method = "GET", token = token)
        return parseClientDetail(json.getJSONObject("data"))
    }

    fun collectorLoans(token: String, cacheOnly: Boolean = false): List<LoanSummary> {
        val json = request(path = "collector/loans?per_page=100", method = "GET", token = token, cacheOnly = cacheOnly)
        return json.optJSONArray("data").mapObjects(::parseLoan)
    }

    fun collectorLoan(token: String, loanId: Long): LoanDetail {
        val json = request(path = "collector/loans/$loanId", method = "GET", token = token)
        return parseLoanDetail(json.getJSONObject("data"))
    }

    fun collectorInstallments(token: String, cacheOnly: Boolean = false): List<InstallmentSummary> {
        val json = request(path = "collector/installments?per_page=100", method = "GET", token = token, cacheOnly = cacheOnly)
        return json.optJSONArray("data").mapObjects(::parseInstallment)
    }

    fun collectorInstallment(token: String, installmentId: Long): InstallmentDetail {
        val json = request(path = "collector/installments/$installmentId", method = "GET", token = token)
        return parseInstallmentDetail(json.getJSONObject("data"))
    }

    fun collectorPayments(token: String, filters: PaymentHistoryFilters = PaymentHistoryFilters(), cacheOnly: Boolean = false): List<PaymentReceipt> {
        val json = request(path = "collector/payments?${filters.toQueryString()}", method = "GET", token = token, cacheOnly = cacheOnly)
        return json.optJSONArray("data").mapObjects(::parsePayment)
    }

    fun collectorPayment(token: String, paymentId: Long): PaymentReceipt {
        val json = request(path = "collector/payments/$paymentId", method = "GET", token = token)
        return parsePayment(json.getJSONObject("data"))
    }

    fun registerCollectorPayment(
        token: String,
        loanId: Long,
        amount: Double,
        paymentDate: String,
        paymentMethod: String,
        mobileUuid: String,
        allocationMode: String = "auto",
        targetInstallmentId: Long? = null,
    ): PaymentReceipt {
        val payload = JSONObject()
            .put("loan_id", loanId)
            .put("amount", amount)
            .put("payment_date", paymentDate)
            .put("payment_method", paymentMethod)
            .put("mobile_uuid", mobileUuid)
            .put("allocation_mode", allocationMode)
        targetInstallmentId?.let { payload.put("target_installment_id", it) }

        val json = request(
            path = "collector/payments",
            method = "POST",
            token = token,
            body = payload,
        )

        return parsePayment(json.getJSONObject("data"))
    }

    /**
     * Genera (o reusa) un documento legal del préstamo. `viaAdmin` decide el
     * prefijo de ruta: back-office (admin/) o cartera del cobrador (collector/).
     */
    fun generateLoanDocument(
        token: String,
        loanId: Long,
        documentType: String,
        viaAdmin: Boolean,
    ): LoanDocument {
        val prefix = if (viaAdmin) "admin" else "collector"
        val json = request(
            path = "$prefix/loans/$loanId/documents",
            method = "POST",
            token = token,
            body = JSONObject().put("document_type", documentType),
        )

        return parseLoanDocument(json.getJSONObject("data"))
    }

    /**
     * Contrato digital más reciente de un préstamo (o null si aún no se ha generado).
     * Requiere permiso legal.manage (ruta admin).
     */
    fun adminLoanContract(token: String, loanId: Long): ContractSummary? {
        val json = request(path = "admin/loans/$loanId/contract", method = "GET", token = token)
        val data = json.opt("data")
        return if (data is JSONObject) parseContract(data) else null
    }

    /** Genera un contrato digital para el préstamo y devuelve los enlaces de firma. */
    fun adminGenerateContract(token: String, loanId: Long, contractType: String = "loan_contract"): ContractSummary {
        val json = request(
            path = "admin/loans/$loanId/contract",
            method = "POST",
            token = token,
            body = JSONObject().put("contract_type", contractType),
        )

        return parseContract(json.getJSONObject("data"))
    }

    // --- Back-office / administrador ---

    fun adminRegisterPayment(
        token: String,
        loanId: Long,
        amount: Double,
        paymentDate: String,
        paymentMethod: String,
        mobileUuid: String,
        allocationMode: String = "auto",
        targetInstallmentId: Long? = null,
    ): PaymentReceipt {
        val payload = JSONObject()
            .put("loan_id", loanId)
            .put("amount", amount)
            .put("payment_date", paymentDate)
            .put("payment_method", paymentMethod)
            .put("mobile_uuid", mobileUuid)
            .put("allocation_mode", allocationMode)
        targetInstallmentId?.let { payload.put("target_installment_id", it) }

        val json = request(
            path = "admin/payments",
            method = "POST",
            token = token,
            body = payload,
        )

        return parsePayment(json.getJSONObject("data"))
    }

    fun adminCollectors(token: String): List<CollectorOption> {
        val json = request(path = "admin/collectors", method = "GET", token = token)
        return json.getJSONArray("data").mapObjects { obj ->
            CollectorOption(id = obj.getLong("id"), name = obj.getString("name"))
        }
    }

    fun adminCreateLoan(token: String, input: NewLoanInput): LoanSummary {
        val payload = JSONObject()
            .put("client_id", input.clientId)
            .put("currency", input.currency)
            .put("late_fee_type", input.lateFeeType)
            .put("start_date", input.startDate)
            .put("first_payment_date", input.firstPaymentDate)

        if (input.quoteId != null) {
            payload.put("quote_id", input.quoteId)
        } else {
            payload.put("principal_amount", input.principalAmount)
            payload.put("interest_rate", input.interestRate)
            payload.put("interest_type", input.interestType)
            payload.put("payment_frequency", input.paymentFrequency)
            payload.put("calculation_method", input.calculationMethod)
            payload.put("term_quantity", input.termQuantity)
        }

        input.collectorId?.let { payload.put("collector_id", it) }
        input.lateFeeValue?.let { payload.put("late_fee_value", it) }
        input.notes?.takeIf { it.isNotBlank() }?.let { payload.put("notes", it) }

        val json = request(path = "admin/loans", method = "POST", token = token, body = payload)
        return parseLoan(json.getJSONObject("data"))
    }

    fun adminUpdateClient(token: String, clientId: Long, input: UpdateClientInput): ClientDetail {
        val payload = JSONObject()
            .put("full_name", input.fullName)
            .put("address", input.address)
            .put("status", input.status)
            .put("risk_level", input.riskLevel)
        input.identification?.let { payload.put("identification", it) }
        input.phone?.let { payload.put("phone", it) }
        input.secondaryPhone?.let { payload.put("secondary_phone", it) }
        input.email?.let { payload.put("email", it) }
        input.locationReference?.let { payload.put("location_reference", it) }
        input.latitude?.let { payload.put("latitude", it) }
        input.longitude?.let { payload.put("longitude", it) }
        input.workplace?.let { payload.put("workplace", it) }
        input.workplacePhone?.let { payload.put("workplace_phone", it) }
        input.monthlyIncome?.let { payload.put("monthly_income", it) }
        input.notes?.let { payload.put("notes", it) }

        val json = request(path = "admin/clients/$clientId", method = "PUT", token = token, body = payload)
        return parseClientDetail(json.getJSONObject("data"))
    }

    fun adminDeleteClient(token: String, clientId: Long) {
        request(path = "admin/clients/$clientId", method = "DELETE", token = token)
    }

    fun adminDeleteLoan(token: String, loanId: Long) {
        request(path = "admin/loans/$loanId", method = "DELETE", token = token)
    }

    fun adminPayments(token: String): List<PaymentReceipt> {
        val json = request(path = "admin/payments", method = "GET", token = token)
        return json.optJSONArray("data").mapObjects(::parsePayment)
    }

    fun adminPayment(token: String, paymentId: Long): PaymentReceipt {
        val json = request(path = "admin/payments/$paymentId", method = "GET", token = token)
        return parsePayment(json.getJSONObject("data"))
    }

    fun adminCancelPayment(token: String, paymentId: Long, reason: String): PaymentReceipt {
        val payload = JSONObject().put("cancellation_reason", reason)
        val json = request(path = "admin/payments/$paymentId/cancel", method = "POST", token = token, body = payload)
        return parsePayment(json.getJSONObject("data"))
    }

    fun adminCreateCollector(token: String, input: NewCollectorInput): CollectorDetail {
        val payload = JSONObject()
            .put("name", input.name)
            .put("commission_type", input.commissionType)
            .put("commission_base", input.commissionBase)
            .put("status", input.status)
            .put("access_mode", "none")
        input.phone?.let { payload.put("phone", it) }
        input.commissionValue?.let { payload.put("commission_value", it) }

        val json = request(path = "admin/collectors", method = "POST", token = token, body = payload)
        return parseCollectorDetail(json.getJSONObject("data"))
    }

    fun adminCollectorDetail(token: String, collectorId: Long): CollectorDetail {
        val json = request(path = "admin/collectors/$collectorId", method = "GET", token = token)
        return parseCollectorDetail(json.getJSONObject("data"))
    }

    fun adminUpdateCollector(token: String, collectorId: Long, input: UpdateCollectorInput): CollectorDetail {
        val payload = JSONObject()
            .put("name", input.name)
            .put("commission_type", input.commissionType)
            .put("commission_base", input.commissionBase)
            .put("status", input.status)
        input.phone?.let { payload.put("phone", it) }
        input.commissionValue?.let { payload.put("commission_value", it) }

        val json = request(path = "admin/collectors/$collectorId", method = "PUT", token = token, body = payload)
        return parseCollectorDetail(json.getJSONObject("data"))
    }

    fun adminPayCommission(token: String, collectorId: Long, commissionId: Long): CollectorCommissionItem {
        val json = request(path = "admin/collectors/$collectorId/commissions/$commissionId/pay", method = "POST", token = token)
        return parseCommission(json.getJSONObject("data"))
    }

    fun adminStoreMovement(token: String, input: CashMovementInput) {
        val payload = JSONObject()
            .put("type", input.type)
            .put("amount", input.amount)
            .put("movement_date", input.movementDate)
            .put("description", input.description)
        input.direction?.let { payload.put("direction", it) }

        request(path = "admin/cash/movements", method = "POST", token = token, body = payload)
    }

    private fun parseCollectorDetail(json: JSONObject): CollectorDetail {
        val summary = json.getJSONObject("commission_summary")
        val stats = json.optJSONObject("stats")
        return CollectorDetail(
            id = json.getLong("id"),
            name = json.getString("name"),
            phone = json.nullableString("phone"),
            commissionType = json.optString("commission_type"),
            commissionBase = json.optString("commission_base", "payment_total"),
            commissionValue = json.optDouble("commission_value", 0.0),
            status = json.optString("status"),
            commissionSummary = CollectorCommissionSummary(
                totalGenerated = summary.optDouble("total_generated", 0.0),
                totalPending = summary.optDouble("total_pending", 0.0),
                totalPaid = summary.optDouble("total_paid", 0.0),
            ),
            pendingCommissions = json.optJSONArray("pending_commissions").mapObjects(::parseCommission),
            stats = if (stats != null) CollectorStats(
                activeLoans = stats.optInt("active_loans", 0),
                lateLoans = stats.optInt("late_loans", 0),
            ) else CollectorStats(0, 0),
        )
    }

    private fun parseCommission(json: JSONObject): CollectorCommissionItem {
        return CollectorCommissionItem(
            id = json.getLong("id"),
            commissionType = json.optString("commission_type"),
            commissionValue = json.optDouble("commission_value", 0.0),
            baseAmount = json.optDouble("base_amount", 0.0),
            commissionAmount = json.optDouble("commission_amount", 0.0),
            status = json.optString("status"),
            paidAt = json.nullableString("paid_at"),
            receiptNumber = json.nullableString("receipt_number"),
        )
    }

    fun adminUpdateLoan(token: String, loanId: Long, input: UpdateLoanInput): LoanDetail {
        val payload = JSONObject()
            .put("currency", input.currency)
            .put("allows_capital_prepayment", input.allowsCapitalPrepayment)

        if (input.collectorId != null) payload.put("collector_id", input.collectorId) else payload.put("collector_id", JSONObject.NULL)
        if (input.guaranteeDescription != null) payload.put("guarantee_description", input.guaranteeDescription) else payload.put("guarantee_description", JSONObject.NULL)
        if (input.notes != null) payload.put("notes", input.notes) else payload.put("notes", JSONObject.NULL)

        input.principalAmount?.let { payload.put("principal_amount", it) }
        input.interestRate?.let { payload.put("interest_rate", it) }
        input.interestType?.let { payload.put("interest_type", it) }
        input.paymentFrequency?.let { payload.put("payment_frequency", it) }
        input.calculationMethod?.let { payload.put("calculation_method", it) }
        input.termQuantity?.let { payload.put("term_quantity", it) }
        input.lateFeeType?.let { payload.put("late_fee_type", it) }
        input.lateFeeValue?.let { payload.put("late_fee_value", it) }
        input.startDate?.let { payload.put("start_date", it) }
        input.firstPaymentDate?.let { payload.put("first_payment_date", it) }

        val json = request(path = "admin/loans/$loanId", method = "PUT", token = token, body = payload)
        return parseLoanDetail(json.getJSONObject("data"))
    }

    fun adminCreateRegistrationLink(token: String, recipientName: String?, recipientPhone: String?): ClientRegistrationLink {
        val payload = JSONObject()
        if (!recipientName.isNullOrBlank()) payload.put("recipient_name", recipientName.trim())
        if (!recipientPhone.isNullOrBlank()) payload.put("recipient_phone", recipientPhone.trim())

        val json = request(path = "admin/registration-links", method = "POST", token = token, body = payload)
        val data = json.getJSONObject("data")
        return ClientRegistrationLink(
            formUrl = data.getString("form_url"),
            whatsappUrl = data.nullableString("whatsapp_url"),
        )
    }

    fun adminCreateClient(token: String, input: NewClientInput): ClientSummary {
        val payload = JSONObject()
            .put("full_name", input.fullName)
            .put("address", input.address)
            .put("status", input.status)
            .put("risk_level", input.riskLevel)

        input.identification?.takeIf { it.isNotBlank() }?.let { payload.put("identification", it) }
        input.phone?.takeIf { it.isNotBlank() }?.let { payload.put("phone", it) }
        input.secondaryPhone?.takeIf { it.isNotBlank() }?.let { payload.put("secondary_phone", it) }
        input.email?.takeIf { it.isNotBlank() }?.let { payload.put("email", it) }
        input.locationReference?.takeIf { it.isNotBlank() }?.let { payload.put("location_reference", it) }
        input.latitude?.let { payload.put("latitude", it) }
        input.longitude?.let { payload.put("longitude", it) }
        input.workplace?.takeIf { it.isNotBlank() }?.let { payload.put("workplace", it) }
        input.workplacePhone?.takeIf { it.isNotBlank() }?.let { payload.put("workplace_phone", it) }
        input.workplaceAddress?.takeIf { it.isNotBlank() }?.let { payload.put("workplace_address", it) }
        input.monthlyIncome?.let { payload.put("monthly_income", it) }
        input.notes?.takeIf { it.isNotBlank() }?.let { payload.put("notes", it) }

        val json = request(path = "admin/clients", method = "POST", token = token, body = payload)
        return parseClient(json.getJSONObject("data"))
    }

    fun adminQuotes(token: String): List<LoanQuote> {
        val json = request(path = "admin/quotes?per_page=50", method = "GET", token = token)
        return json.optJSONArray("data").mapObjects(::parseQuote)
    }

    fun adminCreateQuote(
        token: String,
        clientId: Long?,
        amount: Double,
        interestRate: Double,
        interestType: String,
        paymentFrequency: String,
        calculationMethod: String,
        termQuantity: Int,
    ): LoanQuote {
        val payload = JSONObject()
            .put("amount", amount)
            .put("interest_rate", interestRate)
            .put("interest_type", interestType)
            .put("payment_frequency", paymentFrequency)
            .put("calculation_method", calculationMethod)
            .put("term_quantity", termQuantity)

        clientId?.let { payload.put("client_id", it) }

        val json = request(path = "admin/quotes", method = "POST", token = token, body = payload)
        return parseQuote(json.getJSONObject("data"))
    }

    fun adminQuote(token: String, quoteId: Long): LoanQuote {
        val json = request(path = "admin/quotes/$quoteId", method = "GET", token = token)
        return parseQuote(json.getJSONObject("data"))
    }

    fun adminDeleteQuote(token: String, quoteId: Long) {
        request(path = "admin/quotes/$quoteId", method = "DELETE", token = token)
    }

    private fun parseQuote(json: JSONObject): LoanQuote {
        val client = json.optJSONObject("client")

        return LoanQuote(
            id = json.getLong("id"),
            clientId = client?.optLong("id"),
            clientName = client?.nullableString("full_name"),
            amount = json.optDouble("amount", 0.0),
            interestRate = json.optDouble("interest_rate", 0.0),
            interestType = json.optString("interest_type"),
            paymentFrequency = json.optString("payment_frequency"),
            calculationMethod = json.optString("calculation_method"),
            termQuantity = json.optInt("term_quantity", 0),
            status = json.optString("status"),
            startDate = json.nullableString("start_date"),
            firstPaymentDate = json.nullableString("first_payment_date"),
            createdAt = json.nullableString("created_at"),
            installmentAmount = json.optDouble("installment_amount", 0.0),
            totalInterest = json.optDouble("total_interest", 0.0),
            totalAmount = json.optDouble("total_amount", 0.0),
            installments = json.optJSONArray("installments").mapObjects { item ->
                QuoteInstallment(
                    number = item.optInt("number", 0),
                    principal = item.optDouble("principal", 0.0),
                    interest = item.optDouble("interest", 0.0),
                    amount = item.optDouble("amount", 0.0),
                )
            },
        )
    }

    fun adminClients(token: String, search: String?): List<ClientSummary> {
        val query = buildString {
            append("per_page=100")
            search?.takeIf { it.isNotBlank() }?.let { append("&search=").append(it.urlEncode()) }
        }
        val json = request(path = "admin/clients?$query", method = "GET", token = token)
        return json.getJSONArray("data").mapObjects(::parseClient)
    }

    fun adminClient(token: String, clientId: Long): ClientDetail {
        val json = request(path = "admin/clients/$clientId", method = "GET", token = token)
        return parseClientDetail(json.getJSONObject("data"))
    }

    fun adminLoans(token: String, status: String?, search: String?): List<LoanSummary> {
        val query = buildString {
            append("per_page=100")
            status?.takeIf { it.isNotBlank() }?.let { append("&status=").append(it.urlEncode()) }
            search?.takeIf { it.isNotBlank() }?.let { append("&search=").append(it.urlEncode()) }
        }
        val json = request(path = "admin/loans?$query", method = "GET", token = token)
        return json.optJSONArray("data").mapObjects(::parseLoan)
    }

    /** Variante paginada de la cartera de préstamos para carga incremental ("cargar más"). */
    fun adminLoansPage(
        token: String,
        page: Int,
        status: String? = null,
        search: String? = null,
        perPage: Int = 50,
    ): Page<LoanSummary> {
        val query = buildString {
            append("per_page=").append(perPage)
            append("&page=").append(page)
            status?.takeIf { it.isNotBlank() }?.let { append("&status=").append(it.urlEncode()) }
            search?.takeIf { it.isNotBlank() }?.let { append("&search=").append(it.urlEncode()) }
        }
        val json = request(path = "admin/loans?$query", method = "GET", token = token)
        val meta = json.optJSONObject("meta")
        return Page(
            items = json.optJSONArray("data").mapObjects(::parseLoan),
            currentPage = meta?.optInt("current_page", page) ?: page,
            lastPage = meta?.optInt("last_page", page) ?: page,
        )
    }

    fun adminLoan(token: String, loanId: Long): LoanDetail {
        val json = request(path = "admin/loans/$loanId", method = "GET", token = token)
        return parseLoanDetail(json.getJSONObject("data"))
    }

    fun adminApprovals(token: String): List<LoanSummary> {
        val json = request(path = "admin/approvals", method = "GET", token = token)
        return json.getJSONArray("data").mapObjects(::parseLoan)
    }

    fun adminApproveLoan(token: String, loanId: Long): LoanSummary {
        val json = request(path = "admin/loans/$loanId/approve", method = "POST", token = token)
        return parseLoan(json.getJSONObject("data"))
    }

    fun adminRejectLoan(token: String, loanId: Long, reason: String?): LoanSummary {
        val payload = JSONObject()
        reason?.takeIf { it.isNotBlank() }?.let { payload.put("reason", it) }
        val json = request(path = "admin/loans/$loanId/reject", method = "POST", token = token, body = payload)
        return parseLoan(json.getJSONObject("data"))
    }

    fun adminReportSummary(token: String, dateFrom: String?, dateTo: String?): AdminReportSummary {
        val json = request(path = "admin/reports/summary?${rangeQuery(dateFrom, dateTo)}", method = "GET", token = token)
        val data = json.getJSONObject("data")
        val totals = data.getJSONObject("totals")
        val clients = data.optJSONObject("clients") ?: JSONObject()

        return AdminReportSummary(
            capitalInvested = totals.optDouble("capital_invested", 0.0),
            capitalOnStreet = totals.optDouble("capital_on_street", 0.0),
            capitalRecovered = totals.optDouble("capital_recovered", 0.0),
            interestEarned = totals.optDouble("interest_earned", 0.0),
            lateFeeEarned = totals.optDouble("late_fee_earned", 0.0),
            expenses = totals.optDouble("expenses", 0.0),
            newDisbursed = totals.optDouble("new_disbursed", 0.0),
            netBalance = totals.optDouble("net_balance", 0.0),
            roi = totals.optDouble("roi", 0.0),
            monthlyReturn = totals.optDouble("monthly_return", 0.0),
            activeClients = clients.optInt("active", 0),
            inactiveClients = clients.optInt("inactive", 0),
            overdueClients = clients.optInt("overdue", 0),
        )
    }

    fun adminReportCollectors(token: String, dateFrom: String?, dateTo: String?): List<CollectorPerformanceRow> {
        val json = request(path = "admin/reports/collectors?${rangeQuery(dateFrom, dateTo)}", method = "GET", token = token)
        return json.getJSONObject("data").optJSONArray("rows").mapObjects { row ->
            CollectorPerformanceRow(
                collector = row.optString("collector"),
                capital = row.optDouble("capital", 0.0),
                interest = row.optDouble("interest", 0.0),
                lateFee = row.optDouble("late_fee", 0.0),
                collected = row.optDouble("collected", 0.0),
                disbursed = row.optDouble("disbursed", 0.0),
                activeAccounts = row.optInt("active_accounts", 0),
                overdueAccounts = row.optInt("overdue_accounts", 0),
            )
        }
    }

    fun adminReportCatalog(token: String): List<com.sistemaprestamista.mobile.data.model.ReportCatalogItem> {
        val json = request(path = "admin/reports/catalog", method = "GET", token = token)
        return json.optJSONArray("data").mapObjects { item ->
            com.sistemaprestamista.mobile.data.model.ReportCatalogItem(
                type = item.optString("type"),
                title = item.optString("title"),
                description = item.optString("description"),
                pdfUrl = item.optString("pdf_url"),
            )
        }
    }

    // --- Caja / Contabilidad ---

    fun cashboxExpenses(token: String): List<com.sistemaprestamista.mobile.data.model.ExpenseItem> {
        val json = request(path = "cashbox/expenses", method = "GET", token = token)
        return json.getJSONArray("data").mapObjects(::parseExpense)
    }

    fun cashboxCategories(token: String): List<com.sistemaprestamista.mobile.data.model.ExpenseCategoryOption> {
        val json = request(path = "cashbox/expense-categories", method = "GET", token = token)
        return json.getJSONArray("data").mapObjects { c ->
            com.sistemaprestamista.mobile.data.model.ExpenseCategoryOption(
                id = c.getLong("id"),
                name = c.getString("name"),
            )
        }
    }

    fun cashboxCreateExpense(
        token: String,
        categoryId: Long?,
        description: String,
        amount: Double,
        expenseDate: String,
        paymentMethod: String,
    ): com.sistemaprestamista.mobile.data.model.ExpenseItem {
        val payload = JSONObject()
            .put("description", description)
            .put("amount", amount)
            .put("expense_date", expenseDate)
            .put("payment_method", paymentMethod)
        if (categoryId != null) {
            payload.put("category_id", categoryId)
        }
        val json = request(path = "cashbox/expenses", method = "POST", token = token, body = payload)
        return parseExpense(json.getJSONObject("data"))
    }

    fun cashboxMovements(token: String): List<com.sistemaprestamista.mobile.data.model.CashMovementItem> {
        val json = request(path = "cashbox/movements", method = "GET", token = token)
        return json.getJSONArray("data").mapObjects { m ->
            com.sistemaprestamista.mobile.data.model.CashMovementItem(
                id = m.getLong("id"),
                type = m.optString("type"),
                amount = m.optDouble("amount", 0.0),
                direction = m.optString("direction"),
                description = m.nullableString("description"),
                date = m.nullableString("date"),
            )
        }
    }

    fun cashboxSummary(token: String): com.sistemaprestamista.mobile.data.model.CashSummary {
        val json = request(path = "cashbox/summary", method = "GET", token = token)
        val data = json.getJSONObject("data")
        return com.sistemaprestamista.mobile.data.model.CashSummary(
            totalIn = data.optDouble("total_in", 0.0),
            totalOut = data.optDouble("total_out", 0.0),
            balance = data.optDouble("balance", 0.0),
        )
    }

    private fun parseExpense(json: JSONObject): com.sistemaprestamista.mobile.data.model.ExpenseItem {
        return com.sistemaprestamista.mobile.data.model.ExpenseItem(
            id = json.getLong("id"),
            date = json.nullableString("date"),
            category = json.nullableString("category"),
            categoryId = json.nullableLong("category_id"),
            description = json.optString("description"),
            amount = json.optDouble("amount", 0.0),
            paymentMethod = json.optString("payment_method"),
        )
    }

    private fun rangeQuery(dateFrom: String?, dateTo: String?): String {
        return buildList {
            dateFrom?.takeIf { it.isNotBlank() }?.let { add("date_from" to it) }
            dateTo?.takeIf { it.isNotBlank() }?.let { add("date_to" to it) }
        }.joinToString("&") { (key, value) -> "${key.urlEncode()}=${value.urlEncode()}" }
    }

    /** Borra la caché de respuestas (al cerrar sesión). */
    fun clearCache() {
        responseCache?.clear()
    }

    private fun request(
        path: String,
        method: String,
        token: String?,
        body: JSONObject? = null,
        cacheOnly: Boolean = false,
    ): JSONObject {
        // Las respuestas GET se cachean por ruta para soportar lectura offline y pintado instantáneo.
        val cacheKey = if (method == "GET") path else null

        // Modo solo-caché: la UI pinta lo guardado sin tocar la red (apertura instantánea).
        if (cacheOnly) {
            val cached = cacheKey?.let { responseCache?.read(it) }
            return if (cached.isNullOrBlank()) JSONObject() else JSONObject(cached)
        }

        val requestBuilder = Request.Builder()
            .url(BuildConfig.API_BASE_URL + path)
            .header("Accept", "application/json")

        if (token != null) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        val request = when (method) {
            "GET" -> requestBuilder.get().build()
            "POST" -> requestBuilder.post((body ?: JSONObject()).toString().toRequestBody(jsonMediaType)).build()
            "DELETE" -> requestBuilder.delete().build()
            else -> error("Unsupported HTTP method $method")
        }

        try {
            client.newCall(request).execute().use { response ->
                val rawBody = response.body.string()
                val json = if (rawBody.isBlank()) JSONObject() else JSONObject(rawBody)

                if (!response.isSuccessful) {
                    throw ApiException(
                        message = json.optString("message", "Error de comunicación con el servidor."),
                        statusCode = response.code,
                    )
                }

                if (cacheKey != null) {
                    responseCache?.write(cacheKey, rawBody)
                }

                return json
            }
        } catch (exception: IOException) {
            // Sin conexión / timeout: si hay respuesta en caché para este GET, se sirve.
            if (cacheKey != null) {
                val cached = responseCache?.read(cacheKey)
                if (!cached.isNullOrBlank()) {
                    return JSONObject(cached)
                }
            }
            throw exception
        }
    }

    private fun parseUser(json: JSONObject): UserProfile {
        val company = json.getJSONObject("company")

        return UserProfile(
            id = json.getLong("id"),
            name = json.getString("name"),
            email = json.getString("email"),
            roles = json.optJSONArray("roles").toStringList(),
            permissions = json.optJSONArray("permissions").toStringList(),
            isCollector = json.optBoolean("is_collector", false),
            company = Company(
                id = company.optLong("id"),
                name = company.optString("name"),
                status = company.optString("status"),
                defaultCurrency = company.optString("default_currency", "RD$").ifBlank { "RD$" },
            ),
        )
    }

    private fun parseClient(json: JSONObject): ClientSummary {
        return ClientSummary(
            id = json.getLong("id"),
            fullName = json.getString("full_name"),
            identification = json.nullableString("identification"),
            phone = json.nullableString("phone"),
            address = json.nullableString("address"),
            latitude = json.nullableDouble("latitude"),
            longitude = json.nullableDouble("longitude"),
            locationReference = json.nullableString("location_reference"),
            status = json.optString("status"),
            riskLevel = json.optString("risk_level"),
        )
    }

    private fun parseMapClient(json: JSONObject): MapClient {
        return MapClient(
            summary = parseClient(json),
            financialSummary = parseFinancialSummary(json.getJSONObject("summary")),
            routes = json.optJSONArray("routes").mapObjects { route ->
                ClientRouteMapSummary(
                    id = route.getLong("id"),
                    name = route.getString("name"),
                    orderNumber = route.optInt("order_number"),
                )
            },
        )
    }

    private fun parseRoute(json: JSONObject): CollectorRoute {
        val zone = json.optJSONObject("zone")

        return CollectorRoute(
            id = json.getLong("id"),
            name = json.getString("name"),
            description = json.nullableString("description"),
            zoneName = zone?.nullableString("name"),
            clientsCount = json.optInt("clients_count", 0),
            clients = json.optJSONArray("clients").mapObjects { client ->
                RouteClientStop(
                    summary = parseClient(client),
                    orderNumber = client.optInt("order_number"),
                    financialSummary = parseFinancialSummary(client.getJSONObject("summary")),
                )
            },
        )
    }

    private fun parseRouteSession(json: JSONObject): CollectorRouteSession {
        val collector = json.optJSONObject("collector")
        val route = json.optJSONObject("route")

        return CollectorRouteSession(
            id = json.getLong("id"),
            status = json.optString("status"),
            startedAt = json.nullableString("started_at"),
            endedAt = json.nullableString("ended_at"),
            lastLocationAt = json.nullableString("last_location_at"),
            lastLatitude = json.nullableDouble("last_latitude"),
            lastLongitude = json.nullableDouble("last_longitude"),
            collectorName = collector?.nullableString("name"),
            routeId = route?.nullableLong("id"),
            routeName = route?.nullableString("name"),
            stops = json.optJSONArray("stops").mapObjects { stop ->
                RouteTrackingStop(
                    clientId = stop.getLong("client_id"),
                    clientName = stop.getString("client_name"),
                    address = stop.nullableString("address"),
                    latitude = stop.nullableDouble("latitude"),
                    longitude = stop.nullableDouble("longitude"),
                    expectedOrder = stop.optInt("expected_order"),
                    visited = stop.optBoolean("visited", false),
                    visitedOrder = stop.nullableInt("visited_order"),
                    visitedAt = stop.nullableString("visited_at"),
                    visitStatus = stop.nullableString("visit_status"),
                    distanceMeters = stop.nullableInt("distance_meters"),
                )
            },
        )
    }

    private fun parseClientDetail(json: JSONObject): ClientDetail {
        val financial = json.getJSONObject("summary")

        return ClientDetail(
            summary = parseClient(json),
            secondaryPhone = json.nullableString("secondary_phone"),
            email = json.nullableString("email"),
            workplace = json.nullableString("workplace"),
            workplacePhone = json.nullableString("workplace_phone"),
            monthlyIncome = json.optDouble("monthly_income", 0.0),
            notes = json.nullableString("notes"),
            references = json.optJSONArray("references").mapObjects { reference ->
                ClientReference(
                    id = reference.getLong("id"),
                    name = reference.getString("name"),
                    phone = reference.getString("phone"),
                    relationship = reference.nullableString("relationship"),
                    address = reference.nullableString("address"),
                )
            },
            routes = json.optJSONArray("routes").mapObjects { route ->
                ClientRouteSummary(
                    id = route.getLong("id"),
                    name = route.getString("name"),
                )
            },
            financialSummary = parseFinancialSummary(financial),
            loans = json.optJSONArray("loans").mapObjects(::parseLoan),
            pendingInstallments = json.optJSONArray("pending_installments").mapObjects(::parseInstallment),
            recentPayments = json.optJSONArray("recent_payments").mapObjects(::parsePayment),
        )
    }

    private fun parseLoan(json: JSONObject): LoanSummary {
        return LoanSummary(
            id = json.getLong("id"),
            loanNumber = json.getString("loan_number"),
            client = json.optJSONObject("client")?.let(::parseClient),
            principalAmount = json.optDouble("principal_amount", 0.0),
            installmentAmount = json.optDouble("installment_amount", 0.0),
            totalAmount = json.optDouble("total_amount", 0.0),
            remainingBalance = json.optDouble("remaining_balance", 0.0),
            paymentFrequency = json.optString("payment_frequency"),
            status = json.optString("status"),
        )
    }

    private fun parseLoanDetail(json: JSONObject): LoanDetail {
        val financial = json.getJSONObject("summary")

        return LoanDetail(
            summary = parseLoan(json),
            collectorId = if (json.has("collector_id") && !json.isNull("collector_id")) json.getLong("collector_id") else null,
            collectorName = json.nullableString("collector_name"),
            currency = json.optString("currency", "RD$"),
            allowsCapitalPrepayment = json.optBoolean("allows_capital_prepayment", false),
            interestRate = json.optDouble("interest_rate", 0.0),
            interestType = json.optString("interest_type"),
            calculationMethod = json.optString("calculation_method"),
            termQuantity = json.optInt("term_quantity", 0),
            totalInterest = json.optDouble("total_interest", 0.0),
            paidPrincipal = json.optDouble("paid_principal", 0.0),
            paidInterest = json.optDouble("paid_interest", 0.0),
            paidLateFee = json.optDouble("paid_late_fee", 0.0),
            startDate = json.nullableString("start_date"),
            firstPaymentDate = json.nullableString("first_payment_date"),
            endDate = json.nullableString("end_date"),
            lateFeeType = json.optString("late_fee_type"),
            lateFeeValue = json.optDouble("late_fee_value", 0.0),
            guaranteeDescription = json.nullableString("guarantee_description"),
            notes = json.nullableString("notes"),
            financialSummary = LoanFinancialSummary(
                installmentsTotal = financial.optInt("installments_total", 0),
                installmentsPending = financial.optInt("installments_pending", 0),
                installmentsLate = financial.optInt("installments_late", 0),
                paymentsTotal = financial.optInt("payments_total", 0),
                amountPaid = financial.optDouble("amount_paid", 0.0),
                overdueInstallmentsCount = financial.optInt("overdue_installments_count", 0),
                overdueInstallmentsTotal = financial.optDouble("overdue_installments_total", 0.0),
                overdueLateFeeTotal = financial.optDouble("overdue_late_fee_total", 0.0),
                totalDueToday = financial.optDouble("total_due_today", 0.0),
            ),
            installments = json.optJSONArray("installments").mapObjects(::parseInstallment),
            payments = json.optJSONArray("payments").mapObjects(::parsePayment),
            documents = json.optJSONArray("documents").mapObjects(::parseLoanDocument),
        )
    }

    private fun parseLoanDocument(json: JSONObject): LoanDocument {
        return LoanDocument(
            documentType = json.optString("document_type"),
            label = json.optString("label"),
            generated = json.optBoolean("generated", false),
            documentId = if (json.has("document_id") && !json.isNull("document_id")) json.getLong("document_id") else null,
            title = json.nullableString("title"),
            downloadUrl = json.nullableString("download_url"),
            createdAt = json.nullableString("created_at"),
        )
    }

    private fun parseContract(json: JSONObject): ContractSummary {
        return ContractSummary(
            uuid = json.getString("uuid"),
            contractNumber = json.optString("contract_number"),
            status = json.optString("status"),
            version = json.optInt("version", 1),
            signedAt = json.nullableString("signed_at"),
            signingUrl = json.nullableString("signing_url"),
            whatsappUrl = json.nullableString("whatsapp_url"),
            verifyUrl = json.nullableString("verify_url"),
        )
    }

    private fun parseInstallment(json: JSONObject): InstallmentSummary {
        return InstallmentSummary(
            id = json.getLong("id"),
            loanId = json.getLong("loan_id"),
            loanNumber = json.getString("loan_number"),
            client = json.optJSONObject("client")?.let(::parseClient),
            installmentNumber = json.optInt("installment_number"),
            dueDate = json.nullableString("due_date"),
            principalAmount = json.optDouble("principal_amount", 0.0),
            interestAmount = json.optDouble("interest_amount", 0.0),
            lateFee = json.optDouble("late_fee", 0.0),
            installmentAmount = json.optDouble("installment_amount", 0.0),
            totalPaid = json.optDouble("total_paid", 0.0),
            paidPrincipal = json.optDouble("paid_principal", 0.0),
            paidInterest = json.optDouble("paid_interest", 0.0),
            paidLateFee = json.optDouble("paid_late_fee", 0.0),
            daysLate = json.optInt("days_late", 0),
            status = json.optString("status"),
        )
    }

    private fun parseInstallmentDetail(json: JSONObject): InstallmentDetail {
        return InstallmentDetail(
            summary = parseInstallment(json),
            payments = json.optJSONArray("payments").mapObjects { payment ->
                InstallmentPaymentLine(
                    id = payment.getLong("id"),
                    paymentId = payment.getLong("payment_id"),
                    receiptNumber = payment.nullableString("receipt_number"),
                    paymentDate = payment.nullableString("payment_date"),
                    paymentMethod = payment.nullableString("payment_method"),
                    paymentStatus = payment.nullableString("payment_status"),
                    principalPaid = payment.optDouble("principal_paid", 0.0),
                    interestPaid = payment.optDouble("interest_paid", 0.0),
                    lateFeePaid = payment.optDouble("late_fee_paid", 0.0),
                    amountPaid = payment.optDouble("amount_paid", 0.0),
                )
            },
        )
    }

    private fun parsePayment(json: JSONObject): PaymentReceipt {
        return PaymentReceipt(
            id = json.getLong("id"),
            receiptNumber = json.getString("receipt_number"),
            loanId = json.getLong("loan_id"),
            loanNumber = json.nullableString("loan_number"),
            currency = json.optString("currency", "RD$").ifBlank { "RD$" },
            client = json.optJSONObject("client")?.let(::parseClient),
            paymentDate = json.nullableString("payment_date"),
            amount = json.optDouble("amount", 0.0),
            principalPaid = json.optDouble("principal_paid", 0.0),
            interestPaid = json.optDouble("interest_paid", 0.0),
            lateFeePaid = json.optDouble("late_fee_paid", 0.0),
            previousBalance = json.optDouble("previous_balance", 0.0),
            newBalance = json.optDouble("new_balance", 0.0),
            paymentMethod = json.optString("payment_method"),
            status = json.optString("status"),
            details = json.optJSONArray("details").mapObjects { detail ->
                PaymentDetailLine(
                    id = detail.getLong("id"),
                    installmentId = detail.getLong("installment_id"),
                    installmentNumber = if (detail.isNull("installment_number")) null else detail.optInt("installment_number"),
                    principalPaid = detail.optDouble("principal_paid", 0.0),
                    interestPaid = detail.optDouble("interest_paid", 0.0),
                    lateFeePaid = detail.optDouble("late_fee_paid", 0.0),
                    amountPaid = detail.optDouble("amount_paid", 0.0),
                )
            },
            commission = json.optJSONObject("commission")?.let(::parsePaymentCommission),
            whatsappUrl = json.nullableString("whatsapp_url"),
            receiptUrl = json.nullableString("receipt_url"),
        )
    }

    private fun parsePaymentCommission(json: JSONObject): PaymentCommission {
        return PaymentCommission(
            id = json.getLong("id"),
            commissionType = json.optString("commission_type"),
            commissionValue = json.optDouble("commission_value", 0.0),
            baseAmount = json.optDouble("base_amount", 0.0),
            commissionAmount = json.optDouble("commission_amount", 0.0),
            status = json.optString("status"),
            paidAt = json.nullableString("paid_at"),
        )
    }

    private fun parseFinancialSummary(json: JSONObject): ClientFinancialSummary {
        return ClientFinancialSummary(
            activeLoans = json.optInt("active_loans", 0),
            lateLoans = json.optInt("late_loans", 0),
            totalPrincipal = json.optDouble("total_principal", 0.0),
            remainingBalance = json.optDouble("remaining_balance", 0.0),
            pendingPrincipal = json.optDouble("pending_principal", 0.0),
            pendingInterest = json.optDouble("pending_interest", 0.0),
            pendingInstallments = json.optInt("pending_installments", 0),
            lateInstallments = json.optInt("late_installments", 0),
            maxDaysLate = json.optInt("max_days_late", 0),
            totalPaid = json.optDouble("total_paid", 0.0),
            lastPaymentDate = json.nullableString("last_payment_date"),
        )
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()

        return buildList {
            for (index in 0 until length()) {
                add(optString(index))
            }
        }
    }

    private fun JSONObject.nullableString(name: String): String? {
        return if (isNull(name)) null else optString(name)
    }

    private fun JSONObject.nullableDouble(name: String): Double? {
        return if (isNull(name)) null else optDouble(name)
    }

    private fun JSONObject.nullableInt(name: String): Int? {
        return if (isNull(name)) null else optInt(name)
    }

    private fun JSONObject.nullableLong(name: String): Long? {
        return if (isNull(name)) null else optLong(name)
    }

    private fun PaymentHistoryFilters.toQueryString(): String {
        val params = buildList {
            add("per_page" to "100")
            clientId?.let { add("client_id" to it.toString()) }
            loanId?.let { add("loan_id" to it.toString()) }
            status?.takeIf { it.isNotBlank() }?.let { add("status" to it) }
            dateFrom?.takeIf { it.isNotBlank() }?.let { add("date_from" to it) }
            dateTo?.takeIf { it.isNotBlank() }?.let { add("date_to" to it) }
        }

        return params.joinToString("&") { (key, value) ->
            "${key.urlEncode()}=${value.urlEncode()}"
        }
    }

    private fun String.urlEncode(): String {
        return URLEncoder.encode(this, StandardCharsets.UTF_8.name())
    }

    private fun <T> JSONArray?.mapObjects(transform: (JSONObject) -> T): List<T> {
        if (this == null) return emptyList()

        return buildList {
            for (index in 0 until length()) {
                add(transform(getJSONObject(index)))
            }
        }
    }
}
