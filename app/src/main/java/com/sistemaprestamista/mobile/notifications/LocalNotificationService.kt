package com.sistemaprestamista.mobile.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sistemaprestamista.mobile.R

class LocalNotificationService(
    private val context: Context,
) {
    fun notifySyncResult(sent: Int, failed: Int, remaining: Int) {
        if (sent == 0 && failed == 0 && remaining == 0) {
            return
        }

        val title = if (failed > 0) {
            "Cobros con conflicto"
        } else {
            "Cobros sincronizados"
        }

        val message = when {
            failed > 0 -> "$failed cobros requieren revision. Pendientes: $remaining."
            sent > 0 && remaining > 0 -> "Se sincronizaron $sent cobros. Pendientes: $remaining."
            sent > 0 -> "Se sincronizaron $sent cobros correctamente."
            else -> "Quedan $remaining cobros pendientes por sincronizar."
        }

        show(
            id = SYNC_NOTIFICATION_ID,
            title = title,
            message = message,
            highPriority = failed > 0,
        )
    }

    fun notifyPendingCollections(count: Int) {
        if (count <= 0) {
            return
        }

        show(
            id = COLLECTIONS_NOTIFICATION_ID,
            title = "Cobros pendientes",
            message = "Tienes $count cuotas pendientes para seguimiento.",
            highPriority = false,
        )
    }

    private fun show(id: Int, title: String, message: String, highPriority: Boolean) {
        if (!canPostNotifications()) {
            return
        }

        ensureChannel()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(if (highPriority) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }

    private fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) {
            return
        }

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Alertas de cobros",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Alertas de cuotas, sincronizacion y conflictos de cobro."
            },
        )
    }

    private companion object {
        const val CHANNEL_ID = "prestamista_collections"
        const val SYNC_NOTIFICATION_ID = 4101
        const val COLLECTIONS_NOTIFICATION_ID = 4102
    }
}
