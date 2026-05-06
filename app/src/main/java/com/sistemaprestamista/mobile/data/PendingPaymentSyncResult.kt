package com.sistemaprestamista.mobile.data

data class PendingPaymentSyncResult(
    val sent: Int,
    val failed: Int,
    val remaining: Int,
    val requiresRetry: Boolean,
)
