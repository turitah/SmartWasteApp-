package com.example.smartwaste

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartwaste.admin.AdminDashboardScreen
import com.example.smartwaste.driver.DriverDashboardScreen
import com.example.smartwaste.driver.DriverViewModel
import com.example.smartwaste.ui.MainLoginScreen
import com.example.smartwaste.ui.SmartWasteApp
import com.example.smartwaste.ui.theme.SmartWasteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmartWasteTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
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
                                // PASS THE EMAIL TO SMARTWASTEAPP
                                SmartWasteApp(
                                    initialEmail = email,
                                    onLogoutRequest = { currentEmail = null }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
