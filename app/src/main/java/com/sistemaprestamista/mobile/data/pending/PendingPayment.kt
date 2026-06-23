package com.sistemaprestamista.mobile.data.pending

data class PendingPayment(
    val id: Long,
    val loanId: Long,
    val amount: Double,
    val paymentDate: String,
    val paymentMethod: String,
    val allocationMode: String,
    val targetInstallmentId: Long?,
    val capitalPrepaymentAmount: Double?,
    val mobileUuid: String,
    val status: PendingPaymentStatus,
    val attempts: Int,
    val lastError: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

enum class PendingPaymentStatus(val storageValue: String) {
    Pending("pending"),
    Failed("failed"),
    Sent("sent");

    companion object {
        fun fromStorage(value: String?): PendingPaymentStatus {
            return entries.firstOrNull { it.storageValue == value } ?: Pending
        }
    }
}
