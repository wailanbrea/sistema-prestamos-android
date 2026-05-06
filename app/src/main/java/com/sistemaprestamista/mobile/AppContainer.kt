package com.sistemaprestamista.mobile

import android.content.Context
import com.sistemaprestamista.mobile.data.PrestamistaRepository
import com.sistemaprestamista.mobile.data.SessionStore
import com.sistemaprestamista.mobile.data.remote.PrestamistaApiClient
import com.sistemaprestamista.mobile.printing.PrintSettingsStore

class AppContainer(context: Context) {
    private val sessionStore = SessionStore(context.applicationContext)

    val repository: PrestamistaRepository = PrestamistaRepository(
        apiClient = PrestamistaApiClient(),
        sessionStore = sessionStore,
    )

    val printSettingsStore: PrintSettingsStore = PrintSettingsStore(sessionStore)
}
