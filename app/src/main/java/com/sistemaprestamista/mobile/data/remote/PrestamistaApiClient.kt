package com.sistemaprestamista.mobile.data.remote

import com.sistemaprestamista.mobile.BuildConfig
import com.sistemaprestamista.mobile.data.model.AdminReportSummary
import com.sistemaprestamista.mobile.data.model.CollectorPerformanceRow
import com.sistemaprestamista.mobile.data.model.Company
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
import com.sistemaprestamista.mobile.data.model.LoanFinancialSummary
import com.sistemaprestamista.mobile.data.model.LoanSummary
import com.sistemaprestamista.mobile.data.model.LoginResult
import com.sistemaprestamista.mobile.data.model.MapClient
import com.sistemaprestamista.mobile.data.model.PaymentDetailLine
import com.sistemaprestamista.mobile.data.model.PaymentHistoryFilters
import com.sistemaprestamista.mobile.data.model.PaymentReceipt
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
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

class PrestamistaApiClient {
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

    fun me(token: String): UserProfile {
        val json = request(path = "me", method = "GET", token = token)
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

    fun collectorSummary(token: String): CollectorSummary {
        val json = request(path = "collector/summary", method = "GET", token = token)
        val data = json.getJSONObject("data")
        val collector = data.getJSONObject("collector")

        return CollectorSummary(
            collectorId = collector.getLong("id"),
            collectorName = collector.getString("name"),
            assignedClients = data.optInt("assigned_clients", 0),
            activeLoans = data.optInt("active_loans", 0),
            lateLoans = data.optInt("late_loans", 0),
            pendingInstallments = data.optInt("pending_installments", 0),
            collectedToday = data.optDouble("collected_today", 0.0),
        )
    }

    fun collectorClients(token: String): List<ClientSummary> {
        val json = request(path = "collector/clients?per_page=100", method = "GET", token = token)
        return json.getJSONArray("data").mapObjects(::parseClient)
    }

    fun collectorMapClients(token: String): List<MapClient> {
        val json = request(path = "collector/map-clients", method = "GET", token = token)
        return json.getJSONArray("data").mapObjects(::parseMapClient)
    }

    fun collectorRoutes(token: String): List<CollectorRoute> {
        val json = request(path = "collector/routes", method = "GET", token = token)
        return json.getJSONArray("data").mapObjects(::parseRoute)
    }

    fun activeRouteSession(token: String): CollectorRouteSession? {
        val json = request(path = "collector/route-sessions/active", method = "GET", token = token)
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

    fun collectorLoans(token: String): List<LoanSummary> {
        val json = request(path = "collector/loans?per_page=100", method = "GET", token = token)
        return json.getJSONArray("data").mapObjects(::parseLoan)
    }

    fun collectorLoan(token: String, loanId: Long): LoanDetail {
        val json = request(path = "collector/loans/$loanId", method = "GET", token = token)
        return parseLoanDetail(json.getJSONObject("data"))
    }

    fun collectorInstallments(token: String): List<InstallmentSummary> {
        val json = request(path = "collector/installments?per_page=100", method = "GET", token = token)
        return json.getJSONArray("data").mapObjects(::parseInstallment)
    }

    fun collectorInstallment(token: String, installmentId: Long): InstallmentDetail {
        val json = request(path = "collector/installments/$installmentId", method = "GET", token = token)
        return parseInstallmentDetail(json.getJSONObject("data"))
    }

    fun collectorPayments(token: String, filters: PaymentHistoryFilters = PaymentHistoryFilters()): List<PaymentReceipt> {
        val json = request(path = "collector/payments?${filters.toQueryString()}", method = "GET", token = token)
        return json.getJSONArray("data").mapObjects(::parsePayment)
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
    ): PaymentReceipt {
        val payload = JSONObject()
            .put("loan_id", loanId)
            .put("amount", amount)
            .put("payment_date", paymentDate)
            .put("payment_method", paymentMethod)
            .put("mobile_uuid", mobileUuid)

        val json = request(
            path = "collector/payments",
            method = "POST",
            token = token,
            body = payload,
        )

        return parsePayment(json.getJSONObject("data"))
    }

    // --- Back-office / administrador ---

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
        return json.getJSONArray("data").mapObjects(::parseLoan)
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

    private fun request(
        path: String,
        method: String,
        token: String?,
        body: JSONObject? = null,
    ): JSONObject {
        val requestBuilder = Request.Builder()
            .url(BuildConfig.API_BASE_URL + path)
            .header("Accept", "application/json")

        if (token != null) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        val request = when (method) {
            "GET" -> requestBuilder.get().build()
            "POST" -> requestBuilder.post((body ?: JSONObject()).toString().toRequestBody(jsonMediaType)).build()
            else -> error("Unsupported HTTP method $method")
        }

        client.newCall(request).execute().use { response ->
            val rawBody = response.body.string()
            val json = if (rawBody.isBlank()) JSONObject() else JSONObject(rawBody)

            if (!response.isSuccessful) {
                throw ApiException(
                    message = json.optString("message", "Error de comunicación con el servidor."),
                    statusCode = response.code,
                )
            }

            return json
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
            ),
            installments = json.optJSONArray("installments").mapObjects(::parseInstallment),
            payments = json.optJSONArray("payments").mapObjects(::parsePayment),
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
        )
    }

    private fun parseFinancialSummary(json: JSONObject): ClientFinancialSummary {
        return ClientFinancialSummary(
            activeLoans = json.optInt("active_loans", 0),
            lateLoans = json.optInt("late_loans", 0),
            totalPrincipal = json.optDouble("total_principal", 0.0),
            remainingBalance = json.optDouble("remaining_balance", 0.0),
            pendingInstallments = json.optInt("pending_installments", 0),
            lateInstallments = json.optInt("late_installments", 0),
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
