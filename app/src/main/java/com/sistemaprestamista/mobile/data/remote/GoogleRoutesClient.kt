package com.sistemaprestamista.mobile.data.remote

import com.sistemaprestamista.mobile.BuildConfig
import com.sistemaprestamista.mobile.data.model.RoutePoint
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class GoogleRoutesClient {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun drivingRoute(points: List<RoutePoint>): List<RoutePoint> {
        val apiKey = BuildConfig.GOOGLE_MAPS_API_KEY
        if (apiKey.isBlank() || points.size < 2) {
            return emptyList()
        }

        val origin = points.first()
        val destination = points.last()
        val intermediates = points.drop(1).dropLast(1).take(MAX_INTERMEDIATE_WAYPOINTS)

        val payload = JSONObject()
            .put("origin", origin.toWaypoint())
            .put("destination", destination.toWaypoint())
            .put("travelMode", "DRIVE")
            .put("routingPreference", "TRAFFIC_UNAWARE")
            .put("polylineQuality", "HIGH_QUALITY")

        if (intermediates.isNotEmpty()) {
            payload.put("intermediates", JSONArray(intermediates.map { it.toWaypoint() }))
        }

        val request = Request.Builder()
            .url("https://routes.googleapis.com/directions/v2:computeRoutes")
            .header("Content-Type", "application/json")
            .header("X-Goog-Api-Key", apiKey)
            .header("X-Goog-FieldMask", "routes.polyline.encodedPolyline")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val rawBody = response.body.string()
            if (!response.isSuccessful) {
                throw IOException("Google Routes no pudo calcular la ruta (${response.code}).")
            }

            val routes = JSONObject(rawBody).optJSONArray("routes")
            val encoded = routes
                ?.optJSONObject(0)
                ?.optJSONObject("polyline")
                ?.optString("encodedPolyline")
                .orEmpty()

            return decodePolyline(encoded)
        }
    }

    private fun RoutePoint.toWaypoint(): JSONObject {
        return JSONObject()
            .put(
                "location",
                JSONObject()
                    .put(
                        "latLng",
                        JSONObject()
                            .put("latitude", latitude)
                            .put("longitude", longitude),
                    ),
            )
    }

    private fun decodePolyline(encoded: String): List<RoutePoint> {
        if (encoded.isBlank()) {
            return emptyList()
        }

        val points = mutableListOf<RoutePoint>()
        var index = 0
        var latitude = 0
        var longitude = 0

        while (index < encoded.length) {
            var result = 0
            var shift = 0
            var byte: Int
            do {
                byte = encoded[index++].code - 63
                result = result or ((byte and 0x1f) shl shift)
                shift += 5
            } while (byte >= 0x20)
            latitude += if ((result and 1) != 0) (result shr 1).inv() else result shr 1

            result = 0
            shift = 0
            do {
                byte = encoded[index++].code - 63
                result = result or ((byte and 0x1f) shl shift)
                shift += 5
            } while (byte >= 0x20)
            longitude += if ((result and 1) != 0) (result shr 1).inv() else result shr 1

            points.add(RoutePoint(latitude / 1E5, longitude / 1E5))
        }

        return points
    }

    private companion object {
        const val MAX_INTERMEDIATE_WAYPOINTS = 23
    }
}
