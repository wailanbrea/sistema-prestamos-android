package com.sistemaprestamista.mobile

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import com.sistemaprestamista.mobile.ui.MainViewModel
import com.sistemaprestamista.mobile.ui.PrestamistaApp
import com.sistemaprestamista.mobile.ui.theme.SistemaPrestamistaAndroidTheme

class MainActivity : FragmentActivity() {
    private val container by lazy { AppContainer(applicationContext) }
    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(container.repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SistemaPrestamistaAndroidTheme {
                PrestamistaApp(
                    viewModel = viewModel,
                    printSettingsStore = container.printSettingsStore,
                )
            }
        }
    }
}
