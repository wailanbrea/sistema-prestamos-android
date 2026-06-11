package com.sistemaprestamista.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sistemaprestamista.mobile.data.model.CollectorOption

private val BgCollectors = Color(0xFFF4F7FB)
private val PrimaryCol = Color(0xFF0F4C81)
private val SuccessCol = Color(0xFF16A34A)
private val SuccessSoftCol = Color(0xFFDCFCE7)
private val InactiveCol = Color(0xFF94A3B8)
private val InactiveSoftCol = Color(0xFFF1F5F9)
private val TextMainCol = Color(0xFF0F172A)
private val TextVariantCol = Color(0xFF64748B)

@Composable
internal fun CollectorsScreen(
    collectors: List<CollectorOption>,
    isLoading: Boolean,
    onOpenCollector: (Long) -> Unit,
    onCreateCollector: () -> Unit,
) {
    Scaffold(
        containerColor = BgCollectors,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateCollector,
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("Nuevo cobrador") },
                containerColor = PrimaryCol,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(4.dp),
            )
        },
    ) { innerPadding ->
        if (isLoading && collectors.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (collectors.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.People, contentDescription = null, modifier = Modifier.size(48.dp), tint = TextVariantCol)
                    Text("No hay cobradores registrados.", color = TextVariantCol)
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BgCollectors)
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(collectors, key = { it.id }) { collector ->
                CollectorRowCard(collector = collector, onOpen = { onOpenCollector(collector.id) })
            }
        }
    }
}

@Composable
private fun CollectorRowCard(collector: CollectorOption, onOpen: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE3EDF7)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = collector.name.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryCol,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(collector.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextMainCol)
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = TextVariantCol, modifier = Modifier.size(20.dp))
        }
    }
}
