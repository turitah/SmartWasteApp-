package com.example.smartwaste.admin

import android.net.Uri
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

enum class AdminSubScreen {
    Dashboard, Drivers, MapView, Schedule, Reports, Bins
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

    // State for drivers, schedules, and bins
    var drivers by remember {
        mutableStateOf(
            listOf(
                Driver(name = "Niwamanya Joel", truck = "Truck #001", status = "Active", statusColor = Color(0xFF4CAF50), profileImage = "https://randomuser.me/api/portraits/men/32.jpg"),
                Driver(name = "Moola Joseph", truck = "Truck #005", status = "On Break", statusColor = Color(0xFFFF9800), profileImage = "https://randomuser.me/api/portraits/women/44.jpg"),
                Driver(name = "Okello Peter", truck = "Truck #003", status = "En Route", statusColor = Color(0xFF2196F3), profileImage = "https://randomuser.me/api/portraits/men/46.jpg"),
                Driver(name = "Musa Juma", truck = "Truck #008", status = "Offline", statusColor = Color(0xFF9E9E9E), profileImage = "https://randomuser.me/api/portraits/men/86.jpg")
            )
        )
    }

    var bins by remember {
        mutableStateOf(
            listOf(
                BinData("Bin-001", "Acacia Ave", 85),
                BinData("Bin-002", "Bugolobi Village", 40),
                BinData("Bin-003", "Kiwatule Road", 15),
                BinData("Bin-004", "Ntinda St", 92),
                BinData("Bin-005", "Makerere Main", 60)
            )
        )
    }

    var schedules by remember {
        mutableStateOf(
            listOf(
                ScheduleData("Mon, Jan 22", "Morning", "John Kato"),
                ScheduleData("Tue, Jan 23", "Afternoon", "Sarah Namuli"),
                ScheduleData("Wed, Jan 24", "Morning", "Peter Okello")
            )
        )
    }

    Scaffold(
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
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(Icons.Default.Menu, "Menu", tint = Color.White)
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
                                text = { Text("Reports") },
                                onClick = {
                                    currentSubScreen = AdminSubScreen.Reports
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Report, null) }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Logout", color = Color.Red) },
                                onClick = {
                                    showMenu = false
                                    onLogout()
                                },
                                leadingIcon = { Icon(Icons.Default.Logout, null, tint = Color.Red) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenPrimary)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (currentSubScreen) {
                AdminSubScreen.Dashboard -> {
                    AdminDashboardOverview(
                        onNavigate = { currentSubScreen = it },
                        drivers = drivers,
                        bins = bins
                    )
                }
                AdminSubScreen.Drivers -> ManageDriversScreen(drivers, onEditDriver = { editingDriver = it })
                AdminSubScreen.Schedule -> AdminScheduleScreen(
                    schedules,
                    onEditSchedule = { editingSchedule = it }
                )
                AdminSubScreen.MapView -> Text("Map View Coming Soon", modifier = Modifier.padding(16.dp))
                AdminSubScreen.Reports -> ManageReportsScreen(onReportClick = { selectedReport = it })
                AdminSubScreen.Bins -> ManageBinsScreen(bins)
            }

            if (selectedReport != null) {
                ReportDetailDialog(
                    report = selectedReport!!,
                    onDismiss = { selectedReport = null }
                )
            }

            if (showAddDriverDialog) {
                AddDriverDialog(
                    onDismiss = { showAddDriverDialog = false },
                    onDriverAdded = { drivers = drivers + it }
                )
            }

            if (editingDriver != null) {
                AddDriverDialog(
                    initialDriver = editingDriver,
                    onDismiss = { editingDriver = null },
                    onDriverAdded = { updated ->
                        drivers = drivers.map { if (it.name == editingDriver?.name) updated else it }
                        editingDriver = null
                    }
                )
            }

            if (showAddScheduleDialog) {
                AddScheduleDialog(
                    onDismiss = { showAddScheduleDialog = false },
                    onScheduleAdded = { schedules = schedules + it }
                )
            }

            if (editingSchedule != null) {
                AddScheduleDialog(
                    initialSchedule = editingSchedule,
                    onDismiss = { editingSchedule = null },
                    onScheduleAdded = { updated ->
                        schedules = schedules.map { if (it.date == editingSchedule?.date) updated else it }
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
    bins: List<BinData>
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
                StatCard("Full Bins", bins.count { it.fillLevel > 80 }.toString(), Icons.Default.Delete, Modifier.weight(1f))
            }
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
fun StatCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = GreenPrimary.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, tint = GreenPrimary)
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall)
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
fun ManageDriversScreen(drivers: List<Driver>, onEditDriver: (Driver) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(drivers) { driver ->
            DriverStatusItem(driver) // Reusing the card, could add edit button
        }
    }
}

@Composable
fun ReportDetailDialog(report: ReportItem, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report Detail") },
        text = {
            Column {
                Text("User: ${report.userName}", fontWeight = FontWeight.Bold)
                Text("Location: ${report.location}")
                Spacer(Modifier.height(8.dp))
                Text(report.description)
            }
        },
        confirmButton = {
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
                onDriverAdded(Driver(name, truck, "Active", GreenPrimary, imageUri?.toString() ?: ""))
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AdminScheduleScreen(schedules: List<ScheduleData>, onEditSchedule: (ScheduleData) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(schedules) { schedule ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(schedule.date, fontWeight = FontWeight.Bold)
                    Text("Shift: ${schedule.shift}")
                    Text("Driver: ${schedule.driverName}")
                }
            }
        }
    }
}

@Composable
fun AddScheduleDialog(
    initialSchedule: ScheduleData? = null,
    onDismiss: () -> Unit,
    onScheduleAdded: (ScheduleData) -> Unit
) {
    var date by remember { mutableStateOf(initialSchedule?.date ?: "") }
    var shift by remember { mutableStateOf(initialSchedule?.shift ?: "") }
    var driverName by remember { mutableStateOf(initialSchedule?.driverName ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialSchedule == null) "Add Schedule" else "Edit Schedule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(value = date, onValueChange = { date = it }, label = { Text("Date") })
                TextField(value = shift, onValueChange = { shift = it }, label = { Text("Shift") })
                TextField(value = driverName, onValueChange = { driverName = it }, label = { Text("Driver Name") })
            }
        },
        confirmButton = {
            Button(onClick = {
                onScheduleAdded(ScheduleData(date, shift, driverName))
                onDismiss()
            }) { Text("Save") }
        }
    )
}

@Composable
fun ManageReportsScreen(onReportClick: (ReportItem) -> Unit) {
    val reports = listOf(
        ReportItem("John Doe", "Acacia Ave", "Full bin"),
        ReportItem("Alice Smith", "Kiwatule", "Bad smell")
    )
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(reports) { report ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onReportClick(report) }
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(report.userName, fontWeight = FontWeight.Bold)
                    Text(report.location)
                }
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

// Data Models
data class Driver(val name: String, val truck: String, val status: String, val statusColor: Color, val profileImage: String)
data class BinData(val id: String, val location: String, val fillLevel: Int)
data class ScheduleData(val date: String, val shift: String, val driverName: String)
data class ReportItem(val userName: String, val location: String, val description: String)

@Preview(showBackground = true)
@Composable
fun AdminDashboardPreview() {
    SmartWasteTheme {
        AdminDashboardScreen(onLogout = {})
    }
}
