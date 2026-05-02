package com.example.smartwaste.admin

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.smartwaste.ui.theme.GreenDark
import com.example.smartwaste.ui.theme.GreenPrimary
import com.example.smartwaste.ui.theme.SmartWasteTheme
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

enum class AdminSubScreen {
    Dashboard, Drivers, MapView, Schedule, Reports, Bins, Notifications
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(onLogout: () -> Unit, viewModel: AdminViewModel = viewModel()) {
    val reports by viewModel.reports.collectAsState()
    val drivers = viewModel.drivers
    val bins = viewModel.bins
    val schedules = viewModel.schedules
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var currentSubScreen by remember { mutableStateOf(AdminSubScreen.Dashboard) }
    var selectedReport by remember { mutableStateOf<ReportItem?>(null) }
    var showAddDriverDialog by remember { mutableStateOf(false) }
    var showAddBinDialog by remember { mutableStateOf(false) }
    var showAddScheduleDialog by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<ScheduleData?>(null) }
    var editingDriver by remember { mutableStateOf<Driver?>(null) }
    var editingBin by remember { mutableStateOf<BinData?>(null) }
    var focusedLocation by remember { mutableStateOf<LatLng?>(null) }

    val unreadReportCount = reports.count { it.status == "Pending" && it.id !in viewModel.viewedReportIds }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle new report notifications
    LaunchedEffect(Unit) {
        viewModel.newReportEvent.collect { report ->
            val result = snackbarHostState.showSnackbar(
                message = "New ${report.issueType} report from ${report.userName}",
                actionLabel = "View",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                currentSubScreen = AdminSubScreen.Reports
                selectedReport = report
                viewModel.markReportAsViewed(report.id)
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                AdminDrawerHeader()
                Spacer(Modifier.height(16.dp))
                
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Dashboard Overview") },
                    selected = currentSubScreen == AdminSubScreen.Dashboard,
                    onClick = {
                        currentSubScreen = AdminSubScreen.Dashboard
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Map, contentDescription = "Map") },
                    label = { Text("Live Bin Map") },
                    selected = currentSubScreen == AdminSubScreen.MapView,
                    onClick = {
                        currentSubScreen = AdminSubScreen.MapView
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.People, contentDescription = "Drivers") },
                    label = { Text("Manage Drivers") },
                    selected = currentSubScreen == AdminSubScreen.Drivers,
                    onClick = {
                        currentSubScreen = AdminSubScreen.Drivers
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Schedule") },
                    label = { Text("Pickup Schedule") },
                    selected = currentSubScreen == AdminSubScreen.Schedule,
                    onClick = {
                        currentSubScreen = AdminSubScreen.Schedule
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Delete, contentDescription = "Bins") },
                    label = { Text("Waste Bins") },
                    selected = currentSubScreen == AdminSubScreen.Bins,
                    onClick = {
                        currentSubScreen = AdminSubScreen.Bins
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { 
                        BadgedBox(badge = { if (unreadReportCount > 0) Badge { Text(unreadReportCount.toString()) } }) {
                            Icon(Icons.Default.Report, contentDescription = "Reports")
                        }
                    },
                    label = { Text("User Reports") },
                    selected = currentSubScreen == AdminSubScreen.Reports,
                    onClick = {
                        currentSubScreen = AdminSubScreen.Reports
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Notifications, contentDescription = "Notifications") },
                    label = { Text("Notifications") },
                    selected = currentSubScreen == AdminSubScreen.Notifications,
                    onClick = {
                        currentSubScreen = AdminSubScreen.Notifications
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                
                Spacer(Modifier.weight(1f))
                
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                NavigationDrawerItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = Color.Red) },
                    label = { Text("Logout", color = Color.Red) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onLogout()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    ) {
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
                                AdminSubScreen.Notifications -> "Notifications"
                            },
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, "Menu", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenPrimary)
                )
            },
            floatingActionButton = {
                if (currentSubScreen == AdminSubScreen.Schedule) {
                    FloatingActionButton(
                        onClick = { showAddScheduleDialog = true },
                        containerColor = GreenPrimary,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Add, "Add Schedule", tint = Color.White)
                    }
                } else if (currentSubScreen == AdminSubScreen.Drivers) {
                    FloatingActionButton(
                        onClick = { showAddDriverDialog = true },
                        containerColor = GreenPrimary,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Person, "Add Driver", tint = Color.White)
                    }
                } else if (currentSubScreen == AdminSubScreen.Bins) {
                    FloatingActionButton(
                        onClick = { showAddBinDialog = true },
                        containerColor = GreenPrimary,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Add, "Add Bin", tint = Color.White)
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
                            reports = reports
                        )
                    }
                    AdminSubScreen.Drivers -> ManageDriversScreen(drivers, onEditDriver = { editingDriver = it })
                    AdminSubScreen.Schedule -> AdminScheduleScreen(
                        schedules = schedules,
                        onEditSchedule = { editingSchedule = it },
                        onDeleteSchedule = { viewModel.deleteSchedule(it) }
                    )
                    AdminSubScreen.MapView -> AdminBinMap(bins, targetLocation = focusedLocation)
                    AdminSubScreen.Reports -> ManageReportsScreen(
                        reports = reports,
                        onReportClick = { 
                            selectedReport = it
                            viewModel.markReportAsViewed(it.id)
                        }
                    )
                    AdminSubScreen.Bins -> ManageBinsScreen(
                        bins = bins,
                        onEditBin = { editingBin = it },
                        onLocateBin = { bin ->
                            focusedLocation = LatLng(bin.latitude, bin.longitude)
                            currentSubScreen = AdminSubScreen.MapView
                        }
                    )
                    AdminSubScreen.Notifications -> {
                        AdminNotificationsScreen(reports, viewModel.viewedReportIds, onClear = { viewModel.clearNotifications() })
                    }
                }

                if (selectedReport != null) {
                    ReportDetailDialog(
                        report = selectedReport!!,
                        onDismiss = { selectedReport = null },
                        onMarkAsResolved = { notes ->
                            viewModel.markAsResolved(selectedReport!!.id, notes)
                            selectedReport = null
                        },
                        onDelete = {
                            viewModel.deleteReport(selectedReport!!.id)
                            selectedReport = null
                        },
                        onUpdateNotes = { notes ->
                            viewModel.updateReportNotes(selectedReport!!.id, notes)
                        }
                    )
                }

                if (showAddDriverDialog) {
                    AddDriverDialog(
                        onDismiss = { showAddDriverDialog = false },
                        onDriverAdded = { viewModel.addDriver(it) }
                    )
                }

                if (editingDriver != null) {
                    AddDriverDialog(
                        initialDriver = editingDriver,
                        onDismiss = { editingDriver = null },
                        onDriverAdded = { updated ->
                            viewModel.updateDriver(editingDriver!!.name, updated)
                            editingDriver = null
                        }
                    )
                }

                if (showAddScheduleDialog) {
                    AddScheduleDialog(
                        onDismiss = { showAddScheduleDialog = false },
                        onScheduleAdded = { viewModel.addSchedule(it) }
                    )
                }

                if (editingSchedule != null) {
                    AddScheduleDialog(
                        initialSchedule = editingSchedule,
                        onDismiss = { editingSchedule = null },
                        onScheduleAdded = { updated ->
                            viewModel.updateSchedule(editingSchedule!!.date, updated)
                            editingSchedule = null
                        }
                    )
                }

                if (showAddBinDialog) {
                    AddBinDialog(
                        onDismiss = { showAddBinDialog = false },
                        onBinAdded = { viewModel.addBin(it) }
                    )
                }

                if (editingBin != null) {
                    AddBinDialog(
                        initialBin = editingBin,
                        onDismiss = { editingBin = null },
                        onBinAdded = { updated ->
                            viewModel.updateBin(editingBin!!.id, updated)
                            editingBin = null
                        },
                        onDeleteBin = {
                            viewModel.deleteBin(editingBin!!.id)
                            editingBin = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AdminDrawerHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(GreenPrimary)
            .padding(24.dp)
    ) {
        Column {
            AsyncImage(
                model = "https://randomuser.me/api/portraits/men/1.jpg",
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "System Admin",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "admin@smartwaste.com",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun AdminDashboardOverview(
    onNavigate: (AdminSubScreen) -> Unit,
    drivers: List<Driver>,
    bins: List<BinData>,
    reports: List<ReportItem>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text("Good Morning, Admin", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = GreenDark)
                Text("Here's what's happening with the fleet today.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard("Active Drivers", drivers.count { it.status == "Active" }.toString(), Icons.Default.People, Modifier.weight(1f))
                StatCard("Full Bins", bins.count { it.fillLevel > 80 }.toString(), Icons.Default.Delete, Modifier.weight(1f))
            }
        }

        item {
            StatCard(
                label = "Pending Reports", 
                value = reports.count { it.status == "Pending" }.toString(), 
                icon = Icons.Default.Warning, 
                modifier = Modifier.fillMaxWidth(),
                onClick = { onNavigate(AdminSubScreen.Reports) }
            )
        }

        item {
            WhatsNewSection()
        }

        item {
            QuickActions(onNavigate)
        }

        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Live Fleet Tracking", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { onNavigate(AdminSubScreen.MapView) }) {
                        Text("View Full Map", color = GreenPrimary)
                    }
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    AdminBinMap(bins, showLegend = false)
                }
            }
        }

        item {
            Text("Recent Driver Activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        items(drivers.take(3)) { driver ->
            DriverStatusItem(driver)
        }
    }
}

@Composable
fun WhatsNewSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GreenPrimary.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GreenPrimary.copy(alpha = 0.2f))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("What's New?", fontWeight = FontWeight.Bold, color = GreenDark)
                Text("Live bin tracking is now active! Check the Map View to see bin levels in real-time.", style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.AutoGraph, null, tint = GreenPrimary)
        }
    }
}

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(
                color = GreenPrimary.copy(alpha = 0.1f),
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(icon, null, tint = GreenPrimary, modifier = Modifier.padding(8.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = GreenDark)
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
fun DriverStatusItem(driver: Driver, onEdit: (() -> Unit)? = null) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onEdit != null) { onEdit?.invoke() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = driver.profileImage,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(driver.name, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalShipping, null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                    Spacer(Modifier.width(4.dp))
                    Text(driver.truck, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(12.dp), tint = GreenPrimary)
                    Spacer(Modifier.width(4.dp))
                    Text("Bins: ${driver.assignedBins}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        color = driver.statusColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            driver.status,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = driver.statusColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (onEdit != null) {
                    IconButton(onClick = { onEdit() }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Driver", tint = GreenPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun ManageDriversScreen(drivers: List<Driver>, onEditDriver: (Driver) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Fleet Management", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        items(drivers) { driver ->
            DriverStatusItem(driver, onEdit = { onEditDriver(driver) })
        }
    }
}

@Composable
fun ReportDetailDialog(
    report: ReportItem,
    onDismiss: () -> Unit,
    onMarkAsResolved: (String) -> Unit,
    onDelete: () -> Unit,
    onUpdateNotes: (String) -> Unit
) {
    var notes by remember { mutableStateOf(report.adminNotes) }
    var isEditingNotes by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Report Detail",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete Report", tint = Color.Red.copy(alpha = 0.6f))
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (report.status == "Pending") Color.Red else GreenPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        report.issueType,
                        fontWeight = FontWeight.Bold,
                        color = GreenDark,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                HorizontalDivider()
                DetailItem(Icons.Default.Person, "User", report.userName)
                DetailItem(Icons.Default.Place, "Location", report.location)
                DetailItem(Icons.Default.Info, "Status", report.status)

                Spacer(Modifier.height(8.dp))
                Text("User Description:", fontWeight = FontWeight.SemiBold)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Gray.copy(alpha = 0.05f))
                        .padding(12.dp)
                ) {
                    Text(report.description, style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Admin Notes:", fontWeight = FontWeight.SemiBold)
                    if (!isEditingNotes && report.status == "Pending") {
                        TextButton(onClick = { isEditingNotes = true }) {
                            Text("Edit", color = GreenPrimary)
                        }
                    }
                }

                if (isEditingNotes) {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Add resolution details...") },
                        shape = RoundedCornerShape(12.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { 
                            isEditingNotes = false
                            notes = report.adminNotes
                        }) {
                            Text("Cancel")
                        }
                        TextButton(onClick = {
                            onUpdateNotes(notes)
                            isEditingNotes = false
                        }) {
                            Text("Save", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(GreenPrimary.copy(alpha = 0.05f))
                            .padding(12.dp)
                    ) {
                        Text(
                            notes.ifEmpty { "No notes added yet." },
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = if (notes.isEmpty()) FontStyle.Italic else FontStyle.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (report.status == "Pending") {
                Button(
                    onClick = { onMarkAsResolved(notes) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) { Text("Mark Resolved") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun DetailItem(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(icon, null, modifier = Modifier.size(18.dp), tint = GreenPrimary)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun AddDriverDialog(
    initialDriver: Driver? = null,
    onDismiss: () -> Unit,
    onDriverAdded: (Driver) -> Unit
) {
    var name by remember { mutableStateOf(initialDriver?.name ?: "") }
    var truck by remember { mutableStateOf(initialDriver?.truck ?: "") }
    var assignedBins by remember { mutableStateOf(initialDriver?.assignedBins ?: "") }
    var imageUri by remember { mutableStateOf(initialDriver?.profileImage?.toUri()) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        imageUri = it
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                if (initialDriver == null) "Add New Driver" else "Edit Driver",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = GreenPrimary
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.Gray.copy(alpha = 0.1f))
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUri != null && imageUri.toString().isNotEmpty()) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(32.dp), tint = Color.Gray)
                    }
                }
                Text("Tap to upload photo", style = MaterialTheme.typography.labelSmall, color = Color.Gray)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = GreenPrimary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = truck,
                    onValueChange = { truck = it },
                    label = { Text("Truck Assignment") },
                    leadingIcon = { Icon(Icons.Default.LocalShipping, null, tint = GreenPrimary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = assignedBins,
                    onValueChange = { assignedBins = it },
                    label = { Text("Assigned Bins (e.g. Bin-001, Bin-004)") },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = GreenPrimary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDriverAdded(Driver(name, truck, initialDriver?.status ?: "Active", initialDriver?.statusColor ?: GreenPrimary, imageUri?.toString() ?: "", assignedBins))
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AdminScheduleScreen(
    schedules: List<ScheduleData>,
    onEditSchedule: (ScheduleData) -> Unit,
    onDeleteSchedule: (ScheduleData) -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Pickup Schedule", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        items(schedules) { schedule ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(GreenPrimary.copy(alpha = 0.1f)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.DateRange, null, tint = GreenPrimary)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(schedule.date, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(schedule.shift, color = GreenDark, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                            Spacer(Modifier.width(4.dp))
                            Text(schedule.driverName, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.LocalShipping, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                            Spacer(Modifier.width(4.dp))
                            Text(schedule.vehicle, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Column {
                        IconButton(onClick = { onEditSchedule(schedule) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = GreenPrimary)
                        }
                        IconButton(onClick = { onDeleteSchedule(schedule) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.6f))
                        }
                    }
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
    var vehicle by remember { mutableStateOf(initialSchedule?.vehicle ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                if (initialSchedule == null) "Add Schedule" else "Edit Schedule",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = GreenPrimary
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date") },
                    leadingIcon = { Icon(Icons.Default.DateRange, null, tint = GreenPrimary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = shift,
                    onValueChange = { shift = it },
                    label = { Text("Shift") },
                    leadingIcon = { Icon(Icons.Default.Info, null, tint = GreenPrimary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = driverName,
                    onValueChange = { driverName = it },
                    label = { Text("Driver Name") },
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = GreenPrimary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = vehicle,
                    onValueChange = { vehicle = it },
                    label = { Text("Vehicle (Truck #)") },
                    leadingIcon = { Icon(Icons.Default.LocalShipping, null, tint = GreenPrimary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onScheduleAdded(ScheduleData(date, shift, driverName, vehicle))
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ManageReportsScreen(reports: List<ReportItem>, onReportClick: (ReportItem) -> Unit) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Pending", "Resolved")
    
    val filteredReports = when (selectedTabIndex) {
        0 -> reports.filter { it.status == "Pending" }
        else -> reports.filter { it.status == "Resolved" }
    }

    Column {
        SecondaryTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.White,
            contentColor = GreenPrimary,
            indicator = {
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(selectedTabIndex),
                    color = GreenPrimary
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { 
                        Text(
                            title, 
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                        ) 
                    }
                )
            }
        }

        if (filteredReports.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No ${tabs[selectedTabIndex].lowercase()} reports found", color = Color.Gray)
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(filteredReports) { report ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onReportClick(report) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (report.status == "Pending") Color(0xFFFFF3E0) else Color.White
                        ),
                        elevation = CardDefaults.cardElevation(2.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (report.status == "Pending") Icons.Default.Warning else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (report.status == "Pending") Color.Red else GreenPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(report.issueType, fontWeight = FontWeight.Bold, color = GreenDark)
                                }
                                if (report.status == "Pending") {
                                    Surface(color = Color.Red, shape = CircleShape) {
                                        Text("NEW", color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(report.userName, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Place, null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                                Spacer(Modifier.width(4.dp))
                                Text(report.location, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(report.description, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                            
                            if (report.adminNotes.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                HorizontalDivider(thickness = 0.5.dp, color = Color.Gray.copy(alpha = 0.2f))
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Note: ${report.adminNotes}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GreenDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminNotificationsScreen(reports: List<ReportItem>, viewedIds: Set<String>, onClear: () -> Unit) {
    val newReports = reports.filter { it.status == "Pending" && it.id !in viewedIds }
    
    Column {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Notifications", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (newReports.isNotEmpty()) {
                TextButton(onClick = onClear) {
                    Text("Clear All", color = GreenPrimary)
                }
            }
        }

        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (newReports.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.NotificationsNone, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                            Text("All caught up!", color = Color.Gray)
                        }
                    }
                }
            } else {
                items(newReports) { report ->
                    NotificationItemCard(
                        title = "New Report: ${report.issueType}",
                        message = "${report.userName} reported an issue at ${report.location}",
                        time = "Just now"
                    )
                }
            }
            
            item {
                Text("Earlier", style = MaterialTheme.typography.labelMedium, color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
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
fun ManageBinsScreen(
    bins: List<BinData>,
    onEditBin: (BinData) -> Unit,
    onLocateBin: (BinData) -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Waste Bin Management", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        items(bins) { bin ->
            BinStatusCard(bin, onClick = { onEditBin(bin) }, onLocate = { onLocateBin(bin) })
        }
    }
}

@Composable
fun BinStatusCard(bin: BinData, onClick: () -> Unit, onLocate: () -> Unit) {
    val levelColor = when {
        bin.fillLevel > 80 -> Color.Red
        bin.fillLevel > 50 -> Color(0xFFFF9800)
        else -> Color(0xFF4CAF50)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(levelColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (bin.fillLevel > 80) Icons.Default.DeleteForever else Icons.Default.Delete,
                    contentDescription = null,
                    tint = levelColor,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(bin.id, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Place, null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                    Spacer(Modifier.width(4.dp))
                    Text(bin.location, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(12.dp), tint = GreenPrimary)
                    Spacer(Modifier.width(4.dp))
                    Text("Driver: ${bin.assignedDriver}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("${bin.fillLevel}%", fontWeight = FontWeight.ExtraBold, color = levelColor, fontSize = 20.sp)
                    Text("Full", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onLocate) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Locate Bin", tint = GreenPrimary)
                }
            }
        }
    }
}

@Composable
fun AdminBinMap(
    bins: List<BinData>,
    modifier: Modifier = Modifier.fillMaxSize(),
    showLegend: Boolean = true,
    targetLocation: LatLng? = null
) {
    val kampala = LatLng(0.3340, 32.5930)
    val scope = rememberCoroutineScope()
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(targetLocation ?: kampala, 12f)
    }

    // Auto-zoom to fit all bins if no specific target is set
    LaunchedEffect(bins) {
        if (targetLocation == null && bins.isNotEmpty()) {
            try {
                val builder = LatLngBounds.Builder()
                bins.forEach { builder.include(LatLng(it.latitude, it.longitude)) }
                val bounds = builder.build()
                cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 150))
            } catch (e: Exception) {
                // Handle case where bins might have invalid coordinates
            }
        }
    }

    // Animate to target location if provided (e.g., from "Locate Bin" button)
    LaunchedEffect(targetLocation) {
        targetLocation?.let {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(it, 16f))
        }
    }

    Box(modifier = modifier) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = true,
                myLocationButtonEnabled = true,
                mapToolbarEnabled = true
            ),
            properties = MapProperties(
                mapType = MapType.NORMAL,
                isMyLocationEnabled = false // Ensure permissions are handled if set to true
            )
        ) {
            bins.forEach { bin ->
                val markerColor = when {
                    bin.fillLevel > 80 -> BitmapDescriptorFactory.HUE_RED
                    bin.fillLevel > 50 -> BitmapDescriptorFactory.HUE_ORANGE
                    else -> BitmapDescriptorFactory.HUE_GREEN
                }

                Marker(
                    state = rememberMarkerState(position = LatLng(bin.latitude, bin.longitude)),
                    title = bin.id,
                    snippet = "${bin.location} - ${bin.fillLevel}% Full",
                    icon = BitmapDescriptorFactory.defaultMarker(markerColor)
                )
            }
        }

        if (bins.isEmpty()) {
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GreenPrimary)
            }
        }
        
        // Floating Bin List for quick navigation
        if (showLegend && bins.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .width(120.dp)
            ) {
                Text(
                    "Quick Locate",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                bins.take(5).forEach { bin ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                scope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(
                                            LatLng(bin.latitude, bin.longitude), 
                                            15f
                                        )
                                    )
                                }
                            },
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Text(
                            bin.id,
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        if (showLegend) {
            // Map Legend
            Card(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(Modifier.padding(8.dp)) {
                    Text("Bin Status", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(4.dp))
                    LegendItem(Color.Red, "Full (>80%)")
                    LegendItem(Color(0xFFFF9800), "Warning (50-80%)")
                    LegendItem(Color(0xFF4CAF50), "Normal (<50%)")
                }
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Box(Modifier.size(12.dp).background(color, CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun AddBinDialog(
    initialBin: BinData? = null,
    onDismiss: () -> Unit,
    onBinAdded: (BinData) -> Unit,
    onDeleteBin: (() -> Unit)? = null
) {
    var id by remember { mutableStateOf(initialBin?.id ?: "") }
    var location by remember { mutableStateOf(initialBin?.location ?: "") }
    var assignedDriver by remember { mutableStateOf(initialBin?.assignedDriver ?: "") }
    var latitude by remember { mutableStateOf(initialBin?.latitude?.toString() ?: "0.3136") }
    var longitude by remember { mutableStateOf(initialBin?.longitude?.toString() ?: "32.5811") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (initialBin == null) "Add New Bin" else "Edit Bin",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = GreenPrimary
                )
                if (initialBin != null && onDeleteBin != null) {
                    IconButton(onClick = onDeleteBin) {
                        Icon(Icons.Default.Delete, "Delete", tint = Color.Red.copy(alpha = 0.6f))
                    }
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                OutlinedTextField(
                    value = id,
                    onValueChange = { id = it },
                    label = { Text("Bin ID (e.g. Bin-105)") },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = GreenPrimary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location Name") },
                    leadingIcon = { Icon(Icons.Default.Place, null, tint = GreenPrimary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = assignedDriver,
                    onValueChange = { assignedDriver = it },
                    label = { Text("Assigned Driver") },
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = GreenPrimary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = latitude,
                        onValueChange = { latitude = it },
                        label = { Text("Latitude") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = longitude,
                        onValueChange = { longitude = it },
                        label = { Text("Longitude") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onBinAdded(
                        BinData(
                            id = id,
                            location = location,
                            fillLevel = initialBin?.fillLevel ?: 0,
                            assignedDriver = assignedDriver.ifEmpty { "Unassigned" },
                            latitude = latitude.toDoubleOrNull() ?: 0.3136,
                            longitude = longitude.toDoubleOrNull() ?: 32.5811
                        )
                    )
                    onDismiss()
                },
                enabled = id.isNotBlank() && location.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
            ) { Text(if (initialBin == null) "Add Bin" else "Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun AdminDashboardPreview() {
    SmartWasteTheme {
        AdminDashboardScreen(onLogout = {})
    }
}
