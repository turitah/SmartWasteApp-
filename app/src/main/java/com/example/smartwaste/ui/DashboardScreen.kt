package com.example.smartwaste.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.location.Geocoder
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.smartwaste.admin.AdminDashboardScreen
import com.example.smartwaste.auth.FirebaseAuthService
import com.example.smartwaste.ui.theme.GreenDark
import com.example.smartwaste.ui.theme.GreenPrimary
import com.example.smartwaste.ui.theme.SmartWasteTheme
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * OOP: Abstraction
 * Using an Enum to represent different app states/screens.
 */
enum class SmartWasteScreen {
    Welcome, Login, Register, Home, ReportIssue, Rewards, History, Tips, AdminDashboard
}

const val ADMIN_SECRET_KEY = "SW-ADMIN-2020"
const val ADMIN_PASSWORD = "admin123"

/**
 * OOP: Encapsulation (Data Model)
 */
data class User(
    val email: String,
    val fullName: String,
    val isAdmin: Boolean = false,
    val wasteCollected: Double = 0.0,
    val impact: Int = 0,
    val ecoPoints: Int = 0
)

/**
 * UDF Implementation: State flows DOWN, Events flow UP.
 * SmartWasteApp acts as the single source of truth for the app state.
 */
@Composable
fun SmartWasteApp(
    initialEmail: String? = null, 
    onUserLoggedIn: (String) -> Unit = {},
    onLogoutRequest: () -> Unit = {}
) {
    // State management (Source of Truth)
    var currentScreen by remember { 
        mutableStateOf(if (initialEmail != null) SmartWasteScreen.Home else SmartWasteScreen.Welcome) 
    }
    var currentUser by remember { 
        mutableStateOf(initialEmail?.let { User(it, it.split("@")[0].replaceFirstChar { it.uppercase() }) }) 
    }
    val authService = remember { FirebaseAuthService() }

    // Fetch full user profile from Database when logged in
    LaunchedEffect(currentUser?.email) {
        if (currentUser != null) {
            authService.getCurrentUser()?.uid?.let { uid ->
                val profile = authService.getUserProfile(uid)
                if (profile != null) {
                    currentUser = currentUser?.copy(
                        fullName = profile["fullName"] as? String ?: currentUser!!.fullName,
                        wasteCollected = (profile["wasteCollected"] as? Number)?.toDouble() ?: 0.0,
                        impact = (profile["impact"] as? Number)?.toInt() ?: 0,
                        ecoPoints = (profile["ecoPoints"] as? Number)?.toInt() ?: 0
                    )
                }
            }
        }
    }

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
                authService = authService,
                onLoginSuccess = { email, password, name -> 
                    val isUserAdmin = (email == "admin@smartwaste.com" && password == ADMIN_PASSWORD) || 
                                     (email == "admin" && password == ADMIN_PASSWORD)
                    
                    // Logic for generating greeting name
                    val displayName = if (isUserAdmin) "Admin User" 
                                     else if (name.isNotBlank()) name 
                                     else email.split("@")[0].replaceFirstChar { it.uppercase() }

                    currentUser = User(email = email, fullName = displayName, isAdmin = isUserAdmin)
                    
                    // Critical: Notify MainActivity so it can switch to Driver/Admin dashboards if needed
                    onUserLoggedIn(email)
                    
                    currentScreen = SmartWasteScreen.Home 
                },
                onNavigateToRegister = { currentScreen = SmartWasteScreen.Register }
            )
            SmartWasteScreen.Register -> RegisterScreen(
                authService = authService,
                onRegisterSuccess = { user, email -> 
                    currentUser = user
                    onUserLoggedIn(email)
                    currentScreen = SmartWasteScreen.Home 
                },
                onNavigateToLogin = { currentScreen = SmartWasteScreen.Login }
            )
            SmartWasteScreen.Home -> HomeScreen(
                user = currentUser ?: User("", "User"), // State flows DOWN
                onReportIssue = { currentScreen = SmartWasteScreen.ReportIssue }, // Event flows UP
                onViewRewards = { currentScreen = SmartWasteScreen.Rewards },
                onViewSchedule = { currentScreen = SmartWasteScreen.History },
                onViewTips = { currentScreen = SmartWasteScreen.Tips },
                onLogout = { 
                    authService.logout()
                    currentUser = null
                    currentScreen = SmartWasteScreen.Welcome 
                    onLogoutRequest()
                },
                onAdminAccess = { currentScreen = SmartWasteScreen.AdminDashboard },
                isAdmin = currentUser?.isAdmin ?: false
            )
            SmartWasteScreen.ReportIssue -> ReportIssueScreen(
                authService = authService,
                userEmail = currentUser?.email ?: "anonymous",
                onBack = { currentScreen = SmartWasteScreen.Home },
                onSubmit = { 
                    // Refresh current user state from DB after reporting an issue
                    currentScreen = SmartWasteScreen.Home 
                }
            )
            SmartWasteScreen.Rewards -> RewardsScreen(
                ecoPoints = currentUser?.ecoPoints ?: 0,
                onBack = { currentScreen = SmartWasteScreen.Home }
            )
            SmartWasteScreen.History -> HistoryScreen(onBack = { currentScreen = SmartWasteScreen.Home })
            SmartWasteScreen.Tips -> TipsScreen(onBack = { currentScreen = SmartWasteScreen.Home })
            SmartWasteScreen.AdminDashboard -> AdminDashboardScreen(onLogout = { 
                authService.logout()
                currentScreen = SmartWasteScreen.Welcome 
                onLogoutRequest()
            })
        }
    }
}

@Composable
fun WelcomeScreen(onLogin: () -> Unit, onRegister: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFE8F5E9), Color.White))).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Recycling, null, tint = GreenPrimary, modifier = Modifier.size(120.dp))
        Spacer(Modifier.height(32.dp))
        Text("SmartWaste", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.ExtraBold, color = GreenDark)
        Text("Cleaning Uganda, one pickup at a time.", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, color = Color.Gray)
        Spacer(Modifier.height(80.dp))
        Button(onClick = onRegister, modifier = Modifier.fillMaxWidth().height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary), shape = RoundedCornerShape(16.dp)) {
            Text("Get Started", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onLogin, modifier = Modifier.fillMaxWidth().height(60.dp), shape = RoundedCornerShape(16.dp)) {
            Text("Login", color = GreenDark, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun LoginScreen(authService: FirebaseAuthService, onLoginSuccess: (String, String, String) -> Unit, onNavigateToRegister: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().background(Color.White).padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Welcome Back", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = GreenDark)
        Spacer(Modifier.height(48.dp))
        AuthTextField(value = name, onValueChange = { name = it }, label = "Full Name (Optional)", icon = Icons.Default.Person)
        Spacer(Modifier.height(16.dp))
        AuthTextField(value = email, onValueChange = { email = it; errorMessage = "" }, label = "Email", icon = Icons.Default.Email)
        Spacer(Modifier.height(16.dp))
        AuthTextField(value = password, onValueChange = { password = it; errorMessage = "" }, label = "Password", icon = Icons.Default.Lock, isPassword = true)
        
        if (errorMessage.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(errorMessage, color = Color.Red, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { 
                scope.launch { 
                    isLoading = true
                    errorMessage = ""
                    try {
                        val result = authService.login(email, password)
                        result.onSuccess {
                            onLoginSuccess(email, password, name)
                        }.onFailure { e ->
                            errorMessage = e.message ?: "Login failed"
                        }
                    } catch (e: Exception) {
                        errorMessage = e.message ?: "An unexpected error occurred"
                    } finally {
                        isLoading = false
                    }
                } 
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading && email.isNotEmpty() && password.isNotEmpty()
        ) {
            if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            else Text("Login", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onNavigateToRegister) { Text("Don't have an account? Register", color = GreenDark) }
    }
}

@Composable
fun RegisterScreen(authService: FirebaseAuthService, onRegisterSuccess: (User, String) -> Unit, onNavigateToLogin: () -> Unit) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isAdminRegistration by remember { mutableStateOf(false) }
    var adminKey by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().background(Color.White).padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Create Account", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = GreenDark)
        Spacer(Modifier.height(48.dp))
        AuthTextField(value = fullName, onValueChange = { fullName = it; errorMessage = "" }, label = "Full Name", icon = Icons.Default.Person)
        Spacer(Modifier.height(16.dp))
        AuthTextField(value = email, onValueChange = { email = it; errorMessage = "" }, label = "Email", icon = Icons.Default.Email)
        Spacer(Modifier.height(16.dp))
        AuthTextField(value = password, onValueChange = { password = it; errorMessage = "" }, label = "Password", icon = Icons.Default.Lock, isPassword = true)
        
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { isAdminRegistration = !isAdminRegistration }) {
            Checkbox(checked = isAdminRegistration, onCheckedChange = { isAdminRegistration = it })
            Text("Register as Admin", color = GreenDark, fontWeight = FontWeight.Medium)
        }

        if (isAdminRegistration) {
            AuthTextField(value = adminKey, onValueChange = { adminKey = it; errorMessage = "" }, label = "Admin Secret Key", icon = Icons.Default.VpnKey, isPassword = true)
        }

        if (errorMessage.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(errorMessage, color = Color.Red, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = {
                if (isAdminRegistration && adminKey != ADMIN_SECRET_KEY) {
                    errorMessage = "Invalid Admin Key"
                } else {
                    scope.launch { 
                        isLoading = true
                        errorMessage = ""
                        try {
                            val role = if (isAdminRegistration) "admin" else "user"
                            val result = authService.register(email, password, fullName, role)
                            result.onSuccess {
                                onRegisterSuccess(User(email, fullName, isAdminRegistration), email)
                            }.onFailure { e ->
                                errorMessage = e.message ?: "Registration failed"
                            }
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "An unexpected error occurred"
                        } finally {
                            isLoading = false
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading && fullName.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()
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
        value = value, onValueChange = onValueChange, label = { Text(label) }, leadingIcon = { Icon(icon, null, tint = GreenPrimary) },
        trailingIcon = {
            if (isPassword) {
                IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null) }
            }
        },
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Email),
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GreenPrimary, focusedLabelColor = GreenPrimary)
    )
}

@Composable
fun HomeScreen(user: User, onReportIssue: () -> Unit, onViewRewards: () -> Unit, onViewSchedule: () -> Unit, onViewTips: () -> Unit, onLogout: () -> Unit, onAdminAccess: () -> Unit, isAdmin: Boolean) {
    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(selected = true, onClick = {}, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") })
                NavigationBarItem(selected = false, onClick = onViewSchedule, icon = { Icon(Icons.Default.CalendarMonth, null) }, label = { Text("Schedule") })
                NavigationBarItem(selected = false, onClick = onViewRewards, icon = { Icon(Icons.Default.Stars, null) }, label = { Text("Rewards") })
                NavigationBarItem(selected = false, onClick = onViewTips, icon = { Icon(Icons.Default.Lightbulb, null) }, label = { Text("Tips") })
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize().verticalScroll(rememberScrollState())) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp).background(Brush.verticalGradient(listOf(GreenDark, GreenPrimary))).padding(24.dp)) {
                Column(modifier = Modifier.align(Alignment.BottomStart)) {
                    Text("Hello,", color = Color.White.copy(alpha = 0.8f))
                    Text(user.fullName, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Color.White)
                }
                IconButton(onClick = onLogout, modifier = Modifier.align(Alignment.TopEnd)) { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color.White) }
                if (isAdmin) {
                    Button(onClick = onAdminAccess, modifier = Modifier.align(Alignment.TopStart), colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)), shape = RoundedCornerShape(8.dp)) {
                        Icon(Icons.Default.AdminPanelSettings, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Admin", color = Color.White, fontSize = 12.sp)
                    }
                }
            }

            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard("Collected", "${user.wasteCollected} kg", Icons.Default.Delete, Modifier.weight(1f))
                StatCard("Impact", "${user.impact}%", Icons.AutoMirrored.Filled.TrendingUp, Modifier.weight(1f))
            }

            Text("Service Areas Near You", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Card(modifier = Modifier.padding(16.dp).fillMaxWidth().height(200.dp), shape = RoundedCornerShape(16.dp)) {
                val kampala = LatLng(0.3476, 32.5825)
                val cameraPositionState = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(kampala, 12f) }
                val markerState = rememberMarkerState(position = kampala)
                GoogleMap(modifier = Modifier.fillMaxSize(), cameraPositionState = cameraPositionState) {
                    Marker(state = markerState, title = "Kampala Center", snippet = "Collection Center")
                }
            }

            Text("What would you like to do?", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionCard("Report Waste Issue", "Spotted illegal dumping or overflowing bins? Let us know.", Icons.Default.Report, GreenPrimary, onReportIssue)
                ActionCard("Request Special Pickup", "Have bulky items? Schedule a custom collection.", Icons.Default.LocalShipping, Color(0xFF2196F3)) {}
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, tint = GreenPrimary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(label, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun ActionCard(title: String, desc: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp), shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.1f), modifier = Modifier.size(48.dp)) { Icon(icon, null, tint = color, modifier = Modifier.padding(12.dp)) }
            Spacer(Modifier.width(16.dp))
            Column { Text(title, fontWeight = FontWeight.Bold); Text(desc, fontSize = 12.sp, color = Color.Gray) }
        }
    }
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportIssueScreen(authService: FirebaseAuthService, userEmail: String, onBack: () -> Unit, onSubmit: () -> Unit) {
    var selectedIssue by remember { mutableStateOf("Overflowing Bin") }
    var location by remember { mutableStateOf("Fetching location...") }
    var latitude by remember { mutableDoubleStateOf(0.0) }
    var longitude by remember { mutableDoubleStateOf(0.0) }
    var description by remember { mutableStateOf("") }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Helper to fetch current location
    fun fetchLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                latitude = loc.latitude
                longitude = loc.longitude
                scope.launch {
                    try {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        val addresses = withContext(Dispatchers.IO) {
                            geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                        }
                        if (!addresses.isNullOrEmpty()) {
                            val address = addresses[0]
                            location = address.getAddressLine(0) ?: "Lat: ${loc.latitude}, Lon: ${loc.longitude}"
                        } else {
                            location = "Lat: ${loc.latitude}, Lon: ${loc.longitude}"
                        }
                    } catch (e: Exception) {
                        location = "Lat: ${loc.latitude}, Lon: ${loc.longitude}"
                    }
                }
            } else {
                location = "Location unavailable. Please check GPS."
            }
        }.addOnFailureListener {
            location = "Failed to get location."
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? -> 
        if (uri != null) {
            capturedImageUri = uri
            fetchLocation() // Fetch location when image is chosen
        }
    }
    
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
        if (success) {
            capturedImageUri = tempPhotoUri
            fetchLocation() // Fetch location when photo is taken
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[android.Manifest.permission.CAMERA] ?: false
        val locationGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        
        if (cameraGranted && locationGranted) {
            val photoFile = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "IMG_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
            tempPhotoUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Camera and Location permissions are required", Toast.LENGTH_SHORT).show()
        }
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
                    permissionLauncher.launch(arrayOf(
                        android.Manifest.permission.CAMERA,
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    ))
                }) { Text("Camera") } 
            }
        )
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { /* Don't dismiss on back click */ },
            title = { Text("Report Submitted!") },
            text = { 
                Column {
                    Icon(Icons.Default.CheckCircle, null, tint = GreenPrimary, modifier = Modifier.size(64.dp).align(Alignment.CenterHorizontally))
                    Spacer(Modifier.height(16.dp))
                    Text("Your report has been successfully sent to the administrator. Thank you for keeping our city clean!", textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text("+10 Eco Points Awarded!", color = GreenPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            },
            confirmButton = {
                Button(onClick = { 
                    showSuccessDialog = false
                    onSubmit()
                }, colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)) {
                    Text("Great!")
                }
            }
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Report an Issue", color = Color.White) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenDark)) }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
            Text("Select Issue:", fontWeight = FontWeight.Bold)
            val issues = listOf("Overflowing Bin", "Illegal Dump", "Missed Pickup", "Damaged Bin", "Other")
            var expanded by remember { mutableStateOf(false) }
            Box {
                OutlinedTextField(value = selectedIssue, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { expanded = true }, trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) }, shape = RoundedCornerShape(8.dp), enabled = false, colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface, disabledBorderColor = MaterialTheme.colorScheme.outline))
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) { issues.forEach { issue -> DropdownMenuItem(text = { Text(issue) }, onClick = { selectedIssue = issue; expanded = false }) } }
            }
            Spacer(Modifier.height(16.dp))
            Text("Add Photo", fontWeight = FontWeight.Bold)
            Surface(modifier = Modifier.fillMaxWidth().height(150.dp).padding(vertical = 8.dp).clickable { showImageSourceDialog = true }, shape = RoundedCornerShape(8.dp), color = Color.White, border = CardDefaults.outlinedCardBorder()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    if (capturedImageUri != null) { 
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(48.dp), tint = GreenPrimary)
                        Text("Photo Selected", color = GreenPrimary) 
                    }
                    else { Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(48.dp), tint = Color.Gray); Text("Tap to select Gallery or Camera", color = Color.Gray) }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Auto-Detected Location:", fontWeight = FontWeight.Bold)
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = GreenPrimary.copy(alpha = 0.05f))
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = GreenPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text(location, style = MaterialTheme.typography.bodyMedium, color = GreenDark)
                }
            }
            
            // Map Preview
            if (latitude != 0.0) {
                Card(modifier = Modifier.fillMaxWidth().height(150.dp).padding(vertical = 8.dp), shape = RoundedCornerShape(12.dp)) {
                    val reportLocation = LatLng(latitude, longitude)
                    val cameraPositionState = rememberCameraPositionState {
                        position = CameraPosition.fromLatLngZoom(reportLocation, 15f)
                    }
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        uiSettings = MapUiSettings(zoomControlsEnabled = false, scrollGesturesEnabled = false)
                    ) {
                        Marker(state = rememberMarkerState(position = reportLocation))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Description:", fontWeight = FontWeight.Bold)
            OutlinedTextField(value = description, onValueChange = { description = it }, modifier = Modifier.fillMaxWidth().height(120.dp).padding(vertical = 8.dp), placeholder = { Text("Enter details about the problem...") }, shape = RoundedCornerShape(8.dp))
            Spacer(Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = onBack, modifier = Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray), shape = RoundedCornerShape(8.dp)) { Text("Cancel", color = Color.Black) }
                Button(
                    onClick = {
                        scope.launch {
                            isSubmitting = true
                            try {
                                val db = FirebaseDatabase.getInstance().reference
                                val report = hashMapOf(
                                    "userEmail" to userEmail, 
                                    "issueType" to selectedIssue, 
                                    "location" to location, 
                                    "latitude" to latitude,
                                    "longitude" to longitude,
                                    "description" to description, 
                                    "timestamp" to System.currentTimeMillis(), 
                                    "status" to "Pending",
                                    "imageUri" to (capturedImageUri?.toString() ?: "")
                                )
                                withTimeout(25000) { db.child("reports").push().setValue(report).await() }
                                
                                // Update user's impact/points for reporting
                                authService.getCurrentUser()?.uid?.let { uid ->
                                    val profile = authService.getUserProfile(uid)
                                    if (profile != null) {
                                        val currentPoints = (profile["ecoPoints"] as? Number)?.toInt() ?: 0
                                        val currentImpact = (profile["impact"] as? Number)?.toInt() ?: 0
                                        db.child("users").child(uid).updateChildren(mapOf(
                                            "ecoPoints" to currentPoints + 10,
                                            "impact" to currentImpact + 2
                                        )).await()
                                    }
                                }

                                showSuccessDialog = true
                            } catch (e: Exception) {
                                Log.e("SmartWaste", "Submission failed", e)
                                val msg = if (e is kotlinx.coroutines.TimeoutCancellationException) "Timeout: Check internet/rules." else "Error: ${e.localizedMessage}"
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            } finally {
                                isSubmitting = false
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary), shape = RoundedCornerShape(8.dp), enabled = !isSubmitting && latitude != 0.0
                ) {
                    if (isSubmitting) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else Text("Submit Report", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TipsScreen(onBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Recycling Tips", color = Color.White) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenDark)) }) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
            TipCard("Clean your recyclables", "Rinse food containers.", Icons.Default.CleaningServices)
            TipCard("Check local guidelines", "Look for numbers in triangles.", Icons.Default.Info)
            TipCard("Reduce & Reuse", "Source reduction is best.", Icons.Default.Loop)
            TipCard("Compost organics", "Scraps turn into soil.", Icons.Default.Grass)
        }
    }
}

@Composable
fun TipCard(title: String, description: String, icon: ImageVector) {
    Card(modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = GreenPrimary.copy(alpha = 0.1f), modifier = Modifier.size(48.dp)) { Icon(icon, null, tint = GreenPrimary, modifier = Modifier.padding(12.dp)) }
            Spacer(Modifier.width(16.dp))
            Column { Text(title, fontWeight = FontWeight.Bold, color = GreenDark); Text(description, fontSize = 14.sp, color = Color.Gray) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardsScreen(ecoPoints: Int, onBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Eco Rewards", color = Color.White) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenDark)) }) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize().verticalScroll(rememberScrollState())) {
            Card(modifier = Modifier.padding(16.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(4.dp), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Your Eco Points", color = Color.Gray); Text(ecoPoints.toString(), fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = GreenDark); Text("Points Earned", fontSize = 14.sp, color = Color.Gray)
                    val progress = (ecoPoints % 150) / 150f
                    Spacer(Modifier.height(16.dp)); LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp), color = GreenPrimary, trackColor = Color.LightGray); Text("Next Reward: 150 Points", modifier = Modifier.padding(top = 8.dp), fontSize = 12.sp)
                }
            }
            RewardListItem("Free Coffee", "150 Points", Icons.Default.Coffee, Color(0xFF795548))
            RewardListItem("Discount", "200 Points", Icons.Default.ConfirmationNumber, Color(0xFFFF9800))
        }
    }
}

@Composable
fun RewardListItem(title: String, subtitle: String, icon: ImageVector, iconColor: Color) {
    Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(8.dp), color = iconColor.copy(alpha = 0.1f), modifier = Modifier.size(50.dp)) { Icon(icon, null, tint = iconColor, modifier = Modifier.padding(12.dp)) }
            Spacer(Modifier.width(16.dp))
            Column { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, color = Color.Gray, fontSize = 12.sp) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Schedule", color = Color.White) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenDark)) }) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            ScheduleItem("Tomorrow, 08:00 AM", "Organic Waste", "Scheduled")
        }
    }
}

@Composable
fun ScheduleItem(time: String, type: String, status: String) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column { Text(time, fontWeight = FontWeight.Bold); Text(type, color = Color.Gray) }
            Text(status, color = GreenPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

@Preview(showBackground = true) @Composable fun DashboardPreview() { SmartWasteTheme { SmartWasteApp() } }
