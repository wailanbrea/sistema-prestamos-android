package com.sistemaprestamista.mobile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
        requestNotificationPermissionIfNeeded()
        requestLocationPermissionIfNeeded()
        setContent {
            SistemaPrestamistaAndroidTheme {
                PrestamistaApp(
                    viewModel = viewModel,
                    printSettingsStore = container.printSettingsStore,
                )
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIFICATIONS,
            )
        }
    }

    private fun requestLocationPermissionIfNeeded() {
        val permissions = buildList {
            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissions.toTypedArray(),
                REQUEST_LOCATION,
            )
        }
    }

    private companion object {
        const val REQUEST_NOTIFICATIONS = 3010
        const val REQUEST_LOCATION = 3011
    }
}
