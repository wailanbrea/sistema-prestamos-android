package com.sistemaprestamista.mobile.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sistemaprestamista.mobile.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PendingPaymentSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val repository = AppContainer(applicationContext).repository
        val result = repository.syncPendingPayments()

        when {
            result.requiresRetry -> Result.retry()
            else -> Result.success()
        }
    }
}
