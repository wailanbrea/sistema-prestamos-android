package com.sistemaprestamista.mobile.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.sistemaprestamista.mobile.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/** Datos del manifiesto de actualización (latest.json en el servidor). */
data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val notes: String,
    val mandatory: Boolean,
)

/** Resultado de consultar si hay una actualización disponible. */
sealed interface UpdateCheck {
    data class Available(val info: UpdateInfo) : UpdateCheck
    data object UpToDate : UpdateCheck
    data class Failed(val message: String) : UpdateCheck
}

/**
 * Actualización OTA autohospedada: consulta un latest.json en el servidor, y si hay una
 * versión con versionCode mayor, descarga el APK y lo abre con el instalador del sistema.
 *
 * Requisitos:
 * - El APK del servidor debe estar firmado con la MISMA clave que el APK instalado
 *   (si no, Android lo rechaza con "aplicación no instalada").
 * - En Android 8+, el usuario debe permitir "instalar apps desconocidas" para esta app
 *   (se le redirige a Ajustes automáticamente la primera vez).
 * - La URL del manifiesto se configura en BuildConfig.UPDATE_MANIFEST_URL.
 */
object AppUpdater {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    val currentVersionName: String get() = BuildConfig.VERSION_NAME
    val currentVersionCode: Int get() = BuildConfig.VERSION_CODE

    suspend fun check(): UpdateCheck = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(BuildConfig.UPDATE_MANIFEST_URL).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext UpdateCheck.Failed("El servidor respondió ${response.code}")
                }
                val body = response.body?.string()
                    ?: return@withContext UpdateCheck.Failed("Respuesta vacía del servidor")
                val json = JSONObject(body)
                val info = UpdateInfo(
                    versionCode = json.getInt("versionCode"),
                    versionName = json.optString("versionName", ""),
                    apkUrl = json.getString("apkUrl"),
                    notes = json.optString("notes", ""),
                    mandatory = json.optBoolean("mandatory", false),
                )
                if (info.versionCode > BuildConfig.VERSION_CODE) {
                    UpdateCheck.Available(info)
                } else {
                    UpdateCheck.UpToDate
                }
            }
        } catch (e: Exception) {
            UpdateCheck.Failed(e.message ?: "No se pudo conectar con el servidor")
        }
    }

    /** Descarga el APK a cacheDir/updates/update.apk reportando el progreso 0..100. */
    suspend fun downloadApk(context: Context, url: String, onProgress: (Int) -> Unit): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val outFile = File(dir, "update.apk")

            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("La descarga falló (HTTP ${response.code})")
                }
                val bodyStream = response.body?.byteStream()
                    ?: throw IllegalStateException("La descarga vino vacía")
                val total = response.body?.contentLength() ?: -1L
                bodyStream.use { input ->
                    outFile.outputStream().use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var downloaded = 0L
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloaded += read
                            if (total > 0) onProgress(((downloaded * 100) / total).toInt())
                        }
                    }
                }
            }
            outFile
        }

    /** ¿La app puede instalar APKs? (Android 8+ requiere permiso explícito del usuario.) */
    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    /** Abre Ajustes para que el usuario permita "instalar apps desconocidas" para esta app. */
    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    /** Lanza el instalador del sistema con el APK descargado. */
    fun installApk(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.updateprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
