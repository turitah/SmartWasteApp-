package com.example.smartwaste.driver

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.example.smartwaste.ui.theme.GreenDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverMapScreen(
    tasks: List<PickupTask>,
    currentLocation: LatLng?,
    onBack: () -> Unit,
    trafficMessage: String? = null
) {
    // Center of Kampala, Uganda
    val kampala = LatLng(0.3136, 32.5811)
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(kampala, 13f)
    }

    // Prepare route points
    val routePoints = remember(tasks) {
        tasks.filter { !it.isCompleted && !it.isMissed }
            .map { LatLng(it.latitude, it.longitude) }
    }

    // Zoom to fit all points when map is loaded
    LaunchedEffect(routePoints) {
        if (routePoints.isNotEmpty()) {
            val builder = LatLngBounds.Builder()
            routePoints.forEach { builder.include(it) }
            val bounds = builder.build()
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngBounds(bounds, 100)
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Optimized Route", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenDark)
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(zoomControlsEnabled = true)
            ) {
                // Draw polyline connecting all stops
                if (routePoints.size >= 2) {
                    Polyline(
                        points = routePoints,
                        color = GreenDark,
                        width = 12f
                    )
                }

                // Add markers for each stop
                tasks.filter { !it.isCompleted && !it.isMissed }.forEachIndexed { index, task ->
                    Marker(
                        state = MarkerState(position = LatLng(task.latitude, task.longitude)),
                        title = "Stop ${index + 1}: ${task.customerName}",
                        snippet = task.address
                    )
                }

                // Real-time Driver Location Marker
                currentLocation?.let { location ->
                    Marker(
                        state = MarkerState(position = location),
                        title = "Your Location",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                        zIndex = 1.0f
                    )
                }
            }

            // Traffic Alert Overlay
            if (trafficMessage != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    color = Color(0xFFF44336),
                    shape = RoundedCornerShape(8.dp),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = trafficMessage,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Map Legend / Info Card
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Route Details",
                        fontWeight = FontWeight.Bold,
                        color = GreenDark,
                        fontSize = 16.sp
                    )
                    Text(
                        "Total Stops: ${routePoints.size}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Text(
                        "Estimated Fuel Saving: 15%",
                        fontSize = 14.sp,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
