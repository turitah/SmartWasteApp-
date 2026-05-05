package com.example.smartwaste.admin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextAlign
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
import java.util.Calendar

enum class AdminSubScreen {
    Dashboard, Drivers, MapView, Reports, Bins
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    adminEmail: String = "admin@smartwaste.com",
    onLogout: () -> Unit,
    viewModel: AdminViewModel = viewModel()
) {
    LaunchedEffect(adminEmail) {
        viewModel.setAdminEmail(adminEmail)
    }

    val adminName = viewModel.adminNameState
    val adminProfileImage = viewModel.adminProfileImage

    val reports by viewModel.reports.collectAsState()
    val drivers = viewModel.drivers
    val bins = viewModel.bins

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var currentSubScreen by remember { mutableStateOf(AdminSubScreen.Dashboard) }
    var selectedReport by remember { mutableStateOf<ReportItem?>(null) }
    var binToAssign by remember { mutableStateOf<BinData?>(null) }
    
    // Dialog states for Manage Drivers
    var showAddDriverDialog by remember { mutableStateOf(false) }
    var driverToEdit by remember { mutableStateOf<Driver?>(null) }
    var driverToDelete by remember { mutableStateOf<Driver?>(null) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val unreadReportCount = reports.count { it.status == "Pending" }

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    val adminImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.updateAdminProfileImage(it.toString()) }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GreenPrimary)
                        .padding(32.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(90.dp).border(2.dp, Color.White, CircleShape)
                            ) {
                                if (adminProfileImage != null) {
                                    AsyncImage(
                                        model = adminProfileImage,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Person,
                                        null,
                                        tint = Color.White,
                                        modifier = Modifier.padding(16.dp).size(48.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = { adminImagePicker.launch("image/*") },
                                modifier = Modifier.size(32.dp).background(Color.White, CircleShape).padding(4.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, "Edit Profile Image", tint = GreenPrimary, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(adminName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(viewModel.adminEmailState, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.height(16.dp))
                PaddingValues(16.dp).let {
                    Text("Account Information", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                }
                ListItem(
                    headlineContent = { Text("Role") },
                    supportingContent = { Text("Super Administrator") },
                    leadingContent = { Icon(Icons.Default.AdminPanelSettings, null) }
                )
                ListItem(
                    headlineContent = { Text("Permissions") },
                    supportingContent = { Text("Full Access") },
                    leadingContent = { Icon(Icons.Default.VerifiedUser, null) }
                )
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "$greeting, $adminName",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                when (currentSubScreen) {
                                    AdminSubScreen.Dashboard -> "Overview"
                                    AdminSubScreen.Drivers -> "Manage Drivers"
                                    AdminSubScreen.MapView -> "Live Bin Map"
                                    AdminSubScreen.Reports -> "User Reports"
                                    AdminSubScreen.Bins -> "Waste Bins"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, "Profile", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = onLogout) {
                            Icon(Icons.AutoMirrored.Filled.Logout, "Logout", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenPrimary)
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp
                ) {
                    val navItems = listOf(
                        Triple(AdminSubScreen.Dashboard, Icons.Default.Dashboard, "Home"),
                        Triple(AdminSubScreen.Drivers, Icons.Default.People, "Drivers"),
                        Triple(AdminSubScreen.MapView, Icons.Default.Map, "Map"),
                        Triple(AdminSubScreen.Reports, Icons.Default.Report, "Reports"),
                        Triple(AdminSubScreen.Bins, Icons.Default.Delete, "Bins")
                    )
                    
                    navItems.forEach { (screen, icon, label) ->
                        NavigationBarItem(
                            icon = {
                                if (screen == AdminSubScreen.Reports && unreadReportCount > 0) {
                                    BadgedBox(badge = { Badge { Text(unreadReportCount.toString()) } }) {
                                        Icon(icon, null)
                                    }
                                } else {
                                    Icon(icon, null)
                                }
                            },
                            label = { Text(label, fontSize = 10.sp) },
                            selected = currentSubScreen == screen,
                            onClick = { currentSubScreen = screen },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = GreenPrimary,
                                selectedTextColor = GreenPrimary,
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray,
                                indicatorColor = GreenPrimary.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            },
            floatingActionButton = {
                if (currentSubScreen == AdminSubScreen.Drivers) {
                    FloatingActionButton(
                        onClick = { showAddDriverDialog = true },
                        containerColor = GreenPrimary,
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Default.Add, "Add Driver")
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                when (currentSubScreen) {
                    AdminSubScreen.Dashboard -> DashboardContent(drivers.size, bins.size, unreadReportCount, viewModel.getDayEvaluation())
                    AdminSubScreen.Drivers -> ManageDriversScreen(
                        drivers = drivers,
                        onDelete = { driverToDelete = it },
                        onEdit = { driverToEdit = it }
                    )
                    AdminSubScreen.Reports -> ManageReportsScreen(reports) { selectedReport = it }
                    AdminSubScreen.Bins -> ManageBinsScreen(
                        bins = bins,
                        onAssign = { binToAssign = it }
                    )
                    AdminSubScreen.MapView -> AdminMapContent(bins)
                }
            }
        }
    }

    if (showAddDriverDialog) {
        DriverActionDialog(
            onDismiss = { showAddDriverDialog = false },
            onConfirm = { name, truck, image ->
                viewModel.addDriver(name, truck, image)
                showAddDriverDialog = false
            }
        )
    }

    driverToEdit?.let { driver ->
        DriverActionDialog(
            driver = driver,
            onDismiss = { driverToEdit = null },
            onConfirm = { name, truck, image ->
                viewModel.updateDriver(driver.id, driver.copy(name = name, truck = truck, profileImage = image ?: driver.profileImage))
                driverToEdit = null
            }
        )
    }

    driverToDelete?.let { driver ->
        AlertDialog(
            onDismissRequest = { driverToDelete = null },
            title = { Text("Delete Driver") },
            text = { Text("Are you sure you want to delete ${driver.name}? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDriver(driver)
                        driverToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { driverToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    selectedReport?.let { report ->
        ReportDetailDialog(
            report = report,
            drivers = drivers,
            onDismiss = { selectedReport = null },
            onResolve = { driverName, notes -> viewModel.markAsResolved(report.id, driverName, notes) },
            onDelete = { viewModel.deleteReport(report.id) }
        )
    }

    binToAssign?.let { bin ->
        AssignDriverDialog(
            bin = bin,
            drivers = drivers,
            onDismiss = { binToAssign = null },
            onConfirm = { driverName ->
                viewModel.assignDriverToBin(bin.id, driverName)
                binToAssign = null
            }
        )
    }
}

@Composable
fun DashboardContent(driverCount: Int, binCount: Int, reportCount: Int, evaluation: String) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Dashboard Overview", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = GreenPrimary.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Assessment, null, tint = GreenPrimary, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Daily Evaluation", fontWeight = FontWeight.Bold, color = GreenPrimary)
                        Text(evaluation, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard("Total Drivers", driverCount.toString(), Icons.Default.People, Modifier.weight(1f))
                StatCard("Total Bins", binCount.toString(), Icons.Default.Delete, Modifier.weight(1f))
            }
        }
        item {
            StatCard("Active Reports", reportCount.toString(), Icons.Default.Warning, Modifier.fillMaxWidth(), color = Color(0xFFF44336))
        }
    }
}

@Composable
fun StatCard(title: String, value: String, icon: ImageVector, modifier: Modifier, color: Color = GreenPrimary) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
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
fun ManageDriversScreen(
    drivers: List<Driver>,
    onDelete: (Driver) -> Unit,
    onEdit: (Driver) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(drivers) { driver ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = driver.profileImage,
                        contentDescription = null,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(driver.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Truck: ${driver.truck}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text("Bins: ${driver.assignedBins}", style = MaterialTheme.typography.labelSmall, color = GreenPrimary)
                    }
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
                        Spacer(Modifier.height(8.dp))
                        Row {
                            IconButton(onClick = { onEdit(driver) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Edit, "Edit", tint = GreenPrimary, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { onDelete(driver) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Delete, "Delete", tint = Color.Red, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DriverActionDialog(
    driver: Driver? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String?) -> Unit
) {
    var name by remember { mutableStateOf(driver?.name ?: "") }
    var truck by remember { mutableStateOf(driver?.truck ?: "") }
    var selectedImageUri by remember { mutableStateOf<String?>(driver?.profileImage) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedImageUri = it.toString() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (driver == null) "Add New Driver" else "Edit Driver") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.Gray.copy(alpha = 0.2f))
                        .clickable { imagePicker.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageUri != null) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.AddAPhoto, "Add Photo", tint = Color.Gray)
                    }
                }
                Text("Tap to ${if (selectedImageUri == null) "add" else "change"} photo", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Driver Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = truck,
                    onValueChange = { truck = it },
                    label = { Text("Truck Number") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, truck, selectedImageUri) },
                enabled = name.isNotBlank() && truck.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ManageReportsScreen(reports: List<ReportItem>, onReportClick: (ReportItem) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(reports) { report ->
            val cardColor = when {
                report.priority == "Emergency" -> Color(0xFFFFEBEE)
                report.status == "Pending" -> Color(0xFFFFF3E0)
                else -> Color.White
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onReportClick(report) },
                colors = CardDefaults.cardColors(
                    containerColor = cardColor
                ),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                ListItem(
                    headlineContent = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(report.issueType, fontWeight = FontWeight.Bold)
                            if (report.priority == "Emergency") {
                                Spacer(Modifier.width(8.dp))
                                Surface(color = Color.Red, shape = RoundedCornerShape(4.dp)) {
                                    Text("EMERGENCY", color = Color.White, fontSize = 8.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    },
                    supportingContent = { Text("From: ${report.userName} • ${report.location}") },
                    trailingContent = {
                        Badge(containerColor = if (report.status == "Pending") Color.Red else GreenPrimary) {
                            Text(report.status, color = Color.White)
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }
    }
}

@Composable
fun ManageBinsScreen(
    bins: List<BinData>,
    onAssign: (BinData) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(bins) { bin ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(bin.id, fontWeight = FontWeight.Bold)
                        Text(bin.location, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Assigned: ${bin.assignedDriver}", style = MaterialTheme.typography.labelSmall, color = GreenPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Change",
                                modifier = Modifier.clickable { onAssign(bin) },
                                color = GreenPrimary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
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
fun AssignDriverDialog(
    bin: BinData,
    drivers: List<Driver>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selectedDriver by remember { mutableStateOf(bin.assignedDriver) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign Driver to ${bin.id}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Select a driver to handle this bin:")
                drivers.forEach { driver ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedDriver = driver.name }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedDriver == driver.name,
                            onClick = { selectedDriver = driver.name }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(driver.name)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedDriver) },
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
            ) {
                Text("Assign")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
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
fun ReportDetailDialog(
    report: ReportItem,
    drivers: List<Driver>,
    onDismiss: () -> Unit,
    onResolve: (String, String) -> Unit,
    onDelete: () -> Unit
) {
    var selectedDriver by remember { mutableStateOf("") }
    var adminNotes by remember { mutableStateOf("") }
    var showResolveOptions by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text(report.issueType, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (report.priority == "Emergency") {
                    Surface(color = Color.Red, shape = RoundedCornerShape(4.dp), modifier = Modifier.fillMaxWidth()) {
                        Text("EMERGENCY REQUEST", color = Color.White, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
                    }
                }
                Text("Reported by: ${report.userName}", fontWeight = FontWeight.SemiBold)
                Text("Location: ${report.location}")
                if (report.wasteType.isNotEmpty()) {
                    Text("Waste Type: ${report.wasteType}", fontWeight = FontWeight.SemiBold, color = GreenPrimary)
                }
                Spacer(Modifier.height(8.dp))
                Text("Description:", fontWeight = FontWeight.SemiBold)
                Text(report.description.ifEmpty { "No description provided." })
                Spacer(Modifier.height(8.dp))
                
                if (report.status == "Pending") {
                    if (showResolveOptions) {
                        HorizontalDivider()
                        Text("Assign to Driver:", fontWeight = FontWeight.SemiBold)
                        drivers.forEach { driver ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedDriver = driver.name }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = selectedDriver == driver.name, onClick = { selectedDriver = driver.name })
                                Text(driver.name, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                        OutlinedTextField(
                            value = adminNotes,
                            onValueChange = { adminNotes = it },
                            label = { Text("Admin Notes (Optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            "Status: ${report.status}",
                            color = Color.Red,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        "Status: ${report.status}",
                        color = GreenPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    if (report.adminNotes.isNotEmpty()) {
                        Text("Admin Notes:", fontWeight = FontWeight.SemiBold)
                        Text(report.adminNotes)
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (report.status == "Pending") {
                    if (!showResolveOptions) {
                        Button(
                            onClick = { showResolveOptions = true },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                        ) {
                            Text("Resolve Report")
                        }
                    } else {
                        Button(
                            onClick = {
                                if (selectedDriver.isNotEmpty()) {
                                    onResolve(selectedDriver, adminNotes)
                                    onDismiss()
                                }
                            },
                            enabled = selectedDriver.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                        ) {
                            Text("Confirm Assignment")
                        }
                    }
                }
                
                if (!showResolveOptions) {
                    IconButton(onClick = {
                        onDelete()
                        onDismiss()
                    }) {
                        Icon(Icons.Default.Delete, "Delete", tint = Color.Red)
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { if (showResolveOptions) showResolveOptions = false else onDismiss() }) {
                Text(if (showResolveOptions) "Back" else "Close")
            }
        }
    )
}
