package com.sistemaprestamista.mobile.data

import android.content.Context
import java.io.File

/**
 * Caché en disco (almacenamiento interno privado de la app) de la última respuesta JSON
 * cruda de cada endpoint GET. Habilita dos mejoras de fluidez:
 *
 *  - **Lectura offline / resiliencia:** si la red falla, el [PrestamistaApiClient] devuelve
 *    la última respuesta conocida en vez de fallar, así la cartera sigue visible sin conexión.
 *  - **Apertura instantánea:** la UI puede pintar la caché de inmediato y refrescar en segundo plano.
 *
 * Se guarda el JSON crudo (no entidades Room) a propósito: así se reutilizan los parsers
 * existentes del cliente y se evita duplicar todo el grafo de modelos como entidades/conversores,
 * lo que reduce el riesgo en tiempo de ejecución. El almacenamiento interno es privado de la app
 * (sandbox), adecuado para estos datos.
 */
class ResponseCache(context: Context) {
    private val dir = File(context.applicationContext.filesDir, "api-cache").apply { mkdirs() }

    fun read(key: String): String? {
        val file = fileFor(key)
        return if (file.exists()) runCatching { file.readText() }.getOrNull() else null
    }

    fun write(key: String, body: String) {
        runCatching { fileFor(key).writeText(body) }
    }

    /** Borra toda la caché (p. ej. al cerrar sesión, para no filtrar datos entre cuentas). */
    fun clear() {
        runCatching { dir.listFiles()?.forEach { it.delete() } }
    }

    private fun fileFor(key: String): File {
        val safe = key.replace(Regex("[^A-Za-z0-9]"), "_").take(180)
        return File(dir, "$safe.json")
    }
}
