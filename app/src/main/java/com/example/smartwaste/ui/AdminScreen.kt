package com.example.smartwaste.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.smartwaste.ui.theme.GreenDark
import com.example.smartwaste.ui.theme.GreenPrimary
import com.example.smartwaste.ui.theme.SmartWasteTheme

enum class AdminSubScreen {
    Dashboard, Drivers, MapView, Schedule, Reports, Bins
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(onBack: () -> Unit) {
    var currentSubScreen by remember { mutableStateOf(AdminSubScreen.Dashboard) }
    var selectedReport by remember { mutableStateOf<ReportItem?>(null) }
    var showAddDriverDialog by remember { mutableStateOf(false) }
    var showAddScheduleDialog by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<ScheduleData?>(null) }
    var editingDriver by remember { mutableStateOf<Driver?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    // State for drivers, schedules, and bins
    var drivers by remember {
        mutableStateOf(
            listOf(
                Driver(name = "John Kato", truck = "Truck #001", status = "Active", statusColor = Color(0xFF4CAF50), profileImage = "https://randomuser.me/api/portraits/men/32.jpg"),
                Driver(name = "Sarah Namuli", truck = "Truck #005", status = "On Break", statusColor = Color(0xFFFF9800), profileImage = "https://randomuser.me/api/portraits/women/44.jpg"),
                Driver(name = "Peter Okello", truck = "Truck #003", status = "En Route", statusColor = Color(0xFF2196F3), profileImage = "https://randomuser.me/api/portraits/men/46.jpg"),
                Driver(name = "Musa Juma", truck = "Truck #008", status = "Offline", statusColor = Color(0xFF9E9E9E), profileImage = "https://randomuser.me/api/portraits/men/86.jpg")
            )
        )
    }

    var bins by remember {
        mutableStateOf(
            listOf(
                BinData("Bin #101", "Kampala Road", 85, "High"),
                BinData("Bin #102", "Entebbe St", 40, "Normal"),
                BinData("Bin #103", "Wandegeya", 95, "Critical"),
                BinData("Bin #104", "Makerere", 10, "Normal"),
                BinData("Bin #105", "Kisekka Market", 70, "High")
            )
        )
    }

    var schedules by remember {
        mutableStateOf(
            listOf(
                ScheduleData(route = "Route A - Central", time = "08:00 AM", type = "Organic", driver = "Driver: John Kato"),
                ScheduleData(route = "Route B - North", time = "10:30 AM", type = "Recyclables", driver = "Driver: Sarah Namuli"),
                ScheduleData(route = "Route C - South", time = "02:00 PM", type = "General", driver = "Driver: Peter Okello")
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentSubScreen) {
                            AdminSubScreen.Dashboard -> "Admin Console"
                            AdminSubScreen.Drivers -> "Manage Drivers"
                            AdminSubScreen.MapView -> "Waste Map"
                            AdminSubScreen.Schedule -> "Collection Schedule"
                            AdminSubScreen.Reports -> "User Reports"
                            AdminSubScreen.Bins -> "Bin Management"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentSubScreen == AdminSubScreen.Dashboard) onBack()
                        else currentSubScreen = AdminSubScreen.Dashboard
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Refresh data */ }) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Reports") },
                                onClick = {
                                    currentSubScreen = AdminSubScreen.Reports
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Report, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Bins") },
                                onClick = {
                                    currentSubScreen = AdminSubScreen.Bins
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, null) }
                            )
                            Divider()
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
                                leadingIcon = { Icon(Icons.Default.CalendarToday, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Maps") },
                                onClick = {
                                    currentSubScreen = AdminSubScreen.MapView
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Map, null) }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                onClick = {
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Settings, null) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenDark)
            )
        },
        floatingActionButton = {
            when (currentSubScreen) {
                AdminSubScreen.Drivers -> {
                    ExtendedFloatingActionButton(
                        onClick = { showAddDriverDialog = true },
                        containerColor = GreenPrimary,
                        contentColor = Color.White,
                        icon = { Icon(Icons.Default.Add, null) },
                        text = { Text("Add Driver") }
                    )
                }
                AdminSubScreen.Schedule -> {
                    ExtendedFloatingActionButton(
                        onClick = { showAddScheduleDialog = true },
                        containerColor = GreenPrimary,
                        contentColor = Color.White,
                        icon = { Icon(Icons.Default.AddCircle, null) },
                        text = { Text("New Route") }
                    )
                }
                else -> {}
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentSubScreen) {
                AdminSubScreen.Dashboard -> DashboardContent(
                    onReportClick = { selectedReport = it }
                )
                AdminSubScreen.Drivers -> ManageDriversScreen(drivers, onEditDriver = { editingDriver = it })
                AdminSubScreen.MapView -> AdminMapView()
                AdminSubScreen.Schedule -> AdminScheduleScreen(
                    schedules = schedules,
                    onEditSchedule = { editingSchedule = it }
                )
                AdminSubScreen.Reports -> ManageReportsScreen(onReportClick = { selectedReport = it })
                AdminSubScreen.Bins -> ManageBinsScreen(bins)
            }

            if (selectedReport != null) {
                ReportDetailDialog(
                    report = selectedReport!!,
                    onDismiss = { selectedReport = null }
                )
            }

            if (showAddDriverDialog || editingDriver != null) {
                AddDriverDialog(
                    initialDriver = editingDriver,
                    onDismiss = {
                        showAddDriverDialog = false
                        editingDriver = null
                    },
                    onSave = { name, truck, imageUri ->
                        if (editingDriver != null) {
                            drivers = drivers.map {
                                if (it.id == editingDriver!!.id) it.copy(name = name, truck = truck, profileImage = imageUri) else it
                            }
                        } else {
                            drivers = drivers + Driver(
                                name = name,
                                truck = truck,
                                status = "Offline",
                                statusColor = Color(0xFF9E9E9E),
                                profileImage = imageUri
                            )
                        }
                        showAddDriverDialog = false
                        editingDriver = null
                    }
                )
            }

            if (showAddScheduleDialog || editingSchedule != null) {
                AddScheduleDialog(
                    initialSchedule = editingSchedule,
                    onDismiss = {
                        showAddScheduleDialog = false
                        editingSchedule = null
                    },
                    onSave = { route, time ->
                        if (editingSchedule != null) {
                            schedules = schedules.map {
                                if (it.id == editingSchedule!!.id) it.copy(route = route, time = time) else it
                            }
                        } else {
                            schedules = schedules + ScheduleData(route = route, time = time, type = "General", driver = "Unassigned")
                        }
                        showAddScheduleDialog = false
                        editingSchedule = null
                    }
                )
            }
        }
    }
}

@Composable
fun AdminMapView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE0E0E0)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
            Spacer(Modifier.height(16.dp))
            Text("Interactive Waste Map", fontWeight = FontWeight.Bold)
            Text("Visualizing bin levels across the city", color = Color.Gray)
        }
    }
}

@Composable
fun AddDriverDialog(initialDriver: Driver? = null, onDismiss: () -> Unit, onSave: (String, String, String?) -> Unit) {
    var name by remember { mutableStateOf(initialDriver?.name ?: "") }
    var truck by remember { mutableStateOf(initialDriver?.truck ?: "") }
    var imageUri by remember { mutableStateOf<Uri?>(initialDriver?.profileImage?.let { Uri.parse(it) }) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialDriver == null) "Add New Driver" else "Edit Driver", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray)
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUri != null) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = Color.Gray)
                    }
                }
                Text("Tap to upload photo", fontSize = 12.sp, color = Color.Gray)

                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = truck, onValueChange = { truck = it }, label = { Text("Truck Number") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank() && truck.isNotBlank()) onSave(name, truck, imageUri?.toString()) },
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
            ) {
                Text(if (initialDriver == null) "Save" else "Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddScheduleDialog(initialSchedule: ScheduleData? = null, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var route by remember { mutableStateOf(initialSchedule?.route ?: "") }
    var time by remember { mutableStateOf(initialSchedule?.time ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialSchedule == null) "Create New Route" else "Edit Route", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = route, onValueChange = { route = it }, label = { Text("Route Name") })
                OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Scheduled Time") })
            }
        },
        confirmButton = {
            Button(
                onClick = { if (route.isNotBlank() && time.isNotBlank()) onSave(route, time) },
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
            ) {
                Text(if (initialSchedule == null) "Schedule" else "Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun DashboardContent(
    onReportClick: (ReportItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Stats Overview
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard("Total Reports", "42", Icons.Default.Report, Color(0xFFE57373), Modifier.weight(1f))
            StatCard("Active Trucks", "8", Icons.Default.LocalShipping, Color(0xFF64B5F6), Modifier.weight(1f))
            StatCard("Full Bins", "15", Icons.Default.Delete, Color(0xFF81C784), Modifier.weight(1f))
        }

        Text(
            "Recent Waste Reports",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // Reports List
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val reports = listOf(
                ReportItem("Overflowing Bin", "Kampala Road", "High", "2 mins ago", "A bin near the taxi park is overflowing with plastic waste."),
                ReportItem("Illegal Dumping", "Entebbe St", "Medium", "15 mins ago", "Someone dumped old furniture on the roadside."),
                ReportItem("Missed Pickup", "Wandegeya", "Low", "1 hour ago", "The truck didn't show up this morning at Zone A."),
                ReportItem("Broken Bin", "Makerere", "Medium", "3 hours ago", "The lid of the organic waste bin is broken."),
                ReportItem("Overflowing Bin", "Kisekka Market", "High", "5 hours ago", "Heavy waste accumulation due to market activities.")
            )
            items(reports) { report ->
                AdminReportCard(report, onClick = { onReportClick(report) })
            }
        }
    }
}

@Composable
fun ManageDriversScreen(drivers: List<Driver>, onEditDriver: (Driver) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(drivers) { driver ->
            DriverCard(driver, onEdit = { onEditDriver(driver) })
        }
    }
}

@Composable
fun ReportDetailDialog(report: ReportItem, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(report.title, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Location: ${report.location}", fontWeight = FontWeight.Medium)
                Text("Priority: ${report.priority}", color = if (report.priority == "High") Color.Red else Color.Unspecified)
                Spacer(Modifier.height(8.dp))
                Text(report.description)
                Spacer(Modifier.height(16.dp))
                Text("Assign to Driver:", fontWeight = FontWeight.Bold)
                // In a real app, this would be a dropdown
                OutlinedButton(onClick = { /* Assign */ }, modifier = Modifier.fillMaxWidth()) {
                    Text("Choose Driver")
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)) {
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

data class Driver(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val truck: String,
    val status: String,
    val statusColor: Color,
    val profileImage: String? = null
)

@Composable
fun DriverCard(driver: Driver, onEdit: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(50.dp),
                shape = CircleShape,
                color = GreenPrimary.copy(alpha = 0.1f)
            ) {
                if (driver.profileImage != null) {
                    AsyncImage(
                        model = driver.profileImage,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = GreenPrimary,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(driver.name, fontWeight = FontWeight.Bold)
                Text(driver.truck, fontSize = 12.sp, color = Color.Gray)
            }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = driver.statusColor.copy(alpha = 0.1f)
            ) {
                Text(
                    driver.status,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = driver.statusColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(label, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

data class ReportItem(val title: String, val location: String, val priority: String, val time: String, val description: String)

@Composable
fun AdminReportCard(report: ReportItem, onClick: () -> Unit) {
    val priorityColor = when (report.priority) {
        "High" -> Color(0xFFF44336)
        "Medium" -> Color(0xFFFF9800)
        else -> Color(0xFF4CAF50)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(priorityColor, CircleShape)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(report.title, fontWeight = FontWeight.Bold)
                Text(report.location, fontSize = 14.sp, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(report.time, fontSize = 12.sp, color = Color.Gray)
                Text(report.priority, fontSize = 12.sp, color = priorityColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AdminActionButton(label: String, icon: ImageVector, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .background(GreenPrimary.copy(alpha = 0.1f), CircleShape)
        ) {
            Icon(icon, contentDescription = null, tint = GreenPrimary)
        }
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun AdminScheduleScreen(schedules: List<ScheduleData>, onEditSchedule: (ScheduleData) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(schedules) { schedule ->
            AdminScheduleCard(schedule, onEdit = { onEditSchedule(schedule) })
        }
    }
}

data class ScheduleData(
    val id: String = java.util.UUID.randomUUID().toString(),
    val route: String,
    val time: String,
    val type: String,
    val driver: String
)

@Composable
fun AdminScheduleCard(schedule: ScheduleData, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Map,
                contentDescription = null,
                tint = GreenPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(schedule.route, fontWeight = FontWeight.Bold)
                Text("${schedule.time} • ${schedule.type}", fontSize = 14.sp, color = Color.Gray)
                Text(schedule.driver, fontSize = 12.sp, color = GreenPrimary)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Gray)
            }
        }
    }
}

@Composable
fun ManageReportsScreen(onReportClick: (ReportItem) -> Unit) {
    val reports = listOf(
        ReportItem("Overflowing Bin", "Kampala Road", "High", "2 mins ago", "A bin near the taxi park is overflowing with plastic waste."),
        ReportItem("Illegal Dumping", "Entebbe St", "Medium", "15 mins ago", "Someone dumped old furniture on the roadside."),
        ReportItem("Missed Pickup", "Wandegeya", "Low", "1 hour ago", "The truck didn't show up this morning at Zone A."),
        ReportItem("Broken Bin", "Makerere", "Medium", "3 hours ago", "The lid of the organic waste bin is broken."),
        ReportItem("Overflowing Bin", "Kisekka Market", "High", "5 hours ago", "Heavy waste accumulation due to market activities.")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(reports) { report ->
            AdminReportCard(report, onClick = { onReportClick(report) })
        }
    }
}

data class BinData(
    val id: String,
    val location: String,
    val fillLevel: Int,
    val status: String
)

@Composable
fun ManageBinsScreen(bins: List<BinData>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(bins) { bin ->
            BinCard(bin)
        }
    }
}

@Composable
fun BinCard(bin: BinData) {
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
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(levelColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = levelColor)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(bin.id, fontWeight = FontWeight.Bold)
                Text(bin.location, fontSize = 14.sp, color = Color.Gray)
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { bin.fillLevel / 100f },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = levelColor,
                    trackColor = levelColor.copy(alpha = 0.2f),
                )
            }
            Spacer(Modifier.width(16.dp))
            Text("${bin.fillLevel}%", fontWeight = FontWeight.Bold, color = levelColor)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AdminDashboardPreview() {
    SmartWasteTheme {
        AdminDashboardScreen(onBack = {})
    }
}
