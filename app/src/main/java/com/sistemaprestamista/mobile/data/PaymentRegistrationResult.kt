package com.sistemaprestamista.mobile.data

import com.sistemaprestamista.mobile.data.model.PaymentReceipt

sealed interface PaymentRegistrationResult {
    data class Sent(val receipt: PaymentReceipt) : PaymentRegistrationResult
    data class Queued(val pendingCount: Int) : PaymentRegistrationResult
}
