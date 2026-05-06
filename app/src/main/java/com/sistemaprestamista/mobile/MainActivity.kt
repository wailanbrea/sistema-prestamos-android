package com.sistemaprestamista.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.sistemaprestamista.mobile.ui.MainViewModel
import com.sistemaprestamista.mobile.ui.PrestamistaApp
import com.sistemaprestamista.mobile.ui.theme.SistemaPrestamistaAndroidTheme

class MainActivity : ComponentActivity() {
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
