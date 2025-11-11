package com.runanywhere.startup_hackathon20.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.FlowRow
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.runanywhere.startup_hackathon20.SafetyViewModel
import com.runanywhere.startup_hackathon20.data.SafePlace
import com.runanywhere.startup_hackathon20.R
import android.content.Intent
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun EscapeToSafetyScreen(
    viewModel: SafetyViewModel = viewModel()
) {
    val nearestSafePlaces by viewModel.nearestSafePlaces.collectAsState()
    val userLocation by viewModel.userLocation.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val context = LocalContext.current

    // Map state
    val mapState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(0.0, 0.0),
            12f
        )  // Initial, will update
    }

    // Update map camera on user location change
    LaunchedEffect(userLocation) {
        userLocation?.let { location ->
            mapState.position = CameraPosition.fromLatLngZoom(
                LatLng(location.latitude, location.longitude),
                12f  // ~5km view
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Google Map
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = mapState
        ) {
            // User marker (blue)
            userLocation?.let { location ->
                Marker(
                    state = MarkerState(
                        position = LatLng(
                            location.latitude,
                            location.longitude
                        )
                    ),
                    title = "Your Location",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
                )
            }

            // Safe places markers
            nearestSafePlaces.forEach { place ->
                val markerColor = when {
                    place.types.contains("police") -> BitmapDescriptorFactory.HUE_RED
                    place.types.contains("hospital") -> BitmapDescriptorFactory.HUE_BLUE
                    place.isOpen24h -> BitmapDescriptorFactory.HUE_GREEN
                    else -> BitmapDescriptorFactory.HUE_YELLOW
                }
                Marker(
                    state = MarkerState(position = LatLng(place.latitude, place.longitude)),
                    title = place.name,
                    snippet = "${place.distance.toInt()}m - ${if (place.isOpen24h) "24h" else "Closed"}",
                    icon = BitmapDescriptorFactory.defaultMarker(markerColor)
                )
            }
        }

        // Status header
        TopAppBar(
            title = { Text("🚨 Navigate to Safety") },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Status message
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text(
                text = statusMessage,
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Safe places list
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 72.dp)  // Below top app bar
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(nearestSafePlaces) { place ->
                SafePlaceCard(
                    place = place,
                    onNavigate = {
                        // Call navigateToPlace via ViewModel
                        viewModel.navigateToPlace(place)
                    },
                    onCallPolice = {
                        // Call police
                        viewModel.requestCallPolice()
                    }
                )
            }
        }

        // Bottom buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { viewModel.requestCallPolice() },
                modifier = Modifier.weight(1f)
            ) {
                Text("📞 Call Police")
            }
            Button(
                onClick = {
                    // Mark as safe - set destination to nearest or selected
                    nearestSafePlaces.firstOrNull()?.let { place ->
                        viewModel.navigateToPlace(place)
                    }
                    // Could add dialog to select place
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("✓ Mark Safe")
            }
        }
    }
}

// SafePlaceCard Composable (inline or separate, but for completeness)
@Composable
fun SafePlaceCard(
    place: SafePlace,
    onNavigate: () -> Unit,
    onCallPolice: () -> Unit  // Not used per place, but for context
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = place.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${place.distance.toInt()}m away",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row {
                Icon(
                    imageVector = if (place.isOpen24h) Icons.Default.AccessTime else Icons.Default.Close,
                    contentDescription = if (place.isOpen24h) "24h Open" else "Closed",
                    tint = if (place.isOpen24h) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Text(
                    text = if (place.isOpen24h) "24h Open" else "Closed",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            // Types as tags
            FlowRow {
                place.types.take(2).forEach { type ->
                    AssistChip(
                        onClick = { /* No action */ },
                        label = { Text(type) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
            Button(
                onClick = onNavigate,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Navigate")
            }
        }
    }
}