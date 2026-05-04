package com.example.smartwaste.admin

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.smartwaste.ui.theme.GreenPrimary
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

enum class AdminSubScreen {
    Dashboard, Drivers, MapView, Schedule, Reports, Bins
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
    var showMenu by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val unreadReportCount = reports.count { it.status == "Pending" }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GreenPrimary)
                        .padding(24.dp)
                ) {
                    Column {
                        Icon(Icons.Default.AccountCircle, null, tint = Color.White, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("System Admin", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("admin@smartwaste.com", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Dashboard, null) },
                    label = { Text("Dashboard") },
                    selected = currentSubScreen == AdminSubScreen.Dashboard,
                    onClick = {
                        currentSubScreen = AdminSubScreen.Dashboard
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.People, null) },
                    label = { Text("Manage Drivers") },
                    selected = currentSubScreen == AdminSubScreen.Drivers,
                    onClick = {
                        currentSubScreen = AdminSubScreen.Drivers
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Map, null) },
                    label = { Text("Live Map") },
                    selected = currentSubScreen == AdminSubScreen.MapView,
                    onClick = {
                        currentSubScreen = AdminSubScreen.MapView
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.DateRange, null) },
                    label = { Text("Schedules") },
                    selected = currentSubScreen == AdminSubScreen.Schedule,
                    onClick = {
                        currentSubScreen = AdminSubScreen.Schedule
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Delete, null) },
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
                        BadgedBox(
                            badge = {
                                if (unreadReportCount > 0) {
                                    Badge { Text(unreadReportCount.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Report, null)
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
                Spacer(Modifier.weight(1f))
                HorizontalDivider()
                NavigationDrawerItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color.Red) },
                    label = { Text("Logout", color = Color.Red) },
                    selected = false,
                    onClick = { onLogout() },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
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
                                AdminSubScreen.Bins -> "Waste Bins"
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
                    actions = {
                        Box {
                            IconButton(onClick = { showMenu = !showMenu }) {
                                Icon(Icons.Default.MoreVert, null, tint = Color.White)
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
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color.Red) }
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
                    AdminSubScreen.Dashboard -> DashboardContent(drivers.size, bins.size, unreadReportCount)
                    AdminSubScreen.Drivers -> ManageDriversScreen(drivers)
                    AdminSubScreen.Reports -> ManageReportsScreen(reports) { selectedReport = it }
                    AdminSubScreen.Bins -> ManageBinsScreen(bins)
                    AdminSubScreen.Schedule -> AdminScheduleScreen(schedules)
                    AdminSubScreen.MapView -> AdminMapContent(bins)
                }
            }
        }
    }

    selectedReport?.let { report ->
        ReportDetailDialog(
            report = report,
            onDismiss = { selectedReport = null },
            onResolve = { viewModel.markAsResolved(report.id) }
        )
    }
}

@Composable
fun DashboardContent(driverCount: Int, binCount: Int, reportCount: Int) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Overview", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard("Drivers", driverCount.toString(), Icons.Default.People, Modifier.weight(1f))
                StatCard("Bins", binCount.toString(), Icons.Default.Delete, Modifier.weight(1f))
            }
        }
        item {
            StatCard("Pending Reports", reportCount.toString(), Icons.Default.Warning, Modifier.fillMaxWidth(), color = Color.Red)
        }
    }
}

@Composable
fun StatCard(title: String, value: String, icon: ImageVector, modifier: Modifier, color: Color = GreenPrimary) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            Text(title, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

@Composable
fun ManageDriversScreen(drivers: List<Driver>) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(drivers) { driver ->
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                            .size(50.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(driver.name, fontWeight = FontWeight.Bold)
                        Text("Truck: ${driver.truck}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
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
            }
        }
    }
}

@Composable
fun ManageReportsScreen(reports: List<ReportItem>, onReportClick: (ReportItem) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(reports) { report ->
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
                ListItem(
                    headlineContent = { Text(report.issueType, fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("From: ${report.userName} • ${report.location}") },
                    trailingContent = {
                        Badge(containerColor = if (report.status == "Pending") Color.Red else GreenPrimary) {
                            Text(report.status, color = Color.White)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ManageBinsScreen(bins: List<BinData>) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(bins) { bin ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(bin.id, fontWeight = FontWeight.Bold)
                        Text(bin.location, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text("Assigned: ${bin.assignedDriver}", style = MaterialTheme.typography.labelSmall, color = GreenPrimary)
                    }
                    Text(
                        "${bin.fillLevel}%",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (bin.fillLevel > 80) Color.Red else GreenPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun AdminScheduleScreen(schedules: List<ScheduleData>) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(schedules) { schedule ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                ListItem(
                    headlineContent = { Text(schedule.date, fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("${schedule.shift} • ${schedule.driverName}") },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalShipping, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                            Spacer(Modifier.width(4.dp))
                            Text(schedule.vehicle, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun AdminMapContent(bins: List<BinData>) {
    val kampala = LatLng(0.3136, 32.5811)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(kampala, 12f)
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        uiSettings = MapUiSettings(zoomControlsEnabled = true)
    ) {
        bins.forEach { bin ->
            Marker(
                state = MarkerState(position = LatLng(bin.latitude, bin.longitude)),
                title = bin.id,
                snippet = "${bin.location} - ${bin.fillLevel}% Full",
                icon = BitmapDescriptorFactory.defaultMarker(
                    if (bin.fillLevel > 80) BitmapDescriptorFactory.HUE_RED else BitmapDescriptorFactory.HUE_GREEN
                )
            )
        }
    }
}

@Composable
fun ReportDetailDialog(report: ReportItem, onDismiss: () -> Unit, onResolve: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text(report.issueType, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Reported by: ${report.userName}", fontWeight = FontWeight.SemiBold)
                Text("Location: ${report.location}")
                Spacer(Modifier.height(8.dp))
                Text("Description:", fontWeight = FontWeight.SemiBold)
                Text(report.description.ifEmpty { "No description provided." })
                Spacer(Modifier.height(8.dp))
                Text(
                    "Status: ${report.status}",
                    color = if (report.status == "Pending") Color.Red else GreenPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        confirmButton = {
            if (report.status == "Pending") {
                Button(
                    onClick = {
                        onResolve()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text("Mark Resolved")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
