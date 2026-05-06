package com.sistemaprestamista.mobile.data.remote

import com.sistemaprestamista.mobile.BuildConfig
import com.sistemaprestamista.mobile.data.model.Company
import com.sistemaprestamista.mobile.data.model.ClientDetail
import com.sistemaprestamista.mobile.data.model.ClientFinancialSummary
import com.sistemaprestamista.mobile.data.model.ClientReference
import com.sistemaprestamista.mobile.data.model.ClientRouteSummary
import com.sistemaprestamista.mobile.data.model.ClientSummary
import com.sistemaprestamista.mobile.data.model.CollectorSummary
import com.sistemaprestamista.mobile.data.model.DashboardSummary
import com.sistemaprestamista.mobile.data.model.InstallmentSummary
import com.sistemaprestamista.mobile.data.model.InstallmentDetail
import com.sistemaprestamista.mobile.data.model.InstallmentPaymentLine
import com.sistemaprestamista.mobile.data.model.LoanDetail
import com.sistemaprestamista.mobile.data.model.LoanFinancialSummary
import com.sistemaprestamista.mobile.data.model.LoanSummary
import com.sistemaprestamista.mobile.data.model.LoginResult
import com.sistemaprestamista.mobile.data.model.PaymentDetailLine
import com.sistemaprestamista.mobile.data.model.PaymentHistoryFilters
import com.sistemaprestamista.mobile.data.model.PaymentReceipt
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
            status = json.optString("status"),
            riskLevel = json.optString("risk_level"),
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
            financialSummary = ClientFinancialSummary(
                activeLoans = financial.optInt("active_loans", 0),
                lateLoans = financial.optInt("late_loans", 0),
                totalPrincipal = financial.optDouble("total_principal", 0.0),
                remainingBalance = financial.optDouble("remaining_balance", 0.0),
                pendingInstallments = financial.optInt("pending_installments", 0),
                lateInstallments = financial.optInt("late_installments", 0),
                totalPaid = financial.optDouble("total_paid", 0.0),
                lastPaymentDate = financial.nullableString("last_payment_date"),
            ),
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
