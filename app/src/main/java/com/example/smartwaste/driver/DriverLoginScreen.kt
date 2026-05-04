package com.example.smartwaste.driver

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun DriverLoginScreen(viewModel: DriverViewModel, onLoginSuccess: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isResetLoading by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }
    var resetErrorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    
    val isLoading by viewModel.isLoading.collectAsState()
    val loginStatus by viewModel.loginStatus.collectAsState()

    LaunchedEffect(loginStatus) {
        if (loginStatus?.isSuccess == true) {
            onLoginSuccess()
            viewModel.clearLoginStatus()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Driver Login",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email or Phone") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )
        
        TextButton(
            onClick = {
                if (email.isBlank()) {
                    resetErrorMessage = "Please enter your email to reset password"
                } else {
                    scope.launch {
                        isResetLoading = true
                        resetErrorMessage = ""
                        successMessage = ""
                        // We can't easily access authService from here unless it's public in ViewModel or we use a new instance
                        // DriverViewModel has it private. Let's add a resetPassword function to DriverViewModel.
                        viewModel.resetPassword(email) { result ->
                            isResetLoading = false
                            result.onSuccess {
                                successMessage = "Password reset email sent!"
                            }.onFailure {
                                resetErrorMessage = it.message ?: "Failed to send reset email"
                            }
                        }
                    }
                }
            },
            modifier = Modifier.align(Alignment.End),
            enabled = !isResetLoading && !isLoading
        ) {
            if (isResetLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Text("Forgot Password?", style = MaterialTheme.typography.bodySmall)
            }
        }

        if (resetErrorMessage.isNotEmpty()) {
            Text(
                text = resetErrorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.Start)
            )
        }

        if (successMessage.isNotEmpty()) {
            Text(
                text = successMessage,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.Start)
            )
        }

        if (loginStatus?.isFailure == true) {
            Text(
                text = loginStatus?.exceptionOrNull()?.message ?: "Login failed",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.Start)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                viewModel.login(email, password)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && email.isNotBlank() && password.isNotBlank()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Login")
            }
        }
    }
}
