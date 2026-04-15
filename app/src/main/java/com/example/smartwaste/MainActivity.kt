package com.example.smartwaste

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.example.smartwaste.admin.AdminDashboardScreen
import com.example.smartwaste.driver.DriverDashboardScreen
import com.example.smartwaste.driver.DriverViewModel
import com.example.smartwaste.ui.MainLoginScreen
import com.example.smartwaste.ui.SmartWasteApp
import com.example.smartwaste.ui.theme.SmartWasteTheme
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartWasteTheme {
                var currentEmail by remember { mutableStateOf<String?>(null) }
                
                if (currentEmail == null) {
                    MainLoginScreen(onLoginSuccess = { email ->
                        currentEmail = email
                    })
                } else {
                    val email = currentEmail!!
                    when {
                        email.contains("driver", ignoreCase = true) -> {
                            val driverViewModel: DriverViewModel = viewModel()
                            DriverDashboardScreen(
                                viewModel = driverViewModel,
                                onLogout = { currentEmail = null }
                            )
                        }
                        email.contains("admin", ignoreCase = true) -> {
                            AdminDashboardScreen(
                                onLogout = { currentEmail = null }
                            )
                        }
                        else -> {
                            // SmartWasteApp handles its own internal navigation and logout
                            // but we need a way to get back to login if user logs out from SmartWasteApp
                            SmartWasteApp(onLogoutRequest = { currentEmail = null })
                        }
                    }
                }
            }
        }
    }
}
