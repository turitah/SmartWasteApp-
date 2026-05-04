package com.example.smartwaste.ui

import android.annotation.SuppressLint
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
import androidx.compose.ui.draw.alpha
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
import com.example.smartwaste.admin.ReportItem
import com.example.smartwaste.auth.FirebaseAuthService
import com.example.smartwaste.ui.theme.GreenDark
import com.example.smartwaste.ui.theme.GreenPrimary
import com.example.smartwaste.ui.theme.SmartWasteTheme
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
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
    onUserLoggedIn: (String, String) -> Unit = { _, _ -> },
    onLogoutRequest: () -> Unit = {}
) {
    // State management (Source of Truth)
    var currentScreen by remember { 
        mutableStateOf(if (initialEmail != null) SmartWasteScreen.Home else SmartWasteScreen.Welcome) 
    }
    var currentUser by remember { 
        mutableStateOf(initialEmail?.let { email -> User(email, email.split("@")[0].replaceFirstChar { it.uppercase() }) }) 
    }
    val authService = remember { FirebaseAuthService() }

    // Real-time user profile listener
    LaunchedEffect(currentUser?.email) {
        val uid = authService.getCurrentUser()?.uid
        if (uid != null) {
            val userRef = FirebaseDatabase.getInstance().reference.child("users").child(uid)
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        currentUser = currentUser?.copy(
                            fullName = snapshot.child("fullName").getValue(String::class.java) ?: currentUser!!.fullName,
                            wasteCollected = snapshot.child("wasteCollected").getValue(Double::class.java) ?: 0.0,
                            impact = snapshot.child("impact").getValue(Int::class.java) ?: 0,
                            ecoPoints = snapshot.child("ecoPoints").getValue(Int::class.java) ?: 0,
                            isAdmin = snapshot.child("role").getValue(String::class.java) == "admin"
                        )
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("SmartWasteApp", "Database error: ${error.message}")
                }
            }
            userRef.addValueEventListener(listener)
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
                onLoginSuccess = { email, password, name, role -> 
                    val isUserAdmin = role == "admin"
                    val displayName = if (isUserAdmin) "Admin User" 
                                     else name.ifBlank { email.split("@")[0].replaceFirstChar { it.uppercase() } }

                    currentUser = User(email = email, fullName = displayName, isAdmin = isUserAdmin)
                    onUserLoggedIn(email, role)
                    currentScreen = if (isUserAdmin) SmartWasteScreen.AdminDashboard else SmartWasteScreen.Home 
                },
                onNavigateToRegister = { currentScreen = SmartWasteScreen.Register }
            )
            SmartWasteScreen.Register -> RegisterScreen(
                authService = authService,
                onRegisterSuccess = { user, email, role -> 
                    currentUser = user
                    onUserLoggedIn(email, role)
                    currentScreen = if (role == "admin") SmartWasteScreen.AdminDashboard else SmartWasteScreen.Home 
                },
                onNavigateToLogin = { currentScreen = SmartWasteScreen.Login }
            )
            SmartWasteScreen.Home -> HomeScreen(
                user = currentUser ?: User("", "User"),
                onReportIssue = { currentScreen = SmartWasteScreen.ReportIssue },
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
                userEmail = currentUser?.email ?: "anonymous",
                onBack = { currentScreen = SmartWasteScreen.Home },
                onSubmit = { currentScreen = SmartWasteScreen.Home }
            )
            SmartWasteScreen.Rewards -> RewardsScreen(
                ecoPoints = currentUser?.ecoPoints ?: 0,
                onBack = { currentScreen = SmartWasteScreen.Home }
            )
            SmartWasteScreen.History -> HistoryScreen(
                onBack = { currentScreen = SmartWasteScreen.Home }
            )
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
fun LoginScreen(authService: FirebaseAuthService, onLoginSuccess: (String, String, String, String) -> Unit, onNavigateToRegister: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            color = GreenPrimary.copy(alpha = 0.1f),
            shape = CircleShape,
            modifier = Modifier.size(100.dp)
        ) {
            Icon(
                Icons.Default.LockPerson,
                null,
                tint = GreenPrimary,
                modifier = Modifier.padding(20.dp)
            )
        }
        
        Spacer(Modifier.height(24.dp))
        Text(
            "Welcome Back",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = GreenDark
        )
        Text(
            "Sign in to continue your impact",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        
        Spacer(Modifier.height(48.dp))
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
                        result.onSuccess { user ->
                            val profile = authService.getUserProfile(user.uid)
                            val role = profile?.get("role") as? String ?: "user"
                            val name = profile?.get("fullName") as? String ?: ""
                            onLoginSuccess(email, password, name, role)
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
fun RegisterScreen(authService: FirebaseAuthService, onRegisterSuccess: (User, String, String) -> Unit, onNavigateToLogin: () -> Unit) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("user") }
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
        Spacer(Modifier.height(32.dp))
        AuthTextField(value = fullName, onValueChange = { fullName = it; errorMessage = "" }, label = "Full Name", icon = Icons.Default.Person)
        Spacer(Modifier.height(16.dp))
        AuthTextField(value = email, onValueChange = { email = it; errorMessage = "" }, label = "Email", icon = Icons.Default.Email)
        Spacer(Modifier.height(16.dp))
        AuthTextField(value = password, onValueChange = { password = it; errorMessage = "" }, label = "Password", icon = Icons.Default.Lock, isPassword = true)
        
        Spacer(Modifier.height(16.dp))
        Text("Register as:", modifier = Modifier.fillMaxWidth(), color = GreenDark, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selectedRole == "user", onClick = { selectedRole = "user" })
                Text("User")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selectedRole == "driver", onClick = { selectedRole = "driver" })
                Text("Driver")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selectedRole == "admin", onClick = { selectedRole = "admin" })
                Text("Admin")
            }
        }

        if (selectedRole == "admin") {
            AuthTextField(value = adminKey, onValueChange = { adminKey = it; errorMessage = "" }, label = "Admin Secret Key", icon = Icons.Default.VpnKey, isPassword = true)
        }

        if (errorMessage.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(errorMessage, color = Color.Red, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = {
                if (selectedRole == "admin" && adminKey != ADMIN_SECRET_KEY) {
                    errorMessage = "Invalid Admin Key"
                } else {
                    scope.launch { 
                        isLoading = true
                        errorMessage = ""
                        try {
                            val result = authService.register(email, password, fullName, selectedRole)
                            result.onSuccess {
                                onRegisterSuccess(User(email, fullName, selectedRole == "admin"), email, selectedRole)
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
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null, tint = GreenPrimary) },
        trailingIcon = {
            if (isPassword) {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                }
            }
        },
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Email),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
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
                ActionCard("Request Special Pickup", "Have bulky items? Schedule a custom collection.", Icons.Default.LocalShipping, Color(0xFF2196F3), onViewSchedule)
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
fun ReportIssueScreen(userEmail: String, onBack: () -> Unit, onSubmit: () -> Unit) {
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
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(0.0, 0.0), 15f)
    }

    fun fetchLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    latitude = loc.latitude
                    longitude = loc.longitude
                    cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 15f))
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
                        } catch (_: Exception) {
                            location = "Lat: ${loc.latitude}, Lon: ${loc.longitude}"
                        }
                    }
                } else {
                    location = "Location unavailable. Please check GPS."
                }
            }.addOnFailureListener {
                location = "Failed to get location."
            }
        } catch (_: SecurityException) {
            location = "Location permission denied."
        }
    }

    LaunchedEffect(Unit) {
        fetchLocation()
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? -> 
        if (uri != null) {
            capturedImageUri = uri
            fetchLocation()
        }
    }
    
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
        if (success) {
            capturedImageUri = tempPhotoUri
            fetchLocation()
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
            fetchLocation()
        } else {
            Toast.makeText(context, "Permissions required", Toast.LENGTH_SHORT).show()
        }
    }

    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false }, 
            title = { Text("Select Source") }, 
            text = { Text("Choose image source.") },
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
            onDismissRequest = { },
            title = { Text("Report Submitted!") },
            text = { Text("Your report has been sent. Thank you!") },
            confirmButton = {
                Button(onClick = { 
                    showSuccessDialog = false
                    onSubmit()
                }) { Text("OK") }
            }
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Report Issue", color = Color.White) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenDark)) }
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
                    } else { Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(48.dp), tint = Color.Gray) }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Location:", fontWeight = FontWeight.Bold)
            
            Card(modifier = Modifier.fillMaxWidth().height(200.dp).padding(vertical = 8.dp), shape = RoundedCornerShape(12.dp)) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState
                ) {
                    Marker(
                        state = rememberMarkerState(position = LatLng(latitude, longitude)),
                        title = "Your Location",
                        snippet = location
                    )
                }
            }
            Text(location, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))

            Spacer(Modifier.height(16.dp))
            Text("Description:", fontWeight = FontWeight.Bold)
            OutlinedTextField(value = description, onValueChange = { description = it }, modifier = Modifier.fillMaxWidth().height(120.dp).padding(vertical = 8.dp), placeholder = { Text("Details...") }, shape = RoundedCornerShape(8.dp))
            Spacer(Modifier.height(32.dp))
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
                                "status" to "Pending"
                            )
                            db.child("reports").push().setValue(report).await()
                            showSuccessDialog = true
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        } finally {
                            isSubmitting = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary), shape = RoundedCornerShape(8.dp), enabled = !isSubmitting
            ) {
                if (isSubmitting) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text("Submit Report", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TipsScreen(onBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Eco Tips", color = Color.White) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenDark)) }) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
            TipCard("Clean your recyclables", "Rinse containers.", Icons.Default.CleaningServices)
            TipCard("Reduce & Reuse", "Use reusable bags.", Icons.Default.Loop)
            TipCard("Compost organics", "Fruit peels etc.", Icons.Default.Grass)
        }
    }
}

@Composable
fun TipCard(title: String, description: String, icon: ImageVector) {
    Card(modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = GreenPrimary, modifier = Modifier.size(32.dp))
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
                    Text("Your Eco Points", color = Color.Gray)
                    Text(ecoPoints.toString(), fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = GreenDark)
                }
            }
            Text("Badges", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            BadgeListItem("Eco Starter", "Initial Badge", Icons.Default.Person, Color.Gray, true)
            BadgeListItem("Bronze Badge", "100 Points Required", Icons.Default.MilitaryTech, Color(0xFFCD7F32), ecoPoints >= 100)
            BadgeListItem("Silver Badge", "150 Points Required", Icons.Default.Stars, Color(0xFFC0C0C0), ecoPoints >= 150)
        }
    }
}

@Composable
fun BadgeListItem(title: String, subtitle: String, icon: ImageVector, iconColor: Color, isEarned: Boolean) {
    Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth().alpha(if (isEarned) 1f else 0.6f)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (isEarned) iconColor else Color.Gray, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(16.dp))
            Column { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, color = Color.Gray, fontSize = 12.sp) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val db = FirebaseDatabase.getInstance().reference
    val authService = remember { FirebaseAuthService() }
    val userEmail = authService.getCurrentUser()?.email ?: ""
    var reports by remember { mutableStateOf<List<ReportItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(userEmail) {
        if (userEmail.isNotEmpty()) {
            db.child("reports").orderByChild("userEmail").equalTo(userEmail)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val list = mutableListOf<ReportItem>()
                        for (doc in snapshot.children) {
                            list.add(
                                ReportItem(
                                    id = doc.key ?: "",
                                    userName = doc.child("userEmail").getValue(String::class.java) ?: "Unknown",
                                    issueType = doc.child("issueType").getValue(String::class.java) ?: "General",
                                    location = doc.child("location").getValue(String::class.java) ?: "Unknown",
                                    status = doc.child("status").getValue(String::class.java) ?: "Pending",
                                    description = doc.child("description").getValue(String::class.java) ?: "",
                                    timestamp = doc.child("timestamp").getValue(Long::class.java) ?: 0L,
                                    adminNotes = doc.child("adminNotes").getValue(String::class.java) ?: ""
                                )
                            )
                        }
                        reports = list.sortedByDescending { it.timestamp }
                        isLoading = false
                    }
                    override fun onCancelled(error: DatabaseError) { isLoading = false }
                })
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("My Reports", color = Color.White) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenDark)) }) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
            if (isLoading) { CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally)) }
            else if (reports.isEmpty()) { Text("No reports found.", color = Color.Gray) }
            else { reports.forEach { ReportStatusCard(it) } }
            Spacer(Modifier.height(24.dp))
            Text("Upcoming Collections", fontWeight = FontWeight.Bold, color = GreenDark)
            ScheduleItem("Tomorrow, 08:00 AM", "Organic Waste", "Scheduled")
        }
    }
}

@Composable
fun ReportStatusCard(report: ReportItem) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(report.issueType, fontWeight = FontWeight.Bold)
                Text(report.status, color = if (report.status == "Resolved") GreenPrimary else Color(0xFFFF9800))
            }
            Text(report.location, fontSize = 14.sp, color = Color.Gray)
            Text(report.description, fontSize = 14.sp)
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
