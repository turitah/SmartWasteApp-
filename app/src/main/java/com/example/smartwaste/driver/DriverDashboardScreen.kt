package com.example.smartwaste.driver

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverDashboardScreen(
    viewModel: DriverViewModel = viewModel(),
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val tasks by viewModel.tasks.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val showNotificationBadge by viewModel.showNotificationBadge.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val driverName by viewModel.driverName.collectAsState()
    val reportStatus by viewModel.reportStatus.collectAsState()

    var showNotifications by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var selectedTaskId by remember { mutableStateOf<Int?>(null) }
    var selectedIssueType by remember { mutableStateOf("") }
    var issueReason by remember { mutableStateOf("") }
    var showNoteDialog by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf("") }

    // Show report status feedback
    LaunchedEffect(reportStatus) {
        reportStatus?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearReportStatus()
        }
    }

    fun getGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    fun callCustomer(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL, "tel:$phoneNumber".toUri())
        context.startActivity(intent)
    }

    // Report Issue Dialog
    if (showReportDialog && selectedTaskId != null) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Report Issue") },
            text = {
                Column {
                    Text("Select issue type:", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedIssueType == "breakdown",
                            onClick = { selectedIssueType = "breakdown" },
                            label = { Text("Truck Breakdown", fontSize = 12.sp) }
                        )
                        FilterChip(
                            selected = selectedIssueType == "blocked_road",
                            onClick = { selectedIssueType = "blocked_road" },
                            label = { Text("Blocked Road", fontSize = 12.sp) }
                        )
                        FilterChip(
                            selected = selectedIssueType == "missed_pickup",
                            onClick = { selectedIssueType = "missed_pickup" },
                            label = { Text("Missed Pickup", fontSize = 12.sp) }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = issueReason,
                        onValueChange = { issueReason = it },
                        label = { Text("Reason details") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (selectedIssueType.isNotEmpty() && issueReason.isNotEmpty()) {
                            viewModel.reportIssue(selectedIssueType, selectedTaskId!!, issueReason)
                            showReportDialog = false
                            selectedIssueType = ""
                            issueReason = ""
                        }
                    }
                ) {
                    Text("Submit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Note Dialog
    if (showNoteDialog && selectedTaskId != null) {
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = { Text("Add Note") },
            text = {
                Column {
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Note for this pickup") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (noteText.isNotEmpty()) {
                            viewModel.addDriverNote(selectedTaskId!!, noteText)
                            showNoteDialog = false
                            noteText = ""
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNoteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Notifications Panel
    if (showNotifications) {
        ModalBottomSheet(
            onDismissRequest = { showNotifications = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Notifications", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { viewModel.clearAllNotifications() }) {
                        Text("Clear all", fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (notifications.isEmpty()) {
                    Text("No notifications", color = Color.Gray, modifier = Modifier.padding(32.dp))
                } else {
                    LazyColumn {
                        items(notifications) { notification ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { viewModel.markNotificationRead(notification.id) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (!notification.isRead) Color(0xFF2E7D32).copy(alpha = 0.1f) else Color.White
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(notification.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(notification.timestamp, fontSize = 10.sp, color = Color.Gray)
                                    }
                                    Text(notification.message, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(getGreeting(), fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                        Text(driverName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2E7D32),
                    titleContentColor = Color.White
                ),
                actions = {
                    // Notification Bell with Badge
                    Box {
                        IconButton(onClick = { showNotifications = true }) {
                            Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = Color.White)
                        }
                        if (showNotificationBadge) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color.Red, shape = RoundedCornerShape(5.dp))
                                    .align(Alignment.TopEnd)
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = Color.White)
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = Color.White)
                    }
                }
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Route Map Preview Card (Visual Route Representation)
            val nextTask = tasks.find { !it.isCompleted && !it.isMissed }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clickable(enabled = nextTask != null) {
                        nextTask?.let { task ->
                            val geoUri = "geo:${task.latitude},${task.longitude}?q=${task.latitude},${task.longitude}(${task.customerName})".toUri()
                            val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
                            context.startActivity(mapIntent)
                        }
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Actual Google Map Preview (Lite Mode)
                    val kampala = LatLng(0.3136, 32.5811)
                    val cameraPositionState = rememberCameraPositionState {
                        position = CameraPosition.fromLatLngZoom(kampala, 12f)
                    }

                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(
                            mapType = MapType.NORMAL,
                            isTrafficEnabled = true, // Shows live traffic lines
                            isMyLocationEnabled = false
                        ),
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = false,
                            scrollGesturesEnabled = false, // Static look
                            zoomGesturesEnabled = false,
                            mapToolbarEnabled = false
                        )
                    ) {
                        // Add markers for all pending tasks
                        tasks.filter { !it.isCompleted && !it.isMissed }.forEach { task ->
                            Marker(
                                state = MarkerState(position = LatLng(task.latitude, task.longitude)),
                                title = task.customerName,
                                snippet = task.address
                            )
                        }
                    }

                    // Floating labels like "Optimized Route" and "Heavy Traffic"
                    Column(modifier = Modifier.padding(12.dp)) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Green, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Live Traffic Route", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats Row - Ordering: Pickups, Pending, Completed, Missed
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("Pickups", stats.totalPickups.toString(), Icons.Filled.Delete, Color(0xFF2196F3), Modifier.weight(1f))
                StatCard("Pending", stats.pendingPickups.toString(), Icons.Filled.HourglassEmpty, Color(0xFFFF9800), Modifier.weight(1f))
                StatCard("Completed", stats.completedPickups.toString(), Icons.Filled.CheckCircle, Color(0xFF4CAF50), Modifier.weight(1f))
                StatCard("Missed", stats.missedPickups.toString(), Icons.Filled.Warning, Color(0xFFF44336), Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Route List Header
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Today's Route", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (tasks.isEmpty() && !isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No pickups for today", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(tasks.filter { !it.isCompleted && !it.isMissed }) { task ->
                        RouteCard(
                            task = task,
                            onNavigate = { 
                                val geoUri = "geo:${task.latitude},${task.longitude}?q=${task.latitude},${task.longitude}(${task.customerName})".toUri()
                                val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
                                context.startActivity(mapIntent)
                            },
                            onCall = { callCustomer(task.phoneNumber) },
                            onComplete = { viewModel.markTaskCompleted(task.id) },
                            onReportIssue = {
                                selectedTaskId = task.id
                                showReportDialog = true
                            },
                            onAddNote = {
                                selectedTaskId = task.id
                                showNoteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier.shadow(4.dp, shape = RoundedCornerShape(12.dp)), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(24.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
            Text(title, fontSize = 10.sp, color = Color.Gray)
        }
    }
}

@Composable
fun RouteCard(
    task: PickupTask,
    onNavigate: () -> Unit,
    onCall: () -> Unit,
    onComplete: () -> Unit,
    onReportIssue: () -> Unit,
    onAddNote: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, shape = RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).background(
                        color = Color(0xFFFF9800).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ), contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.RadioButtonUnchecked,
                        contentDescription = "Status",
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        task.customerName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                    Text(task.address, fontSize = 13.sp, color = Color.Gray)
                    Row {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Waste Type",
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFF2E7D32)
                        )
                        Text(task.wasteType, fontSize = 12.sp, color = Color(0xFF2E7D32))
                    }
                    if (task.notes.isNotEmpty()) {
                        Text(task.notes, fontSize = 10.sp, color = Color(0xFFFF9800))
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onNavigate,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Filled.Navigation, contentDescription = null, modifier = Modifier.size(14.dp))
                    Text("Go", fontSize = 11.sp)
                }
                Button(
                    onClick = onCall,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Filled.Phone, contentDescription = "Call Customer", modifier = Modifier.size(14.dp))
                }
                Button(
                    onClick = onComplete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Filled.Done, contentDescription = null, modifier = Modifier.size(14.dp))
                    Text("Done", fontSize = 11.sp)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onReportIssue,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = Color(0xFFF44336)
                    )
                    Text("Report", fontSize = 10.sp, color = Color(0xFFF44336))
                }
                OutlinedButton(
                    onClick = onAddNote,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Filled.EditNote, contentDescription = null, modifier = Modifier.size(12.dp))
                    Text("Add Note", fontSize = 10.sp)
                }
            }
        }
    }
}