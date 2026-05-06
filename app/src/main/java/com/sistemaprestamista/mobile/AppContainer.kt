package com.sistemaprestamista.mobile

import android.content.Context
import com.sistemaprestamista.mobile.data.PrestamistaRepository
import com.sistemaprestamista.mobile.data.SessionStore
import com.sistemaprestamista.mobile.data.pending.PendingPaymentStore
import com.sistemaprestamista.mobile.data.remote.PrestamistaApiClient
import com.sistemaprestamista.mobile.printing.PrintSettingsStore

class AppContainer(context: Context) {
    private val sessionStore = SessionStore(context.applicationContext)
    private val pendingPaymentStore = PendingPaymentStore(context.applicationContext)

    val repository: PrestamistaRepository = PrestamistaRepository(
        context = context.applicationContext,
        apiClient = PrestamistaApiClient(),
        sessionStore = sessionStore,
        pendingPaymentStore = pendingPaymentStore,
    )

    val printSettingsStore: PrintSettingsStore = PrintSettingsStore(sessionStore)
}
