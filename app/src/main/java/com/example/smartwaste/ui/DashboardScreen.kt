package com.example.smartwaste.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartwaste.ui.theme.GreenDark
import com.example.smartwaste.ui.theme.GreenPrimary
import com.example.smartwaste.ui.theme.SmartWasteTheme
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class SmartWasteScreen {
    Welcome, Login, Register, Home, ReportIssue, Rewards, History, Tips
}

@Composable
fun SmartWasteApp() {
    var currentScreen by remember { mutableStateOf(SmartWasteScreen.Welcome) }

    SmartWasteTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF5F5F5)
        ) {
            when (currentScreen) {
                SmartWasteScreen.Welcome -> WelcomeScreen(
                    onLogin = { currentScreen = SmartWasteScreen.Login },
                    onRegister = { currentScreen = SmartWasteScreen.Register }
                )
                SmartWasteScreen.Login -> LoginScreen(
                    onLoginSuccess = { currentScreen = SmartWasteScreen.Home },
                    onNavigateToRegister = { currentScreen = SmartWasteScreen.Register }
                )
                SmartWasteScreen.Register -> RegisterScreen(
                    onRegisterSuccess = { currentScreen = SmartWasteScreen.Home },
                    onNavigateToLogin = { currentScreen = SmartWasteScreen.Login }
                )
                SmartWasteScreen.Home -> HomeScreen(
                    onReportIssue = { currentScreen = SmartWasteScreen.ReportIssue },
                    onViewRewards = { currentScreen = SmartWasteScreen.Rewards },
                    onViewSchedule = { currentScreen = SmartWasteScreen.History },
                    onViewTips = { currentScreen = SmartWasteScreen.Tips },
                    onLogout = { currentScreen = SmartWasteScreen.Welcome }
                )
                SmartWasteScreen.ReportIssue -> ReportIssueScreen(
                    onBack = { currentScreen = SmartWasteScreen.Home },
                    onSubmit = { currentScreen = SmartWasteScreen.Home }
                )
                SmartWasteScreen.Rewards -> RewardsScreen(
                    onBack = { currentScreen = SmartWasteScreen.Home }
                )
                SmartWasteScreen.History -> HistoryScreen(
                    onBack = { currentScreen = SmartWasteScreen.Home }
                )
                SmartWasteScreen.Tips -> TipsScreen(
                    onBack = { currentScreen = SmartWasteScreen.Home }
                )
            }
        }
    }
}

@Composable
fun WelcomeScreen(onLogin: () -> Unit, onRegister: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFE8F5E9), Color.White)))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Recycling,
            contentDescription = null,
            tint = GreenPrimary,
            modifier = Modifier.size(120.dp)
        )
        Spacer(Modifier.height(32.dp))
        Text("SmartWaste", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.ExtraBold, color = GreenDark)
        Text("Cleaning Uganda, one pickup at a time.", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, color = Color.Gray)
        Spacer(Modifier.height(80.dp))
        Button(onClick = onRegister, modifier = Modifier.fillMaxWidth().height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary), shape = RoundedCornerShape(16.dp)) {
            Text("Get Started", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onLogin, modifier = Modifier.fillMaxWidth().height(60.dp), shape = RoundedCornerShape(16.dp), border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(width = 2.dp)) {
            Text("Login", color = GreenDark, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, onNavigateToRegister: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().background(Color.White).padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Welcome Back", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = GreenDark)
        Spacer(Modifier.height(48.dp))
        AuthTextField(value = email, onValueChange = { email = it }, label = "Email", icon = Icons.Default.Email)
        Spacer(Modifier.height(16.dp))
        AuthTextField(value = password, onValueChange = { password = it }, label = "Password", icon = Icons.Default.Lock, isPassword = true)
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { scope.launch { isLoading = true; delay(1000); isLoading = false; onLoginSuccess() } },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            else Text("Login", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onNavigateToRegister) { Text("Don't have an account? Register", color = GreenDark) }
    }
}

@Composable
fun RegisterScreen(onRegisterSuccess: () -> Unit, onNavigateToLogin: () -> Unit) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().background(Color.White).padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Create Account", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = GreenDark)
        Spacer(Modifier.height(48.dp))
        AuthTextField(value = fullName, onValueChange = { fullName = it }, label = "Full Name", icon = Icons.Default.Person)
        Spacer(Modifier.height(16.dp))
        AuthTextField(value = email, onValueChange = { email = it }, label = "Email", icon = Icons.Default.Email)
        Spacer(Modifier.height(16.dp))
        AuthTextField(value = password, onValueChange = { password = it }, label = "Password", icon = Icons.Default.Lock, isPassword = true)
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { scope.launch { isLoading = true; delay(1000); isLoading = false; onRegisterSuccess() } },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            else Text("Register", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onNavigateToLogin) { Text("Already have an account? Login", color = GreenDark) }
    }
}

@Composable
fun AuthTextField(value: String, onValueChange: (String) -> Unit, label: String, icon: ImageVector, isPassword: Boolean = false) {
    var passwordVisible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = GreenPrimary) },
        trailingIcon = {
            if (isPassword) {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null, tint = GreenPrimary)
                }
            }
        },
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GreenPrimary, focusedLabelColor = GreenPrimary)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onReportIssue: () -> Unit,
    onViewRewards: () -> Unit,
    onViewSchedule: () -> Unit,
    onViewTips: () -> Unit,
    onLogout: () -> Unit
) {
    var showBins by remember { mutableStateOf(false) }
    val kampala = LatLng(0.3476, 32.5825)
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(kampala, 14f)
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Text("SmartWaste", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = GreenDark)
                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text("Dashboard") },
                    selected = true,
                    onClick = { scope.launch { drawerState.close() } },
                    icon = { Icon(Icons.Default.Dashboard, null) }
                )
                NavigationDrawerItem(
                    label = { Text("Report Issue") },
                    selected = false,
                    onClick = { scope.launch { drawerState.close(); onReportIssue() } },
                    icon = { Icon(Icons.Default.Report, null) }
                )
                NavigationDrawerItem(
                    label = { Text("Schedule") },
                    selected = false,
                    onClick = { scope.launch { drawerState.close(); onViewSchedule() } },
                    icon = { Icon(Icons.Default.DateRange, null) }
                )
                NavigationDrawerItem(
                    label = { Text("Rewards") },
                    selected = false,
                    onClick = { scope.launch { drawerState.close(); onViewRewards() } },
                    icon = { Icon(Icons.Default.EmojiEvents, null) }
                )
                NavigationDrawerItem(
                    label = { Text("Recycling Tips") },
                    selected = false,
                    onClick = { scope.launch { drawerState.close(); onViewTips() } },
                    icon = { Icon(Icons.Default.Lightbulb, null) }
                )
                Spacer(Modifier.weight(1f))
                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text("Logout") },
                    selected = false,
                    onClick = { scope.launch { drawerState.close(); onLogout() } },
                    icon = { Icon(Icons.AutoMirrored.Filled.Logout, null) }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Dashboard", fontWeight = FontWeight.Bold, color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = null, tint = Color.White)
                        }
                    },
                    actions = { IconButton(onClick = {}) { Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.White) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenDark)
                )
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding).fillMaxSize().verticalScroll(rememberScrollState())) {
                // Map Section
                Box(modifier = Modifier.fillMaxWidth().height(300.dp).background(Color(0xFFE0E0E0))) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(isMyLocationEnabled = true),
                        uiSettings = MapUiSettings(myLocationButtonEnabled = true)
                    ) {
                        Marker(
                            state = rememberMarkerState(position = kampala),
                            title = "Your Location",
                            snippet = "Kampala Center"
                        )

                        if (showBins) {
                            val bins = listOf(
                                LatLng(0.3500, 32.5850),
                                LatLng(0.3450, 32.5800),
                                LatLng(0.3420, 32.5900),
                                LatLng(0.3550, 32.5750)
                            )
                            bins.forEachIndexed { index, binPos ->
                                Marker(
                                    state = rememberMarkerState(position = binPos),
                                    title = "Bin #${index + 1}",
                                    icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_GREEN)
                                )
                            }
                        }
                    }
                    
                    Surface(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.9f),
                        shadowElevation = 4.dp
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.GpsFixed, null, tint = GreenPrimary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (showBins) "Showing Nearby Bins" else "GPS Active", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Quick Actions Grid
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DashboardButton(
                            label = "Find Bins",
                            icon = Icons.Default.LocationOn,
                            color = if (showBins) GreenDark else Color(0xFF4CAF50),
                            modifier = Modifier.weight(1f)
                        ) {
                            showBins = !showBins
                        }
                        DashboardButton("Collection Schedule", Icons.Default.DateRange, Color(0xFF2196F3), Modifier.weight(1f), onViewSchedule)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DashboardButton("Report Issue", Icons.Default.Report, Color(0xFFF44336), Modifier.weight(1f), onReportIssue)
                        DashboardButton("Recycling Tips", Icons.Default.Lightbulb, Color(0xFF009688), Modifier.weight(1f), onViewTips)
                    }
                }

                // Report an Issue Row
                Text("Report an Issue", modifier = Modifier.padding(horizontal = 16.dp), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    IssueTypeItem("Missed Pickup", Icons.Default.Person, onReportIssue)
                    IssueTypeItem("Overflowing Bin", Icons.Default.Delete, onReportIssue)
                    IssueTypeItem("Illegal Dumping", Icons.Default.Warning, onReportIssue)
                }

                // Your Eco Points Card
                Card(
                    modifier = Modifier.padding(16.dp).fillMaxWidth().clickable { onViewRewards() },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = GreenPrimary.copy(alpha = 0.1f), modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Recycling, null, tint = GreenPrimary, modifier = Modifier.padding(8.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Your Eco Points", fontWeight = FontWeight.Bold)
                            Text("120 Points Earned", color = GreenDark, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardButton(label: String, icon: ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(60.dp),
        color = color,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 4.dp
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
fun IssueTypeItem(label: String, icon: ImageVector, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(90.dp).clickable { onClick() }) {
        Surface(shape = RoundedCornerShape(12.dp), color = Color.White, shadowElevation = 2.dp, modifier = Modifier.size(60.dp)) {
            Icon(icon, null, tint = GreenDark, modifier = Modifier.padding(16.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(label, fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportIssueScreen(onBack: () -> Unit, onSubmit: () -> Unit) {
    var selectedIssue by remember { mutableStateOf("Overflowing Bin") }
    var location by remember { mutableStateOf("Current Location: 123 Kampala St") }
    var description by remember { mutableStateOf("") }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showImageSourceDialog by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        capturedImageUri = uri
    }

    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("Select Image Source") },
            text = { Text("Choose where you want to get the photo from.") },
            confirmButton = {
                TextButton(onClick = {
                    showImageSourceDialog = false
                    galleryLauncher.launch("image/*")
                }) { Text("Gallery") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImageSourceDialog = false
                    galleryLauncher.launch("image/*") 
                }) { Text("Camera") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Report an Issue", color = Color.White) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenDark)
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
            Text("Select Issue:", fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = selectedIssue, onValueChange = {}, readOnly = true,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                shape = RoundedCornerShape(8.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text("Add Photo", fontWeight = FontWeight.Bold)
            Surface(
                modifier = Modifier.fillMaxWidth().height(150.dp).padding(vertical = 8.dp).clickable {
                    showImageSourceDialog = true
                },
                shape = RoundedCornerShape(8.dp), color = Color.White, border = CardDefaults.outlinedCardBorder()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    if (capturedImageUri != null) {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(48.dp), tint = GreenPrimary)
                        Text("Photo Selected", color = GreenPrimary)
                    } else {
                        Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                        Text("Tap to select Gallery or Camera", color = Color.Gray)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Location:", fontWeight = FontWeight.Bold)
            OutlinedTextField(value = location, onValueChange = { location = it }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), shape = RoundedCornerShape(8.dp))
            Spacer(Modifier.height(16.dp))
            Text("Description:", fontWeight = FontWeight.Bold)
            OutlinedTextField(value = description, onValueChange = { description = it }, modifier = Modifier.fillMaxWidth().height(120.dp).padding(vertical = 8.dp), placeholder = { Text("Enter details about the problem...") }, shape = RoundedCornerShape(8.dp))
            Spacer(Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = onBack, modifier = Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray), shape = RoundedCornerShape(8.dp)) {
                    Text("Cancel", color = Color.Black)
                }
                Button(onClick = onSubmit, modifier = Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary), shape = RoundedCornerShape(8.dp)) {
                    Text("Submit Report", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TipsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recycling Tips", color = Color.White) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenDark)
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
            TipCard("Clean your recyclables", "Rinse out food containers to avoid contaminating other materials.", Icons.Default.CleaningServices)
            TipCard("Check local guidelines", "Not all plastics are recyclable. Look for the number inside the triangle.", Icons.Default.Info)
            TipCard("Reduce & Reuse", "Recycling is great, but reducing waste at the source is even better.", Icons.Default.Loop)
            TipCard("Compost organic waste", "Food scraps and yard waste can be turned into nutrient-rich soil.", Icons.Default.Grass)
            TipCard("E-Waste Disposal", "Never throw batteries or electronics in the trash. Use specialized drop-off points.", Icons.Default.BatteryChargingFull)
        }
    }
}

@Composable
fun TipCard(title: String, description: String, icon: ImageVector) {
    Card(
        modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = GreenPrimary.copy(alpha = 0.1f), modifier = Modifier.size(48.dp)) {
                Icon(icon, null, tint = GreenPrimary, modifier = Modifier.padding(12.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, color = GreenDark)
                Text(description, fontSize = 14.sp, color = Color.Gray)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Eco Rewards", color = Color.White) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
                actions = { IconButton(onClick = {}) { Icon(Icons.Default.Notifications, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenDark)
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize().verticalScroll(rememberScrollState())) {
            Card(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Your Eco Points", color = Color.Gray)
                    Text("120", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = GreenDark)
                    Text("Points Earned", fontSize = 14.sp, color = Color.Gray)
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(progress = { 0.8f }, modifier = Modifier.fillMaxWidth().height(8.dp), color = GreenPrimary, trackColor = Color.LightGray, strokeCap = androidx.compose.ui.graphics.StrokeCap.Round)
                    Text("Next Reward: 150 Points", modifier = Modifier.padding(top = 8.dp), fontSize = 12.sp)
                }
            }
            RewardListItem("Free Coffee", "Redeem for 150 Points", Icons.Default.Coffee, Color(0xFF795548))
            RewardListItem("Discount Coupon", "Get 10% Off for 200 Points", Icons.Default.ConfirmationNumber, Color(0xFFFF9800))
            RewardListItem("Reusable Tote Bag", "Claim for 300 Points", Icons.Default.ShoppingBag, Color(0xFF4CAF50))
            Spacer(Modifier.height(24.dp))
            Text("My Achievements", modifier = Modifier.padding(horizontal = 16.dp), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                AchievementBadge("Recycler", Color(0xFF4CAF50))
                AchievementBadge("5 Pickups", Color(0xFFFF5722))
                AchievementBadge("Green Hero", Color(0xFF009688))
            }
        }
    }
}

@Composable
fun RewardListItem(title: String, subtitle: String, icon: ImageVector, iconColor: Color) {
    Card(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(8.dp), color = iconColor.copy(alpha = 0.1f), modifier = Modifier.size(50.dp)) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.padding(12.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun AchievementBadge(label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = CircleShape, color = color, modifier = Modifier.size(60.dp), shadowElevation = 4.dp) {
            Icon(Icons.Default.EmojiEvents, null, tint = Color.White, modifier = Modifier.padding(16.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Collection Schedule", color = Color.White) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenDark)
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            ScheduleItem("Tomorrow, 08:00 AM", "Organic Waste", "Scheduled")
            ScheduleItem("Friday, 09:30 AM", "Recyclables", "Confirmed")
        }
    }
}

@Composable
fun ScheduleItem(time: String, type: String, status: String) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(time, fontWeight = FontWeight.Bold)
                Text(type, color = Color.Gray)
            }
            Text(status, color = GreenPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardPreview() { SmartWasteApp() }
