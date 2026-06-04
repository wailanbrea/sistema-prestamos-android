package com.sistemaprestamista.mobile.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.sistemaprestamista.mobile.data.model.CollectorRoute
import com.sistemaprestamista.mobile.data.model.CollectorRouteSession
import com.sistemaprestamista.mobile.data.model.MapClient
import com.sistemaprestamista.mobile.data.model.RoutePoint
import com.sistemaprestamista.mobile.ui.components.EmptyCard
import com.sistemaprestamista.mobile.ui.components.rememberCurrency
import java.util.Locale

private val SantoDomingo = LatLng(18.4861, -69.9312)

@Composable
fun MapScreen(
    clients: List<MapClient>,
    routes: List<CollectorRoute>,
    selectedRouteId: Long,
    optimizedRouteClientIds: List<Long>,
    realRoutePoints: List<RoutePoint>,
    routeWarning: String?,
    activeSession: CollectorRouteSession?,
    isRouteTrackingLoading: Boolean,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onSelectRoute: (Long) -> Unit,
    onStartTracking: (Long, RoutePoint?) -> Unit,
    onFinishTracking: () -> Unit,
    onOpenClient: (Long) -> Unit,
) {
    val context = LocalContext.current
    val currency = rememberCurrency()
    val locationClient = remember(context) { LocationServices.getFusedLocationProviderClient(context) }
    val visibleClients = remember(clients, routes, selectedRouteId, optimizedRouteClientIds) {
        if (selectedRouteId == 0L) {
            clients
        } else {
            val routeClientIds = optimizedRouteClientIds.ifEmpty {
                routes
                    .firstOrNull { it.id == selectedRouteId }
                    ?.clients
                    ?.sortedBy { it.orderNumber }
                    ?.map { it.summary.id }
                    .orEmpty()
            }
            val clientsById = clients.associateBy { it.summary.id }
            routeClientIds.mapNotNull { clientsById[it] }
        }
    }
    val mappedClients = visibleClients.filter { it.hasCoordinates }

    LaunchedEffect(Unit) {
        onRefresh()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Mapa de cobros",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Clientes asignados, balance pendiente y total pagado por ruta.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        item {
            RouteFilter(
                routes = routes,
                selectedRouteId = selectedRouteId,
                onSelectRoute = onSelectRoute,
            )
        }

        item {
            TrackingControls(
                activeSession = activeSession,
                selectedRouteId = selectedRouteId,
                routes = routes,
                isLoading = isRouteTrackingLoading,
                onStartTracking = { routeId ->
                    if (!hasFineLocationPermission(context)) {
                        onStartTracking(routeId, null)
                        return@TrackingControls
                    }

                    locationClient
                        .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                        .addOnSuccessListener { location ->
                            onStartTracking(
                                routeId,
                                location?.let { RoutePoint(it.latitude, it.longitude) },
                            )
                        }
                        .addOnFailureListener {
                            onStartTracking(routeId, null)
                        }
                },
                onFinishTracking = onFinishTracking,
            )
        }

        item {
            MapPanel(
                isLoading = isLoading,
                clients = mappedClients,
                realRoutePoints = realRoutePoints,
                routeWarning = routeWarning,
                onNavigateRoute = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, directionsUri(mappedClients)))
                },
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SummaryPill(
                    label = "En mapa",
                    value = mappedClients.size.toString(),
                    modifier = Modifier.weight(1f),
                )
                SummaryPill(
                    label = "Sin coordenadas",
                    value = (visibleClients.size - mappedClients.size).coerceAtLeast(0).toString(),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (visibleClients.isEmpty() && !isLoading) {
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    EmptyCard("No hay clientes asignados a esta ruta.")
                }
            }
        } else {
            items(visibleClients, key = { it.summary.id }) { client ->
                ClientMapCard(
                    client = client,
                    amountText = currency.format(client.financialSummary.remainingBalance),
                    paidText = currency.format(client.financialSummary.totalPaid),
                    onOpenClient = { onOpenClient(client.summary.id) },
                )
            }
        }
    }
}

@Composable
private fun TrackingControls(
    activeSession: CollectorRouteSession?,
    selectedRouteId: Long,
    routes: List<CollectorRoute>,
    isLoading: Boolean,
    onStartTracking: (Long) -> Unit,
    onFinishTracking: () -> Unit,
) {
    val startRouteId = selectedRouteId.takeIf { it > 0L } ?: routes.singleOrNull()?.id

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Seguimiento GPS", fontWeight = FontWeight.Bold)
                    Text(
                        text = activeSession?.let { "${it.routeName.orEmpty()} · ${it.visitedStops}/${it.totalStops} visitados" }
                            ?: "Selecciona una ruta e inicia la transmision.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { startRouteId?.let(onStartTracking) },
                    enabled = !isLoading && startRouteId != null && activeSession?.status != "active",
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Iniciar")
                }
                OutlinedButton(
                    onClick = onFinishTracking,
                    enabled = !isLoading && activeSession?.status == "active",
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Finalizar")
                }
            }
        }
    }
}

@Composable
private fun RouteFilter(
    routes: List<CollectorRoute>,
    selectedRouteId: Long,
    onSelectRoute: (Long) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AssistChip(
            onClick = { onSelectRoute(0L) },
            label = { Text("Todas") },
            leadingIcon = { Icon(Icons.Outlined.Route, contentDescription = null, modifier = Modifier.size(18.dp)) },
            border = chipBorder(selectedRouteId == 0L),
        )
        routes.forEach { route ->
            AssistChip(
                onClick = { onSelectRoute(route.id) },
                label = { Text(route.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                border = chipBorder(selectedRouteId == route.id),
            )
        }
    }
}

@Composable
private fun chipBorder(selected: Boolean): BorderStroke {
    return BorderStroke(
        width = if (selected) 2.dp else 1.dp,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun MapPanel(
    isLoading: Boolean,
    clients: List<MapClient>,
    realRoutePoints: List<RoutePoint>,
    routeWarning: String?,
    onNavigateRoute: () -> Unit,
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(SantoDomingo, 11f)
    }

    LaunchedEffect(clients.map { it.summary.id }) {
        when (clients.size) {
            0 -> cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(SantoDomingo, 11f))
            1 -> {
                val point = clients.first().latLng()
                if (point != null) {
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(point, 14f))
                }
            }
            else -> {
                val bounds = LatLngBounds.builder().apply {
                    clients.mapNotNull { it.latLng() }.forEach(::include)
                }.build()
                cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 90))
            }
        }
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isBuildingEnabled = true),
                uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false),
            ) {
                val routePoints = realRoutePoints.map { LatLng(it.latitude, it.longitude) }
                if (routePoints.size > 1) {
                    Polyline(points = routePoints, color = Color(0xFF0B63CE), width = 7f)
                }
                clients.forEachIndexed { index, client ->
                    val point = client.latLng() ?: return@forEachIndexed
                    Marker(
                        state = MarkerState(position = point),
                        title = "${index + 1}. ${client.summary.fullName}",
                        snippet = "Debe ${client.financialSummary.remainingBalance} | Pago ${client.financialSummary.totalPaid}",
                    )
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.72f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            if (clients.isEmpty() && !isLoading) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Outlined.LocationOff, contentDescription = null)
                    Text("No hay clientes con coordenadas para mostrar.")
                }
            }

            if (!routeWarning.isNullOrBlank() && clients.size > 1 && !isLoading) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(12.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ) {
                    Text(
                        text = routeWarning,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Button(
                onClick = onNavigateRoute,
                enabled = clients.isNotEmpty(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(14.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Outlined.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text("Navegar")
            }
        }
    }
}

@Composable
private fun SummaryPill(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ClientMapCard(
    client: MapClient,
    amountText: String,
    paidText: String,
    onOpenClient: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(client.summary.fullName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        client.summary.address.orEmpty(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SummaryPill("Debe", amountText, Modifier.weight(1f))
                SummaryPill("Pagado", paidText, Modifier.weight(1f))
            }
            if (!client.hasCoordinates) {
                Text(
                    text = "Pendiente de coordenadas. La direccion ya existe, pero no aparecera en el mapa hasta guardar latitud y longitud.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            OutlinedButton(onClick = onOpenClient, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Text("Ver cliente")
            }
        }
    }
}

private fun MapClient.latLng(): LatLng? {
    val latitude = summary.latitude ?: return null
    val longitude = summary.longitude ?: return null
    return LatLng(latitude, longitude)
}

private fun directionsUri(clients: List<MapClient>): Uri {
    val points = clients.mapNotNull { it.latLng() }
    if (points.isEmpty()) {
        return Uri.EMPTY
    }

    val format = { value: Double -> String.format(Locale.US, "%.6f", value) }
    val destination = points.last()
    val waypoints = points.dropLast(1).joinToString("|") { "${format(it.latitude)},${format(it.longitude)}" }

    return Uri.parse(
        buildString {
            append("https://www.google.com/maps/dir/?api=1")
            append("&destination=${format(destination.latitude)},${format(destination.longitude)}")
            append("&travelmode=driving")
            if (waypoints.isNotBlank()) {
                append("&waypoints=$waypoints")
            }
        },
    )
}

private fun hasFineLocationPermission(context: android.content.Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
}
