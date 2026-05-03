package com.example.smartwaste.admin

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.example.smartwaste.ui.theme.GreenDark
import com.example.smartwaste.ui.theme.GreenPrimary
import com.example.smartwaste.ui.theme.SmartWasteTheme
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

enum class AdminSubScreen {
    Dashboard, Drivers, MapView, Schedule, Reports, Bins, Notifications
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(onLogout: () -> Unit) {
    var currentSubScreen by remember { mutableStateOf(AdminSubScreen.Dashboard) }
    var selectedReport by remember { mutableStateOf<ReportItem?>(null) }
    var showAddDriverDialog by remember { mutableStateOf(false) }
    var showAddScheduleDialog by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<ScheduleData?>(null) }
    var editingDriver by remember { mutableStateOf<Driver?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var reportToAssign by remember { mutableStateOf<ReportItem?>(null) }

    // Realtime Database reports state
    val db = FirebaseDatabase.getInstance().reference
    var reports by remember { mutableStateOf<List<ReportItem>>(emptyList()) }
    var assignedTasks by remember { mutableStateOf<List<ScheduleData>>(emptyList()) }
    var unreadReportCount by remember { mutableIntStateOf(0) }
    var lastKnownReportCount by remember { mutableIntStateOf(-1) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val reportsRef = db.child("reports")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val reportList = mutableListOf<ReportItem>()
                for (doc in snapshot.children) {
                    reportList.add(
                        ReportItem(
                            id = doc.key ?: "",
                            userName = doc.child("userEmail").getValue(String::class.java) ?: "Unknown",
                            location = doc.child("location").getValue(String::class.java) ?: "Unknown",
                            description = doc.child("description").getValue(String::class.java) ?: "",
                            status = doc.child("status").getValue(String::class.java) ?: "Pending",
                            issueType = doc.child("issueType").getValue(String::class.java) ?: "General",
                            timestamp = doc.child("timestamp").getValue(Long::class.java) ?: 0L,
                            latitude = doc.child("latitude").getValue(Double::class.java) ?: 0.0,
                            longitude = doc.child("longitude").getValue(Double::class.java) ?: 0.0,
                            imageUri = doc.child("imageUri").getValue(String::class.java) ?: ""
                        )
                    )
                }
                reportList.sortByDescending { it.timestamp }
                
                if (lastKnownReportCount != -1 && reportList.size > lastKnownReportCount) {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "New user report received!",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
                
                reports = reportList
                unreadReportCount = reportList.count { it.status == "Pending" }
                lastKnownReportCount = reportList.size
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("AdminDashboard", "Database error: ${error.message}")
            }
        }
        reportsRef.addValueEventListener(listener)

        // Listen for assigned tasks to track progress
        val tasksRef = db.child("assigned_tasks")
        tasksRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val taskList = mutableListOf<ScheduleData>()
                for (doc in snapshot.children) {
                    taskList.add(
                        ScheduleData(
                            id = doc.key ?: "",
                            date = doc.child("date").getValue(String::class.java) ?: "",
                            shift = doc.child("shift").getValue(String::class.java) ?: "",
                            driverName = doc.child("driverName").getValue(String::class.java) ?: "",
                            isCompleted = doc.child("isCompleted").getValue(Boolean::class.java) ?: false,
                            address = doc.child("address").getValue(String::class.java) ?: "Scheduled Route",
                            customerName = doc.child("customerName").getValue(String::class.java) ?: "Scheduled Pickup"
                        )
                    )
                }
                assignedTasks = taskList
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // State for drivers, schedules, and bins
    var drivers by remember { mutableStateOf<List<Driver>>(emptyList()) }

    LaunchedEffect(Unit) {
        val driversRef = db.child("drivers")
        driversRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val driverList = mutableListOf<Driver>()
                for (doc in snapshot.children) {
                    driverList.add(
                        Driver(
                            id = doc.key ?: "",
                            name = doc.child("name").getValue(String::class.java) ?: "",
                            truck = doc.child("truck").getValue(String::class.java) ?: "",
                            status = doc.child("status").getValue(String::class.java) ?: "Offline",
                            statusColor = Color(doc.child("statusColor").getValue(Long::class.java) ?: 0xFF9E9E9E),
                            profileImage = doc.child("profileImage").getValue(String::class.java) ?: ""
                        )
                    )
                }
                drivers = driverList
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    var bins by remember {
        mutableStateOf(
            listOf(
                BinData("Bin-001", "Acacia Ave", 85, 0.3136, 32.5811),
                BinData("Bin-002", "Bugolobi Village", 40, 0.3200, 32.6100),
                BinData("Bin-003", "Kiwatule Road", 15, 0.3500, 32.6200),
                BinData("Bin-004", "Ntinda St", 92, 0.3550, 32.6100),
                BinData("Bin-005", "Makerere Main", 60, 0.3300, 32.5700)
            )
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (currentSubScreen) {
                            AdminSubScreen.Dashboard -> "Admin Dashboard"
                            AdminSubScreen.Drivers -> "Manage Drivers"
                            AdminSubScreen.MapView -> "Live Bin Map"
                            AdminSubScreen.Schedule -> "Pickup Schedule"
                            AdminSubScreen.Reports -> "User Reports"
                            AdminSubScreen.Bins -> "Manage Bins"
                            AdminSubScreen.Notifications -> "Admin Notifications"
                        },
                        color = Color.White
                    )
                },
                navigationIcon = {
                    if (currentSubScreen != AdminSubScreen.Dashboard) {
                        IconButton(onClick = { currentSubScreen = AdminSubScreen.Dashboard }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                        }
                    }
                },
                actions = {
                    Box {
                        BadgedBox(
                            badge = {
                                if (unreadReportCount > 0) {
                                    Badge { Text(unreadReportCount.toString()) }
                                }
                            }
                        ) {
                            IconButton(onClick = { showMenu = !showMenu }) {
                                Icon(Icons.Default.Menu, "Menu", tint = Color.White)
                            }
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Dashboard") },
                                onClick = {
                                    currentSubScreen = AdminSubScreen.Dashboard
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Dashboard, null) }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Drivers") },
                                onClick = {
                                    currentSubScreen = AdminSubScreen.Drivers
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.People, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Schedule") },
                                onClick = {
                                    currentSubScreen = AdminSubScreen.Schedule
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.DateRange, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Bins") },
                                onClick = {
                                    currentSubScreen = AdminSubScreen.Bins
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, null) }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Reports")
                                        if (unreadReportCount > 0) {
                                            Spacer(Modifier.width(8.dp))
                                            Badge { Text(unreadReportCount.toString()) }
                                        }
                                    }
                                },
                                onClick = {
                                    currentSubScreen = AdminSubScreen.Reports
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Report, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Notifications") },
                                onClick = {
                                    currentSubScreen = AdminSubScreen.Notifications
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Notifications, null) }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Logout", color = Color.Red) },
                                onClick = {
                                    showMenu = false
                                    onLogout()
                                },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color.Red) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenPrimary)
            )
        },
        floatingActionButton = {
            if (currentSubScreen == AdminSubScreen.Drivers) {
                FloatingActionButton(onClick = { showAddDriverDialog = true }, containerColor = GreenPrimary) {
                    Icon(Icons.Default.Add, "Add Driver", tint = Color.White)
                }
            } else if (currentSubScreen == AdminSubScreen.Schedule) {
                FloatingActionButton(onClick = { showAddScheduleDialog = true }, containerColor = GreenPrimary) {
                    Icon(Icons.Default.Add, "Add Task", tint = Color.White)
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (currentSubScreen) {
                AdminSubScreen.Dashboard -> {
                    AdminDashboardOverview(
                        onNavigate = { currentSubScreen = it },
                        drivers = drivers,
                        bins = bins,
                        reports = reports,
                        assignedTasks = assignedTasks
                    )
                }
                AdminSubScreen.Drivers -> ManageDriversScreen(
                    drivers, 
                    onEditDriver = { editingDriver = it },
                    onDeleteDriver = { driver ->
                        scope.launch {
                            db.child("drivers").child(driver.id).removeValue().await()
                        }
                    }
                )
                AdminSubScreen.Schedule -> AdminScheduleScreen(
                    assignedTasks,
                    onEditSchedule = { editingSchedule = it },
                    onDeleteSchedule = { schedule ->
                        scope.launch {
                            db.child("assigned_tasks").child(schedule.id).removeValue().await()
                        }
                    }
                )
                AdminSubScreen.MapView -> AdminMapView(bins, drivers)
                AdminSubScreen.Reports -> ManageReportsScreen(
                    reports = reports,
                    onReportClick = { selectedReport = it }
                )
                AdminSubScreen.Bins -> ManageBinsScreen(bins)
                AdminSubScreen.Notifications -> AdminNotificationsScreen(reports)
            }

            if (selectedReport != null) {
                ReportDetailDialog(
                    report = selectedReport!!,
                    onDismiss = { selectedReport = null },
                    onMarkAsResolved = {
                        db.child("reports").child(selectedReport!!.id).child("status").setValue("Resolved")
                        selectedReport = null
                    },
                    onAssignTask = {
                        reportToAssign = selectedReport
                        selectedReport = null
                        showAddScheduleDialog = true
                    }
                )
            }

            if (showAddDriverDialog) {
                AddDriverDialog(
                    onDismiss = { showAddDriverDialog = false },
                    onDriverAdded = { driver ->
                        scope.launch {
                            val newDriverRef = db.child("drivers").push()
                            val driverData = mapOf(
                                "name" to driver.name,
                                "truck" to driver.truck,
                                "status" to "Active",
                                "statusColor" to 0xFF4CAF50,
                                "profileImage" to driver.profileImage
                            )
                            newDriverRef.setValue(driverData).await()
                        }
                    }
                )
            }

            if (editingDriver != null) {
                AddDriverDialog(
                    initialDriver = editingDriver,
                    onDismiss = { editingDriver = null },
                    onDriverAdded = { updated ->
                        scope.launch {
                            val driverData = mapOf(
                                "name" to updated.name,
                                "truck" to updated.truck,
                                "profileImage" to updated.profileImage
                            )
                            db.child("drivers").child(editingDriver!!.id).updateChildren(driverData).await()
                        }
                    }
                )
            }

            if (showAddScheduleDialog) {
                AddScheduleDialog(
                    initialSchedule = reportToAssign?.let {
                        ScheduleData(
                            customerName = it.userName,
                            address = it.location,
                            date = "Today",
                            shift = "ASAP"
                        )
                    },
                    drivers = drivers,
                    onDismiss = { 
                        showAddScheduleDialog = false
                        reportToAssign = null
                    },
                    onScheduleAdded = { schedule ->
                        // Assign task to driver in Database
                        scope.launch {
                            try {
                                val taskData = hashMapOf(
                                    "customerName" to schedule.customerName,
                                    "address" to schedule.address,
                                    "wasteType" to "General Waste",
                                    "driverName" to schedule.driverName,
                                    "date" to schedule.date,
                                    "shift" to schedule.shift,
                                    "isCompleted" to false,
                                    "timestamp" to System.currentTimeMillis()
                                )
                                db.child("assigned_tasks").push().setValue(taskData).await()
                                
                                // If assigned from report, update report status
                                reportToAssign?.let {
                                    db.child("reports").child(it.id).child("status").setValue("Assigned")
                                }
                                
                                snackbarHostState.showSnackbar("Task assigned to ${schedule.driverName}")
                            } catch (e: Exception) {
                                Log.e("AdminDashboard", "Error assigning task", e)
                            }
                        }
                    }
                )
            }

            if (editingSchedule != null) {
                AddScheduleDialog(
                    initialSchedule = editingSchedule,
                    drivers = drivers,
                    onDismiss = { editingSchedule = null },
                    onScheduleAdded = { updated ->
                        scope.launch {
                            try {
                                val taskData = mapOf(
                                    "customerName" to updated.customerName,
                                    "address" to updated.address,
                                    "driverName" to updated.driverName,
                                    "date" to updated.date,
                                    "shift" to updated.shift
                                )
                                db.child("assigned_tasks").child(editingSchedule!!.id).updateChildren(taskData).await()
                                snackbarHostState.showSnackbar("Task updated")
                            } catch (e: Exception) {
                                Log.e("AdminDashboard", "Error updating task", e)
                            }
                        }
                        editingSchedule = null
                    }
                )
            }
        }
    }
}

@Composable
fun AdminDashboardOverview(
    onNavigate: (AdminSubScreen) -> Unit,
    drivers: List<Driver>,
    bins: List<BinData>,
    reports: List<ReportItem>,
    assignedTasks: List<ScheduleData>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Overview", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard("Active Drivers", drivers.count { it.status == "Active" }.toString(), Icons.Default.People, Modifier.weight(1f))
                StatCard("Completed Tasks", assignedTasks.count { it.isCompleted }.toString(), Icons.Default.CheckCircle, Modifier.weight(1f))
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard("Full Bins (>80%)", bins.count { it.fillLevel > 80 }.toString(), Icons.Default.Delete, Modifier.weight(1f))
                StatCard("Pending Tasks", assignedTasks.count { !it.isCompleted }.toString(), Icons.Default.Schedule, Modifier.weight(1f))
            }
        }

        item {
            StatCard(
                label = "Pending User Reports", 
                value = reports.count { it.status == "Pending" }.toString(), 
                icon = Icons.Default.Warning, 
                modifier = Modifier.fillMaxWidth(),
                onClick = { onNavigate(AdminSubScreen.Reports) }
            )
        }

        item {
            QuickActions(onNavigate)
        }

        item {
            Text("Recent Driver Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        items(drivers.take(3)) { driver ->
            DriverStatusItem(driver)
        }
    }
}

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, tint = GreenPrimary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

@Composable
fun QuickActions(onNavigate: (AdminSubScreen) -> Unit) {
    Column {
        Text("Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onNavigate(AdminSubScreen.Schedule) }, Modifier.weight(1f)) {
                Text("Schedules")
            }
            OutlinedButton(onClick = { onNavigate(AdminSubScreen.Reports) }, Modifier.weight(1f)) {
                Text("Reports")
            }
        }
    }
}

@Composable
fun DriverStatusItem(driver: Driver) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = driver.profileImage,
                contentDescription = null,
                modifier = Modifier.size(40.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(driver.name, fontWeight = FontWeight.Bold)
                Text(driver.truck, style = MaterialTheme.typography.bodySmall)
            }
            Surface(
                color = driver.statusColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    driver.status,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = driver.statusColor,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
fun ManageDriversScreen(
    drivers: List<Driver>, 
    onEditDriver: (Driver) -> Unit,
    onDeleteDriver: (Driver) -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(drivers) { driver ->
            DriverManagementCard(driver, onEditDriver, onDeleteDriver)
        }
    }
}

@Composable
fun DriverManagementCard(driver: Driver, onEditDriver: (Driver) -> Unit, onDeleteDriver: (Driver) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = driver.profileImage,
                contentDescription = null,
                modifier = Modifier.size(50.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(driver.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(driver.truck, style = MaterialTheme.typography.bodySmall)
                Text(driver.status, color = driver.statusColor, style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = { onEditDriver(driver) }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = GreenPrimary)
            }
            IconButton(onClick = { onDeleteDriver(driver) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
            }
        }
    }
}

@Composable
fun ReportDetailDialog(
    report: ReportItem, 
    onDismiss: () -> Unit, 
    onMarkAsResolved: () -> Unit,
    onAssignTask: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report Detail") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (report.imageUri.isNotEmpty()) {
                    AsyncImage(
                        model = report.imageUri,
                        contentDescription = "Report Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(8.dp))
                }

                Text("Type: ${report.issueType}", fontWeight = FontWeight.Bold, color = GreenDark)
                Text("User: ${report.userName}", style = MaterialTheme.typography.bodyMedium)
                Text("Location: ${report.location}")

                if (report.latitude != 0.0) {
                    Spacer(Modifier.height(8.dp))
                    Card(modifier = Modifier.fillMaxWidth().height(150.dp), shape = RoundedCornerShape(8.dp)) {
                        val reportLocation = LatLng(report.latitude, report.longitude)
                        val cameraPositionState = rememberCameraPositionState {
                            position = CameraPosition.fromLatLngZoom(reportLocation, 15f)
                        }
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraPositionState,
                            uiSettings = MapUiSettings(zoomControlsEnabled = false, scrollGesturesEnabled = false, tiltGesturesEnabled = false, rotationGesturesEnabled = false)
                        ) {
                            Marker(state = rememberMarkerState(position = reportLocation))
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(report.description)
                Spacer(Modifier.height(8.dp))
                Text("Status: ${report.status}", color = when(report.status) {
                    "Pending" -> Color.Red
                    "Assigned" -> Color(0xFF2196F3)
                    else -> Color(0xFF4CAF50)
                })
            }
        },
        confirmButton = {
            Row {
                if (report.status == "Pending") {
                    TextButton(onClick = onAssignTask) { Text("Assign Task") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onMarkAsResolved) { Text("Mark Resolved") }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun AddDriverDialog(
    initialDriver: Driver? = null,
    onDismiss: () -> Unit,
    onDriverAdded: (Driver) -> Unit
) {
    var name by remember { mutableStateOf(initialDriver?.name ?: "") }
    var truck by remember { mutableStateOf(initialDriver?.truck ?: "") }
    var imageUri by remember { mutableStateOf<Uri?>(initialDriver?.profileImage?.toUri()) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        imageUri = it
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialDriver == null) "Add New Driver" else "Edit Driver") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                TextField(value = truck, onValueChange = { truck = it }, label = { Text("Truck Number") })
                Button(onClick = { launcher.launch("image/*") }) {
                    Text("Select Profile Image")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onDriverAdded(Driver(id = initialDriver?.id ?: "", name = name, truck = truck, status = "Active", statusColor = GreenPrimary, profileImage = imageUri?.toString() ?: ""))
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AdminScheduleScreen(
    tasks: List<ScheduleData>, 
    onEditSchedule: (ScheduleData) -> Unit,
    onDeleteSchedule: (ScheduleData) -> Unit
) {
    Column {
        Text(
            "Assigned Pickup Tasks", 
            modifier = Modifier.padding(16.dp), 
            style = MaterialTheme.typography.titleMedium, 
            fontWeight = FontWeight.Bold
        )
        
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(tasks) { task ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(task.date, fontWeight = FontWeight.Bold)
                            Row {
                                IconButton(onClick = { onEditSchedule(task) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Edit, "Edit", tint = GreenPrimary, modifier = Modifier.size(16.dp))
                                }
                                Spacer(Modifier.width(8.dp))
                                IconButton(onClick = { onDeleteSchedule(task) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Delete, "Delete", tint = Color.Red, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(task.customerName, fontWeight = FontWeight.Bold)
                                Text(task.address, style = MaterialTheme.typography.bodySmall)
                                Text("Shift: ${task.shift}")
                                Text("Driver: ${task.driverName}", color = GreenPrimary, fontWeight = FontWeight.Bold)
                            }
                            Surface(
                                color = if (task.isCompleted) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    if (task.isCompleted) "Completed" else "Pending",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = if (task.isCompleted) Color(0xFF2E7D32) else Color(0xFFEF6C00),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScheduleDialog(
    initialSchedule: ScheduleData? = null,
    drivers: List<Driver>,
    onDismiss: () -> Unit,
    onScheduleAdded: (ScheduleData) -> Unit
) {
    var date by remember { mutableStateOf(initialSchedule?.date ?: "") }
    var shift by remember { mutableStateOf(initialSchedule?.shift ?: "") }
    var address by remember { mutableStateOf(initialSchedule?.address ?: "") }
    var customerName by remember { mutableStateOf(initialSchedule?.customerName ?: "") }
    var selectedDriver by remember { mutableStateOf(drivers.find { it.name == initialSchedule?.driverName }) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialSchedule == null) "Add Schedule" else "Edit Schedule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                TextField(value = customerName, onValueChange = { customerName = it }, label = { Text("Customer/Site Name") }, modifier = Modifier.fillMaxWidth())
                TextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
                TextField(value = date, onValueChange = { date = it }, label = { Text("Date (e.g., Jan 22)") }, modifier = Modifier.fillMaxWidth())
                TextField(value = shift, onValueChange = { shift = it }, label = { Text("Shift (Morning/Afternoon)") }, modifier = Modifier.fillMaxWidth())
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    TextField(
                        value = selectedDriver?.name ?: "Select Driver",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Driver") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        colors = ExposedDropdownMenuDefaults.textFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        drivers.forEach { driver ->
                            DropdownMenuItem(
                                text = { Text(driver.name) },
                                onClick = {
                                    selectedDriver = driver
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedDriver != null) {
                        onScheduleAdded(ScheduleData(
                    customerName = customerName,
                    address = address,
                    date = date,
                    shift = shift,
                    driverName = selectedDriver!!.name
                ))
                    }
                },
                enabled = date.isNotEmpty() && shift.isNotEmpty() && selectedDriver != null && customerName.isNotEmpty()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ManageReportsScreen(reports: List<ReportItem>, onReportClick: (ReportItem) -> Unit) {
    if (reports.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No reports found", color = Color.Gray)
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(reports) { report ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onReportClick(report) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (report.status == "Pending") Color(0xFFFFF3E0) else Color.White
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(report.issueType, fontWeight = FontWeight.Bold, color = GreenDark)
                            if (report.status == "Pending") {
                                Surface(color = Color.Red, shape = CircleShape) {
                                    Text("NEW", color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Text(report.userName, style = MaterialTheme.typography.bodySmall)
                        Text(report.location, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(4.dp))
                        Text(report.description, maxLines = 1, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminNotificationsScreen(reports: List<ReportItem>) {
    val pendingReports = reports.filter { it.status == "Pending" }
    
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Recent Activity", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        
        if (pendingReports.isEmpty()) {
            item {
                Text("No new notifications", color = Color.Gray, modifier = Modifier.padding(16.dp))
            }
        } else {
            items(pendingReports) { report ->
                NotificationItemCard(
                    title = "New Report: ${report.issueType}",
                    message = "${report.userName} reported an issue at ${report.location}",
                    time = "Just now"
                )
            }
        }
        
        item {
            NotificationItemCard(
                title = "System Update",
                message = "The weekly pickup schedule has been generated.",
                time = "2 hours ago"
            )
        }
    }
}

@Composable
fun NotificationItemCard(title: String, message: String, time: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = GreenPrimary.copy(alpha = 0.1f), modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Notifications, null, tint = GreenPrimary, modifier = Modifier.padding(8.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(message, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text(time, style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
            }
        }
    }
}

@Composable
fun ManageBinsScreen(bins: List<BinData>) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(bins) { bin ->
            BinStatusCard(bin)
        }
    }
}

@Composable
fun BinStatusCard(bin: BinData) {
    val levelColor = when {
        bin.fillLevel > 80 -> Color.Red
        bin.fillLevel > 50 -> Color(0xFFFF9800)
        else -> Color(0xFF4CAF50)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(levelColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = levelColor)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(bin.id, fontWeight = FontWeight.Bold)
                Text(bin.location, style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${bin.fillLevel}%", fontWeight = FontWeight.Bold, color = levelColor)
                Text("Full", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun AdminMapView(bins: List<BinData>, drivers: List<Driver>) {
    val kampala = LatLng(0.3136, 32.5811)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(kampala, 13f)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(mapType = MapType.NORMAL),
            uiSettings = MapUiSettings(zoomControlsEnabled = true)
        ) {
            // Add markers for bins
            bins.forEach { bin ->
                val color = when {
                    bin.fillLevel > 80 -> 0f // Hue for Red
                    bin.fillLevel > 50 -> 30f // Hue for Orange
                    else -> 120f // Hue for Green
                }
                Marker(
                    state = MarkerState(position = LatLng(bin.lat, bin.lng)),
                    title = "Bin: ${bin.id}",
                    snippet = "Fill Level: ${bin.fillLevel}%",
                    icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(color)
                )
            }
        }
        
        // Legend
        Card(
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.BottomStart),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text("Legend", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp).background(Color.Red, CircleShape))
                    Spacer(Modifier.width(4.dp))
                    Text("Critical (>80%)", fontSize = 10.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp).background(Color(0xFFFF9800), CircleShape))
                    Spacer(Modifier.width(4.dp))
                    Text("Warning (50-80%)", fontSize = 10.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp).background(Color(0xFF4CAF50), CircleShape))
                    Spacer(Modifier.width(4.dp))
                    Text("Good (<50%)", fontSize = 10.sp)
                }
            }
        }
    }
}

// Data Models
data class Driver(val id: String = "", val name: String, val truck: String, val status: String, val statusColor: Color, val profileImage: String, val lat: Double = 0.0, val lng: Double = 0.0)
data class BinData(val id: String, val location: String, val fillLevel: Int, val lat: Double = 0.0, val lng: Double = 0.0)
data class ScheduleData(
    val id: String = "", 
    val date: String, 
    val shift: String, 
    val driverName: String, 
    val isCompleted: Boolean = false,
    val address: String = "Scheduled Route",
    val customerName: String = "Scheduled Pickup"
)
data class ReportItem(
    val id: String = "",
    val userName: String,
    val location: String,
    val description: String,
    val status: String = "Pending",
    val issueType: String = "General",
    val timestamp: Long = 0L,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val imageUri: String = ""
)

@Preview(showBackground = true)
@Composable
fun AdminDashboardPreview() {
    SmartWasteTheme {
        AdminDashboardScreen(onLogout = {})
    }
}
