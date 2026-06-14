package com.sistemaprestamista.mobile.data.pending

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class PendingPaymentStore(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION,
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            create table pending_payments (
                id integer primary key autoincrement,
                loan_id integer not null,
                amount real not null,
                payment_date text not null,
                payment_method text not null,
                allocation_mode text not null default 'auto',
                target_installment_id integer,
                mobile_uuid text not null unique,
                status text not null,
                attempts integer not null default 0,
                last_error text,
                created_at integer not null,
                updated_at integer not null
            )
            """.trimIndent(),
        )
        db.execSQL("create index pending_payments_status_index on pending_payments(status)")
        db.execSQL("create index pending_payments_updated_at_index on pending_payments(updated_at)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("alter table pending_payments add column allocation_mode text not null default 'auto'")
        }
        if (oldVersion < 3) {
            db.execSQL("alter table pending_payments add column target_installment_id integer")
        }
    }

    fun create(
        loanId: Long,
        amount: Double,
        paymentDate: String,
        paymentMethod: String,
        allocationMode: String,
        targetInstallmentId: Long?,
        mobileUuid: String,
    ): PendingPayment {
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put("loan_id", loanId)
            put("amount", amount)
            put("payment_date", paymentDate)
            put("payment_method", paymentMethod)
            put("allocation_mode", allocationMode)
            if (targetInstallmentId != null) put("target_installment_id", targetInstallmentId) else putNull("target_installment_id")
            put("mobile_uuid", mobileUuid)
            put("status", PendingPaymentStatus.Pending.storageValue)
            put("attempts", 0)
            putNull("last_error")
            put("created_at", now)
            put("updated_at", now)
        }

        writableDatabase.insertWithOnConflict(
            "pending_payments",
            null,
            values,
            SQLiteDatabase.CONFLICT_IGNORE,
        )

        return getByMobileUuid(mobileUuid) ?: error("No se pudo crear el cobro pendiente.")
    }

    fun pendingCount(): Int {
        readableDatabase.rawQuery(
            "select count(*) from pending_payments where status in (?, ?)",
            arrayOf(PendingPaymentStatus.Pending.storageValue, PendingPaymentStatus.Failed.storageValue),
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    fun allPending(): List<PendingPayment> {
        readableDatabase.rawQuery(
            """
            select * from pending_payments
            where status in (?, ?)
            order by
                case status when ? then 0 else 1 end,
                updated_at desc
            """.trimIndent(),
            arrayOf(
                PendingPaymentStatus.Failed.storageValue,
                PendingPaymentStatus.Pending.storageValue,
                PendingPaymentStatus.Failed.storageValue,
            ),
        ).use { cursor ->
            return buildList {
                while (cursor.moveToNext()) {
                    add(cursor.toPendingPayment())
                }
            }
        }
    }

    fun pendingForSync(limit: Int = 25): List<PendingPayment> {
        readableDatabase.rawQuery(
            """
            select * from pending_payments
            where status = ?
            order by created_at asc
            limit ?
            """.trimIndent(),
            arrayOf(
                PendingPaymentStatus.Pending.storageValue,
                limit.toString(),
            ),
        ).use { cursor ->
            return buildList {
                while (cursor.moveToNext()) {
                    add(cursor.toPendingPayment())
                }
            }
        }
    }

    fun markFailed(mobileUuid: String, message: String) {
        val values = ContentValues().apply {
            put("status", PendingPaymentStatus.Failed.storageValue)
            put("last_error", message.take(240))
            put("updated_at", System.currentTimeMillis())
        }

        writableDatabase.update("pending_payments", values, "mobile_uuid = ?", arrayOf(mobileUuid))
    }

    fun markPending(mobileUuid: String) {
        val values = ContentValues().apply {
            put("status", PendingPaymentStatus.Pending.storageValue)
            putNull("last_error")
            put("updated_at", System.currentTimeMillis())
        }

        writableDatabase.update("pending_payments", values, "mobile_uuid = ?", arrayOf(mobileUuid))
    }

    fun incrementAttempts(mobileUuid: String, message: String?) {
        writableDatabase.execSQL(
            """
            update pending_payments
            set attempts = attempts + 1,
                last_error = ?,
                updated_at = ?
            where mobile_uuid = ?
            """.trimIndent(),
            arrayOf<Any?>(message?.take(240), System.currentTimeMillis(), mobileUuid),
        )
    }

    fun delete(mobileUuid: String) {
        writableDatabase.delete("pending_payments", "mobile_uuid = ?", arrayOf(mobileUuid))
    }

    private fun getByMobileUuid(mobileUuid: String): PendingPayment? {
        readableDatabase.rawQuery(
            "select * from pending_payments where mobile_uuid = ? limit 1",
            arrayOf(mobileUuid),
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.toPendingPayment() else null
        }
    }

    private fun android.database.Cursor.toPendingPayment(): PendingPayment {
        return PendingPayment(
            id = getLong(getColumnIndexOrThrow("id")),
            loanId = getLong(getColumnIndexOrThrow("loan_id")),
            amount = getDouble(getColumnIndexOrThrow("amount")),
            paymentDate = getString(getColumnIndexOrThrow("payment_date")),
            paymentMethod = getString(getColumnIndexOrThrow("payment_method")),
            allocationMode = getString(getColumnIndexOrThrow("allocation_mode")) ?: "auto",
            targetInstallmentId = getLong(getColumnIndexOrThrow("target_installment_id")).takeIf { !isNull(getColumnIndexOrThrow("target_installment_id")) },
            mobileUuid = getString(getColumnIndexOrThrow("mobile_uuid")),
            status = PendingPaymentStatus.fromStorage(getString(getColumnIndexOrThrow("status"))),
            attempts = getInt(getColumnIndexOrThrow("attempts")),
            lastError = getString(getColumnIndexOrThrow("last_error")),
            createdAt = getLong(getColumnIndexOrThrow("created_at")),
            updatedAt = getLong(getColumnIndexOrThrow("updated_at")),
        )
    }

    private companion object {
        const val DATABASE_NAME = "pending_payments.db"
        const val DATABASE_VERSION = 3
    }
}
