package com.sistemaprestamista.mobile.tracking

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.sistemaprestamista.mobile.AppContainer
import com.sistemaprestamista.mobile.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.OffsetDateTime

class RouteTrackingForegroundService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var appContainer: AppContainer
    private var sessionId: Long? = null

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(applicationContext)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let(::sendLocation)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopTracking()
            else -> startTracking()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }

    private fun startTracking() {
        startForeground(NOTIFICATION_ID, notification())

        if (!hasLocationPermission()) {
            stopSelf()
            return
        }

        scope.launch {
            sessionId = runCatching { appContainer.repository.activeRouteSession()?.id }.getOrNull()
            if (sessionId == null) {
                stopSelf()
                return@launch
            }
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_INTERVAL_MS)
            .setMinUpdateIntervalMillis(FASTEST_INTERVAL_MS)
            .setWaitForAccurateLocation(false)
            .build()

        fusedLocationClient.requestLocationUpdates(request, locationCallback, mainLooper)
    }

    private fun stopTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun sendLocation(location: Location) {
        scope.launch {
            val activeSessionId = sessionId ?: runCatching { appContainer.repository.activeRouteSession()?.id }.getOrNull()
            if (activeSessionId == null) {
                stopSelf()
                return@launch
            }

            runCatching {
                appContainer.repository.sendRouteLocation(
                    sessionId = activeSessionId,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracyMeters = if (location.hasAccuracy()) location.accuracy.toInt() else null,
                    batteryLevel = batteryLevel(),
                    recordedAt = OffsetDateTime.now().toString(),
                )
            }.onSuccess { session ->
                sessionId = session.id
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun batteryLevel(): Int? {
        val manager = getSystemService(BATTERY_SERVICE) as BatteryManager
        val level = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return level.takeIf { it in 0..100 }
    }

    private fun notification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle("Ruta activa")
        .setContentText("Compartiendo ubicacion para seguimiento de cobrador.")
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .also {
            ensureChannel()
        }
        .build()

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) {
            return
        }

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Seguimiento de ruta",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Ubicacion en primer plano para verificar visitas de ruta."
            },
        )
    }

    companion object {
        private const val CHANNEL_ID = "prestamista_route_tracking"
        private const val NOTIFICATION_ID = 5101
        private const val LOCATION_INTERVAL_MS = 30000L
        private const val FASTEST_INTERVAL_MS = 15000L
        private const val ACTION_STOP = "com.sistemaprestamista.mobile.tracking.STOP"

        fun startIntent(context: Context): Intent = Intent(context, RouteTrackingForegroundService::class.java)

        fun stopIntent(context: Context): Intent = Intent(context, RouteTrackingForegroundService::class.java).apply {
            action = ACTION_STOP
        }
    }
}
