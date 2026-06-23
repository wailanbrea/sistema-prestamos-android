package com.sistemaprestamista.mobile.ui

import android.content.Context
import android.location.Geocoder
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private val DefaultMapCenter = LatLng(18.4861, -69.9312) // Santo Domingo
private val PickerPrimary = Color(0xFF1A4F8B)

/**
 * Selector de dirección. El mapa pequeño es solo una vista previa (sin gestos, para
 * no pelear con el scroll del formulario); al tocarlo se abre un mapa a pantalla
 * completa con buscador donde el usuario elige el punto con calma y confirma con OK.
 * Al confirmar se fija lat/lng y se autocompleta la dirección por geocodificación
 * inversa (Geocoder de Android; sin SDK de Places).
 */
@Composable
internal fun AddressMapPicker(
    latitude: Double?,
    longitude: Double?,
    onLocationPicked: (lat: Double, lng: Double) -> Unit,
    onAddressResolved: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showFullScreen by remember { mutableStateOf(false) }

    val previewPosition = if (latitude != null && longitude != null) {
        LatLng(latitude, longitude)
    } else {
        DefaultMapCenter
    }
    val previewCamera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(previewPosition, if (latitude != null) 15f else 11f)
    }
    LaunchedEffect(previewPosition) {
        previewCamera.position = CameraPosition.fromLatLngZoom(previewPosition, if (latitude != null) 15f else 11f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(170.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { showFullScreen = true },
    ) {
        // Lite mode: el mapa se dibuja como imagen estática, no intercepta el scroll.
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = previewCamera,
            googleMapOptionsFactory = { GoogleMapOptions().liteMode(true) },
            uiSettings = MapUiSettings(
                scrollGesturesEnabled = false,
                zoomGesturesEnabled = false,
                zoomControlsEnabled = false,
                rotationGesturesEnabled = false,
                tiltGesturesEnabled = false,
                myLocationButtonEnabled = false,
                mapToolbarEnabled = false,
            ),
            onMapClick = { showFullScreen = true },
        ) {
            if (latitude != null && longitude != null) {
                Marker(state = MarkerState(position = previewPosition))
            }
        }

        Text(
            text = if (latitude != null) "Toca para ajustar en el mapa" else "Toca para elegir en el mapa",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PickerPrimary)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }

    if (showFullScreen) {
        FullScreenMapPicker(
            initialLatitude = latitude,
            initialLongitude = longitude,
            onConfirm = { lat, lng, address ->
                onLocationPicked(lat, lng)
                if (!address.isNullOrBlank()) onAddressResolved(address)
                showFullScreen = false
            },
            onDismiss = { showFullScreen = false },
        )
    }
}

@Composable
private fun FullScreenMapPicker(
    initialLatitude: Double?,
    initialLongitude: Double?,
    onConfirm: (lat: Double, lng: Double, address: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val start = remember {
        if (initialLatitude != null && initialLongitude != null) {
            LatLng(initialLatitude, initialLongitude)
        } else {
            DefaultMapCenter
        }
    }
    val markerState = remember { MarkerState(position = start) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(start, if (initialLatitude != null) 16f else 12f)
    }
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var currentAddress by remember { mutableStateOf<String?>(null) }
    var notFound by remember { mutableStateOf(false) }

    // Cada vez que el marcador se mueve (tap/arrastre/búsqueda) resolvemos la dirección.
    LaunchedEffect(markerState.position) {
        val point = markerState.position
        currentAddress = reverseGeocode(context, point.latitude, point.longitude)
    }

    fun runSearch() {
        if (query.isBlank()) return
        scope.launch {
            searching = true
            notFound = false
            val match = forwardGeocode(context, query)
            searching = false
            if (match != null) {
                markerState.position = match
                cameraPositionState.position = CameraPosition.fromLatLngZoom(match, 16f)
            } else {
                notFound = true
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isBuildingEnabled = true),
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = true,
                        myLocationButtonEnabled = false,
                        mapToolbarEnabled = false,
                    ),
                    onMapClick = { markerState.position = it },
                ) {
                    Marker(state = markerState, draggable = true)
                }

                // Barra de búsqueda arriba.
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(12.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    shadowElevation = 6.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Outlined.Close, contentDescription = "Cerrar", tint = PickerPrimary)
                        }
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it; notFound = false },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Buscar dirección o lugar...") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { runSearch() }),
                            isError = notFound,
                        )
                        IconButton(onClick = ::runSearch) {
                            if (searching) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = PickerPrimary)
                            } else {
                                Icon(Icons.Outlined.Search, contentDescription = "Buscar", tint = PickerPrimary)
                            }
                        }
                    }
                }

                // Tarjeta inferior: dirección actual + acciones.
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(12.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 8.dp,
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (notFound) "No se encontró esa dirección." else (currentAddress ?: "Mueve el marcador o busca una dirección."),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF1A1C20),
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text("Cancelar")
                            }
                            Button(
                                onClick = {
                                    val point = markerState.position
                                    onConfirm(point.latitude, point.longitude, currentAddress)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PickerPrimary, contentColor = Color.White),
                            ) {
                                Text("OK", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun reverseGeocode(context: Context, lat: Double, lng: Double): String? =
    withContext(Dispatchers.IO) {
        runCatching {
            @Suppress("DEPRECATION")
            Geocoder(context, Locale.getDefault())
                .getFromLocation(lat, lng, 1)
                ?.firstOrNull()
                ?.getAddressLine(0)
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }

private suspend fun forwardGeocode(context: Context, query: String): LatLng? =
    withContext(Dispatchers.IO) {
        runCatching {
            @Suppress("DEPRECATION")
            Geocoder(context, Locale.getDefault())
                .getFromLocationName(query, 1)
                ?.firstOrNull()
                ?.let { LatLng(it.latitude, it.longitude) }
        }.getOrNull()
    }
